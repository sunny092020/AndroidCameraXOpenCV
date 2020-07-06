package com.ami.icamdocscanner.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.ami.icamdocscanner.R;

public class Preferences {
    public static final int CAPTURE_MODE_SINGLE = 1, CAPTURE_MODE_BATCH = 2;

    public static boolean getAutoCapture(Activity activity) {
        return getSharedPreferences(activity).getBoolean(activity.getString(R.string.auto_capture_key), false);
    }

    public static void setAutoCapture(Activity activity, boolean value) {
        SharedPreferences sharedPref  = getSharedPreferences(activity);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(activity.getString(R.string.auto_capture_key), value);
        editor.commit();
    }

    public static int getCaptureMode(Activity activity) {
        return getSharedPreferences(activity).getInt(activity.getString(R.string.capture_mode_key), CAPTURE_MODE_SINGLE);
    }

    public static void setCaptureMode(Activity activity, int value) {
        SharedPreferences sharedPref  = getSharedPreferences(activity);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(activity.getString(R.string.capture_mode_key), value);
        editor.commit();
    }

    private static SharedPreferences getSharedPreferences(Activity activity) {
        return activity.getSharedPreferences(
                activity.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
    }
}
