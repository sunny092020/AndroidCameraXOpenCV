package com.ami.icamdocscanner.activities;

import android.os.Bundle;
import android.util.Log;

import com.ami.icamdocscanner.R;
import com.ami.icamdocscanner.helpers.Preferences;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsActivity extends AppCompatActivity implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        return true;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {}

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.layout.preferences, rootKey);
            CheckBoxPreference autoCrop = findPreference(getString(R.string.is_crop_after_each_capture_key));
            autoCrop.setOnPreferenceChangeListener((preference, newValue) -> {
                Preferences.setIsCropAfterEachCapture(this.getActivity(), (Boolean) newValue);
                return true;
            });
        }
    }
}