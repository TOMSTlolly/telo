package com.tomst.lolly.ui.graph;

import static android.graphics.Color.RED;
import static com.tomst.lolly.core.shared.getSerialNumberFromFileName;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.components.Legend;

import com.github.mikephil.charting.utils.ColorTemplate;
import com.tomst.lolly.LollyActivity;
import com.tomst.lolly.R;
import com.tomst.lolly.core.TDendroInfo;
import com.tomst.lolly.core.TMereni;
import com.tomst.lolly.core.TPhysValue;
import com.tomst.lolly.databinding.FragmentGraphBinding;
import com.tomst.lolly.core.DmdViewModel;
import com.tomst.lolly.core.CSVReader;
import com.tomst.lolly.fileview.FileDetail;


@RequiresApi(api = Build.VERSION_CODES.O)
public class GraphFragment extends Fragment {
    // constants for loading CSV files
    private static final String DATE_PATTERN = "yyyy.MM.dd HH:mm";
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
    private static final String SHORT_DATE_PATTERN = "M.d.yy";
    private DateTimeFormatter display_formatter = DateTimeFormatter.ofPattern(SHORT_DATE_PATTERN);
    private static final byte SERIAL_INDEX = 0;
    private static final byte LONGITUDE_INDEX = 1;
    private static final byte LATITUDE_INDEX = 2;
    private static final byte PICTURE_INDEX = 4;

    // constants for loading measurements
    private static final byte DATETIME_INDEX = 1;
    private static final byte TEMP1_INDEX = 3;
    private static final byte TEMP2_INDEX = 4;
    private static final byte TEMP3_INDEX = 5;
    private static final byte HUMIDITY_INDEX = 6;
    private static final byte MVS_INDEX = 7;
    private final String TMD_DELIM = " ";
    private final String TAG = "GraphFragment";
    private static String fSerialNumber="";
    private int MaxPos = 0;  // progress bar max value

    // CSV loading
    public int headerIndex = 0;
    public int numDataSets = 0;
    public float[] intervals = {10f, 10f};

    public class LongAxisValueFormatter extends ValueFormatter {
        @Override
        public String getAxisLabel(float value, AxisBase axis) {

            // Convert float value (epoch seconds) to LocalDateTime
            LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond((long) value),
                    ZoneOffset.MAX);

            // Format LocalDateTime to string
            return dateTime.format(display_formatter);
        }
    }

    public GraphFragment() {
        // Required empty public constructor
        headerIndex = 0;
    }

    // visualization data holders
    //private final int barCount = 12;
    private ArrayList<ILineDataSet> dataSets = new ArrayList<>();
    private ArrayList<TDendroInfo> dendroInfos = new ArrayList<>();
    private ArrayList<LegendEntry> LegendEntrys = new ArrayList<>();


    // graphing
    private CombinedChart chart;
    private CombinedData combinedData;

    private SeekBar seekBarX;
    private TextView tvX;

    private final int[] colors = new int[]{
            ColorTemplate.VORDIPLOM_COLORS[0],
            ColorTemplate.VORDIPLOM_COLORS[1],
            ColorTemplate.VORDIPLOM_COLORS[2]
    };

    private FragmentGraphBinding binding;
    private DmdViewModel dmd;
    private Integer fIdx = 0;

    @Override
    public void onDestroy() {
        super.onDestroy();
        //dmd.sendMessageToGraph("");
    }

    @Override
    public void onStop() {
        dmd.sendMessageToFragment("");
        dmd.ClearMereni();

        super.onStop();
    }

    private void LoadDmdData(){
        LineDataSet d = null;

        // **** linearni graf
        d = SetLine(dmd.getT1(),TPhysValue.vT1);
        dataSets.add(d);
        d = SetLine(dmd.getT2(),TPhysValue.vT2);
        dataSets.add(d);
        d = SetLine(dmd.getT3(),TPhysValue.vT3);
        dataSets.add(d);
        // humidita
        d = SetLine(dmd.getHA(),TPhysValue.vHum);
        dataSets.add(d);
        LineData lines = new LineData(dataSets);
        combinedData.setData(lines);

        // combinedData.setData(generateBarData());
        chart.setData(combinedData);
        chart.getAxisLeft().setEnabled(true);
        chart.getAxisRight().setEnabled(true);

        // startup animation
        chart.animateX(2000, Easing.EaseInCubic);

        // sets view to start of graph and zooms into x axis by 7x
        chart.zoomAndCenterAnimated(7f, 1f, 0, 0, chart.getAxisLeft().getAxisDependency(), 3000);

    }
    private  float min(float a, float b, float c) {
        return Math.min(Math.min(a, b), c);
    }
    private  float max(float a, float b, float c) {
        return Math.max(Math.max(a, b), c);
    }

    protected Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            TMereni mer = (TMereni) msg.obj;
            //Log.d(TAG,String.valueOf(mer.idx));
            dmd.AddMereni(mer);
            fIdx++;
        }
    };

    private boolean loadCSVFil(String uriPath)  {
        Uri fileUri = Uri.parse(uriPath);
        CSVReader csv = new CSVReader(fileUri.toString());
        csv.SetHandler(handler);

        // progress bar
        csv.SetProgressListener(value -> {
               //  Log.d(TAG, "Bar: " + value);
                if (value<0) {
                    binding.proBar.setMax((int) -value); // posledni adresa
                    MaxPos = -value;
                    binding.proBar.setProgress(value);
                }
                else {  // progress
                    binding.proBar.setProgress(value);
                }
        });
        // konec vycitani
        csv.SetFinListener(value -> {
            // tady muzu otypovat zarizeni a nastavit checkboxy, titulky, apod.
            FileDetail det = csv.getFileDetail();
            dmd.setDeviceType(det.getDeviceType());
            setGraphLines();

            Log.d(TAG,"Finished");
            //DisplayData();
            LoadDmdData();
            //  binding.proBar.setProgress(0);
        });

        csv.start();
        return true;
    }


    private void DoBtnClick(View view)
    {
        boolean checked = ((CheckBox) view).isChecked();
        Object ob = ((CheckBox) view).getTag();
        int tag = Integer.valueOf(ob.toString());
        if (tag <= 0)
        {
            throw new UnsupportedOperationException(
                    "Selected line dataset doesn't exists"
            );
        }

        if ((dataSets == null) || (dataSets.size()<1))
             return;

        do {
            dataSets.get(tag - 1).setVisible(checked);
            chart.invalidate();
            tag+=4;
        } while ( tag <= dataSets.size());

    }


    // nahraje data pridane TMD adapterem ve fragmentu HomeFragment
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

 //       String deviceName = LollyActivity.getInstance().getSerialNumber(); // or your method
 //       requireActivity().setTitle("Test");


        // Set the orientation to landscape (90 degrees)
        if (LollyActivity.getInstance().getPrefRotateGraph()){
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }


    private void setupGraph(){
   //     getActivity().setTitle("Lolly 4");
        chart = binding.chart1;
 //       Description description = new Description();
   //     description.setText("Lolly 4x");
     //   chart.setDescription(description);



        //chart.getDescription().setText(CsvFileName);
        chart.setTouchEnabled(true);
        chart.setDragDecelerationFrictionCoef(0.9f);

        // enable scaling and dragging
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setDrawGridBackground(false);
        chart.setHighlightPerDragEnabled(true);

        // set an alternative background color
        // chart.setBackgroundColor(Color.WHITE);
        chart.setViewPortOffsets(0f, 0f, 0f, 0f);

        // if disabled, scaling can be done on x- and y-axis separately
        chart.setPinchZoom(false);

        // get the legend (only possible after setting data)
        Legend l = chart.getLegend();
        /*
        l.setWordWrapEnabled(true);
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);
        l.setEnabled(false);
        */

        l.setForm(Legend.LegendForm.LINE);
        //l.setTypeface(tfLight);
        l.setTextSize(11f);
        l.setTextColor(Color.BLACK);
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(true);

        // osa humidit
        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setDrawGridLines(true);
        rightAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
        //rightAxis.setAxisMinimum(0f); // this replaces setStartAtZero(true)
        //rightAxis.setAxisMaximum(1000f);

        // osa teplot
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
        //leftAxis.setAxisMinimum(-10f); // this replaces setStartAtZero(true)
        //leftAxis.setAxisMaximum(30f);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.TOP_INSIDE);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setDrawAxisLine(true);
        xAxis.setDrawGridLines(true);
        xAxis.setTextColor(Color.rgb(255, 192, 56));
        xAxis.setCenterAxisLabels(true);
        xAxis.setGranularity(1f); // one hour
        combinedData = new CombinedData();
    }

    // set the graph components based on the device type
    private void setGraphLines() {
        // all off
        binding.vT1.setChecked(false);
        binding.vT2.setChecked(false);
        binding.vT3.setChecked(false);
        binding.vGrowth.setChecked(false);

        String title = "";
        switch (dmd.GetDeviceType()) {
            case dLolly4:
                title = "TMS-4";
                binding.vT1.setChecked(true);
                binding.vT2.setChecked(true);
                binding.vT3.setChecked(true);
                binding.vGrowth.setChecked(true);
                break;

            case dLolly3:
                title = "TMS-3";
                binding.vT1.setChecked(true);
                binding.vT2.setChecked(true);
                binding.vT3.setChecked(true);
                binding.vGrowth.setChecked(true);
                break;

            case dAdMicro:
                title = "Dendrometer/Micro";
                binding.vT1.setChecked(true);
                binding.vGrowth.setChecked(true);
                break;
            case dAD:
                title = "Dendrometer/Raw";
                binding.vT1.setChecked(true);
                binding.vGrowth.setChecked(true);
                break;

            case dTermoChron:
                title = "Thermochron";
                binding.vT1.setChecked(true);
                break;

            default:
                throw new UnsupportedOperationException("Not yet implemented");
        }
        binding.chartTitle.setText(fSerialNumber + " / " + title);
    }


    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        Log.d("MERGECALL", "MERGE IS CALLED");
        GraphViewModel readerViewModel =
                new ViewModelProvider(this).get(GraphViewModel.class);


        // Check the device's orientation
        View root;
        root = inflater.inflate(R.layout.fragment_graph, container, false);

        binding = FragmentGraphBinding.bind(root);
        setupGraph();

        dmd = new ViewModelProvider(getActivity()).get(DmdViewModel.class);

        // we have set two observers, one for physical data received from TMD adapter, second from the ListFragment viewer
        // observer message for TMD adapter
        dmd.getMessageContainerToFragment()
                .observe(getViewLifecycleOwner(), msg ->
                {
                    //
                    int loadCSVCode = 0;
                    Log.d("GRAPH", "Received: " + msg);
                    if (msg.split(TMD_DELIM)[0].equals("TMD"))
                    {
                        // the data has been sent by the TMD USB adapter using TMSReader
                        Log.d("GRAPH", "Setup dendrometer graph");

                        fSerialNumber = msg.split(TMD_DELIM)[1];
                        setGraphLines();
                        LoadDmdData();

                        //setupDMDGraph(fSerialNumber);
                        //DisplayData(); // create lines and send them to chart
                    }
                    else
                    {
                         // the data has been sent from the ListFragment viewer
                         String[] fileNames = msg.split(";");
                         String  fileName = fileNames[0];
                         if (fileName.length()<1)
                             return;

                         if (loadCSVFil(fileName)){
                                fSerialNumber = getSerialNumberFromFileName(fileNames[0]);
                         }

                    }

                    dmd.getMessageContainerToFragment()
                            .removeObservers(getViewLifecycleOwner());
                });


        // tohle je defaultni nastaveni, ktere odsud zmizne
        // nastavuju v grafu podle typu zarizeni

        // teplota 1
        CheckBox cbT1 = binding.vT1;
        cbT1.setOnClickListener(view ->
        {
            DoBtnClick(view);
        });
        cbT1.setChecked(true);

        // teplota 2
        CheckBox cbT2 = binding.vT2;
        cbT2.setOnClickListener(view ->
        {
            DoBtnClick(view);
        });
        cbT2.setChecked(false);

        // teplota 3
        CheckBox cbT3 = binding.vT3;
        cbT3.setOnClickListener(view ->
        {
            DoBtnClick(view);
        });
        cbT3.setChecked(false);

        // humidita
        CheckBox cbHum = binding.vGrowth;
        cbHum.setOnClickListener(view ->
        {
            DoBtnClick(view);
        });
        cbHum.setChecked(true);

 //       getActivity().setTitle("Lolly 4");
        return root;
    }

    private LineDataSet SetLine(ArrayList<Entry> vT, TPhysValue val)
    {
        Log.d(TAG, "SetLine");
        int lineColor=0;
        int colorStep=255/3;

        LineDataSet set = new LineDataSet(vT, "DataSet " + (val.ordinal() + 1));
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.LINEAR);
        set.setDrawFilled(false);
  //      set.setLabel(val.valToString(val));
        set.setLineWidth(1f);

        //differentiating values by different dash patterns
        switch (val)
        {
            case vT1:
                //GREEN
                lineColor = Color.rgb(20,83,45);
                set.setLabel("T1");
                set.setAxisDependency(YAxis.AxisDependency.LEFT);
                set.setLineWidth(2f);
                break;

            case vT2:
                //ORANGE
                set.setLabel("T2");
                lineColor = Color.rgb(241,168,51);
                set.setAxisDependency(YAxis.AxisDependency.LEFT);

                set.setLineWidth(2f);
                break;

            case vT3:
                //DARK BROWN
                set.setLabel("T3");
                //lineColor = Color.rgb(52,21,0);
                lineColor = RED;

                set.setAxisDependency(YAxis.AxisDependency.LEFT);
                set.setLineWidth(2f);
                break;

            case vHum:
                //LIGHT BLUE
                set.setLabel("Hum/Adu");
                lineColor = Color.rgb(0,197,255);

                set.setAxisDependency(YAxis.AxisDependency.RIGHT);
                set.setLineWidth(2f);
                set.enableDashedLine(20f, 20f, 0f);
                break;


            case vAD:
                //LIGHT BLUE
                lineColor = Color.rgb(0,197,255);
                set.setAxisDependency(YAxis.AxisDependency.RIGHT);
                set.setLineWidth(2f);
                set.enableDashedLine(20f, 20f, 0f);

                set.setLineWidth(5f);
                break;

            case vMicro:
                //LIGHT BLUE
                lineColor = Color.rgb(0,197,255);
                set.setLineWidth(2f);
                set.enableDashedLine(20f, 20f, 0f);
                set.setAxisDependency(YAxis.AxisDependency.RIGHT);
                break;

            default:
                throw new UnsupportedOperationException("Not yet implemented");
        }
        set.setColor(lineColor);

        return set;
    }


    protected float getRandom(float range, float start)
    {
        return (float) (Math.random() * range) + start;
    }


    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        binding = null;
    }

}