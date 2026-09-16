package com.hafiztraveltours.app.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.hafiztraveltours.app.R;

import java.util.Locale;

public class QiblaActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "QiblaCompassDebug";

    // State machine enum
    private enum QiblaState {
        SEARCHING,
        HOLDING,
        CONFIRMED
    }

    private QiblaState currentState = QiblaState.SEARCHING;

    // Coordinates of Kaaba in Makkah, Saudi Arabia
    private static final double KAABA_LATITUDE = 21.422487;
    private static final double KAABA_LONGITUDE = 39.826206;

    private ImageView ivCompassDial; // ONLY the Red Qibla Needle View
    private ImageView ivQiblaPointer;
    private TextView tvQiblaLocation;
    private TextView tvQiblaStatus;
    private TextView tvQiblaStatusSub;
    private TextView tvQiblaDegrees;
    private TextView tvQiblaWarning;
    private MaterialButton btnGrantLocation;
    private ImageView ivRefreshQiblaLocation;

    private TextView tvKaabaDistance;
    private TextView tvRelativeAngleOffset;

    private SensorManager sensorManager;
    private Sensor rotationVectorSensor;
    private Sensor accelerometer;
    private Sensor magnetometer;

    private float[] rotationMatrix = new float[9];
    private float[] orientationValues = new float[3];
    private float[] gravityValues = new float[3];
    private float[] geomagneticValues = new float[3];
    private boolean hasGravity = false;
    private boolean hasGeomagnetic = false;

    private double userLatitude = 1.4927; // default JB fallback
    private double userLongitude = 103.7414;
    private double qiblaBearing = 292.5; // default JB bearing

    private static final float ALIGNMENT_TOLERANCE_DEG = 5.0f;
    private static final long CONFIRMATION_TOTAL_MS = 2000L;
    private static final long TIMER_INTERVAL_MS = 100L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private long alignmentStartTimeMs = 0L;

    private final Runnable timerUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentState != QiblaState.HOLDING) return;

            long elapsed = System.currentTimeMillis() - alignmentStartTimeMs;
            long remainingMs = Math.max(0L, CONFIRMATION_TOTAL_MS - elapsed);
            float remainingSec = remainingMs / 1000.0f;

            if (tvQiblaStatus != null) {
                tvQiblaStatus.setText(String.format(Locale.getDefault(), getString(R.string.qibla_hold_steady_timer), remainingSec));
            }

            if (elapsed >= CONFIRMATION_TOTAL_MS) {
                transitionToState(QiblaState.CONFIRMED);
            } else {
                handler.postDelayed(this, TIMER_INTERVAL_MS);
            }
        }
    };

    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    if (tvQiblaWarning != null) tvQiblaWarning.setVisibility(View.GONE);
                    if (btnGrantLocation != null) btnGrantLocation.setVisibility(View.GONE);
                    fetchLocationAndCalculateQibla();
                } else {
                    if (tvQiblaWarning != null) {
                        tvQiblaWarning.setText(R.string.qibla_permission_needed);
                        tvQiblaWarning.setVisibility(View.VISIBLE);
                    }
                    if (btnGrantLocation != null) btnGrantLocation.setVisibility(View.VISIBLE);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qibla);

        ivCompassDial = findViewById(R.id.ivCompassDial);
        ivQiblaPointer = findViewById(R.id.ivQiblaPointer);
        tvQiblaLocation = findViewById(R.id.tvQiblaLocation);
        tvQiblaStatus = findViewById(R.id.tvQiblaStatus);
        tvQiblaStatusSub = findViewById(R.id.tvQiblaStatusSub);
        tvQiblaDegrees = findViewById(R.id.tvQiblaDegrees);
        tvQiblaWarning = findViewById(R.id.tvQiblaWarning);
        btnGrantLocation = findViewById(R.id.btnGrantLocation);
        ivRefreshQiblaLocation = findViewById(R.id.ivRefreshQiblaLocation);
        tvKaabaDistance = findViewById(R.id.tvKaabaDistance);
        tvRelativeAngleOffset = findViewById(R.id.tvRelativeAngleOffset);

        View btnBack = findViewById(R.id.btnBackQibla);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (btnGrantLocation != null) {
            btnGrantLocation.setOnClickListener(v ->
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION));
        }

        if (ivRefreshQiblaLocation != null) {
            ivRefreshQiblaLocation.setOnClickListener(v -> fetchLocationAndCalculateQibla());
        }

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }

        if (rotationVectorSensor == null && (accelerometer == null || magnetometer == null)) {
            if (tvQiblaWarning != null) {
                tvQiblaWarning.setText(R.string.qibla_no_sensor);
                tvQiblaWarning.setVisibility(View.VISIBLE);
            }
        } else {
            TextView btnCalibrate = findViewById(R.id.btnCalibrateCompass);
            if (btnCalibrate != null) {
                btnCalibrate.setVisibility(View.VISIBLE);
                btnCalibrate.setOnClickListener(v -> showCalibrationDialog());
            }
        }

        transitionToState(QiblaState.SEARCHING);
        checkPermissionAndLocate();
    }

    private void showCalibrationDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.qibla_calibrate_dialog_title)
                .setMessage(R.string.qibla_calibrate_dialog_msg)
                .setPositiveButton(R.string.qibla_calibrate_dialog_ok, (dialog, which) -> {
                    hasGravity = false;
                    hasGeomagnetic = false;
                    dialog.dismiss();
                })
                .show();
    }

    private void checkPermissionAndLocate() {
        boolean hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (hasFine || hasCoarse) {
            fetchLocationAndCalculateQibla();
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void fetchLocationAndCalculateQibla() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm != null) {
            Location lastLoc = null;
            try {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    lastLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (lastLoc == null) {
                        lastLoc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }
                }
            } catch (Exception ignored) {}

            if (lastLoc != null) {
                userLatitude = lastLoc.getLatitude();
                userLongitude = lastLoc.getLongitude();
            }

            qiblaBearing = calculateQiblaBearing(userLatitude, userLongitude);
            updateLocationAndDistanceUI();

            try {
                LocationListener listener = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location loc) {
                        if (loc != null) {
                            userLatitude = loc.getLatitude();
                            userLongitude = loc.getLongitude();
                            qiblaBearing = calculateQiblaBearing(userLatitude, userLongitude);
                            updateLocationAndDistanceUI();
                        }
                        try { lm.removeUpdates(this); } catch (Exception ignored) {}
                    }
                    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
                    @Override public void onProviderEnabled(String provider) {}
                    @Override public void onProviderDisabled(String provider) {}
                };
                if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    lm.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, getMainLooper());
                } else if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, getMainLooper());
                }
            } catch (Exception ignored) {}
        }
    }

    private void updateLocationAndDistanceUI() {
        if (tvQiblaLocation != null) {
            tvQiblaLocation.setText(String.format(Locale.getDefault(), "Lat: %.2f°, Lon: %.2f°", userLatitude, userLongitude));
        }

        if (tvQiblaDegrees != null) {
            tvQiblaDegrees.setText(String.format(Locale.getDefault(), "%.0f°", qiblaBearing));
        }

        double distKm = calculateDistanceToKaaba(userLatitude, userLongitude);
        if (tvKaabaDistance != null) {
            tvKaabaDistance.setText(String.format(Locale.getDefault(), "%,.0f km", distKm));
        }
    }

    private double calculateDistanceToKaaba(double lat, double lon) {
        double R = 6371.0; // Earth radius in km
        double dLat = Math.toRadians(KAABA_LATITUDE - lat);
        double dLon = Math.toRadians(KAABA_LONGITUDE - lon);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(KAABA_LATITUDE)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private double calculateQiblaBearing(double lat, double lon) {
        double userLatRad = Math.toRadians(lat);
        double userLonRad = Math.toRadians(lon);
        double kaabaLatRad = Math.toRadians(KAABA_LATITUDE);
        double kaabaLonRad = Math.toRadians(KAABA_LONGITUDE);

        double lonDiff = kaabaLonRad - userLonRad;

        double y = Math.sin(lonDiff);
        double x = Math.cos(userLatRad) * Math.tan(kaabaLatRad) - Math.sin(userLatRad) * Math.cos(lonDiff);

        double qiblaRad = Math.atan2(y, x);
        double qiblaDeg = Math.toDegrees(qiblaRad);

        return (qiblaDeg + 360) % 360;
    }

    private float filteredAzimuth = -1f;
    private float lastDisplayedAngle = -999f;
    private static final float HEADING_SMOOTH_ALPHA = 0.15f;
    private static final float MIN_HEADING_CHANGE_THRESHOLD = 0.3f;

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null) {
            if (rotationVectorSensor != null) {
                sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI);
            } else {
                if (accelerometer != null) {
                    sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
                }
                if (magnetometer != null) {
                    sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        cancelTimer();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    private static final float ALPHA = 0.25f;

    private float[] lowPassFilter(float[] input, float[] output) {
        if (output == null) return input;
        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float rawAzimuthInDegrees = 0f;
        boolean hasOrientation = false;

        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            SensorManager.getOrientation(rotationMatrix, orientationValues);
            rawAzimuthInDegrees = (float) Math.toDegrees(orientationValues[0]);
            rawAzimuthInDegrees = (rawAzimuthInDegrees + 360) % 360;
            hasOrientation = true;
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravityValues = lowPassFilter(event.values, gravityValues);
            hasGravity = true;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagneticValues = lowPassFilter(event.values, geomagneticValues);
            hasGeomagnetic = true;

            float magStrength = (float) Math.sqrt(
                    event.values[0] * event.values[0] +
                    event.values[1] * event.values[1] +
                    event.values[2] * event.values[2]);
            if (magStrength < 25f || magStrength > 70f) {
                if (tvQiblaWarning != null) {
                    tvQiblaWarning.setText(R.string.qibla_interference);
                    tvQiblaWarning.setVisibility(View.VISIBLE);
                }
            } else if (tvQiblaWarning != null && getString(R.string.qibla_interference).equals(tvQiblaWarning.getText())) {
                tvQiblaWarning.setVisibility(View.GONE);
            }
        }

        if (!hasOrientation && hasGravity && hasGeomagnetic) {
            float[] R_matrix = new float[9];
            float[] I_matrix = new float[9];
            boolean success = SensorManager.getRotationMatrix(R_matrix, I_matrix, gravityValues, geomagneticValues);
            if (success) {
                SensorManager.getOrientation(R_matrix, orientationValues);
                rawAzimuthInDegrees = (float) Math.toDegrees(orientationValues[0]);
                rawAzimuthInDegrees = (rawAzimuthInDegrees + 360) % 360;
                hasOrientation = true;
            }
        }

        if (hasOrientation) {
            if (filteredAzimuth < 0) {
                filteredAzimuth = rawAzimuthInDegrees;
            } else {
                float delta = rawAzimuthInDegrees - filteredAzimuth;
                if (delta > 180f) delta -= 360f;
                if (delta < -180f) delta += 360f;
                filteredAzimuth = (filteredAzimuth + HEADING_SMOOTH_ALPHA * delta + 360f) % 360f;
            }

            float phoneHeading = filteredAzimuth;
            float relativeQiblaAngle = (float) ((qiblaBearing - phoneHeading + 360) % 360);

            float angleChange = Math.abs(relativeQiblaAngle - lastDisplayedAngle);
            if (angleChange > 180f) angleChange = 360f - angleChange;

            if (lastDisplayedAngle < -900f || angleChange >= MIN_HEADING_CHANGE_THRESHOLD) {
                lastDisplayedAngle = relativeQiblaAngle;

                Log.d(TAG, String.format(Locale.US,
                        "phoneHeading: %.2f° | qiblaBearing: %.2f° | relativeQiblaAngle: %.2f°",
                        phoneHeading, qiblaBearing, relativeQiblaAngle));

                if (ivCompassDial != null) {
                    ivCompassDial.setRotation(relativeQiblaAngle);
                }

                float diffDeg = (float) (qiblaBearing - phoneHeading);
                while (diffDeg > 180f) diffDeg -= 360f;
                while (diffDeg < -180f) diffDeg += 360f;

                float absDiff = Math.abs(diffDeg);

                if (tvRelativeAngleOffset != null) {
                    if (absDiff <= ALIGNMENT_TOLERANCE_DEG) {
                        tvRelativeAngleOffset.setText(R.string.qibla_angle_aligned);
                    } else if (diffDeg > 0) {
                        tvRelativeAngleOffset.setText(String.format(Locale.getDefault(), getString(R.string.qibla_angle_right), Math.round(absDiff)));
                    } else {
                        tvRelativeAngleOffset.setText(String.format(Locale.getDefault(), getString(R.string.qibla_angle_left), Math.round(absDiff)));
                    }
                }

                // STATE MACHINE EVALUATION
                if (absDiff <= ALIGNMENT_TOLERANCE_DEG) {
                    if (currentState == QiblaState.SEARCHING) {
                        transitionToState(QiblaState.HOLDING);
                    }
                } else {
                    if (currentState != QiblaState.SEARCHING) {
                        transitionToState(QiblaState.SEARCHING);
                    }
                }
            }
        }
    }

    private void transitionToState(QiblaState newState) {
        cancelTimer();
        currentState = newState;

        switch (newState) {
            case SEARCHING:
                if (tvQiblaStatus != null) {
                    tvQiblaStatus.setText(R.string.qibla_instruction);
                    tvQiblaStatus.setTextColor(ContextCompat.getColor(this, R.color.pink_dark));
                    tvQiblaStatus.setBackgroundResource(R.drawable.bg_prayer_countdown_pill);
                }
                if (tvQiblaStatusSub != null) tvQiblaStatusSub.setVisibility(View.GONE);
                if (ivQiblaPointer != null) ivQiblaPointer.setVisibility(View.GONE);
                break;

            case HOLDING:
                alignmentStartTimeMs = System.currentTimeMillis();
                if (tvQiblaStatus != null) {
                    tvQiblaStatus.setText(String.format(Locale.getDefault(), getString(R.string.qibla_hold_steady_timer), 2.0f));
                    tvQiblaStatus.setTextColor(ContextCompat.getColor(this, R.color.pink_dark));
                    tvQiblaStatus.setBackgroundResource(R.drawable.bg_prayer_countdown_pill);
                }
                if (tvQiblaStatusSub != null) tvQiblaStatusSub.setVisibility(View.GONE);
                if (ivQiblaPointer != null) ivQiblaPointer.setVisibility(View.GONE);

                handler.postDelayed(timerUpdateRunnable, TIMER_INTERVAL_MS);
                break;

            case CONFIRMED:
                if (tvQiblaStatus != null) {
                    tvQiblaStatus.setText(R.string.qibla_confirmed_aligned);
                    tvQiblaStatus.setTextColor(ContextCompat.getColor(this, R.color.white));
                    tvQiblaStatus.setBackgroundResource(R.drawable.bg_button_pink);
                }
                if (tvQiblaStatusSub != null) {
                    tvQiblaStatusSub.setText(R.string.qibla_facing_sub);
                    tvQiblaStatusSub.setVisibility(View.VISIBLE);
                }
                if (ivQiblaPointer != null) ivQiblaPointer.setVisibility(View.VISIBLE);

                triggerSubtleVibration();
                break;
        }
    }

    private void cancelTimer() {
        handler.removeCallbacks(timerUpdateRunnable);
    }

    private void triggerSubtleVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vibratorManager = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                if (vibratorManager != null) {
                    Vibrator vibrator = vibratorManager.getDefaultVibrator();
                    if (vibrator.hasVibrator()) {
                        vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE));
                    }
                }
            } else {
                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        vibrator.vibrate(80);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Vibration unavailable: " + e.getMessage());
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD && accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            if (tvQiblaWarning != null && tvQiblaWarning.getVisibility() != View.VISIBLE) {
                tvQiblaWarning.setText(R.string.qibla_accuracy_notice);
            }
        }
    }
}
