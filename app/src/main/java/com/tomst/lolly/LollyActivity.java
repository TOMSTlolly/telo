package com.tomst.lolly;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.Manifest;
import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.content.pm.PackageManager;
import android.location.GnssStatus;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;


import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;
import com.tomst.lolly.core.Constants;
import com.tomst.lolly.core.DatabaseHandler;
import com.tomst.lolly.core.FileOpener;
import com.tomst.lolly.core.TDeviceType;
import com.tomst.lolly.databinding.ActivityMainBinding;
import com.tomst.lolly.core.DmdViewModel;
import com.tomst.lolly.core.LocationExtended;
import com.tomst.lolly.core.EventBusMSG;
import com.tomst.lolly.core.PhysicalDataFormatter;
import com.tomst.lolly.core.Track;
import com.tomst.lolly.core.EGM96;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.FirebaseApp;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;


public class LollyActivity extends AppCompatActivity implements View.OnClickListener, LocationListener {
    public static final int JOB_TYPE_NONE       = 0;                // No operation
    public static final int JOB_TYPE_EXPORT     = 1;                // Bulk Exportation
    public static final int JOB_TYPE_VIEW       = 2;                // Bulk View
    public static final int JOB_TYPE_SHARE      = 3;                // Bulk Share
    public static final int JOB_TYPE_DELETE     = 4;                // Bulk Delete

    public static final int GPS_DISABLED                = 0;
    public static final int GPS_OUTOFSERVICE            = 1;
    public static final int GPS_TEMPORARYUNAVAILABLE    = 2;
    public static final int GPS_SEARCHING               = 3;
    public static final int GPS_STABILIZING             = 4;
    public static final int GPS_OK                      = 5;

    private static final int STABILIZER_TIME = 3000;                // The application discards fixes for 3000 ms (minimum)
    private static final int DEFAULT_SWITCHOFF_HANDLER_TIME = 5000; // Default time for turning off GPS on exit
    private static final int GPS_UNAVAILABLE_HANDLER_TIME = 7000;   // The "GPS temporary unavailable" time

    private static final int MAX_ACTIVE_EXPORTER_THREADS = 3;       // The maximum number of Exporter threads to run simultaneously
    private static final int EXPORTING_STATUS_CHECK_INTERVAL = 16;  // The app updates the progress of exportation every 16 milliseconds

    private static final String TASK_SHUTDOWN       = "TASK_SHUTDOWN";      // The AsyncTodo Type to Shut down the DB connection
    private static final String TASK_NEWTRACK       = "TASK_NEWTRACK";      // The AsyncTodo Type to create a new track into DB
    private static final String TASK_ADDLOCATION    = "TASK_ADDLOCATION";   // The AsyncTodo Type to create a new track into DB
    private static final String TASK_ADDPLACEMARK   = "TASK_ADDPLACEMARK";  // The AsyncTodo Type to create a new placemark into DB
    private static final String TASK_UPDATEFIX      = "TASK_UPDATEFIX";     // The AsyncTodo Type to update the current FIX
    private static final String TASK_DELETETRACKS   = "TASK_DELETETRACKS";  // The AsyncTodo Type to delete some tracks

    public static final String FLAG_RECORDING       = "flagRecording";      // The persistent Flag is set when the app is recording, in order to detect Background Crashes
    public static final String FILETYPE_KML         = ".kml";
    public static final String FILETYPE_GPX         = ".gpx";

    private static final float[] NEGATIVE = {
            -1.0f,      0,      0,     0,  248,         // red
            0,  -1.0f,      0,     0,  248,         // green
            0,      0,  -1.0f,     0,  248,         // blue
            0,      0,      0, 1.00f,    0          // alpha
    };

    private String placemarkDescription = "";                    // The description of the Placemark (annotation) set by PlacemarkDialog
    private boolean isPlacemarkRequested;                        // True if the user requested to add a placemark (Annotation)
    private boolean isQuickPlacemarkRequest;                     // True if the user requested to add a placemark in a quick way (no annotation dialog)
    private boolean isRecording;                                 // True if the recording is active
    private boolean isBottomBarLocked;                           // True if the bottom bar is locked
    private boolean isGPSLocationUpdatesActive;                  // True if the Location Manager is active (is requesting FIXes)
    private boolean isForcedTrackpointsRecording = false;        // if True, the current fix is recorded into the track;
    private int gpsStatus = GPS_SEARCHING;                       // The status of the GPS: GPS_DISABLED, GPS_OUTOFSERVICE,

    // Preferences Variables
    private boolean prefShowDecimalCoordinates;                  // If true the coordinates are shows in decimal notation
    private int     prefUM                      = PhysicalDataFormatter.UM_METRIC;     // The units of measurement to use for visualization
    private int     prefUMOfSpeed               = PhysicalDataFormatter.UM_SPEED_KMH;  // The units of measurement to use for visualization of the speeds
    private float   prefGPSdistance             = 0f;            // The distance filter value
    private float   prefGPSinterval             = 0f;            // The interval filter value
    private long    prefGPSupdatefrequency      = 1000L;         // The GPS Update frequency in milliseconds
    private boolean prefEGM96AltitudeCorrection;                 // True if the EGM96 altitude correction is active
    private double  prefAltitudeCorrection      = 0d;            // The manual offset for the altitude correction, in meters
    private boolean prefExportKML               = true;          // If true the KML file are exported on Share/Export
    private boolean prefExportGPX               = true;          // If true the GPX file are exported on Share/Export
    private int     prefGPXVersion              = 100;           // The version of the GPX schema
    private boolean prefExportTXT;                               // If true the TXT file are exported on Share/Export
    private int     prefKMLAltitudeMode         = 0;             // The altitude mode for KML files: 1="clampToGround"; 0="absolute"
    private int     prefShowTrackStatsType      = 0;             // What shown stats are based on: 0="Total time"; 1="Time in movement"
    private int     prefShowDirections          = 0;             // Visualization of headings: 0="NSWE"; 1="Degrees"
    private boolean prefGPSWeekRolloverCorrected;                // A flag for Week Rollover correction
    private boolean prefShowLocalTime           = true;          // I true the app shows GPS Time instead of local time
    private String  prefExportFolder            = "";            // The folder for tracks exportation



    // ---- Moje cast LollyActivity
    SharedPreferences sharedPref = null;
    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final int STORAGE_PERMISSION_CODE = 100;

    public static String DIRECTORY_TEMP;             // The directory to store temporary tracks = getCacheDir() + "/Tracks"
    public static String DIRECTORY_FW;              // The directory that contains the empty gpx and kml file = getFilesDir() + "/URI"
    public static String DIRECTORY_LOGS;             // The directory that contains the empty gpx and kml file = getFilesDir() + "/URI"

    public static List<String> SAVE_LOG = new ArrayList<>();       // The list of log messages

    // nastaveni preferovane cesty pro ukladani dat
//    private String  prefExportFolder            = "";            // The folder for csv exportation
    public DatabaseHandler gpsDataBase;

    private static Location location = null;  // GPS souradnice


    private static String SerialNumber;
    public String getSerialNumber() {
        return SerialNumber;
    }
    public void setSerialNumber(String serialNumber) {
        SerialNumber = serialNumber;
    }

    public String getPrefExportFolder() {
        return prefExportFolder;                                 // The folder for csv exportation
    }
    public void setPrefExportFolder(String prefExportFolder) {
        this.prefExportFolder = prefExportFolder;                 // The folder for csv exportation
    }

    public int getPrefUMOfSpeed() {
        return prefUMOfSpeed;
    }

    public int getPrefUM() {
        return prefUM;
    }

    public boolean isAccuracyDecimal() {
        return (isAccuracyDecimalCounter != 0);
    }

    public int getPrefShowDirections() {
        return prefShowDirections;
    }

    public boolean getPrefShowDecimalCoordinates() {
        return prefShowDecimalCoordinates;
    }

    public boolean getPrefShowLocalTime() {
        return prefShowLocalTime;
    }

    public int getPrefShowTrackStatsType() {
        return prefShowTrackStatsType;
    }


    //private static final float M_TO_FT = 3.280839895f;
    public static final int NOT_AVAILABLE = -100000;
    private boolean isMockProvider;

    private LocationManager locationManager = null;              // GPS LocationManager
    private int numberOfSatellitesTotal = 0;                     // The total Number of Satellites
    private int numberOfSatellitesUsedInFix = 0;                 // The Number of Satellites used in Fix
    private int isAccuracyDecimalCounter        = 0;             // 0 = The GPS has accuracy rounded to the meter (not precise antennas)

    private int gpsActivityActiveTab = 1;                       // The active tab on GPSActivity
    private int jobProgress = 0;
    private int jobsPending = 0;                                 // The number of jobs to be done
    public int jobType = JOB_TYPE_NONE;                          // The type of job that is pending

    private int numberOfStabilizationSamples = 3;
    private int stabilizer = numberOfStabilizationSamples;       // The number of stabilization FIXes before the first valid Location
    private int handlerTime = DEFAULT_SWITCHOFF_HANDLER_TIME;              // The time for the GPS update requests deactivation

    private LocationExtended currentLocationExtended = null;     // The current Location
    private LocationExtended currentPlacemark = null;            // The location used to add the Placemark (Annotation)

//    private Track currentTrack = null;                           // The current track. Used for adding Trackpoints and Annotations
//    private Track trackToEdit = null;                            // The Track that the user selected to edit with the "Track Properties" Dialog
    private int selectedTrackTypeOnDialog = NOT_AVAILABLE;       // The Activity type selected into the Edit Details dialog.

    private boolean isPrevFixRecorded;                           // true if the previous fix has been recorded
    private boolean isFirstFixFound;                             // True if at less one fix has been obtained

    private LocationExtended prevFix            = null;          // The previous fix
    private LocationExtended prevRecordedFix    = null;          // The previous recorded fix
 //   private int gpsStatus = GPS_SEARCHING;                       // The status of the GPS: GPS_DISABLED, GPS_OUTOFSERVICE,

//    private LocationExtended currentPlacemark = null;            // The location used to add the Placemark (Annotation)
    private Track currentTrack = null;                           // The current track. Used for adding Trackpoints and Annotations
    private Track trackToEdit = null;                            // The Track that the user selected to edit with the "Track Properties" Dialog
//    private int selectedTrackTypeOnDialog = NOT_AVAILABLE;       // The Activity type selected into the Edit Details dialog.



    public int getNumberOfSatellitesTotal() {
        return numberOfSatellitesTotal;
    }

    public int getNumberOfSatellitesUsedInFix() {
        return numberOfSatellitesUsedInFix;
    }

    /**
     * The Class defines a Database transaction to be enqueued
     */
    private static class AsyncTODO {
        String taskType;
        LocationExtended location;
    }

    private final BlockingQueue<AsyncTODO> asyncTODOQueue
            = new LinkedBlockingQueue<>();                      // The FIFO for asynchronous DB operations

    public void setPlacemarkDescription(String Description) {
        this.placemarkDescription = Description;
    }

    // The Handler that sets the GPS Status to GPS_TEMPORARYUNAVAILABLE
    private final Handler gpsUnavailableHandler = new Handler();
    private final Runnable gpsUnavailableRunnable = new Runnable() {
        @Override
        public void run() {
            if ((gpsStatus == GPS_OK) || (gpsStatus == GPS_STABILIZING)) {
                gpsStatus = GPS_TEMPORARYUNAVAILABLE;
                stabilizer = numberOfStabilizationSamples;
                EventBus.getDefault().post(EventBusMSG.UPDATE_FIX);
            }
        }
    };



    @RequiresPermission(Manifest.permission.VIBRATE)
    @Override
    public void onLocationChanged(@NonNull Location loc) {
        //if ((loc != null) && (loc.getProvider().equals(LocationManager.GPS_PROVIDER)) {
        if (loc != null) {      // Location data is valid
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {          // For API >= 18
                if ((prevFix == null) || (loc.isFromMockProvider() != isMockProvider)) {  // Reset the number of satellites when the provider changes between GPS and MOCK
                    if (loc.isFromMockProvider() != isMockProvider) {
                        numberOfSatellitesTotal = NOT_AVAILABLE;
                        numberOfSatellitesUsedInFix = NOT_AVAILABLE;
                        isAccuracyDecimalCounter = 0;
                    }
                    isMockProvider = loc.isFromMockProvider();
                    if (isMockProvider) Log.w("myApp", "[#] GPSApplication.java - Provider Type = MOCK PROVIDER");
                    else Log.w("myApp", "[#] GPSApplication.java - Provider Type = GPS PROVIDER");
                }
            }

            if (Math.round(loc.getAccuracy()) != loc.getAccuracy())
                isAccuracyDecimalCounter = 10;                                          // Sets the visualization of the accuracy in decimal mode (>0)
            else
                isAccuracyDecimalCounter -= isAccuracyDecimalCounter > 0 ? 1 : 0;       // If the accuracy is integer for 10 samples, we start to show it rounded to the meter

            //Log.w("myApp", "[#] GPSApplication.java - onLocationChanged: provider=" + loc.getProvider());
            if (loc.hasSpeed() && (loc.getSpeed() == 0)) loc.removeBearing();           // Removes bearing if the speed is zero
            // --------- Workaround for old GPS that are affected to Week Rollover
            //loc.setTime(loc.getTime() - 619315200000L);                               // Commented out, it simulate the old GPS hardware Timestamp
            if (loc.getTime() <= 1388534400000L)                                        // if the Location Time is <= 01/01/2014 00:00:00.000
                loc.setTime(loc.getTime() + 619315200000L);                             // Timestamp incremented by 1024×7×24×60×60×1000 = 619315200000 ms
            // This value must be doubled every 1024 weeks !!!
            LocationExtended eloc = new LocationExtended(loc);
            eloc.setNumberOfSatellites(getNumberOfSatellitesTotal());
            eloc.setNumberOfSatellitesUsedInFix(getNumberOfSatellitesUsedInFix());
            boolean forceRecord = false;

            gpsUnavailableHandler.removeCallbacks(gpsUnavailableRunnable);                            // Cancel the previous unavail countdown handler
            gpsUnavailableHandler.postDelayed(gpsUnavailableRunnable, GPS_UNAVAILABLE_HANDLER_TIME);  // starts the unavailability timeout (in 7 sec.)

            if (gpsStatus != GPS_OK) {
                if (gpsStatus != GPS_STABILIZING) {
                    gpsStatus = GPS_STABILIZING;
                    stabilizer = numberOfStabilizationSamples;
                    EventBus.getDefault().post(EventBusMSG.UPDATE_FIX);
                }
                else stabilizer--;
                if (stabilizer <= 0) gpsStatus = GPS_OK;
                prevFix = eloc;
                prevRecordedFix = eloc;
                isPrevFixRecorded = true;
            }

            // Save fix in case this is a STOP or a START (the speed is "old>0 and new=0" or "old=0 and new>0")
            if ((prevFix != null) && (prevFix.getLocation().hasSpeed()) && (eloc.getLocation().hasSpeed()) && (gpsStatus == GPS_OK) && (isRecording)
                    && (((eloc.getLocation().getSpeed() == 0) && (prevFix.getLocation().getSpeed() != 0)) || ((eloc.getLocation().getSpeed() != 0) && (prevFix.getLocation().getSpeed() == 0)))) {
                if (!isPrevFixRecorded) {                   // Record the old sample if not already recorded
                    AsyncTODO ast = new AsyncTODO();
                    ast.taskType = TASK_ADDLOCATION;
                    ast.location = prevFix;
                    asyncTODOQueue.add(ast);
                    prevRecordedFix = prevFix;
                    isPrevFixRecorded = true;
                }
                forceRecord = true;                         // + Force to record the new
            }

            if ((isRecording) && (isPlacemarkRequested)) forceRecord = true;                                    //  Adding an annotation while recording also adds a trackpoint (issue #213)

            if (gpsStatus == GPS_OK) {
                AsyncTODO ast = new AsyncTODO();

                // Distance Filter and Interval Filter in AND
                // The Trackpoint is recorded when both filters are True.
//                if ((isRecording) && ((prevRecordedFix == null)
//                        || (forceRecord)
//                        || (((loc.getTime() - prevRecordedFix.getTime()) >= (prefGPSinterval * 1000.0f))
//                        && (loc.distanceTo(prevRecordedFix.getLocation()) >= prefGPSdistance)))) {

                // Distance Filter and Interval Filter in OR
                // The Trackpoint is recorded when at less one filter is True.
                if ((isRecording && ((prevRecordedFix == null)
                        || (forceRecord)                                                                        // Forced to record the point
                        || ((prefGPSinterval == 0) && (prefGPSdistance == 0))                                   // No filters enabled --> it records all the points
                        || ((prefGPSinterval > 0)
                        && (prefGPSdistance > 0)                                                            // Both filters enabled, check conditions in OR
                        && (((loc.getTime() - prevRecordedFix.getTime()) >= (prefGPSinterval * 1000.0f))
                        || (loc.distanceTo(prevRecordedFix.getLocation()) >= prefGPSdistance)))
                        || ((prefGPSinterval > 0)
                        && (prefGPSdistance == 0)                                                           // Only interval filter enabled
                        && ((loc.getTime() - prevRecordedFix.getTime()) >= (prefGPSinterval * 1000.0f)))
                        || ((prefGPSinterval == 0)
                        && (prefGPSdistance > 0)                                                            // Only distance filter enabled
                        && ((loc.distanceTo(prevRecordedFix.getLocation()) >= prefGPSdistance)))
                        || (currentTrack.getNumberOfLocations() == 0)))                                         // It is the first point of a track
                        || (isForcedTrackpointsRecording)){                                                     // recording button is long pressed

                    if (isForcedTrackpointsRecording) {
                        Vibrator vibrator;
                        vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                        vibrator.vibrate(150);
                    }

                    prevRecordedFix = eloc;
                    ast.taskType = TASK_ADDLOCATION;
                    ast.location = eloc;
                    asyncTODOQueue.add(ast);
                    isPrevFixRecorded = true;
                } else {
                    ast.taskType = TASK_UPDATEFIX;
                    ast.location = eloc;
                    asyncTODOQueue.add(ast);
                    isPrevFixRecorded = false;
                }
                if (isPlacemarkRequested) {
                    currentPlacemark = new LocationExtended(loc);
                    currentPlacemark.setNumberOfSatellites(getNumberOfSatellitesTotal());
                    currentPlacemark.setNumberOfSatellitesUsedInFix(getNumberOfSatellitesUsedInFix());
                    isPlacemarkRequested = false;
                    EventBus.getDefault().post(EventBusMSG.UPDATE_TRACK);
                    if (!isQuickPlacemarkRequest) {
                        // Shows the dialog for placemark creation
                        EventBus.getDefault().post(EventBusMSG.REQUEST_ADD_PLACEMARK);
                    } else {
                        // Create a placemark, with an empty description, without showing the dialog
                        setPlacemarkDescription("");
                        EventBus.getDefault().post(EventBusMSG.ADD_PLACEMARK);
                    }
                }
                prevFix = eloc;
                isFirstFixFound = true;
            }
        }
    }



    public  String getCacheCsvPath() {
       //File tempDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
       // String ret =  getBaseContext() .getCacheDir() .getAbsolutePath()+ "/Tracks";
        String ret= null;
        try {
            ret = getCacheDir().getCanonicalPath() + "/Tracks";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ret;
    }


    public  String getCacheLogPath() {
        String ret= null;
        try {
            ret = getCacheDir().getCanonicalPath() + "/Logs";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ret;
    }


    public TDeviceType getDeviceType() {
        return dmdViewModel.GetDeviceType();
    }

    private boolean prefShowGraph = true;                         // Show the graph in the main activity
    public boolean getPrefShowGraph() {
        prefShowGraph = sharedPref.getBoolean("showgraph", true);
        return prefShowGraph;                                                   // Show the graph in the main activity
    }
    private boolean prefRotateGraph=true;                         // Rotate the graph in the main activity
    public boolean getPrefRotateGraph() {
        prefRotateGraph = sharedPref.getBoolean("rotategraph", true);
        return prefRotateGraph;                                                  // Rotate the graph in the main activity
    }

    private View view;
    private ActivityMainBinding binding;

    private final FileOpener fopen;
    private DmdViewModel dmdViewModel;
    private LollyService lolly;
    private final String TAG = "TOMST";


    private static LollyActivity singleton;
    public static LollyActivity getInstance(){
        return singleton;
    }

    public LollyActivity(){
        fopen = new FileOpener(this);
    }

    private ServiceConnection connection = new ServiceConnection() {
        private boolean bound;

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            LollyService.LollyBinder lollyBinder =
                    (LollyService.LollyBinder) binder;
            lolly = lollyBinder.getOdometer();
            bound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bound = false;
        }
    };

    // Request location
    public Location getLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
        }
        return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    }


    public boolean checkPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            //Android is 11(R) or above
            return Environment.isExternalStorageManager();
        }
        else{
            //Android is below 11(R)
            int write = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int read = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

            return write == PackageManager.PERMISSION_GRANTED && read == PackageManager.PERMISSION_GRANTED;
        }
    }


    @Override
    public void onClick(View v) {
        if (!checkPermission()) {
            Snackbar.make(view,"requesting permission", Snackbar.LENGTH_LONG).show();
            requestPermission();
        }
    }

    private void requestPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            //Android is 11(R) or above
            try {
                Log.d(Constants.TAG, "requestPermission: try");

                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", this.getPackageName(), null);
                intent.setData(uri);
                storageActivityResultLauncher.launch(intent);
            }
            catch (Exception e){
                Log.e(Constants.TAG, "requestPermission: catch", e);
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                storageActivityResultLauncher.launch(intent);
            }
        }
        else {
            //Android is below 11(R)
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE
            );
        }
    }

    private void createFolder(){
        //get folder name
        String folderName = "test"; //folderNameEt.getText().toString().trim();

        //create folder using name we just input
        File file = new File(Environment.getExternalStorageDirectory() + "/" + folderName);
        //create folder
        boolean folderCreated = file.mkdir();

        //show if folder created or not
        if (folderCreated) {
            Toast.makeText(this, "Folder Created....\n" + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Folder not created...", Toast.LENGTH_SHORT).show();
        }
    }

    private ActivityResultLauncher<Intent> storageActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Log.d(Constants.TAG, "onActivityResult: ");
                    //here we will handle the result of our intent
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                        //Android is 11(R) or above
                        if (Environment.isExternalStorageManager()){
                            //Manage External Storage Permission is granted
                            Log.d(Constants.TAG, "onActivityResult: Manage External Storage Permission is granted");
                            createFolder();
                        }
                        else{
                            //Manage External Storage Permission is denied
                            Log.d(Constants.TAG, "onActivityResult: Manage External Storage Permission is denied");
                            Toast.makeText(LollyActivity.this, "Manage External Storage Permission is denied", Toast.LENGTH_SHORT).show();
                        }
                    }
                    else {
                        //Android is below 11(R)
                    }
                }
            }
    );

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE){
            if (grantResults.length > 0){
                //check each permission if granted or not
                boolean write = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean read = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                if (write && read){
                    //External Storage permissions granted
                    Log.d(Constants.TAG, "onRequestPermissionsResult: External Storage permissions granted");
                    createFolder();
                }
                else{
                    //External Storage permission denied
                    Log.d(Constants.TAG, "onRequestPermissionsResult: External Storage permission denied");
                    Toast.makeText(this, "External Storage permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    public void createPrivateFolders() {
        // csv
        File sd = new File(DIRECTORY_TEMP);
        if (!sd.exists()) {
            if (sd.mkdir()) Log.w("myApp", "[#] GPSApplication.java - Folder created: " + sd.getAbsolutePath());
            else Log.w("myApp", "[#] GPSApplication.java - Unable to create the folder: " + sd.getAbsolutePath());
        } else Log.w("myApp", "[#] GPSApplication.java - Folder exists: " + sd.getAbsolutePath());

        // male obrazky grafu ?
        sd = new File(getApplicationContext().getFilesDir() + "/Thumbnails");
        if (!sd.exists()) {
            if (sd.mkdir()) Log.w("myApp", "[#] GPSApplication.java - Folder created: " + sd.getAbsolutePath());
            else Log.w("myApp", "[#] GPSApplication.java - Unable to create the folder: " + sd.getAbsolutePath());
        } else Log.w("myApp", "[#] GPSApplication.java - Folder exists: " + sd.getAbsolutePath());

        sd = new File(DIRECTORY_FW);
        if (!sd.exists()) {
            if (sd.mkdir()) Log.w("myApp", "[#] GPSApplication.java - Folder created: " + sd.getAbsolutePath());
            else Log.w("myApp", "[#] GPSApplication.java - Unable to create the folder: " + sd.getAbsolutePath());
        } else Log.w("myApp", "[#] GPSApplication.java - Folder exists: " + sd.getAbsolutePath());

        sd = new File(DIRECTORY_LOGS);
        if (!sd.exists()) {
            if (sd.mkdir()) Log.w("myApp", "[#] GPSApplication.java - Folder created: " + sd.getAbsolutePath());
            else Log.w("myApp", "[#] GPSApplication.java - Unable to create the folder: " + sd.getAbsolutePath());
        } else Log.w("myApp", "[#] GPSApplication.java - Folder exists: " + sd.getAbsolutePath());

    }

    public boolean isExportFolderWritable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Context context = getApplicationContext();

            Uri uri = Uri.parse(prefExportFolder);
            Log.w("myApp", "[#] GPSApplication.java - isExportFolderWritable: " + prefExportFolder);

            final List<UriPermission> list = getApplicationContext().getContentResolver().getPersistedUriPermissions();
            for (final UriPermission item : list) {
                Log.w("myApp", "[#] GPSApplication.java - isExportFolderWritable check: " + item.getUri());
                if (item.getUri().equals(uri)) {
                    try {
                        DocumentFile pickedDir;
                        if (prefExportFolder.startsWith("content")) {
                            pickedDir = DocumentFile.fromTreeUri(getApplicationContext(), uri);
                        } else {
                            pickedDir = DocumentFile.fromFile(new File(prefExportFolder));
                        }

                        if ((pickedDir==null))
                        {
                            Log.w("myApp", "[#] GPSApplication.java - THE EXPORT FOLDER DOESN'T EXIST");
                            return false;
                        }
                        if (!pickedDir.exists()) {
                            Log.w("myApp", "[#]");
                            return false;
                        }
                        /*
                        if ((pickedDir == null) || (!pickedDir.exists())) {
                            Log.w("myApp", "[#] GPSApplication.java - THE EXPORT FOLDER DOESN'T EXIST");
                            return false;
                        }
                         */

                        if ((!pickedDir.canRead()) || !pickedDir.canWrite()) {
                            Log.w("myApp", "[#] GPSApplication.java - CANNOT READ/WRITE INTO THE EXPORT FOLDER");
                            return false;
                        }
                        return true;
                    } catch (IllegalArgumentException e) {
                        Log.w("myApp", "[#] GPSApplication.java - IllegalArgumentException - isExportFolderWritable = FALSE: " + item.getUri());
                    }
                }
                // Releases the unused persistable permission
                getApplicationContext().getContentResolver().releasePersistableUriPermission(item.getUri(),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }
            Log.w("myApp", "[#] GPSApplication.java - isExportFolderWritable = FALSE");
            return false;
        } else {
            // Old Android 4, check that the app has the storage permission and the folder /GPSLogger exists.
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                File sd = new File(prefExportFolder);
                if (!sd.exists()) {
                    return sd.mkdir();
                } else return true;
            }
            return false;
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(LollyActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    // for user authentication
    FirebaseAuth auth;
    FirebaseUser user;

    // service binding
    public void startAndBindService() {
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
        (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            Intent intent = new Intent(this, LollyService.class);
//            startService(intent);
            bindService(intent, connection, Context.BIND_AUTO_CREATE);
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        //Intent intent = new Intent(this, LollyService.class);
        //bindService(intent, connection, Context.BIND_AUTO_CREATE);
        startAndBindService();

    //    dmdViewModel.sendMessageToFragment("Hello from MainActivity");

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d("LollyActivity", "onNewIntent called with intent action: " + intent.getAction());
    }


    // The Handler that try to enable location updates after a time delay.
    // It is used when the GPS provider is not available, to periodically check
    // if there is a new one available (for example when a Bluetooth GPS antenna is connected)
    private final Handler enableLocationUpdatesHandler = new Handler();
    private final Runnable enableLocationUpdatesRunnable = new Runnable() {
        @Override
        public void run() {
            setGPSLocationUpdates(false);
            setGPSLocationUpdates(true);
        }
    };

    private final Satellites satellites = new Satellites();      // The class that contains all the information about satellites
    private boolean isScreenOn                  = true;          // True if the screen of the device is ON


    /**
     * Updates the GPS Status for legacy Androids.
     */
    public void updateGPSStatus() {
        try {
            if ((locationManager != null) && (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                satellites.updateStatus(locationManager.getGpsStatus(null));
                numberOfSatellitesTotal = satellites.getNumSatsTotal();
                numberOfSatellitesUsedInFix = satellites.getNumSatsUsedInFix();
            } else {
                numberOfSatellitesTotal = NOT_AVAILABLE;
                numberOfSatellitesUsedInFix = NOT_AVAILABLE;
            }
        } catch (NullPointerException e) {
            numberOfSatellitesTotal = NOT_AVAILABLE;
            numberOfSatellitesUsedInFix = NOT_AVAILABLE;
            //Log.w("myApp", "[#] GPSApplication.java - updateSats: Caught NullPointerException: " + e);
        }
        if (gpsStatus != GPS_OK) {
            if (isScreenOn) EventBus.getDefault().post(EventBusMSG.UPDATE_FIX);
            //Log.w("myApp", "[#] GPSApplication.java - updateSats: Used=" + numberOfSatellitesUsedInFix + " Total=" + numberOfSatellitesTotal);
        }
        //Log.w("myApp", "[#] GPSApplication.java - updateSats: Total=" + _NumberOfSatellites + " Used=" + _NumberOfSatellitesUsedInFix);
    }


    /**
     * Updates the GPS Status for new Androids (Build.VERSION_CODES >= N).
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void updateGNSSStatus(android.location.GnssStatus status) {
        try {
            if ((locationManager != null) && (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                satellites.updateStatus(status);
                numberOfSatellitesTotal = satellites.getNumSatsTotal();
                numberOfSatellitesUsedInFix = satellites.getNumSatsUsedInFix();
            } else {
                numberOfSatellitesTotal = NOT_AVAILABLE;
                numberOfSatellitesUsedInFix = NOT_AVAILABLE;
            }
        } catch (NullPointerException e) {
            numberOfSatellitesTotal = NOT_AVAILABLE;
            numberOfSatellitesUsedInFix = NOT_AVAILABLE;
            //Log.w("myApp", "[#] GPSApplication.java - updateSats: Caught NullPointerException: " + e);
        }
        if (gpsStatus != GPS_OK) {
            if (isScreenOn) EventBus.getDefault().post(EventBusMSG.UPDATE_FIX);
            //Log.w("myApp", "[#] GPSApplication.java - updateSats: Used=" + numberOfSatellitesUsedInFix + " Total=" + numberOfSatellitesTotal);
        }
        //Log.w("myApp", "[#] GPSApplication.java - updateSats: Total=" + _NumberOfSatellites + " Used=" + _NumberOfSatellitesUsedInFix);
    }



    /**
     * The Class that manages the GPS Status, using the appropriate methods
     * depending on the Android Version.
     * - For VERSION_CODES > N it uses the new GnssStatus.Callback;
     * - For older Android it uses the legacy GpsStatus.Listener;
     */
    private class MyGPSStatus {
        private GpsStatus.Listener gpsStatusListener;
        private GnssStatus.Callback mGnssStatusListener;

        public MyGPSStatus() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mGnssStatusListener = new GnssStatus.Callback() {
                    @Override
                    public void onSatelliteStatusChanged(@NonNull GnssStatus status) {
                        super.onSatelliteStatusChanged (status);
                        updateGNSSStatus(status);
                    }
                };
            } else {
                gpsStatusListener = new GpsStatus.Listener() {
                    @Override
                    public void onGpsStatusChanged(int event) {
                        switch (event) {
                            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                                updateGPSStatus();
                                break;
                        }
                    }
                };
            }
        }

        /**
         * Enables the GPS Status listener
         */
        public void enable() {
            if (ContextCompat.checkSelfPermission(LollyActivity.getInstance(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) locationManager.registerGnssStatusCallback(mGnssStatusListener);
                else locationManager.addGpsStatusListener(gpsStatusListener);
            }
        }

        /**
         * Disables the GPS Status listener
         */
        public void disable() {
            if (ContextCompat.checkSelfPermission(LollyActivity.getInstance(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) locationManager.unregisterGnssStatusCallback(mGnssStatusListener);
                else locationManager.removeGpsStatusListener(gpsStatusListener);
            }
        }
    }




    private MyGPSStatus gpsStatusListener;                       // The listener for the GPS Status changes events

    public boolean isRecording() {
        return isRecording;
    }
    /**
     * Enables / Disables the GPS Location Updates
     *
     * @param state Tne state of GPS Location Updates: true = enabled; false = disabled.
     */
    public void setGPSLocationUpdates (boolean state) {
        enableLocationUpdatesHandler.removeCallbacks(enableLocationUpdatesRunnable);

        if (!state && !isRecording() && isGPSLocationUpdatesActive
                && (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            gpsStatus = GPS_SEARCHING;
            gpsStatusListener.disable();
            locationManager.removeUpdates(this);
            isGPSLocationUpdatesActive = false;
            //Log.w("myApp", "[#] GPSApplication.java - setGPSLocationUpdates = false");
        }
        if (state && !isGPSLocationUpdatesActive
                && (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            boolean enabled = false;
            try {
                //throw new IllegalArgumentException();
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, prefGPSupdatefrequency, 0, this); // Requires Location update
                enabled = true;
            } catch (IllegalArgumentException e) {
                gpsStatus = GPS_OUTOFSERVICE;
                enableLocationUpdatesHandler.postDelayed(enableLocationUpdatesRunnable, 1000);  // Starts the switch-off handler (delayed by HandlerTimer)
                Log.w("myApp", "[#] GPSApplication.java - unable to set GPSLocationUpdates: GPS_PROVIDER not available");
            }
            if (enabled) {
                // The location updates are active!
                gpsStatusListener.enable();
                isGPSLocationUpdatesActive = true;
                Log.w("myApp", "[#] GPSApplication.java - setGPSLocationUpdates = true");
                if (prefGPSupdatefrequency >= 1000)
                    numberOfStabilizationSamples = (int) Math.ceil(STABILIZER_TIME / prefGPSupdatefrequency);
                else numberOfStabilizationSamples = (int) Math.ceil(STABILIZER_TIME / 1000);
            }
        }
    }


    // The Handler that switches off the location updates after a time delay:
    private final Handler disableLocationUpdatesHandler = new Handler();
    private final Runnable disableLocationUpdatesRunnable = new Runnable() {
        @Override
        public void run() {
            setGPSLocationUpdates(false);
        }
    };

    public void setHandlerTime(int handlerTime) {
        this.handlerTime = handlerTime;
    }

    public int getHandlerTime() {
        return handlerTime;
    }

    // ---------------------------------------------------------------------- Foreground Service

    Intent gpsServiceIntent;                            // The intent for GPSService
//    GPSService gpsService;                              // The Foreground Service that keeps the app alive in Background
    LollyService gpsService;
    boolean isGPSServiceBound = false;                  // True if the GPSService is bound

    private final ServiceConnection gpsServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
//            GPSService.LocalBinder binder = (GPSService.LocalBinder) service;
            LollyService.LollyBinder binder = (LollyService.LollyBinder) service;

            //gpsService = binder.getServiceInstance();                     //Get instance of your service!
            gpsService = binder.getOdometer();
            Log.w("myApp", "[#] GPSApplication.java - GPSSERVICE CONNECTED - onServiceConnected event");
            isGPSServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.w("myApp", "[#] GPSApplication.java - GPSSERVICE DISCONNECTED - onServiceDisconnected event");
            isGPSServiceBound = false;
        }
    };


    /**
     * Stops and Unbinds to the Foreground Service GPSService
     */
    public void stopAndUnbindGPSService() {
        try {
            unbindService(gpsServiceConnection);                                        //Unbind to the service
            Log.w("myApp", "[#] GPSApplication.java - Service unbound");
        } catch (Exception e) {
            Log.w("myApp", "[#] GPSApplication.java - Unable to unbind the GPSService");
        }
        try {
            stopService(gpsServiceIntent);                                                  //Stop the service
            Log.w("myApp", "[#] GPSApplication.java - Service stopped");
        } catch (Exception e) {
            Log.w("myApp", "[#] GPSApplication.java - Unable to stop GPSService");
        }
    }

    private boolean mustUpdatePrefs             = true;          // True if preferences needs to be updated
    private boolean isBackgroundActivityRestricted;              // True if the App is Background Restricted
    private boolean isBatteryOptimisedWarningVisible = true;     // True if the App shows the warning when the battery optimisation is active

    /**
     * Updates the GPS Location update frequency, basing on the value of prefGPSupdatefrequency.
     * Set prefGPSupdatefrequency to a new value before calling this in order to change
     * frequency.
     */
    public void updateGPSLocationFrequency () {
        if (isGPSLocationUpdatesActive
                && (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            //Log.w("myApp", "[#] GPSApplication.java - updateGPSLocationFrequency");
            gpsStatusListener.disable();
            locationManager.removeUpdates(this);
            if (prefGPSupdatefrequency >= 1000) numberOfStabilizationSamples = (int) Math.ceil(STABILIZER_TIME / prefGPSupdatefrequency);
            else numberOfStabilizationSamples = (int) Math.ceil(STABILIZER_TIME / 1000);
            gpsStatusListener.enable();
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, prefGPSupdatefrequency, 0, this);
        }
    }

    /**
     * (re-)Loads the Preferences and Launch signals in order to updates the UI.
     */
    private void LoadPreferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = preferences.edit();

        // -----------------------
        // TODO: Uncomment it to test the conversion of prefUMSpeed into prefUMOfSpeed (For Test Purpose)
        //editor.putString("prefUMSpeed", "0").commit();
        // -----------------------

        prefUM = Integer.parseInt(preferences.getString("prefUM", "0"));

        // Conversion from the previous versions of the unit of measurement of the speeds
        if (preferences.contains("prefUMSpeed")) {       // The old setting
            Log.w("myApp", "[#] GPSApplication.java - Old setting prefUMSpeed present (" + preferences.getString("prefUMSpeed", "0") + "). Converting to new preference prefUMOfSpeed.");
            String UMspd = preferences.getString("prefUMSpeed", "0");
            switch (prefUM) {
                case PhysicalDataFormatter.UM_METRIC:
                    editor.putString("prefUMOfSpeed", (UMspd.equals("0") ? String.valueOf(PhysicalDataFormatter.UM_SPEED_MS) : String.valueOf(PhysicalDataFormatter.UM_SPEED_KMH)));
                    break;
                case PhysicalDataFormatter.UM_IMPERIAL:
                    editor.putString("prefUMOfSpeed", (UMspd.equals("0") ? String.valueOf(PhysicalDataFormatter.UM_SPEED_FPS) : String.valueOf(PhysicalDataFormatter.UM_SPEED_MPH)));
                    break;
                case PhysicalDataFormatter.UM_NAUTICAL:
                    editor.putString("prefUMOfSpeed", (UMspd.equals("0") ? String.valueOf(PhysicalDataFormatter.UM_SPEED_KN) : String.valueOf(PhysicalDataFormatter.UM_SPEED_MPH)));
                    break;
            }
            editor.remove("prefUMSpeed");
            editor.commit();
        } else prefUMOfSpeed = Integer.parseInt(preferences.getString("prefUMOfSpeed", "1"));

        // Remove the prefIsStoragePermissionChecked in preferences if present
        if (preferences.contains("prefIsStoragePermissionChecked")) {
            editor.remove("prefIsStoragePermissionChecked");
            editor.commit();
        }

        //prefKeepScreenOn = preferences.getBoolean("prefKeepScreenOn", true);
        prefGPSWeekRolloverCorrected = preferences.getBoolean("prefGPSWeekRolloverCorrected", false);
        prefShowDecimalCoordinates = preferences.getBoolean("prefShowDecimalCoordinates", false);
        prefShowLocalTime = preferences.getBoolean("prefShowLocalTime", true);

        try {
            prefGPSdistance = Float.parseFloat(preferences.getString("prefGPSdistance", "0"));
        }
        catch(NumberFormatException nfe) {
            prefGPSdistance = 0;
        }
        try {
            prefGPSinterval = Float.parseFloat(preferences.getString("prefGPSinterval", "0"));
        }
        catch(NumberFormatException nfe) {
            prefGPSinterval = 0;
        }

        Log.w("myApp", "[#] GPSApplication.java - prefGPSdistance = " + prefGPSdistance + " m");

        prefEGM96AltitudeCorrection = preferences.getBoolean("prefEGM96AltitudeCorrection", false);
        prefAltitudeCorrection = Double.parseDouble(preferences.getString("prefAltitudeCorrection", "0"));
        Log.w("myApp", "[#] GPSApplication.java - Manual Correction set to " + prefAltitudeCorrection + " m");
        prefExportKML = preferences.getBoolean("prefExportKML", true);
        prefExportGPX = preferences.getBoolean("prefExportGPX", true);
        prefExportTXT = preferences.getBoolean("prefExportTXT", false);
        prefKMLAltitudeMode = Integer.parseInt(preferences.getString("prefKMLAltitudeMode", "1"));
        prefGPXVersion = Integer.parseInt(preferences.getString("prefGPXVersion", "100"));               // Default value = v.1.0
        prefShowTrackStatsType = Integer.parseInt(preferences.getString("prefShowTrackStatsType", "0"));
        prefShowDirections = Integer.parseInt(preferences.getString("prefShowDirections", "0"));

        double altcorm = Double.parseDouble(preferences.getString("prefAltitudeCorrection", "0"));
        double altcor = preferences.getString("prefUM", "0").equals("0") ? altcorm : altcorm * PhysicalDataFormatter.M_TO_FT;
        double distfilterm = Double.parseDouble(preferences.getString("prefGPSdistance", "0"));
        double distfilter = preferences.getString("prefUM", "0").equals("0") ? distfilterm : distfilterm * PhysicalDataFormatter.M_TO_FT;
        editor.putString("prefAltitudeCorrectionRaw", String.valueOf(altcor));
        editor.putString("prefGPSdistanceRaw", String.valueOf(distfilter));
        //editor.remove("prefGPSDistanceRaw");
        editor.commit();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) prefExportFolder = preferences.getString("prefExportFolder", "");
        else setPrefExportFolder(Environment.getExternalStorageDirectory() + "/GPSLogger");

        long oldGPSupdatefrequency = prefGPSupdatefrequency;
        prefGPSupdatefrequency = Long.parseLong(preferences.getString("prefGPSupdatefrequency", "1000"));

        // Update the GPS Update Frequency if needed
        if (oldGPSupdatefrequency != prefGPSupdatefrequency) updateGPSLocationFrequency();

        // If no Exportation formats are enabled, enable the GPX one
        if (!prefExportKML && !prefExportGPX && !prefExportTXT) {
            editor.putBoolean("prefExportGPX", true);
            editor.commit();
            prefExportGPX = true;
        }

        // Load EGM Grid if needed
        EGM96 egm96 = EGM96.getInstance();
        if (egm96 != null) {
            egm96.loadGrid(prefExportFolder, getApplicationContext().getFilesDir().toString());
        }

        // Request of UI Update
        EventBus.getDefault().post(EventBusMSG.APPLY_SETTINGS);
        EventBus.getDefault().post(EventBusMSG.UPDATE_FIX);
        EventBus.getDefault().post(EventBusMSG.UPDATE_TRACK);
        EventBus.getDefault().post(EventBusMSG.UPDATE_TRACKLIST);
    }

    @Subscribe
    public void onEvent(Short msg) {
        if (msg == EventBusMSG.NEW_TRACK) {
            AsyncTODO ast = new AsyncTODO();
            ast.taskType = TASK_NEWTRACK;
            ast.location = null;
            asyncTODOQueue.add(ast);
            return;
        }
        if (msg == EventBusMSG.ADD_PLACEMARK) {
            AsyncTODO ast = new AsyncTODO();
            ast.taskType = TASK_ADDPLACEMARK;
            ast.location = currentPlacemark;
            currentPlacemark.setDescription(placemarkDescription);
            asyncTODOQueue.add(ast);
            return;
        }
        if (msg == EventBusMSG.APP_PAUSE) {
            disableLocationUpdatesHandler.postDelayed(disableLocationUpdatesRunnable, getHandlerTime());  // Starts the switch-off handler (delayed by HandlerTimer)
            if ((currentTrack.getNumberOfLocations() == 0) && (currentTrack.getNumberOfPlacemarks() == 0)
                    && (!isRecording) && (!isPlacemarkRequested)) stopAndUnbindGPSService();
            System.gc();                                // Clear mem from released objects with Garbage Collector
            return;
        }
        if (msg == EventBusMSG.APP_RESUME) {
            isScreenOn = true;
            Log.w("myApp", "[#] GPSApplication.java - Received EventBusMSG.APP_RESUME");

            /*
            if (!asyncPrepareActionmodeToolbar.isAlive()) {
                asyncPrepareActionmodeToolbar = new AsyncPrepareActionmodeToolbar();
                asyncPrepareActionmodeToolbar.start();
            } else Log.w("myApp", "[#] GPSApplication.java - asyncPrepareActionmodeToolbar already alive");
            */

            disableLocationUpdatesHandler.removeCallbacks(disableLocationUpdatesRunnable);                 // Cancel the switch-off handler
            setHandlerTime(DEFAULT_SWITCHOFF_HANDLER_TIME);
            setGPSLocationUpdates(true);
            if (mustUpdatePrefs) {
                mustUpdatePrefs = false;
                LoadPreferences();
            }
            // startAndBindGPSService();
            startAndBindService();

            // Check if the App is Background Restricted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ActivityManager activityManager = (ActivityManager)getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
                if ((activityManager != null) && (activityManager.isBackgroundRestricted())) {
                    isBackgroundActivityRestricted = true;
                    Log.w("myApp", "[#] GPSApplication.java - THE APP IS BACKGROUND RESTRICTED!");
                } else {
                    isBackgroundActivityRestricted = false;
                }
            } else {
                isBackgroundActivityRestricted = false;
            }
            return;
        }
        if (msg == EventBusMSG.UPDATE_SETTINGS) {
            mustUpdatePrefs = true;
            return;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Log.d("LollyActivity", "onCreate called with intent action: " +
                (intent != null ? intent.getAction() : "null") +
                " - Instance: " + Integer.toHexString(System.identityHashCode(this)));

        singleton = this;
        location = LollyActivity.getInstance().getLocation();
 //       Log.w(TAG, "[#] LollyApplication.java - onCreate");

        // Initialize Firebase and crashlytics for collecting crash reports and analytics
        FirebaseApp.initializeApp(this);
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        view = binding.getRoot();


        // remove stupid line on bottom of action bar
        getSupportActionBar().setElevation(0);

        // create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "GPSLoggerServiceChannel",
                    getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setSound(null, null);
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setSound(null,null);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        //EventBus eventBus = EventBus.builder().addIndex(new EventBusIndex()).build();
        EventBus.builder().addIndex(new EventBusIndex()).installDefaultEventBus();
        EventBus.getDefault().register(this);


        //  TOAST_VERTICAL_OFFSET = (int)(75 * getResources().getDisplayMetrics().density);
        DIRECTORY_TEMP = getApplicationContext().getCacheDir() + "/Tracks";
        DIRECTORY_FW = getApplicationContext().getCacheDir() + "/Fw";
        DIRECTORY_LOGS = getApplicationContext().getCacheDir() + "/Logs";

        createPrivateFolders();
        gpsDataBase = new DatabaseHandler(this);

        Context context = getApplicationContext();
        sharedPref= context.getSharedPreferences(getString(R.string.save_options), context.MODE_PRIVATE);
        prefExportFolder = sharedPref.getString("prefExportFolder", "");
        if (!prefExportFolder.isEmpty()) {
            if (!isExportFolderWritable())
                Toast.makeText(context, "Export folder is not writable!", Toast.LENGTH_SHORT).show();
            //   prefExportFolder = "";  // pokud neexistuje, tak se nastavi na prazdny retezec (
            //prefExportFolder = Environment.DIRECTORY_DOWNLOADS;)
        }
       prefShowGraph = sharedPref.getBoolean("prefShowGraph", true);
       prefRotateGraph = sharedPref.getBoolean("prefRotateGraph", true);


 //       Intent intent = getIntent();
        String action = intent.getAction();

        if (action != null)
        {
            switch (intent.getAction()) {
                case Intent.ACTION_GET_CONTENT:
                    fopen.isRequestDocument = true;
                    setResult(RESULT_CANCELED);
                    break;
                case Intent.ACTION_OPEN_DOCUMENT: {
                    fopen.isRequestDocument = true;
                    setResult(RESULT_CANCELED);
                    break;
                }
                default:
                    fopen.isRequestDocument = false;
            }
        }

        //checkPermission();
        if (!checkPermission()) {
            requestPermission();
        }

        // sdileny datovy modul
        dmdViewModel = new ViewModelProvider(this).get(DmdViewModel.class);

        //BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_graph, R.id.navigation_notifications, R.id.navigation_options)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    public void sendMessageToHomeFragment(String message) {
        dmdViewModel.sendMessageToFragment(message);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu,menu);
        return true;
    }

    private void switchToSettingsFragment(){
        BottomNavigationView bottomNavigationView;
        bottomNavigationView = (BottomNavigationView) binding.navView;
        View view = bottomNavigationView.findViewById(R.id.navigation_options);
        view.performClick();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if  (id ==R.id.action_settings) {
            switchToSettingsFragment();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }






}