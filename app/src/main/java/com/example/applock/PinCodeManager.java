package com.example.applock;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.HashSet;
import java.util.Set;

public class PinCodeManager {

    private static final String PREF_PIN_CODE_KEY = "pin_code";
    private static final String PREF_LOCKED_APPS_KEY = "locked_apps";
    private static final String PREF_IS_APP_UNLOCKED_KEY = "is_app_unlocked";
    private static final String PREF_UNLOCKED_APP_PACKAGE_KEY = "unlocked_app_package";
    private static final String PREF_UNLOCK_TIMESTAMP_KEY = "unlocked_app_timestamp";
    private static final long UNLOCK_TIMEOUT_MS = 30 * 1000; // 30 seconds

    private final SharedPreferences sharedPreferences;

    public PinCodeManager(Context context) {
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    // --- PIN ---
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

    // --- Locked apps list ---
    public void lockApp(String packageName) {
        Set<String> current = sharedPreferences.getStringSet(PREF_LOCKED_APPS_KEY, new HashSet<>());
        Set<String> copy = new HashSet<>(current);
        copy.add(packageName);
        sharedPreferences.edit().putStringSet(PREF_LOCKED_APPS_KEY, copy).apply();
    }

    public void unlockApp(String packageName) {
        Set<String> current = sharedPreferences.getStringSet(PREF_LOCKED_APPS_KEY, new HashSet<>());
        Set<String> copy = new HashSet<>(current);
        copy.remove(packageName);
        sharedPreferences.edit().putStringSet(PREF_LOCKED_APPS_KEY, copy).apply();
    }

    public boolean isAppLocked(String packageName) {
        Set<String> current = sharedPreferences.getStringSet(PREF_LOCKED_APPS_KEY, new HashSet<>());
        return current.contains(packageName);
    }

    // --- Temporary unlock state for a single package ---
    public void markAppUnlocked(String packageName) {
        long now = System.currentTimeMillis();
        sharedPreferences.edit()
                .putBoolean(PREF_IS_APP_UNLOCKED_KEY, true)
                .putString(PREF_UNLOCKED_APP_PACKAGE_KEY, packageName)
                .putLong(PREF_UNLOCK_TIMESTAMP_KEY, now)
                .apply();
    }

    public boolean isAppUnlocked() {
        boolean unlocked = sharedPreferences.getBoolean(PREF_IS_APP_UNLOCKED_KEY, false);
        if (!unlocked) return false;
        long ts = sharedPreferences.getLong(PREF_UNLOCK_TIMESTAMP_KEY, 0);
        long now = System.currentTimeMillis();
        if (now - ts > UNLOCK_TIMEOUT_MS) {
            resetUnlockState();
            return false;
        }
        return true;
    }

    public String getUnlockedAppPackage() {
        return sharedPreferences.getString(PREF_UNLOCKED_APP_PACKAGE_KEY, "");
    }

    public void resetUnlockState() {
        sharedPreferences.edit()
                .putBoolean(PREF_IS_APP_UNLOCKED_KEY, false)
                .remove(PREF_UNLOCKED_APP_PACKAGE_KEY)
                .remove(PREF_UNLOCK_TIMESTAMP_KEY)
                .apply();
    }
}
