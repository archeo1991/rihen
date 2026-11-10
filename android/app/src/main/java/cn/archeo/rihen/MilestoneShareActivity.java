package cn.archeo.rihen;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import java.time.LocalDate;

public class MilestoneShareActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            finish();
            return;
        }

        int milestone = getIntent().getIntExtra(MilestoneReceiver.EXTRA_MILESTONE, 0);
        LocalDate birthday = LifeDaysStore.loadBirthday(this);
        if (birthday == null || !Milestones.isSpecialDay(milestone)) {
            Toast.makeText(this, R.string.share_expired, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        LocalDate today = LocalDate.now();
        long lived = Milestones.livedDays(birthday, today);
        if (lived != milestone) {
            Toast.makeText(this, R.string.share_expired, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!MilestoneShareCard.share(this, birthday, milestone, today)) {
            Toast.makeText(this, R.string.share_unavailable, Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}
