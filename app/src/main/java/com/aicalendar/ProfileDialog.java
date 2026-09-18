package com.aicalendar;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import com.aicalendar.R;

public class ProfileDialog {

    private static boolean isCreatingNew = false;
    private static int currentProfileIndex = -1;

    public static void show(final MainActivity activity, final int profileIndex) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final SharedPreferences prefs = activity.getSharedPreferences("AiCalendarPrefs", Context.MODE_PRIVATE);

        currentProfileIndex = profileIndex;
        isCreatingNew = (currentProfileIndex == -1);

        // Главный контейнер диалога
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 30, 40, 30);
        layout.setBackgroundColor(0xFF121212);

        LinearLayout.LayoutParams elementParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        elementParams.setMargins(0, 0, 0, 24);

        // --- КАСТОМНЫЙ ЗАГОЛОВОК ---
        LinearLayout headerLayout = new LinearLayout(activity);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(Gravity.CENTER); 
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 130 
        );
        headerParams.setMargins(0, 10, 0, 20);
        headerLayout.setLayoutParams(headerParams);

        final TextView tvCustomTitle = new TextView(activity);
        tvCustomTitle.setText(isCreatingNew ? "Создание типа дня" : "Редактирование дня");
        tvCustomTitle.setTextColor(Color.WHITE);
        tvCustomTitle.setTextSize(22); 
        tvCustomTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvCustomTitle.setGravity(Gravity.CENTER); 

        headerLayout.addView(tvCustomTitle);
        layout.addView(headerLayout); 

        // --- ПОДПИСЬ ДЛЯ НАЗВАНИЯ ---
        TextView tvNameLabel = new TextView(activity);
        tvNameLabel.setText("Название дня:");
        tvNameLabel.setTextColor(Color.GRAY);
        tvNameLabel.setPadding(0, 0, 0, 8);
        layout.addView(tvNameLabel);

        final EditText etName = new EditText(activity);
        etName.setHint("напр. Рабочая смена");
        etName.setHintTextColor(Color.GRAY);
        etName.setTextColor(Color.WHITE);
        etName.setPadding(30, 25, 30, 25);
        etName.setBackgroundColor(0xFF222222);
        etName.setLayoutParams(elementParams);
        if (!isCreatingNew) etName.setText(activity.getProfileName(currentProfileIndex));
        layout.addView(etName);

        // --- ПОДПИСЬ ДЛЯ ПЕРИОДА ---
        TextView tvPeriodLabel = new TextView(activity);
        tvPeriodLabel.setText("Повторять каждые (в днях):");
        tvPeriodLabel.setTextColor(Color.GRAY);
        tvPeriodLabel.setPadding(0, 10, 0, 8);
        layout.addView(tvPeriodLabel);

        final EditText etPeriod = new EditText(activity);
        etPeriod.setHint("напр. 3");
        etPeriod.setHintTextColor(Color.GRAY);
        etPeriod.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etPeriod.setTextColor(Color.WHITE);
        etPeriod.setPadding(30, 25, 30, 25);
        etPeriod.setBackgroundColor(0xFF222222);
        etPeriod.setLayoutParams(elementParams);
        if (!isCreatingNew) etPeriod.setText(String.valueOf(activity.getProfilePeriod(currentProfileIndex)));
        layout.addView(etPeriod);

		// Выбор цвета
        TextView tvColorLabel = new TextView(activity);
        tvColorLabel.setText("Выберите цвет дня:");
        tvColorLabel.setTextColor(Color.GRAY);
        tvColorLabel.setPadding(0, 10, 0, 12);
        layout.addView(tvColorLabel);

        // Горизонтальный скролл на случай, если палитра не влезет по ширине
        android.widget.HorizontalScrollView colorScrollView = new android.widget.HorizontalScrollView(activity);
        colorScrollView.setHorizontalScrollBarEnabled(false);

        LinearLayout colorLayout = new LinearLayout(activity);
        colorLayout.setOrientation(LinearLayout.HORIZONTAL);
        colorLayout.setGravity(Gravity.CENTER_VERTICAL);
        colorLayout.setLayoutParams(elementParams);

        // --- ПАЛИТРА ЯРКИХ СОЧНЫХ ЦВЕТОВ ---
        final int[] colors = {
            0xFFFF1744, // Ярко-красный (Neon Red)
            0xFFFF5252, // Коралловый
            0xFFFF4081, // Сочный розовый (Hot Pink)
            0xFFE040FB, // Неоновый фиолетовый
            0xFF7C4DFF, // Фиолетово-синий
            0xFF536DFE, // Индиго / Электрик
            0xFF00E5FF, // Неоновый Голубой / Cyan
            0xFF18FFFF, // Яркая Бирюза
            0xFF00E676, // Изумрудно-зеленый
            0xFF76FF03, // Салатовый / Неоновый зеленый
            0xFFFFEA00, // Ярко-желтый
            0xFFFF9100, // Оранжевый
            Color.TRANSPARENT // Без цвета (прозрачный)
        };

        final int[] selectedColor = {isCreatingNew ? colors[0] : activity.getProfileColor(currentProfileIndex)};
        final ArrayList<TextView> blocks = new ArrayList<TextView>();

        for (int i = 0; i < colors.length; i++) {
            final int currColor = colors[i];
            final TextView sq = new TextView(activity);

            // Размер квадратика 70x70 для аккуратности
            LinearLayout.LayoutParams sqParams = new LinearLayout.LayoutParams(70, 70);
            sqParams.setMargins(6, 0, 6, 0);
            sq.setLayoutParams(sqParams);

            if (currColor == Color.TRANSPARENT) {
                android.graphics.drawable.GradientDrawable emptyDrawable = new android.graphics.drawable.GradientDrawable();
                emptyDrawable.setColor(0xFF222222); 
                emptyDrawable.setStroke(3, Color.GRAY); 
                emptyDrawable.setCornerRadius(8f);
                sq.setBackground(emptyDrawable);
            } else {
                // Красивые закругленные квадраты с сочным цветом
                android.graphics.drawable.GradientDrawable colorTile = new android.graphics.drawable.GradientDrawable();
                colorTile.setColor(currColor);
                colorTile.setCornerRadius(8f);
                sq.setBackground(colorTile);
            }

            if (currColor == selectedColor[0]) sq.setText("✓");
            sq.setGravity(Gravity.CENTER);
            sq.setTextColor(Color.WHITE);
            sq.setTextSize(16);

            sq.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						selectedColor[0] = currColor;
						for (TextView b : blocks) b.setText("");
						sq.setText("✓");
					}
				});
            blocks.add(sq);
            colorLayout.addView(sq);
        }

        colorScrollView.addView(colorLayout);
        layout.addView(colorScrollView);
		

        // Будильники
        final LinearLayout alarmContainer = new LinearLayout(activity);
        alarmContainer.setOrientation(LinearLayout.VERTICAL);
        final ArrayList<EditText> times = new ArrayList<EditText>();
        final ArrayList<EditText> tasks = new ArrayList<EditText>();

        if (!isCreatingNew) {
            activity.fillAlarmRows(currentProfileIndex, alarmContainer, times, tasks);
        }

        TextView btnAdd = new TextView(activity);
        btnAdd.setText("+ ДОБАВИТЬ НАПОМИНАНИЕ");
        btnAddAlarmStyle(btnAdd);
        btnAdd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.addAlarmRowExternal(alarmContainer, times, tasks, "06:00", "");
                }
            });
        layout.addView(btnAdd);
        layout.addView(alarmContainer);

        // --- БЛОК КНОПОК УПРАВЛЕНИЯ ВНИЗУ ---
        LinearLayout buttonsLayout = new LinearLayout(activity);
        buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonsLayout.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f
        );
        btnParams.setMargins(6, 60, 6, 10); 

        final AlertDialog dialog = builder.create();

        // Кнопка ОТМЕНА
        Button btnCancel = new Button(activity);
        btnCancel.setText("ОТМЕНА");
        btnCancel.setTextSize(11);
        btnCancel.setSingleLine(true);
        btnCancel.setTextColor(Color.LTGRAY);
        btnCancel.setBackgroundColor(0xFF222222);
        btnCancel.setPadding(0, 15, 0, 15);
        btnCancel.setLayoutParams(btnParams);
        btnCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        buttonsLayout.addView(btnCancel);

        // Кнопка УДАЛИТЬ
        final Button btnDelete = new Button(activity);
        btnDelete.setText("УДАЛИТЬ");
        btnDelete.setTextSize(11);
        btnDelete.setSingleLine(true);
        btnDelete.setTextColor(0xFFCD5C5C);
        btnDelete.setBackgroundColor(0xFF222222);
        btnDelete.setPadding(0, 15, 0, 15);
        btnDelete.setLayoutParams(btnParams);
        btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.deleteProfileExternal(currentProfileIndex);
                    dialog.dismiss();
                }
            });

        // Кнопка КЛОНИРОВАТЬ
        final Button btnDuplicate = new Button(activity);
        btnDuplicate.setText("КЛОНИРОВАТЬ");
        btnDuplicate.setTextSize(11);
        btnDuplicate.setSingleLine(true);
        btnDuplicate.setTextColor(0xFFFFA500); 
        btnDuplicate.setBackgroundColor(0xFF222222);
        btnDuplicate.setPadding(0, 15, 0, 15);
        btnDuplicate.setLayoutParams(btnParams);

        if (isCreatingNew) {
            btnDelete.setVisibility(View.GONE);
            btnDuplicate.setVisibility(View.GONE);
        }

        btnDuplicate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    isCreatingNew = true;
                    currentProfileIndex = -1; // Сбрасываем индекс, чтобы сохранить как новый

                    String currentName = etName.getText().toString().trim();
                    etName.setText(currentName + " (копия)");

                    tvCustomTitle.setText("Создание типа дня");

                    btnDelete.setVisibility(View.GONE);
                    btnDuplicate.setVisibility(View.GONE);

                    Toast.makeText(activity, "Режим клонирования. Отредактируйте и нажмите СОХРАНИТЬ", Toast.LENGTH_LONG).show();
                }
            });

        buttonsLayout.addView(btnDelete);
        buttonsLayout.addView(btnDuplicate);

        // Кнопка СОХРАНИТЬ
        Button btnSave = new Button(activity);
        btnSave.setText("СОХРАНИТЬ");
        btnSave.setTextSize(11);
        btnSave.setSingleLine(true);
        btnSave.setTextColor(0xFF4682B4);
        btnSave.setBackgroundColor(0xFF222222);
        btnSave.setPadding(0, 15, 0, 15);
        btnSave.setLayoutParams(btnParams);
        btnSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        String name = etName.getText().toString().trim();
                        String periodStr = etPeriod.getText().toString().trim();
                        if (name.isEmpty() || periodStr.isEmpty()) {
                            Toast.makeText(activity, "Заполните поля!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        int period = Integer.parseInt(periodStr);
                        if (period <= 0) return;

                        activity.saveProfileExternal(currentProfileIndex, name, period, selectedColor[0], times, tasks);
                        Toast.makeText(activity, "Шаблон дня сохранен!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } catch (Exception e) {
                        Toast.makeText(activity, "Ошибка данных!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        buttonsLayout.addView(btnSave);

        layout.addView(buttonsLayout);

        // Оборачиваем всё в скролл
        ScrollView sv = new ScrollView(activity);
        sv.addView(layout);
        dialog.setView(sv);

        // Показываем диалог
        dialog.show();

        // --- ХАК ДЛЯ ЦЕНТРИРОВАНИЯ ОКНА НА ЭКРАНЕ ---
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.gravity = Gravity.CENTER; 
            window.setAttributes(layoutParams);
        }
    }

    private static void btnAddAlarmStyle(TextView btn) {
        btn.setTextColor(0xFF4682B4);
        btn.setTextSize(14);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(0, 20, 0, 20);
        btn.setBackgroundColor(0xFF1E1E1E);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 10, 0, 16);
        btn.setLayoutParams(p);
    }
}

