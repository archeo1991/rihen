package cn.archeo.rihen;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.LocalDate;

final class LifeDaysStore {
    private static final String PREFS = "life_days";
    private static final String KEY_BIRTHDAY = "birthday";

    private LifeDaysStore() {
    }

    static void saveBirthday(Context context, LocalDate birthday) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_BIRTHDAY, birthday.toString())
                .apply();
    }

    static LocalDate loadBirthday(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String value = prefs.getString(KEY_BIRTHDAY, null);
        if (value == null) return null;
        try {
            LocalDate birthday = LocalDate.parse(value);
            if (birthday.isAfter(LocalDate.now())) return null;
            return birthday;
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
