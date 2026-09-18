package com.aicalendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Проверяем, что телефон действительно завершил загрузку
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Создаем временный объект MainActivity, чтобы вызвать пересчет будильников
            MainActivity activity = new MainActivity();

            // Чтобы запустить пересчет в фоне, передаем контекст
            // Но так как метод recalculateAndScheduleAlarms у нас в MainActivity использует prefs и логику,
            // мы просто безопасно запустим MainActivity в фоновом режиме на секунду для автонастройки
            Intent serviceIntent = new Intent(context, MainActivity.class);
            serviceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Но чтобы не пугать пользователя внезапным открытием приложения на весь экран, 
            // мы заставим MainActivity тихо инициализировать будильники.
            // Для этого мы передаем специальный флаг "AUTO_BOOT_RECALC"
            serviceIntent.putExtra("AUTO_BOOT_RECALC", true);
            context.startActivity(serviceIntent);
        }
    }
}
