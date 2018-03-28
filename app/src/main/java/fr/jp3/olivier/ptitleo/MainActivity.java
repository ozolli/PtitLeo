package fr.jp3.olivier.ptitleo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.components.YAxis.AxisDependency;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

public class MainActivity extends ActionBarActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */

    private static final int MAX_UDP_DATAGRAM_LEN = 1500;
    private static int UDP_SERVER_PORT;
    private static boolean bGraph;
    private static boolean bThemeLight;
    private static float graphText;
    private static int textColor;
    String logname = "";
    OutputStreamWriter outputWriter;

    private boolean bRecordLog = false;
    // fragment_perf
    private static TextView cible, twa, tws, bsp, hdg, capab, twd, dopt, popt, twdavg10, twsavg10;
    private static String mcible, mtwa, mtws, mbsp, mhdg, mcapab, mtwd, mdopt, mpopt, mtwdavg10, mtwsavg10;
    // fragment_gps
    private static TextView cog, sog, set, drift, stddev, sat, awa, aws, capwp, distwp, vmc, nomwp;
    private static String mcog, msog, mset, mdrift, mstddev, msat, mawa, maws, mcapwp, mdistwp, mvmc, mnomwp;
    // fragment_avg
    private static TextView baro, airtemp, depth, watertemp, dayloch, totalloch, pitch, heel;
    private static String mbaro, mairtemp, mdepth, mwatertemp, mdayloch, mtotalloch, mpitch, mheel;
    // fragment_graph
    private static LineChart mChartTWS, mChartTWD, mChartBARO;
    private static YAxis leftAxisTWS, leftAxisTWD, leftAxisBARO;

    Timer timer;
    TimerTask timerTask;
    //we are going to use a handler to be able to run in our TimerTask
    final Handler handler = new Handler();

    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        UDP_SERVER_PORT = Integer.parseInt(SP.getString("udpPort", "10110"));
        bGraph = SP.getBoolean("enableGraph", false);
        boolean bAwake = SP.getBoolean("disableSleep", false);
        bThemeLight = SP.getBoolean("themeLight",false);

        if (bThemeLight) setTheme(R.style.AppThemeLight);
        else setTheme(R.style.AppTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }


        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Lecture des préférences enregistrées
        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(4);
        if (bGraph) mViewPager.setCurrentItem(1);
        else mViewPager.setCurrentItem(0);

        udpReceiver = new UDPReceiver();
        udpReceiver.start();

        if (bAwake) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        TypedValue graphTextValue = new TypedValue();
        MainActivity.this.getTheme().resolveAttribute(R.attr.graphTextColor, graphTextValue, true);
        textColor = graphTextValue.data;

    }

        @Override
    protected void onResume() {
        super.onResume();

        //onResume we start our timer so it can start when the app comes from the background
        startTimer();
   }

    public void startTimer() {
        //set a new Timer
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        //schedule the timer, after the first 1000ms the TimerTask will run every 1000ms
        timer.schedule(timerTask, 1000, 2000); //
    }

    public void stoptimertask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void initializeTimerTask() {

        timerTask = new TimerTask() {
            public void run() {

                // Mise à jour des informations à l'écran
                handler.post(new Runnable() {
                    public void run() {
                        if(cible != null) cible.setText(mcible);
                        if(twa != null) twa.setText(mtwa);
                        if(tws != null) tws.setText(mtws);
                        if(bsp != null) bsp.setText(mbsp);
                        if(hdg != null) hdg.setText(mhdg);
                        if(capab != null) capab.setText(mcapab);
                        if(twd != null) twd.setText(mtwd);
                        if(dopt != null) dopt.setText(mdopt);
                        if(popt != null) popt.setText(mpopt);
                        if(twdavg10 != null) twdavg10.setText(mtwdavg10);
                        if(twsavg10 != null) twsavg10.setText(mtwsavg10);
                        if(cog != null) cog.setText(mcog);
                        if(sog != null) sog.setText(msog);
                        if(pitch != null) pitch.setText(mpitch);
                        if(heel != null) heel.setText(mheel);
                        if(set != null) set.setText(mset);
                        if(drift != null) drift.setText(mdrift);
                        if(stddev != null) stddev.setText(mstddev);
                        if(sat != null) sat.setText(msat);
                        if(awa != null) awa.setText(mawa);
                        if(aws != null) aws.setText(maws);
                        if(baro != null) baro.setText(mbaro);
                        if(airtemp != null) airtemp.setText(mairtemp);
                        if(depth != null) depth.setText(mdepth);
                        if(watertemp != null) watertemp.setText(mwatertemp);
                        if(dayloch != null) dayloch.setText(mdayloch);
                        if(totalloch != null) totalloch.setText(mtotalloch);
                        if(capwp != null) capwp.setText(mcapwp);
                        if(distwp != null) distwp.setText(mdistwp);
                        if(vmc != null) vmc.setText(mvmc);
                        if(nomwp != null) nomwp.setText(mnomwp);
                    }
                });
            }
        };
    }

    @Override
    protected void onPause() {
        super.onPause();
        stoptimertask();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        if (bThemeLight) {
            MenuItem item = menu.findItem(R.id.action_record);
            item.setIcon(getResources().getDrawable(R.mipmap.ic_rec_black));
            item = menu.findItem(R.id.action_settings);
            item.setIcon(getResources().getDrawable(R.mipmap.ic_settings_black));
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case android.R.id.home:
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle("Quitter P'tit Léo");
                alertDialog.setMessage("Sûr ?");
                if (bThemeLight) alertDialog.setIcon(R.mipmap.ic_launcher_black);
                else alertDialog.setIcon(R.mipmap.ic_launcher);

                alertDialog.setButton(-1, "Oui", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.this.finish();
                    }
                });

                alertDialog.setButton(-2, "Non", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

                alertDialog.show();
                return true;

            case R.id.action_settings:
                Intent i = new Intent(this, UserSettingActivity.class);
                startActivity(i);
                return true;

            case R.id.action_record:
                String status = item.getTitle().toString();
                if (Objects.equals(status, "Rec")) {
                    item.setTitle("Stop");
                    if (bThemeLight) item.setIcon(R.mipmap.ic_stop_black);
                    else item.setIcon(R.mipmap.ic_stop_white);
                    RecordLog();
                }
                else if (Objects.equals(status, "Stop")) {
                    item.setTitle("Rec");
                    if (bThemeLight) item.setIcon(R.mipmap.ic_rec_black);
                    else item.setIcon(R.mipmap.ic_rec_white);
                    StopLog();
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if (bGraph) {
                switch (position) {
                    case 0: return GraphFragment.newInstance(position + 1);
                    case 1: return PerfFragment.newInstance(position + 1);
                    case 2: return GPSFragment.newInstance(position + 1);
                    case 3: return AvgFragment.newInstance(position + 1);
                    default: return PerfFragment.newInstance(position + 1);
                }
            }
            else {
                switch(position){
                    case 0: return PerfFragment.newInstance(position + 1);
                    case 1: return GPSFragment.newInstance(position + 1);
                    case 2: return AvgFragment.newInstance(position + 1);
                    default: return PerfFragment.newInstance(position + 1);
                }
            }
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            if (bGraph) return 4;
            else return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
                case 2:
                    return getString(R.string.title_section3).toUpperCase(l);
            }
            return null;
        }
    }

    public static class PerfFragment extends Fragment {
        private static final String ARG_SECTION_NUMBER = "section_number";

        public static PerfFragment newInstance(int sectionNumber) {
            PerfFragment fragment = new PerfFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PerfFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View perfView = inflater.inflate(R.layout.fragment_perf, container, false);

            bsp = (TextView) perfView.findViewById(R.id.textViewBSP);
            twa = (TextView) perfView.findViewById(R.id.textViewTWA);
            tws = (TextView) perfView.findViewById(R.id.textViewTWS);
            twsavg10 = (TextView) perfView.findViewById(R.id.textViewTWSavg10mn);
            twd = (TextView) perfView.findViewById(R.id.textViewTWD);
            twdavg10 = (TextView) perfView.findViewById(R.id.textViewTWDavg10mn);
            cible = (TextView) perfView.findViewById(R.id.textViewCible);
            dopt = (TextView) perfView.findViewById(R.id.textViewDOptVMG);
            popt = (TextView) perfView.findViewById(R.id.textViewPOptVMG);
            hdg = (TextView) perfView.findViewById(R.id.textViewHDM);
            capab = (TextView) perfView.findViewById(R.id.textViewCapab);

            return perfView;
        }

    }

    public static class GPSFragment extends Fragment {
        private static final String ARG_SECTION_NUMBER = "section_number";

        public static GPSFragment newInstance(int sectionNumber) {
            GPSFragment fragment = new GPSFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public GPSFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View gpsView = inflater.inflate(R.layout.fragment_gps, container, false);
            cog = (TextView) gpsView.findViewById(R.id.textViewCOG);
            sog = (TextView) gpsView.findViewById(R.id.textViewSOG);
            set = (TextView) gpsView.findViewById(R.id.textViewSET);
            drift = (TextView) gpsView.findViewById(R.id.textViewDRIFT);
            capwp = (TextView) gpsView.findViewById(R.id.textViewCapWP);
            distwp = (TextView) gpsView.findViewById(R.id.textViewDistWP);
            vmc = (TextView) gpsView.findViewById(R.id.textViewVMC);
            nomwp = (TextView) gpsView.findViewById(R.id.textViewNomWPLabel);
            awa = (TextView) gpsView.findViewById(R.id.textViewAWA);
            aws = (TextView) gpsView.findViewById(R.id.textViewAWS);
            stddev = (TextView) gpsView.findViewById(R.id.textViewStdDev);
            sat = (TextView) gpsView.findViewById(R.id.textViewSat);
            return gpsView;
        }
    }

    public static class AvgFragment extends Fragment {
        private static final String ARG_SECTION_NUMBER = "section_number";

        public static AvgFragment newInstance(int sectionNumber) {
            AvgFragment fragment = new AvgFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public AvgFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View avgView = inflater.inflate(R.layout.fragment_avg, container, false);
            airtemp = (TextView) avgView.findViewById(R.id.textViewAirTemp);
            baro = (TextView) avgView.findViewById(R.id.textViewBaro);
            watertemp = (TextView) avgView.findViewById(R.id.textViewTempEau);
            depth = (TextView) avgView.findViewById(R.id.textViewProf);
            dayloch = (TextView) avgView.findViewById(R.id.textViewLochJour);
            totalloch = (TextView) avgView.findViewById(R.id.textViewLochTot);
            pitch = (TextView) avgView.findViewById(R.id.textViewPitch);
            heel = (TextView) avgView.findViewById(R.id.textViewGite);
            return avgView;
        }
    }

    public static class GraphFragment extends Fragment {
        private static final String ARG_SECTION_NUMBER = "section_number";

        public static GraphFragment newInstance(int sectionNumber) {
            GraphFragment fragment = new GraphFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public GraphFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View graphView = inflater.inflate(R.layout.fragment_graph, container, false);

            TypedValue outValue = new TypedValue();
            getResources().getValue(R.dimen.graphText, outValue, true);
            graphText = outValue.getFloat();

            mChartTWS = (LineChart) graphView.findViewById(R.id.chartTWS);
            mChartTWS.setDescription("TWS");
            mChartTWS.setDescriptionColor(textColor);
            mChartTWS.setDescriptionTextSize(16f);
            mChartTWS.setAutoScaleMinMaxEnabled(true);
            mChartTWS.setHighlightEnabled(false);
            mChartTWS.setTouchEnabled(false);
            mChartTWS.setDragEnabled(false);
            mChartTWS.setScaleEnabled(false);
            mChartTWS.setDrawGridBackground(false);
            mChartTWS.setPinchZoom(false);
            LineData dataTWS = new LineData();
            dataTWS.setValueTextColor(textColor);
            mChartTWS.setData(dataTWS);
            mChartTWS.getLegend().setEnabled(false);
            XAxis xTWS = mChartTWS.getXAxis();
            xTWS.setTextColor(textColor);
            xTWS.setTextSize(graphText);
            xTWS.setDrawGridLines(false);
            xTWS.setAvoidFirstLastClipping(true);
            xTWS.setSpaceBetweenLabels(5);
            xTWS.setEnabled(true);
            leftAxisTWS = mChartTWS.getAxisLeft();
            leftAxisTWS.setTextColor(textColor);
            leftAxisTWS.setTextSize(graphText);
            leftAxisTWS.setStartAtZero(false);
            leftAxisTWS.setDrawGridLines(true);
            YAxis rightAxisTWS = mChartTWS.getAxisRight();
            rightAxisTWS.setEnabled(false);

            mChartTWD = (LineChart) graphView.findViewById(R.id.chartTWD);
            mChartTWD.setDescription("TWD");
            mChartTWD.setDescriptionColor(textColor);
            mChartTWD.setDescriptionTextSize(16f);
            mChartTWD.setAutoScaleMinMaxEnabled(true);
            mChartTWD.setHighlightEnabled(false);
            mChartTWD.setTouchEnabled(false);
            mChartTWD.setDragEnabled(false);
            mChartTWD.setScaleEnabled(false);
            mChartTWD.setDrawGridBackground(false);
            mChartTWD.setPinchZoom(false);
            LineData dataTWD = new LineData();
            dataTWD.setValueTextColor(textColor);
            mChartTWD.setData(dataTWD);
            mChartTWD.getLegend().setEnabled(false);
            XAxis xTWD = mChartTWD.getXAxis();
            xTWD.setTextColor(textColor);
            xTWD.setTextSize(graphText);
            xTWD.setDrawGridLines(false);
            xTWD.setAvoidFirstLastClipping(true);
            xTWD.setSpaceBetweenLabels(5);
            xTWD.setEnabled(true);
            leftAxisTWD = mChartTWD.getAxisLeft();
            leftAxisTWD.setTextColor(textColor);
            leftAxisTWD.setTextSize(graphText);
            leftAxisTWD.setStartAtZero(false);
            leftAxisTWD.setDrawGridLines(true);
            YAxis rightAxisTWD = mChartTWD.getAxisRight();
            rightAxisTWD.setEnabled(false);

            mChartBARO = (LineChart) graphView.findViewById(R.id.chartBARO);
            mChartBARO.setDescription("BARO");
            mChartBARO.setDescriptionColor(textColor);
            mChartBARO.setDescriptionTextSize(16f);
            mChartBARO.setAutoScaleMinMaxEnabled(true);
            mChartBARO.setHighlightEnabled(false);
            mChartBARO.setTouchEnabled(false);
            mChartBARO.setDragEnabled(false);
            mChartBARO.setScaleEnabled(false);
            mChartBARO.setDrawGridBackground(false);
            mChartBARO.setPinchZoom(false);
            LineData dataBARO = new LineData();
            dataBARO.setValueTextColor(textColor);
            mChartBARO.setData(dataBARO);
            mChartBARO.getLegend().setEnabled(false);
            XAxis xBARO = mChartBARO.getXAxis();
            //xBARO.setTextColor(R.color.colorFront);
            xBARO.setTextSize(graphText);
            xBARO.setDrawGridLines(false);
            xBARO.setAvoidFirstLastClipping(true);
            xBARO.setSpaceBetweenLabels(5);
            xBARO.setEnabled(true);
            leftAxisBARO = mChartBARO.getAxisLeft();
            leftAxisBARO.setTextColor(textColor);
            leftAxisBARO.setTextSize(graphText);
            leftAxisBARO.setStartAtZero(false);
            leftAxisBARO.setDrawGridLines(true);
            YAxis rightAxisBARO = mChartBARO.getAxisRight();
            rightAxisBARO.setEnabled(false);

            return graphView;
        }
    }

    private void addGraphEntry(LineChart lc, YAxis la, String v, float avg, int visible) {

        Date updatedate = new Date(System.currentTimeMillis());
        SimpleDateFormat format = new SimpleDateFormat("HH:mm");
        String h = format.format(updatedate);

        LineData data = lc.getData();

        if (data != null) {
            LineDataSet set = data.getDataSetByIndex(0);

            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }

            if (set.getEntryCount() == visible) {
                data.removeXValue(0);
                set.removeEntry(0);
                for (Entry entry : set.getYVals()) {
                    entry.setXIndex(entry.getXIndex() - 1);
                }
            }

            // add a new x-value first
            data.addXValue(h);
            data.addEntry(new Entry((float) (Double.parseDouble(v)), set.getEntryCount()), 0);

            LimitLine ll = new LimitLine(avg, "");
            ll.setLineColor(textColor);
            ll.setLineWidth(2f);

            la.removeAllLimitLines();
            la.addLimitLine(ll);

            // let the chart know it's data has changed
            lc.notifyDataSetChanged();

            // limit the number of visible entries
            lc.setVisibleXRangeMaximum(visible - 1);

            // move to the latest entry
            lc.moveViewToX(data.getXValCount() - visible);
        }
    }

    private LineDataSet createSet() {

        LineDataSet set = new LineDataSet(null, "");
        set.setAxisDependency(AxisDependency.LEFT);
        set.setColor(textColor);
        set.setDrawCircles(false);
        set.setDrawCubic(true);
        set.setLineWidth(2f);
        set.setValueTextColor(textColor);
        set.setValueTextSize(graphText);
        set.setDrawValues(false);
        return set;
    }

    public void RecordLog() {
        Date updatedate = new Date(System.currentTimeMillis());
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        logname = format.format(updatedate) + ".txt";

        // Dossier /sdcard/Documents/PtitLeo (Mémoire de stockage interne)
        File sdCard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File dir = new File (sdCard.getAbsolutePath() + "/PtitLeo");
        dir.mkdirs();
        File file = new File(dir, logname);

        try {
            FileOutputStream fileout = new FileOutputStream(file);
            outputWriter = new OutputStreamWriter(fileout);
            bRecordLog = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void StopLog() {
        try {
            outputWriter.close();
            bRecordLog = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void UpdateCell(final TextView t, final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run(){
                if(t != null) t.setText(s);
            }
        });
    }

    public void UpdateCellStyle(final TextView t, final int i) {
        runOnUiThread(new Runnable() {
            @Override
            public void run(){
                if(t != null) t.setTypeface(Typeface.create(t.getTypeface(), i), i);
            }
        });
    }

    // Implémente une moyenne mobile sur un échantillonnage donné
    private class AverageQueue {
        double total = 0;
        double avg = 0;
        int capa = 0;
        Queue q;


        public AverageQueue(int Capacity) {
            capa = Capacity;
            q = new ArrayDeque(Capacity);
        }

        // Attend une string contenant le dernier échantillon
        // Retourne un float contenant la moyenne mise à jour
        public float update(String v) {
            Double d = Double.parseDouble(v);

            // Si on a atteint la capacité maximale on commence à faire glisser la moyenne
            if (q.size() > capa) {
                total -= (Double) q.poll();
            }
            q.add(d);
            total += d;
            avg = total / q.size();
            return (float) avg;
        }

    }

    private AverageQueue tws10mn = new AverageQueue(600);
    private AverageQueue twd10mn = new AverageQueue(600);
    private AverageQueue baro12h = new AverageQueue(2880);

    private UDPReceiver udpReceiver = null;

    private class UDPReceiver extends Thread {
        private boolean bKeepRunning = true;
        private String lastMessage = "";
        private InetAddress server_addr;
        private DatagramSocket socket;
        private int b = 0;

        public void run() {
            String message;
            byte[] lmessage = new byte[MAX_UDP_DATAGRAM_LEN];
            DatagramPacket packet = new DatagramPacket(lmessage, lmessage.length);

/*
            try {
                server_addr = InetAddress.getByName(UDP_SERVER_IP);
            } catch (UnknownHostException e) {
            }
*/

            try {
                socket = new DatagramSocket(UDP_SERVER_PORT);

                while(bKeepRunning) {
                    socket.receive(packet);
                    message = new String(lmessage, 0, packet.getLength());
                    lastMessage = message;
                    parseNMEA(lastMessage);
                }

            } catch (Throwable e) {
                e.printStackTrace();
            }

            if (socket != null) {
                socket.close();
            }
        }

        public void kill() {
            bKeepRunning = false;
        }

        // Ecriture du paquet udp dans le log
        private void WriteLog(String str) {
            try {
                outputWriter.write(str + "\r\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Traitement d'un paquet UDP reçu
        private void parseNMEA(String str) {

            int stddevStyle = Typeface.NORMAL;

            // Découpe un paquet UDP en phrases uniques
            List<String> sentences = Arrays.asList(str.split("\\r?\\n"));

            for (int i = 0; i < sentences.size(); i++) {

                if (bRecordLog) {
                    String logline = String.valueOf(System.currentTimeMillis()) + ":" + sentences.get(i);
                    WriteLog(logline);
                }

                // Supprime * et checksum
                String line = sentences.get(i).substring(0, sentences.get(i).indexOf('*'));

                // Découpe en valeurs dans un array
                // items.get(0) contient l'identifiant de la phrase ($-----)
                final List<String> items = Arrays.asList(line.split("\\s*,\\s*"));
                String ID = items.get(0).substring(3, 6);

                if (ID.equals("HDM")) {
                    mhdg = ent(items.get(1));

                } else if (ID.equals("VHW")) {
                    mbsp = items.get(5);

                } else if (ID.equals("VLW")) {
                    mtotalloch = ent(items.get(1));
                    mdayloch = items.get(3);

                } else if (ID.equals("MWV")) {
                    final String wss = items.get(3);
                    if (Objects.equals(items.get(2), "T")) {
                        final float wss10mn = tws10mn.update(wss);
                        if (bGraph) addGraphEntry(mChartTWS, leftAxisTWS, wss, wss10mn, 600);
                        mtwa = getwa(items.get(1));
                        mtws = wss;
                        mtwsavg10 = String.valueOf(roundTwoDecimals(wss10mn));
                    } else if (Objects.equals(items.get(2), "R")) {
                        mawa = getwa(items.get(1));
                        maws = wss;
                    }

                } else if (ID.equals("MWD")) {
                    final String twds = items.get(3);
                    final float twds10mn = twd10mn.update(twds);
                    if (bGraph) addGraphEntry(mChartTWD, leftAxisTWD, twds, twds10mn, 600);
                    mtwd = ent(twds);
                    mtwdavg10 = ent(String.valueOf(twds10mn));

                } else if (ID.equals("XDR")) {
                    if (Objects.equals(items.get(4), "Heel")) {
                        mheel = abs(ent(items.get(2))) ;
                    } else if (Objects.equals(items.get(4), "Pitch")) {
                        mpitch = abs(ent(items.get(2)));
                    } else if (Objects.equals(items.get(4), "AirTemp")) {
                        mairtemp = items.get(2);
                    } else if (Objects.equals(items.get(4), "Barometer")) {// Toutes les 15 secondes on met à jour le graphique
                        if (b == 15) {
                            // baros contient la pression au 10ème de mbar
                            Double d = Double.parseDouble(items.get(2)) * 10000;
                            long l = Math.round(d);
                            d = (double) l / 10;
                            final String baros = String.valueOf(d);
                            final float baros12h = baro12h.update(baros);
                            if (bGraph) addGraphEntry(mChartBARO, leftAxisBARO, baros, baros12h, 2880);
                            b = 0;
                        }
                        b++;
                        mbaro = bar2mbar(items.get(2));
                    }

                } else if (ID.equals("RMC")) {// Fix valide = A
                    if (Objects.equals(items.get(2), "A")) {
                        msog = items.get(7);
                        mcog = ent(items.get(8));
                    }

                } else if (ID.equals("GGA")) {// Qualité : 1 = GPS, 2 = DGPS
                    msat = items.get(7);
                    if (Objects.equals(items.get(6), "1")) {
                        if (stddevStyle == Typeface.NORMAL) {
                            UpdateCellStyle(stddev, Typeface.ITALIC);
                            stddevStyle = Typeface.ITALIC;
                        }
                    } else if (Objects.equals(items.get(6), "2")) {
                        if (stddevStyle == Typeface.ITALIC) {
                            UpdateCellStyle(stddev, Typeface.NORMAL);
                            stddevStyle = Typeface.NORMAL;
                        }
                    }

                } else if (ID.equals("VDR")) {
                    mdrift = items.get(5);
                    mset = ent(items.get(3));

                } else if (ID.equals("MTW")) {
                    mwatertemp = items.get(1);

                } else if (ID.equals("DPT")) {
                    mdepth = items.get(1);

                } else if (ID.equals("ZPE")) {
                    mcible = items.get(1);
                    mcapab = ent(items.get(2));
                    mdopt = items.get(3);
                    final int opt = Integer.parseInt(items.get(3));
                    if (opt < 90)
                        mpopt = items.get(4);
                    else if (opt >= 90)
                        mpopt = items.get(5);

                } else if (ID.equals("RMB")) {
                    mdistwp = items.get(10);
                    mcapwp = ent(items.get(11));
                    mvmc = items.get(12);
                    mnomwp = "CAP " + items.get(4).substring(0, 4);

                } else if (ID.equals("GST")) {
                    Double lat = Double.parseDouble(items.get(6));
                    Double lon = Double.parseDouble(items.get(7));
                    mstddev = String.valueOf(roundTwoDecimals((float) Math.sqrt(lat*lat + lon*lon))) + "m";

                }
            }
        }

        // Retourne bars décimaux en mbar entiers
        private String bar2mbar(String v) {
            Double d = str2double(v)*1000;
            return String.valueOf(Math.round(d));
        }

        // Calcule le TWA ou AWA. Retourne une String entière
        private String getwa(String v) {
            if (str2double(v) < 180) return ent(v) + "<";
            else return ">" + String.valueOf(Math.round(360 - str2double(v)));
        }

        // Retourne une String entière en double
        private double str2double(String v) {
            return Double.parseDouble(v);
        }

        // Retourne une String entière
        public String ent(String v) {
            return String.valueOf(Math.round(str2double(v)));
        }

        // Retourne une String en valeur absolue
        public String abs(String v) {
            return v.substring(v.indexOf('-') + 1, v.length());
        }


        // Retourne un float arrondi à 2 décimales
        float roundTwoDecimals(float d) {
            DecimalFormat twoDForm = new DecimalFormat("#.##");
            return Float.valueOf(twoDForm.format(d));
        }


    }
}
