package com.example.applock;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PinCodeManager {

    private static final String PREF_PIN_CODE_KEY = "pin_code";

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
}
