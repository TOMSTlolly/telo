package com.tomst.lolly;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.Manifest;
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
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
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

import com.google.android.material.snackbar.Snackbar;
import com.tomst.lolly.core.Constants;
import com.tomst.lolly.core.DatabaseHandler;
import com.tomst.lolly.core.FileOpener;
import com.tomst.lolly.core.TDeviceType;
import com.tomst.lolly.databinding.ActivityMainBinding;
import com.tomst.lolly.core.DmdViewModel;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.FirebaseApp;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

public class LollyApplication extends AppCompatActivity implements View.OnClickListener {

    SharedPreferences sharedPref = null;
    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final int STORAGE_PERMISSION_CODE = 100;

    public static String DIRECTORY_TEMP;                         // The directory to store temporary tracks = getCacheDir() + "/Tracks"
    public static String DIRECTORY_URI;              // The directory that contains the empty gpx and kml file = getFilesDir() + "/URI"
    public static String DIRECTORY_LOGS;              // The directory that contains the empty gpx and kml file = getFilesDir() + "/URI"

    // nastaveni preferovane cesty pro ukladani dat
    private String  prefExportFolder            = "";            // The folder for csv exportation
    public DatabaseHandler gpsDataBase;

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


    private static LollyApplication singleton;
    public static LollyApplication getInstance(){
        return singleton;
    }

    public LollyApplication(){
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
                            Toast.makeText(LollyApplication.this, "Manage External Storage Permission is denied", Toast.LENGTH_SHORT).show();
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

        sd = new File(DIRECTORY_URI);
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
        new AlertDialog.Builder(LollyApplication.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    // for user authentication
    FirebaseAuth auth;
    FirebaseUser user;

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, LollyService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    //    dmdViewModel.sendMessageToFragment("Hello from MainActivity");

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        singleton = this;

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

        //  TOAST_VERTICAL_OFFSET = (int)(75 * getResources().getDisplayMetrics().density);
        DIRECTORY_TEMP = getApplicationContext().getCacheDir() + "/Tracks";
        DIRECTORY_URI = getApplicationContext().getCacheDir() + "/URI";
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
       /*
        String sExportFolder = sharedPref.getString("prefExportFolder",",");
        File tsd = new File(sExportFolder);  // otevre adresar
        if (!tsd.exists()) tsd.mkdir();
        tsd = new File(DIRECTORY_TEMP);
        if (!tsd.exists()) {
            tsd.mkdir();
            if (tsd.exists()) Log.w(TAG, "[#] LollyApplication.java - Folder /Tracks OK");
            else Log.w(TAG, "[#] LollyApplication.java - Unable to create folder /Tracks");
           // Log.w(TAG, "[#] LollyApplication.java - " + (isGPSLoggerFolder ? "Folder /GPSLogger/AppData OK" : "Unable to create folder /GPSLogger/AppData"));
        }
         */

        // for user authentication
//        auth = FirebaseAuth.getInstance();
 //       user = auth.getCurrentUser();
        /*
        if (user == null)
        {
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
            finish();
        }
        */

        // permissionManager = new PermissionManager(this);

        Intent intent = getIntent();
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