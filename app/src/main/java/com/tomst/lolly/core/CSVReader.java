package com.tomst.lolly.core;

 import android.content.Context;
 import android.database.Cursor;
 import android.net.Uri;
 import android.os.Build;
 import android.os.Handler;
 import android.os.Looper;
 import android.os.Message;
 import android.provider.DocumentsContract;
 import android.provider.MediaStore;
 import android.util.Log;

 import androidx.annotation.RequiresApi;
 import androidx.core.content.FileProvider;
 import androidx.documentfile.provider.DocumentFile;

 import java.io.BufferedInputStream;
 import java.io.BufferedReader;
 import java.io.DataInputStream;
 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.io.RandomAccessFile;
 import java.time.format.DateTimeFormatter;

 import java.nio.file.Files;
 import java.nio.file.Path;
 import java.nio.file.Paths;

 import java.io.FileOutputStream;

 import java.time.LocalDateTime;
 import java.util.Locale;

 import com.tomst.lolly.BuildConfig;
 import com.tomst.lolly.LollyApplication;
 import com.tomst.lolly.fileview.FileDetail;
import com.tomst.lolly.core.Constants;

 import org.apache.commons.io.input.RandomAccessFileInputStream;

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
    private static final byte iLine=0;
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
   private  DocumentFile privateDocumentDir;
   private  DocumentFile documentFile;

    public static class FileUtils {
        public static File copyDocumentFileToTempFile(Context context, DocumentFile documentFile) throws IOException {
            File tempFile = File.createTempFile("temp", null, context.getCacheDir());
            try (InputStream inputStream = context.getContentResolver().openInputStream(documentFile.getUri());
                 FileOutputStream outputStream = new FileOutputStream(tempFile)) {

                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }

                /*
                public static ByteArrayInputStream readStreamToSeekableStream(InputStream inputStream) throws IOException {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        byteArrayOutputStream.write(buffer, 0, bytesRead);
                    }
                    return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                }
                */
            }
            return tempFile;
        }
    }

    public String getRealPathFromUri(Context context, Uri uri) {
        String filePath = "";
        if (DocumentsContract.isDocumentUri(context, uri)) {
            String documentId = DocumentsContract.getDocumentId(uri);
            String[] split = documentId.split(":");
            String type = split[0];

            Uri contentUri = MediaStore.Files.getContentUri(type);
            String selection = MediaStore.Files.FileColumns._ID + "=?";
            String[] selectionArgs = new String[] { split[1] };

            Cursor cursor = context.getContentResolver().query(contentUri, null, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
                filePath = cursor.getString(columnIndex);
                cursor.close();
            }
        }
        return filePath;
    }


    public String getRealPathFromUri(Uri uri) {
        String filePath = "";
        String[] projection = { MediaStore.Images.Media.DATA };
       // Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        Cursor cursor = LollyApplication.getInstance().getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            filePath = cursor.getString(columnIndex);
            cursor.close();
        }
        return filePath;
    }

    public String getPathFromUri(Context context, Uri uri) {
        String filePath = "";
        if (DocumentsContract.isDocumentUri(context, uri)) {
            DocumentFile documentFile = DocumentFile.fromSingleUri(context, uri);
            filePath = documentFile.getUri().getPath();
        }
        return filePath;
    }

    public void readN(DocumentFile docFile, int n)
    {
        try {
            Context context = LollyApplication.getInstance().getApplicationContext();
            File tempFile = FileUtils.copyDocumentFileToTempFile(context, docFile);
            RandomAccessFile randomAccessFile = new RandomAccessFile(tempFile, "r");

            // Use the RandomAccessFile as needed
            randomAccessFile.seek(100);
            byte[] buffer = new byte[1024];
            int bytesRead = randomAccessFile.read(buffer);
            //System.out.write(buffer, 0, bytesRead);
            Log.d(TAG, "readN: " + bytesRead);
            randomAccessFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void OpenForRead(String AFileName){
        this.context = LollyApplication.getInstance().getApplicationContext();
       // String AFileName = this.FileName;
        try {
            if (AFileName.startsWith("content")) {
                Uri uri = Uri.parse(AFileName);
                documentFile = DocumentFile.fromSingleUri(this.context, uri);;
                privateDocumentDir = DocumentFile.fromTreeUri(this.context, uri);
            } else {
                File file = new File(AFileName);
                if (!file.exists()) {
                    Log.w(TAG, "[#] CSVReader.java - UNABLE TO FIND THE FILE");
                    return;
                }
                privateDocumentDir = DocumentFile.fromFile(file);
            }

            /*
            InputStream fin;
            try {
                fin = LollyApplication.getInstance().getContentResolver().openInputStream(privateDocumentDir.getUri());
            } catch (FileNotFoundException e) {
                Log.w("myApp", "[#] CSVReader.java - FileNotFoundException");
                return;
            }
             */
        }catch (Exception e) {
            e.printStackTrace();
            return ;
        }

        /*
        try
        {
            fNewCsv = new FileOutputStream(AFileName);
        }
        catch(Exception e)
        {
            System.out.println(e);
        }
        this.fout  = null;
        Log.d(TAG,"New CSV file name = " +AFileName);
        */

    }


    private void ClearPrivate()
    {
        currDay = 0;
        iline = 0;
        currIx = 1;
    }
    // constructor
    public CSVReader(String AFileName)
    {
        this.FileName = AFileName;
        ClearPrivate();
        ClearAvg();
    }

    public CSVReader()
    {
        this.context = LollyApplication.getInstance().getApplicationContext();
        this.FileName = null;
       ClearPrivate();
       ClearAvg();
    }


    public static void printLastNLines(String filePath, int n) {
        File file = new File(filePath);
        StringBuilder builder = new StringBuilder();
        try {
            RandomAccessFile randomAccessFile = new RandomAccessFile(filePath, "r");
            long pos = file.length() - 1;
            randomAccessFile.seek(pos);

            for (long i = pos - 1; i >= 0; i--) {
                randomAccessFile.seek(i);
                char c = (char) randomAccessFile.read();
                if (c == '\n') {
                    n--;
                    if (n == 0) {
                        break;
                    }
                }
                builder.append(c);
            }
            builder.reverse();
            System.out.println(builder.toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /*
    public class RandomAccessFileExample {
            File file = new File("path/to/your/file.txt");
            try (RandomAccessFileInputStream rafis = new RandomAccessFileInputStream(file)) {
                byte[] buffer = new byte[1024];
                int bytesRead;

                // Read the file content
                while ((bytesRead = rafis.read(buffer)) != -1) {
                    System.out.write(buffer, 0, bytesRead);
                }

                // Seek to a specific position
                rafis.seek(100);
                bytesRead = rafis.read(buffer);
                System.out.write(buffer, 0, bytesRead);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
*/

    public void readL(DocumentFile docFile)
    {
        InputStream fin;
        try {
            fin  = LollyApplication.getInstance().getContentResolver().openInputStream(docFile.getUri());
        } catch (FileNotFoundException e) {
            Log.w("myApp", "[#] EGM96.java - FileNotFoundException");
            //Toast.makeText(getApplicationContext(), "Oops", Toast.LENGTH_SHORT).show();
            //e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return;
        }
        BufferedInputStream bin = new BufferedInputStream(fin);
        DataInputStream din = new DataInputStream(bin);

        if (docFile.getUri().getPath() == null)
            return;;
    }



    // tohle pustim po startu
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void run()
    {
        try
        {
            Looper.prepare();
            if (this.FileName.contains(".csv") ){
            openCsv(this.FileName);
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

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
        if (handler==null)
            return;

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


    private void FindMinMax(TMereni Mer)
    {
        if (Mer.t1 > maxT1)
            maxT1 = Mer.t1;
        if (Mer.t2 > maxT2)
            maxT2 = Mer.t2;
        if (Mer.t3 > maxT3)
            maxT3 = Mer.t3;
        if (Mer.t1 < minT1)
            minT1 = Mer.t1;
        if (Mer.t2 < minT2)
            minT2 = Mer.t2;
        if (Mer.t3 < minT3)
            minT3 = Mer.t3;
        if (Mer.hum > maxHm)
            maxHm = Mer.hum;
        if (Mer.hum < minHm)
            minHm = Mer.hum;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void ProcessLine(String currentline) {
        String T1, T2, T3;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DMD_PATTERN);
        LocalDateTime dateTime = null;

        if (!currentline.isEmpty()) {
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

                Mer.idx = Integer.parseInt(str[iLine]);

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
                    Mer.dev = GuessDevice(Mer);


            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    // nacte prvni a posledni radek ze souboru
    @RequiresApi(api = Build.VERSION_CODES.O)
    public FileDetail FirstLast(Uri uri) throws IOException {
        FileDetail fdet = new FileDetail(uri.getLastPathSegment());     // nastavim rovnou jmeno souboru
        DocumentFile docFile = DocumentFile.fromSingleUri(context, uri);
        File tempFile = FileUtils.copyDocumentFileToTempFile(context, docFile);
        RandomAccessFile raf = new RandomAccessFile(tempFile, "r");
        // extrahuj seriove cislo z jmena souboru
        String serialNumber = shared.getSerialNumberFromFileName(uri.getLastPathSegment());

        // prvni radek
        String s = raf.readLine();
        ProcessLine(s);
        fdet .setFrom(Mer.dtm);

        // posledni radek
        long fileLength = raf.length() - 1;
        StringBuilder sb = new StringBuilder();
        for (long pointer = fileLength; pointer >= 0; pointer--) {
            raf.seek(pointer);
            char c = (char) raf.read();
            if (c == '\n' && sb.length() > 0) {
                break;
            }
            sb.append(c);
        }
        s= sb.reverse().toString();
        ProcessLine(s);
        fdet.setInto(Mer.dtm);
        fdet.setCount(Mer.idx);
        return fdet;
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public FileDetail readFileContent(Uri uri) throws IOException
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
        // cas prvniho mereni
        FileDetail fileDetail = new FileDetail(uri.getLastPathSegment());
        String currentline = reader.readLine();
        ProcessLine(currentline);
        fileDetail.setFrom(Mer.dtm);

        while ((currentline = reader.readLine()) != null)
        {
            //  vysledek je v lokalni promenne Mer
            ProcessLine(currentline);
            Mer.idx = ++iline;
            idx = j - inputStream.available();

           // AppendStat(Mer);
            FindMinMax(Mer);
            sendMessage(Mer);
            if (progressBarHandler != null)
                DoProgress(idx);

            stringBuilder.append(currentline).append("\n");
        }
        DoFinished(0);
        inputStream.close();

        fileDetail.setInto(Mer.dtm);
        fileDetail.setCount(Mer.idx);
        fileDetail.setMaxT1(maxT1);
        fileDetail.setMinT1(minT1);
        fileDetail.setMaxT2(maxT2);
        fileDetail.setMinT2(minT2);
        fileDetail.setMaxT3(maxT3);
        fileDetail.setMinT3(minT3);
        fileDetail.setMaxHum(maxHm);
        fileDetail.setMinHum(minHm);

        //return stringBuilder.toString();
        return fileDetail;
    }






}