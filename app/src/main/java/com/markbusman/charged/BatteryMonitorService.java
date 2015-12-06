package com.markbusman.charged;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.net.URI;
import java.util.HashMap;

import static com.markbusman.charged.R.string.pref_default_message;

/**
 * Created by markbusman on 14/11/2015.
 */
public class BatteryMonitorService extends Service {
    //public static Boolean isRunning = false;
    private static final String TAG = "BatteryMonitorService";
    public static final String BATTERY_UPDATE = "battery";
    public static final String HANDLE_REBOOT = "com.markbusman.Charged.HANDLE_REBBOT";
    public static final String BATTERY_STATUS_UPDATE = "com.markbusman.Charged.BATTERY_STATUS_UPDATE";
    public static final String BATTERY_STATUS_INFO = "com.markbusman.Charged.BATTERY_STATUS_INFO";
    public static final String BATTERY_STOP_MONITORING = "com.markbusman.Charged.BATTERY_STOP_MONITORING";
    public static final String BATTERY_START_MONITORING = "com.markbusman.Charged.BATTERY_START_MONITORING";

    public static final String notificationID = "com.markbusman.Charged.NOTIFICATION_ALERT";


    SoundPool soundPool;
    HashMap<Integer, Integer> soundPoolMap;
    int soundAlertID = 1;
    private String alarmSound = "airhorn";


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Boolean backgroundMonitoring = true;

        //if (intent != null && intent.hasExtra(BootReceiver.ACTION_BOOT)){
            //AlarmReceiver.startAlarms(BatteryMonitorService.this.getApplicationContext());

        //}
        if (intent != null && intent.hasExtra(BATTERY_UPDATE) && backgroundMonitoring){
                new BatteryCheckAsync().execute();
        }
        if (intent != null && intent.hasExtra(BATTERY_START_MONITORING) && backgroundMonitoring) {
            AlarmReceiver.startAlarms(BatteryMonitorService.this.getApplicationContext());
            //Log.d("BATTERY SERVICE", "start command received");
        }
        if (intent != null && intent.hasExtra(BATTERY_STOP_MONITORING) && backgroundMonitoring){
            AlarmReceiver.stopAlarms(BatteryMonitorService.this.getApplicationContext());
            this.stopSelf();
            //isRunning = false;
            //Log.d("BATTERY SERVICE", "stop command received");
        }


        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //broadcaster = LocalBroadcastManager.getInstance(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private class BatteryCheckAsync extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            //Battery State check - create log entries of current battery state
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = BatteryMonitorService.this.registerReceiver(null, ifilter);

            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            float batteryLevel = (level / (float) scale);
            //Log.i("BATTERY SERVICE BatteryInfo", "Battery charge level: " + batteryLevel);

            SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE);
            float warningLevel = sharedPref.getFloat(getString(R.string.warningLevel_preference_key), -1);

            SharedPreferences sharedUserPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String alertSetting = sharedUserPref.getString("alert_type_setting", "2");

            Log.d("alert settings", alertSetting);

            if (alertSetting.equals("0")) {
                return "-1";
            } else if (alertSetting.equals("1")) {
                if (batteryLevel == warningLevel && isPlugged(getApplicationContext())) {
                    return "1";
                }
            } else if (alertSetting.equals("2")) {
                if (batteryLevel >= warningLevel && isPlugged(getApplicationContext())) {
                    return "1";
                }
            }

            return "-1";
        }

        protected void onPostExecute(String result){
            //Log.d("BATTERY SERVICE", "POST EXEC: " + result);
            if (result.equals("1")) {
                showNotification();
            }

            /*
            Context tcontext = getApplicationContext();
            CharSequence text = "Checked Battery!";
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(tcontext, text, duration);
            toast.show();
            */

            if (!isPlugged(getApplicationContext())) {
                AlarmReceiver.stopAlarms(BatteryMonitorService.this.getApplicationContext());
            }

            BatteryMonitorService.this.stopSelf();
        }
    }

    @Override
    public boolean stopService(Intent name) {
        AlarmReceiver.stopAlarms(BatteryMonitorService.this.getApplicationContext());
        //isRunning = false;
        return super.stopService(name);
    }

    private void showNotification() {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String soundPref = sharedPref.getString("notifications_sound", "airhorn");
        String message = sharedPref.getString("notifications_sound", "The battery has reached the desired level");
        Uri soundFile = Uri.parse("android.resource://com.markbusman.charged/raw/" + soundPref);
        Boolean vibratePref = sharedPref.getBoolean("notifications_vibrate_switch", true);

        Log.d("notice sound", soundPref);

        long[] vibrate = {0,100,200,300};
        if (!vibratePref) {
            vibrate = new long[]{0l};
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_notification)
                        .setSound(soundFile)
                        .setVibrate(vibrate)
                        //.ledOnMS  = 200;    //Set led blink (Off in ms)
                        //.ledOffMS = 200;    //Set led blink (Off in ms)
                        //.ledARGB = 0x9400d3;   //Set led color
                        .setContentTitle("Charged Status")
                        .setContentText(message);


        int NOTIFICATION_ID = notificationID.hashCode();

        Intent targetIntent = new Intent(this, BatteryMonitorAlert.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, targetIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);
        NotificationManager nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nManager.notify(NOTIFICATION_ID, builder.build());

    }

    private static boolean isPlugged(Context context) {
        boolean isPlugged= false;
        Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        isPlugged = plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            isPlugged = isPlugged || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS;
        }
        return isPlugged;
    }
}
