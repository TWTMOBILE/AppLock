package com.example.applock;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PinCodeManager {
    private static final String PREF_PIN_CODE_KEY = "pin_code";
    private static final String PREF_LOCKED_APPS_KEY = "locked_apps";

    // Per-app temp unlock expiry keys: "temp_unlock_until_<pkg>" -> long (epoch millis)
    private static final String TEMP_UNLOCK_PREFIX = "temp_unlock_until_";

    private final SharedPreferences sharedPreferences;

    public PinCodeManager(Context context) {
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(
                context.getApplicationContext());
    }

    // --- PIN ---
    public void savePinCode(String pinCode) {
        sharedPreferences.edit().putString(PREF_PIN_CODE_KEY, pinCode).apply();
    }
    public String getPinCode() { return sharedPreferences.getString(PREF_PIN_CODE_KEY, ""); }
    public boolean isPinCodeSet() { return sharedPreferences.contains(PREF_PIN_CODE_KEY); }
    public boolean validatePinCode(String enteredPinCode) {
        String savedPin = getPinCode();
        return enteredPinCode.equals(savedPin);
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
        resetTempUnlock(packageName);
    }
    public boolean isAppLocked(String packageName) {
        Set<String> current = sharedPreferences.getStringSet(PREF_LOCKED_APPS_KEY, new HashSet<>());
        return current.contains(packageName);
    }

    // --- Temporary unlocks with TTL ---
    private String k(String pkg) { return TEMP_UNLOCK_PREFIX + pkg; }

    /** Grant a temp unlock until now + graceMs (e.g., 10_000..60_000). */
    public void unlockAppTemporarily(String packageName, long graceMs) {
        long until = System.currentTimeMillis() + Math.max(0, graceMs);
        sharedPreferences.edit().putLong(k(packageName), until).apply();
    }

    /** Is temp unlock still valid (now < expiry)? */
    public boolean isAppTemporarilyUnlocked(String packageName) {
        long until = sharedPreferences.getLong(k(packageName), 0L);
        if (until <= 0L) return false;
        if (System.currentTimeMillis() < until) return true;
        // expired: clean up
        sharedPreferences.edit().remove(k(packageName)).apply();
        return false;
    }

    /** Revoke temp unlock for a specific app. */
    public void resetTempUnlock(String packageName) {
        sharedPreferences.edit().remove(k(packageName)).apply();
    }

    /** Revoke all temp unlocks (best-effort). Call on SCREEN_OFF. */
    public void clearAllTempUnlocks() {
        // Best-effort sweep: iterate over all keys and remove our prefix
        Map<String, ?> all = sharedPreferences.getAll();
        SharedPreferences.Editor ed = sharedPreferences.edit();
        for (String key : all.keySet()) {
            if (key != null && key.startsWith(TEMP_UNLOCK_PREFIX)) {
                ed.remove(key);
            }
        }
        ed.apply();
    }
}
