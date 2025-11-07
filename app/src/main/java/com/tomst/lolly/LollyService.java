package com.tomst.lolly;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Binder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.tomst.lolly.core.BoundServiceListener;
import com.tomst.lolly.core.TDevState;
import com.tomst.lolly.core.TInfo;
import com.tomst.lolly.core.TMSReader;
import com.tomst.lolly.core.EventBusMSG;
import com.tomst.lolly.core.PhysicalData;
import com.tomst.lolly.core.PhysicalDataFormatter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class LollyService extends Service {

    private static final int ID = 1;                        // The id of the notification
    private String oldNotificationText = "";
    private NotificationCompat.Builder builder;
    private NotificationManager mNotificationManager;
    private boolean recordingState;
    private PowerManager.WakeLock wakeLock;                 // PARTIAL_WAKELOCK


    public void SetRunning(boolean val){
        ftTMS.SetRunning(val);
    };

    private Context mContext;

    public void enableLoop(boolean Enable){
        ftTMS.Enable(Enable);
    }

    public void SetServiceState(TDevState devState){

        ftTMS.SetDevState(devState);
    }

    TDevState GetDevState(){
        return ftTMS.GetDevState();
    }

    public void SetContext(Context context){
        mContext = context;
    }
    private TMSReader ftTMS;
    private BoundServiceListener mListener;
    private Looper serviceLooper;
    private ServiceHandler serviceHandler;

    // handlers for exchanging status, data and full  hardware flow between service and main activity
    private Handler dataHandler;
    private static Handler handler = null;
    private static Handler loghandler=null;
    public  void SetHandler(Handler han){
        this.handler = han;
    }
    public void SetDataHandler(Handler han) {
        this.dataHandler=han;
    }
    public void SetLogHandler(Handler han) {  this.loghandler=han; }
    private void sendDataProgress(TDevState stat, int pos) { // Handle sending message back to handler
        Message message = handler.obtainMessage();
        TInfo info = new TInfo();
        info.stat  = stat;
        info.msg   = String.valueOf(pos); // pozice
        info.idx = pos;                   // pozice progress baru
        message.obj = info;
        handler.sendMessage(message);
    }



    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override public void handleMessage(Message msg) {
            // Normally we would do some work here, like download a file.
            // For our sample, we just sleep for 5 seconds.
            try {
                sendDataProgress(TDevState.tLollyService, -1000);

                Log.w("lollyService", "[#] LollyService.java - handleMessage " + msg.arg1);

                ftTMS.Enable(true);
                if (!ftTMS.started)
                   ftTMS.start();

                Thread.sleep(500);

            } catch (InterruptedException e) {
                // Restore interrupt status.
                Thread.currentThread().interrupt();
            }
            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1);
        }
    }


    // constructor
    public LollyService() {
        mContext = null;
    }

    private final IBinder binder = new LollyBinder();
    public static final String PERMISSION_STRING
            = android.Manifest.permission.ACCESS_FINE_LOCATION;
    private LocationListener listener;
    private LocationManager locManager;

    public class LollyBinder extends Binder {
        public LollyService getOdometer() {
            return LollyService.this;  // vraci odkaz na instanci tridy
        }

        public void setListener(BoundServiceListener listener){
            mListener = listener;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // dle developer.android.com/guide/components/services#java
        HandlerThread thread = new HandlerThread("ServiceStartArguments",Thread.NORM_PRIORITY);
        thread.start();

        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);


        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"GPSLogger:wakelock");
        Log.w("myApp", "[#] GPSService.java - CREATE = onCreate");
        // Workaround for Nokia Devices, Android 9
        // https://github.com/BasicAirData/GPSLogger/issues/77
        if (EventBus.getDefault().isRegistered(this)) {
            //Log.w("myApp", "[#] GPSActivity.java - EventBus: GPSActivity already registered");
            EventBus.getDefault().unregister(this);
        }
        EventBus.getDefault().register(this);

    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent){
        ftTMS.SetRunning(false); // vylez z vycitaciho threadu
        ftTMS.stopAndRelease();
        super.onUnbind(intent);
        return true;
    }

    public void SetState(TDevState state){
        ftTMS.SetDevState(state);
    }

    public void startBindService(){
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job

        if (mContext == null){
            throw new UnsupportedOperationException("startBindService.mContext is null / (set app context !)");
        }
        //Context context = getContext();
        SharedPreferences sharedPref = mContext.getSharedPreferences(getString(R.string.save_options), mContext.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        if (ftTMS==null)
          ftTMS = new TMSReader(mContext);
        ftTMS.SetHandler(handler);
        ftTMS.SetDataHandler(this.dataHandler);
        ftTMS.SetLogHandler(this.loghandler);
        ftTMS.ConnectDevice();  // beware, first setup callback handlers before calling this
        sharedPref.getBoolean("checkoxBookmark", false);

        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = 12;
        serviceHandler.sendMessage(msg);
        ftTMS.SetRunning(true); // povol provoz v mLoop
     //  ftTMS.start();
    }

    /**
     * Creates and gets the Notification.
     *
     * @return the Notification
     */
    private Notification getNotification() {
        final String CHANNEL_ID = "GPSLoggerServiceChannel";

        recordingState = isIconRecording();
        builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        builder.setSmallIcon(R.drawable.ic_launcher_foreground)
       // builder.setSmallIcon(recordingState ? R.mipmap.ic_notify_recording_24dp : R.mipmap.ic_notify_24dp)
                .setColor(getResources().getColor(R.color.colorPrimaryLight))
                .setContentTitle(getString(R.string.app_name))
                .setShowWhen(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentText(composeContentText());

        final Intent startIntent = new Intent(getApplicationContext(), LollyActivity.class);
        startIntent.setAction(Intent.ACTION_MAIN);
        startIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        startIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 1, startIntent, PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(contentIntent);
        return builder.build();
    }

    /**
     * @return true if the icon should be the filled one, indicating that the app is recording.
     */
    private boolean isIconRecording () {
        return ((LollyActivity.getInstance().getGPSStatus() == LollyActivity.GPS_OK) && LollyActivity.getInstance().isRecording());
    }



    /**
     * @return The string to use as Notification description.
     */
    private String composeContentText () {
        String notificationText = "";
        int gpsStatus = LollyActivity.getInstance().getGPSStatus();
        switch (gpsStatus) {
            case LollyActivity.GPS_DISABLED:
                notificationText = getString(R.string.gps_disabled);
                break;
            case LollyActivity.GPS_OUTOFSERVICE:
                notificationText = getString(R.string.gps_out_of_service);
                break;
            case LollyActivity.GPS_TEMPORARYUNAVAILABLE:
            case LollyActivity.GPS_SEARCHING:
                notificationText = getString(R.string.gps_searching);
                break;
            case LollyActivity.GPS_STABILIZING:
                notificationText = getString(R.string.gps_stabilizing);
                break;
            case LollyActivity.GPS_OK:
                if (LollyActivity.getInstance().isRecording() && (LollyActivity.getInstance().getCurrentTrack() != null)) {
                    PhysicalDataFormatter phdformatter = new PhysicalDataFormatter();
                    PhysicalData phdDuration;
                    PhysicalData phdDistance;

                    // Duration
                    phdDuration = phdformatter.format(LollyActivity.getInstance().getCurrentTrack().getPrefTime(), PhysicalDataFormatter.FORMAT_DURATION);
                    if (phdDuration.value.isEmpty()) phdDuration.value = "00:00";
                    notificationText = getString(R.string.duration) + ": " + phdDuration.value;

                    // Distance (if available)
                    phdDistance = phdformatter.format(LollyActivity.getInstance().getCurrentTrack().getEstimatedDistance(), PhysicalDataFormatter.FORMAT_DISTANCE);
                    if (!phdDistance.value.isEmpty()) {
                        notificationText += " - " + getString(R.string.distance) + ": " + phdDistance.value + " " + phdDistance.um;
                    }
                } else {
                    notificationText = getString(R.string.notification_contenttext);
                }
        }
        return notificationText;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        startForeground(ID, getNotification());
        Log.w("myApp", "[#] GPSService.java - START = onStartCommand");
        return START_NOT_STICKY;
    }


    /**
     * The EventBus receiver for Short Messages.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Short msg) {
        if ((msg == EventBusMSG.UPDATE_FIX) && (builder != null)
                && ((Build.VERSION.SDK_INT < Build.VERSION_CODES.N) || (mNotificationManager.areNotificationsEnabled()))) {
            String notificationText = composeContentText();
            if (!oldNotificationText.equals(notificationText)) {
                builder.setContentText(notificationText);
                builder.setOngoing(true);                   // https://developer.android.com/develop/background-work/services/foreground-services#user-dismiss-notification
                if (isIconRecording() != recordingState) {
                    recordingState = isIconRecording();
//                    builder.setSmallIcon(recordingState ? R.mipmap.ic_notify_recording_24dp : R.mipmap.ic_notify_24dp);
                     builder.setSmallIcon(recordingState ? R.mipmap.ic_launcher_adaptive_fore : R.mipmap.ic_launcher_adaptive_back);

                }
                mNotificationManager.notify(ID, builder.build());
                oldNotificationText = notificationText;
                //Log.w("myApp", "[#] GPSService.java - Update Notification Text");
            }
        }
    }

    @Override
    public void onDestroy() {

        if (locManager != null && listener != null) {
            if (ContextCompat.checkSelfPermission(this, PERMISSION_STRING)
                    == PackageManager.PERMISSION_GRANTED) {
                locManager.removeUpdates(listener);
            }
            locManager = null;
            listener = null;
        }

        // NAKOPIROVANO Z GPSLogger
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            Log.w("myApp", "[#] GPSService.java - WAKELOCK released");
        }
        EventBus.getDefault().unregister(this);
        Log.w("myApp", "[#] GPSService.java - DESTROY = onDestroy");
        // THREAD FOR DEBUG PURPOSE
        //if (t.isAlive()) t.interrupt();
        super.onDestroy();

        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }


}