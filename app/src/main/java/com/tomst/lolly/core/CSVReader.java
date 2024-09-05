package com.tomst.lolly.core;

 import android.content.Context;
 import android.net.Uri;
 import android.os.Build;
 import android.os.Handler;
 import android.os.Looper;
 import android.os.Message;
 import android.util.Log;

 import androidx.annotation.RequiresApi;
 import androidx.core.content.FileProvider;
 import java.io.BufferedReader;
 import java.io.File;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.text.SimpleDateFormat;
 import java.time.ZoneId;
 import java.time.format.DateTimeFormatter;
 import java.util.Date;

 import java.nio.file.Files;
 import java.nio.file.Path;
 import java.nio.file.Paths;

 import java.io.FileOutputStream;

 import java.time.LocalDateTime;
 import java.util.Locale;

 import com.tomst.lolly.BuildConfig;

public class CSVReader extends Thread
{
    public static  Handler handler = null;
    private static Handler progressBarHandler = new Handler(Looper.getMainLooper());  // handler pro vysilani z Threadu

    private FileOutputStream fout ;

    private FileOutputStream fNewCsv;
    private boolean writeTxf=false;

    private int iline=0;
    private static String DMD_PATTERN = "yyyy.MM.dd HH:mm";
    private static final int MAXTX = 1000;
    private static final int MINTX = -1000;

    private static final int MAXHM = 10000;
    private static final int MINHM = 0;

    // pozice v csv radku
    private static final byte iT1=3;
    private static final byte iT2=4;
    private static final byte iT3=5;

    private static final byte iHum=6;

    private static final byte iMvs=7;

    double currT1, currT2, currT3;
    double maxT1, maxT2, maxT3;
    double minT1, minT2, minT3;
    long currIx,currHm,minHm,maxHm;
    private Integer currDay;
   // private Date currDate;
    private LocalDateTime currDate;
    private final String TAG = "TOMST";

    public void SetHandler(Handler han){
        this.handler = han;
    }
    TMereni Mer = new TMereni(); // mereni

    private OnGeekEventListener mListener; // callback pro vysilani z threadu

    public OnProListener mBarListener; // listener field

    public OnProListener mFinListener; // listener field


    //private Handler progressBarHandler = new Handler();  // handler pro vysilani z Threadu

    long iFileSize = 0; // velikost souboru v bytech
    private String FileName="";

    public String getFileName(){
        return this.FileName;
    }

    // pozice progressBaru
    public void SetBarListener(OnProListener AListener){
        this.mBarListener = AListener;
    }

    public void SetFinListener(OnProListener AListener){
        this.mFinListener = AListener;
    }

    public void SetMereniListener(OnGeekEventListener AListener){
        this.mListener = AListener;
    }


    private static Context context = null;

    // csv file constructor
    public CSVReader(Context context)
    {
        this.context = context;
        this.fout  = null;
        this.writeTxf = false;

        currDay = 0;
        iline = 0;
        currIx = 1;
        ClearAvg();
    }

    // Tomasuv zkraceny format zobrazeni grafu
    public void SetTxf(boolean AWriteTxf)
    {
        this.writeTxf = AWriteTxf; // budu vytvaret txf soubor
    }

    // uloz a zavri txf
    private  void CloseTxf()
    {
        try
        {
          fout.close();
        }
        catch (IOException e)
        {
          throw new RuntimeException(e);
        }
    }


    // Txf soubor pro zkraceny zapis
    private void openTxf(String AFileName)
    {
      // vymen priponu csv -> txf
      AFileName = AFileName.replace(".csv",".txf");
      Log.d(TAG,"TxfFileName="+AFileName);
      try
      {
          fout = new FileOutputStream(AFileName);
      }
      catch(Exception e)
      {
          System.out.println(e);
      }
    }

    // novy CSV soubor
    public void CreateCsvFile(String file_name)
    {
        Log.d(TAG,"New CSV file name = " + file_name);
        try
        {
            fNewCsv = new FileOutputStream(file_name);
            // otevre AFileName, ale s priponou .txf
            if (this.writeTxf ) openTxf(file_name);

        }
        catch(Exception e)
        {
            System.out.println(e);
        }
    }

    public void setFileName(String AFileName)
    {
        this.FileName = AFileName;

        if (AFileName.contains(".txf"))
        {
            this.writeTxf = false;
            return;
        }

        // otevre pro zapis zkracenych grafu
        if (this.writeTxf) openTxf(AFileName);
    }

    // tohle pustim po startu
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void run()
    {
        try
        {
            Looper.prepare();

            if (this.FileName.contains(".csv")
                    || this.FileName.contains(".txf"))
            {
                openCsv(this.FileName);
            }

        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    // TODO: write a custom function for TXF files
    @RequiresApi(api = Build.VERSION_CODES.O)
    private Uri openCsv(String full_file_name) throws IOException
    {
        File file = new File(full_file_name);

        Log.e(TAG, full_file_name);

        return FileProvider.getUriForFile(
                this.context,
                BuildConfig.APPLICATION_ID + ".provider",
                file
        );
    }


    public long getFileSize(String fileName)
    {
        Path path = null;
        try
        {
            if (android.os.Build.VERSION.SDK_INT
                    >= android.os.Build.VERSION_CODES.O)
            {
                path = Paths.get(fileName);

                // size of a file (in bytes)
                long bytes = Files.size(path);

                return bytes;
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return 0;
    }

    private void DoProgress(long pos)
    {
        progressBarHandler.post(new Runnable()
        {
            public void run()
            {
                if (mBarListener != null)
                {
                    mBarListener.OnProEvent(pos);
                }
            }
        });
    }

    private void DoFinished(long pos)
    {
        progressBarHandler.post(new Runnable()
        {
            public void run()
            {
                if (mFinListener != null)
                {
                    mFinListener.OnProEvent(pos);
                }
            }
        });
    }


    private void sendMessage (TMereni mer)
    {
        // Handle sending message back to handler
        Message message = handler.obtainMessage();
        message.obj = new TMereni(mer);
        handler.sendMessage(message);
    }


    public void CloseExternalCsv()
    {
        try
        {
            fNewCsv.close();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public void AddMerToCsv(TMereni Mer)
    {
        try
        {
            AddToCsv(fNewCsv, Mer);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    // csv radek z TMereni
    @RequiresApi(api = Build.VERSION_CODES.O)
    private String FormatLine(TMereni Mer)
    {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DMD_PATTERN);
        String dts = Mer.dtm.format(formatter);
        String line = String.format(
                Locale.US,
                "%d;%s;%d;%.4f;%.4f;%.4f;%d;%d;%d",
                Mer.idx,dts,Mer.gtm,Mer.t1,Mer.t2,Mer.t3,Mer.hum,Mer.mvs,Mer.Err
        );

        return (line);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void AddToCsv(
            FileOutputStream AStream,
            TMereni Mer
    ) throws IOException
    {
        String line = FormatLine(Mer);
        AStream.write((line.getBytes()));
        AStream.write(13);
        AStream.write(10);
        AStream.flush();
    }

    private void ClearAvg()
    {
        currDay =0;
        currT1 = 0;
        currT2 = 0;
        currT3 = 0;
        currHm = 0;
    }

    // pridej stat
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void AppendStat(TMereni Mer)
    {
        double  t1,t2,t3;
        int hum,day;
        //Date dtm;
        LocalDateTime dtm;
        String s ;

        if (currDay == 0) {
            currDay = Mer.day;
            currDate = Mer.dtm;
        }

           if (currDay == Mer.day)  {
            currT1 = currT1 + Mer.t1;
            currT2 = currT2 + Mer.t2;
            currT3 = currT3 + Mer.t3;
            currHm = currHm + Mer.hum;
            currIx = currIx+1;
        } else {
            t1 = Mer.t1;
            t2 = Mer.t2;
            t3 = Mer.t3;
            hum =Mer.hum;
            day =Mer.day;  // nove cislo dne
            dtm =Mer.dtm;

            // odvysilej prumerne hodnoty callbackem do grafu
            Mer.t1 = currT1 / currIx;
            Mer.t2 = currT2 / currIx;
            Mer.t3 = currT3 / currIx;
            Mer.hum = (int) (currHm / currIx);
            Mer.day = currDay; // zapisuju prumer k minulemu datu

            if (currDate !=null)
              Mer.dtm = currDate;

            currDay = day;
            currDate = dtm;

            if (this.writeTxf) {
                try {
                    AddToCsv(fout,Mer);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            // prumery
            currT1 = t1;
            currT2 = t2;
            currT3 = t3;
            currHm = hum;

            // maxima
            maxT1 = MINTX;
            maxT2 = MINTX;
            maxT3 = MINTX;

            // minima
            minT1 = MAXTX;
            minT2 = MAXTX;
            minT3 = MAXTX;

            // maxima v humidite
            maxHm = MINHM;
            minHm = MAXHM;

            // counter
            currIx = 1;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void ProcessLine(String currentline)
    {
        String T1,T2,T3;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DMD_PATTERN);
        LocalDateTime dateTime = null;

        if (!currentline.isEmpty())
        {
            // rozsekej radku
            String[] str = currentline.split(";", 0);
            // datum
            try {
                //DateTimeFormatter formatter = DateTimeFormatter.ofPattern(OFFPATTERN);
                dateTime = LocalDateTime.parse(str[1], formatter);
                Mer.dtm = dateTime;
                Mer.day = dateTime.getDayOfMonth();
                if (currDay == 0) {
                    currDay = Mer.day;
                    currDate = Mer.dtm;
                }

            } catch (Exception e) {
                System.out.println(e);
            }
            // teploty
            T1 = str[iT1].replace(',', '.');//replaces all occurrences of 'a' to 'e'
            T2 = str[iT2].replace(',', '.');//replaces all occurrences of 'a' to 'e'
            T3 = str[iT3].replace(',', '.');//replaces all occurrences of 'a' to 'e'
            Mer.t1 = Float.parseFloat(T1);
            Mer.t2 = Float.parseFloat(T2);
            Mer.t3 = Float.parseFloat(T3);
            Mer.hum = Integer.parseInt(str[iHum]);
            Mer.mvs = Integer.parseInt(str[iMvs]);

            if (Mer.mvs >= 200)
               Mer.dev = shared.MvsToDevice(Mer.mvs);
            else
               Mer.dev = GuessDevice(Mer) ;

            Mer.idx = ++iline;
            if (this.writeTxf)
               AppendStat(Mer);
            else
               sendMessage(Mer);
        }
    }

    private TDeviceType GuessDevice(TMereni mer)
    {
        mer.dev = TDeviceType.dUnknown;
        if (mer.mvs == 1)
        {
            return (TDeviceType.dLolly3);
        }

        //  existuji t2,t3 teplomery
        if ((mer.t2 <-199) && (mer.t3<-199))
        {
           // wurst nebo dendrometr
           if (mer.adc>65300)
           {
               mer.dev = TDeviceType.dTermoChron;
           }
           else
           {
               mer.dev = TDeviceType.dAD;
           }
        }
        else
        {
            mer.dev = TDeviceType.dLolly4;
        }

        return (mer.dev);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private String readFileContent(Uri uri) throws IOException
    {
        Integer idx = 0;

        // Streamovane vycteni, zatim nejrychlejsi verze
        InputStream inputStream =
                this.context.getContentResolver().openInputStream(uri);
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(inputStream));
        Integer j = inputStream.available();  // pocet dostupnych bytu
        StringBuilder stringBuilder = new StringBuilder();

        currDay = 0;
        iline = 0;
        ClearAvg();

        String currentline = "";
        while ((currentline = reader.readLine()) != null)
        {
            ProcessLine(currentline);
            idx = j - inputStream.available();
            DoProgress(idx);
            stringBuilder.append(currentline).append("\n");
            idx++;
        }
        DoFinished(0);
        inputStream.close();
        if (this.writeTxf) CloseTxf();

        return stringBuilder.toString();
    }
}