package cn.archeo.rihen;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Locale;

public class LifeDaysAppWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_DAILY_REFRESH = "cn.archeo.rihen.ACTION_DAILY_REFRESH_WIDGET";
    private static final int REQUEST_DAILY_REFRESH = 9300;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
        scheduleDailyRefresh(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent != null && ACTION_DAILY_REFRESH.equals(intent.getAction())) {
            updateAll(context);
            scheduleDailyRefresh(context);
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        scheduleDailyRefresh(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        cancelDailyRefresh(context);
    }

    static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context, LifeDaysAppWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(componentName);
        for (int id : ids) {
            updateWidget(context, manager, id);
        }
        if (ids.length > 0) {
            scheduleDailyRefresh(context);
        }
    }

    static void scheduleDailyRefresh(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context, LifeDaysAppWidgetProvider.class);
        if (manager.getAppWidgetIds(componentName).length == 0) return;
        if (LifeDaysStore.loadBirthday(context) == null) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        LocalDateTime nextRefresh = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(0, 1));
        long triggerAtMillis = nextRefresh.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        PendingIntent pendingIntent = dailyRefreshPendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT);

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

    static void cancelDailyRefresh(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        PendingIntent pendingIntent = dailyRefreshPendingIntent(context, PendingIntent.FLAG_NO_CREATE);
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    private static PendingIntent dailyRefreshPendingIntent(Context context, int flag) {
        Intent intent = new Intent(context, LifeDaysAppWidgetProvider.class);
        intent.setAction(ACTION_DAILY_REFRESH);
        return PendingIntent.getBroadcast(
                context,
                REQUEST_DAILY_REFRESH,
                intent,
                flag | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static void updateWidget(Context context, AppWidgetManager manager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.life_days_widget);
        LocalDate birthday = LifeDaysStore.loadBirthday(context);

        if (birthday == null) {
            views.setTextViewText(R.id.widget_title, "\u65e5\u75d5");
            views.setTextViewText(R.id.widget_days, "--");
            views.setTextViewText(R.id.widget_hint, "\u8bbe\u7f6e");
        } else {
            long lived = Milestones.livedDays(birthday, LocalDate.now());
            views.setTextViewText(R.id.widget_title, "\u4eba\u95f4\u7b2c");
            views.setTextViewText(R.id.widget_days, NumberFormat.getInstance(Locale.CHINA).format(lived));
            views.setTextViewText(R.id.widget_hint, "\u5929");
        }

        Intent openIntent = new Intent(context, MainActivity.class);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                context,
                9100,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_root, openPendingIntent);

        manager.updateAppWidget(appWidgetId, views);
    }
}
