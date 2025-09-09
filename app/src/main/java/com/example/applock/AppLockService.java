package com.example.applock;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

public class AppLockService extends AccessibilityService {

    private static final String TAG = "AppLockService";
    public static final String ACTION_UNLOCK = "com.example.applock.ACTION_UNLOCK";

    private PinCodeManager pinCodeManager;
    private boolean lockUiShowing = false;
    private String lastPromptedPkg = null;

    private final Handler handler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
        pinCodeManager = new PinCodeManager(this);
        registerReceiver(unlockReceiver, new IntentFilter(ACTION_UNLOCK));

        // Keep-alive foreground service (optional but helps OEMs)
        Intent serviceIntent = new Intent(this, AppLockForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int type = event.getEventType();

        // Only process window state changes for better reliability
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }

        CharSequence pkgCs = event.getPackageName();
        if (pkgCs == null) return;

        String currentPkg = pkgCs.toString();
        Log.d(TAG, "Window changed: " + currentPkg);

        // Check if this is a root window change (actual app switch)
        if (event.getWindowId() != -1 && !isRootWindow(event)) {
            Log.d(TAG, "Not a root window, ignoring");
            return;
        }

        // 1) Skip our own app to avoid self-lock
        if (currentPkg.equals(getPackageName())) {
            Log.d(TAG, "Our own app, skipping");
            lockUiShowing = false;
            return;
        }

        // 2) Check if this app should be locked
        if (!pinCodeManager.isAppLocked(currentPkg)) {
            Log.d(TAG, "App not locked: " + currentPkg);
            lockUiShowing = false;
            return;
        }

        // 3) Check if already showing lock UI for this app
        if (lockUiShowing && currentPkg.equals(lastPromptedPkg)) {
            Log.d(TAG, "Lock UI already showing for: " + currentPkg);
            return;
        }

        // 4) Check if app is temporarily unlocked
        if (pinCodeManager.isAppUnlocked() && currentPkg.equals(pinCodeManager.getUnlockedAppPackage())) {
            Log.d(TAG, "App already unlocked: " + currentPkg);
            return;
        }

        // 5) Show lock screen
        Log.d(TAG, "Launching lock screen for: " + currentPkg);
        lockUiShowing = true;
        lastPromptedPkg = currentPkg;

        handler.postDelayed(() -> {
            // Double check it wasn't unlocked during the delay
            if (!pinCodeManager.isAppUnlocked() || !currentPkg.equals(pinCodeManager.getUnlockedAppPackage())) {
                Intent i = new Intent(AppLockService.this, EnterPinCodeActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        | Intent.FLAG_ACTIVITY_NO_HISTORY
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                i.putExtra("locked_package_name", currentPkg);
                startActivity(i);
                Log.d(TAG, "Lock screen launched for: " + currentPkg);
            } else {
                Log.d(TAG, "App unlocked during delay, skipping lock");
                lockUiShowing = false;
            }
        }, 50); // Reduced delay for faster response
    }

    private boolean isRootWindow(AccessibilityEvent event) {
        return event.getSource() != null &&
                event.getSource().getParent() == null;
    }

    @Override
    public void onInterrupt() { /* no-op */ }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(unlockReceiver);
        } catch (Exception ignored) {}
        stopService(new Intent(this, AppLockForegroundService.class));
        handler.removeCallbacksAndMessages(null);
    }

    private final BroadcastReceiver unlockReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (!ACTION_UNLOCK.equals(intent.getAction())) return;
            lockUiShowing = false;
            // Keep lastPromptedPkg so we don’t immediately re-trigger for the same app
            Log.d(TAG, "Received ACTION_UNLOCK for " + pinCodeManager.getUnlockedAppPackage());
        }
    };
}
