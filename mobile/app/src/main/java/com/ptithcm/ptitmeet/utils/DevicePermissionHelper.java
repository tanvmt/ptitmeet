package com.ptithcm.ptitmeet.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

public final class DevicePermissionHelper {

    public static final String REQUEST_MIC = "REQUEST_MIC";
    public static final String REQUEST_CAMERA = "REQUEST_CAMERA";

    private DevicePermissionHelper() {
    }

    public static boolean hasMicrophonePermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasCameraPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }
}
