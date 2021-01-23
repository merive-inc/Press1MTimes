package com.merive.press1mtimes;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Html;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.preference.PreferenceManager;

import com.jetradarmobile.snowfall.SnowfallView;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

import static com.merive.press1mtimes.Rotation.runRotation;
import static java.util.Calendar.YEAR;


public class MainActivity extends AppCompatActivity
        implements SensorEventListener {

    static SharedPreferences sharedPreferences;
    static int score;
    static Boolean vibrationState;
    static Boolean accelerationState;
    Boolean notificationState;
    TextView label, counter, info;
    ImageButton button;
    SwitchCompat vibration, notification, acceleration;
    SensorManager sensorManager;
    Sensor accelerometer;
    float[] axisData = new float[3];

    public void vibration() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(250,
                    VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(250);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Press1MTimes);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createNotificationChannel();

        sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this.getBaseContext());

        counter = findViewById(R.id.counter);
        label = findViewById(R.id.label);
        button = findViewById(R.id.button);

        vibration = findViewById(R.id.vibration);
        notification = findViewById(R.id.notification);
        acceleration = findViewById(R.id.acceleration);
        info = findViewById(R.id.info);

        setCounter();
        setSwitches();
        setInfo();

        checkMonth();


        sensorManager = (SensorManager) getSystemService(
                Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER);
    }

    /* Set methods */
    public void setCounter() {
        StringBuilder score =
                new StringBuilder(sharedPreferences.getString("score", ""));
        // Add 0s for counter
        while (score.length() != 6)
            score.insert(0, "0");
        counter.setText(score);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void checkMonth() {
        Date date = new Date();
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        int month = localDate.getMonthValue();

        if (month == 12 || month == 1) {
            SnowfallView snow = findViewById(R.id.snow);
            snow.setVisibility(View.VISIBLE);
        }
    }


    public void setInfo() {
        String s = "Version: " + BuildConfig.VERSION_NAME +
                "\n@merive-studio, " + Calendar.getInstance().get(YEAR);
        info.setText(s);
    }

    public void setSwitches() {
        vibrationState = sharedPreferences.getBoolean("vibration", false);
        vibration.setChecked(vibrationState);

        notificationState = sharedPreferences.getBoolean("notification", false);
        notification.setChecked(notificationState);
        if (notificationState) setAlarm(12);

        accelerationState = sharedPreferences.getBoolean("acceleration", false);
        acceleration.setChecked(accelerationState);
    }

    /* Click methods */
    public void buttonClick(View view) {
        score = Integer.parseInt(String.valueOf(counter.getText()));
        if (score == 999999) {
            sharedPreferences.edit().putString("score", "000000").apply();
            counter.setText(R.string.counter);

            Intent intent = new Intent(this, Finish.class);
            startActivity(intent);
        } else {
            score += 1;
            @SuppressLint("DefaultLocale") String result = String.format("%06d", score);

            sharedPreferences.edit().putString("score", result).apply();
            counter.setText(result);
        }
        if (Integer.parseInt(sharedPreferences.getString("score",
                "000000")) % 100 == 0) {
            if (vibrationState)
                vibration();
        }
    }

    public void clickVibration(View view) {
        if (vibration.isChecked()) {
            sharedPreferences.edit().putBoolean("vibration", true).apply();
            vibrationState = true;
        } else {
            sharedPreferences.edit().putBoolean("vibration", false).apply();
            vibrationState = false;
        }
    }

    public void clickNotification(View view) {
        if (notification.isChecked()) {
            sharedPreferences.edit().putBoolean("notification", true).apply();
            notificationState = true;
            setAlarm(12);
        } else {
            sharedPreferences.edit().putBoolean("notification", false).apply();
            notificationState = false;
            offAlarm();
        }
    }

    public void setAlarm(int HOUR) {
        Intent intent = new Intent(MainActivity.this, Broadcast.class);
        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(MainActivity.this, 0, intent, 0);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());

        if (calendar.get(Calendar.HOUR_OF_DAY) >= HOUR) calendar.add(Calendar.DATE, 1);
        calendar.set(Calendar.HOUR_OF_DAY, HOUR);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, pendingIntent);
    }

    public void offAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(MainActivity.this, Broadcast.class);
        PendingIntent pendingIntent =
                PendingIntent.getService(MainActivity.this, 0, intent,
                        0);
        if (pendingIntent != null && alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Press1MTimesChannel";
            String description = "Channel for Press1MTimes";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel =
                    new NotificationChannel("notifyPress1MTimes", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void clickAcceleration(View view) {
        if (acceleration.isChecked()) {
            sharedPreferences.edit().putBoolean("acceleration", true).apply();
            accelerationState = true;
        } else {
            sharedPreferences.edit().putBoolean("acceleration", false).apply();
            accelerationState = false;

            runRotation(0, 0, label);
            runRotation(0, 0, counter);
            runRotation(0, 0, button);
        }
    }

    public void clickReset(View view) {
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    sharedPreferences.edit().putString("score", "000000").apply();
                    counter.setText(R.string.counter);
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    break;
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(Html.fromHtml("<font color='#C82A1E'>Are you sure?</font>"))
                .setPositiveButton(
                        Html.fromHtml("<font color='#C82A1E'>Yes</font>"), dialogClickListener)
                .setNegativeButton(
                        Html.fromHtml("<font color='#C82A1E'>No</font>"), dialogClickListener)
                .show();
    }


    /* Accelerometer methods */
    @Override
    protected void onStart() {
        super.onStart();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sharedPreferences.getBoolean("acceleration", false)) {
            int sensorType = sensorEvent.sensor.getType();
            if (sensorType == Sensor.TYPE_ACCELEROMETER) {
                axisData = sensorEvent.values.clone();

                runRotation(axisData[1], axisData[0], label);
                runRotation(axisData[1], axisData[0], counter);
                runRotation(axisData[1], axisData[0], button);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        /* Don't write */
    }
}
