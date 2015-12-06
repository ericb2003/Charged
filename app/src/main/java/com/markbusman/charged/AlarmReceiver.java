package com.markbusman.charged;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;
import android.util.Log;

/**
 * Created by markbusman on 14/11/2015.
 */
public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";
    private static final int REQUEST_CODE = 777;
    public static final long ALARM_INTERVAL = DateUtils.MINUTE_IN_MILLIS;


    // Call this from your service
    public static void startAlarms(final Context context) {
        final AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        // start alarm right away
        manager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, ALARM_INTERVAL,
                getAlarmIntent(context));
    }

    public static void stopAlarms(final Context context) {
        final AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        // stop alarm right away
        manager.cancel(getAlarmIntent(context));
    }

    /*
     * Creates the PendingIntent used for alarms of this receiver.
     */
    private static PendingIntent getAlarmIntent(final Context context) {
        return PendingIntent.getBroadcast(context, REQUEST_CODE, new Intent(context, AlarmReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {


        if (context == null) {
            // Somehow you've lost your context; this really shouldn't happen
            return;
        }
        if (intent == null){
            // No intent was passed to your receiver; this also really shouldn't happen
            return;
        }
        if (intent.getAction() == null) {
            //Log.d("Alarm Receiver", "alarm notice received");
            // If you called your Receiver explicitly, this is what you should expect to happen
            Intent monitorIntent = new Intent(context, BatteryMonitorService.class);
            monitorIntent.putExtra(BatteryMonitorService.BATTERY_UPDATE, true);
            context.startService(monitorIntent);

        }
        if (intent != null && intent.hasExtra(BatteryMonitorService.BATTERY_STOP_MONITORING)){
            //AlarmReceiver.startAlarms(BatteryMonitorService.this.getApplicationContext());
            //Log.d("alarm receiver", "stop alarms from being received");
            Intent monitorIntent = new Intent(context, BatteryMonitorService.class);
            monitorIntent.putExtra(BatteryMonitorService.BATTERY_UPDATE, true);
            context.stopService(monitorIntent);
        }
    }
}