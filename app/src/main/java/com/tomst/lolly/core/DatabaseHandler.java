package com.tomst.lolly.core;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.tomst.lolly.fileview.FileDetail;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class DatabaseHandler extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "lolly.db";

    //  tables
    private static final String TABLE_CLIST= "csvlist";
    private static final String TABLE_TRACKS = "tracks";
    private static final String TABLE_PLACEMARKS = "placemarks";

    // common field name
    private static final String KEY_ID = "id";
    private static final String KEY_LINE ="idj";

    /** @noinspection SpellCheckingInspection*/ //  table csvlist "list of data .csv files"
    private static final String KEY_CLIST_NAME = "name";   //  nazev souboru
    private static final String KEY_CLIST_URL = "url";             //  cela cesta k souboru
    private static final String KEY_CLIST_TYPE = "type";           //  typ zarizeni
    private static final String KEY_CLIST_MD5 = "md5";
    private static final String  KEY_CLIST_CREATED ="created";
    private static final String  KEY_CLIST_FIRST="first";
    private static final String  KEY_CLIST_LAST="last";
    private static final String  KEY_CLIST_COUNT = "count";
    private static final String  KEY_CLIST_SIZE = "size"; // velikost souboru
    private static final  String  KEY_CLIST_MINT1 = "mint1";
    private static final String  KEY_CLIST_MAXT1 = "maxt1";
    private static final String  KEY_CLIST_MINT2 = "mint2";
    private static final String  KEY_CLIST_MAXT2 = "maxt2";
    private static final String  KEY_CLIST_MINT3 = "mint3";
    private static final String  KEY_CLIST_MAXT3 = "maxt3";
    private static final String  KEY_CLIST_MINHUM="minhum";
    private static final String KEY_CLIST_MAXHUM="maxhum";

    private static final String KEY_LOCATION_NUMBER = "nr";
    private static final String KEY_LOCATION_LATITUDE = "latitude";
    private static final String KEY_LOCATION_LONGITUDE = "longitude";
    private static final String KEY_LOCATION_ALTITUDE = "altitude";
    private static final String KEY_LOCATION_SPEED = "speed";
    private static final String KEY_LOCATION_ACCURACY = "accuracy";
    private static final String KEY_LOCATION_BEARING = "bearing";
    private static final String KEY_LOCATION_TIME = "time";
    private static final String KEY_LOCATION_NUMBEROFSATELLITES = "number_of_satellites";
    private static final String KEY_LOCATION_TYPE = "loctype";
    private static final String KEY_LOCATION_NUMBEROFSATELLITESUSEDINFIX = "number_of_satellites_used_in_fix";
    private static final String KEY_CLIST_ERR = "err";

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CLIST_TABLE = "CREATE TABLE " + TABLE_CLIST + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_LINE + " INTEGER,"
                + KEY_CLIST_NAME + " TEXT,"
                + KEY_CLIST_URL + " TEXT,"
                + KEY_CLIST_TYPE + " INTEGER,"
                + KEY_CLIST_MD5 + " TEXT,"
                + KEY_CLIST_CREATED + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + KEY_CLIST_FIRST + " DATETIME,"
                + KEY_CLIST_LAST + " DATETIME,"
                + KEY_CLIST_COUNT + " INTEGER,"
                + KEY_CLIST_SIZE + " INTEGER,"
                + KEY_CLIST_MINT1 + " REAL,"
                + KEY_CLIST_MAXT1 + " REAL,"
                + KEY_CLIST_MINT2 + " REAL,"
                + KEY_CLIST_MAXT2 + " REAL,"
                + KEY_CLIST_MINT3 + " REAL,"
                + KEY_CLIST_MAXT3 + " REAL,"
                + KEY_CLIST_MINHUM + " REAL,"
                + KEY_CLIST_MAXHUM + " REAL,"
                + KEY_LOCATION_NUMBER + " INTEGER,"
                + KEY_LOCATION_LATITUDE + " REAL,"
                + KEY_LOCATION_LONGITUDE + " REAL,"
                + KEY_LOCATION_ALTITUDE + " REAL,"
                + KEY_LOCATION_SPEED + " REAL,"
                + KEY_LOCATION_ACCURACY + " REAL,"
                + KEY_LOCATION_BEARING + " REAL,"
                + KEY_LOCATION_TIME + " DATETIME,"
                + KEY_LOCATION_NUMBEROFSATELLITES + " INTEGER,"
                + KEY_LOCATION_TYPE + " INTEGER,"
                + KEY_LOCATION_NUMBEROFSATELLITESUSEDINFIX + " INTEGER,"
                + KEY_CLIST_ERR + " INTEGER)";
        db.execSQL(CREATE_CLIST_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CLIST);
        onCreate(db);
    }

    public void addFileDetail(FileDetail fdet) {

    }

    public long addFile(FileDetail fileDetail, Location location) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        // Populate ContentValues with FileDetail data
        //values.put(KEY_CLIST_ERR,fileDetail.getErrFlag());
        values.put(KEY_CLIST_ERR,fileDetail.errFlag);

        values.put(KEY_CLIST_NAME, fileDetail.getName());
        values.put(KEY_CLIST_URL, fileDetail.getFull());
        values.put(KEY_CLIST_TYPE, fileDetail.getDeviceType().ordinal()); // ordinalni hodnota typu zarizeni
        values.put(KEY_CLIST_MD5, ""); // Assuming MD5 is empty, adjust as needed
        values.put(KEY_CLIST_CREATED, fileDetail.getCreated().toString());
        values.put(KEY_CLIST_FIRST, fileDetail.iFrom != null ? fileDetail.iFrom.toString() : "");
        values.put(KEY_CLIST_LAST, fileDetail.iInto != null ? fileDetail.iInto.toString() : "");
        values.put(KEY_CLIST_COUNT, fileDetail.iCount);
        values.put(KEY_CLIST_SIZE, fileDetail.getFileSize());
        values.put(KEY_CLIST_MINT1, fileDetail.getMinT1());
        values.put(KEY_CLIST_MAXT1, fileDetail.getMaxT1());
        values.put(KEY_CLIST_MINT2, fileDetail.getMinT2());
        values.put(KEY_CLIST_MAXT2, fileDetail.getMaxT2());
        values.put(KEY_CLIST_MINT3, fileDetail.getMinT3());
        values.put(KEY_CLIST_MAXT3, fileDetail.getMaxT3());
        values.put(KEY_CLIST_MINHUM, fileDetail.getMinHum());
        values.put(KEY_CLIST_MAXHUM, fileDetail.getMaxHum());

        // Populate ContentValues with Location data
        if (location != null) {
            values.put(KEY_LOCATION_LATITUDE, location.getLatitude());
            values.put(KEY_LOCATION_LONGITUDE, location.getLongitude());
            values.put(KEY_LOCATION_ALTITUDE, location.getAltitude());
            values.put(KEY_LOCATION_SPEED, location.getSpeed());
            values.put(KEY_LOCATION_ACCURACY, location.getAccuracy());
            values.put(KEY_LOCATION_BEARING, location.getBearing());
            values.put(KEY_LOCATION_TIME, location.getTime());
            values.put(KEY_LOCATION_NUMBEROFSATELLITES, 0); // Assuming 0, adjust as needed
            values.put(KEY_LOCATION_TYPE, 0); // Assuming type is 0, adjust as needed
            values.put(KEY_LOCATION_NUMBEROFSATELLITESUSEDINFIX, 0); // Assuming 0, adjust as needed
        }
        long result = db.insert(TABLE_CLIST, null, values);
        //db.close();
        return 0;
    }

    public void updateFile(FileDetail fileDetail) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        // Populate ContentValues with FileDetail data
        values.put(KEY_CLIST_ERR, fileDetail.errFlag);
        values.put(KEY_CLIST_NAME, fileDetail.getName());
        values.put(KEY_CLIST_URL, fileDetail.getFull());
        values.put(KEY_CLIST_TYPE, 0); // Assuming type is 0, adjust as needed
        values.put(KEY_CLIST_MD5, ""); // Assuming MD5 is empty, adjust as needed
        values.put(KEY_CLIST_CREATED, fileDetail.iFrom.toString());
        values.put(KEY_CLIST_FIRST, fileDetail.iFrom != null ? fileDetail.iFrom.toString() : "");
        values.put(KEY_CLIST_LAST, fileDetail.iInto != null ? fileDetail.iInto.toString() : "");
        values.put(KEY_CLIST_COUNT, fileDetail.iCount);
        values.put(KEY_CLIST_SIZE, fileDetail.getFileSize());
        values.put(KEY_CLIST_MINT1, fileDetail.getMinT1());
        values.put(KEY_CLIST_MAXT1, fileDetail.getMaxT1());
        values.put(KEY_CLIST_MINT2, fileDetail.getMinT2());
        values.put(KEY_CLIST_MAXT2, fileDetail.getMaxT2());
        values.put(KEY_CLIST_MINT3, fileDetail.getMinT3());
        values.put(KEY_CLIST_MAXT3, fileDetail.getMaxT3());
        values.put(KEY_CLIST_MINHUM, fileDetail.getMinHum());
        values.put(KEY_CLIST_MAXHUM, fileDetail.getMaxHum());

        db.update(TABLE_CLIST, values, KEY_ID + " = ?", new String[]{String.valueOf(fileDetail.getId())});
        //db.close();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private LocalDateTime parseDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.isEmpty() || dateTimeString.equals("null")) {
            return null;
        }
        try {
            // First, try parsing with the standard format
            return LocalDateTime.parse(dateTimeString);
        } catch (DateTimeParseException e) {
            // If that fails, try parsing with the custom "dd.MM.yyyy HH:mm:ss" format
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
                return LocalDateTime.parse(dateTimeString, formatter);
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("Range")
    public FileDetail copyCursorToFileDetail(Cursor cursor)
    {
        FileDetail fileDetail = new FileDetail(cursor.getLong(cursor.getColumnIndex(KEY_ID)));
        fileDetail.errFlag = cursor.getInt(cursor.getColumnIndex(KEY_CLIST_ERR));
        fileDetail.setName(cursor.getString(cursor.getColumnIndex(KEY_CLIST_NAME)));
        fileDetail.setDeviceType(TDeviceType.values()[cursor.getInt(cursor.getColumnIndex(KEY_CLIST_TYPE))]);
        fileDetail.setFull(cursor.getString(cursor.getColumnIndex(KEY_CLIST_URL)));
        fileDetail.setCreated(parseDateTime(cursor.getString(cursor.getColumnIndex(KEY_CLIST_CREATED))));
        fileDetail.iFrom = parseDateTime(cursor.getString(cursor.getColumnIndex(KEY_CLIST_FIRST)));
        fileDetail.iInto = parseDateTime(cursor.getString(cursor.getColumnIndex(KEY_CLIST_LAST)));
        fileDetail.iCount = cursor.getInt(cursor.getColumnIndex(KEY_CLIST_COUNT));
        fileDetail.setFileSize(cursor.getLong(cursor.getColumnIndex(KEY_CLIST_SIZE)));
        fileDetail.setMinT1(cursor.getDouble(cursor.getColumnIndex(KEY_CLIST_MINT1)));
        fileDetail.setMaxT1(cursor.getDouble(cursor.getColumnIndex(KEY_CLIST_MAXT1)));
        fileDetail.setMinT2(cursor.getDouble(cursor.getColumnIndex(KEY_CLIST_MINT2)));
        fileDetail.setMaxT2(cursor.getDouble(cursor.getColumnIndex(KEY_CLIST_MAXT2)));
        fileDetail.setMinT3(cursor.getDouble(cursor.getColumnIndex(KEY_CLIST_MINT3)));
        fileDetail.setMaxT3(cursor.getDouble(cursor.getColumnIndex(KEY_CLIST_MAXT3)));
        fileDetail.setMinHum(cursor.getDouble(cursor.getColumnIndex(KEY_CLIST_MINHUM)));
        fileDetail.setMaxHum(cursor.getDouble(cursor.getColumnIndex(KEY_CLIST_MAXHUM)));

        // Populate the Location object with the data from the database
        Location location = new Location("");
        location.setLatitude(cursor.getDouble(cursor.getColumnIndex(KEY_LOCATION_LATITUDE)));
        location.setLongitude(cursor.getDouble(cursor.getColumnIndex(KEY_LOCATION_LONGITUDE)));
        location.setAltitude(cursor.getDouble(cursor.getColumnIndex(KEY_LOCATION_ALTITUDE)));
        location.setSpeed(cursor.getFloat(cursor.getColumnIndex(KEY_LOCATION_SPEED)));
        location.setAccuracy(cursor.getFloat(cursor.getColumnIndex(KEY_LOCATION_ACCURACY)));
        location.setBearing(cursor.getFloat(cursor.getColumnIndex(KEY_LOCATION_BEARING)));
        location.setTime(cursor.getLong(cursor.getColumnIndex(KEY_LOCATION_TIME)));
        fileDetail.setLocation(location);
        return fileDetail;
    }

    // Přidejte tuto metodu do vaší třídy DatabaseHandler.java
    @RequiresApi(api = Build.VERSION_CODES.O)
    public Map<String, FileDetail> getAllFileDetailsAsMap() {
        Map<String, FileDetail> fileDetailsMap = new HashMap<>();
        String selectQuery = "SELECT * FROM " + TABLE_CLIST; // Nahraďte skutečným názvem tabulky

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        FileDetail fileDetail = null;

        // Procházení všech řádků a jejich přidání do mapy
        if (cursor.moveToFirst()) {
            do {

                // Zde naplňte objekt fileDetail z kurzoru (stejně jako v getFileDetail)
                // Např.: fileDetail.setName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME)));
                // ... naplnit ostatní vlastnosti
                fileDetail = copyCursorToFileDetail(cursor);

                String fileName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CLIST_NAME)); // Předpokládám sloupec KEY_NAME pro název souboru
                fileDetailsMap.put(fileName, fileDetail);
            } while (cursor.moveToNext());
        }
        cursor.close();
       // db.close();

        return fileDetailsMap;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("Range")
    public FileDetail getFileDetail(String fileName) {
        SQLiteDatabase db = this.getReadableDatabase();
        FileDetail fileDetail = null;

        // Query the database for the file with the given name
        Cursor cursor = db.query(TABLE_CLIST, null, KEY_CLIST_NAME + " = ?", new String[]{fileName}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            fileDetail = copyCursorToFileDetail(cursor);
            cursor.close();
        }
       // db.close();
        return fileDetail;
    }

      // skutecne existujici soubory v cache aplikace
      // vyhodim z databaze zaznamy, ktere nemaji obraz v adresari
      // TEST - vycti zarizeni
      // TEST - vymaz tento soubor z adresari
      // TEST - novy beh mi soubor oznaci a vyhodi z csvlist (breakpoint v (!found)
      public int ClearUnusedFiles(String[] UsedFileNames) { // fileNames is an array of file names
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM csvlist", null);
        // Iterate through the results and log each line
        int i = 0;
        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") String dbName = cursor.getString(cursor.getColumnIndex("name"));
                @SuppressLint("Range") int id = cursor.getInt(cursor.getColumnIndex("id"));
               // System.out.println("Name: " + name + ", ID: " + id);

                //
                boolean found = false;
                for (String fileName : UsedFileNames) {
                    if (dbName.equals(fileName)) {
                        found = true;
                        break;
                    }
                }

                // If the name is not found in UsedFileNames, delete the entry
                if (!found) {
                    int result = db.delete(TABLE_CLIST, KEY_CLIST_NAME + " = ?", new String[]{dbName});
                    System.out.println("Deleted file: " + dbName);
                    i++;
                }
            } while (cursor.moveToNext());
        }

        cursor.close();
      //  db.close();
        return i; // vracim, pocet vymazanych zaznamu
    }

}
