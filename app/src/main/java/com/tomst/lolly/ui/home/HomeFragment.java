package com.tomst.lolly.ui.home;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.tomst.lolly.LollyApplication;
import com.tomst.lolly.LollyService;
import com.tomst.lolly.R;
import com.tomst.lolly.core.CSVReader;
import com.tomst.lolly.core.Constants;
import com.tomst.lolly.core.DmdViewModel;
import com.tomst.lolly.core.FileOperation;
import com.tomst.lolly.core.PermissionManager;
import com.tomst.lolly.core.TDevState;
import com.tomst.lolly.core.TInfo;
import com.tomst.lolly.core.TMereni;
import com.tomst.lolly.core.TMeteo;
import com.tomst.lolly.databinding.FragmentHomeBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class HomeFragment extends Fragment {
    LollyApplication lollyApp = LollyApplication.getInstance();

    static final byte MIN_ADANUMBER = 5;  // pozaduju, aby mel adapter minimalne 5 znaku
    private FragmentHomeBinding binding;
    private long MaxPos;

    private DocumentFile kmlFile;
    private DocumentFile gpxFile;
    private DocumentFile txtFile;
    private CSVReader csv;

    private int heartIdx = 0;
    private char cHeart = '-';
    private int DevCount =-1;
    private boolean uart_configured = false;

    private int currentIndex = -1;

    private String serialNumber = "Unknown";

    private final int openIndex = 0;

    private boolean bReadThreadGoing=false;

    public com.tomst.lolly.core.uHer fHer;

    private Handler progressBarHandler = new Handler(Looper.getMainLooper());

    protected Handler datahandler = new Handler(Looper.getMainLooper()) {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void handleMessage(Message msg) {
            TMereni mer = (TMereni) msg.obj;
            dmd.AddMereni(mer);   // array of values for graph
            csv.AddMerToCsv(mer); // add to csv file
            //csv.AppendStat(mer);  // statistics, we'll omit this in the next version
        }
    };

    private int fAddr;
    private int progressBarStatus=0;

    private  DmdViewModel dmd;

    private final int PERMISSION_REQUEST_CODE = 698;
    private final int NOTIFICATION_ID = 423;
    private PermissionManager permissionManager;

    private boolean bound = false;
    private LollyService odometer;

    private FirebaseFirestore db;
    private TextView dataTextView;
    private Button viewDataButton;
    private void getData()
    {
        // initialize instance of cloud firestore
        db = FirebaseFirestore.getInstance();

        // get user data
        db.collection("users").get()
                .addOnCompleteListener(getActivity(), new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        // check if getting data was successful
                        if (task.isSuccessful())
                        {
                            // create a string builder
                            StringBuilder userData = new StringBuilder();

                            // loop through all the data
                            for (QueryDocumentSnapshot document: task.getResult())
                            {
                                // get user data
                                String userName = document.getString("name");
                                Long userSalary = document.getLong("salary");

                                // create user data list
                                if (userName != null && userSalary != null)
                                {
                                    userData.append("Name: ").append(userName)
                                            .append(", Salary: ").append(userSalary)
                                            .append("\n");
                                }

                                Log.d("dbUsers", "onComplete: " + document.getData());
                            }

                            // display the name for each user
                            dataTextView.setText(userData.toString());
                        }
                        else
                        {
                            // display error
                            dataTextView.setText("Error getting data: " + task.getException().getMessage( ));
                            Log.d("dbUsers", "onComplete: " + task.getException().getMessage());
                        }
                    }
                });
    }

    // load native C library
    static {
        System.loadLibrary("lolly-backend-lib");
    }

    public native String getExampleStringJNI();

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Log.i("| DEBUG |", "Home Fragment, right above jni string");

        Log.i("| DEBUG |", getExampleStringJNI());
    }

    private ServiceConnection connection = new ServiceConnection() {
       @Override
       public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
           LollyService.LollyBinder odometerBinder =
                   (LollyService.LollyBinder) iBinder;
           odometer = odometerBinder.getOdometer();
           odometer.SetHandler(handler);
           odometer.SetDataHandler(datahandler);   // do tohoto handleru posilam naparsovane data
           odometer.SetContext(getContext());      // az tady muze startovat hardware
           odometer.startBindService();
           bound = true;
       }


       @Override
       public void onServiceDisconnected(ComponentName componentName) {
           bound = false;
       }
    };

     @Override
     public void onResume() {
        super.onResume();

        if (odometer != null) {
            odometer.SetServiceState(TDevState.tStart);

        }
         // Set the orientation to landscape (90 degrees)
         getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //ConnectDevice();
     };

     @Override
     public void onStart(){
        Constants.showMicro = true;

        super.onStart();

        Intent intent = new Intent(getContext(), LollyService.class);
        //getActivity().startService(intent);
        getContext().bindService(intent, connection, Context.BIND_AUTO_CREATE);
     }

    /*
     public void LogMsg(String msg)
    {
        binding.mShowCount.append(msg+"\n");
    }
     */

     private void switchToGraphFragment(){
        BottomNavigationView bottomNavigationView;
        bottomNavigationView = (BottomNavigationView) getActivity().findViewById(R.id.nav_view);
        View view = bottomNavigationView.findViewById(R.id.navigation_graph);
        view.performClick();
     }

     /*
    public static void downloadFile(Context context, String url, String fileName) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url))
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                .setTitle(fileName)
                .setDescription("Downloading...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        downloadManager.enqueue(request);
    }
     */

    private String FullName(String AFileName){
        File[] rootDirectories = FileOperation.getAllStorages(getContext());
        //return FILEPATH+AFileName;
         return Constants.FILEDIR+AFileName;
     }

    // 2024-04-24_92225141_0.csv
    // vyrobi testovaci soubor s daty, chci vyzkouset, jestli mi hraje zapis na dane misto
    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean CreateTestFile(String saveIntoFolder) {
        try {
            DocumentFile pickedDir;
            if (saveIntoFolder.startsWith("content")) {
                Uri uri = Uri.parse(saveIntoFolder);
                pickedDir = DocumentFile.fromTreeUri(getContext(), uri);
            } else {
                pickedDir = DocumentFile.fromFile(new File(saveIntoFolder));
            }

            if (!pickedDir.exists()) {
                Log.w("myApp", "[#] Exporter.java - UNABLE TO CREATE THE FOLDER");
                //exportingTask.setStatus(ExportingTask.STATUS_ENDED_FAILED);
                return false;
            }

            String fName = "testfile";
            txtFile = pickedDir.findFile(fName + ".txt");
            if ((txtFile != null) && (txtFile.exists()))
                txtFile.delete();
            txtFile = pickedDir.createFile("", fName + ".txt");
            Log.w("myApp", "[#] HomeFragment.java - Export " + txtFile.getUri().toString());

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // 2024-04-24_92225141_0.csv
    @RequiresApi(api = Build.VERSION_CODES.O)
    private String CompileFileName(String Serial, String ADir){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd").withZone(ZoneId.of("UTC"));
        LocalDateTime localDateTime = LocalDateTime.now();

        int idx=0;
        boolean filex = true;
        String locFile = null;
        String fmtdate = localDateTime.format(formatter);

        //CreateTestFile(ADir);

        try {
            DocumentFile pickedDir;
            if (ADir.startsWith("content")) {
                Uri uri = Uri.parse(ADir);
                pickedDir = DocumentFile.fromTreeUri(getContext(), uri);
            } else {
                pickedDir = DocumentFile.fromFile(new File(ADir));
            }

            // katastrofa, nemuzu vytvorit adresar
            if (!pickedDir.exists()) {
                Log.w("myApp", "[#] Exporter.java - UNABLE TO CREATE THE FOLDER");
                //exportingTask.setStatus(ExportingTask.STATUS_ENDED_FAILED);
                return null;
            }

            //String fName = "testfile";
            filex = true;
            Integer i = 0;
            while ((filex == true) && (i<100)) {
                //locFile = ADir + "//data_"+Serial+"_"+fmtdate+"_"+ Integer.valueOf(i)+".csv";
                locFile = "data_"+Serial+"_"+fmtdate+"_"+Integer.valueOf(i)+".csv";
                txtFile = pickedDir.findFile(locFile);
                // soubor uz existuje, neprepisuju, ale pridam index na konci souboru

                if ((txtFile ==null) || (!txtFile.exists()))
                    filex = false;
                else
                    i++;
            }
          //  txtFile = pickedDir.createFile("", locFile);
         //   Log.w("myApp", "[#] HomeFragment.java - Export " + txtFile.getUri().toString());


        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return locFile;
    }

    private void setMeteoImage(ImageView img, TMeteo met)
    {
        switch (met){
            case mBasic:
                //img.setImageResource(R.drawable.basic);
                img.setImageResource(R.drawable.home_basic);
                break;

            case mMeteo:
                //img.setImageResource(R.drawable.meteo);
                img.setImageResource(R.drawable.home_meteo);
                break;

            case mSmart:
                //img.setImageResource(R.drawable.smart);
                img.setImageResource(R.drawable.home_smart);
                break;

            case mIntensive:
                //img.setImageResource(R.drawable.a5);
                img.setImageResource(R.drawable.home_5min);
                break;

            case mExperiment:
                //img.setImageResource(R.drawable.a1);
                img.setImageResource(R.drawable.home_1min);
                break;

            default:
                img.setImageResource(R.drawable.shape_circle);
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private String FormatInstant(Instant value){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DEVICE_FORMAT)
                .withZone(ZoneId.systemDefault());

        String result = formatter.format(value);
        return(result);
    }

    protected Handler handler = new Handler(Looper.getMainLooper()) {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void handleMessage(Message msg) {
            if (binding == null) {
                Log.d(Constants.TAG, "binding is null, clean better !");
                return;
            }

            TInfo info = (TInfo) msg.obj;
            Log.d(Constants.TAG,String.valueOf(info.idx)+' '+info.msg);

            // tady rozebiram vystupy ze stavu v threadu
            switch(info.stat){

                case tNoHardware:
                    binding.proMessage.setText("NO HARDWARE !!!");
                    break;

                case tWaitForAdapter:
                    // if (ftTMS.AdapterNumber.length()>MIN_ADANUMBER) ;
                    if (info.msg.length()>MIN_ADANUMBER); // tady bude zobrazeni cisla adapteru
                    break;

                case tHead:
                    //binding.devser.setText(ftTMS.SerialNumber);
                    //binding.devser.setText(info.msg); // zapis hw.fw firmware
                    break;

                case tSerial:
                    serialNumber = info.msg;
                    binding.devser.setText(info.msg);

                    // nakompiluj jmeno souboru pro zapis, snazime se o unikatni nazev
                   String  ADir = LollyApplication.getInstance().getCacheDirectoryPath();
                   String AFileName = CompileFileName(info.msg,ADir);
                   AFileName = ADir + "/" + AFileName;
                    csv = new CSVReader(AFileName);
                    csv.OpenForWrite();  // otevre vystupni stream pro addCsv vyse
                    break;

                case tInfo:
                    binding.devhumAD.setText(String.valueOf(info.humAd));
                    binding.devt1.setText(String.valueOf(info.t1));
                    binding.devt2.setText(String.valueOf(info.t2));
                    binding.devt3.setText(String.valueOf(info.t3));
                    break;

                case tCapacity:
                    // kapacita je v %
                    int capUsed = Integer.parseInt(info.msg);
                    binding.devMemory.setProgress(capUsed);
                    break;

                case tGetTime:
                    String devTime = info.msg; // cas v lizatku
                    if (info.msg.length() <1)
                        devTime = "Invalid time, check PCF8563";   // pokud neni cas, tak nula

                    binding.devTime.setText(devTime);
                    break;

                case tGetTimeError:
                    //String devTime = info.msg;
                    binding.devTime.setText("Invalid time");
                    String line = String.format("Invalid time %s",info.msg);
                    binding.proMessage.setText(line);
                    break;

                case tCompareTime:
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DEVICE_FORMAT).withZone(ZoneId.systemDefault());
                    LocalDateTime localDateTime = LocalDateTime.now();
                    String phTime = localDateTime.format(formatter);
                    binding.phoneTime.setText(phTime);       // cas v telefonu

                    //String deltas = String.valueOf (ftTMS.delta /1000.0);
                    float delta = Float.valueOf(info.msg);
                    String deltas = String.format("%.1f", delta/1000.0);
                    binding.diffTime.setText(deltas);
                   break;

                case tReadMeteo:
                    // show meteo mode and image
                    ImageView img = (ImageView)  getActivity().findViewById(R.id.devImage);
                    setMeteoImage(img,info.meteo);
                    binding.devMode.setText(info.msg); // here is wordly description of mode
                    break;

                case tProgress:
                    // progress bar, slouceno s infem.
                    if (info.idx < 0)
                        binding.proBar.setMax(-info.idx);
                    else
                        binding.proBar.setProgress(info.idx);

                    //DateTimeFormatter buttonFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd"); //DateTimeFormatter.ofPattern(Constants.BUTTON_FORMAT);
                    if (info.currDay != null) {
                        DateTimeFormatter buttonFormat = DateTimeFormatter.ofPattern("YY-MM-dd").withZone(ZoneId.of("UTC"));
                        String sFmt = buttonFormat.format(info.currDay);
                        String s = String.format("%s rem:%d days",sFmt, info.remainDays);
                        binding.tvStatus.setText(s);
                    }

                    HandleHeartbeat();
                    //binding.tvStatus.setText(info.msg);
                    break;

                case tLollyService:
                    binding.proMessage.setText("LollyService.serviceHandler");
                    break;

                case tReadType:
                    //binding.devMode.setText(info.msg);
                    String rs = info.msg;
                    binding.proMessage.setText(rs);
                    break;

                case tRemainDays:
                    break;

                case tFinishedData:
                    csv.CloseExternalCsv();

                    // get option for showing graph
                    boolean showGraph = getContext()
                            .getSharedPreferences(
                                    "save_options",
                                    Context.MODE_PRIVATE
                            )
                            .getBoolean("showgraph", false);

                    if (true) {
                        // prepni se do Grafu
                        dmd.sendMessageToFragment("TMD " + serialNumber);
                        switchToGraphFragment();
                    }
                    break;

                default:
                   break;
            }
        }
    };


    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Perform additional view setup here, such as finding views by ID and setting up listeners
        binding.expPath.setText("Home Fragment onViewCreated");
    }


    @RequiresApi(api = Build.VERSION_CODES.O)

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        // sdileny datovy model
        dmd = new ViewModelProvider(getActivity()).get(DmdViewModel.class);
        dmd.ClearMereni();

        // tady vybiram callbacky od jinych fragmentu a aplikace
        dmd.getMessageContainerToFragment().observe(getViewLifecycleOwner(), message -> {
             String exportPath = lollyApp.getPrefExportFolder();
            //binding.expPath.setText(exportPath);
        });

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        binding.proBar.setProgress(0); // vycisti progress bar

        Button testLollyInstance = binding.testLolly;
        testLollyInstance.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                lollyApp = LollyApplication.getInstance();
                String exportPath = lollyApp.getPrefExportFolder();
                //binding.expPath.setText(exportPath);
            }
        });

        //final TextView textView = binding.mShowCount;
        //homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        // Opravneni - jak je spravne navrstvit ...
        permissionManager = new PermissionManager(getActivity());
        Context mContext = getContext();

        // testovaci crash button

        Button crashButton = binding.testCrash;
        crashButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                    throw new RuntimeException("Test Crash"); // Force a crash
            }
        });

        // nasimulu odeslani serioveho cisla z threadu
        Button sendSerial =binding.genSerial;
        sendSerial.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                odometer.SetState(TDevState.tSerial);
                // dmd.sendMessageToFragment("TSN");
                //Message message = handler.obtainMessage();//odometer.obtainMessage();
                //TInfo info = (TInfo) msg.obj;
                //message.obj = "Hello from HomeFragment";
                //handler.sendMessage(message);
            }
        });

        /*
        ftTMS = new TMSReader(mContext);
        ftTMS.ConnectDevice();
        ftTMS.SetHandler(handler);
        ftTMS.SetDataHandler(datahandler);
        ftTMS.SetBarListener(new OnProListener() {
            @Override
            public void OnProEvent(long Pos) {
                if (binding == null)
                    return;

                if (Pos < 0) {
                    binding.proBar.setMax((int) -Pos); // posledni adresa
                    MaxPos = -Pos;

                } else
                {
                    binding.proBar.setProgress((int) Pos);
                    int j = (int)(Pos/(double) MaxPos * 100);
                    String s = String.format("%d %%",j);
                    binding.tvStatus.setText(s);
                    HandleHeartbeat();  // otoci vrtuli
                }
            }
        });
        ftTMS.start();
        binding.mShowCount.setText("downloading");
        dmd.sendMessageToGraph("TMD"); // observer v GraphFragment vi, ze data byla vyctena pomoci TMD
        */

        // initialize UI elements
        //dataTextView = binding.getRoot().findViewById(R.id.dataTextView);
        //viewDataButton = binding.getRoot().findViewById(R.id.btnViewData);

        // set onclick listener for the button
        /*
        viewDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getData();
            }
        });
         */

        return root;
    }

    public void onViewDataButtonClick(View view) {
        getData();
    }

    // otoc vrtuli
    private void HandleHeartbeat(){
       switch (heartIdx) {
           case 0:
               heartIdx++;
               binding.tvHeartbeat.setText("\\");
               break;
           case 1:
               heartIdx++;
               binding.tvHeartbeat.setText("|");
               break;
           case 2:
               heartIdx++;
               binding.tvHeartbeat.setText("/");
               break;
           case 3:
               heartIdx=0;
               binding.tvHeartbeat.setText("-");
               break;
           default:
               heartIdx = 0;
               binding.tvHeartbeat.setText("\\");
               break;
       }
    }

    @Override
    public void onDestroyView() {
        //odometer.SetRunning(false);

        super.onDestroyView();

        binding = null;
    }
}