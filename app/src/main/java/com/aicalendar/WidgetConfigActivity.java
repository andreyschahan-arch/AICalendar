package com.aicalendar;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

public class WidgetConfigActivity extends Activity {

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private SeekBar seekBarAlpha;
    private TextView txtAlphaLabel;
    private RadioGroup rgColors;

    private int selectedColorRGB = Color.parseColor("#121212"); // По умолчанию тёмный
    private int currentAlpha = 204; // 80% прозрачности (из 255)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        setContentView(R.layout.activity_widget_config);

        txtAlphaLabel = findViewById(R.id.txt_alpha_label);
        seekBarAlpha = findViewById(R.id.seekbar_alpha);
        rgColors = findViewById(R.id.rg_colors);
        Button btnSave = findViewById(R.id.btn_save_config);

        // Ползунок прозрачности
        seekBarAlpha.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					currentAlpha = progress;
					int percent = (int) ((progress / 255.0f) * 100);
					txtAlphaLabel.setText("Прозрачность фона: " + percent + "%");
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {}

				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {}
			});

        // Выбор цвета
        rgColors.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(RadioGroup group, int checkedId) {
					if (checkedId == R.id.rb_graphite) {
						selectedColorRGB = Color.parseColor("#2A2A2A");
					} else if (checkedId == R.id.rb_blue) {
						selectedColorRGB = Color.parseColor("#1A233A");
					} else if (checkedId == R.id.rb_light) {
						selectedColorRGB = Color.parseColor("#E0E0E0");
					} else {
						selectedColorRGB = Color.parseColor("#121212"); // Тёмный
					}
				}
			});

        // Сохранение
        btnSave.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					saveWidgetSettings();
				}
			});
    }

    private void saveWidgetSettings() {
        // Вычисляем итоговый цвет ARGB с учетом альфа-канала
        int finalColor = Color.argb(
            currentAlpha, 
            Color.red(selectedColorRGB), 
            Color.green(selectedColorRGB), 
            Color.blue(selectedColorRGB)
        );

        SharedPreferences prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        prefs.edit().putInt("widget_bg_color_" + appWidgetId, finalColor).apply();

        // Обновляем виджет
        ShiftWidgetProvider.updateAllWidgets(this);

        // Передаем результат обратно системе (для штатного меню EMUI)
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }
}

