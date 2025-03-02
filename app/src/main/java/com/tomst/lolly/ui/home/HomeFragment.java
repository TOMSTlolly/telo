package com.tomst.lolly.ui.home;

import static com.tomst.lolly.core.shared.CompileFileName;

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
import androidx.core.content.ContextCompat;
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
import com.tomst.lolly.core.RFirmware;
import com.tomst.lolly.core.TDevState;
import com.tomst.lolly.core.TDeviceType;
import com.tomst.lolly.core.TInfo;
import com.tomst.lolly.core.TMSRec;
import com.tomst.lolly.core.TMSSim;
import com.tomst.lolly.core.TMereni;
import com.tomst.lolly.core.TMeteo;
import com.tomst.lolly.core.shared;
import com.tomst.lolly.databinding.FragmentHomeBinding;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    LollyApplication lollyApp = LollyApplication.getInstance();

    static final byte MIN_ADANUMBER = 5;  // pozaduju, aby mel adapter minimalne 5 znaku
    private FragmentHomeBinding binding;

    private  DmdViewModel dmd;
    private final int PERMISSION_REQUEST_CODE = 698;
    private final int NOTIFICATION_ID = 423;
    private PermissionManager permissionManager;
    private boolean bound = false;
    private LollyService odometer;
    // load native C library
    static {
        //   System.loadLibrary("lolly-backend-lib");
    }
    private DocumentFile kmlFile;
    private DocumentFile gpxFile;
    private DocumentFile txtFile;
    private CSVReader csv;
    private int heartIdx = 0;
    private String serialNumber = "Unknown";
    private boolean readWasFinished=false;
    private String ALogFileName="";

    //private Handler progressBarHandler = new Handler(Looper.getMainLooper());
    private TMereni merold =null;

    private List<TMSRec> logs;  // logy, ktere mi lezou z UARTU

    @RequiresApi(api = Build.VERSION_CODES.O)
    private  long calculateSecondsBetween(LocalDateTime start, LocalDateTime end) {
        Duration duration = Duration.between(start, end);
        return duration.getSeconds();
    }

    protected Handler datahandler = new Handler(Looper.getMainLooper()) {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void handleMessage(Message msg) {
            // zkontroluj odstup od posledniho mereni

            TMereni mer = (TMereni) msg.obj;
            if (mer != null &&  merold!= null) {
                if (mer.dtm==null || merold.dtm==null)
                    return;

               long delta = calculateSecondsBetween(merold.dtm, mer.dtm);
               if (delta>Constants.MAX_DELTA) {
                   // zobraz chybu
                   DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                   // Format the dates
                   String meroFormatted = merold.dtm.format(formatter);
                   String merFormatted = mer.dtm.format(formatter);
                   // Display the error with formatted dates
                   String ss = String.format("Between messages >%d seconds (from %s to %s)", delta, meroFormatted, merFormatted);

                   binding.proMessage.setText(ss);
               }
            }
            merold = mer;
            dmd.AddMereni(mer);   // array of values for graph
            csv.AddMerToCsv(mer); // add to csv file
            //csv.AppendStat(mer);  // statistics, we'll omit this in the next version
        }
    };

    // logovani do souboru
    protected Handler loghandler = new Handler(Looper.getMainLooper()) {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void handleMessage(Message msg) {
            // vstupy a vystupy z hardware v uHer.java
            TMSRec log = (TMSRec) msg.obj;
            logs.add(log);

            //binding.proMessage.setText(log);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Log.i("| DEBUG |", "Home Fragment, right above jni string");
  }

    private ServiceConnection connection = new ServiceConnection() {
       @Override
       public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
           LollyService.LollyBinder odometerBinder =
                   (LollyService.LollyBinder) iBinder;
           odometer = odometerBinder.getOdometer();
           odometer.SetHandler(handler);                      // info o pozici ve stavovem stroji
           odometer.SetDataHandler(datahandler);   // do tohoto handleru posilam naparsovane data
           odometer.SetLogHandler(loghandler);
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
        //Constants.showMicro =

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


    private void setFirmwareInfo(RFirmware fw){
        binding.devser.setText(fw.Serial);
        /*
        binding.devhw.setText(String.valueOf(rfir.Hw));
        binding.devfw.setText(String.valueOf(rfir.Fw));
        binding.devsub.setText(String.valueOf(rfir.Sub));
        binding.devname.setText(rfir.DeviceName);
        binding.devstriska.setText(rfir.Striska);
        binding.devfile.setText(rfir.FirmwareFile);
        binding.devpoznamka.setText(rfir.Poznamka);
         */
    }


    private void  setDeviceImage(TDeviceType devType){
        ImageView img = (ImageView)  getActivity().findViewById(R.id.devImage);
        switch (devType){
            case dLolly3:
            case dLolly4:
                img.setImageResource(R.drawable.dev_lolly);
                break;

            case dTermoChron:
                img.setImageResource(R.drawable.dev_wurst);
                break;

            case dAD:
            case dAdMicro:
                img.setImageResource(R.drawable.dev_ad);
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
     //       String  ATrackDir=null;
       //     String ALogDir =  null;

            // tady rozebiram vystupy ze stavu v threadu
            switch(info.stat){

                case tNoHardware:
                    binding.proMessage.setText("NO HARDWARE !!!");
                    break;

                case tWaitForAdapter:
                    // if (ftTMS.AdapterNumber.length()>MIN_ADANUMBER) ;

                    // tady je zobrazeni cisla adapteru
                    if (info.msg.length()>MIN_ADANUMBER) {
                        binding.proMessage.setText(info.msg);

                        ImageView adapterImage = getActivity().findViewById(R.id.adapterImage);
                        adapterImage.setImageResource(R.drawable.adapter_green);
                    }
                    break;

                case tTMDCycling:
                    // sem me dostane adapter, ktery neumi podepsat dlouhy paket
                    binding.proMessage.setText(info.msg);
                    binding.proMessage.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_accent)); // Set text color to red
                    break;

                case tHead:
                    // nastav info o firmware v lizatku

                    // nastav obrazek zarizeni
                    setDeviceImage(info.fw.DeviceType);

                    // nastav typ zarizeni do dmdViewModel a tim i LollyApplication
                    dmd.setDeviceType(info.fw.DeviceType);

                    break;

                case tSerial:
                    serialNumber = info.msg;
                    binding.devser.setText(info.msg);

                    // csv file output, it should be unique for each device and each download
                    LollyApplication.getInstance().setSerialNumber(serialNumber);

                    String ATrackDir = LollyApplication.getInstance().getCacheCsvPath();
                    String ACsvFileName =   CompileFileName("data_",serialNumber,ATrackDir);
                    ALogFileName= "log_"+shared.aft(ACsvFileName,"data_");
                    ALogFileName = LollyApplication.getInstance().getCacheLogPath()+"/"+ALogFileName;

                    ACsvFileName = ATrackDir + "/" + ACsvFileName;
                    csv = new CSVReader(ACsvFileName);
                    csv.OpenForWrite();  // otevre vystupni stream pro addCsv vyse
                    break;

                case tInfo:
                    binding.devhumADVal.setText(String.valueOf(info.humAd));
                    binding.devt1.setText(String.format("%.1f",info.t1));
                    binding.devt2.setText(String.format("%.1f",info.t2));
                    binding.devt3.setText(String.format("%.1f",info.t3));


                    // teprve ted vim, co mam za zarizeni na sonde a muzu nastavit format do csv
                    csv.SetupFormat(info.devType);
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
                    ImageView img = (ImageView)  getActivity().findViewById(R.id.modeImage);
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
                    saveLogAndData();
                    readWasFinished = true;

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

    private void saveLogAndData(){
        if (csv == null)
            return;

        csv.CloseExternalCsv();
        saveLogToFile(ALogFileName);
    }



    private void saveLogToFile(String ALogFileName){
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(ALogFileName))) {
                for (TMSRec log : logs) {
                    writer.write("<<"+log.sCmd);
                    writer.newLine();
                    writer.write(">>"+log.sRsp);
                    writer.newLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            logs.clear();
    }


    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Perform additional view setup here, such as finding views by ID and setting up listeners
        binding.expPath.setText("Home Fragment onViewCreated");
    }

    private void FragmentToDefaultState() {
        //binding.proMessage.setText("Home Fragment onViewCreated");
        binding.devser.setText("0123456789");
        binding.devTime.setText("01.01.2000 12:34:56");
        binding.phoneTime.setText("01.01.2000 12:34:56");
        binding.devMode.setText("Basic");
        binding.devMemory.setProgress(0);
        binding.devhumADVal.setText("0");
        binding.devt1.setText("0");
        binding.devt2.setText("0");
        binding.devt3.setText("0");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        // sdileny datovy model
        dmd = new ViewModelProvider(getActivity()).get(DmdViewModel.class);
        dmd.ClearMereni();

        logs  = new ArrayList<>();

        // tady vybiram callbacky od jinych fragmentu a aplikace
        dmd.getMessageContainerToFragment().observe(getViewLifecycleOwner(), message -> {
             String exportPath = lollyApp.getPrefExportFolder();
            //binding.expPath.setText(exportPath);
        });

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        binding.proBar.setProgress(0); // vycisti progress bar

        // do formulare nahrej defaultni nastaveni
        FragmentToDefaultState();

        Button testLollyInstance = binding.testLolly;
        testLollyInstance.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                lollyApp = LollyApplication.getInstance();
                String exportPath = lollyApp.getPrefExportFolder();
                //binding.expPath.setText(exportPath);
            }
        });

        Button genCommand=binding.genCommand;
        genCommand.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                String ALogName = LollyApplication.getInstance().DIRECTORY_LOGS + "/command.csv.";
                TMSSim sim = new TMSSim(ALogName);
                //  dmd.sendMessageToFragment("TMD");
                // Message message = handler.obtainMessage();
                // message.obj = "Hello from HomeFragment";
                // handler.sendMessage(message);
            }
        });

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
                ALogFileName = LollyApplication.getInstance().getCacheLogPath()+"/"+"testlog.csv";
                saveLogAndData();
                //odometer.SetState(TDevState.tSerial);
                // dmd.sendMessageToFragment("TSN");
            }
        });


        return root;
    }

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

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onDestroyView() {

        // save if we skipped the tFinishedData
        if (!readWasFinished)
           saveLogAndData();

        super.onDestroyView();
        binding = null;
    }
}