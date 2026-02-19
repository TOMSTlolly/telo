package com.tomst.lolly.core;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.tomst.lolly.core.Constants.DEFAULT_SERIAL_NUMBER_VALUE;
import static com.tomst.lolly.core.Constants.SERIAL_NUMBER_INDEX;
import static com.tomst.lolly.core.Constants.fMicroInter;
import static com.tomst.lolly.core.Constants.fMicroSlope;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.documentfile.provider.DocumentFile;

import com.tomst.lolly.LollyActivity;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;



public class shared {

    public static RFirmware rfir;

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static  String CompileFileName(String Prefix,String Serial, String ADir){
        Context context = LollyActivity.getInstance().getApplicationContext();

        Uri treeUri = Uri.parse(ADir);
        DocumentFile targetDirectory = DocumentFile.fromTreeUri(context, treeUri);

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd");
        String timestamp = now.format(formatter);

        // Vytvoření základního názvu, např. "data_92221411_2023_10_26_14_30_05"
        String baseFileName = Prefix  + Serial + "_" + timestamp;

        int index = 0;
        String finalFileName = baseFileName + ".csv";
        // Kontrola existence souboru - dokud nachází shodu, zvyšuje index
        while (targetDirectory.findFile(finalFileName) != null) {
            index++;
            finalFileName = baseFileName + "_" + index + ".csv";
            Log.w("CompileFileName", "File already exists, generating new name: " + finalFileName);
        }

        DocumentFile newFile = targetDirectory.createFile("text/csv", finalFileName);

        if (newFile != null) {
            Log.i("CompileFileName", "Successfully created file: " + newFile.getUri().toString());
            return newFile.getUri().toString();
        } else {
            Log.e("CompileFileName", "Failed to create file in the target directory.");
            return "";
        }


    }

    /**
     * Extracts the folder name starting from the encoded uri.
     *
     * @param uriPath The encoded URI path
     * @return the path of the folder
     */
    public static String extractFolderNameFromEncodedUri(String uriPath) {
        String spath = Uri.decode(uriPath);
        String pathSeparator = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) ? ":" : "/";
        if (spath.contains(pathSeparator)) {
            String[] spathParts = spath.split(pathSeparator);
            return spathParts[spathParts.length - 1];
        } else return spath;
    }


    public static String aft(String line, String splitChar){
        if (line.length()<1)
            return "";

        String [] str = line.split(splitChar);
        if (str.length >1)
            return (str[1]);
        return "";
    }

    public static String bef(String line, String splitChar){
        if (line.length()<1)
            return "";

        String [] str = line.split(splitChar);
        if (str.length >1)
            return (str[0]);
        return "";
    }

    public static int getaddr(String respond)
    {
        //String s = new String("P=$05ED00");
        try {
            String[] arr = respond.split("=", 2);
            String s = arr[1].substring(1);
            s = s.replaceAll("(\\r|\\n)", "");
            int ret = Integer.parseInt(s, 16);
            return (ret);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return 0;
    }

    public static String between(String line, String s1, String s2){
        if ( (line.indexOf(s1) <0) ||  (line.indexOf(s2)<0)  )
            return "";

        String subStr = line.substring(line.indexOf(s1 ) + 1, line.indexOf(s2));
        return subStr;
    }

    public static  String getSerialNumberFromFileName(String fileName) {
        // filename should look like "data_92221411_2023_09_26_0.csv"
        // serial number should be the second value. No other way to get the serial number
        // if not in the title, just use a default value
        String[] titleSplit = fileName.split("_");
        String serialNumber;
        if (titleSplit.length > SERIAL_NUMBER_INDEX) {
            serialNumber = fileName.split("_")[SERIAL_NUMBER_INDEX];
        }
        else {
            // could theoretically add a dataset count to the end of this unknown
            // but then it becomes an issue when merging several unknown datasets together
            // would end up looking like: unknown1, unknown2, unknown1
            serialNumber = DEFAULT_SERIAL_NUMBER_VALUE;
        }

        return serialNumber;
    }
    
    public static String MeteoToString(TMeteo met){
        String ret = met.toString();
        if (ret.length()>0)
            ret = ret.substring(1);

        return ret;
    }
    public static TDeviceType MvsToDevice(int mvs){
        if (mvs == 0)
            return TDeviceType.dUnknown;

        if ((mvs>0) && (mvs<200))
            return TDeviceType.fromValue(mvs);

        mvs = mvs -200;
        TDeviceType ret = TDeviceType.fromValue(mvs);
        return (ret);
    }

    public static int ConvToMvs(TDeviceType val){
        return (val.ordinal()+200);
    }
    public static int convToMicro(int AValue){
        int ret = 0;
        if (AValue<=255){
            return(8890);
        }
        else{
            if (AValue>1279){
                ret = (int) Math.round((AValue- fMicroInter) * fMicroSlope + 0.5);
            }
        }
        return(ret);
    }

    // vraci vzdy UTC cas v zakladni zone + gmts tvar
    // pouziva se pro NASTAVENI TMS zarizeni !!!
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String getDateTime(){
        ZoneId tz = ZoneId.systemDefault();
        String offsetId = tz.getRules().getStandardOffset(Instant.now()).getId();
        int sec = tz.getRules().getStandardOffset(Instant.now()).getTotalSeconds();

        // vypis offset
        String str[] = offsetId.split(":");
        //System.out.println(str[0]);
        //System.out.println(str[1]);
        //System.out.print(sec);

        // offset beru ze systemu
        // = 7200+960; // 2 hod + 15 min
        int hod = (sec / 3600)  ;
        int min = (sec % 3600) / 60; // tady by mely byt minuty
        int gmt= 4*hod + min/15;
        String gmts = String.valueOf(gmt);
        if (gmts.length()==1){
            gmts = "0"+gmts;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd,HH:mm:ss").withZone(ZoneId.of("UTC"));
        final ZonedDateTime jobStartDateTimeZ = ZonedDateTime.now();
        Instant startTime = jobStartDateTimeZ.toInstant();
        String command =  formatter.format(startTime) + "+"+gmts;;
        // System.out.println(command);

        return command;
    }


    public static int CopyHex(String reply, int start, int count)
    {
        String s = reply.substring(start-1,start+count-1);
        int i = Integer.parseInt(s,16);
        return (i);
    }

    private static PermissionManager permissionManager;

    public static boolean checkPermission() {
        permissionManager.getPermission(READ_EXTERNAL_STORAGE, "Storage access is required", false);
        permissionManager.getPermission(WRITE_EXTERNAL_STORAGE, "Storage access is required", false);

        return
          permissionManager.havePermission(new String[]{READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE});
    }


    public static boolean ParseHeader(String line)
    {
        // @ =&93^33%01.80 TMS-A   // dendrometr
        // @ =&93^A0%01.80 TMSx1#
        boolean result = false;
        line = aft(line,"=");

        rfir = new RFirmware();


        rfir.Result = line.length()>0;
        if (!rfir.Result)
            return (false);

        try {
            // hardware
            String s = between(line, "&", "^");
            if (s.length() > 0) {
                rfir.Hw = (byte) Integer.parseInt(s);
                rfir.Striska = between(line, "^", "%");
            }

            // firmware
            int i = line.indexOf("%");
            if (i > 0) {
                s = line.substring(i + 1, i + 3);
                rfir.Fw = (byte) Integer.parseInt(s);

                s = line.substring(i + 4, i + 6);
                rfir.Sub = (byte) Integer.parseInt(s);
            }

            i = line.indexOf("TMS3");
            if (i > 1) {
                rfir.DeviceType = TDeviceType.dLolly3;
                return true;
            }

            // kdyz tu neni TMS, je hlavicka blbe
            i = line.indexOf("TMS");
            if (i < 1)
                return false;

            //1                     2
            //123456789012345678
            // &93^03%01.82 TMS-A
            // TODO : TMS-A, zkontroluj burta
            // je za TMS pomlcka ?
            // boolean ret = true;
            rfir.Result = true;
            char c = line.charAt(i + 3);
            if (c == '-') {
                c = line.charAt(i + 4);
                switch (c) {
                    case 'T':
                        rfir.DeviceType = TDeviceType.dTermoChron;
                        break;

                    case 'A':
                        rfir.DeviceType = TDeviceType.dAD;
                        break;

                    default:
                        rfir.DeviceType = TDeviceType.dUnknown;
                        rfir.Result = false;
                }
                return rfir.Result;
            }
            else if (c=='x') {
                rfir.DeviceType = TDeviceType.dLolly4;
                //return true;
                i++;
            }

            // ok bude to TMS3/TMS4
            rfir.Result = true;
            c = line.charAt(i + 3);
            switch (c) {
                case '3':
                    rfir.DeviceType = TDeviceType.dLolly3;
                    break;


                default: {
                    if (Character.isDigit(c) || (c == '#'))
                        rfir.DeviceType = TDeviceType.dLolly4;
                    else{
                        rfir.DeviceType = TDeviceType.dUnknown;
                        rfir.Result = false;
                    }
                    break;
                }
            }
            return rfir.Result;

        } catch (Exception e){
            e.printStackTrace();
   //       Log.e(TAG,e.getMessage());
            rfir.Result = false;

   //       String msg = String.format("readHeader error: %s: %s",e.getMessage(),line);
   //       SendMeasure(TDevState.tReadType, msg);
   //       SendLytics(TDevState.tReadType,msg);
            return false;
        }
    }

}
