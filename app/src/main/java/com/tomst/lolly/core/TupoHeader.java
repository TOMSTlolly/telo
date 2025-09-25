package com.tomst.lolly.core;

import android.util.Log; // Potřebujeme pro logování
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TupoHeader {
    public byte Hw;
    public byte Fw;
    public byte Sub;
    public String Comment;

    private static final String TAG = "TupoHeader"; // Tag pro logování

    // Privátní konstruktor, pokud chceme instance vytvářet jen přes statické metody
    private TupoHeader() {}

    // Statická metoda pro parsování řetězce hlavičky
    // Tuto metodu jsi už možná měl, upravíme ji nebo ponecháme, pokud je potřeba jinde
    public static TupoHeader parseFromString(String headerLine) {
        if (headerLine == null || headerLine.isEmpty()) {
            Log.e(TAG, "Header line is null or empty in parseFromString");
            return null;
        }

        TupoHeader header = new TupoHeader();
        Pattern pattern = Pattern.compile("TUP\\*(\\d+)%(\\d+)\\.(\\d+);(.*)");
        Matcher matcher = pattern.matcher(headerLine);

        if (matcher.find()) {
            try {
                header.Hw = Byte.parseByte(matcher.group(1));
                header.Fw = Byte.parseByte(matcher.group(2));
                header.Sub = Byte.parseByte(matcher.group(3));
                header.Comment = matcher.group(4).trim();
                return header;
            } catch (NumberFormatException e) {
                Log.e(TAG, "NumberFormatException while parsing header string: " + headerLine + " - " + e.getMessage());
                return null;
            }
        } else {
            Log.e(TAG, "Header line does not match expected format: " + headerLine);
            return null;
        }
    }

    // NOVÁ STATICKÁ METODA: Načte a parsuje hlavičku z první řádky souboru
    public static TupoHeader loadFromFile(File fwFile) {
        if (fwFile == null) {
            Log.e(TAG, "Input file is null in loadFromFile.");
            return null;
        }
        if (!fwFile.exists()) {
            Log.e(TAG, "Firmware file not found: " + fwFile.getAbsolutePath());
            // Zde bychom neměli přímo volat SendMeasure nebo měnit devState,
            // protože tato třída by neměla vědět o stavovém stroji TMSReader.
            // Volající metoda (v TMSReader) by se měla postarat o reakci na null.
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(fwFile))) {
            String firstLine = reader.readLine();
            if (firstLine != null && !firstLine.isEmpty()) {
                // Použijeme existující metodu parseFromString
                return parseFromString(firstLine);
            } else {
                Log.e(TAG, "Firmware file is empty or first line is empty: " + fwFile.getAbsolutePath());
                return null;
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException while reading firmware file: " + fwFile.getAbsolutePath() + " - " + e.getMessage());
            return null;
        }
    }

    // Můžeš přidat gettery, pokud jsou potřeba
    public byte getHw() { return Hw; }
    public byte getFw() { return Fw; }
    public byte getSub() { return Sub; }
    public String getComment() { return Comment; }

    @Override
    public String toString() {
        return "TupoHeader{" +
                "Hw=" + Hw +
                ", Fw=" + Fw +
                ", Sub=" + Sub +
                ", Comment='" + Comment + '\'' +
                '}';
    }
}
