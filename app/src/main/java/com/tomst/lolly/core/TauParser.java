package com.tomst.lolly.core;

import android.util.Log;

import com.tomst.lolly.LollyActivity;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TauParser {

    private static final String TAG = "TauParser";
    private int ER_CRYPT_DECODE = 238;


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

    public int DecodeTAUFile(String tauFile, ByteArrayOutputStream strmOutput) {

        // Delphi: strmInput: TFileStream
        // Java: FileInputStream v try-with-resources pro automatické uzavření
        try (FileInputStream strmInput = new FileInputStream(tauFile);
             java.io.DataInputStream dis = new java.io.DataInputStream(strmInput)) {

            // Delphi: Salt: array[0..7] of byte;
            byte[] salt = new byte[8];
            dis.readFully(salt) ; // vyctu 8 bytu soli

            byte[] hashDigest = new byte[32];
            //dis.readFully(cipherIV);

            // passphrase je stejná jako v Delphi ukázce
            String passphrase = "abcdefghijklmnopqrstuvwxyz";
            com.tomst.lolly.core.DcpHaval dh = new com.tomst.lolly.core.DcpHaval();
            dh.init();
            dh.update(salt,0,8);
            dh.updateStr(passphrase);
            dh.doFinal(hashDigest,0);

            // finalni kodovani
            com.tomst.lolly.core.Blowfish cipher = new com.tomst.lolly.core.Blowfish();
            byte [] cipheriv = new byte[8];
            dis.readFully(cipheriv);


            // Zjistíme, kolik bajtů zbývá ve streamu
            int remainingBytes = strmInput.available();
            if (remainingBytes <= 0) {
                // Žádná data k dekódování
                return 0;
            }

            // zasifrovany vstup
            byte[] encryptedData = new byte[remainingBytes];
            strmInput.read(encryptedData);

            // odsifrovany vystup
            byte[] decryptedData = new byte[remainingBytes];
            cipher.reset();
            cipher.init(hashDigest);
            cipher.setSalt(cipheriv);
            cipher.decryptECB_DelphiCompatible(encryptedData, 0, decryptedData, 0);

            // Procházíme šifrovaná data po 8bajtových blocích
            for (int i = 0; i < encryptedData.length; i += 8) {
                // Dekódujeme jeden 8bajtový blok ze vstupního pole (encryptedData)
                // a výsledek uložíme na odpovídající pozici do výstupního pole (decryptedData).
                cipher.decryptECB(encryptedData, i, decryptedData, i);
            }

            //

            strmOutput.write(decryptedData);
            // --- LADÍCÍ BLOK: Uložení šifrovaných dat do souboru ---
            try {
                //File cacheDir = context.getCacheDir();
                File cacheDir = LollyActivity.getInstance().getApplicationContext().getCacheDir();
                File outputFile = new File(cacheDir, "decrypted.txt");
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(outputFile)) {
                    fos.write(decryptedData);
                    Log.d("DecodeTAU", "Šifrovaná data uložena do: " + outputFile.getAbsolutePath());
                }
            } catch (IOException e) {
                Log.e("DecodeTAU+", "Chyba při ukládání šifrovaných dat do cache.", e);
            }

            return 0; // Vše proběhlo v pořádku

        } catch (Exception e) {
            // Delphi: except result := ER_CRYPT_DECODE;
            e.printStackTrace(); // Pro ladění je dobré chybu vypsat
            return ER_CRYPT_DECODE;
        }
        // Delphi: Cipher.Free; Hash.Free;
        // V Javě není potřeba, Garbage Collector se postará o objekty cipher a hash.
    }

    /**
     * Z interního seznamu firmwarů najde a vrátí nejnovější verzi pro zadaný hardware.
     *
     * @param targetHw Cílová verze hardwaru.
     * @return Objekt RTau s nejnovějším firmwarem, nebo null, pokud pro daný HW nebyl žádný nalezen.
     */
    public RTau getLatestFirmwareForHardware(byte targetHw) {
        if (this.fTauInfo == null || this.fTauInfo.isEmpty()) {
            Log.w(TAG, "Seznam firmwarů je prázdný. Zavolejte nejprve 'findAllFirmwareVariants'.");
            return null;
        }
        RTau latestFirmware = null;
        for (RTau currentFirmware : this.fTauInfo) {
            if (currentFirmware.hw == targetHw) {
                if (latestFirmware == null || Byte.toUnsignedInt(currentFirmware.fw) > Byte.toUnsignedInt(latestFirmware.fw)) {
                    latestFirmware = currentFirmware;
                }
            }
        }
        return latestFirmware;
    }

    /**
     * Z interního seznamu firmwarů najde a vrátí specifickou variantu.
     *
     * @param targetHw Cílový hardware.
     * @param targetFw Cílový firmware.
     * @return Objekt RTau, pokud je nalezena shoda, jinak null.
     */
    public RTau getSpecificFirmware(byte targetHw, byte targetFw) {
        if (this.fTauInfo == null || this.fTauInfo.isEmpty()) {
            Log.w(TAG, "Seznam firmwarů je prázdný. Zavolejte nejprve 'findAllFirmwareVariants'.");
            return null;
        }
        for (RTau fwVariant : this.fTauInfo) {
            if (fwVariant.hw == targetHw && fwVariant.fw == targetFw) {
                return fwVariant;
            }
        }
        return null;
    }



    public List<RTau> findAllFirmwareVariants(ByteArrayOutputStream decodedContent) {
        if (decodedContent == null || decodedContent.size() == 0) {
            return new ArrayList<>();
        }
        // String[] lines = decodedContent.split("\\r?\\n");

        // Použijeme Set pro automatické zajištění unikátnosti dvojic.
        // Jako klíč použijeme jednoduchý string "HW:FW".
        Set<String> uniquePairs = new HashSet<>();

        // Čteme stream řádek po řádku, abychom nemuseli celý obsah načítat do paměti jako jeden String
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(decodedContent.toByteArray())))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.length() != 16) {
                    continue;
                }

                byte[] idData = hexToBytes(line);
                if (idData == null || idData[7] != calculateCRC8(idData)) {
                    continue; // Přeskočit neplatné řádky
                }

                byte currentHw = idData[0];
                byte currentFw = idData[1];

                String key = String.format("%02X:%02X", currentHw, currentFw);
                uniquePairs.add(key);
            }
        } catch (IOException e) {
            Log.e(TAG, "Chyba při čtení dekódovaného streamu.", e);
            return new ArrayList<>(); // V případě chyby vrátíme prázdný seznam
        }

        // Převedeme Set klíčů na seznam RTau objektů
        List<RTau> resultList = new ArrayList<>();
        for (String pairKey : uniquePairs) {
            String[] parts = pairKey.split(":");
            // Parsování zpět z hex stringu na byte
            byte hw = (byte) Integer.parseInt(parts[0], 16);
            byte fw = (byte) Integer.parseInt(parts[1], 16);
            resultList.add(new RTau(hw, fw));
        }
        this.fTauInfo = resultList;
        return this.fTauInfo;
    }

    public RTau getLatestFirmwareOverall() {
        if (this.fTauInfo == null || this.fTauInfo.isEmpty()) {
            Log.w(TAG, "Seznam firmwarů je prázdný. Zavolejte nejprve 'findAllFirmwareVariants'.");
            return null;
        }

        // Začneme s prvním prvkem jako dosavadním maximem
        RTau latestFirmware = fTauInfo.get(0);

        // Projdeme zbytek seznamu a hledáme vyšší verzi FW
        for (int i = 1; i < fTauInfo.size(); i++) {
            RTau current = fTauInfo.get(i);
            // Použijeme porovnání bez znaménka, aby byla verze 0xFF (255) správně vyhodnocena jako nejvyšší
            if (Byte.toUnsignedInt(current.fw) > Byte.toUnsignedInt(latestFirmware.fw)) {
                latestFirmware = current; // Našli jsme nový absolutní nejnovější FW
            }
        }
        return latestFirmware;
    }


    // hardware znam, to je D1
    // targetFw bude bud -1 nebo explicitne vybrane cislo
    public List<byte[]> extractLatestFirmwareData(String decodedContent, byte targetHw, byte targetFw) {
        if (decodedContent == null || decodedContent.isEmpty()) {
            return new ArrayList<>();
        }

        String[] lines = decodedContent.split("\\r?\\n");

        // --- PRVNÍ PRŮCHOD: Nalezení nejnovější verze FW pro daný HW ---
        byte latestFw = -1;
        boolean foundFwForHw = false;

        for (String line : lines) {
            if (line.isEmpty() || line.length() != 16) {
                continue;
            }

            byte[] idData = hexToBytes(line);
            if (idData == null || idData[7] != calculateCRC8(idData)) {
                continue; // Přeskočit neplatné řádky (špatný formát nebo CRC)
            }

            byte currentHw = idData[0];
            if (currentHw == targetHw) {
                byte currentFw = idData[1];
                // Porovnáváme jako unsigned int, abychom správně určili nejvyšší verzi
                if (!foundFwForHw || Byte.toUnsignedInt(currentFw) > Byte.toUnsignedInt(latestFw)) {
                    latestFw = currentFw;
                    foundFwForHw = true;
                }
            }
        }

        if (!foundFwForHw) {
            Log.w(TAG, "Pro cílový HW " + String.format("$%02X", targetHw) + " nebyl nalezen žádný firmware.");
            return new ArrayList<>(); // Cílový HW v souboru není
        }

        Log.d(TAG, "Nalezena nejnovější verze FW: " + String.format("$%02X", latestFw) + " pro HW: " + String.format("$%02X", targetHw));

        // --- DRUHÝ PRŮCHOD: Extrakce dat pro nalezenou nejnovější verzi ---
        List<byte[]> firmwareData = new ArrayList<>();
        for (String line : lines) {
            if (line.isEmpty() || line.length() != 16) {
                continue;
            }

            byte[] idData = hexToBytes(line);
            if (idData == null || idData[7] != calculateCRC8(idData)) {
                continue;
            }

            byte currentHw = idData[0];
            byte currentFw = idData[1];

            // Pokud řádek odpovídá cílovému HW a nejnovější verzi FW, extrahujeme data
            if (currentHw == targetHw && currentFw == latestFw) {
                firmwareData.add(idData);
            }
        }

        Log.d(TAG, "Extrahováno " + firmwareData.size() + " datových paketů.");
        return firmwareData;
    }


    // Gettery pro výsledky, pokud jsou potřeba
    public List<RTau> getTauInfo() {
        return fTauInfo;
    }

    public int getTauCount() {
        return fTauCount;
    }
}