package com.tomst.lolly.core;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TauParser {

    private static final String TAG = "TauParser";

    // CRC Tabulka z Delphi kódu
    private static final int[] CRC8_TABLE = {
            0, 94, 188, 226, 97, 63, 221, 131, 194, 156, 126, 32, 163, 253, 31, 65,
            157, 195, 33, 127, 252, 162, 64, 30, 95, 1, 227, 189, 62, 96, 130, 220,
            35, 125, 159, 193, 66, 28, 254, 160, 225, 191, 93, 3, 128, 222, 60, 98,
            190, 224, 2, 92, 223, 129, 99, 61, 124, 34, 192, 158, 29, 67, 161, 255,
            70, 24, 250, 164, 39, 121, 155, 197, 132, 218, 56, 102, 229, 187, 89, 7,
            219, 133, 103, 57, 186, 228, 6, 88, 25, 71, 165, 251, 120, 38, 196, 154,
            101, 59, 217, 135, 4, 90, 184, 230, 167, 249, 27, 69, 198, 152, 122, 36,
            248, 166, 68, 26, 153, 199, 37, 123, 58, 100, 134, 216, 91, 5, 231, 185,
            140, 210, 48, 110, 237, 179, 81, 15, 78, 16, 242, 172, 47, 113, 147, 205,
            17, 79, 173, 243, 112, 46, 204, 146, 211, 141, 111, 49, 178, 236, 14, 80,
            175, 241, 19, 77, 206, 144, 114, 44, 109, 51, 209, 143, 12, 82, 176, 238,
            50, 108, 142, 208, 83, 13, 239, 177, 240, 174, 76, 18, 145, 207, 45, 115,
            202, 148, 118, 40, 171, 245, 23, 73, 8, 86, 180, 234, 105, 55, 213, 139,
            87, 9, 235, 181, 54, 104, 138, 212, 149, 203, 41, 119, 244, 170, 72, 22,
            233, 183, 85, 11, 136, 214, 52, 106, 43, 117, 151, 201, 74, 20, 246, 168,
            116, 42, 200, 150, 21, 75, 169, 247, 182, 232, 10, 84, 215, 137, 107, 53
    };

    /**
     * Reprezentuje informace o firmwaru pro konkrétní verzi hardwaru/firmwaru.
     */
    public static class RTau {
        public byte hw;
        public byte fw;
        public int fwCount;

        public RTau(byte hw, byte fw) {
            this.hw = hw;
            this.fw = fw;
            this.fwCount = 0;
        }

        @Override
        public String toString() {
            return String.format("HW[$%02X] FW[$%02X] Count: %d", hw, fw, fwCount);
        }
    }

    private List<RTau> fTauInfo = new ArrayList<>();
    private int fTauCount = 0;

    /**
     * Převede 16znakový hexadecimální řetězec na pole 8 bajtů.
     *
     * @param hexString Hexadecimální řetězec (musí mít 16 znaků).
     * @return 8bajtové pole, nebo null, pokud je řetězec neplatný.
     */
    public static byte[] hexToBytes(String hexString) {
        if (hexString == null || hexString.length() != 16) {
            return null;
        }
        byte[] bytes = new byte[8];
        for (int i = 0; i < 8; i++) {
            try {
                int index = i * 2;
                int j = Integer.parseInt(hexString.substring(index, index + 2), 16);
                bytes[i] = (byte) j;
            } catch (NumberFormatException e) {
                Log.e(TAG, "Neplatný hexadecimální znak v řetězci: " + hexString);
                return null;
            }
        }
        return bytes;
    }

    /**
     * Vypočítá CRC-8 pro prvních 7 bajtů datového pole.
     * Dodržuje logiku z poskytnutého Delphi kódu.
     *
     * @param data 8bajtové pole. 8. bajt je ignorován.
     * @return Vypočítaný CRC-8 bajt.
     */
    public static byte calculateCRC8(byte[] data) {
        if (data == null || data.length < 8) {
            return 0;
        }

        int crc = 0;

        // Použijeme neznaménkové hodnoty pro výpočet pomocí maskování s 0xFF
        crc = CRC8_TABLE[crc ^ (data[0] & 0xFF)];
        // Delphi smyčka: For j:=7 DownTo 2 Do
        // Pole v Delphi je 1-based, takže v Javě je to index 6 až 1
        for (int j = 6; j >= 1; j--) {
            crc = CRC8_TABLE[crc ^ (data[j] & 0xFF)];
        }

        return (byte) crc;
    }

    /**
     * Analyzuje data firmwaru z dekódovaného řetězce.
     *
     * @param decodedContent Víceřádkový řetězcový obsah z dekódovaného .tau souboru.
     * @param targetHw       Cílová verze hardwaru, pro kterou se mají počítat firmwarové pakety.
     * @param targetFw       Cílová verze firmwaru, pro kterou se mají počítat firmwarové pakety.
     * @return Počet firmwarových paketů pro zadaný HW/FW.
     */
    public int analyzeFirmware(String decodedContent, byte targetHw, byte targetFw) {
        // Použijeme mapu pro robustní sčítání paketů pro každou verzi HW/FW
        Map<String, RTau> tauMap = new HashMap<>();

        if (decodedContent == null) {
            return 0;
        }
        
        String[] lines = decodedContent.split("\\r?\\n");

        for (String line : lines) {
            if (line.isEmpty() || line.length() != 17 || !line.startsWith("D")) {
                continue; // Přeskočit neplatné řádky
            }

            String hexData = line.substring(1); // Odstranit úvodní 'D'
            byte[] idData = hexToBytes(hexData);

            if (idData == null) {
                Log.e(TAG, "Nelze převést řádek na bajty: " + line);
                continue;
            }

            byte calculatedCrc = calculateCRC8(idData);
            if (idData[7] != calculatedCrc) {
                Log.e(TAG, "Špatné CRC pro řádek: " + line +
                        " Očekáváno: " + String.format("%02X", idData[7]) +
                        " Vypočítáno: " + String.format("%02X", calculatedCrc));
                continue;
            }

            String key = String.format("%02X:%02X", idData[0], idData[1]);
            RTau currentTau = tauMap.get(key);
            if (currentTau == null) {
                currentTau = new RTau(idData[0], idData[1]);
                tauMap.put(key, currentTau);
            }
            currentTau.fwCount++;
        }

        // Naplnit konečný seznam a počet z mapy
        this.fTauInfo = new ArrayList<>(tauMap.values());
        this.fTauCount = this.fTauInfo.size();
        
        // Logovat všechny nalezené firmwary a jejich počty
        for(RTau info : fTauInfo) {
            Log.d(TAG, "Nalezeno: " + info.toString());
        }

        // Najít počet pro konkrétní cíl
        String targetKey = String.format("%02X:%02X", targetHw, targetFw);
        if (tauMap.containsKey(targetKey)) {
            return tauMap.get(targetKey).fwCount;
        }

        return 0; // Cíl nenalezen
    }

    // Gettery pro výsledky, pokud jsou potřeba
    public List<RTau> getTauInfo() {
        return fTauInfo;
    }

    public int getTauCount() {
        return fTauCount;
    }
}