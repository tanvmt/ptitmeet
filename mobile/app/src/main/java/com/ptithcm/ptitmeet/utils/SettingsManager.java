package com.ptithcm.ptitmeet.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {
    private static final String PREF_NAME = "PTITMeetSettings";
    private static final String KEY_CHAT_NOTIF = "ptitmeet_chatNotif";
    private static final String KEY_JOIN_LEAVE_NOTIF = "ptitmeet_joinLeaveNotif";
    private static final String KEY_RAISE_HAND_NOTIF = "ptitmeet_raiseHandNotif";
    private static final String KEY_REMINDER_NOTIF = "ptitmeet_reminderNotif";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isChatNotifEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_CHAT_NOTIF, true);
    }

    public static void setChatNotifEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_CHAT_NOTIF, enabled).apply();
    }

    public static boolean isJoinLeaveNotifEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_JOIN_LEAVE_NOTIF, true);
    }

    public static void setJoinLeaveNotifEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_JOIN_LEAVE_NOTIF, enabled).apply();
    }

    public static boolean isRaiseHandNotifEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_RAISE_HAND_NOTIF, true);
    }

    public static void setRaiseHandNotifEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_RAISE_HAND_NOTIF, enabled).apply();
    }

    public static boolean isReminderNotifEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_REMINDER_NOTIF, true);
    }

    public static void setReminderNotifEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_REMINDER_NOTIF, enabled).apply();
    }
}
