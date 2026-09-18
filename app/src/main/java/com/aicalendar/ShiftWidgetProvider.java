package com.aicalendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.text.TextUtils;
import android.widget.RemoteViews;

import java.util.Calendar;

public class ShiftWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_WIDGET_UPDATE = "com.aicalendar.ACTION_WIDGET_UPDATE";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Пересчитываем график и обновляем виджет
        MainActivity.recalculateAndScheduleAlarms(context);
        updateAllWidgets(context);
        scheduleNextMidnightUpdate(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();

        if (ACTION_WIDGET_UPDATE.equals(action) 
            || Intent.ACTION_DATE_CHANGED.equals(action) 
            || Intent.ACTION_TIME_CHANGED.equals(action)
            || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
            || Intent.ACTION_BOOT_COMPLETED.equals(action)) {

            // При наступлении новых суток или смене времени принудительно обновляем логику смены
            MainActivity.recalculateAndScheduleAlarms(context);
            updateAllWidgets(context);
            scheduleNextMidnightUpdate(context);
        }
    }

    public static void updateAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, ShiftWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int id : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, id);
        }
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);

        // --- ДОБАВЛЯЕМ: Настройка цвета и прозрачности фона виджета ---
        // По умолчанию ставим полупрозрачный темный фон (#CC121212), если настройка еще не создана
        int defaultBgColor = Color.parseColor("#CC121212");
        int widgetBgColor = prefs.getInt("widget_bg_color_" + appWidgetId, defaultBgColor);
        views.setInt(R.id.widget_main_layout, "setBackgroundColor", widgetBgColor);
        // -------------------------------------------------------------

        // 1. Отображение названия дня смены для "Сегодня"
        String todayShift = prefs.getString("today_shift_name", "");
        if (TextUtils.isEmpty(todayShift)) {
            todayShift = prefs.getString("day_0_name", "2 РАБОЧИЙ");
        }
        views.setTextViewText(R.id.widget_shift_name, todayShift);

        // 2. Отображение ближайшего будильника
        long nextAlarmMs = prefs.getLong("next_alarm_time_ms", 0);
        String alarmLabel = prefs.getString("next_alarm_label", "Будильник");

        if (TextUtils.isEmpty(alarmLabel)) {
            alarmLabel = "Будильник";
        }

        views.setTextViewText(R.id.widget_alarm_title, "⏰ " + alarmLabel);

        long now = System.currentTimeMillis();
        if (nextAlarmMs > now) {
            long diffMs = nextAlarmMs - now;
            long hours = diffMs / (1000 * 60 * 60);
            long minutes = (diffMs / (1000 * 60)) % 60;
            views.setTextViewText(R.id.widget_alarm_time, hours > 0 ? "через " + hours + "ч " + minutes + "м" : "через " + minutes + "м");
        } else {
            // Вместо сухой надписи "отключен" проверяем статус
            boolean isAlarmEnabled = prefs.getBoolean("alarm_enabled", true);
            views.setTextViewText(R.id.widget_alarm_time, isAlarmEnabled ? "включен" : "отключен");
        }

        // 3. Отрисовка 5 дней и их цветов из SharedPreferences
        String[] daysOfWeekStr = new String[]{"Вс", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб"};
        int[] numIds = new int[]{R.id.day_1_num, R.id.day_2_num, R.id.day_3_num, R.id.day_4_num, R.id.day_5_num};
        int[] subIds = new int[]{R.id.day_1_sub, R.id.day_2_sub, R.id.day_3_sub, R.id.day_4_sub, R.id.day_5_sub};
        int[] bgIds = new int[]{R.id.day_1_bg, R.id.day_2_bg, R.id.day_3_bg, R.id.day_4_bg, R.id.day_5_bg};

        for (int i = 0; i < 5; i++) {
            Calendar cardCal = Calendar.getInstance();
            cardCal.add(Calendar.DAY_OF_MONTH, i - 2);

            int dayNum = cardCal.get(Calendar.DAY_OF_MONTH);
            int dayOfWeekIdx = cardCal.get(Calendar.DAY_OF_WEEK) - 1;

            views.setTextViewText(numIds[i], String.valueOf(dayNum));
            if (i != 2) {
                views.setTextViewText(subIds[i], daysOfWeekStr[dayOfWeekIdx]);
            }

            // Берем реальные сохраненные цвета widget_color_0...4
            int color = prefs.getInt("widget_color_" + i, Color.TRANSPARENT);
            if (color != Color.TRANSPARENT) {
                views.setInt(bgIds[i], "setColorFilter", color);
            }
        }

        // 4. Открытие MainActivity по клику на весь виджет
        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, openAppIntent, flags);

        // Назначаем клик на заголовок и на главный макет (если есть R.id.widget_main_layout)
        views.setOnClickPendingIntent(R.id.widget_shift_name, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
		
		// Открытие MainActivity по клику на весь виджет
		views.setOnClickPendingIntent(R.id.widget_main_layout, pendingIntent);
		views.setOnClickPendingIntent(R.id.widget_shift_name, pendingIntent);
		
    }

    private static void scheduleNextMidnightUpdate(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Calendar midnight = Calendar.getInstance();
        midnight.add(Calendar.DAY_OF_MONTH, 1);
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 1);

        Intent intent = new Intent(context, ShiftWidgetProvider.class);
        intent.setAction(ACTION_WIDGET_UPDATE);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1001, intent, flags);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, midnight.getTimeInMillis(), pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, midnight.getTimeInMillis(), pendingIntent);
        }
    }
}

