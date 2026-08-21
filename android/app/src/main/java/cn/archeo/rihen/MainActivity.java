package cn.archeo.rihen;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQUEST_NOTIFICATIONS = 1101;

    private TextView hintView;
    private TextView daysView;
    private TextView nextView;
    private Button birthdayButton;
    private Button editButton;
    private Button reminderButton;
    private LocalDate birthday;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        birthday = LifeDaysStore.loadBirthday(this);
        buildUi();
        MilestoneReceiver.ensureChannel(this);
        render();
        requestNotificationPermissionIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
        if (birthday != null && notificationsAllowed()) {
            MilestoneScheduler.reschedule(this, birthday);
        }
        LifeDaysAppWidgetProvider.updateAll(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATIONS && birthday != null
                && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            MilestoneScheduler.reschedule(this, birthday);
        }
        render();
    }

    private void buildUi() {
        int side = dp(22);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(side, side, side, side);
        root.setBackgroundColor(Color.parseColor("#F7FAF3"));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(22), dp(20), dp(22), dp(32));
        card.setBackgroundResource(R.drawable.card_bg);
        root.addView(card, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        TextView title = new TextView(this);
        title.setText("\u65e5\u75d5");
        title.setTextColor(Color.parseColor("#323B2A"));
        title.setTextSize(14);
        title.setGravity(Gravity.CENTER);
        card.addView(title);

        hintView = new TextView(this);
        hintView.setTextColor(Color.parseColor("#7C8774"));
        hintView.setTextSize(13);
        hintView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        hintParams.setMargins(0, dp(20), 0, dp(6));
        card.addView(hintView, hintParams);

        daysView = new TextView(this);
        daysView.setTextColor(Color.parseColor("#182015"));
        daysView.setTextSize(72);
        daysView.setGravity(Gravity.CENTER);
        daysView.setIncludeFontPadding(false);
        card.addView(daysView);

        TextView unit = new TextView(this);
        unit.setText("\u5929");
        unit.setTextColor(Color.parseColor("#526748"));
        unit.setTextSize(13);
        unit.setGravity(Gravity.CENTER);
        unit.setPadding(dp(14), dp(5), dp(14), dp(5));
        unit.setBackgroundResource(R.drawable.pill_bg);
        LinearLayout.LayoutParams unitParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        unitParams.setMargins(0, dp(8), 0, 0);
        card.addView(unit, unitParams);

        nextView = new TextView(this);
        nextView.setTextColor(Color.parseColor("#6B7665"));
        nextView.setTextSize(12);
        nextView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams nextParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        nextParams.setMargins(0, dp(18), 0, 0);
        card.addView(nextView, nextParams);

        reminderButton = button("\u5f00\u542f\u51c6\u65f6\u63d0\u9192");
        reminderButton.setOnClickListener(v -> handleReminderAction());
        LinearLayout.LayoutParams reminderParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(38)
        );
        reminderParams.setMargins(0, dp(14), 0, 0);
        card.addView(reminderButton, reminderParams);

        birthdayButton = button("\u8bbe\u7f6e\u751f\u65e5");
        birthdayButton.setOnClickListener(v -> showBirthdayPicker());
        LinearLayout.LayoutParams birthdayParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(46)
        );
        birthdayParams.setMargins(0, dp(18), 0, dp(8));
        card.addView(birthdayButton, birthdayParams);

        editButton = button("\u4fee\u6539\u751f\u65e5");
        editButton.setOnClickListener(v -> showBirthdayPicker());
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(38)
        );
        editParams.setMargins(0, dp(14), 0, dp(10));
        card.addView(editButton, editParams);

        setContentView(root);
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(13);
        button.setTextColor(Color.parseColor("#43523A"));
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(18), 0, dp(18), 0);
        button.setBackgroundResource(R.drawable.button_bg);
        return button;
    }

    private void render() {
        if (birthday == null) {
            hintView.setText("\u5148\u8bbe\u7f6e\u751f\u65e5");
            daysView.setText("--");
            nextView.setText("1111\u30011234 \u7b49\u7279\u6b8a\u5929\u6570\u4f1a\u63d0\u9192\u4f60\u3002");
            birthdayButton.setVisibility(View.VISIBLE);
            editButton.setVisibility(View.GONE);
            reminderButton.setVisibility(View.GONE);
            return;
        }

        long lived = Milestones.livedDays(birthday, LocalDate.now());
        daysView.setText(NumberFormat.getInstance(Locale.CHINA).format(lived));
        hintView.setText("\u4f60\u5df2\u7ecf\u6d3b\u4e86");
        birthdayButton.setVisibility(View.GONE);
        editButton.setVisibility(View.VISIBLE);

        List<Integer> upcoming = Milestones.upcomingSpecialDays(birthday, LocalDate.now(), 1);
        if (upcoming.isEmpty()) {
            nextView.setText("\u63a5\u4e0b\u6765\u7684\u65e5\u5b50\uff0c\u4e5f\u90fd\u7b97\u6570\u3002");
        } else {
            int next = upcoming.get(0);
            LocalDate date = Milestones.dateForMilestone(birthday, next);
            nextView.setText("\u4e0b\u4e00\u4e2a\u63d0\u9192\uff1a\u7b2c " + next + " \u5929\uff0c" + date);
        }

        if (needsReminderAction()) {
            reminderButton.setText(reminderActionText());
            reminderButton.setVisibility(View.VISIBLE);
        } else {
            reminderButton.setVisibility(View.GONE);
        }
    }

    private void showBirthdayPicker() {
        LocalDate initial = birthday == null ? LocalDate.of(2000, 1, 1) : birthday;
        LinearLayout pickerLayout = new LinearLayout(this);
        pickerLayout.setOrientation(LinearLayout.HORIZONTAL);
        pickerLayout.setGravity(Gravity.CENTER);
        pickerLayout.setPadding(dp(12), dp(8), dp(12), 0);

        NumberPicker yearPicker = new NumberPicker(this);
        yearPicker.setMinValue(1850);
        yearPicker.setMaxValue(LocalDate.now().getYear());
        yearPicker.setValue(initial.getYear());
        yearPicker.setWrapSelectorWheel(false);

        NumberPicker monthPicker = new NumberPicker(this);
        monthPicker.setMinValue(1);
        monthPicker.setMaxValue(12);
        monthPicker.setDisplayedValues(twoDigitLabels(12));
        monthPicker.setValue(initial.getMonthValue());

        NumberPicker dayPicker = new NumberPicker(this);
        dayPicker.setMinValue(1);
        dayPicker.setMaxValue(daysInMonth(initial.getYear(), initial.getMonthValue()));
        dayPicker.setDisplayedValues(twoDigitLabels(dayPicker.getMaxValue()));
        dayPicker.setValue(initial.getDayOfMonth());

        LinearLayout.LayoutParams columnParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        pickerLayout.addView(yearPicker, columnParams);
        pickerLayout.addView(monthPicker, columnParams);
        pickerLayout.addView(dayPicker, columnParams);

        NumberPicker.OnValueChangeListener listener = (picker, oldValue, newValue) -> {
            int maxDay = daysInMonth(yearPicker.getValue(), monthPicker.getValue());
            int selectedDay = Math.min(dayPicker.getValue(), maxDay);
            dayPicker.setDisplayedValues(null);
            dayPicker.setMaxValue(maxDay);
            dayPicker.setDisplayedValues(twoDigitLabels(maxDay));
            dayPicker.setValue(selectedDay);
        };
        yearPicker.setOnValueChangedListener(listener);
        monthPicker.setOnValueChangedListener(listener);

        new AlertDialog.Builder(this)
                .setTitle("\u9009\u62e9\u751f\u65e5")
                .setView(pickerLayout)
                .setNegativeButton("\u53d6\u6d88", null)
                .setPositiveButton("\u4fdd\u5b58", (dialog, which) -> onBirthdayPicked(
                        yearPicker.getValue(),
                        monthPicker.getValue(),
                        dayPicker.getValue()
                ))
                .show();
    }

    private void onBirthdayPicked(int year, int month, int day) {
        birthday = LocalDate.of(year, month, day);
        LifeDaysStore.saveBirthday(this, birthday);
        render();
        LifeDaysAppWidgetProvider.updateAll(this);
        requestNotificationPermissionIfNeeded();
        MilestoneScheduler.reschedule(this, birthday);
    }

    private boolean needsReminderAction() {
        return birthday != null && (!notificationsAllowed() || !exactAlarmsAllowed());
    }

    private String reminderActionText() {
        if (!notificationsAllowed()) return "\u5f00\u542f\u901a\u77e5\u6743\u9650";
        return "\u5f00\u542f\u51c6\u65f6\u63d0\u9192";
    }

    private void handleReminderAction() {
        if (!notificationsAllowed()) {
            openNotificationSettings();
            return;
        }
        requestExactAlarmPermissionIfNeeded();
    }

    private void openNotificationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(intent);
        } else {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName())));
        }
    }

    private void requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || exactAlarmsAllowed()) return;
        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:" + getPackageName()));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ignored) {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName())));
        }
    }

    private int daysInMonth(int year, int month) {
        return LocalDate.of(year, month, 1).lengthOfMonth();
    }

    private String[] twoDigitLabels(int count) {
        String[] labels = new String[count];
        for (int i = 0; i < count; i++) {
            labels[i] = String.format(Locale.US, "%02d", i + 1);
        }
        return labels;
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsAllowed()) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        }
    }

    private boolean notificationsAllowed() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean exactAlarmsAllowed() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        return alarmManager != null && alarmManager.canScheduleExactAlarms();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
