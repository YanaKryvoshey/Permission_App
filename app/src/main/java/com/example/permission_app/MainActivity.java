package com.example.permission_app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.security.Policy;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final int PERMISSION_CAMERA_REQUEST_CODE = 123;
    private static final int MANUALLY_CAMERA_PERMISSION_REQUEST_CODE = 124;

    private MaterialButton main_BTN_enter;
    private TextView main_LBL_info;
    private TextInputEditText main_EDT_Password;
    private Switch main_SWT_flash;
    private TextView main_LBL_degree;
    private ImageView main_IMG_compass;
    private boolean flashLightStatus = false;
    CameraManager cameraManager;

    private SensorManager sensorManager;
    private Sensor sensorAccelerometer;
    private Sensor sensorMagneticField;

    private float[] floatGravity = new float[3];
    private float[] floatGeoMagnetic = new float[3];
    private float[] floatRotationMatrix = new float[9];
    private float[] floatOrientation = new float[3];

    boolean GravityCopy = true;
    boolean GeoMagneticCopy = true;

    long lastUpdatedTime = 0;
    float currentDegree = 0f;
    int degree = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        checkIfCameraAvailable();
        moveCompass();
        flashAction();
        permissionAction();
    }

    //press on Permission button
    private void permissionAction() {
        main_BTN_enter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSecondActivity();
            }
        });
    }
//if the password is the num of battery charging and the volume on maximum volume and the flash is on and compass Rotated 152 degree then open new activity
    private void openSecondActivity() {
        BatteryManager bm = (BatteryManager) MainActivity.this.getSystemService(BATTERY_SERVICE);
        int batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);


        if (batLevel == Integer.parseInt(main_EDT_Password.getText().toString()) && flashLightStatus == true && currentVolume == maxVolume && degree == 152) {
            Toast.makeText(MainActivity.this, "The Battery is Right", Toast.LENGTH_SHORT).show();
            passin();
        }

    }
//open new activity
    private void passin() {
        Intent myIntent = new Intent(MainActivity.this, SecondActivity.class);
        startActivity(myIntent);
    }

    //press on flash on/off
    private void flashAction() {
        main_SWT_flash.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    main_SWT_flash.setText("Flash OFF");
                    flashLightOn();
                } else {
                    main_SWT_flash.setText("Flash ON");
                    flashLightOff();
                }
            }
        });
    }

    private void flashLightOn() {
        try {
            String cameraId = cameraManager.getCameraIdList()[0];
            cameraManager.setTorchMode(cameraId, true);
            flashLightStatus = true;
        } catch (CameraAccessException e) {
        }
    }

    private void flashLightOff() {
        try {
            String cameraId = cameraManager.getCameraIdList()[0];
            cameraManager.setTorchMode(cameraId, false);
            flashLightStatus = false;
        } catch (CameraAccessException e) {
        }
    }

    private void moveCompass() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorMagneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

//react on moving compass
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == sensorAccelerometer) {
            System.arraycopy(event.values, 0, floatGravity, 0, event.values.length);
            GravityCopy = true;
        } else if (event.sensor == sensorMagneticField) {
            System.arraycopy(event.values, 0, floatGeoMagnetic, 0, event.values.length);
            GeoMagneticCopy = true;
        }
        if (GravityCopy && GeoMagneticCopy && System.currentTimeMillis() - lastUpdatedTime > 250) {
            SensorManager.getRotationMatrix(floatRotationMatrix, null, floatGravity, floatGeoMagnetic);
            SensorManager.getOrientation(floatRotationMatrix, floatOrientation);

            float azimuthInRadians = floatOrientation[0];
            float azimuthInDegree = (float) Math.toDegrees(azimuthInRadians);

            RotateAnimation rotateAnimation = new RotateAnimation(currentDegree, -azimuthInDegree, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            rotateAnimation.setFillAfter(true);
            main_IMG_compass.startAnimation(rotateAnimation);

            currentDegree = -azimuthInDegree;
            lastUpdatedTime = System.currentTimeMillis();

            degree = (int) azimuthInDegree;
            main_LBL_degree.setText(degree + "°");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, sensorMagneticField, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this, sensorAccelerometer);
        sensorManager.unregisterListener(this, sensorMagneticField);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
       // Toast.makeText(MainActivity.this, "Compass Accuracy Changed", Toast.LENGTH_SHORT).show();
    }

    private void checkIfCameraAvailable() {
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                Toast.makeText(MainActivity.this, "there is flash", Toast.LENGTH_SHORT).show();
                main_SWT_flash.setEnabled(true);
            } else {
                Toast.makeText(MainActivity.this, "there is no flash you can't login", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(MainActivity.this, "there is no camera you can't login", Toast.LENGTH_SHORT).show();
        }
    }

    private void initViews() {
        main_BTN_enter = findViewById(R.id.main_BTN_enter);
        main_LBL_info = findViewById(R.id.main_LBL_enterPassword);
        main_EDT_Password = findViewById(R.id.main_EDT_Password);
        main_SWT_flash = findViewById(R.id.main_SWT_flash);
        main_LBL_degree = findViewById(R.id.main_LBL_degree);
        main_IMG_compass = findViewById(R.id.main_IMG_compass);
    }
}