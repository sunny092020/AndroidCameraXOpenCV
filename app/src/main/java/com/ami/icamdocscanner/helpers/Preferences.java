package com.ami.icamdocscanner.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.ami.icamdocscanner.R;
import com.ami.icamdocscanner.models.OcrLanguage;

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
        return getSharedPreferences(activity).getBoolean(activity.getString(R.string.is_crop_after_each_capture_key), false);
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

    public static boolean getLangUsed(Activity activity, String lang) {
        return getSharedPreferences(activity).getBoolean(lang, false);
    }

    public static void setlangUsed(Activity activity, String lang, boolean value) {
        SharedPreferences sharedPref  = getSharedPreferences(activity);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(lang, value);
        editor.commit();
    }

    public static String getUsedLangs(Activity activity) {
        String result = "";
        for(int i=0; i<ScannerConstant.LANGS.length; i++) {
            String lang = ScannerConstant.LANGS[i][0];
            //lang, name
            if(getLangUsed(activity, lang)) {
                if(i>0) result = result + "+";
                result += lang;
            }
        }
        return result;
    }
}
