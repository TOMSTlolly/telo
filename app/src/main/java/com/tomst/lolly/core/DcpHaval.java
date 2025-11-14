package com.tomst.lolly.core;

import java.util.Arrays;
import java.nio.charset.StandardCharsets;

/**
 * Abstraktní základní třída simulující TDCP_hash z Delphi.
 */
abstract class DcpHash {
    protected boolean fInitialized = false;
    public abstract void init();
    public abstract void burn();
    public abstract void update(byte[] buffer, int offset, int size);
    public abstract void doFinal(byte[] digest, int digestOff); // 'final' je klíčové slovo v Javě

    /**
     * Pomocná metoda pro SelfTest (náhrada za UpdateStr(AnsiString(...))).
     */
    public void updateStr(String s) {
        // AnsiString v Delphi odpovídá ISO-8859-1 (Latin-1)
        byte[] data = s.getBytes(StandardCharsets.ISO_8859_1);
        update(data, 0, data.length);
    }

    /**
     * Pomocná metoda pro SelfTest (náhrada za CompareMem).
     */
    public static boolean compareMem(byte[] a, byte[] b) {
        return Arrays.equals(a, b);
    }
}


/**
 * Překlad Delphi DCPcrypt implementace Haval do Javy.
 * Původní autor: David Barton (crypto@cityinthesky.co.uk)
 *
 * Plně funkční verze s přeloženým jádrem z 'DCPhaval5.inc'.
 * Konfigurace: 5 průchodů, 256-bitový hash.
 */
public class DcpHaval extends DcpHash {

    // --- Konstanty z {$DEFINE} ---
    private static final int HASH_SIZE_BITS = 256;
    private static final int HASH_SIZE_BYTES = 256 / 8;
    private static final int PASSES = 5;

    // --- Atributy třídy (náhrada za 'protected' v Delphi) ---
    private long totalBitsProcessed;
    private int index;
    private int[] currentHash = new int[8];
    private byte[] hashBuffer = new byte[128]; // 1024 bitů

    /**
     * Konstruktor
     */
    public DcpHaval() {
        super();
        burn();
    }

    // --- Statické metody (náhrada za 'class function') ---

    public static int getId() {
        return 3; // DCP_haval (předpokládaná hodnota z DCPconst)
    }

    public static String getAlgorithm() {
        // Hodnoty na základě {$DEFINE}
        return "Haval (256bit, 5 passes)";
    }

    public static int getHashSize() {
        // Hodnota na základě {$DEFINE DIGEST256}
        return HASH_SIZE_BITS;
    }

    /**
     * Překlad metody SelfTest z Delphi.
     */
    public static boolean selfTest() {
        // Tento blok odpovídá {$IFDEF PASS5} a {$IFDEF DIGEST256}
        final byte[] test1Out = {
                (byte) 0x1A, (byte) 0x1D, (byte) 0xC8, (byte) 0x09, (byte) 0x9B, (byte) 0xDA, (byte) 0xA7, (byte) 0xF3,
                (byte) 0x5B, (byte) 0x4D, (byte) 0xA4, (byte) 0xE8, (byte) 0x05, (byte) 0xF1, (byte) 0xA2, (byte) 0x8F,
                (byte) 0xEE, (byte) 0x90, (byte) 0x9D, (byte) 0x8D, (byte) 0xEE, (byte) 0x92, (byte) 0x01, (byte) 0x98,
                (byte) 0x18, (byte) 0x5C, (byte) 0xBC, (byte) 0xAE, (byte) 0xD8, (byte) 0xA1, (byte) 0x0A, (byte) 0x8D
        };
        final byte[] test2Out = {
                (byte) 0xC5, (byte) 0x64, (byte) 0x7F, (byte) 0xC6, (byte) 0xC1, (byte) 0x87, (byte) 0x7F, (byte) 0xFF,
                (byte) 0x96, (byte) 0x74, (byte) 0x2F, (byte) 0x27, (byte) 0xE9, (byte) 0x26, (byte) 0x6B, (byte) 0x68,
                (byte) 0x74, (byte) 0x89, (byte) 0x4F, (byte) 0x41, (byte) 0xA0, (byte) 0x8F, (byte) 0x59, (byte) 0x13,
                (byte) 0x03, (byte) 0x3D, (byte) 0x9D, (byte) 0x53, (byte) 0x2A, (byte) 0xED, (byte) 0xDB, (byte) 0x39
        };

        System.out.println("Spouštím Haval SelfTest (256-bit, 5 průchodů)...");
        DcpHaval testHash = new DcpHaval();
        byte[] testOut = new byte[HASH_SIZE_BYTES];

        // Test 1
        testHash.init();
        testHash.updateStr("abcdefghijklmnopqrstuvwxyz");
        testHash.doFinal(testOut, 0);
        boolean result = DcpHash.compareMem(testOut, test1Out);
        if (!result) {
            System.out.println("  Test 1 SELHAL!");
            return false;
        }

        // Test 2
        testHash.init();
        testHash.updateStr("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789");
        testHash.doFinal(testOut, 0);
        result = result && DcpHash.compareMem(testOut, test2Out);
        if (!result) {
            System.out.println("  Test 2 SELHAL!");
            return false;
        }

        System.out.println("  Všechny testy prošly.");
        return result;
    }

    // --- Implementace metod ---

    @Override
    public void init() {
        burn();
        // Počáteční hodnoty hash (konstanty z FIPS-81)
        currentHash[0] = 0x243F6A88;
        currentHash[1] = 0x85A308D3;
        currentHash[2] = 0x13198A2E;
        currentHash[3] = 0x03707344;
        currentHash[4] = 0xA4093822;
        currentHash[5] = 0x299F31D0;
        currentHash[6] = 0x082EFA98;
        currentHash[7] = 0xEC4E6C89;
        fInitialized = true;
    }

    @Override
    public void burn() {
        totalBitsProcessed = 0;
        index = 0;
        Arrays.fill(hashBuffer, (byte) 0);
        Arrays.fill(currentHash, 0);
        fInitialized = false;
    }

    @Override
    public void update(byte[] buffer, int offset, int size) {
        if (!fInitialized) {
            throw new IllegalStateException("Hash není inicializován");
        }

        // Aktualizujeme celkový počet zpracovaných bitů
        totalBitsProcessed += (long) size * 8;

        int bytesToCopy;
        int currentOffset = offset;
        int remainingSize = size;

        while (remainingSize > 0) {
            int spaceLeft = hashBuffer.length - index;
            if (spaceLeft <= remainingSize) {
                // Zaplníme zbytek bufferu a zkomprimujeme
                bytesToCopy = spaceLeft;
                System.arraycopy(buffer, currentOffset, hashBuffer, index, bytesToCopy);
                remainingSize -= bytesToCopy;
                currentOffset += bytesToCopy;
                compress();
                // index se nastaví na 0 uvnitř compress() v původním kódu
                // (přesněji v Compress na konci)
                // Náš kód to ale musí udělat explicitně, protože Delphi
                // compress() resetuje 'Index' (globální pro třídu).
                index = 0;
            } else {
                // Zkopírujeme zbytek dat a skončíme
                bytesToCopy = remainingSize;
                System.arraycopy(buffer, currentOffset, hashBuffer, index, bytesToCopy);
                index += bytesToCopy;
                remainingSize = 0;
            }
        }
    }

    @Override
    public void doFinal(byte[] digest, int digestOff) {
        if (!fInitialized) {
            throw new IllegalStateException("Hash není inicializován");
        }

        // Padding: Přidáme 1 bit (0x80)
        hashBuffer[index] = (byte) 0x80;
        index++;

        // Pokud je buffer plný (nebo téměř plný), zkomprimujeme
        if (index > 118) { // 128 - 10 (prostor pro flags a délku)
            Arrays.fill(hashBuffer, index, hashBuffer.length, (byte) 0);
            compress();
            index = 0; // Nový prázdný blok
        }

        // Doplníme nulami až po pozici 118
        Arrays.fill(hashBuffer, index, 118, (byte) 0);

        // Nastavíme finální blok (flags a délka)
        // Podle {$DEFINE PASS5} a {$DEFINE DIGEST256}
        // HashBuffer[118]:= ((256 and 3) shl 6) or (5 shl 3) or 1;
        //   ((0) shl 6) | (40) | 1 = 41 (0x29)
        hashBuffer[118] = (byte) 0x29;
        // HashBuffer[119]:= (256 shr 2) and $FF;
        //   64 & 0xFF = 64 (0x40)
        hashBuffer[119] = (byte) 0x40;

        // Zapíšeme 64-bitovou délku zprávy v bitech (Little-Endian)
        int lenLo = (int) (totalBitsProcessed & 0xFFFFFFFFL);
        int lenHi = (int) (totalBitsProcessed >>> 32);
        intToLittleEndian(lenLo, hashBuffer, 120);
        intToLittleEndian(lenHi, hashBuffer, 124);

        // Finální komprese
        compress();

        // Zkopírujeme výsledek.
        // Pro 256-bit hash se jen přesune 8 DWordů (Little-Endian).
        for (int i = 0; i < 8; i++) {
            intToLittleEndian(currentHash[i], digest, digestOff + i * 4);
        }

        burn(); // Vyčistíme citlivá data
    }


    // --- Jádro algoritmu ---

    /**
     * Kompresní funkce.
     * Toto je přímý překlad z 'DCPhaval5.inc'.
     */
    private void compress() {
        int t0, t1, t2, t3, t4, t5, t6, t7;
        int temp;
        int[] w = new int[32];

        // 1. Převedeme 128B buffer na 32x 32-bit intů (Little-Endian)
        for (int i = 0; i < 32; i++) {
            w[i] = littleEndianToInt(hashBuffer, i * 4);
        }

        // 2. Načteme aktuální stav hashe
        t0 = currentHash[0];
        t1 = currentHash[1];
        t2 = currentHash[2];
        t3 = currentHash[3];
        t4 = currentHash[4];
        t5 = currentHash[5];
        t6 = currentHash[6];
        t7 = currentHash[7];

        // 3. VLASTNÍ KOMPRESE (Překlad DCPhaval5.inc)
        // Delphi (x shr A) or (x shl B) je Integer.rotateRight(x, A) v Javě
        // Delphi 'and' je '&', 'xor' je '^', 'not' je '~'

        // --- PASS 1 ---
        temp = (t2 & (t6 ^ t1) ^ (t5 & t4) ^ (t0 & t3) ^ t6);
        t7 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t7, 11) + w[ 0];
        temp = (t1 & (t5 ^ t0) ^ (t4 & t3) ^ (t7 & t2) ^ t5);
        t6 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t6, 11) + w[ 1];
        temp = (t0 & (t4 ^ t7) ^ (t3 & t2) ^ (t6 & t1) ^ t4);
        t5 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t5, 11) + w[ 2];
        temp = (t7 & (t3 ^ t6) ^ (t2 & t1) ^ (t5 & t0) ^ t3);
        t4 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t4, 11) + w[ 3];
        temp = (t6 & (t2 ^ t5) ^ (t1 & t0) ^ (t4 & t7) ^ t2);
        t3 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t3, 11) + w[ 4];
        temp = (t5 & (t1 ^ t4) ^ (t0 & t7) ^ (t3 & t6) ^ t1);
        t2 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t2, 11) + w[ 5];
        temp = (t4 & (t0 ^ t3) ^ (t7 & t6) ^ (t2 & t5) ^ t0);
        t1 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t1, 11) + w[ 6];
        temp = (t3 & (t7 ^ t2) ^ (t6 & t5) ^ (t1 & t4) ^ t7);
        t0 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t0, 11) + w[ 7];

        temp = (t2 & (t6 ^ t1) ^ (t5 & t4) ^ (t0 & t3) ^ t6);
        t7 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t7, 11) + w[ 8];
        temp = (t1 & (t5 ^ t0) ^ (t4 & t3) ^ (t7 & t2) ^ t5);
        t6 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t6, 11) + w[ 9];
        temp = (t0 & (t4 ^ t7) ^ (t3 & t2) ^ (t6 & t1) ^ t4);
        t5 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t5, 11) + w[10];
        temp = (t7 & (t3 ^ t6) ^ (t2 & t1) ^ (t5 & t0) ^ t3);
        t4 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t4, 11) + w[11];
        temp = (t6 & (t2 ^ t5) ^ (t1 & t0) ^ (t4 & t7) ^ t2);
        t3 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t3, 11) + w[12];
        temp = (t5 & (t1 ^ t4) ^ (t0 & t7) ^ (t3 & t6) ^ t1);
        t2 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t2, 11) + w[13];
        temp = (t4 & (t0 ^ t3) ^ (t7 & t6) ^ (t2 & t5) ^ t0);
        t1 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t1, 11) + w[14];
        temp = (t3 & (t7 ^ t2) ^ (t6 & t5) ^ (t1 & t4) ^ t7);
        t0 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t0, 11) + w[15];

        temp = (t2 & (t6 ^ t1) ^ (t5 & t4) ^ (t0 & t3) ^ t6);
        t7 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t7, 11) + w[16];
        temp = (t1 & (t5 ^ t0) ^ (t4 & t3) ^ (t7 & t2) ^ t5);
        t6 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t6, 11) + w[17];
        temp = (t0 & (t4 ^ t7) ^ (t3 & t2) ^ (t6 & t1) ^ t4);
        t5 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t5, 11) + w[18];
        temp = (t7 & (t3 ^ t6) ^ (t2 & t1) ^ (t5 & t0) ^ t3);
        t4 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t4, 11) + w[19];
        temp = (t6 & (t2 ^ t5) ^ (t1 & t0) ^ (t4 & t7) ^ t2);
        t3 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t3, 11) + w[20];
        temp = (t5 & (t1 ^ t4) ^ (t0 & t7) ^ (t3 & t6) ^ t1);
        t2 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t2, 11) + w[21];
        temp = (t4 & (t0 ^ t3) ^ (t7 & t6) ^ (t2 & t5) ^ t0);
        t1 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t1, 11) + w[22];
        temp = (t3 & (t7 ^ t2) ^ (t6 & t5) ^ (t1 & t4) ^ t7);
        t0 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t0, 11) + w[23];

        temp = (t2 & (t6 ^ t1) ^ (t5 & t4) ^ (t0 & t3) ^ t6);
        t7 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t7, 11) + w[24];
        temp = (t1 & (t5 ^ t0) ^ (t4 & t3) ^ (t7 & t2) ^ t5);
        t6 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t6, 11) + w[25];
        temp = (t0 & (t4 ^ t7) ^ (t3 & t2) ^ (t6 & t1) ^ t4);
        t5 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t5, 11) + w[26];
        temp = (t7 & (t3 ^ t6) ^ (t2 & t1) ^ (t5 & t0) ^ t3);
        t4 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t4, 11) + w[27];
        temp = (t6 & (t2 ^ t5) ^ (t1 & t0) ^ (t4 & t7) ^ t2);
        t3 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t3, 11) + w[28];
        temp = (t5 & (t1 ^ t4) ^ (t0 & t7) ^ (t3 & t6) ^ t1);
        t2 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t2, 11) + w[29];
        temp = (t4 & (t0 ^ t3) ^ (t7 & t6) ^ (t2 & t5) ^ t0);
        t1 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t1, 11) + w[30];
        temp = (t3 & (t7 ^ t2) ^ (t6 & t5) ^ (t1 & t4) ^ t7);
        t0 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t0, 11) + w[31];

        // --- PASS 2 ---
        temp = (t3 & ((t4 & ~t0) ^ (t1 & t2) ^ t6 ^ t5) ^ (t1 & (t4 ^ t2)) ^ (t0 & t2) ^ t5);
        t7 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t7, 11) + w[ 5] + 0x452821E6;
        temp = (t2 & ((t3 & ~t7) ^ (t0 & t1) ^ t5 ^ t4) ^ (t0 & (t3 ^ t1)) ^ (t7 & t1) ^ t4);
        t6 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t6, 11) + w[14] + 0x38D01377;
        temp = (t1 & ((t2 & ~t6) ^ (t7 & t0) ^ t4 ^ t3) ^ (t7 & (t2 ^ t0)) ^ (t6 & t0) ^ t3);
        t5 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t5, 11) + w[26] + 0xBE5466CF;
        temp = (t0 & ((t1 & ~t5) ^ (t6 & t7) ^ t3 ^ t2) ^ (t6 & (t1 ^ t7)) ^ (t5 & t7) ^ t2);
        t4 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t4, 11) + w[18] + 0x34E90C6C;
        temp = (t7 & ((t0 & ~t4) ^ (t5 & t6) ^ t2 ^ t1) ^ (t5 & (t0 ^ t6)) ^ (t4 & t6) ^ t1);
        t3 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t3, 11) + w[11] + 0xC0AC29B7;
        temp = (t6 & ((t7 & ~t3) ^ (t4 & t5) ^ t1 ^ t0) ^ (t4 & (t7 ^ t5)) ^ (t3 & t5) ^ t0);
        t2 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t2, 11) + w[28] + 0xC97C50DD;
        temp = (t5 & ((t6 & ~t2) ^ (t3 & t4) ^ t0 ^ t7) ^ (t3 & (t6 ^ t4)) ^ (t2 & t4) ^ t7);
        t1 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t1, 11) + w[ 7] + 0x3F84D5B5;
        temp = (t4 & ((t5 & ~t1) ^ (t2 & t3) ^ t7 ^ t6) ^ (t2 & (t5 ^ t3)) ^ (t1 & t3) ^ t6);
        t0 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t0, 11) + w[16] + 0xB5470917;

        temp = (t3 & ((t4 & ~t0) ^ (t1 & t2) ^ t6 ^ t5) ^ (t1 & (t4 ^ t2)) ^ (t0 & t2) ^ t5);
        t7 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t7, 11) + w[ 0] + 0x9216D5D9;
        temp = (t2 & ((t3 & ~t7) ^ (t0 & t1) ^ t5 ^ t4) ^ (t0 & (t3 ^ t1)) ^ (t7 & t1) ^ t4);
        t6 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t6, 11) + w[23] + 0x8979FB1B;
        temp = (t1 & ((t2 & ~t6) ^ (t7 & t0) ^ t4 ^ t3) ^ (t7 & (t2 ^ t0)) ^ (t6 & t0) ^ t3);
        t5 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t5, 11) + w[20] + 0xD1310BA6;
        temp = (t0 & ((t1 & ~t5) ^ (t6 & t7) ^ t3 ^ t2) ^ (t6 & (t1 ^ t7)) ^ (t5 & t7) ^ t2);
        t4 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t4, 11) + w[22] + 0x98DFB5AC;
        temp = (t7 & ((t0 & ~t4) ^ (t5 & t6) ^ t2 ^ t1) ^ (t5 & (t0 ^ t6)) ^ (t4 & t6) ^ t1);
        t3 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t3, 11) + w[ 1] + 0x2FFD72DB;
        temp = (t6 & ((t7 & ~t3) ^ (t4 & t5) ^ t1 ^ t0) ^ (t4 & (t7 ^ t5)) ^ (t3 & t5) ^ t0);
        t2 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t2, 11) + w[10] + 0xD01ADFB7;
        temp = (t5 & ((t6 & ~t2) ^ (t3 & t4) ^ t0 ^ t7) ^ (t3 & (t6 ^ t4)) ^ (t2 & t4) ^ t7);
        t1 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t1, 11) + w[ 4] + 0xB8E1AFED;
        temp = (t4 & ((t5 & ~t1) ^ (t2 & t3) ^ t7 ^ t6) ^ (t2 & (t5 ^ t3)) ^ (t1 & t3) ^ t6);
        t0 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t0, 11) + w[ 8] + 0x6A267E96;

        temp = (t3 & ((t4 & ~t0) ^ (t1 & t2) ^ t6 ^ t5) ^ (t1 & (t4 ^ t2)) ^ (t0 & t2) ^ t5);
        t7 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t7, 11) + w[30] + 0xBA7C9045;
        temp = (t2 & ((t3 & ~t7) ^ (t0 & t1) ^ t5 ^ t4) ^ (t0 & (t3 ^ t1)) ^ (t7 & t1) ^ t4);
        t6 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t6, 11) + w[ 3] + 0xF12C7F99;
        temp = (t1 & ((t2 & ~t6) ^ (t7 & t0) ^ t4 ^ t3) ^ (t7 & (t2 ^ t0)) ^ (t6 & t0) ^ t3);
        t5 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t5, 11) + w[21] + 0x24A19947;
        temp = (t0 & ((t1 & ~t5) ^ (t6 & t7) ^ t3 ^ t2) ^ (t6 & (t1 ^ t7)) ^ (t5 & t7) ^ t2);
        t4 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t4, 11) + w[ 9] + 0xB3916CF7;
        temp = (t7 & ((t0 & ~t4) ^ (t5 & t6) ^ t2 ^ t1) ^ (t5 & (t0 ^ t6)) ^ (t4 & t6) ^ t1);
        t3 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t3, 11) + w[17] + 0x0801F2E2;
        temp = (t6 & ((t7 & ~t3) ^ (t4 & t5) ^ t1 ^ t0) ^ (t4 & (t7 ^ t5)) ^ (t3 & t5) ^ t0);
        t2 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t2, 11) + w[24] + 0x858EFC16;
        temp = (t5 & ((t6 & ~t2) ^ (t3 & t4) ^ t0 ^ t7) ^ (t3 & (t6 ^ t4)) ^ (t2 & t4) ^ t7);
        t1 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t1, 11) + w[29] + 0x636920D8;
        temp = (t4 & ((t5 & ~t1) ^ (t2 & t3) ^ t7 ^ t6) ^ (t2 & (t5 ^ t3)) ^ (t1 & t3) ^ t6);
        t0 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t0, 11) + w[ 6] + 0x71574E69;

        temp = (t3 & ((t4 & ~t0) ^ (t1 & t2) ^ t6 ^ t5) ^ (t1 & (t4 ^ t2)) ^ (t0 & t2) ^ t5);
        t7 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t7, 11) + w[19] + 0xA458FEA3;
        temp = (t2 & ((t3 & ~t7) ^ (t0 & t1) ^ t5 ^ t4) ^ (t0 & (t3 ^ t1)) ^ (t7 & t1) ^ t4);
        t6 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t6, 11) + w[12] + 0xF4933D7E;
        temp = (t1 & ((t2 & ~t6) ^ (t7 & t0) ^ t4 ^ t3) ^ (t7 & (t2 ^ t0)) ^ (t6 & t0) ^ t3);
        t5 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t5, 11) + w[15] + 0x0D95748F;
        temp = (t0 & ((t1 & ~t5) ^ (t6 & t7) ^ t3 ^ t2) ^ (t6 & (t1 ^ t7)) ^ (t5 & t7) ^ t2);
        t4 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t4, 11) + w[13] + 0x728EB658;
        temp = (t7 & ((t0 & ~t4) ^ (t5 & t6) ^ t2 ^ t1) ^ (t5 & (t0 ^ t6)) ^ (t4 & t6) ^ t1);
        t3 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t3, 11) + w[ 2] + 0x718BCD58;
        temp = (t6 & ((t7 & ~t3) ^ (t4 & t5) ^ t1 ^ t0) ^ (t4 & (t7 ^ t5)) ^ (t3 & t5) ^ t0);
        t2 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t2, 11) + w[25] + 0x82154AEE;
        temp = (t5 & ((t6 & ~t2) ^ (t3 & t4) ^ t0 ^ t7) ^ (t3 & (t6 ^ t4)) ^ (t2 & t4) ^ t7);
        t1 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t1, 11) + w[31] + 0x7B54A41D;
        temp = (t4 & ((t5 & ~t1) ^ (t2 & t3) ^ t7 ^ t6) ^ (t2 & (t5 ^ t3)) ^ (t1 & t3) ^ t6);
        t0 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t0, 11) + w[27] + 0xC25A59B5;

        // --- PASS 3 ---
        temp = (t4 & ((t1 & t3) ^ t2 ^ t5) ^ (t1 & t0) ^ (t3 & t6) ^ t5);
        t7 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t7, 11) + w[19] + 0x9C30D539;
        temp = (t3 & ((t0 & t2) ^ t1 ^ t4) ^ (t0 & t7) ^ (t2 & t5) ^ t4);
        t6 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t6, 11) + w[ 9] + 0x2AF26013;
        temp = (t2 & ((t7 & t1) ^ t0 ^ t3) ^ (t7 & t6) ^ (t1 & t4) ^ t3);
        t5 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t5, 11) + w[ 4] + 0xC5D1B023;
        temp = (t1 & ((t6 & t0) ^ t7 ^ t2) ^ (t6 & t5) ^ (t0 & t3) ^ t2);
        t4 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t4, 11) + w[20] + 0x286085F0;
        temp = (t0 & ((t5 & t7) ^ t6 ^ t1) ^ (t5 & t4) ^ (t7 & t2) ^ t1);
        t3 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t3, 11) + w[28] + 0xCA417918;
        temp = (t7 & ((t4 & t6) ^ t5 ^ t0) ^ (t4 & t3) ^ (t6 & t1) ^ t0);
        t2 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t2, 11) + w[17] + 0xB8DB38EF;
        temp = (t6 & ((t3 & t5) ^ t4 ^ t7) ^ (t3 & t2) ^ (t5 & t0) ^ t7);
        t1 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t1, 11) + w[ 8] + 0x8E79DCB0;
        temp = (t5 & ((t2 & t4) ^ t3 ^ t6) ^ (t2 & t1) ^ (t4 & t7) ^ t6);
        t0 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t0, 11) + w[22] + 0x603A180E;

        temp = (t4 & ((t1 & t3) ^ t2 ^ t5) ^ (t1 & t0) ^ (t3 & t6) ^ t5);
        t7 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t7, 11) + w[29] + 0x6C9E0E8B;
        temp = (t3 & ((t0 & t2) ^ t1 ^ t4) ^ (t0 & t7) ^ (t2 & t5) ^ t4);
        t6 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t6, 11) + w[14] + 0xB01E8A3E;
        temp = (t2 & ((t7 & t1) ^ t0 ^ t3) ^ (t7 & t6) ^ (t1 & t4) ^ t3);
        t5 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t5, 11) + w[25] + 0xD71577C1;
        temp = (t1 & ((t6 & t0) ^ t7 ^ t2) ^ (t6 & t5) ^ (t0 & t3) ^ t2);
        t4 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t4, 11) + w[12] + 0xBD314B27;
        temp = (t0 & ((t5 & t7) ^ t6 ^ t1) ^ (t5 & t4) ^ (t7 & t2) ^ t1);
        t3 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t3, 11) + w[24] + 0x78AF2FDA;
        temp = (t7 & ((t4 & t6) ^ t5 ^ t0) ^ (t4 & t3) ^ (t6 & t1) ^ t0);
        t2 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t2, 11) + w[30] + 0x55605C60;
        temp = (t6 & ((t3 & t5) ^ t4 ^ t7) ^ (t3 & t2) ^ (t5 & t0) ^ t7);
        t1 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t1, 11) + w[16] + 0xE65525F3;
        temp = (t5 & ((t2 & t4) ^ t3 ^ t6) ^ (t2 & t1) ^ (t4 & t7) ^ t6);
        t0 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t0, 11) + w[26] + 0xAA55AB94;

        temp = (t4 & ((t1 & t3) ^ t2 ^ t5) ^ (t1 & t0) ^ (t3 & t6) ^ t5);
        t7 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t7, 11) + w[31] + 0x57489862;
        temp = (t3 & ((t0 & t2) ^ t1 ^ t4) ^ (t0 & t7) ^ (t2 & t5) ^ t4);
        t6 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t6, 11) + w[15] + 0x63E81440;
        temp = (t2 & ((t7 & t1) ^ t0 ^ t3) ^ (t7 & t6) ^ (t1 & t4) ^ t3);
        t5 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t5, 11) + w[ 7] + 0x55CA396A;
        temp = (t1 & ((t6 & t0) ^ t7 ^ t2) ^ (t6 & t5) ^ (t0 & t3) ^ t2);
        t4 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t4, 11) + w[ 3] + 0x2AAB10B6;
        temp = (t0 & ((t5 & t7) ^ t6 ^ t1) ^ (t5 & t4) ^ (t7 & t2) ^ t1);
        t3 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t3, 11) + w[ 1] + 0xB4CC5C34;
        temp = (t7 & ((t4 & t6) ^ t5 ^ t0) ^ (t4 & t3) ^ (t6 & t1) ^ t0);
        t2 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t2, 11) + w[ 0] + 0x1141E8CE;
        temp = (t6 & ((t3 & t5) ^ t4 ^ t7) ^ (t3 & t2) ^ (t5 & t0) ^ t7);
        t1 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t1, 11) + w[18] + 0xA15486AF;
        temp = (t5 & ((t2 & t4) ^ t3 ^ t6) ^ (t2 & t1) ^ (t4 & t7) ^ t6);
        t0 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t0, 11) + w[27] + 0x7C72E993;

        temp = (t4 & ((t1 & t3) ^ t2 ^ t5) ^ (t1 & t0) ^ (t3 & t6) ^ t5);
        t7 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t7, 11) + w[13] + 0xB3EE1411;
        temp = (t3 & ((t0 & t2) ^ t1 ^ t4) ^ (t0 & t7) ^ (t2 & t5) ^ t4);
        t6 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t6, 11) + w[ 6] + 0x636FBC2A;
        temp = (t2 & ((t7 & t1) ^ t0 ^ t3) ^ (t7 & t6) ^ (t1 & t4) ^ t3);
        t5 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t5, 11) + w[21] + 0x2BA9C55D;
        temp = (t1 & ((t6 & t0) ^ t7 ^ t2) ^ (t6 & t5) ^ (t0 & t3) ^ t2);
        t4 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t4, 11) + w[10] + 0x741831F6;
        temp = (t0 & ((t5 & t7) ^ t6 ^ t1) ^ (t5 & t4) ^ (t7 & t2) ^ t1);
        t3 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t3, 11) + w[23] + 0xCE5C3E16;
        temp = (t7 & ((t4 & t6) ^ t5 ^ t0) ^ (t4 & t3) ^ (t6 & t1) ^ t0);
        t2 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t2, 11) + w[11] + 0x9B87931E;
        temp = (t6 & ((t3 & t5) ^ t4 ^ t7) ^ (t3 & t2) ^ (t5 & t0) ^ t7);
        t1 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t1, 11) + w[ 5] + 0xAFD6BA33;
        temp = (t5 & ((t2 & t4) ^ t3 ^ t6) ^ (t2 & t1) ^ (t4 & t7) ^ t6);
        t0 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t0, 11) + w[ 2] + 0x6C24CF5C;

        // --- PASS 4 ---
        temp = (t3 & ((t5 & ~t0) ^ (t2 & ~t1) ^ t4 ^ t1 ^ t6) ^ (t2 & ((t4 & t0) ^ t5 ^ t1)) ^ (t0 & t1) ^ t6);
        t7 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t7, 11) + w[24] + 0x7A325381;
        temp = (t2 & ((t4 & ~t7) ^ (t1 & ~t0) ^ t3 ^ t0 ^ t5) ^ (t1 & ((t3 & t7) ^ t4 ^ t0)) ^ (t7 & t0) ^ t5);
        t6 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t6, 11) + w[ 4] + 0x28958677;
        temp = (t1 & ((t3 & ~t6) ^ (t0 & ~t7) ^ t2 ^ t7 ^ t4) ^ (t0 & ((t2 & t6) ^ t3 ^ t7)) ^ (t6 & t7) ^ t4);
        t5 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t5, 11) + w[ 0] + 0x3B8F4898;
        temp = (t0 & ((t2 & ~t5) ^ (t7 & ~t6) ^ t1 ^ t6 ^ t3) ^ (t7 & ((t1 & t5) ^ t2 ^ t6)) ^ (t5 & t6) ^ t3);
        t4 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t4, 11) + w[14] + 0x6B4BB9AF;
        temp = (t7 & ((t1 & ~t4) ^ (t6 & ~t5) ^ t0 ^ t5 ^ t2) ^ (t6 & ((t0 & t4) ^ t1 ^ t5)) ^ (t4 & t5) ^ t2);
        t3 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t3, 11) + w[ 2] + 0xC4BFE81B;
        temp = (t6 & ((t0 & ~t3) ^ (t5 & ~t4) ^ t7 ^ t4 ^ t1) ^ (t5 & ((t7 & t3) ^ t0 ^ t4)) ^ (t3 & t4) ^ t1);
        t2 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t2, 11) + w[ 7] + 0x66282193;
        temp = (t5 & ((t7 & ~t2) ^ (t4 & ~t3) ^ t6 ^ t3 ^ t0) ^ (t4 & ((t6 & t2) ^ t7 ^ t3)) ^ (t2 & t3) ^ t0);
        t1 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t1, 11) + w[28] + 0x61D809CC;
        temp = (t4 & ((t6 & ~t1) ^ (t3 & ~t2) ^ t5 ^ t2 ^ t7) ^ (t3 & ((t5 & t1) ^ t6 ^ t2)) ^ (t1 & t2) ^ t7);
        t0 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t0, 11) + w[23] + 0xFB21A991;

        temp = (t3 & ((t5 & ~t0) ^ (t2 & ~t1) ^ t4 ^ t1 ^ t6) ^ (t2 & ((t4 & t0) ^ t5 ^ t1)) ^ (t0 & t1) ^ t6);
        t7 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t7, 11) + w[26] + 0x487CAC60;
        temp = (t2 & ((t4 & ~t7) ^ (t1 & ~t0) ^ t3 ^ t0 ^ t5) ^ (t1 & ((t3 & t7) ^ t4 ^ t0)) ^ (t7 & t0) ^ t5);
        t6 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t6, 11) + w[ 6] + 0x5DEC8032;
        temp = (t1 & ((t3 & ~t6) ^ (t0 & ~t7) ^ t2 ^ t7 ^ t4) ^ (t0 & ((t2 & t6) ^ t3 ^ t7)) ^ (t6 & t7) ^ t4);
        t5 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t5, 11) + w[30] + 0xEF845D5D;
        temp = (t0 & ((t2 & ~t5) ^ (t7 & ~t6) ^ t1 ^ t6 ^ t3) ^ (t7 & ((t1 & t5) ^ t2 ^ t6)) ^ (t5 & t6) ^ t3);
        t4 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t4, 11) + w[20] + 0xE98575B1;
        temp = (t7 & ((t1 & ~t4) ^ (t6 & ~t5) ^ t0 ^ t5 ^ t2) ^ (t6 & ((t0 & t4) ^ t1 ^ t5)) ^ (t4 & t5) ^ t2);
        t3 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t3, 11) + w[18] + 0xDC262302;
        temp = (t6 & ((t0 & ~t3) ^ (t5 & ~t4) ^ t7 ^ t4 ^ t1) ^ (t5 & ((t7 & t3) ^ t0 ^ t4)) ^ (t3 & t4) ^ t1);
        t2 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t2, 11) + w[25] + 0xEB651B88;
        temp = (t5 & ((t7 & ~t2) ^ (t4 & ~t3) ^ t6 ^ t3 ^ t0) ^ (t4 & ((t6 & t2) ^ t7 ^ t3)) ^ (t2 & t3) ^ t0);
        t1 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t1, 11) + w[19] + 0x23893E81;
        temp = (t4 & ((t6 & ~t1) ^ (t3 & ~t2) ^ t5 ^ t2 ^ t7) ^ (t3 & ((t5 & t1) ^ t6 ^ t2)) ^ (t1 & t2) ^ t7);
        t0 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t0, 11) + w[ 3] + 0xD396ACC5;

        temp = (t3 & ((t5 & ~t0) ^ (t2 & ~t1) ^ t4 ^ t1 ^ t6) ^ (t2 & ((t4 & t0) ^ t5 ^ t1)) ^ (t0 & t1) ^ t6);
        t7 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t7, 11) + w[22] + 0x0F6D6FF3;
        temp = (t2 & ((t4 & ~t7) ^ (t1 & ~t0) ^ t3 ^ t0 ^ t5) ^ (t1 & ((t3 & t7) ^ t4 ^ t0)) ^ (t7 & t0) ^ t5);
        t6 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t6, 11) + w[11] + 0x83F44239;
        temp = (t1 & ((t3 & ~t6) ^ (t0 & ~t7) ^ t2 ^ t7 ^ t4) ^ (t0 & ((t2 & t6) ^ t3 ^ t7)) ^ (t6 & t7) ^ t4);
        t5 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t5, 11) + w[31] + 0x2E0B4482;
        temp = (t0 & ((t2 & ~t5) ^ (t7 & ~t6) ^ t1 ^ t6 ^ t3) ^ (t7 & ((t1 & t5) ^ t2 ^ t6)) ^ (t5 & t6) ^ t3);
        t4 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t4, 11) + w[21] + 0xA4842004;
        temp = (t7 & ((t1 & ~t4) ^ (t6 & ~t5) ^ t0 ^ t5 ^ t2) ^ (t6 & ((t0 & t4) ^ t1 ^ t5)) ^ (t4 & t5) ^ t2);
        t3 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t3, 11) + w[ 8] + 0x69C8F04A;
        temp = (t6 & ((t0 & ~t3) ^ (t5 & ~t4) ^ t7 ^ t4 ^ t1) ^ (t5 & ((t7 & t3) ^ t0 ^ t4)) ^ (t3 & t4) ^ t1);
        t2 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t2, 11) + w[27] + 0x9E1F9B5E;
        temp = (t5 & ((t7 & ~t2) ^ (t4 & ~t3) ^ t6 ^ t3 ^ t0) ^ (t4 & ((t6 & t2) ^ t7 ^ t3)) ^ (t2 & t3) ^ t0);
        t1 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t1, 11) + w[12] + 0x21C66842;
        temp = (t4 & ((t6 & ~t1) ^ (t3 & ~t2) ^ t5 ^ t2 ^ t7) ^ (t3 & ((t5 & t1) ^ t6 ^ t2)) ^ (t1 & t2) ^ t7);
        t0 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t0, 11) + w[ 9] + 0xF6E96C9A;

        temp = (t3 & ((t5 & ~t0) ^ (t2 & ~t1) ^ t4 ^ t1 ^ t6) ^ (t2 & ((t4 & t0) ^ t5 ^ t1)) ^ (t0 & t1) ^ t6);
        t7 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t7, 11) + w[ 1] + 0x670C9C61;
        temp = (t2 & ((t4 & ~t7) ^ (t1 & ~t0) ^ t3 ^ t0 ^ t5) ^ (t1 & ((t3 & t7) ^ t4 ^ t0)) ^ (t7 & t0) ^ t5);
        t6 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t6, 11) + w[29] + 0xABD388F0;
        temp = (t1 & ((t3 & ~t6) ^ (t0 & ~t7) ^ t2 ^ t7 ^ t4) ^ (t0 & ((t2 & t6) ^ t3 ^ t7)) ^ (t6 & t7) ^ t4);
        t5 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t5, 11) + w[ 5] + 0x6A51A0D2;
        temp = (t0 & ((t2 & ~t5) ^ (t7 & ~t6) ^ t1 ^ t6 ^ t3) ^ (t7 & ((t1 & t5) ^ t2 ^ t6)) ^ (t5 & t6) ^ t3);
        t4 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t4, 11) + w[15] + 0xD8542F68;
        temp = (t7 & ((t1 & ~t4) ^ (t6 & ~t5) ^ t0 ^ t5 ^ t2) ^ (t6 & ((t0 & t4) ^ t1 ^ t5)) ^ (t4 & t5) ^ t2);
        t3 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t3, 11) + w[17] + 0x960FA728;
        temp = (t6 & ((t0 & ~t3) ^ (t5 & ~t4) ^ t7 ^ t4 ^ t1) ^ (t5 & ((t7 & t3) ^ t0 ^ t4)) ^ (t3 & t4) ^ t1);
        t2 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t2, 11) + w[10] + 0xAB5133A3;
        temp = (t5 & ((t7 & ~t2) ^ (t4 & ~t3) ^ t6 ^ t3 ^ t0) ^ (t4 & ((t6 & t2) ^ t7 ^ t3)) ^ (t2 & t3) ^ t0);
        t1 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t1, 11) + w[16] + 0x6EEF0B6C;
        temp = (t4 & ((t6 & ~t1) ^ (t3 & ~t2) ^ t5 ^ t2 ^ t7) ^ (t3 & ((t5 & t1) ^ t6 ^ t2)) ^ (t1 & t2) ^ t7);
        t0 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t0, 11) + w[13] + 0x137A3BE4;

        // --- PASS 5 ---
        temp = (t1 & ((t3 & t4 & t6) ^ ~t5) ^ (t3 & t0) ^ (t4 & t5) ^ (t6 & t2));
        t7 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t7, 11) + w[27] + 0xBA3BF050;
        temp = (t0 & ((t2 & t3 & t5) ^ ~t4) ^ (t2 & t7) ^ (t3 & t4) ^ (t5 & t1));
        t6 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t6, 11) + w[ 3] + 0x7EFB2A98;
        temp = (t7 & ((t1 & t2 & t4) ^ ~t3) ^ (t1 & t6) ^ (t2 & t3) ^ (t4 & t0));
        t5 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t5, 11) + w[21] + 0xA1F1651D;
        temp = (t6 & ((t0 & t1 & t3) ^ ~t2) ^ (t0 & t5) ^ (t1 & t2) ^ (t3 & t7));
        t4 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t4, 11) + w[26] + 0x39AF0176;
        temp = (t5 & ((t7 & t0 & t2) ^ ~t1) ^ (t7 & t4) ^ (t0 & t1) ^ (t2 & t6));
        t3 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t3, 11) + w[17] + 0x66CA593E;
        temp = (t4 & ((t6 & t7 & t1) ^ ~t0) ^ (t6 & t3) ^ (t7 & t0) ^ (t1 & t5));
        t2 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t2, 11) + w[11] + 0x82430E88;
        temp = (t3 & ((t5 & t6 & t0) ^ ~t7) ^ (t5 & t2) ^ (t6 & t7) ^ (t0 & t4));
        t1 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t1, 11) + w[20] + 0x8CEE8619;
        temp = (t2 & ((t4 & t5 & t7) ^ ~t6) ^ (t4 & t1) ^ (t5 & t6) ^ (t7 & t3));
        t0 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t0, 11) + w[29] + 0x456F9FB4;

        temp = (t1 & ((t3 & t4 & t6) ^ ~t5) ^ (t3 & t0) ^ (t4 & t5) ^ (t6 & t2));
        t7 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t7, 11) + w[19] + 0x7D84A5C3;
        temp = (t0 & ((t2 & t3 & t5) ^ ~t4) ^ (t2 & t7) ^ (t3 & t4) ^ (t5 & t1));
        t6 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t6, 11) + w[ 0] + 0x3B8B5EBE;
        temp = (t7 & ((t1 & t2 & t4) ^ ~t3) ^ (t1 & t6) ^ (t2 & t3) ^ (t4 & t0));
        t5 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t5, 11) + w[12] + 0xE06F75D8;
        temp = (t6 & ((t0 & t1 & t3) ^ ~t2) ^ (t0 & t5) ^ (t1 & t2) ^ (t3 & t7));
        t4 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t4, 11) + w[ 7] + 0x85C12073;
        temp = (t5 & ((t7 & t0 & t2) ^ ~t1) ^ (t7 & t4) ^ (t0 & t1) ^ (t2 & t6));
        t3 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t3, 11) + w[13] + 0x401A449F;
        temp = (t4 & ((t6 & t7 & t1) ^ ~t0) ^ (t6 & t3) ^ (t7 & t0) ^ (t1 & t5));
        t2 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t2, 11) + w[ 8] + 0x56C16AA6;
        temp = (t3 & ((t5 & t6 & t0) ^ ~t7) ^ (t5 & t2) ^ (t6 & t7) ^ (t0 & t4));
        t1 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t1, 11) + w[31] + 0x4ED3AA62;
        temp = (t2 & ((t4 & t5 & t7) ^ ~t6) ^ (t4 & t1) ^ (t5 & t6) ^ (t7 & t3));
        t0 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t0, 11) + w[10] + 0x363F7706;

        temp = (t1 & ((t3 & t4 & t6) ^ ~t5) ^ (t3 & t0) ^ (t4 & t5) ^ (t6 & t2));
        t7 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t7, 11) + w[ 5] + 0x1BFEDF72;
        temp = (t0 & ((t2 & t3 & t5) ^ ~t4) ^ (t2 & t7) ^ (t3 & t4) ^ (t5 & t1));
        t6 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t6, 11) + w[ 9] + 0x429B023D;
        temp = (t7 & ((t1 & t2 & t4) ^ ~t3) ^ (t1 & t6) ^ (t2 & t3) ^ (t4 & t0));
        t5 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t5, 11) + w[14] + 0x37D0D724;
        temp = (t6 & ((t0 & t1 & t3) ^ ~t2) ^ (t0 & t5) ^ (t1 & t2) ^ (t3 & t7));
        t4 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t4, 11) + w[30] + 0xD00A1248;
        temp = (t5 & ((t7 & t0 & t2) ^ ~t1) ^ (t7 & t4) ^ (t0 & t1) ^ (t2 & t6));
        t3 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t3, 11) + w[18] + 0xDB0FEAD3;
        temp = (t4 & ((t6 & t7 & t1) ^ ~t0) ^ (t6 & t3) ^ (t7 & t0) ^ (t1 & t5));
        t2 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t2, 11) + w[ 6] + 0x49F1C09B;
        temp = (t3 & ((t5 & t6 & t0) ^ ~t7) ^ (t5 & t2) ^ (t6 & t7) ^ (t0 & t4));
        t1 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t1, 11) + w[28] + 0x075372C9;
        temp = (t2 & ((t4 & t5 & t7) ^ ~t6) ^ (t4 & t1) ^ (t5 & t6) ^ (t7 & t3));
        t0 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t0, 11) + w[24] + 0x80991B7B;

        temp = (t1 & ((t3 & t4 & t6) ^ ~t5) ^ (t3 & t0) ^ (t4 & t5) ^ (t6 & t2));
        t7 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t7, 11) + w[ 2] + 0x25D479D8;
        temp = (t0 & ((t2 & t3 & t5) ^ ~t4) ^ (t2 & t7) ^ (t3 & t4) ^ (t5 & t1));
        t6 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t6, 11) + w[23] + 0xF6E8DEF7;
        temp = (t7 & ((t1 & t2 & t4) ^ ~t3) ^ (t1 & t6) ^ (t2 & t3) ^ (t4 & t0));
        t5 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t5, 11) + w[16] + 0xE3FE501A;
        temp = (t6 & ((t0 & t1 & t3) ^ ~t2) ^ (t0 & t5) ^ (t1 & t2) ^ (t3 & t7));
        t4 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t4, 11) + w[22] + 0xB6794C3B;
        temp = (t5 & ((t7 & t0 & t2) ^ ~t1) ^ (t7 & t4) ^ (t0 & t1) ^ (t2 & t6));
        t3 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t3, 11) + w[ 4] + 0x976CE0BD;
        temp = (t4 & ((t6 & t7 & t1) ^ ~t0) ^ (t6 & t3) ^ (t7 & t0) ^ (t1 & t5));
        t2 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t2, 11) + w[ 1] + 0x04C006BA;
        temp = (t3 & ((t5 & t6 & t0) ^ ~t7) ^ (t5 & t2) ^ (t6 & t7) ^ (t0 & t4));
        t1 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t1, 11) + w[25] + 0xC1A94FB6;
        temp = (t2 & ((t4 & t5 & t7) ^ ~t6) ^ (t4 & t1) ^ (t5 & t6) ^ (t7 & t3));
        t0 = Integer.rotateRight(temp, 7) + Integer.rotateRight(t0, 11) + w[15] + 0x409F60C4;

        // --- Konec překladu ---

        // 4. Přičteme výsledek zpět k hash (sčítání je modulo 2^32)
        currentHash[0] += t0;
        currentHash[1] += t1;
        currentHash[2] += t2;
        currentHash[3] += t3;
        currentHash[4] += t4;
        currentHash[5] += t5;
        currentHash[6] += t6;
        currentHash[7] += t7;

        // 5. Vyčištění dočasných polí
        Arrays.fill(w, 0);

        // Nulování indexu a bufferu probíhá vně (v Update a Final)
        // V Delphi kódu je 'Index:= 0' a 'FillChar(HashBuffer...)'
        // na konci Compress. V naší implementaci je to logičtější
        // v Update() a Final(), ale pro přesnou shodu s Delphi
        // to přidáme i sem.
        index = 0;
        Arrays.fill(hashBuffer, (byte) 0);
    }

    // --- Pomocné metody pro Endianitu ---

    /**
     * Přečte 4 bajty z pole jako 32-bitový int (Little-Endian).
     */
    private int littleEndianToInt(byte[] b, int offset) {
        return (b[offset] & 0xFF) |
                ((b[offset + 1] & 0xFF) << 8) |
                ((b[offset + 2] & 0xFF) << 16) |
                ((b[offset + 3] & 0xFF) << 24);
    }

    /**
     * Zapíše 32-bitový int do 4 bajtů v poli (Little-Endian).
     */
    private void intToLittleEndian(int value, byte[] dest, int offset) {
        dest[offset] = (byte) (value & 0xFF);
        dest[offset + 1] = (byte) ((value >>> 8) & 0xFF);
        dest[offset + 2] = (byte) ((value >>> 16) & 0xFF);
        dest[offset + 3] = (byte) ((value >>> 24) & 0xFF);
    }


    /**
     * Hlavní metoda pro spuštění SelfTestu.
     */
    public static void main(String[] args) {
        DcpHaval.selfTest();
    }
}
