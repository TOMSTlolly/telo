package com.tomst.lolly.core;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.tomst.lolly.R;
import com.tomst.lolly.fileview.FileDetail;
import com.tomst.lolly.fileview.FileViewerAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class FireBaseShare {

    List<FileDetail> fFriends = null;
    Context context = null;
    ListView mListView = null; //rootView.findViewById(R.id.listView);

    private void uploadDataToStorage(ProgressBar progressBar)
    {
        // show the loading icon
//        ProgressBar progressBar = rootView.findViewById(R.id.uploadProgressBar);
        progressBar.setVisibility(View.VISIBLE);

        FileViewerAdapter friendsAdapter = new FileViewerAdapter(context, fFriends);

        ArrayList<String> fileNames = friendsAdapter.collectSelected();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        AtomicInteger filesUploaded = new AtomicInteger(0);
        int totalFiles = fileNames.size();

        // check if user is logged in
        if (user != null)
        {
            // go through all selected files
            for (String fileName : fileNames)
            {
                // get info
                Uri fileUri = Uri.fromFile(new File(fileName));
                String userEmail = user != null ? user.getEmail() : "unknown";

                // check if the file has already been uploaded
                boolean isAlreadyUploaded = false;
                for (FileDetail fileDetail : friendsAdapter.getAllFiles())
                {
                    if (fileDetail.getFull().equals(fileName) && fileDetail.isUploaded())
                    {
                        isAlreadyUploaded = true;
                        break;
                    }
                }

                // file is already uploaded, skip it
                if (isAlreadyUploaded)
                {
                    Toast.makeText(context, fileUri.getLastPathSegment() + " Already Uploaded", Toast.LENGTH_SHORT).show();

                    filesUploaded.incrementAndGet();
                    // check if all files uploaded
                    if (filesUploaded.get() == totalFiles)
                    {
                        progressBar.setVisibility(View.GONE);
                    }
                    continue;
                }

                StorageReference storageRef = FirebaseStorage.getInstance().getReference();
                StorageReference fileRef = storageRef.child("Files/" + fileUri.getLastPathSegment());

                fileRef.putFile(fileUri)
                        .addOnSuccessListener(taskSnapshot ->
                        {
                            // update the file's metadata to include the user Id
                            fileRef.updateMetadata(
                                    new StorageMetadata.Builder()
                                            .setCustomMetadata("user", userEmail)
                                            .build()
                            ).addOnSuccessListener(aVoid ->
                            {
                                // set cloud icon
                                for (FileDetail fileDetail : friendsAdapter.getAllFiles())
                                {
                                    if (fileDetail.getFull().equals(fileName))
                                    {
                                        fileDetail.setUploaded(true);
                                        break;
                                    }
                                }
                                filesUploaded.incrementAndGet();

                                // check if all files uploaded
                                if (filesUploaded.get() == totalFiles)
                                {
                                    // update list view with icons
//                                    ListView mListView = rootView.findViewById(R.id.listView);
                                    mListView.setAdapter(friendsAdapter);

                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(context, fileUri.getLastPathSegment() + " Uploaded Successfully", Toast.LENGTH_SHORT).show();
                                }
                            }).addOnFailureListener(e ->
                            {
                                Toast.makeText(context, "Failed to update metadata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                filesUploaded.incrementAndGet();

                                // check if all files uploaded
                                if (filesUploaded.get() == totalFiles)
                                {
                                    progressBar.setVisibility(View.GONE);
                                }
                            });
                        })
                        .addOnFailureListener(e ->
                        {
                            Toast.makeText(context, "Data Upload Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                            filesUploaded.incrementAndGet();

                            // check if all files uploaded
                            if (filesUploaded.get() == totalFiles)
                            {
                                progressBar.setVisibility(View.GONE);
                            }
                        });
            }
        }
        else
        {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(context, "Must login to upload files", Toast.LENGTH_SHORT).show();
        }
    }
}
