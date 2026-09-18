package com.aicalendar;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();

        // 0. ОБРАБОТКА КНОПКИ "ОТЛОЖИТЬ" (SNOOZE)
        if ("ACTION_SNOOZE".equals(action)) {
            long snoozeTimeMs = intent.getLongExtra("SNOOZE_TIME_MS", System.currentTimeMillis() + 10 * 60 * 1000L);
            String title = intent.getStringExtra("ALARM_TITLE");
            String task = intent.getStringExtra("ALARM_TASK");
            int reqId = intent.getIntExtra("ALARM_REQ_ID", (int) System.currentTimeMillis());

            Intent snoozeIntent = new Intent(context, AlarmReceiver.class);
            snoozeIntent.putExtra("SHIFT_DAY_NAME", title);
            snoozeIntent.putExtra("CUSTOM_TASK", task);
            snoozeIntent.putExtra("IS_PRE_ALARM", false);

            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT;

            PendingIntent pi = PendingIntent.getBroadcast(context, reqId + 99, snoozeIntent, flags);
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            if (am!=null) {
                if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {
                    am.setAlarmClock(new AlarmManager.AlarmClockInfo(snoozeTimeMs, pi), pi);
                } else if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTimeMs, pi);
                } else {
                    am.setExact(AlarmManager.RTC_WAKEUP, snoozeTimeMs, pi);
                }
            }

            String widgetLabel = (task != null && !task.trim().isEmpty() && !task.equals("Пора вставать!")) ? task : title;

            SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
            prefs.edit()
                .putLong("next_alarm_time_ms", snoozeTimeMs)
                .putString("next_alarm_label", widgetLabel)
                .apply();

            ShiftWidgetProvider.updateAllWidgets(context);
            return;
        }

        // 1. МГНОВЕННОЕ ОБНОВЛЕНИЕ ВИДЖЕТА ПРИ СРАБАТЫВАНИИ
        ShiftWidgetProvider.updateAllWidgets(context);

        boolean isPreAlarm = intent.getBooleanExtra("IS_PRE_ALARM", false);
        String dayName = intent.getStringExtra("SHIFT_DAY_NAME");
        int shiftDayNumber = intent.getIntExtra("SHIFT_DAY_NUMBER", 1);
        String customTask = intent.getStringExtra("CUSTOM_TASK");

        if (dayName == null || dayName.trim().isEmpty()) {
            dayName = "День смены №" + shiftDayNumber;
        }

        if (customTask == null || customTask.isEmpty()) {
            customTask = isPreAlarm ? "Пре-сигнал. Скоро вставать." : "Пора вставать!";
        }

        // 2. ПОДГОТОВКА ИНТЕНТА И ПРЯМОЙ ЗАПУСК ЭКРАНА СМАРТФОНА
        Intent alarmIntent = new Intent(context, isPreAlarm ? PreAlarmScreenActivity.class : AlarmScreenActivity.class);
        alarmIntent.putExtra("ALARM_TITLE", dayName);
        alarmIntent.putExtra("ALARM_TASK", customTask);
        alarmIntent.putExtra("SHIFT_DAY_NUMBER", shiftDayNumber);

        if (intent.hasExtra("RINGTONE_URI")) {
            alarmIntent.putExtra("RINGTONE_URI", intent.getStringExtra("RINGTONE_URI"));
        }
        if (intent.hasExtra("PRE_ALARM_RINGTONE_URI")) {
            alarmIntent.putExtra("PRE_ALARM_RINGTONE_URI", intent.getStringExtra("PRE_ALARM_RINGTONE_URI"));
        }

        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK 
                             | Intent.FLAG_ACTIVITY_CLEAR_TOP 
                             | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        try {
            context.startActivity(alarmIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 3. ФОНОВОЕ УВЕДОМЛЕНИЕ ДЛЯ ЧАСОВ И ТЕЛЕФОНА
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = isPreAlarm ? "pre_alarm_watch_channel_v3" : "main_alarm_watch_channel_v3";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                channelId, 
                isPreAlarm ? "Пре-будильник (Часы)" : "Основной будильник (Часы)", 
                NotificationManager.IMPORTANCE_HIGH
            );

            channel.setSound(null, null); 
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500, 200, 500});

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT;

        PendingIntent pendingIntent = PendingIntent.getActivity(context, isPreAlarm ? 777 : 666, alarmIntent, flags);

        long[] vibratePattern = new long[]{0, 500, 200, 500, 200, 500};

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(dayName)
            .setContentText(customTask)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setSound(null)
            .setVibrate(vibratePattern)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true);

        if (notificationManager != null) {
            notificationManager.notify(isPreAlarm ? 998 : 999, builder.build());
        }

        // 4. ПОЛНЫЙ ПЕРЕРАСЧЕТ И ОБНОВЛЕНИЕ ВИДЖЕТА
        MainActivity.recalculateAndScheduleAlarms(context);
    }
}

