package com.example.applock;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class AppLockService extends AccessibilityService {

    private static final String TAG = "AppLockService";
    private PinCodeManager pinCodeManager;

    @Override
    public void onCreate() {
        super.onCreate();
        pinCodeManager = new PinCodeManager(this);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String packageName = event.getPackageName().toString();
            Log.d(TAG, "Detected package: " + packageName);
            if (pinCodeManager.isAppLocked(packageName)) {
                Log.d(TAG, "Package is locked: " + packageName);
                Intent intent = new Intent(this, EnterPinCodeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        }
    }

    @Override
    public void onInterrupt() {
        // Handle interruptions if needed
    }
}
