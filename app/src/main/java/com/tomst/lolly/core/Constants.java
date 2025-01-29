package com.tomst.lolly.core;

import android.annotation.SuppressLint;

public class Constants {
    public static final long MAX_DELTA = 60*60;  // hours 1 in seconds
    public static boolean showMicro = true;
    static public int fMicroInter = 1279;
    static public double fMicroSlope = (8890.0 / (34000.0 - 1279.0));
    public static final String DEVICE_FORMAT = "dd.MM.yyyy hh:mm:ss";
    public static final String BUTTON_FORMAT = "dd.MM.yyyy";
    public static final String TAG = "TOMST";
    private static boolean SHOWTXF = false;
    private static final int MAXTIMEDIFF = 5; // pod 5 sekund nenastavuju cas v lizatku
    public static final String FILEDIR = "/storage/emulated/0/Documents/";
    public static final String KEY_EMAIL = "krata@tomst.com";
    public static final int MVS_OFFSET =  200; // Offset
    // constants for merging CSV files
    public  static final int HEADER_LINE_LENGTH = 3;
    public static final int SERIAL_NUMBER_INDEX = 1;
    public static final String DEFAULT_SERIAL_NUMBER_VALUE = "Unknown";

    // constants for parser
    public static final int PARSER_OK=0;
    public static final int PARSER_ERROR=1;

}
