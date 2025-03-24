package com.tomst.lolly.core;

import static com.tomst.lolly.LollyApplication.DIRECTORY_LOGS;
import static com.tomst.lolly.core.shared.aft;

import android.annotation.SuppressLint;
import android.content.Context;

//import android.util.Log;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;

public class TMSSim {

    // smer dovnitr, ven
    public enum TSimState {
        tWfc,
        tCommand,
        tRespond
    }

    private static final String TAG = "TMSSim";
    private static final String URL = "https://tms.tomst.com/api/v1/sim";
    private static final String API = "API_KEY";
    private static final String SECRET = "";
//    private static final String AUTH = "Basic " + Base64.encodeToString((API + ":" + SECRET).getBytes(), Base64.NO_WRAP);
    private static final String CONTENT_TYPE = "application/json";
    private static final String ACCEPT = "application/json";
    private static final String USER_AGENT = "TMS Android App";
    private static final String CHARSET = "UTF-8";
    private static final int TIMEOUT = 10000;
    private static final int RETRY = 3;
    private static final int BACKOFF = 1000;
    private Context context =null;

    public TMSSim(Context context,String AFileName) {

        this.context = context;
        //this.context = LollyApplication.getInstance().getApplicationContext();
        //InputStream inputStream = getClass().getClassLoader().getResourceAsStream("testFile.txt"


        File cacheDir = new File(DIRECTORY_LOGS);
        if (cacheDir.isDirectory()) {
            File[] files = cacheDir.listFiles();
            if (files == null || files.length == 0)
                return;

            for (File file : files) {
                if (file != null) {
                    if (file.length() == 0) {
                        Log.d(TAG, "File " + file.getName() + " is empty.");
                        continue;
                    }

                    if (file.getPath().contains("log_")==false){
                       // Log.d(TAG, "File " + file.getName() + " is not a log file.");
                        continue;
                    }


                    //Uri fileUri = Uri.fromFile(file);  // Convert File to Uri
                    try {
                        LoadCommand(file.getPath());
                        //LoadCommand(fileUri);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    @SuppressLint("NewApi")
    public boolean LoadCommand(String AFileName) throws IOException{
        //int  idx = 0;
        int fAddr=0;
        File file = new File(AFileName);
        InputStream inputStream = new FileInputStream(file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        Integer totalBytes = inputStream.available();  // pocet dostupnych bytu
        StringBuilder stringBuilder = new StringBuilder();


        // cas prvniho mereni
       // FileDetail fileDetail = new FileDetail(uri.getLastPathSegment());
        String currentline = reader.readLine();
        if (currentline == null)
            return false;

        String respond = "";
        DecodeTmsFormat parser = new DecodeTmsFormat();
        // DecodeTmsFormat.SetHandler(datahandler);
        // DecodeTmsFormat.SetDeviceType(rfir.DeviceType);
        //DoProgress(-totalBytes);  // vynuluj progress bar
        String s = "";
        TMSRec rec = new TMSRec();
        TSimState state = TSimState.tWfc;
        boolean pok = false;
        LocalDateTime fDate= null;

        boolean nextLineIsReadData = false;
        int  iline = 1;
        while ((currentline = reader.readLine()) != null) {
            // jsou to data
            iline++;

            if (currentline.contains("<<D")==true){
                nextLineIsReadData = true;
                //iline++;
                continue;
            }

            currentline = currentline.replace(">>","");

            if (nextLineIsReadData ==true)
            {
               nextLineIsReadData = false;
               //currentline = currentline.replace(">>","");

               pok = parser.dpack(fAddr,currentline);
               if (pok == false){
                   Log.e("PAR",String.format("%s, i=[%d] err: %s",AFileName,iline,currentline));
                   continue;
               }
               fAddr =parser.GetActAddr();
               // Log.e("TAG",Boolean.toString(pok));
               // state = TSimState.tRespond;
            }

            // hlidej si nastaveni adresy, pro kontrolu na konci bloku
            // <<@S=$06D5D8
            else if (currentline.startsWith("@S=")){
              String pom  = aft(currentline,"@S=");
              int i = Integer.parseInt(pom.substring(1),16);
              DecodeTmsFormat.SetSafeAddress(i);
              fAddr = i;
              // parser.CheckAddress =  Integer.parseInt(pom);
            }

            //
            else if (currentline.startsWith("@ =")){
               // state = TSimState.tWfc;
               String line = aft(currentline,"@ ");
               if (shared.ParseHeader(line) == false){
                   Log.e("TAG",String.format("ParseHeader err: %s",line));
                   continue;
               }
               parser.SetDeviceType(shared.rfir.DeviceType);
            }

            else if (currentline.startsWith("@S"))
            {
                //@E=$000FB8;&;&93%01.80#91190110
                if (currentline.length() > 1) {
                    fAddr = shared.getaddr(currentline);
                    DecodeTmsFormat.SetSafeAddress(fAddr);
                    //rState = TMSReader.TReadState.rsReadPacket;
                }
            }
           // iline++;
            // DoProgress(iline);
        }
    return true;
    }


}
