package com.ami.icamdocscanner.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.ami.icamdocscanner.R;

public class Preferences {

    public static boolean getAutoCapture(Activity activity) {
        return getSharedPreferences(activity).getBoolean(activity.getString(R.string.auto_capture_key), false);
    }

    public static void setAutoCapture(Activity activity, boolean value) {
        SharedPreferences sharedPref  = getSharedPreferences(activity);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(activity.getString(R.string.auto_capture_key), value);
        editor.commit();
    }

    public static boolean getIsCropAfterEachCapture(Activity activity) {
        return getSharedPreferences(activity).getBoolean(activity.getString(R.string.is_crop_after_each_capture_key), true);
    }

    public static void setIsCropAfterEachCapture(Activity activity, boolean value) {
        SharedPreferences sharedPref  = getSharedPreferences(activity);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(activity.getString(R.string.is_crop_after_each_capture_key), value);
        editor.commit();
    }

    private static SharedPreferences getSharedPreferences(Activity activity) {
        return activity.getSharedPreferences(
                activity.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
    }
}
