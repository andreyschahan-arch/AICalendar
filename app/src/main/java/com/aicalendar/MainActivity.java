package com.aicalendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import java.util.ArrayList;
import java.util.Calendar;
import android.widget.Button;
import com.aicalendar.R;

public class MainActivity extends Activity {

    private ViewPager viewPager;
    private MonthPagerAdapter pagerAdapter;
    private TextView monthTitle;
    private SharedPreferences prefs;

    private android.net.Uri tempRingtoneUri;
    private String tempRingtoneTitle;
    private android.widget.TextView tvRingtoneNameRef; 

    private android.net.Uri tempPreRingtoneUri;
    private String tempPreRingtoneTitle;
    private android.widget.TextView tvPreRingtoneNameRef;

    public static class DayProfile {
        int id;
        String name;
        int period;
        long startTimestamp;
        int color;
        ArrayList<String> alarmTimes = new ArrayList<String>();
        ArrayList<String> alarmTasks = new ArrayList<String>();
    }

    private static ArrayList<DayProfile> activeProfiles = new ArrayList<DayProfile>();
    private final int BASE_POSITION = 500; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("AiCalendarPrefs", MODE_PRIVATE);
        loadProfilesFromPrefs(); 

        if (getIntent() != null && getIntent().getBooleanExtra("AUTO_BOOT_RECALC", false)) {
            recalculateAndScheduleAlarms(this);
            finish(); 
            return;
        }

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(0xFF121212);

        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(android.view.Gravity.CENTER);
        headerLayout.setPadding(16, 25, 16, 15);

        TextView btnPrev = new TextView(this);
        btnPrev.setText(" ❮ ");
        btnPrev.setTextColor(Color.GRAY);
        btnPrev.setTextSize(22);
        btnPrev.setPadding(30, 10, 30, 10);
        btnPrev.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					viewPager.setCurrentItem(viewPager.getCurrentItem() - 1, true); 
				}
			});

        monthTitle = new TextView(this);
        monthTitle.setTextColor(Color.WHITE);
        monthTitle.setTextSize(20);
        monthTitle.setGravity(android.view.Gravity.CENTER);

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
        );
        monthTitle.setLayoutParams(titleParams);

        TextView btnNext = new TextView(this);
        btnNext.setText(" ❯ ");
        btnNext.setTextColor(Color.GRAY);
        btnNext.setTextSize(22);
        btnNext.setPadding(30, 10, 30, 10);
        btnNext.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, true); 
				}
			});

        headerLayout.addView(btnPrev);
        headerLayout.addView(monthTitle);
        headerLayout.addView(btnNext);
        rootLayout.addView(headerLayout);

        GridView weekDaysGrid = new GridView(this);
        weekDaysGrid.setNumColumns(7);
        weekDaysGrid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        weekDaysGrid.setAdapter(new BaseAdapter() {
				private final String[] weekDays = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
				@Override public int getCount() { return 7; }
				@Override public Object getItem(int p) { return weekDays[p]; }
				@Override public long getItemId(int p) { return p; }
				@Override public View getView(int p, View cv, ViewGroup parent) {
					TextView tv = new TextView(MainActivity.this);
					tv.setText(weekDays[p]);
					tv.setGravity(android.view.Gravity.CENTER);
					tv.setTextColor(p >= 5 ? 0xFF994444 : Color.GRAY); 
					tv.setTextSize(14);
					tv.setPadding(0, 10, 0, 10);
					return tv;
				}
			});
        rootLayout.addView(weekDaysGrid);

        viewPager = new ViewPager(this);

        LinearLayout.LayoutParams pagerParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 
            0
        );
        pagerParams.weight = 1f;

        rootLayout.addView(viewPager, pagerParams);
        setContentView(rootLayout);

        pagerAdapter = new MonthPagerAdapter();
        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(BASE_POSITION); 

        updateTitle(BASE_POSITION);

        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
				@Override
				public void onPageSelected(int position) {
					updateTitle(position);
				}
			});

        recalculateAndScheduleAlarms(this); 

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
            }
        }
    }

    private void updateTitle(int position) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, position - BASE_POSITION);
        String[] months = {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};
        monthTitle.setText(months[cal.get(Calendar.MONTH)] + " " + cal.get(Calendar.YEAR));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_profiles) {
            showProfilesManagerDialog(); 
            return true;
        } else if (id == R.id.action_settings) {
            showAlarmSettingsDialog(); 
            return true;
        } else if (id == R.id.action_vacation) {
            showVacationDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showAlarmSettingsDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this, android.app.AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        builder.setTitle("Настройки будильника");

        android.view.LayoutInflater inflater = getLayoutInflater();
        android.view.View dialogView = inflater.inflate(R.layout.dialog_alarm_settings, null);
        builder.setView(dialogView);

        final android.widget.SeekBar volumeSeekBar = dialogView.findViewById(R.id.volume_seekbar);
        final android.widget.EditText repeatCountEdit = dialogView.findViewById(R.id.repeat_count_edit);
        final android.widget.EditText repeatIntervalEdit = dialogView.findViewById(R.id.repeat_interval_edit);
        final android.widget.Button btnChooseRingtone = dialogView.findViewById(R.id.btn_choose_ringtone);
        final android.widget.TextView tvRingtoneName = dialogView.findViewById(R.id.tv_ringtone_name);

        final android.widget.EditText preAlarmMinuteEdit = dialogView.findViewById(R.id.pre_alarm_minute_edit);
        final android.widget.Button btnChoosePreRingtone = dialogView.findViewById(R.id.btn_choose_pre_ringtone);
        final android.widget.TextView tvPreRingtoneName = dialogView.findViewById(R.id.tv_pre_ringtone_name);

        tvRingtoneNameRef = tvRingtoneName;
        tvPreRingtoneNameRef = tvPreRingtoneName;

        final android.content.SharedPreferences settingsPrefs = getSharedPreferences("alarm_settings", MODE_PRIVATE);
        int savedVolume = settingsPrefs.getInt("alarm_volume", 70);
        int savedCount = settingsPrefs.getInt("alarm_repeat_count", 3);
        int savedInterval = settingsPrefs.getInt("alarm_repeat_interval", 5);
        int savedPreMinute = settingsPrefs.getInt("alarm_pre_minute", 0);

        String savedRingtoneTitle = settingsPrefs.getString("alarm_ringtone_title", "По умолчанию");
        String savedRingtoneUriStr = settingsPrefs.getString("ringtone_uri", null);

        String savedPreRingtoneTitle = settingsPrefs.getString("pre_alarm_ringtone_title", "По умолчанию");
        String savedPreRingtoneUriStr = settingsPrefs.getString("pre_alarm_ringtone_uri", null);

        tempRingtoneTitle = savedRingtoneTitle;
        tempRingtoneUri = savedRingtoneUriStr != null ? android.net.Uri.parse(savedRingtoneUriStr) : null;

        tempPreRingtoneTitle = savedPreRingtoneTitle;
        tempPreRingtoneUri = savedPreRingtoneUriStr != null ? android.net.Uri.parse(savedPreRingtoneUriStr) : null;

        volumeSeekBar.setProgress(savedVolume);
        repeatCountEdit.setText(String.valueOf(savedCount));
        repeatIntervalEdit.setText(String.valueOf(savedInterval));
        if (preAlarmMinuteEdit != null) {
            preAlarmMinuteEdit.setText(String.valueOf(savedPreMinute));
        }
        tvRingtoneName.setText(tempRingtoneTitle);
        if (tvPreRingtoneName != null) {
            tvPreRingtoneName.setText(tempPreRingtoneTitle);
        }

        btnChooseRingtone.setOnClickListener(new android.view.View.OnClickListener() {
				@Override
				public void onClick(android.view.View v) {
					android.content.Intent intent = new android.content.Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER);
					intent.putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_ALARM);
					intent.putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TITLE, "Основная мелодия");
					if (tempRingtoneUri != null) {
						intent.putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, tempRingtoneUri);
					}
					startActivityForResult(intent, 999); 
				}
			});

        if (btnChoosePreRingtone != null) {
            btnChoosePreRingtone.setOnClickListener(new android.view.View.OnClickListener() {
					@Override
					public void onClick(android.view.View v) {
						android.content.Intent intent = new android.content.Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER);
						intent.putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_ALARM);
						intent.putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TITLE, "Мелодия пре-будильника");
						if (tempPreRingtoneUri != null) {
							intent.putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, tempPreRingtoneUri);
						}
						startActivityForResult(intent, 888); 
					}
				});
        }

        builder.setPositiveButton("Сохранить", new android.content.DialogInterface.OnClickListener() {
				@Override
				public void onClick(android.content.DialogInterface dialog, int which) {
					int newVolume = volumeSeekBar.getProgress();
					int newCount = 3;
					try { newCount = Integer.parseInt(repeatCountEdit.getText().toString()); } catch (NumberFormatException e) {}
					int newInterval = 5;
					try { newInterval = Integer.parseInt(repeatIntervalEdit.getText().toString()); } catch (NumberFormatException e) {}
					int newPreMinute = 0;
					if (preAlarmMinuteEdit != null) {
						try { newPreMinute = Integer.parseInt(preAlarmMinuteEdit.getText().toString()); } catch (NumberFormatException e) {}
					}

					android.content.SharedPreferences.Editor editor = settingsPrefs.edit();
					editor.putInt("alarm_volume", newVolume);
					editor.putInt("alarm_repeat_count", newCount);
					editor.putInt("alarm_repeat_interval", newInterval);
					editor.putInt("alarm_pre_minute", newPreMinute);

					editor.putString("alarm_ringtone_title", tempRingtoneTitle);
					if (tempRingtoneUri != null) editor.putString("ringtone_uri", tempRingtoneUri.toString());
					else editor.remove("ringtone_uri");

					editor.putString("pre_alarm_ringtone_title", tempPreRingtoneTitle);
					if (tempPreRingtoneUri != null) editor.putString("pre_alarm_ringtone_uri", tempPreRingtoneUri.toString());
					else editor.remove("pre_alarm_ringtone_uri");

					editor.apply();
					recalculateAndScheduleAlarms(MainActivity.this);
					android.widget.Toast.makeText(MainActivity.this, "Настройки сохранены!", android.widget.Toast.LENGTH_SHORT).show();
				}
			});

        builder.setNegativeButton("Отмена", null);
        builder.create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            android.net.Uri selectedAudioUri = data.getParcelableExtra(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (selectedAudioUri == null) {
                selectedAudioUri = data.getData();
            }

            if (selectedAudioUri != null) {
                if (requestCode == 999) { 
                    tempRingtoneUri = selectedAudioUri;

                    android.media.Ringtone ringtone = android.media.RingtoneManager.getRingtone(this, selectedAudioUri);
                    if (ringtone != null) {
                        tempRingtoneTitle = ringtone.getTitle(this);
                    } else {
                        tempRingtoneTitle = "Выбрана мелодия";
                    }

                    if (tvRingtoneNameRef != null) {
                        tvRingtoneNameRef.setText(tempRingtoneTitle);
                    }

                } else if (requestCode == 888) { 
                    tempPreRingtoneUri = selectedAudioUri;

                    android.media.Ringtone ringtone = android.media.RingtoneManager.getRingtone(this, selectedAudioUri);
                    if (ringtone != null) {
                        tempPreRingtoneTitle = ringtone.getTitle(this);
                    } else {
                        tempPreRingtoneTitle = "Выбрана мелодия";
                    }

                    try {
                        java.io.InputStream inputStream = getContentResolver().openInputStream(selectedAudioUri);
                        if (inputStream != null) {
                            java.io.File preAlarmFile = new java.io.File(getFilesDir(), "custom_pre_alarm.mp3");
                            if (preAlarmFile.exists()) {
                                preAlarmFile.delete();
                            }

                            java.io.FileOutputStream outputStream = new java.io.FileOutputStream(preAlarmFile);
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }
                            inputStream.close();
                            outputStream.close();

                            tempPreRingtoneUri = android.net.Uri.fromFile(preAlarmFile);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (tvPreRingtoneNameRef != null) {
                        tvPreRingtoneNameRef.setText(tempPreRingtoneTitle);
                    }
                }
            }
        }
    }

    private void loadProfilesFromPrefs() {
        activeProfiles.clear();
        int count = prefs.getInt("profiles_count", 0);
        for (int i = 0; i < count; i++) {
            DayProfile p = new DayProfile();
            p.id = i;
            p.name = prefs.getString("profile_name_" + i, "День " + (i + 1));
            p.period = prefs.getInt("profile_period_" + i, 1);
            p.startTimestamp = prefs.getLong("profile_start_" + i, 0);
            p.color = prefs.getInt("profile_color_" + i, 0xFFCD5C5C);

            int alarmCount = prefs.getInt("profile_alarms_count_" + i, 0);
            for (int a = 0; a < alarmCount; a++) {
                p.alarmTimes.add(prefs.getString("p_" + i + "_alarm_time_" + a, "06:00"));
                p.alarmTasks.add(prefs.getString("p_" + i + "_alarm_task_" + a, ""));
            }
            activeProfiles.add(p);
        }
    }

    private void showProfilesManagerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ваши профили дней");

        ArrayList<String> names = new ArrayList<String>();
        names.add("[ + Создать новый тип дня ]");
        for (int i = 0; i < activeProfiles.size(); i++) {
            names.add(activeProfiles.get(i).name + " (Каждые " + activeProfiles.get(i).period + " дн.)");
        }

        String[] items = names.toArray(new String[0]);
        builder.setItems(items, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (which == 0) {
						showEditProfileDialog(-1);
					} else {
						showEditProfileDialog(which - 1);
					}
				}
			});
        builder.show();
    }

    private void showEditProfileDialog(final int profileIndex) {
        ProfileDialog.show(this, profileIndex);
    }

    public String getProfileName(int idx) { 
        if (idx >= 0 && idx < activeProfiles.size()) return activeProfiles.get(idx).name; 
        return "";
    }
    public int getProfilePeriod(int idx) { 
        if (idx >= 0 && idx < activeProfiles.size()) return activeProfiles.get(idx).period; 
        return 1;
    }
    public int getProfileColor(int idx) { 
        if (idx >= 0 && idx < activeProfiles.size()) return activeProfiles.get(idx).color; 
        return Color.TRANSPARENT;
    }

    public void fillAlarmRows(int idx, LinearLayout container, ArrayList<EditText> times, ArrayList<EditText> tasks) {
        if (idx < 0 || idx >= activeProfiles.size()) return;
        DayProfile current = activeProfiles.get(idx);
        for (int i = 0; i < current.alarmTimes.size(); i++) {
            addAlarmRowExternal(container, times, tasks, current.alarmTimes.get(i), current.alarmTasks.get(i));
        }
    }

    public void addAlarmRowExternal(final LinearLayout container, final ArrayList<EditText> times, final ArrayList<EditText> tasks, String defTime, String defTask) {
        final LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 16, 0, 16);
        row.setLayoutParams(rowParams);

        final EditText etTime = new EditText(this);
        etTime.setHint("06:00"); etTime.setText(defTime); etTime.setTextColor(Color.WHITE);
        etTime.setFocusable(false); etTime.setClickable(true);
        etTime.setPadding(25, 35, 25, 35); 
        etTime.setBackgroundColor(0xFF222222);

        LinearLayout.LayoutParams pTime = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        pTime.setMargins(0, 0, 16, 0); 
        etTime.setLayoutParams(pTime);

        etTime.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					int h = 6, m = 0;
					try {
						String[] p = etTime.getText().toString().split(":");
						h = Integer.parseInt(p[0].trim()); m = Integer.parseInt(p[1].trim());
					} catch (Exception e) {}
					new android.app.TimePickerDialog(MainActivity.this, AlertDialog.THEME_HOLO_DARK,
						new android.app.TimePickerDialog.OnTimeSetListener() {
							@Override
							public void onTimeSet(android.widget.TimePicker view, int hour, int minute) {
								String formattedTime = (hour < 10 ? "0" + hour : hour) + ":" + (minute < 10 ? "0" + minute : minute);
								etTime.setText(formattedTime);
								sortAlarmRowsInContainer(container, times, tasks);
							}
						}, h, m, true).show();
				}
			});
        times.add(etTime); 
        row.addView(etTime);

        final EditText etTask = new EditText(this);
        etTask.setHint("Что сделать?"); etTask.setText(defTask); etTask.setTextColor(Color.WHITE);
        etTask.setPadding(25, 35, 25, 35); 
        etTask.setBackgroundColor(0xFF222222);

        LinearLayout.LayoutParams pTask = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2.5f);
        pTask.setMargins(0, 0, 16, 0);
        etTask.setLayoutParams(pTask);
        tasks.add(etTask); 
        row.addView(etTask);

        TextView btnDelete = new TextView(this);
        btnDelete.setText("✕");
        btnDelete.setTextColor(0xFF888888);
        btnDelete.setTextSize(18);
        btnDelete.setGravity(android.view.Gravity.CENTER);
        btnDelete.setPadding(25, 25, 25, 25);

        btnDelete.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					times.remove(etTime);
					tasks.remove(etTask);
					container.removeView(row);
				}
			});
        row.addView(btnDelete);

        container.addView(row);
        sortAlarmRowsInContainer(container, times, tasks);
    }

    private void sortAlarmRowsInContainer(final LinearLayout container, final ArrayList<EditText> times, final ArrayList<EditText> tasks) {
        int count = container.getChildCount();
        if (count <= 1) return; 

        class UIAlarmRow implements Comparable<UIAlarmRow> {
            View rowView;
            EditText timeField;
            EditText taskField;
            String timeStr;

            UIAlarmRow(View rowView, EditText timeField, EditText taskField) {
                this.rowView = rowView;
                this.timeField = timeField;
                this.taskField = taskField;
                this.timeStr = timeField.getText().toString().trim();

                if (this.timeStr.contains(":")) {
                    String[] parts = this.timeStr.split(":");
                    if (parts.length == 2) {
                        try {
                            int h = Integer.parseInt(parts[0].trim());
                            int m = Integer.parseInt(parts[1].trim());
                            this.timeStr = String.format(java.util.Locale.US, "%02d:%02d", h, m);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            @Override
            public int compareTo(UIAlarmRow other) {
                return this.timeStr.compareTo(other.timeStr);
            }
        }

        ArrayList<UIAlarmRow> rows = new ArrayList<UIAlarmRow>();
        for (int i = 0; i < count; i++) {
            View child = container.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout rowLayout = (LinearLayout) child;
                if (rowLayout.getChildCount() >= 2) {
                    EditText tField = (EditText) rowLayout.getChildAt(0);
                    EditText kField = (EditText) rowLayout.getChildAt(1);
                    rows.add(new UIAlarmRow(rowLayout, tField, kField));
                }
            }
        }

        java.util.Collections.sort(rows);

        container.removeAllViews();
        times.clear();
        tasks.clear();

        for (UIAlarmRow item : rows) {
            container.addView(item.rowView);
            times.add(item.timeField);
            tasks.add(item.taskField);
        }
    }

    public void saveProfileExternal(int profileIndex, String name, int period, int selectedColor, ArrayList<EditText> timeFields, ArrayList<EditText> taskFields) {
        SharedPreferences.Editor editor = prefs.edit();
        boolean isNew = (profileIndex == -1);
        int index = isNew ? activeProfiles.size() : profileIndex;

        editor.putString("profile_name_" + index, name);
        editor.putInt("profile_period_" + index, period);
        editor.putInt("profile_color_" + index, selectedColor);

        if (isNew) {
            editor.putLong("profile_start_" + index, 0L);
        }

        int validAlarmsCount = 0;
        for (int i = 0; i < timeFields.size(); i++) {
            String rawTime = timeFields.get(i).getText().toString().trim();
            String task = taskFields.get(i).getText().toString().trim();
            if (rawTime.contains(":")) {
                editor.putString("p_" + index + "_alarm_time_" + validAlarmsCount, rawTime);
                editor.putString("p_" + index + "_alarm_task_" + validAlarmsCount, task);
                validAlarmsCount++;
            }
        }
        editor.putInt("profile_alarms_count_" + index, validAlarmsCount);

        if (isNew) {
            editor.putInt("profiles_count", index + 1);
        }
        editor.apply();

        loadProfilesFromPrefs(); 
        recalculateAndScheduleAlarms(this); 

        int currentItem = viewPager.getCurrentItem();
        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(currentItem);
    }

    public void deleteProfileExternal(int profileIndex) {
        int total = prefs.getInt("profiles_count", 0);
        SharedPreferences.Editor editor = prefs.edit();
        int currentWriteIndex = 0;

        for (int i = 0; i < total; i++) {
            if (i == profileIndex) continue;
            editor.putString("profile_name_" + currentWriteIndex, prefs.getString("profile_name_" + i, ""));
            editor.putInt("profile_period_" + currentWriteIndex, prefs.getInt("profile_period_" + i, 1));
            editor.putLong("profile_start_" + currentWriteIndex, prefs.getLong("profile_start_" + i, 0));
            editor.putInt("profile_color_" + currentWriteIndex, prefs.getInt("profile_color_" + i, 0));

            int ac = prefs.getInt("profile_alarms_count_" + i, 0);
            editor.putInt("profile_alarms_count_" + currentWriteIndex, ac);
            for (int a = 0; a < ac; a++) {
                editor.putString("p_" + currentWriteIndex + "_alarm_time_" + a, prefs.getString("p_" + i + "_alarm_time_" + a, ""));
                editor.putString("p_" + currentWriteIndex + "_alarm_task_" + a, prefs.getString("p_" + i + "_alarm_task_" + a, ""));
            }
            currentWriteIndex++;
        }
        editor.putInt("profiles_count", currentWriteIndex);
        editor.apply();

        loadProfilesFromPrefs();
        recalculateAndScheduleAlarms(this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(BASE_POSITION);
    }

    private void showDayActionDialog(final Calendar selectedDate) {
        if (activeProfiles.isEmpty()) {
            Toast.makeText(this, "Сначала создайте профиль дня в меню!", Toast.LENGTH_LONG).show();
            return;
        }

        final int day = selectedDate.get(Calendar.DAY_OF_MONTH);
        final int month = selectedDate.get(Calendar.MONTH);
        final int year = selectedDate.get(Calendar.YEAR);
        final String fullDateTitle = day + " " + monthStr(month) + " " + year;

        final int baseRequestId = (year * 10000) + ((month + 1) * 100) + day;

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(40, 40, 40, 50);

        android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
        background.setColor(0xFF1E1E1E); 
        background.setCornerRadius(32); 
        container.setBackground(background);

        TextView titleView = new TextView(this);
        titleView.setText(fullDateTitle);
        titleView.setTextSize(20);
        titleView.setTextColor(Color.WHITE);
        titleView.setGravity(android.view.Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 40);
        container.addView(titleView);

        int matchedIdx = -1;
        for (int i = 0; i < activeProfiles.size(); i++) {
            DayProfile p = activeProfiles.get(i);
            if (p.startTimestamp == 0) continue;

            Calendar checkCal = (Calendar) selectedDate.clone();
            checkCal.set(Calendar.HOUR_OF_DAY, 0); checkCal.set(Calendar.MINUTE, 0);
            checkCal.set(Calendar.SECOND, 0); checkCal.set(Calendar.MILLISECOND, 0);

            long diff = checkCal.getTimeInMillis() - p.startTimestamp;
            if (diff >= 0 && (int)(diff / (1000 * 60 * 60 * 24)) % p.period == 0) {
                matchedIdx = i;
                break;
            }
        }
        final int matchedProfileIdx = matchedIdx;

        final AlertDialog dialog = builder.setView(container).create();

        if (matchedProfileIdx != -1) {
            DayProfile activeProfile = activeProfiles.get(matchedProfileIdx);

            boolean isVacationEnabled = prefs.getBoolean("vacation_enabled", false);
            long vacationStart = prefs.getLong("vacation_start", 0L);
            long vacationEnd = prefs.getLong("vacation_end", 0L);

            Calendar checkCal = (Calendar) selectedDate.clone();
            checkCal.set(Calendar.HOUR_OF_DAY, 0); checkCal.set(Calendar.MINUTE, 0);
            checkCal.set(Calendar.SECOND, 0); checkCal.set(Calendar.MILLISECOND, 0);
            long currentDayTs = checkCal.getTimeInMillis();

            final boolean isTodayVacation = isVacationEnabled && currentDayTs >= vacationStart && currentDayTs <= vacationEnd;

            if (activeProfile.alarmTimes != null && !activeProfile.alarmTimes.isEmpty()) {
                TextView alarmsHeader = new TextView(this);

                if (isTodayVacation) {
                    alarmsHeader.setText("Будильники отключены (ОТПУСК 🏖️):");
                    alarmsHeader.setTextColor(0xFFFFA500); 
                } else {
                    alarmsHeader.setText("Будильники на сегодня:");
                    alarmsHeader.setTextColor(Color.GRAY);
                }

                alarmsHeader.setTextSize(13);
                alarmsHeader.setPadding(10, 0, 0, 16);
                container.addView(alarmsHeader);

                for (int a = 0; a < activeProfile.alarmTimes.size(); a++) {
                    final int alarmId = a;
                    final String time = activeProfile.alarmTimes.get(a);
                    final String task = activeProfile.alarmTasks.get(a);

                    LinearLayout alarmRow = new LinearLayout(this);
                    alarmRow.setOrientation(LinearLayout.HORIZONTAL);
                    alarmRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
                    alarmRow.setPadding(20, 16, 20, 16);
                    alarmRow.setBackgroundColor(0xFF252525);

                    LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    rowParams.setMargins(0, 0, 0, 16);
                    alarmRow.setLayoutParams(rowParams);

                    TextView tvInfo = new TextView(this);
                    String infoText = time + (task.isEmpty() ? "" : " — " + task);
                    tvInfo.setText(infoText);
                    tvInfo.setTextColor(isTodayVacation ? Color.GRAY : Color.WHITE); 
                    tvInfo.setTextSize(16);
                    LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                    tvInfo.setLayoutParams(infoParams);
                    alarmRow.addView(tvInfo);

                    android.widget.Switch sw = new android.widget.Switch(this);
                    final String blockKey = "disabled_alarm_" + baseRequestId + "_" + matchedProfileIdx + "_" + alarmId;

                    boolean isDisabled = prefs.getBoolean(blockKey, false);
                    sw.setChecked(!isDisabled);

                    if (isTodayVacation) {
                        sw.setEnabled(false); 
                        alarmRow.setAlpha(0.4f); 
                    } else {
                        sw.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
								@Override
								public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
									SharedPreferences.Editor editor = prefs.edit();
									editor.putBoolean(blockKey, !isChecked);
									editor.apply();

									recalculateAndScheduleAlarms(MainActivity.this);
								}
							});
                    }
                    alarmRow.addView(sw);

                    container.addView(alarmRow);
                }
            }

            TextView btnEdit = new TextView(this);
            btnEdit.setText("✏️  Редактировать день (" + activeProfile.name + ")");
            btnEdit.setTextColor(Color.WHITE);
            btnEdit.setTextSize(16);
            btnEdit.setPadding(30, 35, 30, 35);
            btnEdit.setBackgroundColor(0xFF2D2D2D);

            LinearLayout.LayoutParams pEdit = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            pEdit.setMargins(0, 16, 0, 24); 
            btnEdit.setLayoutParams(pEdit);

            btnEdit.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						dialog.dismiss(); 
						showEditProfileDialog(matchedProfileIdx); 
					}
				});
            container.addView(btnEdit);
        }

        final TextView tvToggleSpoiler = new TextView(this);
        tvToggleSpoiler.setText("⚙️ Параметры старта циклов ▾");
        tvToggleSpoiler.setTextColor(0xFF888888);
        tvToggleSpoiler.setTextSize(14);
        tvToggleSpoiler.setGravity(android.view.Gravity.CENTER);
        tvToggleSpoiler.setPadding(20, 20, 20, 20);

        LinearLayout.LayoutParams pToggle = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pToggle.setMargins(0, 10, 0, 0);
        tvToggleSpoiler.setLayoutParams(pToggle);
        container.addView(tvToggleSpoiler);

        final LinearLayout spoilerContainer = new LinearLayout(this);
        spoilerContainer.setOrientation(LinearLayout.VERTICAL);
        spoilerContainer.setVisibility(View.GONE);

        for (int i = 0; i < activeProfiles.size(); i++) {
            final int profileId = i;
            final String profileName = activeProfiles.get(i).name;

            TextView btnStart = new TextView(this);
            btnStart.setText("📅  Сделать стартом для: " + profileName);
            btnStart.setTextColor(Color.GRAY);
            btnStart.setTextSize(15);
            btnStart.setPadding(30, 25, 30, 25);

            LinearLayout.LayoutParams pStart = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            pStart.setMargins(0, 6, 0, 6);
            btnStart.setLayoutParams(pStart);

            btnStart.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						new AlertDialog.Builder(MainActivity.this)
							.setTitle("Изменение старта цикла")
							.setMessage("Вы уверены, что хотите назначить " + fullDateTitle + " начальной точкой для «" + profileName + "»?\n\nВесь календарь будет перестроен.")
							.setPositiveButton("Да, изменить", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface confirmDialog, int which) {
									dialog.dismiss();

									Calendar startCal = (Calendar) selectedDate.clone();
									startCal.set(Calendar.HOUR_OF_DAY, 0); startCal.set(Calendar.MINUTE, 0);
									startCal.set(Calendar.SECOND, 0); startCal.set(Calendar.MILLISECOND, 0);

									SharedPreferences.Editor editor = prefs.edit();
									editor.putLong("profile_start_" + profileId, startCal.getTimeInMillis());
									editor.apply();

									Toast.makeText(MainActivity.this, "Старт привязан к " + fullDateTitle + "!", Toast.LENGTH_SHORT).show();

									loadProfilesFromPrefs();
									recalculateAndScheduleAlarms(MainActivity.this);

									int currentItem = viewPager.getCurrentItem();
									viewPager.setAdapter(pagerAdapter);
									viewPager.setCurrentItem(currentItem);
								}
							})
							.setNegativeButton("Отмена", null)
							.show();
					}
				});
            spoilerContainer.addView(btnStart);
        }

        container.addView(spoilerContainer);

        tvToggleSpoiler.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (spoilerContainer.getVisibility() == View.GONE) {
						spoilerContainer.setVisibility(View.VISIBLE);
						tvToggleSpoiler.setText("⚙️ Параметры старта циклов ▴");
					} else {
						spoilerContainer.setVisibility(View.GONE);
						tvToggleSpoiler.setText("⚙️ Параметры старта циклов ▾");
					}
				}
			});

        if (dialog.getWindow() != null) {
            android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.gravity = android.view.Gravity.CENTER_HORIZONTAL | android.view.Gravity.TOP;
            params.y = 250; 
            dialog.getWindow().setAttributes(params);
        }

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }
    }

    private String monthStr(int index) {
        String[] russianMonths = {"января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря"};
        return russianMonths[index];
    }

    public static void recalculateAndScheduleAlarms(final Context context) {
        new Thread(new Runnable() {
				@Override
				public void run() {
					SharedPreferences prefs = context.getSharedPreferences("app_prefs", MODE_PRIVATE);

					if (activeProfiles == null || activeProfiles.isEmpty()) {
						String json = prefs.getString("day_profiles_json", "");
						if (!android.text.TextUtils.isEmpty(json)) {
							try {
								org.json.JSONArray array = new org.json.JSONArray(json);
								activeProfiles = new java.util.ArrayList<>();
								for (int i = 0; i < array.length(); i++) {
									org.json.JSONObject obj = array.getJSONObject(i);
									DayProfile p = new DayProfile();
									p.name = obj.optString("name", "");
									p.color = obj.optInt("color", android.graphics.Color.RED);
									p.period = obj.optInt("period", 1);
									p.startTimestamp = obj.optLong("startTimestamp", 0L);

									org.json.JSONArray timesArr = obj.optJSONArray("alarmTimes");
									if (timesArr != null) {
										for (int t = 0; t < timesArr.length(); t++) {
											p.alarmTimes.add(timesArr.getString(t));
										}
									}
									org.json.JSONArray tasksArr = obj.optJSONArray("alarmTasks");
									if (tasksArr != null) {
										for (int t = 0; t < tasksArr.length(); t++) {
											p.alarmTasks.add(tasksArr.getString(t));
										}
									}
									activeProfiles.add(p);
								}
							} catch (Exception ignored) {}
						}
					}

					android.app.AlarmManager alarmManager = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
					if (alarmManager == null) return;

					SharedPreferences alarmSettings = context.getSharedPreferences("alarm_settings", MODE_PRIVATE);
					SharedPreferences.Editor editor = prefs.edit();

					boolean isVacationEnabled = prefs.getBoolean("vacation_enabled", false);
					long vacationStart = prefs.getLong("vacation_start", 0L);
					long vacationEnd = prefs.getLong("vacation_end", 0L);

					int preAlarmMinutes = alarmSettings.getInt("alarm_pre_minute", 0);

					String todayShiftName = "Выходной";
					long now = System.currentTimeMillis();

					long closestAlarmTs = 0;
					android.content.Intent closestIntent = null;
					int closestReqId = 0;

					long closestPreAlarmTs = 0;
					android.content.Intent closestPreIntent = null;
					int closestPreReqId = 0;

					String nextAlarmLabel = "";

					Calendar cal = Calendar.getInstance();
					cal.add(Calendar.DAY_OF_MONTH, -2);

					Calendar alarmCal = Calendar.getInstance();
					Calendar profileCal = Calendar.getInstance();

					for (int i = 0; i < 5; i++) {
						int year = cal.get(Calendar.YEAR);
						int month = cal.get(Calendar.MONTH);
						int day = cal.get(Calendar.DAY_OF_MONTH);

						Calendar midnightCal = (Calendar) cal.clone();
						midnightCal.set(Calendar.HOUR_OF_DAY, 0);
						midnightCal.set(Calendar.MINUTE, 0);
						midnightCal.set(Calendar.SECOND, 0);
						midnightCal.set(Calendar.MILLISECOND, 0);
						long checkTs = midnightCal.getTimeInMillis();

						int baseRequestId = (year * 10000) + ((month + 1) * 100) + day;

						int colorToSave = android.graphics.Color.parseColor("#4CAF50");
						String currentDayShiftName = "Выходной";

						if (isVacationEnabled && checkTs >= vacationStart && checkTs <= vacationEnd) {
							currentDayShiftName = "🏖 В отпуске";
							colorToSave = android.graphics.Color.parseColor("#00E676");
						} else if (activeProfiles != null) {
							for (int pIdx = 0; pIdx < activeProfiles.size(); pIdx++) {
								DayProfile p = activeProfiles.get(pIdx);
								if (p.startTimestamp == 0) continue;

								profileCal.setTimeInMillis(p.startTimestamp);
								profileCal.set(Calendar.HOUR_OF_DAY, 0);
								profileCal.set(Calendar.MINUTE, 0);
								profileCal.set(Calendar.SECOND, 0);
								profileCal.set(Calendar.MILLISECOND, 0);
								long profileStartMidnight = profileCal.getTimeInMillis();

								long diffMs = checkTs - profileStartMidnight;
								if (diffMs >= 0) {
									long daysDiff = Math.round((double) diffMs / (1000L * 60L * 60L * 24L));
									if (daysDiff % p.period == 0) {
										colorToSave = p.color; 
										currentDayShiftName = p.name;
										break;
									}
								}
							}
						}

						editor.putInt("widget_color_" + i, colorToSave);

						if (i == 2) {
							todayShiftName = currentDayShiftName;
						}

						if (!(isVacationEnabled && checkTs >= vacationStart && checkTs <= vacationEnd) && activeProfiles != null) {
							for (int pIdx = 0; pIdx < activeProfiles.size(); pIdx++) {
								DayProfile p = activeProfiles.get(pIdx);
								if (p.startTimestamp == 0) continue;

								profileCal.setTimeInMillis(p.startTimestamp);
								profileCal.set(Calendar.HOUR_OF_DAY, 0);
								profileCal.set(Calendar.MINUTE, 0);
								profileCal.set(Calendar.SECOND, 0);
								profileCal.set(Calendar.MILLISECOND, 0);
								long profileStartMidnight = profileCal.getTimeInMillis();

								long diffMs = checkTs - profileStartMidnight;
								if (diffMs >= 0) {
									long daysDiff = Math.round((double) diffMs / (1000L * 60L * 60L * 24L));
									if (daysDiff % p.period == 0) {
										for (int a = 0; a < p.alarmTimes.size(); a++) {
											try {
												String blockKey = "disabled_alarm_" + baseRequestId + "_" + pIdx + "_" + a;
												if (prefs.getBoolean(blockKey, false)) continue;

												String timeStr = p.alarmTimes.get(a);
												int colonIdx = timeStr.indexOf(':');
												if (colonIdx == -1) continue;
												int hour = Integer.parseInt(timeStr.substring(0, colonIdx).trim());
												int minute = Integer.parseInt(timeStr.substring(colonIdx + 1).trim());
												String task = p.alarmTasks.get(a);

												alarmCal.set(year, month, day, hour, minute, 0);
												alarmCal.set(Calendar.MILLISECOND, 0);

												long mainAlarmTs = alarmCal.getTimeInMillis();
												int currentReqId = baseRequestId + (a * 1000000);

												if (mainAlarmTs > now) {
													if (closestAlarmTs == 0 || mainAlarmTs < closestAlarmTs) {
														closestAlarmTs = mainAlarmTs;
														closestReqId = currentReqId;
														closestIntent = new android.content.Intent(context, AlarmReceiver.class);
														closestIntent.putExtra("SHIFT_DAY_NAME", p.name);
														closestIntent.putExtra("SHIFT_DAY_NUMBER", pIdx + 1);
														closestIntent.putExtra("CUSTOM_TASK", task);
														closestIntent.putExtra("IS_PRE_ALARM", false);
														closestIntent.putExtra("SHIFT_PERIOD", p.period);
														closestIntent.putExtra("ALARM_REQ_ID", closestReqId);
														nextAlarmLabel = task.isEmpty() ? p.name : task;
													}
												}

												if (preAlarmMinutes > 0) {
													long preTs = mainAlarmTs - (preAlarmMinutes * 60 * 1000L);
													if (preTs > now) {
														if (closestPreAlarmTs == 0 || preTs < closestPreAlarmTs) {
															closestPreAlarmTs = preTs;
															closestPreReqId = currentReqId + 500000;
															closestPreIntent = new android.content.Intent(context, AlarmReceiver.class);
															closestPreIntent.putExtra("SHIFT_DAY_NAME", p.name);
															closestPreIntent.putExtra("SHIFT_DAY_NUMBER", pIdx + 1);
															closestPreIntent.putExtra("CUSTOM_TASK", "Предварительный сигнал");
															closestPreIntent.putExtra("IS_PRE_ALARM", true);
															closestPreIntent.putExtra("SHIFT_PERIOD", p.period);
															closestPreIntent.putExtra("ALARM_REQ_ID", closestPreReqId);
														}
													}
												}
											} catch (Exception ignored) {}
										}
									}
								}
							}
						}

						cal.add(Calendar.DAY_OF_MONTH, 1);
					}

					int flags = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M ? 
						android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE : 
						android.app.PendingIntent.FLAG_UPDATE_CURRENT;

					if (closestAlarmTs > 0 && closestIntent != null) {
						android.app.PendingIntent pIntent = android.app.PendingIntent.getBroadcast(context, closestReqId, closestIntent, flags);
						setSystemAlarmClock(alarmManager, closestAlarmTs, pIntent);
					}

					if (closestPreAlarmTs > 0 && closestPreIntent != null) {
						android.app.PendingIntent prePIntent = android.app.PendingIntent.getBroadcast(context, closestPreReqId, closestPreIntent, flags);
						setSystemAlarmClock(alarmManager, closestPreAlarmTs, prePIntent);
					}

					editor.putString("today_shift_name", todayShiftName);
					editor.putLong("next_alarm_time_ms", closestAlarmTs);
					editor.putString("next_alarm_label", nextAlarmLabel);
					editor.commit();

					ShiftWidgetProvider.updateAllWidgets(context);
				}
			}).start();
    }

    private void showVacationDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(50, 40, 50, 50);
        container.setBackgroundColor(0xFF1E1E1E);

        TextView titleView = new TextView(this);
        titleView.setText("🏖️ Режим отпуска");
        titleView.setTextSize(22);
        titleView.setTextColor(Color.WHITE);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setGravity(android.view.Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 40);
        container.addView(titleView);

        final android.widget.Switch swVacation = new android.widget.Switch(this);
        swVacation.setText("Активировать режим отпуска ");
        swVacation.setTextColor(Color.WHITE);
        swVacation.setTextSize(16);
        swVacation.setPadding(0, 0, 0, 40);
        swVacation.setChecked(prefs.getBoolean("vacation_enabled", false));
        container.addView(swVacation);

        final LinearLayout datePickersLayout = new LinearLayout(this);
        datePickersLayout.setOrientation(LinearLayout.VERTICAL);
        datePickersLayout.setVisibility(swVacation.isChecked() ? View.VISIBLE : View.GONE);

        final Calendar startCal = Calendar.getInstance();
        long savedStart = prefs.getLong("vacation_start", 0L);
        if (savedStart != 0) startCal.setTimeInMillis(savedStart);
        else {
            startCal.set(Calendar.HOUR_OF_DAY, 0); startCal.set(Calendar.MINUTE, 0);
            startCal.set(Calendar.SECOND, 0); startCal.set(Calendar.MILLISECOND, 0);
        }

        final Calendar endCal = Calendar.getInstance();
        long savedEnd = prefs.getLong("vacation_end", 0L);
        if (savedEnd != 0) endCal.setTimeInMillis(savedEnd);
        else {
            endCal.add(Calendar.DAY_OF_MONTH, 7); 
            endCal.set(Calendar.HOUR_OF_DAY, 23); endCal.set(Calendar.MINUTE, 59);
            endCal.set(Calendar.SECOND, 59); endCal.set(Calendar.MILLISECOND, 999);
        }

        final Button btnStart = new Button(this);
        btnStart.setBackgroundColor(0xFF2D2D2D);
        btnStart.setTextColor(Color.WHITE);
        final java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMMM yyyy", new java.util.Locale("ru"));
        btnStart.setText("Начало отпуска (С): " + sdf.format(startCal.getTime()));

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(0, 0, 0, 20);
        btnStart.setLayoutParams(btnParams);

        btnStart.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					new android.app.DatePickerDialog(MainActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_DARK,
						new android.app.DatePickerDialog.OnDateSetListener() {
							@Override
							public void onDateSet(android.widget.DatePicker view, int year, int month, int dayOfMonth) {
								startCal.set(year, month, dayOfMonth, 0, 0, 0);
								startCal.set(Calendar.MILLISECOND, 0);
								btnStart.setText("Начало отпуска (С): " + sdf.format(startCal.getTime()));
							}
						}, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH)).show();
				}
			});
        datePickersLayout.addView(btnStart);

        final Button btnEnd = new Button(this);
        btnEnd.setBackgroundColor(0xFF2D2D2D);
        btnEnd.setTextColor(Color.WHITE);
        btnEnd.setText("Конец отпуска (ПО): " + sdf.format(endCal.getTime()));
        btnEnd.setLayoutParams(btnParams);

        btnEnd.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					new android.app.DatePickerDialog(MainActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_DARK,
						new android.app.DatePickerDialog.OnDateSetListener() {
							@Override
							public void onDateSet(android.widget.DatePicker view, int year, int month, int dayOfMonth) {
								endCal.set(year, month, dayOfMonth, 23, 59, 59);
								endCal.set(Calendar.MILLISECOND, 999);
								btnEnd.setText("Конец отпуска (ПО): " + sdf.format(endCal.getTime()));
							}
						}, endCal.get(Calendar.YEAR), endCal.get(Calendar.MONTH), endCal.get(Calendar.DAY_OF_MONTH)).show();
				}
			});
        datePickersLayout.addView(btnEnd);

        container.addView(datePickersLayout);

        swVacation.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
					datePickersLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
				}
			});

        LinearLayout buttonsLayout = new LinearLayout(this);
        buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonsLayout.setGravity(android.view.Gravity.CENTER);
        buttonsLayout.setPadding(0, 30, 0, 0);

        LinearLayout.LayoutParams pBtn = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        pBtn.setMargins(10, 0, 10, 0);

        final AlertDialog dialog = builder.setView(container).create();

        Button btnCancel = new Button(this);
        btnCancel.setText("ОТМЕНА");
        btnCancel.setTextColor(Color.GRAY);
        btnCancel.setBackgroundColor(0xFF2D2D2D);
        btnCancel.setLayoutParams(pBtn);
        btnCancel.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					dialog.dismiss();
				}
			});
        buttonsLayout.addView(btnCancel);

        Button btnSave = new Button(this);
        btnSave.setText("СОХРАНИТЬ");
        btnSave.setTextColor(0xFF4682B4);
        btnSave.setBackgroundColor(0xFF2D2D2D);
        btnSave.setLayoutParams(pBtn);
        btnSave.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					boolean enabled = swVacation.isChecked();

					if (enabled && startCal.getTimeInMillis() > endCal.getTimeInMillis()) {
						Toast.makeText(MainActivity.this, "Дата старта не может быть позже даты окончания!", Toast.LENGTH_LONG).show();
						return;
					}

					SharedPreferences.Editor editor = prefs.edit();
					editor.putBoolean("vacation_enabled", enabled);
					editor.putLong("vacation_start", startCal.getTimeInMillis());
					editor.putLong("vacation_end", endCal.getTimeInMillis());
					editor.apply();

					recalculateAndScheduleAlarms(MainActivity.this);

					int currentItem = viewPager.getCurrentItem();
					viewPager.setAdapter(pagerAdapter);
					viewPager.setCurrentItem(currentItem);

					Toast.makeText(MainActivity.this, enabled ? "Режим отпуска сохранен и активирован!" : "Режим отпуска отключен!", Toast.LENGTH_SHORT).show();
					dialog.dismiss();
				}
			});
        buttonsLayout.addView(btnSave);

        container.addView(buttonsLayout);

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }
    }

    private class MonthPagerAdapter extends PagerAdapter {
        @Override public int getCount() { return 1000; }
        @Override public boolean isViewFromObject(View view, Object object) { return view == object; }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            final GridView grid = new GridView(MainActivity.this);
            grid.setNumColumns(7);
            grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);

            int spacing = (int) (4 * getResources().getDisplayMetrics().density);
            grid.setHorizontalSpacing(spacing);
            grid.setVerticalSpacing(spacing);

            final Calendar monthCalendar = Calendar.getInstance();
            monthCalendar.add(Calendar.MONTH, position - BASE_POSITION);

            final GridAdapter gridAdapter = new GridAdapter(MainActivity.this, monthCalendar, grid);
            grid.setAdapter(gridAdapter);

            grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
						Calendar selected = (Calendar) gridAdapter.getItem(pos);
						if (selected != null) {
							showDayActionDialog(selected);
						}
					}
				});

            container.addView(grid);
            return grid;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
    }

    private class GridAdapter extends BaseAdapter {
        private Context context;
        private ArrayList<Calendar> days = new ArrayList<Calendar>();
        private Calendar pageMonth;
        private GridView parentGrid;

        private float density;
        private int cellHeight = 0;
        private int defaultHeightPx;
        private android.graphics.drawable.GradientDrawable normalTileDrawable;
        private long zoneOffset; 

        private android.graphics.Paint linePaint;
        private int lineWidth;  
        private int lineHeight; 

        private class DayTileView extends TextView {
            private boolean hasAlarms = false;
            private boolean isToday = false;
            private android.graphics.Paint todayPaint;

            public DayTileView(Context context) {
                super(context);

                todayPaint = new android.graphics.Paint();
                todayPaint.setAntiAlias(true);
                todayPaint.setColor(0xFFFF4081); 
                todayPaint.setStyle(android.graphics.Paint.Style.FILL);
            }

            public void setAlarms(int count) {
                this.hasAlarms = count > 0;
            }

            public void setIsToday(boolean today) {
                this.isToday = today;
                invalidate();
            }

            @Override
            protected void onDraw(android.graphics.Canvas canvas) {
                if (isToday) {
                    float radius = 12 * density;

                    android.graphics.Paint textPaint = getPaint();
                    String text = getText().toString();

                    android.graphics.Rect textBounds = new android.graphics.Rect();
                    textPaint.getTextBounds(text, 0, text.length(), textBounds);

                    float textX = getWidth() - (getPaddingRight()) - (textPaint.measureText(text) / 2f);
                    float textY = getBaseline() - (textBounds.height() / 2f);

                    canvas.drawCircle(textX, textY, radius, todayPaint);
                }

                super.onDraw(canvas);

                if (hasAlarms) {
                    int width = getWidth();
                    int height = getHeight();

                    float startX = (width - lineWidth) / 2f;
                    float endX = startX + lineWidth;
                    float posY = height - (12 * density);

                    canvas.drawRoundRect(
                        startX, 
                        posY - lineHeight, 
                        endX, 
                        posY, 
                        2 * density, 
                        2 * density, 
                        linePaint
                    );
                }
            }
        }

        public GridAdapter(Context context, Calendar month, GridView grid) {
            this.context = context;
            this.pageMonth = (Calendar) month.clone();
            this.parentGrid = grid;

            this.density = context.getResources().getDisplayMetrics().density;
            this.defaultHeightPx = (int) (48 * density);

            this.lineWidth = (int) (30 * density);  
            this.lineHeight = (int) (1 * density);  

            this.linePaint = new android.graphics.Paint();
            this.linePaint.setAntiAlias(true);
            this.linePaint.setColor(0xFF00E676);    
            this.linePaint.setStyle(android.graphics.Paint.Style.FILL);

            this.normalTileDrawable = new android.graphics.drawable.GradientDrawable();
            this.normalTileDrawable.setCornerRadius(16f * density / 2); 
            this.normalTileDrawable.setColor(0xFF222222);

            Calendar cal = (Calendar) month.clone();
            cal.set(Calendar.DAY_OF_MONTH, 1);

            int monthStartCell = cal.get(Calendar.DAY_OF_WEEK) - 2;
            if (monthStartCell < 0) monthStartCell = 6; 

            cal.add(Calendar.DAY_OF_MONTH, -monthStartCell);

            Calendar tempCal = (Calendar) cal.clone();
            tempCal.add(Calendar.DAY_OF_MONTH, monthStartCell);
            int maxDaysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH);
            int totalCells = (monthStartCell + maxDaysInMonth > 35) ? 42 : 35;

            while (days.size() < totalCells) {
                days.add((Calendar) cal.clone());
                cal.add(Calendar.DAY_OF_MONTH, 1);
            }
            this.zoneOffset = java.util.TimeZone.getDefault().getOffset(System.currentTimeMillis());
        }

        @Override 
        public int getCount() { 
            return days.size(); 
        }

        @Override 
        public Object getItem(int position) { 
            return days.get(position); 
        }

        @Override 
        public long getItemId(int position) { 
            return position; 
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            DayTileView tileView = (DayTileView) convertView;
            if (tileView == null) {
                tileView = new DayTileView(context);
                tileView.setGravity(android.view.Gravity.TOP | android.view.Gravity.END);
                tileView.setTextSize(14);
                tileView.setPadding(0, (int)(12 * density), (int)(12 * density), 0); 
            }

            if (cellHeight <= 0) {
                int gridHeight = parentGrid.getHeight();
                if (gridHeight > 0) {
                    int spacing = (int) (4 * density);
                    int rowsCount = days.size() / 7;
                    int totalSpacing = spacing * (rowsCount - 1);
                    cellHeight = (gridHeight - totalSpacing) / rowsCount;
                }
            }

            if (cellHeight > 0) {
                tileView.setHeight(cellHeight);
            } else {
                tileView.setHeight(defaultHeightPx); 
            }

            Calendar date = days.get(position);
            int day = date.get(Calendar.DAY_OF_MONTH);
            tileView.setText(String.valueOf(day));

            Calendar today = Calendar.getInstance();
            boolean isToday = (date.get(Calendar.YEAR) == today.get(Calendar.YEAR)) &&
                (date.get(Calendar.MONTH) == today.get(Calendar.MONTH)) &&
                (date.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH));

            tileView.setIsToday(isToday);

            if (date.get(Calendar.MONTH) == pageMonth.get(Calendar.MONTH)) {
                tileView.setTextColor(Color.WHITE);

                int matchedColor = Color.TRANSPARENT;
                int alarmsCount = 0;

                long localCheckTime = date.getTimeInMillis() + zoneOffset;
                long checkTime = (localCheckTime / 86400000L) * 86400000L;

                for (int i = 0; i < activeProfiles.size(); i++) {
                    DayProfile p = activeProfiles.get(i);
                    if (p.startTimestamp == 0) continue;

                    long localStart = p.startTimestamp + zoneOffset;
                    long startZero = (localStart / 86400000L) * 86400000L;

                    long diff = checkTime - startZero;

                    if (diff >= 0) {
                        int daysDiff = (int) (diff / 86400000L);

                        if (daysDiff % p.period == 0) {
                            matchedColor = p.color; 
                            alarmsCount = p.alarmTimes.size(); 
                            break; 
                        }
                    }
                }

                tileView.setAlarms(alarmsCount);

                if (matchedColor != Color.TRANSPARENT) {
                    android.graphics.drawable.GradientDrawable activeTile = new android.graphics.drawable.GradientDrawable();
                    activeTile.setCornerRadius(16f * density / 2);
                    activeTile.setColor(0xFF161616);         
                    activeTile.setStroke(4, matchedColor);   
                    tileView.setBackground(activeTile);
                } else {
                    tileView.setBackground(normalTileDrawable); 
                }

            } else {
                tileView.setTextColor(Color.DKGRAY);
                tileView.setBackground(null);
                tileView.setAlarms(0); 
            }

            return tileView;
        }
    }

    private static void setSystemAlarmClock(android.app.AlarmManager alarmManager, long triggerAtMillis, android.app.PendingIntent operationIntent) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            android.app.AlarmManager.AlarmClockInfo clockInfo = 
                new android.app.AlarmManager.AlarmClockInfo(triggerAtMillis, operationIntent);
            alarmManager.setAlarmClock(clockInfo, operationIntent);
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerAtMillis, operationIntent);
        } else {
            alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, triggerAtMillis, operationIntent);
        }
    }
}

