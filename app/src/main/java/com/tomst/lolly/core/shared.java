package com.tomst.lolly.core;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.tomst.lolly.core.Constants.DEFAULT_SERIAL_NUMBER_VALUE;
import static com.tomst.lolly.core.Constants.SERIAL_NUMBER_INDEX;
import static com.tomst.lolly.core.Constants.fMicroInter;
import static com.tomst.lolly.core.Constants.fMicroSlope;

import android.os.Build;
import androidx.annotation.RequiresApi;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class shared {


    public static String aft(String line, String splitChar){
        if (line.length()<1)
            return "";

        String [] str = line.split(splitChar);
        if (str.length >1)
            return (str[1]);
        return "";
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
        if (mvs<200)
            return TDeviceType.dUnknown;

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
}
