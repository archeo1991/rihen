package cn.archeo.rihen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.time.LocalDate;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_DATE_CHANGED.equals(action)
                && !Intent.ACTION_TIME_CHANGED.equals(action)
                && !Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            return;
        }

        LocalDate birthday = LifeDaysStore.loadBirthday(context);
        if (birthday != null) {
            MilestoneScheduler.reschedule(context, birthday);
        }
        LifeDaysAppWidgetProvider.updateAll(context);
    }
}
