package cn.archeo.rihen;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

final class MilestoneScheduler {
    private static final int REQUEST_BASE = 7300;
    private static final int SCHEDULE_LIMIT = 8;

    private MilestoneScheduler() {
    }

    static void reschedule(Context context, LocalDate birthday) {
        cancelAll(context);
        if (birthday == null) return;

        List<Integer> milestones = Milestones.upcomingSpecialDays(birthday, LocalDate.now(), SCHEDULE_LIMIT);
        for (int i = 0; i < milestones.size(); i++) {
            schedule(context, birthday, milestones.get(i), i);
        }
    }

    static void cancelAll(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        for (int i = 0; i < SCHEDULE_LIMIT; i++) {
            Intent intent = new Intent(context, MilestoneReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    REQUEST_BASE + i,
                    intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
            }
        }
    }

    private static void schedule(Context context, LocalDate birthday, int milestone, int slot) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        if (!canPostNotifications(context)) return;

        LocalDate milestoneDate = Milestones.dateForMilestone(birthday, milestone);
        LocalDateTime notifyAt = LocalDateTime.of(milestoneDate, LocalTime.of(9, 0));
        long triggerAtMillis = notifyAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        if (triggerAtMillis <= System.currentTimeMillis()) return;

        Intent intent = new Intent(context, MilestoneReceiver.class)
                .putExtra(MilestoneReceiver.EXTRA_MILESTONE, milestone);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_BASE + slot,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        scheduleAlarm(alarmManager, triggerAtMillis, pendingIntent);
    }

    private static void scheduleAlarm(AlarmManager alarmManager, long triggerAtMillis, PendingIntent pendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }

    private static boolean canPostNotifications(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }
}
