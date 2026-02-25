package com.tomst.lolly.core;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.documentfile.provider.DocumentFile;

import com.tomst.lolly.LollyActivity;
import com.tomst.lolly.fileview.FileDetail;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;


public class CSVReader extends Thread
{
    public static Handler handler = null;
    private static Handler progressBarHandler = new Handler(Looper.getMainLooper());  // handler pro vysilani z Threadu

    private OutputStream fNewCsv;
    private int oldPos=0;
    private int iline=0;

    // Zůstává pouze pro formátování ZÁPISU do CSV, aby byl výstup vždy konzistentní
    private static final String DMD_PATTERN = "yyyy.MM.dd HH:mm";

    // TADY JE NOVÝ DYNAMICKÝ FORMATTER PRO ČTENÍ
    private DateTimeFormatter currentFormatter = null;

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
    private LocalDateTime currDate;
    private final String TAG = "TOMST";

    public void SetHandler(Handler han){
        CSVReader.handler = han;
    }

    TMereni Mer = new TMereni(); // mereni
    TMereni Mol = new TMereni(); // stare mereni

    private OnProListener progressListener; // listener field
    public OnProListener mFinListener; // listener field

    private String FileName="";

    // pozice progressBaru
    public void SetProgressListener(OnProListener AListener){
        this.progressListener = AListener;
    }

    public void SetFinListener(OnProListener AListener){
        this.mFinListener = AListener;
    }

    private static FileDetail det= null;

    // posledni vycteny detail souboru
    // zodpovidam za volani fce readFileContent() predtim
    public FileDetail getFileDetail(){
        return det;
    }

    private static Context context = null;

    public static class FileUtils {
        public static File copyDocumentFileToTempFile(Context context, DocumentFile documentFile) throws IOException {
            File tempFile = File.createTempFile("temp", null, context.getCacheDir());
            try (InputStream inputStream = context.getContentResolver().openInputStream(documentFile.getUri());
                 FileOutputStream outputStream = new FileOutputStream(tempFile)) {

                byte[] buffer = new byte[1024];
                int length;
                while (inputStream != null && (length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }

            }
            return tempFile;
        }
    }

    private void ClearPrivate()
    {
        currDay = 0;
        iline = 0;
        currIx = 1;
        currentFormatter = null; // Reset formátovače
    }

    // constructor
    public CSVReader(String AFileName)
    {
        super("CSVReaderThread"); // nazev vlakna pro debugger
        CSVReader.context = LollyActivity.getInstance().getApplicationContext();

        this.FileName = AFileName;
        ClearPrivate();
        ClearAvg();
    }

    public CSVReader()
    {
        super("CSVReaderThread"); // nazev vlakna pro debugger
        CSVReader.context = LollyActivity.getInstance().getApplicationContext();
        this.FileName = null;
        ClearPrivate();
        ClearAvg();
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

    public void OpenForWrite(String AFileName) {
        CSVReader.context = LollyActivity.getInstance().getApplicationContext();

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
                sFmt = "%d;%s;%d;%.3f;%.0f;%.0f;%d;%d;%d";
                break;
        }
        return sFmt;
    }

    // csv radek z TMereni (Používá výchozí masku DMD_PATTERN pro konzistentní export)
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
        return String.format(sFmt,Mer.idx, dts, Mer.gtm, Mer.t1, Mer.t2, Mer.t3, hum, Mer.mvs, Mer.Err);
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
        if (Mer.t1 > maxT1) maxT1 = Mer.t1;
        if (Mer.t2 > maxT2) maxT2 = Mer.t2;
        if (Mer.t3 > maxT3) maxT3 = Mer.t3;
        if (Mer.t1 < minT1) minT1 = Mer.t1;
        if (Mer.t2 < minT2) minT2 = Mer.t2;
        if (Mer.t3 < minT3) minT3 = Mer.t3;
        if (Mer.hum > maxHm) maxHm = Mer.hum;
        if (Mer.hum < minHm) minHm = Mer.hum;
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
    private void WasHole(TMereni Mer)
    {
        // zjisti zda je mezera v datech
        if (Mol.dtm==null) {
            copyMerToMol(Mer);
            return;
        }
        Duration duration = Duration.between(Mol.dtm, Mer.dtm);
        copyMerToMol(Mer);
        // rozdil > 1 hod je podezrely
        if (duration.getSeconds() > 60*60) {
            Mer.Err = Constants.PARSER_HOLE_ERR;
        }
    }

    // --- PRE-SCAN FUNKCE ---
    public static String detectDateFormat(Context ctx, Uri uri) {
        String dateFormat = "dd.MM.yyyy"; // Výchozí fallback (evropský)
        String timeFormat = "HH:mm";

        // TADY JE ZMĚNA: Používáme předaný 'ctx' místo vnitřního 'context'
        try (ParcelFileDescriptor pfd = ctx.getContentResolver().openFileDescriptor(uri, "r");
             FileInputStream fis = new FileInputStream(pfd.getFileDescriptor());
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {

            String line;
            int maxLinesToRead = 2000; // Pojistka (přečte max. ~20 dní měření)
            int lineCount = 0;

            while ((line = reader.readLine()) != null && lineCount < maxLinesToRead) {
                lineCount++;
                String[] str = line.split(";");
                if (str.length < 8) continue;

                String dateString = str[1].trim();
                String[] parts = dateString.split(" ");
                if (parts.length < 2) continue;

                String datePart = parts[0];
                String timePart = parts[1];

                // Detekce času (jestli obsahuje vteřiny)
                if (timePart.split(":").length == 3) {
                    timeFormat = "HH:mm:ss";
                }

                // Detekce data
                String[] dTokens = datePart.split("\\.");
                if (dTokens.length == 3) {
                    // 1. případ: Rok je na začátku (2019.11.29) -> ISO
                    if (dTokens[0].length() == 4) {
                        dateFormat = "yyyy.MM.dd";
                        break;
                    }
                    // 2. případ: Rok je na konci (28.03.2019 nebo 03.28.2019)
                    else if (dTokens[2].length() == 4) {
                        try {
                            int v1 = Integer.parseInt(dTokens[0]);
                            int v2 = Integer.parseInt(dTokens[1]);

                            if (v1 > 12) {
                                dateFormat = "dd.MM.yyyy"; // Den je první
                                break;
                            } else if (v2 > 12) {
                                dateFormat = "MM.dd.yyyy"; // Den je druhý (US formát)
                                break;
                            }
                            // Pokud obě hodnoty <= 12, pre-scan pokračuje dál...
                        } catch (NumberFormatException e) {
                            // Vadný řádek ignorujeme
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("TOMST", "Chyba při pre-scanu formátu data", e);
        }

        return dateFormat + " " + timeFormat;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean ProcessLine(String currentline) {
        if (currentline.isEmpty()) {
            return false;
        }
        // rozsekej radku
        String[] str = currentline.split(";", 0);
        if (str.length < 8) return false;

        // datum
        try {
            // Pojistka, pokud by pre-scan z nějakého důvodu neprošel
            if (currentFormatter == null) {
                currentFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
            }

            // TADY JE ZMĚNA: Používáme dynamický formatter
            LocalDateTime dateTime = LocalDateTime.parse(str[1], currentFormatter);
            Mer.dtm = dateTime;
            Mer.day = dateTime.getDayOfMonth();

            if (currDay == 0) {
                currDay = Mer.day;
                currDate = Mer.dtm;
            }

            Mer.idx = Integer.parseInt(str[iLine]);

            // teploty
            String T1 = str[iT1].replace(',', '.');
            String T2 = str[iT2].replace(',', '.');
            String T3 = str[iT3].replace(',', '.');
            Mer.t1 = Float.parseFloat(T1);
            Mer.t2 = Float.parseFloat(T2);
            Mer.t3 = Float.parseFloat(T3);
            Mer.hum = Integer.parseInt(str[iHum]);
            Mer.mvs = Integer.parseInt(str[iMvs]);

            // zjisti typ zarizeni
            if (Mer.mvs > 0)
                Mer.dev = shared.MvsToDevice(Mer.mvs);
            else
                Mer.dev = GuessDevice(Mer);
        }
        catch (Exception e) {
            System.out.println(e);
            return false;
        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public FileDetail FirstLast(DocumentFile file) {
        // Nejprve si dynamicky načteme formát z hlavičky souboru
        currentFormatter = DateTimeFormatter.ofPattern(detectDateFormat(context,file.getUri()));

        FileDetail fdet = new FileDetail(file.getName());
        fdet.setErr(Constants.PARSER_OK);

        try (ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(file.getUri(), "r")) {
            if (pfd == null) {
                fdet.setErr(Constants.PARSER_ERROR);
                return fdet;
            }
            FileDescriptor fd = pfd.getFileDescriptor();

            // Read first line
            try (FileInputStream fis = new FileInputStream(fd);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
                String firstLine = reader.readLine();
                if (firstLine == null) {
                    fdet.setErr(Constants.PARSER_FILE_EMPTY);
                    return fdet;
                }
                if (!ProcessLine(firstLine)) {
                    fdet.setErr(Constants.PARSER_ERROR);
                    return fdet;
                }
                fdet.setFrom(Mer.dtm);
            }

            // Read last line using FileChannel from a new FileInputStream
            try (FileInputStream fis = new FileInputStream(fd);
                 FileChannel channel = fis.getChannel()) {
                channel.position(0); // Reset position for reading
                long fileSize = channel.size();
                if (fileSize > 0) {
                    int bufferSize = (int) Math.min(fileSize, 8192);
                    ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
                    channel.read(buffer, fileSize - bufferSize);

                    String chunk = new String(buffer.array(), StandardCharsets.UTF_8);
                    int lastNewline = chunk.lastIndexOf('\n');
                    String lastLine = (lastNewline == -1) ? chunk : chunk.substring(lastNewline + 1);

                    if (!ProcessLine(lastLine.trim())) {
                        fdet.setErr(Constants.PARSER_ERROR);
                        return fdet;
                    }
                    fdet.setInto(Mer.dtm);
                    fdet.setCount(Mer.idx);
                }
            }

            fdet.setDeviceType(Mer.dev);
            fdet.setFileSize(file.length());

        } catch (IOException e) {
            Log.e(TAG, "Error reading file with FileDescriptor", e);
            fdet.setErr(Constants.PARSER_ERROR);
        }

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
    }

    private void DoFinished() {
        progressBarHandler.post(() -> {
            if (mFinListener != null) {
                mFinListener.OnProEvent(0);
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public FileDetail readFileContent(Uri uri) throws IOException {
        // Nejprve si dynamicky načteme formát z hlavičky souboru
        currentFormatter = DateTimeFormatter.ofPattern(detectDateFormat(context,uri));

        FileDetail fileDetail = new FileDetail(uri.getLastPathSegment());
        ClearAvg();
        ClearPrivate();

        try (ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
             FileInputStream fis = new FileInputStream(pfd.getFileDescriptor());
             FileChannel channel = fis.getChannel()) {

            long totalBytes = channel.size();
            DoProgress((int) -totalBytes);

            ByteBuffer buffer = ByteBuffer.allocate(256 * 1024);
            ByteArrayOutputStream lineBuilder = new ByteArrayOutputStream();
            boolean firstLineProcessed = false;

            while (channel.read(buffer) != -1) {
                buffer.flip();
                while (buffer.hasRemaining()) {
                    byte b = buffer.get();
                    if (b == '\n' || b == '\r') {
                        if (lineBuilder.size() > 0) {
                            String line = lineBuilder.toString(StandardCharsets.UTF_8.name());
                            if (ProcessLine(line)) {
                                if (!firstLineProcessed) {
                                    fileDetail.setFrom(Mer.dtm);
                                    firstLineProcessed = true;
                                    copyMerToMol(Mer);
                                }
                                FindMinMax(Mer);
                                WasHole(Mer);
                                sendMessage(Mer);
                            }
                            lineBuilder.reset();
                        }
                    } else {
                        lineBuilder.write(b);
                    }
                }
                buffer.compact();
                DoProgress((int) channel.position());
            }

            // Process the last line if the file doesn't end with a newline
            if (lineBuilder.size() > 0) {
                String line = lineBuilder.toString(StandardCharsets.UTF_8.name());
                if (ProcessLine(line)) {
                    FindMinMax(Mer);
                    WasHole(Mer);
                    sendMessage(Mer);
                }
            }
        }

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
        fileDetail.setErr(0);
        //fileDetail.setErr(Mer.Err);

        det = fileDetail;
        DoFinished();
        return fileDetail;
    }

    // tohle pustim po startu
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void run()
    {
        Looper.prepare();
        if (!this.FileName.contains(".csv") )
            return;

        CSVReader.context = LollyActivity.getInstance().getApplicationContext();

        String ADir = LollyActivity.getInstance().getPrefExportFolder();
        DocumentFile exportDir = DocumentFile.fromTreeUri(CSVReader.context, Uri.parse(ADir));
        if (exportDir == null) return;

        DocumentFile file = exportDir.findFile(this.FileName);

        try {
            if (file != null) {
                det = readFileContent(file.getUri());
            }
        } catch (IOException e) {
            // Handle error here
            Log.d(TAG,e.toString());
        }
    }
}