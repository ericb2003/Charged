package com.markbusman.charged;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.preference.PreferenceManager;

/**
 * Created by markbusman on 14/11/2015.
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    static final String ACTION_BOOT = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equalsIgnoreCase(ACTION_BOOT)) {
            // This intent action can only be set by the Android system after a boot
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            Boolean restartPref = sharedPref.getBoolean("resume_restart_switch", true);
            if (restartPref && isPlugged(context)) {
                Intent monitorIntent = new Intent(context, BatteryMonitorService.class);
                monitorIntent.putExtra(BatteryMonitorService.BATTERY_START_MONITORING, true);
                context.startService(monitorIntent);
            }
        }
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