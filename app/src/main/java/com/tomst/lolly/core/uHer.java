package com.tomst.lolly.core;

import static java.lang.System.currentTimeMillis;

import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.RequiresApi;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;
import com.tomst.lolly.LollyActivity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

//import java.nio.charset.StandardCharsets;


public class uHer extends Thread {
    // common buffer before writing
    static private byte[] fBuf;
    static private byte INBUF = 80;
    static private int WTIME = 200;
    static private int k = 0; // pozice pri cteni textovych dat
    static private StringBuilder ous = new StringBuilder();

    private String ALogName = LollyActivity.getInstance().DIRECTORY_LOGS + "/command.csv.";

    public FT_Device ftDev = null;  // ukazatel na hardware
    public boolean bReadThreadGoing = false;
    public int iavailable = 0;

    //private final int  readLength = 512; // maximalni mozna delka stringu, ktery leze z FTDI
    private int readLength = 512;  // ocekavana delka telegramu z FTDI
    private byte[] readData = new byte[readLength]; // vyctene data
    private StringBuilder sb = new StringBuilder();

    Handler logHandler;               // timhle vysilam ven z tridy

    public uHer(Handler h) {
        fBuf = new byte[INBUF];
        logHandler = h;
        this.setPriority(Thread.MIN_PRIORITY);
//        File dir = new File(DIRECTORY_TEMP);
    }

    // jmeno log souboru
    public void setLogName(String ALogX) {
        this.ALogName = LollyActivity.getInstance().DIRECTORY_LOGS + "/" + ALogX;
        File log = new File(this.ALogName);
    }

    public int getPigCnt() {
        return (k);
    }

    // vypise obsah bufferu
    private static void dumps(byte[] b) {
        String line = "";
        int j;
        for (int i = 0; i < b.length; i++) {
            j = b[i] & 0xff;
            line = line + Integer.toHexString(j) + ",";
            //System.out.println(j);
        }
        System.out.println(line);
    }

    // vycisti buffer
    private static void clear(byte[] b) {
        for (int i = 0; i < b.length; i++) {
            //b[i]=(byte)i;
            b[i] = 0;
        }
    }

    private void msleep(int milis) {
        try {
            Thread.sleep(milis);
        } catch (InterruptedException e) {
        }
    }

    private boolean EnterProgrammMode() {
        byte[] b = new byte[2];
        boolean ret;
        ret = cmdapi("1;0xFA", 1, b);
        if (!ret)
            return (false);

        // shod RTS
        ret = ftDev.clrRts();
        return (true);
    }

    private long getTickCount() {
        return (currentTimeMillis());
    }

    // cmd = "1;0xF0"; vstupem je hexa buffer zadany jako HexString, oddeleny strednikem
    private boolean writebuf(String cmd) {

        clear(fBuf);
        // rozbiju cmd retezec
        int i, j;
        i = 0;
        for (String val : cmd.split(";")) {
            // prevedu z hexa
            j = Integer.decode(val);
            fBuf[i] = (byte) (j & 0xff);
            i++;
        }
        // vypis, co zapisuju
        //  dumps(fBuf);

        // zapis prevedeny buffer
        j = ftDev.write(fBuf, i);
        return (i == j);
    }

    private boolean readbuf(byte[] b, int cnt) {
        int i=0;
        long start, diff;

        start = getTickCount();
        do {
            i = ftDev.getQueueStatus();
            diff = getTickCount() - start;
            msleep(10); // 10 ms between attempts
        } while ((i < cnt) && (diff < WTIME));

        if (i < cnt)
            return (false);

        ftDev.read(b, cnt);
        // dumps(b);
        return (true);
    }

    public boolean cmdapi(String cmd, int cnt, byte[] b) {
        boolean ret = false;
        ret = writebuf(cmd);
        if (!ret)
            return (false);

        ret = readbuf(b, cnt);
        if (!ret)
            return (false);

        //dumps(b);
        return (true);
    }

    private int platformTouch() {
        boolean ret;
        byte[] b = new byte[2];
        ret = cmdapi("1;0xC0", 1, b);
        if (!ret)
            return (-1);

        int i = b[0] & 0xFF;
        return (i);
    }

    public String getAdapter() {
        ftDev.clrDtr();
        ftDev.clrRts();
        msleep(1);
        ftDev.setRts();
        ftDev.setDtr();

        byte[] b = new byte[5];
        clear(b);

        boolean ret;
        ret = cmdapi("1;0xF0", 5, b);
        if (!ret)
            return ("");

        byte[] fid = new byte[8];
        clear(fid);

        fid[0] = 0;  // prefix ???
        fid[1] = 0;
        fid[2] = 0;
        fid[3] = b[1];
        fid[4] = b[2];
        fid[5] = b[3];
        fid[6] = b[4];

        fid[7] = calc8(fid);
        // dumps(b);

        String Adapter = "";
        for (int i = 1; i <= 6; i++) {
            Adapter = Adapter + String.format("%02X", fid[i]);
        }
        Adapter = "00-" + Adapter + "*" + String.format("%02X", fid[7]);

        // mel bych mit tohle cislo
        // 00-0000D3021105*A0
        // cmdHex na D je 4;$55;$40;$44;$0D"

        return (Adapter);
    }

    private int tuxCommand(String cmd, byte[] ou) {
        k = 0;
        if (!writebuf(cmd))
            return (-1);

        int i = readTextData(ou);
        return (i);
    }

    private enum tuxstate {
        xCMD, xASCIIDATA, xACK, xNOTIMPLEMENTED, xFINAL
    }

    private int readTextData(byte[] ou) {
        int firstbyte = 0;
        int j;

        byte[] o1 = new byte[1];
        try {

            tuxstate tux = tuxstate.xCMD;
            do {
                switch (tux) {
                    case xCMD:
                        // v i by melo byt 1 (vycten jeden byte)

                        int i = ftDev.read(ou, 1);
                        firstbyte = (byte) ou[0];

                        switch (firstbyte) {
                            case (byte) 254:
                                tux = tuxstate.xFINAL;
                                break; // timeout
                            case (byte) 0x7F:
                                tux = tuxstate.xACK;
                                break;
                            case (byte) '@':
                                tux = tuxstate.xASCIIDATA;
                                break;
                            case (byte) '8':
                                tux = tuxstate.xNOTIMPLEMENTED;
                                break;
                            case (byte) '0':
                                tux = tuxstate.xNOTIMPLEMENTED;
                                break;
                        }
                        break;

                    case xASCIIDATA:
                        // je to ASCII ?
                        boolean ret;

                        ret = (firstbyte > 1) && (firstbyte < 0x7F);
                        if (!ret)
                            return (255);

                        // boolean error = false;
                        ou[k++] = (byte) firstbyte;

                        do {
                            // ctu byte po byte a
                            j = ftDev.read(o1, 1);
                            if (j != 1)
                                return (255);

                            firstbyte = (byte) o1[0];
                            ou[k++] = (byte) firstbyte;

                            if ((byte) (firstbyte) > 0x7F)
                                return (256);

                        } while ((firstbyte != '\r') && (firstbyte != 0x7F) && (ret != false));

                        // if (firstbyte == 0)
                        tux = tuxstate.xACK;
                        break;


                    case xACK:
                        j = ftDev.read(o1, 1);
                        if (j != 1)
                            return (255);
                        firstbyte = o1[0];

                        switch (firstbyte) {
                            case 0:
                                tux = tuxstate.xFINAL;
                                break;
                            case '.':
                                tux = tuxstate.xFINAL;
                                break;
                            case 1:
                                tux = tuxstate.xCMD;
                                break;
                            case '!':
                                tux = tuxstate.xCMD;
                                break;
                            case 27:
                                tux = tuxstate.xFINAL;
                                break;
                            case (byte) 0x7F:
                                tux = tuxstate.xFINAL;
                                ou[k++] = (byte) 127;
                                break;
                            case '~':
                                tux = tuxstate.xFINAL;
                                ou[k++] = (byte) 127;
                                break;
                        }
                        break;

                    case xFINAL:
                        break;

                }

            } while (tux != tuxstate.xFINAL);

        } catch (Exception e) {
            System.out.println(e);
        }

        return (firstbyte);
    }

    private enum twstate {
        xSTART, xPESCIP, xPLATTOUCH, xPUTTEXT, xNOADAPTER, xTIMEOUT, xUX, xREPEAT,
        xSETSTATE, xFINAL, xERROR,xDEAD
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public boolean flashTux(Uri uri) {
        Path path = new File(uri.getPath()).toPath();

        // nejdriv poslat prikaz pro zahajeni flashovani
        twstate xst = twstate.xPLATTOUCH;
        int plat = 0;  // vysledek platform touche
        byte[] ou = new byte[128];
        File file = new File(uri.getPath());
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String lineFromFile = null;

            // prvni radek
            lineFromFile = reader.readLine();
            do {
                switch (xst) {
                    case xSTART:
                        // restart adapteru
                        String a = getAdapter();
                        if (!a.isEmpty()) {
                            xst = twstate.xPLATTOUCH;
                        }
                        break;

                    case xPLATTOUCH:
                        plat = platformTouch();
                        if (plat < 0)
                            break;

                        switch (plat) {
                            case 0x55:
                                xst = twstate.xPUTTEXT;
                                break;
                            case 255:
                                xst = twstate.xSTART;
                                break;
                            case 254:
                                xst = twstate.xTIMEOUT;
                                break;
                            default:
                                xst = twstate.xERROR;
                                break;
                        }
                        break;

                    case xPUTTEXT:
                        if (lineFromFile != null) {
                            // nandam radek do lizatka a pockam na odpoved
                            System.out.println("Read line: " + lineFromFile); // Or Log.d for Android
                            boolean b = flashLine(lineFromFile);
                            if (b)
                                lineFromFile = reader.readLine();
                                if (lineFromFile == null)
                                    xst = twstate.xFINAL;
                        }

                        break;
                    //
                    case xFINAL:
                        break;

                    case xTIMEOUT:
                        xst = twstate.xFINAL;
                        break;

                    case xERROR:
                        xst = twstate.xFINAL;
                        break;
                }
            } while ((xst != twstate.xFINAL));


        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private String chewLine(String ALine)
    {

        return "";
    }


    private boolean pigCommand(String cmd, byte[] ou) {
        twstate xst = twstate.xPLATTOUCH;
        int plat = 0;  // vysledek platform touche
        byte iLastByte = 0 ;
        int ib = 0; // navratovy byte z vycitani ASCII textu
        // int k = 0;  // pozice ukazatele ve vystupnim bufferu
        // --- ZMĚNA ZDE: Přidání časového limitu ---
        long startTime = System.currentTimeMillis();
        final int TIMEOUT_MS = 4000;

        do {
            // --- ZMĚNA ZDE: Kontrola časového limitu na začátku každé iterace ---
            if (System.currentTimeMillis() - startTime > TIMEOUT_MS) {
                // Log.w("pigCommand", "Timeout reached after " + TIMEOUT_MS + " ms");
                xst = twstate.xDEAD; // Vynutíme stav timeout a ukončení smyčky
            }
            // -------------------------------------------------------------------

            switch (xst) {
                case xSTART:
                    // restart adapteru
                   // Log.d("pigCmd.xStart", "xSTART: ");
                    String a = getAdapter();
                    if (!a.isEmpty()) {
                        xst = twstate.xPLATTOUCH;
                    }
                    break;

                case xPESCIP:
                   // Log.d("pigCmd", "xPESCIP: ");
                    break;

                case xPLATTOUCH:
                   // Log.d("pigCmd", "xPlatTOUCH: ");
                    plat = platformTouch();
                    if (plat < 0) {
                        xst = twstate.xDEAD;
                        break;
                    }

                    switch (plat) {
                        case 0x55:
                            xst = twstate.xUX;
                            break;
                        case 255:
                            xst = twstate.xSTART;
                            break;
                        case 254:
                            xst = twstate.xTIMEOUT;
                            break;
                        default:
                            xst = twstate.xERROR;
                            break;
                    }
                    break;

                case xUX:
                    //Log.d("pigCmd", "xUX: ");
                    // // cmdHex na D je 4;$55;$40;$44;$0D"
                    ib = tuxCommand(cmd, ou);

                    if (ib >= 0) {
                        iLastByte = (byte) (ib );
                        xst = twstate.xSETSTATE;
                    }

                case xSETSTATE:
                    //Log.d("pigCmd", "xSetState: ");
                    switch (iLastByte) {
                        case (byte) 254:
                            xst = twstate.xTIMEOUT;
                            break;
                        case (byte) 0x7F:
                            xst = twstate.xREPEAT;
                            break;
                        case (byte) 255:
                            xst = twstate.xERROR;
                            break;
                        case 1:
                            xst = twstate.xUX;
                            break;
                        case '!':
                            xst = twstate.xUX;
                            break;
                        case 0:
                            xst = twstate.xFINAL;
                            break;
                        case '.':
                            xst = twstate.xFINAL;
                            break;
                        default:
                            xst = twstate.xERROR;
                            break;
                    }
                    break;

                case xFINAL:
                    break;

                case xTIMEOUT:
                    // pozor neplest si s tim, ze mi upadl kabel od adapteru !
                    xst = twstate.xFINAL; // tohle je TIMEOUT od adapteru
                    break;

                case xERROR:
                    xst = twstate.xFINAL;
                    break;

                case xNOADAPTER:
                    xst = twstate.xFINAL;
                    break;

                case xREPEAT:
                    //Log.d("pigCmd", String.format("k = %d",k));

                    ib = readTextData(ou);
                    if (ib >= 0) {
                        iLastByte = (byte) (ib);
                        xst = twstate.xSETSTATE;
                        //Log.d("PigCommand",String.format("%d",k));
                        break;
                    }

                    xst = twstate.xFINAL;
                    break;

                default:
                    break;
            }

        } while ((xst != twstate.xFINAL) && (xst != twstate.xDEAD));
        return ( xst  == twstate.xFINAL);
    }

    // nastavim adresu prikazem S=$000001
    // zkontroluju, ze se vraci spravne nastavena adresa
    public boolean doSACH(int AValue){
        String respond = doCommand("S=$"+String.format("%06X",AValue));
        if (respond.isEmpty())
            return(false);

        //String sHexString = aft(respond, "=");
        //String sHexString = shared.getaddr(respond);
        //if (AValue !=Integer.parseInt(sHexString, 16))
         //  return(false);
        return (AValue == shared.getaddr(respond));
    }

    private void doLog(String cmd, String resp){
        String s="<<"+cmd;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ALogName, true))) {
            writer.write( cmd + "\n");
            writer.write(">>"+resp + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // posle do lizatka jeden radek, vstupem je ASCII line
    public boolean flashLine(String cmd){
        byte[] chr = new byte[cmd.length()+4]; // delka + ridici znaky + pocet znaku na zacatku
        chr[0] = (byte)(cmd.length()+3);
        chr[1] = (byte)0x55;
        chr[2] = (byte)0x40;
        for (int i =0;i<cmd.length();i++){
            chr[i+3] = (byte) cmd.charAt(i);
        }
        chr[3+cmd.length()] = (byte)0x0D;
        // poskladam do stringu
        String command = "";
        for (int i=0;i<chr.length;i++)
            command = command+String.format("0x%02X",chr[i]) +';';

        byte[] ou = new byte[2];
        boolean ret = pigCommand(command,ou);
        if (!ret)
              return(false);

        if (ou[0]==127)
          return(true);

        return(false);
    }


    public enum cmdstate {
        xFalse, xTrue, xTimeout
    }

    // Tuto třídu vložte někam dovnitř třídy uHer
    public class CommandResult {
        public final String response;
        public final cmdstate status;

        public CommandResult(String response, cmdstate status) {
            this.response = response;
            this.status = status;
        }
    }

    public CommandResult doCommandEx(String cmd){
        byte[] chr = new byte[cmd.length()+4]; // delka + ridici znaky + pocet znaku na zacatku
        chr[0] = (byte)(cmd.length()+3);
        chr[1] = (byte)0x55;
        chr[2] = (byte)0x40;
        for (int i =0;i<cmd.length();i++){
            chr[i+3] = (byte) cmd.charAt(i);
        }
        chr[3+cmd.length()] = (byte)0x0D;
        // poskladam do stringu
        String command = "";
        for (int i=0;i<chr.length;i++)
            command = command+String.format("0x%02X",chr[i]) +';';

        byte[] ou = new byte[65526];
        boolean ret = pigCommand(command,ou);
        if (!ret)
            return new CommandResult("TIMEOUT\n", cmdstate.xTimeout);

        ous.setLength(0);
        int i=0;
        if (ou[0] == 0)
            return new CommandResult("", cmdstate.xTrue);
        do {
            //ous = ous+String.format("%c",ou[i]) ; //String.format("0x%02X",ou[i]) +';';
            ous.append((char)(ou[i]));
            i++;
        } while (ou[i] !=0);

        String rts = ous.toString();
        //doLog(cmd,rts);

        SendLog(cmd,rts);

        return new CommandResult(rts, cmdstate.xTrue);
        //return(ous.toString());
    }

    // cmd prevedu na array of char a predradim TK magicke znaky
    public String doCommand(String cmd){
        byte[] chr = new byte[cmd.length()+4]; // delka + ridici znaky + pocet znaku na zacatku
        chr[0] = (byte)(cmd.length()+3);
        chr[1] = (byte)0x55;
        chr[2] = (byte)0x40;
        for (int i =0;i<cmd.length();i++){
            chr[i+3] = (byte) cmd.charAt(i);
        }
        chr[3+cmd.length()] = (byte)0x0D;
        // poskladam do stringu
        String command = "";
        for (int i=0;i<chr.length;i++)
            command = command+String.format("0x%02X",chr[i]) +';';

        byte[] ou = new byte[65526];
        boolean ret = pigCommand(command,ou);
        if (!ret)
            return("TIMEOUT\n");

        ous.setLength(0);
        int i=0;
        if (ou[0] == 0)
                return("");
        do {
            //ous = ous+String.format("%c",ou[i]) ; //String.format("0x%02X",ou[i]) +';';
            ous.append((char)(ou[i]));
            i++;
        } while (ou[i] !=0);

       String rts = ous.toString();
       //doLog(cmd,rts);

        SendLog(cmd,rts);

        return(rts);
       //return(ous.toString());
    }

    private String trimFormattingChars(String input){
        return input.replaceAll("[\\s\\t\\n\\r\u007F]+", " ").trim();
    }

    private void SendLog(String cmd, String resp){
        Message msg = logHandler.obtainMessage();

        TMSRec info = new TMSRec();
        info.sCmd = trimFormattingChars(cmd); // prikaz
        info.sRsp   = trimFormattingChars(resp);            // pozice progress baru
        msg.obj = info;
        logHandler.sendMessage(msg);
    }


    public void prepcom(){
        ftDev.setLatencyTimer((byte) 16);
        ftDev.purge((byte) (D2xxManager.FT_PURGE_TX | D2xxManager.FT_PURGE_RX));

        // nastav RTS/DTR
        ftDev.clrRts();
        ftDev.clrDtr();
        //android.os.SystemClock.sleep(1);

        try {
            Thread.sleep(50);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        ftDev.setRts();
        ftDev.setDtr();
    }

    // vraci vyctene data
    public String readString()
    {
       return sb.toString();
    }

    @Override
    public void run()
    {
        int i;

        //D2xxManager ftdid2xx
        while(true == bReadThreadGoing)
        {
            // 50 ms pred dalsim zjistovani poctu bytu ve fronte
            try {
                Thread.sleep(50);
                }
            catch (InterruptedException e) {
            }

            synchronized(ftDev)
            {
                // check the amount of available data
                iavailable = ftDev.getQueueStatus();
                if (iavailable >= readLength) {

                    bReadThreadGoing = false;

                    /*
                    if(iavailable > readLength){
                        iavailable = readLength;
                    }
                    byte[] per = new byte[] { };
                    sb.delete(0, sb.length());
                    ftDev.read(readData, iavailable);

                    for (i = 0; i < iavailable; i++) {
                        sb.append(String.format("%02X ", (byte)readData[i]));
                        //readDataToText[i] = (char) readData[i];
                    }
                    Message msg = mHandler.obtainMessage();
                    mHandler.sendMessage(msg);  // posli zpravu do handleru
                     */
                }
            }

        }
    }
    public static byte calc8(byte[] id){

        byte crc = 0 ;
        int cid = 0;

        cid = (byte)((crc ^ id[0]) & (0xFF));
        crc = (byte) CRC_TAB_8_VALUE[cid];
        for (int j=6;j>0;j--){
            cid = ((crc ^ id[j]) & (0xFF));
            crc = (byte) CRC_TAB_8_VALUE[cid];
        }
        return(crc);
    }

    private static byte crc8_tab(byte data, byte crcInit) {
        short ci = (short) (crcInit & 0xFF);
        byte crc = (byte) (CRC_TAB_8_VALUE[ci] ^ (data & 0xFF));
        return crc;
    }

    final static public byte CRC_TAB_8_VALUE[] =
            {0, (byte)94, (byte)188, (byte)226, (byte)97, (byte)63, (byte)221, (byte)131, (byte)194, (byte)156, (byte)126, (byte)32, (byte)163, (byte)253, (byte)31, (byte)65,
                    (byte)157, (byte)195, (byte)33, (byte)127, (byte)252, (byte)162, (byte)64, (byte)30, (byte)95, (byte)1, (byte)227, (byte)189, (byte)62, (byte)96, (byte)130, (byte)220,
                    (byte)35, (byte)125, (byte)159, (byte)193, (byte)66, (byte)28, (byte)254, (byte)160, (byte)225, (byte)191, (byte)93, 3, (byte)128, (byte)222, 60, 98,
                    (byte)190, (byte)224, 2, 92, (byte)223, (byte)129, 99, 61, 124, 34, (byte)192, (byte)158, 29, 67, (byte)161, (byte)255,
            70, 24, (byte)250, (byte)164, 39, 121, (byte)155, (byte)197, (byte)132, (byte)218, 56, 102, (byte)229, (byte)187, 89, 7,
                    (byte)219, (byte)133, 103, 57, (byte)186, (byte)228, 6, 88, 25, 71, (byte)165, (byte)251, 120, 38, (byte)196, (byte)154,
            101, 59, (byte)217, (byte)135, 4, 90, (byte)184, (byte)230, (byte)167, (byte)249, 27, 69, (byte)198, (byte)152, 122, 36,
                    (byte)248, (byte)166, 68, 26, (byte)153, (byte)199, 37, 123, 58, 100, (byte)134, (byte)216, 91, 5, (byte)231, (byte)185,
                    (byte)140, (byte)210, 48, 110, (byte)237, (byte)179, 81, 15, 78, 16, (byte)242, (byte)172, 47, 113, (byte)147, (byte)205,
            17, 79, (byte)173, (byte)243, 112, 46, (byte)204, (byte)146, (byte)211, (byte)141, 111, 49, (byte)178, (byte)236, 14, 80,
                    (byte)175, (byte)241, 19, 77, (byte)206, (byte)144, 114, 44, 109, 51, (byte)209, (byte)143, 12, 82, (byte)176, (byte)238,
            50, 108, (byte)142, (byte)208, 83, 13, (byte)239, (byte)177, (byte)240, (byte)174, 76, 18, (byte)145, (byte)207, 45, 115,
                    (byte)202, (byte)148, 118, 40, (byte)171, (byte)245, 23, 73, 8, 86, (byte)180, (byte)234, 105, 55, (byte)213, (byte)139,
            87, 9, (byte)235, (byte)181, 54, 104, (byte)138, (byte)212, (byte)149, (byte)203, 41, 119, (byte)244, (byte)170, 72, 22,
                    (byte)233, (byte)183, 85, 11, (byte)136, (byte)214, 52, 106, 43, 117, (byte)151, (byte)201, 74, 20, (byte)246, (byte)168,
            116, 42, (byte)200, (byte)150, 21, 75, (byte)169, (byte)247, (byte)182, (byte)232, 10, 84, (byte)215, (byte)137, 107, 53
            };
}
