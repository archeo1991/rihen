package cn.archeo.rihen;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import java.time.LocalDate;

public class MilestoneReceiver extends BroadcastReceiver {
    static final String EXTRA_MILESTONE = "milestone";
    static final String CHANNEL_ID = "life_days_milestones";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        int milestone = intent.getIntExtra(EXTRA_MILESTONE, 0);
        if (milestone <= 0) return;

        ensureChannel(context);

        Intent launchIntent = new Intent(context, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = "\u4eca\u5929\u662f\u7b2c " + milestone + " \u5929";
        String body = "\u4e00\u4e2a\u5b89\u9759\u7684\u5c0f\u91cc\u7a0b\u7891\uff0c\u5df2\u7ecf\u8d70\u5230\u8fd9\u91cc\u4e86\u3002";

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);
        Notification notification = builder
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build();

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(milestone, notification);
        }

        LocalDate birthday = LifeDaysStore.loadBirthday(context);
        if (birthday != null) {
            MilestoneScheduler.reschedule(context, birthday);
        }
    }

    static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "\u7279\u6b8a\u5929\u6570\u63d0\u9192",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("\u5728 1111 \u5929\u30011234 \u5929\u7b49\u7279\u6b8a\u65e5\u5b50\u63d0\u9192\u4f60\u3002");
        manager.createNotificationChannel(channel);
    }
}
