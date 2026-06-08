package com.fengshui.compass;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.app.Activity;
import android.view.WindowManager;

/**
 * 风水罗盘主Activity
 * 使用加速度计和磁力计传感器计算方向
 */
public class MainActivity extends Activity implements SensorEventListener {

    private CompassView compassView;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;

    private float[] gravity;
    private float[] geomagnetic;
    private float azimuth = 0f;

    // 低通滤波器平滑角度
    private float filteredAzimuth = 0f;
    private static final float ALPHA = 0.15f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        // 初始化罗盘View
        compassView = findViewById(R.id.compassView);

        // 初始化传感器
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 注册传感器监听
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
        if (magnetometer != null) {
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 取消注册
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravity = lowPassFilter(event.values.clone(), gravity);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic = lowPassFilter(event.values.clone(), geomagnetic);
        }

        if (gravity != null && geomagnetic != null) {
            float[] R = new float[9];
            float[] I = new float[9];

            if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(R, orientation);

                // 计算方位角（弧度 -> 度）
                azimuth = (float) Math.toDegrees(orientation[0]);

                // 归一化到 0-360
                azimuth = (azimuth + 360) % 360;

                // 低通滤波平滑
                filteredAzimuth = lowPassAngle(azimuth, filteredAzimuth);

                // 更新罗盘显示
                compassView.setAzimuth(filteredAzimuth);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // 传感器精度变化时可以做一些处理
    }

    /**
     * 低通滤波器 - 平滑传感器数据
     */
    private float[] lowPassFilter(float[] input, float[] output) {
        if (output == null) return input;

        final float alpha = 0.8f;
        for (int i = 0; i < input.length; i++) {
            output[i] = alpha * output[i] + (1 - alpha) * input[i];
        }
        return output;
    }

    /**
     * 角度低通滤波 - 处理角度跨越360度的边界情况
     */
    private float lowPassAngle(float current, float previous) {
        float delta = current - previous;

        // 处理跨越360度/0度的情况
        if (delta > 180) {
            delta -= 360;
        } else if (delta < -180) {
            delta += 360;
        }

        float filtered = previous + delta * ALPHA;

        // 归一化
        if (filtered < 0) {
            filtered += 360;
        } else if (filtered >= 360) {
            filtered -= 360;
        }

        return filtered;
    }
}
