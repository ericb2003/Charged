package com.markbusman.charged;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by markbusman on 10/11/2015.
 */
public class SurfaceViewBatteryLevel extends SurfaceView implements SurfaceHolder.Callback {

    private int shaderColor = Color.WHITE;
    private static String shadeColor = "#8CD6DD";

    public float batteryPercent = 1;  // 1 - perc
    public float warningLevel = 0.98f;  // 98 / 100

    public SurfaceViewBatteryLevel(Context context) {
        super(context);
        shaderColor = Color.parseColor(shadeColor);
        getHolder().addCallback(this);
    }

    public SurfaceViewBatteryLevel(Context context, AttributeSet attrs) {
        super(context, attrs);
        shaderColor = Color.parseColor(shadeColor);
        getHolder().addCallback(this);
    }

    public SurfaceViewBatteryLevel(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        shaderColor = Color.parseColor(shadeColor);
        getHolder().addCallback(this);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(Color.WHITE);

        int width = canvas.getWidth();
        int height = canvas.getHeight();
        float actualHeight = (float) (getHeight() * batteryPercent);
        float powerHeight = (float) (getHeight() * warningLevel) - 5;

        Paint p = new Paint();
        p.setColor(Color.BLUE);
        p.setAntiAlias(true);
        p.setShader(new LinearGradient(0, 0, 0, getHeight(), Color.WHITE, shaderColor, Shader.TileMode.MIRROR));
        canvas.drawRect(0, 0, width, height, p);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        Boolean drawPref = sharedPref.getBoolean("battery_status_switch", true);

        if (drawPref) {
            p.setColor(Color.BLUE);
            p.setAntiAlias(true);
            p.setShader(new LinearGradient(0, 0, 0, height, Color.WHITE, Color.BLUE, Shader.TileMode.MIRROR));
            canvas.drawRect(0, actualHeight, width, height, p);
        }

        p = new Paint();
        p.setColor(Color.GREEN);
        p.setAntiAlias(true);
        canvas.drawRect(0, powerHeight , width, powerHeight + 10, p);

    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Canvas canvas = null;
        try {
            canvas = holder.lockCanvas(null);
            synchronized (holder) {
                draw(canvas);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (canvas != null) {
                holder.unlockCanvasAndPost(canvas);
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}