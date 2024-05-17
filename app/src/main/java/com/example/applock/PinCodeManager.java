package com.example.applock;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.HashSet;
import java.util.Set;

public class PinCodeManager {

    private static final String PREF_PIN_CODE_KEY = "pin_code";
    private static final String PREF_LOCKED_APPS_KEY = "locked_apps";

    private SharedPreferences sharedPreferences;

    public PinCodeManager(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void savePinCode(String pinCode) {
        sharedPreferences.edit().putString(PREF_PIN_CODE_KEY, pinCode).apply();
    }

    public String getPinCode() {
        return sharedPreferences.getString(PREF_PIN_CODE_KEY, "");
    }

    public boolean isPinCodeSet() {
        return sharedPreferences.contains(PREF_PIN_CODE_KEY);
    }

    public boolean validatePinCode(String enteredPinCode) {
        String savedPinCode = getPinCode();
        return enteredPinCode.equals(savedPinCode);
    }

    public void lockApp(String packageName) {
        Set<String> lockedApps = sharedPreferences.getStringSet(PREF_LOCKED_APPS_KEY, new HashSet<>());
        lockedApps.add(packageName);
        sharedPreferences.edit().putStringSet(PREF_LOCKED_APPS_KEY, lockedApps).apply();
    }

    public void unlockApp(String packageName) {
        Set<String> lockedApps = sharedPreferences.getStringSet(PREF_LOCKED_APPS_KEY, new HashSet<>());
        lockedApps.remove(packageName);
        sharedPreferences.edit().putStringSet(PREF_LOCKED_APPS_KEY, lockedApps).apply();
    }

    public boolean isAppLocked(String packageName) {
        Set<String> lockedApps = sharedPreferences.getStringSet(PREF_LOCKED_APPS_KEY, new HashSet<>());
        return lockedApps.contains(packageName);
    }
}
