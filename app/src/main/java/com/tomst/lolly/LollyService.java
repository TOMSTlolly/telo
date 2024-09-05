package com.tomst.lolly;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Binder;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import com.tomst.lolly.core.BoundServiceListener;
import com.tomst.lolly.core.TDevState;
import com.tomst.lolly.core.TInfo;
import com.tomst.lolly.core.TMSReader;

public class LollyService extends Service {

    public void SetRunning(boolean val){
        ftTMS.SetRunning(val);
    };

    private Context mContext;

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

    private Handler dataHandler;
    private static Handler handler = null;
    public void SetHandler(Handler han){
        this.handler = han;
    }
    public void SetDataHandler(Handler han) {
        this.dataHandler=han;
    }

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

        @Override
        public void handleMessage(Message msg) {
            // Normally we would do some work here, like download a file.
            // For our sample, we just sleep for 5 seconds.
            try {
                sendDataProgress(TDevState.tLollyService, -1000);
                /*
                for (int i=0;i<3600;i++) {
                    Thread.sleep(1000);
                    sendDataProgress(TDevState.tReadData, i);
                }
                */
                ftTMS.start();
                Thread.sleep(1000);

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
    }

    private void PrepareHardware(){

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


        ftTMS = new TMSReader(mContext);
        ftTMS.ConnectDevice();
        ftTMS.SetHandler(handler);
        ftTMS.SetDataHandler(this.dataHandler);

        ftTMS.SetRunning(true); // povol provoz v mLoop
        //ftTMS.start();

        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = 12;
        serviceHandler.sendMessage(msg);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locManager != null && listener != null) {
            if (ContextCompat.checkSelfPermission(this, PERMISSION_STRING)
                    == PackageManager.PERMISSION_GRANTED) {
                locManager.removeUpdates(listener);
            }
            locManager = null;
            listener = null;
        }
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }

}