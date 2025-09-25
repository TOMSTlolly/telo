package com.tomst.lolly.ui.options;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.DatePicker;
import android.app.DatePickerDialog;

import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.tomst.lolly.LoginActivity;
import com.tomst.lolly.LollyActivity;
import com.tomst.lolly.R;
import com.tomst.lolly.databinding.FragmentOptionsBinding;

import java.time.format.DateTimeFormatter;
import java.util.Calendar;

import java.time.LocalDate;


public class OptionsFragment extends Fragment implements AdapterView.OnItemSelectedListener {

    private static final int REQUEST_ACTION_OPEN_DOCUMENT_TREE = 3;

    private ActivityResultLauncher<Intent> openDocumentTreeLauncher;

    private NestedScrollView nested_scroll_view;
    private ImageButton bt_toggle_info = null;  //  (ImageButton) findViewById(R.id.bt_toggle_info);
    private Button bt_hide_info = null;         //(Button) findViewById(R.id.bt_hide_info);
    private String[] modes_desc,down_desc ;

    private View root = null;
    private FragmentOptionsBinding binding;

    private OptionsViewModel mViewModel;

    private MaterialRadioButton[] additional;

    private View lyt_expand_info;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public static OptionsFragment newInstance() {
        return new OptionsFragment();
    }

    public final int SPI_DOWNLOAD_NONE = 100;
    public final int SPI_DOWNLOAD_ALL = 0;
    public final int SPI_DOWNLOAD_BOOKMARK = 1;
    public final int SPI_DOWNLOAD_DATE = 2;
    public final int SPI_DOWNLOAD_PREVIEW=3;



    @Override
    public void onResume() {
        super.onResume();

        // Set the orientation to landscape (90 degrees)
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //ConnectDevice();
    };


    @Override
    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        /*
        int i = R.id.spiDownload;
        int j = parent.getId();

        if (i==R.id.spiDownload) {
            String line = String.format("%s time: %s", modes_desc[pos], parent.getItemAtPosition(pos).toString());
        }

        switch (parent.getId()){
            case R.id.spiDownload :
                break;

            case R.id.spiInterval:
                break;

            default:
                Toast.makeText(getActivity(),"Spinner without onItemSelected ",Toast.LENGTH_LONG).show();
        }
         */

        int j = parent.getId();
        if ((j != R.id.spiDownload) && (j != R.id.spiInterval))
            Toast.makeText(getActivity(), "Spinner without onItemSelected ", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        //
    }

    /*
    private CheckBoxState checkedStatus() {
        int count = 0;
        for (MaterialRadioButton cb : additional) {
            if (cb.isChecked()) count++;
        }
        if (count == 0) {
            return CheckBoxState.UNCHECKED;
        } else if (count == additional.length) {
            return CheckBoxState.CHECKED;
        } else {
            return CheckBoxState.INDETERMINATE;
        }
    }


    private void updateParentCheckbox() {
        CheckBoxState stat = checkedStatus();
        if (stat.equals(CheckBoxState.CHECKED)) {
            checkbox_add.setButtonDrawable(R.drawable.ic_check_box);
            checkbox_add.setTag(CheckBoxState.CHECKED);
        } else if (stat.equals(CheckBoxState.UNCHECKED)) {
            checkbox_add.setButtonDrawable(R.drawable.ic_check_box_outline);
            checkbox_add.setTag(CheckBoxState.UNCHECKED);
        } else {
            checkbox_add.setButtonDrawable(R.drawable.ic_indeterminate_check_box);
            checkbox_add.setTag(CheckBoxState.INDETERMINATE);
        }
    }
     */


    /*
    private void toggleSectionText(View view) {
        boolean show = Tools.toggleArrow(view);
        if (!show) {
            ViewAnimation.expand(lyt_sub, new ViewAnimation.AnimListener() {
                @Override
                public void onFinish() {
                }
            });
        } else {
            ViewAnimation.collapse(lyt_sub);
        }
    }

     */

    public boolean toggleArrow(View view) {
        if (view.getRotation() == 0) {
            view.animate().setDuration(200).rotation(180);
            return true;
        } else {
            view.animate().setDuration(200).rotation(0);
            return false;
        }
    }

    /*
    private void toggleSectionInfo(View view) {
        boolean show = toggleArrow(view);
        if (show) {
            ViewAnimation.expand(lyt_expand_info, new ViewAnimation.AnimListener() {
                @Override
                public void onFinish() {
                    Tools.nestedScrollTo(nested_scroll_view, lyt_expand_info);
                }
            });
        } else {
            ViewAnimation.collapse(lyt_expand_info);
        }
    }
     */

    @Override
    public void onDestroyView() {
        SaveForm();
        SynchronizeAppCache();
        super.onDestroyView();
        binding = null;
    }


    private void SaveForm()
    {
        Context context = getContext();
        SharedPreferences sharedPref = context.getSharedPreferences(getString(R.string.save_options), context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        // jaky je stav radiobuttonu ?
//        boolean b = binding.readAll.isChecked();
        //editor.putBoolean(getString(R.string.ReadAll),b);
        //editor.putBoolean(getString(R.string.ReadFromBookmark),b);
        //editor.putBoolean(getString(R.string.ReadFromDate),b);
//        editor.putBoolean("read_all",binding.readAll.isChecked());
//        editor.putBoolean("read_bookmark",binding.readBookmark.isChecked());
//        editor.putBoolean("read_date",binding.readDate.isChecked());

        // adresar, kam se budou ukladat soubory
        // POZOR prefExportFolder se uklada pri zpracovani SAF v setPrefExportFolder
        // editor.putString("prefExportFolder", binding.pExportFolder.toString());

        // odkud vycitam
        int spiDownload = (int) binding.spiDownload.getSelectedItemId();
        editor.putInt("readFrom",spiDownload);

        //  malinkaty prikaz na zacatku komunikace
        String s = binding.commandBookmark.getText().toString();
        editor.putString("commandBookmark",s);

        boolean b = binding.checkboxBookmark.isChecked();
        editor.putBoolean("checkboxBookmark",b);

        // interval mezi merenima
        int spiInterval = (int) binding.spiInterval.getSelectedItemId();
        editor.putInt("mode",spiInterval);

//        editor.putBoolean("bookmark",binding.bookmark.isChecked());
        editor.putBoolean("showgraph",binding.showgraph.isChecked());
        editor.putBoolean("rotategraph",binding.rotategraph.isChecked());
        editor.putBoolean("noledlight",binding.noledlight.isChecked());
        editor.putBoolean("showmicro",binding.showmicro.isChecked());
        editor.putBoolean("settime",binding.settime.isChecked());

        // nastav carku-tecku
        s = String.valueOf(binding.Deci.getText());
        editor.putString("decimalseparator",s);

        String bookmarkStr = String.valueOf(binding.bookmarkDeci.getText());
        String dateStr = String.valueOf(binding.fromDate.getText());
        // check for bookmarked days before
        if (!bookmarkStr.isEmpty())
        {
            int bookmarkVal = Integer.parseInt(bookmarkStr);
            editor.putInt("bookmarkVal", bookmarkVal);
        }
        // check for a date from the user
        if (!dateStr.isEmpty())
        {
            editor.putString("fromDate", dateStr);
        }

        //editor.putString("decimalseparator",",");
        //editor.putString("decimalseparator",",");

        editor.apply();
       //
    }

    // uprav cache ini v LollyApplication
    private void SynchronizeAppCache()
    {
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Context context = getContext();
        SharedPreferences prefs = context.getSharedPreferences(getString(R.string.save_options), context.MODE_PRIVATE);
        //SharedPreferences.Editor editor = prefs.edit();

        LollyActivity.getInstance().setPrefExportFolder(prefs.getString("prefExportFolder", ""));
        // adresar, kam se budou ukladat soubory

    }


    private void ReadForm()
    {
        Context context = getContext();
        SharedPreferences sharedPref = context.getSharedPreferences(getString(R.string.save_options), context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        // vytahnu si cislo resource ze strings.xml
        boolean r1 = sharedPref.getBoolean(
                "read_all", false
        );  // false je default, kdyz neexistuje
        boolean r2 = sharedPref.getBoolean(
                "read_bookmark", false
        );  // false je default, kdyz neexistuje
        boolean r3 = sharedPref.getBoolean(
                "read_date", false
        );  // false je default, kdyz neexistuje

        // malinkaty prikaz, ktery se provede jenom jednou na zacatku stavoveho stroje
        boolean checkoxBookmark = sharedPref.getBoolean("checkboxBookmark", false);
        binding.checkboxBookmark.setChecked(checkoxBookmark);

        String commandBookmark = sharedPref.getString("commandBookmark", "");
        binding.commandBookmark.setText(commandBookmark);

        //  TextView textView = root.findViewById(R.id.userDetails);
        //  TextView pExp =  root.findViewById(R.id.p_export_folder);
        binding.pExportFolder.setText(sharedPref.getString("prefExportFolder", ""));
        if (binding.pExportFolder.getText().toString().isEmpty())
        {
            binding.pExportFolder.setText("Exportation folder is empty");
        }

        // zobrazeni uri cesty
        String s = sharedPref.getString("prefExportFolder", "");
        s = extractFolderNameFromEncodedUri(s);
        binding.pExportFolder.setText(s)  ;

        // jak budu vycitat
        int rx = sharedPref.getInt("readFrom",-1);
        binding.spiDownload.setSelection(rx);

        // nastaveni modu
        int i = sharedPref.getInt("mode",-1);
        binding.spiInterval.setSelection(i);

        // nastav checkboxy
//        boolean b = sharedPref.getBoolean("bookmark",false);
//        binding.bookmark.setChecked(b);
        binding.showgraph.setChecked(sharedPref.getBoolean("showgraph",false));
        binding.rotategraph.setChecked(sharedPref.getBoolean("rotategraph",false));
        binding.noledlight.setChecked(sharedPref.getBoolean("noledlight",false));
        binding.showmicro.setChecked(sharedPref.getBoolean("showmicro",false));
        binding.settime.setChecked(sharedPref.getBoolean("settime",false));

        s = sharedPref.getString("decimalseparator",",");  // desetinny oddelovac
        binding.Deci.setText(s);

        // bookmark and fromDate
        int bookmarkVal = sharedPref.getInt("bookmarkVal", 0);
        String bookmarkStr = bookmarkVal == 0 ? "" : String.valueOf(bookmarkVal);
        String dateStr = sharedPref.getString("fromDate", "");
        binding.bookmarkDeci.setText(bookmarkStr);
        binding.fromDate.setText(dateStr);
    }



    /*
    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == REQUEST_ACTION_OPEN_DOCUMENT_TREE && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.

            if (resultData != null) {
                Uri treeUri = resultData.getData();
                getActivity().grantUriPermission(getActivity().getPackageName(), treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);


                // GPSApplication.getInstance().getContentResolver().takePersistableUriPermission(treeUri, Intent
                //        .FLAG_GRANT_READ_URI_PERMISSION | Intent
                //        .FLAG_GRANT_WRITE_URI_PERMISSION);

                getContext().getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                Log.w("myApp", "[#] GPSActivity.java - onActivityResult URI: " + treeUri.toString());
                Log.w("myApp", "[#] GPSActivity.java - onActivityResult URI: " + treeUri.getPath());
                Log.w("myApp", "[#] GPSActivity.java - onActivityResult URI: " + treeUri.getEncodedPath());

                setPrefExportFolder(treeUri.toString());
                LollyApplication.getInstance().setPrefExportFolder(treeUri.toString()); // ulozim do LollyApplication cache
                SetupPreferences();
            }
        }
        super.onActivityResult(resultCode, resultCode, resultData);
    }

     */


    public void setPrefExportFolder(String folder) {
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Context context = getContext();
        SharedPreferences prefs = context.getSharedPreferences(getString(R.string.save_options), context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // adresar, kam se budou ukladat soubory
        editor.putString("prefExportFolder", folder);
        editor.commit();

        folder = extractFolderNameFromEncodedUri(folder);
        binding.pExportFolder.setText(folder);
        Log.w("myApp", "[#] GPSApplication.java - prefExportFolder = " + folder);
    }

    /**
     * Extracts the folder name starting from the encoded uri.
     *
     * @param uriPath The encoded URI path
     * @return the path of the folder
     */
    public String extractFolderNameFromEncodedUri(String uriPath) {
        String spath = Uri.decode(uriPath);
        String pathSeparator = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) ? ":" : "/";
        if (spath.contains(pathSeparator)) {
            String[] spathParts = spath.split(pathSeparator);
            return spathParts[spathParts.length - 1];
        } else return spath;
    }


    private void SetupPreferences() {
        // nastavim adresar pro export
        // binding.pExportFolder.setText(GPSApplication.getInstance().getPrefExportFolder());
        Button buttonLogout = root.findViewById(R.id.btnLogout);
        Button buttonLogin = root.findViewById(R.id.btnLoginOptions);
        TextView textView = root.findViewById(R.id.userDetails);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user != null)
        {
            textView.setText(user.getEmail());
        }

        buttonLogout.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (user != null)
                {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    startActivity(intent);
                    getActivity().finish();
                }
                else
                {
                    Toast.makeText(v.getContext(), "Not Logged In", Toast.LENGTH_SHORT).show();
                }
            }
        });

        buttonLogin.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (user == null)
                {
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    startActivity(intent);
                    getActivity().finish();
                }
                else
                {
                    Toast.makeText(v.getContext(), "Already Logged In",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.O)
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState)
    {
        binding = FragmentOptionsBinding.inflate(inflater, container, false);
        root = binding.getRoot();
        Resources res = getResources();


        // Initialize the ActivityResultLauncher
        openDocumentTreeLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent resultData = result.getData();
                        if (resultData != null) {
                            Uri treeUri = resultData.getData();
                            if (treeUri != null) {
                                getActivity().grantUriPermission(getActivity().getPackageName(), treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                getContext().getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                                Log.w("myApp", "[#] OptionsFragment.java - onActivityResult URI: " + treeUri.toString());
                                Log.w("myApp", "[#] OptionsFragment.java - onActivityResult URI: " + treeUri.getPath());
                                Log.w("myApp", "[#] OptionsFragment.java - onActivityResult URI: " + treeUri.getEncodedPath());

                                setPrefExportFolder(treeUri.toString());
                                LollyActivity.getInstance().setPrefExportFolder(treeUri.toString()); // Save to LollyApplication cache
                                SetupPreferences();
                            }
                        }
                    }
                });


        // user auth

        // odkud vycitam
        Spinner spiDownload = (Spinner) root.findViewById(R.id.spiDownload);
        ArrayAdapter<CharSequence> adaDownload =
                ArrayAdapter.createFromResource(
                        this.getContext(),
                        R.array.download_array,
                        android.R.layout.simple_spinner_item
                );
        //adaDownload.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adaDownload.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spiDownload.setAdapter(adaDownload); // Apply the adapter to the spinner
        spiDownload.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                LinearLayout bookmarkLayout = (LinearLayout) root.findViewById(R.id.bookmarkLayout);
                LinearLayout fromDateLayout = (LinearLayout) root.findViewById(R.id.fromDateLayout);

                switch (position) {
                    case SPI_DOWNLOAD_BOOKMARK:
                        //bookmarkLayout.setVisibility(View.VISIBLE);
                        bookmarkLayout.setVisibility(View.GONE);
                        fromDateLayout.setVisibility(View.GONE);
                        break;

                    case SPI_DOWNLOAD_DATE:
                        bookmarkLayout.setVisibility(View.GONE);
                        fromDateLayout.setVisibility(View.VISIBLE);
                        break;

                    case SPI_DOWNLOAD_NONE:
                        break;
                    case SPI_DOWNLOAD_ALL:
                        bookmarkLayout.setVisibility(View.GONE);
                        fromDateLayout.setVisibility(View.GONE);
                        break;
                    case SPI_DOWNLOAD_PREVIEW:
                        bookmarkLayout.setVisibility(View.GONE);
                        fromDateLayout.setVisibility(View.GONE);
                        break;

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // nothing
            }
        });
        down_desc = res.getStringArray(R.array.download_array);

        // vzdalenost mezi merenimi
        Spinner spiInterval = (Spinner) root.findViewById(R.id.spiInterval);
        ArrayAdapter<CharSequence> adaInterval = ArrayAdapter.createFromResource(
                this.getContext(), R.array.modes_array, android.R.layout.simple_spinner_item);
        adaInterval.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spiInterval.setAdapter(adaInterval); // Apply the adapter to the spinner
        spiInterval.setOnItemSelectedListener(this);
        //Resources res = getResources();
        modes_desc = res.getStringArray(R.array.modes_desc);

        /*
        bt_toggle_info = (ImageButton) root.findViewById(R.id.bt_toggle_info);
        bt_toggle_info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleSectionInfo(bt_toggle_info);
            }
        });
         */

        // nested scrollview
        nested_scroll_view = (NestedScrollView) root.findViewById(R.id.nested_scroll_view);
        lyt_expand_info = (View) root.findViewById(R.id.lyt_expand_info);

        ReadForm();

        // export folder
        TextView pExp = root.findViewById(R.id.p_export_folder);
        pExp.setOnClickListener(new View.OnClickListener() {
            @Override   // export folder
            public void onClick(View v) {
                Log.w("myApp", "[#]");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    // Choose a directory using the system's file picker.
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
                    intent.putExtra("android.content.extra.FANCY", true);
                    //intent.putExtra("android.content.extra.SHOW_FILESIZE", true);
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

                   // startActivityForResult(intent, REQUEST_ACTION_OPEN_DOCUMENT_TREE);
                    openDocumentTreeLauncher.launch(intent);
                }

            }
        } );


        ImageButton bt_save_form = root.findViewById(R.id.bt_save_form);
        bt_save_form.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                SaveForm();
                Toast.makeText(getContext(), "Options saved", Toast.LENGTH_SHORT).show();
            }
        });
        /*
        lyt_sub = root.findViewById(R.id.lyt_sub);
        ViewAnimation.collapse(lyt_sub);   // zabal ramecek
        bt_toggle = root.findViewById(R.id.bt_toggle);
        bt_toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleSectionText(bt_toggle);
            }
        });

        additional = new MaterialRadioButton[additional_id.length];
        for (int i = 0; i < additional_id.length; i++) {
            //Log.d("com.tomst.com.ListFragment", String.valueOf(i));
            additional[i] = root.findViewById(additional_id[i]);

            additional[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateParentCheckbox();
                }
            });

        }
        */

        /*
        checkbox_add = root.findViewById(R.id.checkbox_add);
        checkbox_add.setTag(CheckBoxState.CHECKED);
        checkbox_add.setText("Read all data");
        checkbox_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkbox_add.getTag().equals(CheckBoxState.CHECKED) || checkbox_add.getTag().equals(CheckBoxState.INDETERMINATE)) {
                    checkbox_add.setButtonDrawable(R.drawable.ic_check_box_outline);
                    checkbox_add.setTag(CheckBoxState.UNCHECKED);
                    //checkUncheckCheckBox(false);
                } else if (checkbox_add.getTag().equals(CheckBoxState.UNCHECKED)) {
                    checkbox_add.setButtonDrawable(R.drawable.ic_check_box);
                    checkbox_add.setTag(CheckBoxState.CHECKED);
                    //checkUncheckCheckBox(true);
                }
            }
        });
         */
        Button pickDateBtn = root.findViewById(R.id.fromDate);

        // on below line we are adding click listener for our pick date button
        pickDateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // on below line we are getting
                // the instance of our calendar.
                final Calendar c = Calendar.getInstance();

                // on below line we are getting
                // our day, month and year.
                int year = c.get(Calendar.YEAR);
                int month = c.get(Calendar.MONTH);
                int day = c.get(Calendar.DAY_OF_MONTH);

                // on below line we are creating a variable for date picker dialog.
                DatePickerDialog datePickerDialog = new DatePickerDialog(
                        // on below line we are passing context.
                            root.getContext(),
                           new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year,
                                                  int monthOfYear, int dayOfMonth) {
                                // on below line we are setting date to our text view.
                                //pickDateBtn.setText(dayOfMonth + "-" + (monthOfYear + 1) + "-" + year);
                                String dateStr = String.format("%02d-%02d-%d", dayOfMonth, monthOfYear + 1, year);
                                LocalDate selectedDate = LocalDate.parse(dateStr, formatter);
                                pickDateBtn.setText(selectedDate.toString());
                            }
                        },
                        // on below line we are passing year,
                        // month and day for selected date in our date picker.
                        year, month, day);
                // at last we are calling show to
                // display our date picker dialog.
                datePickerDialog.show();
            }
        });

        return root;
        //return inflater.inflate(R.layout.fragment_options, container, false);

    }

    /*
    private void checkUncheckCheckBox(boolean isChecked) {
        for (MaterialCheckBox cb : additional) {
            cb.setChecked(isChecked);
        }
    }
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(OptionsViewModel.class);
    }

    /*
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(OptionsViewModel.class);
        // TODO: Use the ViewModel
    }
     */
}
