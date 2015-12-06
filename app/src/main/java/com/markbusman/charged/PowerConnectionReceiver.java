package com.markbusman.charged;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import static com.markbusman.charged.BatteryMonitorService.BATTERY_STATUS_INFO;
import static com.markbusman.charged.BatteryMonitorService.BATTERY_STATUS_UPDATE;

/**
 * Created by markbusman on 13/11/2015.
 */
public class PowerConnectionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean alertPref = sharedPref.getBoolean("notifications_power_alert_switch", true);

        if (isPlugged(context)) {
            if (!isMyServiceRunning(BatteryMonitorService.class, context)) {
                Intent monitorIntent = new Intent(context, BatteryMonitorService.class);
                monitorIntent.putExtra(BatteryMonitorService.BATTERY_START_MONITORING, true);
                context.startService(monitorIntent);
            }

            if (alertPref) {
                Context tcontext = context.getApplicationContext();
                CharSequence text = "Monitoring Battery!";
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(tcontext, text, duration);
                toast.show();
            }
        } else {
            Intent monitorIntent = new Intent(context, BatteryMonitorService.class);
            context.stopService(monitorIntent);
            AlarmReceiver.stopAlarms(context);

            if (alertPref) {
                Context tcontext = context.getApplicationContext();
                CharSequence text = "Battery Monitoring Stopped!";
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(tcontext, text, duration);
                toast.show();
            }
        }

        /*
        Log.d("Battery Status", "received");
        Boolean monitorBackground = true;
        //int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        //boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        //        status == BatteryManager.BATTERY_STATUS_FULL;
        Intent intentPower = new Intent(BATTERY_STATUS_UPDATE);
        intentPower.putExtra(BATTERY_STATUS_INFO, 1);
        context.sendBroadcast(intentPower);

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);

        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;
        Log.i("BatteryInfo PWERCOnnection", "Battery is charging: " + isCharging);

        if (isCharging && monitorBackground) {
            Log.d("Power receiver", "charging");
            Intent monitorIntent = new Intent(context, BatteryMonitorService.class);
            monitorIntent.putExtra(BatteryMonitorService.BATTERY_START_MONITORING, true);
            if (!BatteryMonitorService.isRunning) {
                context.startService(monitorIntent);
            }

            Context tcontext = context.getApplicationContext();
            CharSequence text = "Monitoring Battery!";
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(tcontext, text, duration);
            toast.show();

        } else {
            Log.d("Power receiver", "Not charging");
            Intent monitorIntent = new Intent(context, BatteryMonitorService.class);
            monitorIntent.putExtra(BatteryMonitorService.BATTERY_START_MONITORING, true);
            context.stopService(monitorIntent);

            Context tcontext = context.getApplicationContext();
            CharSequence text = "Battery Monitoring Stopped!";
            int duration = Toast.LENGTH_SHORT;

            //Toast toast = Toast.makeText(tcontext, text, duration);
            //toast.show();
        }*/
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

    private boolean isMyServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager)  context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}