package com.markbusman.charged;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import static com.markbusman.charged.BatteryMonitorService.*;

public class MainActivity extends AppCompatActivity {

    private IntentFilter mIntentFilter;
    private CheckBatStatus checkBatStatus;
    private Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SeekBar seekBar = (SeekBar) findViewById(R.id.seekBarBatteryLevelAlert);
        SurfaceViewBatteryLevel sfcBatteryLevel = (SurfaceViewBatteryLevel) findViewById(R.id.surfcvBatteryLevel);
        TextView percentageText = (TextView) findViewById(R.id.txtBatteryPercent);

        updatePlugState(isPlugged(this));
        serviceControl();

        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE);
        float warningLevel = sharedPref.getFloat(getString(R.string.warningLevel_preference_key), -1);
        if (warningLevel > -1) {
            seekBar.setProgress((int)(warningLevel * 100));
        } else {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putFloat(getString(R.string.warningLevel_preference_key), 0.98f);
            editor.commit();
            seekBar.setProgress(98);
        }

        updatePlugState(isPlugged(this));

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(BATTERY_STATUS_UPDATE);

        checkBatStatus = new CheckBatStatus();

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                TextView percentageText = (TextView) findViewById(R.id.txtBatteryPercent);
                percentageText.setText(progress + " %");
                SurfaceViewBatteryLevel sfcBatteryLevel = (SurfaceViewBatteryLevel) findViewById(R.id.surfcvBatteryLevel);
                float level = 1.0f - (progress / 100f);
                sfcBatteryLevel.warningLevel = level;
                //Log.d("Alert Level", sfcBatteryLevel.warningLevel + "");
                sfcBatteryLevel.invalidate();

                SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putFloat(getString(R.string.warningLevel_preference_key), progress / 100f);
                editor.commit();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        /*ImageButton startButton = (ImageButton) findViewById(R.id.imgButtonStart);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSound();
            }
        });*/

        ImageButton settingsButton = (ImageButton) findViewById(R.id.imageButtonSettings);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), SettingsActivity.class);
                startActivity(intent);
            }
        });

        percentageText.setText(seekBar.getProgress() + " %");
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = this.registerReceiver(null, ifilter);

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryPct = level / (float)scale;
        sfcBatteryLevel.warningLevel = 1.0f - (seekBar.getProgress() / 100f);
        sfcBatteryLevel.batteryPercent = 1 - batteryPct;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        checkBatStatus.cancel(true);
        //unregisterReceiver(mReceiver);
        //Log.d("ON PAUSE", "aPP Stopped");
        super.onStop();
    }

    @Override
    protected void onPause() {
        timer.cancel();
        checkBatStatus.cancel(true);
        unregisterReceiver(mReceiver);
       // Log.d("ON PAUSE", "aPP Paused");

        updatePlugState(isPlugged(this));

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
        callAsynchronousTask();
        updatePlugState(isPlugged(this));
    }

    private void updatePlugState(Boolean state) {
        //Log.d("PLUG STATE", state + "");

        ImageButton startButton = (ImageButton) findViewById(R.id.imgButtonStart);
        if (state) {
            startButton.setPressed(true);
        } else {
            startButton.setPressed(false);
        }
    }

    private static boolean isPlugged(Context context) {
        boolean isPlugged = false;
        Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        isPlugged = plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            isPlugged = isPlugged || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS;
        }
        return isPlugged;
    }

    private void serviceControl() {

        if (isPlugged(this) && !isMyServiceRunning(BatteryMonitorService.class)) {
            Intent monitorIntent = new Intent(this, BatteryMonitorService.class);
            monitorIntent.putExtra(BatteryMonitorService.BATTERY_START_MONITORING, true);
            this.startService(monitorIntent);
        } else {
            Intent monitorIntent = new Intent(this, BatteryMonitorService.class);
            this.stopService(monitorIntent);
            AlarmReceiver.stopAlarms(this);
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void callAsynchronousTask() {
        final Handler handler = new Handler();
        timer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            checkBatStatus = new CheckBatStatus();
                            // PerformBackgroundTask this class is the class that extends AsynchTask
                            checkBatStatus.execute();
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                        }
                    }
                });
            }
        };
        timer.schedule(doAsynchronousTask, 0, 5000); //execute in every 5000 ms

    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Log.d("POWER STATE", "Received from power connection");
            updatePlugState(isPlugged(context));
        }
    };


    private class CheckBatStatus extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            //Battery State check - create log entries of current battery state
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = registerReceiver(null, ifilter);

            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;
            //Log.i("BatteryInfo Thread", "Battery is charging: " + isCharging);

            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            float batteryLevel = (level / (float) scale);
            //Log.i("BatteryInfo Thread", "Battery charge level: " + batteryLevel);

            return batteryLevel + "";
        }

        @Override
        protected void onPostExecute(String result) {
            updatePlugState(isPlugged(getApplicationContext()));
            float level = Float.parseFloat(result);
            SurfaceViewBatteryLevel sfcBatteryLevel = (SurfaceViewBatteryLevel) findViewById(R.id.surfcvBatteryLevel);
            sfcBatteryLevel.batteryPercent = 1 - level;
            sfcBatteryLevel.invalidate();
        }


        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }

}
