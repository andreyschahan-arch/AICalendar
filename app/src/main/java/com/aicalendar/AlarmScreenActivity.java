package com.aicalendar;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class AlarmScreenActivity extends Activity {

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;

    private float targetVolume = 0.7f;
    private float currentVolume = 0.0f;
    private Handler volumeHandler = new Handler(Looper.getMainLooper());
    private Runnable volumeRunnable;

    private Handler autoSilenceHandler = new Handler(Looper.getMainLooper());
    private Runnable autoSilenceRunnable;

    private int repeatIntervalMin = 5;
    private int maxRepeatCount = 3;
    private int currentRepeatCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Принудительный показ поверх экрана блокировки для Android 8.1 - 15 (EMUI 15)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager != null) {
                keyguardManager.requestDismissKeyguard(this, null);
            }
        }

        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN
            | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_FULLSCREEN 
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY 
            );
        }

        final String title = getIntent().getStringExtra("ALARM_TITLE");
        final String task = getIntent().getStringExtra("ALARM_TASK");

        SharedPreferences prefs = getSharedPreferences("alarm_settings", Context.MODE_PRIVATE);
        repeatIntervalMin = prefs.getInt("snooze_interval", 5); 
        maxRepeatCount = prefs.getInt("snooze_repeat_count", 3);
        currentRepeatCount = getIntent().getIntExtra("CURRENT_AUTO_REPEAT_COUNT", 0);

        try {
            String uriString = getIntent().getStringExtra("RINGTONE_URI");
            if (uriString == null || uriString.isEmpty()) {
                uriString = prefs.getString("ringtone_uri", null);
            }

            Uri alarmUri = (uriString != null && !uriString.isEmpty()) ? Uri.parse(uriString) : android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM);

            mediaPlayer = new MediaPlayer();

            // 2. Исправление звукового потока под EMUI (USAGE_ALARM)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
											   .setUsage(AudioAttributes.USAGE_ALARM)
											   .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
											   .build());
            } else {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            }

            int savedVolumeInt = prefs.getInt("alarm_volume", 70);
            targetVolume = savedVolumeInt / 100f;

            boolean success = false;
            if (alarmUri != null) {
                try {
                    mediaPlayer.setDataSource(this, alarmUri);
                    success = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (!success) {
                Uri defaultUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM);
                if (defaultUri == null) {
                    defaultUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE);
                }
                if (defaultUri != null) {
                    mediaPlayer.reset();
                    mediaPlayer.setDataSource(this, defaultUri);
                }
            }

            mediaPlayer.setVolume(0, 0);
            mediaPlayer.setLooping(true);

            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
					@Override
					public void onPrepared(MediaPlayer mp) {
						mp.start();
						startFadeInVolume(); 
					}
				});

            mediaPlayer.prepareAsync();

        } catch (Exception e) { 
            e.printStackTrace(); 
        }

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            long[] pattern = {0, 1000, 1000};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
            else vibrator.vibrate(pattern, 0);
        }

        autoSilenceRunnable = new Runnable() {
            @Override
            public void run() {
                triggerAutoSnoozeAndFinish(title, task);
            }
        };
        autoSilenceHandler.postDelayed(autoSilenceRunnable, 60000);

        float density = getResources().getDisplayMetrics().density;

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.parseColor("#0A0A0A")); 
        mainLayout.setGravity(Gravity.CENTER); 
        mainLayout.setPadding(32, 32, 32, 32);

        TextView titleTv = new TextView(this);
        titleTv.setText(title != null ? title : "БУДИЛЬНИК");
        titleTv.setTextSize(26);
        titleTv.setTextColor(Color.parseColor("#E0E0E0"));
        titleTv.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        titleTv.setGravity(Gravity.CENTER);
        mainLayout.addView(titleTv);

        TextView taskTv = new TextView(this);
        taskTv.setText(task != null ? task : "Пора на работу");
        taskTv.setTextSize(16);
        taskTv.setTextColor(Color.parseColor("#757575")); 
        taskTv.setGravity(Gravity.CENTER);
        taskTv.setPadding(0, 8, 0, 60); 
        mainLayout.addView(taskTv);

        Button dismissButton = new Button(this);
        dismissButton.setText("ВЫКЛ\n(зажать)");
        dismissButton.setTextSize(15);
        dismissButton.setTextColor(Color.parseColor("#E65100")); 
        dismissButton.setTypeface(Typeface.DEFAULT_BOLD);

        GradientDrawable circleShape = new GradientDrawable();
        circleShape.setShape(GradientDrawable.OVAL);
        circleShape.setColor(Color.parseColor("#FFCC80")); 
        circleShape.setStroke(3, Color.parseColor("#FFE0B2")); 
        dismissButton.setBackground(circleShape);

        int sizePx = (int) (140 * density);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(sizePx, sizePx);
        btnParams.setMargins(0, 0, 0, (int) (60 * density)); 
        dismissButton.setLayoutParams(btnParams);

        dismissButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Toast.makeText(AlarmScreenActivity.this, "Удерживайте кнопку для выключения!", Toast.LENGTH_SHORT).show();
				}
			});

        dismissButton.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					stopAlarmMedia();
					onAlarmDismissed();
					finish();
					return true;
				}
			});
        mainLayout.addView(dismissButton);

        LinearLayout snoozeContainer = new LinearLayout(this);
        snoozeContainer.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (int) (85 * density)
        );
        snoozeContainer.setLayoutParams(containerParams);

        int[] snoozeMinutes = {5, 10, 15};

        String[] bgColors = {"#B3E5FC", "#C8E6C9", "#E1BEE7"}; 
        String[] textColors = {"#01579B", "#1B5E20", "#4A148C"}; 
        String[] strokeColors = {"#81D4FA", "#A5D6A7", "#CE93D8"}; 

        for (int i = 0; i < snoozeMinutes.length; i++) {
            final int minutes = snoozeMinutes[i];

            Button btn = new Button(this);
            btn.setText("+" + minutes + "\nмин");
            btn.setTextSize(16); 
            btn.setTextColor(Color.parseColor(textColors[i]));
            btn.setTypeface(Typeface.DEFAULT_BOLD);

            GradientDrawable btnShape = new GradientDrawable();
            btnShape.setShape(GradientDrawable.RECTANGLE);
            btnShape.setCornerRadius(24); 
            btnShape.setColor(Color.parseColor(bgColors[i])); 
            btnShape.setStroke(3, Color.parseColor(strokeColors[i])); 
            btn.setBackground(btnShape);

            LinearLayout.LayoutParams btnParams3 = new LinearLayout.LayoutParams(
                0, 
                LinearLayout.LayoutParams.MATCH_PARENT, 
                1.0f 
            );

            int marginSide = (int) (6 * density);
            btnParams3.setMargins(marginSide, 0, marginSide, 0);
            btn.setLayoutParams(btnParams3);

            btn.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						stopAlarmMedia();
						AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
						Intent snoozeIntent = new Intent(AlarmScreenActivity.this, AlarmReceiver.class);
						snoozeIntent.putExtra("SHIFT_DAY_NAME", title);
						snoozeIntent.putExtra("SHIFT_DAY_NUMBER", getIntent().getIntExtra("SHIFT_DAY_NUMBER", 1));
						snoozeIntent.putExtra("CUSTOM_TASK", task);
						snoozeIntent.putExtra("IS_PRE_ALARM", false); 

						int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M 
							? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE 
							: PendingIntent.FLAG_UPDATE_CURRENT;

						PendingIntent pi = PendingIntent.getBroadcast(AlarmScreenActivity.this, 555, snoozeIntent, flags);

						long triggerAtMillis = System.currentTimeMillis() + (minutes * 60 * 1000L);

						if (alarmManager != null) {
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
								alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
							} else {
								alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
							}
						}

						String widgetLabel = (task != null && !task.trim().isEmpty() && !task.equals("Пора вставать!")) ? task : title;
						updateWidgetForSnooze(triggerAtMillis, widgetLabel);

						finish();
					}
				});

            snoozeContainer.addView(btn);
        }

        mainLayout.addView(snoozeContainer);

        setContentView(mainLayout);
    }

    private void triggerAutoSnoozeAndFinish(String title, String task) {
        stopAlarmMedia();

        if (currentRepeatCount < maxRepeatCount) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent snoozeIntent = new Intent(AlarmScreenActivity.this, AlarmReceiver.class);
            snoozeIntent.putExtra("SHIFT_DAY_NAME", title);
            snoozeIntent.putExtra("SHIFT_DAY_NUMBER", getIntent().getIntExtra("SHIFT_DAY_NUMBER", 1));
            snoozeIntent.putExtra("CUSTOM_TASK", task);
            snoozeIntent.putExtra("IS_PRE_ALARM", false);
            snoozeIntent.putExtra("CURRENT_AUTO_REPEAT_COUNT", currentRepeatCount + 1);

            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT;
            PendingIntent pi = PendingIntent.getBroadcast(AlarmScreenActivity.this, 555, snoozeIntent, flags);

            long triggerAtMillis = System.currentTimeMillis() + (repeatIntervalMin * 60 * 1000L);
            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
                }
            }

            String widgetLabel = (task != null && !task.trim().isEmpty() && !task.equals("Пора вставать!")) ? task : title;
            updateWidgetForSnooze(triggerAtMillis, widgetLabel);
        } else {
            onAlarmDismissed();
        }

        finish();
    }

    private void updateWidgetForSnooze(long triggerAtMillis, String label) {
        SharedPreferences prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("next_alarm_time_ms", triggerAtMillis);
        if (label != null && !label.isEmpty()) {
            editor.putString("next_alarm_label", label);
        }
        editor.apply();

        ShiftWidgetProvider.updateAllWidgets(this);
    }

    private void onAlarmDismissed() {
        MainActivity.recalculateAndScheduleAlarms(getApplicationContext());
    }

    private void startFadeInVolume() {
        volumeRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && currentVolume < targetVolume) {
                    currentVolume += (targetVolume / 20f); 
                    if (currentVolume > targetVolume) currentVolume = targetVolume;
                    try {
                        mediaPlayer.setVolume(currentVolume, currentVolume);
                    } catch (Exception e) { e.printStackTrace(); }
                    volumeHandler.postDelayed(this, 250); 
                }
            }
        };
        volumeHandler.post(volumeRunnable);
    }

    private void stopAlarmMedia() {
        if (autoSilenceHandler != null && autoSilenceRunnable != null) {
            autoSilenceHandler.removeCallbacks(autoSilenceRunnable);
        }
        if (volumeHandler != null && volumeRunnable != null) {
            volumeHandler.removeCallbacks(volumeRunnable);
        }
        try { 
            if (mediaPlayer != null) { 
                if (mediaPlayer.isPlaying()) mediaPlayer.stop(); 
                mediaPlayer.release(); 
                mediaPlayer = null; 
            } 
        } catch (Exception e) { e.printStackTrace(); }
        if (vibrator != null) vibrator.cancel();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) notificationManager.cancel(999);
    }

    @Override public void onBackPressed() {}
    @Override protected void onDestroy() { super.onDestroy(); stopAlarmMedia(); }
}

