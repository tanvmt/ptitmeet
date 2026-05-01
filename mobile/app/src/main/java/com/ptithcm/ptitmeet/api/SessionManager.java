package com.ptithcm.ptitmeet.api;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "PTITMeetSession";
    private static final String KEY_TOKEN = "accessToken";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_NAME = "fullName";

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void saveAuthData(String token, Long userId, String fullName) {
        editor.putString(KEY_TOKEN, token);
        editor.putLong(KEY_USER_ID, userId);
        editor.putString(KEY_USER_NAME, fullName);
        editor.apply();
    }

    public String getToken() {
        return pref.getString(KEY_TOKEN, null);
    }
    public String getUserName() {
        return pref.getString(KEY_USER_NAME, "User");
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }
}