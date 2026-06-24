package com.ptithcm.ptitmeet.api;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "PTITMeetSession";
    private static final String KEY_TOKEN = "accessToken";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_NAME = "fullName";
    private static final String KEY_AVATAR_URL = "avatarUrl";

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void saveAuthData(String token, String userId, String fullName, String avatarUrl) {
        editor.putString(KEY_TOKEN, token);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_NAME, fullName);
        editor.putString(KEY_AVATAR_URL, avatarUrl);
        editor.apply();
    }

    public String getToken() {
        return pref.getString(KEY_TOKEN, null);
    }

    public String getUserId() {
        return pref.getString(KEY_USER_ID, null);
    }

    public String getUserName() {
        return pref.getString(KEY_USER_NAME, "User");
    }

    public String getAvatarUrl() {
        return pref.getString(KEY_AVATAR_URL, null);
    }

    public void updateUserName(String fullName) {
        editor.putString(KEY_USER_NAME, fullName);
        editor.apply();
    }

    public void updateAvatarUrl(String avatarUrl) {
        editor.putString(KEY_AVATAR_URL, avatarUrl);
        editor.apply();
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }
}
