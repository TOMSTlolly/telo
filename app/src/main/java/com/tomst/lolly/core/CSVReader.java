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
 import java.nio.file.attribute.FileTime;
 import java.time.Duration;
 import java.time.ZoneId;
 import java.time.format.DateTimeFormatter;
 import java.nio.file.Files;
 import java.nio.file.Path;
 import java.io.FileOutputStream;
 import java.io.OutputStream;
 import java.time.LocalDateTime;

 import com.tomst.lolly.LollyActivity;
 import com.tomst.lolly.fileview.FileDetail;

public class CSVReader extends Thread
{
    public static  Handler handler = null;
    private static Handler progressBarHandler = new Handler(Looper.getMainLooper());  // handler pro vysilani z Threadu

    private FileOutputStream fout ;
//    private FileOutputStream fNewCsv;
    private OutputStream fNewCsv;
    private boolean writeTxf=false;

    private int  oldPos=0;

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

    private static String sFmt="";  // nastav formatovani podle zarizeni

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
    TMereni Mol = new TMereni(); // stare mereni

    private OnGeekEventListener mListener; // callback pro vysilani z threadu

    private OnProListener progressListener; // listener field

    public OnProListener mFinListener; // listener field

    //private Handler progressBarHandler = new Handler();  // handler pro vysilani z Threadu

    private String FileName="";
    public String getFileName(){
        return this.FileName;
    }

    // pozice progressBaru
    public void SetProgressListener(OnProListener AListener){
        this.progressListener = AListener;
    }

    public void SetFinListener(OnProListener AListener){
        this.mFinListener = AListener;
    }

    public void SetMereniListener(OnGeekEventListener AListener){
        this.mListener = AListener;
    }

    private static FileDetail det= null;

    // posledni vycteny detail souboru
    // zodpovidam za volani fce readFileContent() predtim
    public FileDetail getFileDetail(){
        return det;
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
        Cursor cursor = LollyActivity.getInstance().getContentResolver().query(uri, projection, null, null, null);
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

    public void OpenForRead(String AFileName){
        this.context = LollyActivity.getInstance().getApplicationContext();
       // String AFileName = this.FileName;
        try {
            if (AFileName.startsWith("content")) {
                Uri uri = Uri.parse(AFileName);
                documentFile = DocumentFile.fromSingleUri(this.context, uri);;
                privateDocumentDir = DocumentFile.fromTreeUri(this.context, uri);  // no usage
            } else {
                File file = new File(AFileName);
                if (!file.exists()) {
                    Log.w(TAG, "[#] CSVReader.java - UNABLE TO FIND THE FILE");
                    return;
                }
                privateDocumentDir = DocumentFile.fromFile(file);
            }

        }catch (Exception e) {
            e.printStackTrace();
            return ;
        }
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
        super("CSVReaderThread"); // nazev vlakna pro debugger
        this.context = LollyActivity.getInstance().getApplicationContext();

        this.FileName = AFileName;
        ClearPrivate();
        ClearAvg();
    }

    public CSVReader()
    {
        super("CSVReaderThread"); // nazev vlakna pro debugger<
       this.context = LollyActivity.getInstance().getApplicationContext();
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



    public void readL(DocumentFile docFile)
    {
        InputStream fin;
        try {
            fin  = LollyActivity.getInstance().getContentResolver().openInputStream(docFile.getUri());
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

    private void showErrorDialog(String message) {
        new android.app.AlertDialog.Builder(this.context)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }


    public void OpenForWrite(String AFileName) {
        this.context = LollyActivity.getInstance().getApplicationContext();

        Uri fileUri = Uri.parse(AFileName);
        try {
            fNewCsv = context.getContentResolver().openOutputStream(fileUri);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }


    }





    // format staci nastavit jenom jednou na zacatku
    public String SetupFormat(TDeviceType dev)
    {

        switch (dev)
        {
            default:
            case dLolly3:
            case dLolly4:
                sFmt = "%d;%s;%d;%.3f;%.3f;%.3f;%d;%d;%d";
                break;
            case dAD:
            case dAdMicro:
            case dTermoChron:
                //sFmt = "%d;%s;%d;%.2f,%d;%d;%d;%d;%d;%d";
                sFmt = "%d;%s;%d;%.3f;%.0f;%.0f;%d;%d;%d";
                break;
        }
        return sFmt;
    }

    // csv radek z TMereni
    @RequiresApi(api = Build.VERSION_CODES.O)
    private String FormatLine(TMereni Mer)
    {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DMD_PATTERN);

        if (Mer.dtm==null)
            Mer.dtm = LocalDateTime.of(0,0,0,0,0);
        String dts = Mer.dtm.format(formatter);

        int hum = Mer.adc; // v defaultu ukladam vystup z prevodniku, bez prepoctu na mikrometry
        if (Mer.dev == TDeviceType.dAD)
         {
            if (Constants.showMicro) {
                Mer.mvs = TDeviceType.dAdMicro.ordinal() + Constants.MVS_OFFSET;
                hum = Mer.hum;
            }
            else
                Mer.mvs = TDeviceType.dAD.ordinal() + Constants.MVS_OFFSET;
         }
        /*
        String line = String.format(
                    Locale.US,
                    "%d;%s;%d;%.2f;%.2f;%.2f;%d;%d;%d",
                    Mer.idx, dts, Mer.gtm, Mer.t1, Mer.t2, Mer.t3, hum, Mer.mvs, Mer.Err
        );
         */
         String line = String.format(sFmt,Mer.idx, dts, Mer.gtm, Mer.t1, Mer.t2, Mer.t3, hum, Mer.mvs, Mer.Err);
        return (line);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void AddToCsv(
            OutputStream AStream,
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

        minT1 = MAXTX;
        minT2 = MAXTX;
        minT3=MAXTX;
        minHm = MAXHM;

        maxT1 = MINTX;
        maxT2 = MINTX;
        maxT3 = MINTX;
        maxHm = MINHM;
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

    private void copyMerToMol(TMereni Mer) {
        Mol.dtm = Mer.dtm;
        Mol.day = Mer.day;
        Mol.idx = Mer.idx;
        Mol.t1 = Mer.t1;
        Mol.t2 = Mer.t2;
        Mol.t3 = Mer.t3;
        Mol.hum = Mer.hum;
        Mol.mvs = Mer.mvs;
        Mol.dev = Mer.dev;
        Mol.Err = Mer.Err;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean WasHole(TMereni Mer)
    {
        // zjisti zda je mezera v datech
        if (Mol.dtm==null) {
            //Mol = Mer; // presun do starych dat
            copyMerToMol(Mer);
            return false;
        }
        Duration duration = Duration.between(Mol.dtm, Mer.dtm);
        //Mol=Mer;
        copyMerToMol(Mer);
        // rozdil > 1 hod je podezrely
        if (duration.getSeconds() > 60*60) {
            Mer.Err = Constants.PARSER_HOLE_ERR;
            return true;
        }
        return false;
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean ProcessLine(String currentline) {
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

                // zjisti typ zarizeni
                if (Mer.mvs >0)
                    Mer.dev = shared.MvsToDevice(Mer.mvs);
                else
                    Mer.dev = GuessDevice(Mer);
            }
            catch (Exception e) {
                    System.out.println(e);
                    return(false);
            }
        }
        return (true);
    }

    // nakopiruje soubor do temp a vrati prvni a posledni radek
    @RequiresApi(api = Build.VERSION_CODES.O)
    public FileDetail FirstLast(DocumentFile file)
    {
        try {
            // Copy DocumentFile to a temporary file
            File tempFile = FileUtils.copyDocumentFileToTempFile(context, file);
            // Get Uri from the temp file
            Uri tempUri = Uri.fromFile(tempFile);
            // Call the existing FirstLast(Uri uri) method
            //FileDetail fdet = new FileDetail(file.getUri().getLastPathSegment());
            FileDetail fdet = FirstLast(tempUri);
            return fdet;

        } catch (IOException e) {
            Log.e(TAG, "Error copying DocumentFile to temp file", e);
            FileDetail fdet = new FileDetail(file.getName());
            fdet.setErr(Constants.PARSER_ERROR);
            return fdet;
        }// nakopiruj do temp
    }

    // nacte prvni a posledni radek ze souboru
    @RequiresApi(api = Build.VERSION_CODES.O)
    public FileDetail FirstLast(Uri uri) throws IOException {
        FileDetail fdet = new FileDetail(uri.getLastPathSegment());     // nastavim rovnou jmeno souboru

        // Get the creation date
        Path path = new File(uri.getPath()).toPath();
        FileTime fileTime = (FileTime) Files.getAttribute(path, "creationTime");
        LocalDateTime creationDate = LocalDateTime.ofInstant(fileTime.toInstant(), ZoneId.systemDefault());
        fdet.setCreated(creationDate);

        DocumentFile docFile = DocumentFile.fromSingleUri(context, uri);
        File tempFile = FileUtils.copyDocumentFileToTempFile(context, docFile);
        RandomAccessFile raf = new RandomAccessFile(tempFile, "r");
        // extrahuj seriove cislo z jmena souboru
        String serialNumber = shared.getSerialNumberFromFileName(uri.getLastPathSegment());

        // prvni radek -> ... data od
        String s = raf.readLine();
        if (!ProcessLine(s)) {
            fdet.setErr(Constants.PARSER_ERROR);
            return fdet;
        }
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
        // posledni radek , data do...
        s= sb.reverse().toString();
        if (!ProcessLine(s)) {
            fdet.setErr(Constants.PARSER_ERROR);
            return fdet;
        }

        fdet.setInto(Mer.dtm);
        fdet.setCount(Mer.idx);
        fdet.setDeviceType(Mer.dev);
        fdet.setFileSize(fileLength);

        return fdet;
    }

    private void DoProgress(int pos)
    {
        if (oldPos == pos)
            return;
        oldPos = pos;


        if (progressListener != null) {
            progressListener.OnProEvent(pos);
        }



        /*
        progressBarHandler.post(new Runnable()
        {
            public void run()
            {
                if (progressListener != null)
                {
                    progressListener.OnProEvent(pos);
                }
            }
        });

         */
    }


    private void DoFinished(int pos) {
        progressBarHandler.post(new Runnable() {
            public void run() {
                if (mFinListener != null) {
                    mFinListener.OnProEvent(pos);
                }
            }
        });
    }


    private LocalDateTime getCreationDateFromUri(Context context, Uri uri) {
        String[] projection = { MediaStore.MediaColumns.DATE_ADDED };
        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                long dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED));
                return LocalDateTime.ofEpochSecond(dateAdded, 0, java.time.ZoneOffset.UTC);
            }
        }
        return null;
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public FileDetail readFileContent(Uri uri) throws IOException
    {
        int  idx = 0;

        // Streamovane vycteni, zatim nejrychlejsi verze
        InputStream inputStream =
              //  this.context.getContentResolver().openInputStream(uri);
                LollyActivity.getInstance().getContentResolver().openInputStream(uri);
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(inputStream));
        Integer totalBytes = inputStream.available();  // pocet dostupnych bytu
        StringBuilder stringBuilder = new StringBuilder();

        currDay = 0;
        iline = 0;
        ClearAvg();
        // cas prvniho mereni
        FileDetail fileDetail = new FileDetail(uri.getLastPathSegment());

        // soubor byl vytvoren
        //Path path = new File(uri.getPath()).toPath();
        //FileTime fileTime = (FileTime) Files.getAttribute(path, "creationTime");
        //LocalDateTime creationDate = LocalDateTime.ofInstant(fileTime.toInstant(), ZoneId.systemDefault());
        LocalDateTime creationDate = getCreationDateFromUri(context, uri);
        fileDetail.setCreated(creationDate);

        // je soubor prazdny ?
        String currentline = reader.readLine();
        if (currentline == null)
            return fileDetail;

        if (!ProcessLine(currentline))
        {
            fileDetail.setErr(Constants.PARSER_ERROR);
            return fileDetail;
        }

        fileDetail.setFrom(Mer.dtm);
        fileDetail.setErr(Constants.PARSER_OK);  // zadne chyby

        DoProgress(-totalBytes);  // vynuluj progress bar

        Mer.Err = 0;
        copyMerToMol(Mer);
        while ((currentline = reader.readLine()) != null)
        {
            //  vysledek je v lokalni promenne Mer
            if (ProcessLine(currentline) == false)
               fileDetail.setErr(Constants.PARSER_ERROR);

            Mer.idx = ++iline;
            idx = inputStream.available();
            idx = (totalBytes - idx) ;

            // AppendStat(Mer);
            FindMinMax(Mer);
            WasHole(Mer);       // nastaveni priznaku Mer.Err
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
        fileDetail.setDeviceType(Mer.dev);
        fileDetail.setErr(Mer.Err);

        // pozor ! musi byt pred DoProgress
        det = fileDetail;  // ulozime si detail pro GetFileDetail()

        //return stringBuilder.toString();
        DoProgress(-1);
        return fileDetail;
    }

    public void Prepare()
    {
        this.context = LollyActivity.getInstance().getApplicationContext();
        this.FileName = null;
        ClearPrivate();
        ClearAvg();

        Looper.prepare();
    }


    // tohle pustim po startu
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void run()
    {
        Looper.prepare();
        if (!this.FileName.contains(".csv") )
            return;

        this.context = LollyActivity.getInstance().getApplicationContext();
        // Uri uri = Uri.parse(this.FileName);

        String ADir = LollyActivity.getInstance().getPrefExportFolder();
        DocumentFile exportDir = DocumentFile.fromTreeUri(this.context, Uri.parse(ADir));
        if (exportDir == null) return;

        DocumentFile file = exportDir.findFile(this.FileName);

        try {
            det = readFileContent(file.getUri());
        } catch (IOException e) {
            // Handle error here
            Log.d(TAG,e.toString());
        }

    }





}