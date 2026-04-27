package com.example.smartspendai.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.smartspendai.data.model.User;

public class SessionManager {
    private static final String PREF_NAME = "smart_spend_session";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveUser(User user) {
        preferences.edit()
                .putLong(KEY_USER_ID, user.getId())
                .putString(KEY_NAME, user.getName())
                .putString(KEY_EMAIL, user.getEmail())
                .apply();
    }

    public User getCurrentUser() {
        long id = preferences.getLong(KEY_USER_ID, -1);
        if (id == -1) {
            return null;
        }
        return new User(
                id,
                preferences.getString(KEY_NAME, ""),
                preferences.getString(KEY_EMAIL, "")
        );
    }

    public void clear() {
        preferences.edit().clear().apply();
    }
}
