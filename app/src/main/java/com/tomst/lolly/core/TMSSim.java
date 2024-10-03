package com.tomst.lolly.core;

import static com.tomst.lolly.LollyApplication.DIRECTORY_LOGS;
import static com.tomst.lolly.core.shared.aft;
import static com.tomst.lolly.core.shared.between;
i
import android.content.Context;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import com.tomst.lolly.LollyApplication;
import com.tomst.lolly.fileview.FileDetail;


import java.io.BufferedReader;
import java.io.File;
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
    private static final String AUTH = "Basic " + Base64.encodeToString((API + ":" + SECRET).getBytes(), Base64.NO_WRAP);
    private static final String CONTENT_TYPE = "application/json";
    private static final String ACCEPT = "application/json";
    private static final String USER_AGENT = "TMS Android App";
    private static final String CHARSET = "UTF-8";
    private static final int TIMEOUT = 10000;
    private static final int RETRY = 3;
    private static final int BACKOFF = 1000;
    private Context context =null;

    public TMSSim(String AFileName) {
        this.context = LollyApplication.getInstance().getApplicationContext();

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

                    Uri fileUri = Uri.fromFile(file);  // Convert File to Uri
                    try {
                        LoadCommand(fileUri);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }




    public boolean LoadCommand(Uri uri) throws IOException{
            int  idx = 0;

            // Streamovane vycteni, zatim nejrychlejsi verze
            InputStream inputStream =
                    this.context.getContentResolver().openInputStream(uri);
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(inputStream));
            Integer totalBytes = inputStream.available();  // pocet dostupnych bytu
            StringBuilder stringBuilder = new StringBuilder();

            int  iline = 0;
            // cas prvniho mereni
            FileDetail fileDetail = new FileDetail(uri.getLastPathSegment());
            String currentline = reader.readLine();
            if (currentline == null)
                return false;

            //DoProgress(-totalBytes);  // vynuluj progress bar
            TMSRec rec = new TMSRec();
            TSimState state = TSimState.tWfc;
            while ((currentline = reader.readLine()) != null) {

                //  je to smer dovnitr

                else if (currentline.startsWith("<<@D")) {
                   // vyber odpoved z D
                   rec.sRsp  = between(currentline, "<< ", " <<").trim();
                    state = TSimState.tRespond;
                }

                switch (state) {
                    case tWfc:
                        if (currentline.startsWith(">> D")) {
                            state = TSimState.tCommand;
                        }

                    case tCommand:
                        rec.sCmd = aft(currentline, ">> ").trim(); // nechci mezery
                        // zpracuj prikaz
                        break;
                    case tRespond:
                        // zpracuj odpoved
                        break;
                }

                iline++;

               // DoProgress(iline);
            }
        return true;
    }


}
