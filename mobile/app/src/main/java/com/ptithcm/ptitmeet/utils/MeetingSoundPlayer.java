package com.ptithcm.ptitmeet.utils;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;

public class MeetingSoundPlayer {

    private static ToneGenerator toneGenerator;

    private static synchronized ToneGenerator getToneGenerator() {
        if (toneGenerator == null) {
            try {
                // Use STREAM_MUSIC with 50% volume for standard sound effect play
                toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 50);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return toneGenerator;
    }

    public static void playChatSound(Context context) {
        if (SettingsManager.isChatNotifEnabled(context)) {
            // Play a standard double beep or high pitch confirmation beep
            playTone(ToneGenerator.TONE_PROP_ACK);
        }
    }

    public static void playJoinLeaveSound(Context context) {
        if (SettingsManager.isJoinLeaveNotifEnabled(context)) {
            // Play a prompt/alert sound
            playTone(ToneGenerator.TONE_PROP_BEEP);
        }
    }

    public static void playHandRaiseSound(Context context) {
        if (SettingsManager.isRaiseHandNotifEnabled(context)) {
            // Play a hand raise chime
            playTone(ToneGenerator.TONE_PROP_PROMPT);
        }
    }

    private static void playTone(int toneType) {
        new Thread(() -> {
            try {
                ToneGenerator tg = getToneGenerator();
                if (tg != null) {
                    tg.startTone(toneType, 150); // Play for 150 milliseconds
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static synchronized void release() {
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
    }
}
