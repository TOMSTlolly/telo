package com.tomst.lolly.ui.viewfile;


import static android.app.Activity.RESULT_CANCELED;
import static android.os.Environment.*;
import static com.tomst.lolly.LollyActivity.DIRECTORY_TEMP;
import static com.tomst.lolly.core.shared.bef;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import com.tomst.lolly.core.shared;
import com.tomst.lolly.LollyActivity;
import com.tomst.lolly.R;
import com.tomst.lolly.core.CSVReader;
import com.tomst.lolly.core.Constants;
import com.tomst.lolly.core.DatabaseHandler;
import com.tomst.lolly.core.DmdViewModel;
import com.tomst.lolly.core.FileOpener;
import com.tomst.lolly.core.OnProListener;
import com.tomst.lolly.core.ZipFiles;
import com.tomst.lolly.databinding.FragmentViewerBinding;
import com.tomst.lolly.fileview.FileDetail;
import com.tomst.lolly.fileview.FileViewerAdapter;
import com.tomst.lolly.core.PermissionManager;
import com.tomst.lolly.BuildConfig;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ListFragment extends Fragment implements OnProListener
{
    private ExecutorService executor;
    public void SetFilePath(String path)
    {
        filePath = path;
    }
    DocumentFile sharedFolder;
    DocumentFile privateFolder;



    private FragmentViewerBinding binding;
    private View rootView = null;
    private int mywidth;
    private Bitmap
            fileImage,
            pictureImage,
            audioImage,
            videoImage,
            unknownImage,
            archiveImage,
            folderImage;
    private PermissionManager permissionManager;
    private String filePath;
    private File parent;
    private final String TAG = "TOMST";
    public FileOpener fopen;
    private  DmdViewModel dmd;
    private String SelectedFileName= "";

    FirebaseFirestore db;

    List<FileDetail> fFriends = null;

    // najde prvek ve fFriends podle jmena souboru
    private FileDetail findFileName(String name)
    {
        for (FileDetail file : fFriends)
        {
            if (file.getName().equals(name))
            {
                return file;
            }
        }
        return null;
    }

    // prepise prvek ve fFriends
    private void updateFriends(FileDetail source, FileDetail target){
        int index = fFriends.indexOf(target);
        fFriends.set(index, source);
    }



    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Set the orientation to landscape (90 degrees)
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //ConnectDevice();
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        executor = Executors.newSingleThreadExecutor();
    }



    @RequiresApi(api = Build.VERSION_CODES.O)
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        ListViewModel listViewModel =
                new ViewModelProvider(this).get(ListViewModel.class);

        binding = FragmentViewerBinding.inflate(
                inflater, container, false
        );
        rootView = binding.getRoot();



        // trik, kterym si aplikace rekne o opravneni pri vytvareni formulare
        // Location location = LollyActivity.getInstance().getLocation();

        binding.proBar.setMin(0);
        binding.proBar.setMax(100);
        binding.proBar.setProgress(5);

        //  cesty pro export do SAF
        String sharedPath = LollyActivity.getInstance().getPrefExportFolder();
        if (sharedPath.startsWith("content"))
            sharedFolder = DocumentFile.fromTreeUri(LollyActivity.getInstance(), Uri.parse(sharedPath));
        else
            sharedFolder = DocumentFile.fromFile(new File(sharedPath));


        String privatePath = getContext().getFilesDir().toString();
        privateFolder = DocumentFile.fromFile(new File(privatePath));

        // textova popiska k umisteni souboru
        TextView folderName = binding.tvFolderDest;
        String s = shared.extractFolderNameFromEncodedUri(sharedPath);
        folderName.setText(s);

        // Saving folder destination
        Button btn_reload = binding.btnLoadFolder;
        btn_reload.setOnClickListener(new View.OnClickListener()
        {
            @Override   // load files from the folder
            public void onClick(View view)
            {
               // DoLoadFiles();
                if (SelectedFileName != "")
                {
                    //csvPath = SelectedFileName;
                    loadCSVFil(SelectedFileName);
                }
            }

        });

        Button zip_btn = binding.buttonZipall;
        zip_btn.setText("Upload Zip");
        zip_btn.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                Context context = getContext();
                String sharedPath = LollyActivity.getInstance().getPrefExportFolder();
                DocumentFile exportFolder = DocumentFile.fromTreeUri(context, Uri.parse(sharedPath));
                if (exportFolder == null || !exportFolder.isDirectory()) {
                    Toast.makeText(context, "Export folder not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                File cacheDir = context.getCacheDir();
                File zipFile = new File(cacheDir, "tmp.zip");

                ZipFiles zipFiles = new ZipFiles();
                zipFiles.zipDocumentFileDirectory(exportFolder, zipFile.getAbsolutePath(), context);

                Uri zipUri = FileProvider.getUriForFile(
                        context,
                        BuildConfig.APPLICATION_ID + ".provider",
                        zipFile
                );
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.setType("application/zip");
                sendIntent.putExtra(Intent.EXTRA_STREAM, zipUri);
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Zip file");
                sendIntent.putExtra(Intent.EXTRA_TEXT, "Here is content of my download from lolly phone app.");
                startActivity(sendIntent);
            }
        });


        Button shareBtn = binding.btnShare;
        shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Context context = getContext();
                String sharedPath = LollyActivity.getInstance().getPrefExportFolder();
                DocumentFile exportFolder = DocumentFile.fromTreeUri(context, Uri.parse(sharedPath));
                if (exportFolder == null || !exportFolder.isDirectory()) {
                    Toast.makeText(context, "Export folder not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                File cacheDir = context.getCacheDir();
                File zipFile = new File(cacheDir, "tmp.zip");

                ZipFiles zipFiles = new ZipFiles();
                zipFiles.zipDocumentFileDirectory(exportFolder, zipFile.getAbsolutePath(), context);

                Uri zipUri = FileProvider.getUriForFile(
                        context,
                        BuildConfig.APPLICATION_ID + ".provider",
                        zipFile
                );
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.setType("application/zip");
                sendIntent.putExtra(Intent.EXTRA_STREAM, zipUri);
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Zip file");
                sendIntent.putExtra(Intent.EXTRA_TEXT, "Here is content of my download from lolly phone app.");
                startActivity(sendIntent);
            }
        });


        dmd = new ViewModelProvider(getActivity()).get(DmdViewModel.class);
        dmd.sendMessageToFragment("");

        permissionManager = new PermissionManager(getActivity());
        fopen = new FileOpener(getActivity());

        fFriends = new ArrayList<>();

        ListView mListView = rootView.findViewById(R.id.listView);
        FileViewerAdapter friendsAdapter = new FileViewerAdapter(getContext(), fFriends);
        mListView.setAdapter(friendsAdapter);
        mListView.animate();

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FileDetail selectedItem = (FileDetail) parent.getItemAtPosition(position);
//                SelectedFileName = selectedItem.getName();
                SelectedFileName = selectedItem.getFull();
                friendsAdapter.setSelectedPosition(position);
            }
        });

        Button select_sets_btn = binding.selectGraph;
        select_sets_btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view) {
                if (SelectedFileName != "")
                {
                    dmd.sendMessageToFragment(SelectedFileName);
                    switchToGraphFragment();
                }
                // ArrayList<String> fileNames = friendsAdapter.collectSelected();
                //for (String fileName : fileNames) {
                //   fileNameMsg += fileName + ";";
            }
        });

        Intent intent = getActivity() != null ? getActivity().getIntent() : null;
        if (intent != null)
        {
            switch (intent.getAction())
            {
                case Intent.ACTION_GET_CONTENT:
                    fopen.isRequestDocument = true;
                    getActivity().setResult(RESULT_CANCELED);
                    break;

                case Intent.ACTION_OPEN_DOCUMENT : {
                    fopen.isRequestDocument = true;
                    getActivity().setResult(RESULT_CANCELED);
                    break;
                }

                default :
                    fopen.isRequestDocument = false;
            }
        }
        //setupBitmaps();

        return rootView;
    }

    private void loadAllFiles()
    {
        // will most likely not exceed number of datasets on device
        ArrayList<String> filenames = new ArrayList<String>();
        ListView mListView = (ListView) rootView.findViewById(R.id.listView);
        FileViewerAdapter friendsAdapter = new FileViewerAdapter(
                getContext(), fFriends
        );
        mListView.setAdapter(friendsAdapter);
        mListView.animate();


        // add listener for loading selected datasets to graph fragment
        Button select_sets_btn = binding.selectGraph;
        select_sets_btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                String fileNameMsg = "";
                ArrayList<String> fileNames = friendsAdapter.collectSelected();
                for (String fileName : fileNames)
                {
                    fileNameMsg += fileName + ";";
                }

                if (!fileNameMsg.contains("_parallel"))
                {
                    dmd.sendMessageToFragment(fileNameMsg);
                    switchToGraphFragment();
                }
                else
                {
                    Toast.makeText(
                        getContext(),
                        "Parallel formatted files are unable to be"
                            + " visualized!",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }
        });
    }

    /*
    private void loadFromStorage()
    {
        // show the loading icon
        ProgressBar progressBar = rootView.findViewById(R.id.uploadProgressBar);
        progressBar.setVisibility(View.VISIBLE);

        // set adapter
        FileViewerAdapter friendsAdapter = new FileViewerAdapter(getContext(), fFriends);

        // get current user
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        String userEmail = user != null ? user.getEmail() : "unknown";

        // load files from storage
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("Files");
        storageRef.listAll()
                .addOnSuccessListener(listResult ->
                {
                    for (StorageReference fileRef : listResult.getItems())
                    {
                        fileRef.getMetadata().addOnSuccessListener(storageMetadata ->
                        {
                            String fileUsers = storageMetadata.getCustomMetadata("user");
                            if (fileUsers != null)
                            {
                                String[] users = fileUsers.split(",");
                                for (String currentUser : users)
                                {
                                    if (currentUser.equals(userEmail))
                                    {
                                        // file belongs to the current user, download it
                                        String fileName = fileRef.getName();
                                        String filePath = fileRef.getPath();
                                        downloadCSVFile(fileName, filePath);

                                        // set the icon
                                        for (FileDetail fileDetail : fFriends)
                                        {
                                            if (fileDetail.getName().equals(fileName))
                                            {
                                                fileDetail.setUploaded(true);
                                                break;
                                            }
                                        }

                                        // update list view with icons
                                        ListView mListView = rootView.findViewById(R.id.listView);
                                        mListView.setAdapter(friendsAdapter);
                                    }
                                }

                            }
                        }).addOnFailureListener(e ->
                        {
                            Log.e(TAG, "Failed to get metadata: " + e.getMessage());
                        });
                    }
                    // hide loading icon
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e ->
                {
                    // hide loading icon
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Failed to list files: " + e.getMessage());
                });
    }
     */

    private void downloadCSVFile(String fileName, String filePath)
    {
        File localFile = new File(getExternalStoragePublicDirectory(
                DIRECTORY_DOCUMENTS), fileName);

        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(filePath);

        storageRef.getFile(localFile)
                .addOnSuccessListener(taskSnapshot ->
                {
                    Log.d(TAG, "File downloaded to " + localFile.getAbsolutePath());
                })
                .addOnFailureListener(exception ->
                {
                    Log.e(TAG, "Failed to download file: " + exception.getMessage());
                });
    }

    private void switchToGraphFragment()
    {
        BottomNavigationView bottomNavigationView;
        bottomNavigationView = (BottomNavigationView) getActivity()
                .findViewById(R.id.nav_view);
        View view = bottomNavigationView.findViewById(R.id.navigation_graph);
        view.performClick();
    }



    private void shareData()
    {
        FileViewerAdapter friendsAdapter = new FileViewerAdapter(getContext(), fFriends);
        ArrayList<String> selectedFiles = friendsAdapter.collectSelected();

        boolean allUploaded = true;
        for (String selectedFile : selectedFiles)
        {
            for (FileDetail fileDetail : fFriends)
            {
                if (fileDetail.getFull().equals(selectedFile) && !fileDetail.isUploaded())
                {
                    allUploaded = false;
                    break;
                }
            }
        }

        if (!allUploaded)
        {
            Toast.makeText(getContext(), "Please upload all files before sharing", Toast.LENGTH_SHORT).show();
            return;
        }

        // create an alert dialog with an edit text field
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Enter Email Addresse(s)");
        builder.setMessage("User a comma(,) to seperate emails");

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // set up the buttons
        builder.setPositiveButton("Share", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                String emails = input.getText().toString();
                if (!TextUtils.isEmpty(emails))
                {
                    String[] emailArray = emails.split(",");
                    updateMetadata(emailArray);
                }
                else
                {
                    Toast.makeText(getContext(), "Please enter at least one email", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void updateMetadata(String[] emails) {
        FileViewerAdapter friendsAdapter = new FileViewerAdapter(getContext(), fFriends);
        ArrayList<String> selectedFiles = friendsAdapter.collectSelected();

        for (String selectedFile : selectedFiles)
        {
            Uri fileUri = Uri.fromFile(new File(selectedFile));
            StorageReference storageRef = FirebaseStorage.getInstance().getReference();
            StorageReference fileRef = storageRef.child("Files/" + fileUri.getLastPathSegment());

            // get current metadata
            fileRef.getMetadata().addOnSuccessListener(metadata ->
            {
                String currentUser = metadata.getCustomMetadata("user");
                if (currentUser != null)
                {
                    // append new emails to the existing user metadata
                    StringBuilder newUserMetadata = new StringBuilder(currentUser);
                    for (String email : emails)
                    {
                        if (!newUserMetadata.toString().contains(email))
                        {
                            newUserMetadata.append(",").append(email);
                            Toast.makeText(getContext(), "Shared with " + email, Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            Toast.makeText(getContext(), "Already shared with " + email, Toast.LENGTH_SHORT).show();
                        }
                    }

                    // update metadata
                    fileRef.updateMetadata(new StorageMetadata.Builder()
                                    .setCustomMetadata("user", newUserMetadata.toString())
                                    .build())
                            .addOnSuccessListener(aVoid ->
                            {
                                Toast.makeText(getContext(), "Updated metadata successfully", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                            {
                                Toast.makeText(getContext(), "Failed to update metadata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                }
            }).addOnFailureListener(e ->
            {
                Toast.makeText(getContext(), "Failed to get metadata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }


    public static boolean canListFiles(File f) {
        boolean ret=false;
        if (f.canRead())
         if (f.isDirectory())
             ret = true;
        return(ret);
    }




    // _data_98765432_2024_10_28_5.csv
    // 1
    // 2  data
    // 3 98765432

    // 4 2024
    // 5 10
    // 6 28
    // 7  5.csv
    private String getNiceName(String name)
    {

        if (!name.contains("_"))
            return name;

        String[] parts = name.split("_");
        if (parts.length != 8)
            return name;

        if (parts[1].isEmpty())
            return null;

         //String s = parts[1] + "-" + parts[2] + parts[3] + parts[4]+ "-" + bef(parts[5],"\\.");
        String s = parts[1] + "-" + parts[2] + parts[3] + parts[4] + bef(parts[7],"\\.");

        return s;
    }

    protected Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(android.os.Message msg) {
            super.handleMessage(msg);
        }
    };

    protected int px=0;

    @Override
    public void OnProEvent(int pos){
        if (pos < 0) {
            binding.proBar.setProgress(0);
            //binding.proBar.setMax(-pos);
            binding.proBar.setMax(1000);
            return;
        }

        handler.post(() -> binding.proBar.setProgress(px));
        px++;
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private FileDetail LoadCsvFile(DocumentFile file)
    {
        Context context = getContext();
        File tempFile = null;
        try {
            tempFile = CSVReader.FileUtils.copyDocumentFileToTempFile(context, file);
            Uri tempUri = Uri.fromFile(tempFile);
            FileDetail fdet =  LoadCsvFile(tempUri);
            // Get last modified date
            long lastModified = file.lastModified();
            // Assuming FileDetail has a setLastModified(long) method; adjust as needed
            // This is the corrected code
            fdet.setCreated(
                    Instant.ofEpochMilli(lastModified)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime()
            );

            return fdet;
        } catch (IOException e) {
            Log.e(TAG, "Error copying DocumentFile to temp file", e);
            FileDetail fdet = new FileDetail(file.getName());
            fdet.setErr(Constants.PARSER_ERROR);
            return fdet;
        }
    }

    // zjisti podrobnosti o csv souboru
    // nacte hlavicku, prvni a posledni zaznam
    // vrati FileDetail
    @RequiresApi(api = Build.VERSION_CODES.O)
    private FileDetail LoadCsvFile(Uri fileUri)
    {
        FileDetail fdet = null;

        CSVReader csv = new CSVReader(fileUri.toString());
        csv.SetHandler(handler);
        csv.SetProgressListener(this);
        csv.SetProgressListener(value -> {
            Log.d(TAG, "Bar: " + value);
            if (value < 0) {
                binding.proBar.setMax((int) -value); // posledni adresa
                binding.proBar.setProgress(value);
            } else {  // progress
                binding.proBar.setProgress(value);
            }
        });
        csv.SetFinListener(value -> {
            Log.d(TAG, "Finished");
            binding.proBar.setProgress(0);
        });

        try{
            // precte cele CSV, zjisti Maxima, minima, diry v datech
            fdet = csv.readFileContent(fileUri);  //FileDetail

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return fdet;
    }



    private boolean loadCSVFil(String uriPath)  {
        Uri fileUri = Uri.parse(uriPath);
        CSVReader csv = new CSVReader(fileUri.toString());
        csv.SetHandler(handler);

        //FileDetail fileDetail = null;
        // progress bar
        csv.SetProgressListener(value -> {
            Log.d(TAG, "Bar: " + value);
            if (value<0) {
                binding.proBar.setMax((int) -value); // posledni adresa
                binding.proBar.setProgress(value);
            }
            else {  // progress
                binding.proBar.setProgress(value);
            }
        });
        // konec vycitani
        csv.SetFinListener(value -> {
            Log.d(TAG,"Finished");

            // pote co prolezu csv, mam hotovou statistiku, kterou muzu prehrat do radky
            FileDetail fdet= csv.getFileDetail();
            FileDetail temp = findFileName(fileUri.toString());  // najdi existujici radek v recycleru podle jmena souboru
            if (temp != null) {
                updateFriends(fdet, temp);
            }

            //DisplayData();
            //LoadDmdData();
              binding.proBar.setProgress(0);
        });
        csv.start();
        /*
        try {
            csv.start();
            csv.join();
        }
        catch (InterruptedException e) {
            Log.d(TAG, e.toString());
        };

         */

        return true;
    }

    // naplni seznam fFriends podle csv souboru v adresari
    // bez nacitani podrobnosti
    // jen jmeno, velikost, datum
    // pro rychle zobrazeni seznamu
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void DoLoadFil()
    {
        fFriends.clear();
        //File cacheDir = new File(DIRECTORY_TEMP);
        // Log.d(TAG, "Loading files from: " + cacheDir.getAbsolutePath());
        //if (cacheDir.isDirectory()) {
        //    File[] files = cacheDir.listFiles();
        //    if (files == null || files.length == 0)
        //       return;
        if (sharedFolder != null && sharedFolder.isDirectory()) {
            DocumentFile[] files = sharedFolder.listFiles();
            if (files == null || files.length == 0)
                return;

            FileDetail fdet = null;
            CSVReader reader = new CSVReader();
            //for (File file : files) {
            for (DocumentFile file : files) {
                if (file != null) {
                    fdet = reader.FirstLast(file);
                    fdet.setFileSize((int) file.length());
                    fdet.setCreated(
                            Instant.ofEpochMilli(file.lastModified())
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDateTime()
                    );

                    fdet.setFull(file.getUri().toString());
                    fdet.setName(file.getName());
                    fdet.setNiceName(getNiceName(file.getName()));

                    fdet.setFileSize((int) file.length());
                    fFriends.add(fdet);
                }
            }
        }
    }

    //public void DoLoadFiles(String sharedPath, String privatePath)
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void DoLoadFiles() {

        if (sharedFolder != null && sharedFolder.isDirectory()) {
            DocumentFile[] files = sharedFolder.listFiles();
            if (files == null || files.length == 0)
                return;


            Context context = LollyActivity.getInstance().getApplicationContext();
            DatabaseHandler db = LollyActivity.getInstance().gpsDataBase;

            // remove not used files from db
            String[] usedFiles = new String[files.length];
            int index = 0;

            // vyhodim prazdne soubory
            // ty, ktere jsou >0 a jsou relevantni (pripona .csv a prefix data_ )
            for (DocumentFile file: files){
                usedFiles[index++] = file.getName();
            }
            db.ClearUnusedFiles(usedFiles);  // vyhodi z databaze nazvy souboru, ktere nemam v adresari

            //Location location = LollyActivity.getInstance().getLocation();
            FileDetail fdet = null;

            for (DocumentFile file : files) {

                String fileName = file.getName();
                if (fileName == null || !fileName.toLowerCase().endsWith(".csv"))
                    continue;

                // vymaz prazdny soubor
                if (file.length() ==0) {
                    Log.d(TAG, "File " + fileName + " is empty. Deleting...");
                    if (file.delete()) {
                        Log.d(TAG, "Empty file " + fileName + " deleted successfully.");
                    } else {
                        Log.e(TAG, "Failed to delete empty file: " + fileName);
                    }
                    continue; // Skip to the next file
                }


                if (db.getFileDetail(file.getName()) != null) {
                    fdet = db.getFileDetail(file.getName());
                    fdet.setNiceName(getNiceName(file.getName()));
                }
                else
                {
                    // vycitam uplne cely soubor
                    fdet = LoadCsvFile(file);
                    fdet.setName(file.getName());
                    fdet.setNiceName(getNiceName(file.getName()));

                    fdet.setFileSize((int) file.length());
                    //fdet.setFull(fileUri.toString());
                    fdet.setFull(file.getName());


                    if (fdet.getFileSize()<1)
                        fdet.errFlag = Constants.PARSER_FILE_EMPTY;

                    // prazdny soubor v seznamu nechci, jenom mi to dela brikule

                    // je to datovy soubor ?
                    if ((fdet.errFlag == Constants.PARSER_OK) || (fdet.errFlag == Constants.PARSER_HOLE_ERR))
                        db.addFile(fdet, null);
                }
                // predelej radek v recycleru
                fdet.setFileSize((int) file.length());
                fFriends.add(fdet);
                /*
                FileDetail temp = findFileName(file.getName());  // najdi existujici radek v recycleru podle jmena souboru
                if (temp != null) {
                    updateFriends(fdet, temp);
                }
                 */


            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onStart()
    {
        Log.d("LIST", "Started DoLoadFile() ...");
        super.onStart();

     //   DoLoadFil();


        Log.d("LIST", "Started DoLoadFiles() ...");

       DoLoadFiles();

        /*
        executor.execute(() -> {
            DoLoadFiles();
        });
         */


    }
}