package com.tomst.lolly.core;

import static com.tomst.lolly.LollyApplication.DIRECTORY_LOGS;
import static com.tomst.lolly.core.shared.aft;
import com.tomst.lolly.core.Log;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.util.Base64;
//import android.util.Log;

import com.tomst.lolly.LollyApplication;
import com.tomst.lolly.fileview.FileDetail;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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
        int  idx = 0;

        /*
        InputStream inputStream =
                this.context.getContentResolver().openInputStream(uri);
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(inputStream));
       */
        File file = new File(AFileName);
        InputStream inputStream = new FileInputStream(file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        Integer totalBytes = inputStream.available();  // pocet dostupnych bytu
        StringBuilder stringBuilder = new StringBuilder();

        int  iline = 0;
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
        while ((currentline = reader.readLine()) != null) {
            // odpoved z TMS
            if (currentline.contains(">>@D")==true){
                currentline = currentline.replace(">>@D","");

               pok = parser.dpacket(currentline);
               Log.e("TAG",Boolean.toString(pok));
               // state = TSimState.tRespond;
            }
            // hlidej si nastaveni adresy, pro kontrolu na konci bloku
            // <<@S=$06D5D8
            else if (currentline.startsWith("<<@S=")){
              String pom  = aft(currentline,"@S=");

              //DecodeTmsFormat.CheckAddress = Integer.parseInt(pom.substring(1),16);
              int i = Integer.parseInt(pom.substring(1),16);
              DecodeTmsFormat.SetSafeAddress(i);
              // parser.CheckAddress =  Integer.parseInt(pom);
            }

            // tohle jsem posilal do TMS
            // odvysilam do home formy teploty a vsechno co se da
            else if (currentline.startsWith(">>")){
               // state = TSimState.tWfc;
            }

            iline++;

           // DoProgress(iline);
        }
    return true;
    }


}
