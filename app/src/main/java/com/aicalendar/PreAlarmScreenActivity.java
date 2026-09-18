package com.aicalendar;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;

public class PreAlarmScreenActivity extends Activity {

    private MediaPlayer mediaPlayer;
    private Handler autoCloseHandler = new Handler();
    private Runnable autoCloseRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Принудительное включение экрана и показ поверх замка для всех версий Android/EMUI
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager != null) {
                keyguardManager.requestDismissKeyguard(this, null);
            }
        }

        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }

        String title = getIntent().getStringExtra("ALARM_TITLE");
        String task = getIntent().getStringExtra("ALARM_TASK");

        autoCloseRunnable = new Runnable() {
            @Override
            public void run() {
                closeScreen();
            }
        };
        autoCloseHandler.postDelayed(autoCloseRunnable, 30000);

        startAlarmSound();

        float density = getResources().getDisplayMetrics().density;
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.parseColor("#0A0A0A")); 
        mainLayout.setGravity(Gravity.CENTER); 
        mainLayout.setPadding(48, 48, 48, 48);

        TextView preAlarmTv = new TextView(this);
        preAlarmTv.setText("ПРЕ-СИГНАЛ\n(Скоро вставать)");
        preAlarmTv.setTextSize(14);
        preAlarmTv.setTextColor(Color.parseColor("#FF9800")); 
        preAlarmTv.setTypeface(Typeface.DEFAULT_BOLD);
        preAlarmTv.setGravity(Gravity.CENTER);
        preAlarmTv.setPadding(0, 0, 0, 40);
        mainLayout.addView(preAlarmTv);

        TextView titleTv = new TextView(this);
        titleTv.setText(title != null ? title : "ПРЕ-БУДИЛЬНИК");
        titleTv.setTextSize(24);
        titleTv.setTextColor(Color.parseColor("#E0E0E0"));
        titleTv.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        titleTv.setGravity(Gravity.CENTER);
        mainLayout.addView(titleTv);

        TextView taskTv = new TextView(this);
        taskTv.setText(task != null ? task : "Скоро вставать");
        taskTv.setTextSize(16);
        taskTv.setTextColor(Color.parseColor("#757575")); 
        taskTv.setGravity(Gravity.CENTER);
        taskTv.setPadding(0, 8, 0, 60); 
        mainLayout.addView(taskTv);

        Button dismissButton = new Button(this);
        dismissButton.setText("ЗАКРЫТЬ");
        dismissButton.setTextSize(14);
        dismissButton.setTextColor(Color.WHITE);
        dismissButton.setTypeface(Typeface.DEFAULT_BOLD);

        GradientDrawable circleShape = new GradientDrawable();
        circleShape.setShape(GradientDrawable.OVAL);
        circleShape.setColor(Color.parseColor("#222222"));
        circleShape.setStroke(2, Color.parseColor("#444444"));
        dismissButton.setBackground(circleShape);

        int sizePx = Math.round(140 * density);
        dismissButton.setLayoutParams(new LinearLayout.LayoutParams(sizePx, sizePx));

        dismissButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					closeScreen();
				}
			});
        mainLayout.addView(dismissButton);

        setContentView(mainLayout);
    }

    private void startAlarmSound() {
        try {
            android.content.SharedPreferences prefs = getSharedPreferences("alarm_settings", Context.MODE_PRIVATE);

            mediaPlayer = new MediaPlayer();

            // 2. Исправленный атрибут звука (USAGE_ALARM + CONTENT_TYPE_SONIFICATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
											   .setUsage(AudioAttributes.USAGE_ALARM)
											   .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
											   .build());
            } else {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            }

            int savedVolumeInt = prefs.getInt("pre_alarm_volume", 100);
            float volume = (float) savedVolumeInt / 100.0f;
            mediaPlayer.setVolume(volume, volume);

            boolean isSet = false;

            String uriString = getIntent().getStringExtra("PRE_ALARM_RINGTONE_URI");
            if (uriString == null || uriString.isEmpty()) {
                uriString = prefs.getString("pre_alarm_ringtone_uri", null);
            }

            if (uriString != null && !uriString.isEmpty()) {
                try {
                    mediaPlayer.setDataSource(this, Uri.parse(uriString));
                    isSet = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (!isSet) {
                File customPreAlarmFile = new File(getFilesDir(), "custom_pre_alarm.mp3");
                if (customPreAlarmFile.exists() && customPreAlarmFile.length() > 0) {
                    try {
                        mediaPlayer.setDataSource(customPreAlarmFile.getAbsolutePath());
                        isSet = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            if (!isSet) {
                Uri defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (defaultUri == null) {
                    defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                }
                if (defaultUri != null) {
                    mediaPlayer.setDataSource(this, defaultUri);
                }
            }

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
					@Override
					public void onCompletion(MediaPlayer mp) {
						closeScreen();
					}
				});

            mediaPlayer.prepare();
            mediaPlayer.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closeScreen() {
        if (autoCloseHandler != null && autoCloseRunnable != null) {
            autoCloseHandler.removeCallbacks(autoCloseRunnable);
        }
        try { 
            if (mediaPlayer != null) { 
                if (mediaPlayer.isPlaying()) mediaPlayer.stop(); 
                mediaPlayer.release(); 
                mediaPlayer = null; 
            } 
        } catch (Exception e) { 
            e.printStackTrace(); 
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) notificationManager.cancel(998);

        finish();
    }

    @Override public void onBackPressed() {}
    @Override protected void onDestroy() { super.onDestroy(); closeScreen(); }
}

