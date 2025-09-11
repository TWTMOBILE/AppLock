package com.example.applock;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class AppLockService extends AccessibilityService {
    private static final String TAG = "AppLockService";
    public static final String ACTION_UNLOCK = "com.example.applock.ACTION_UNLOCK";

    private PinCodeManager pinCodeManager;
    private boolean lockUiShowing = false;
    private String lastPromptedPkg = null;

    private final Handler handler = new Handler();
    private long lastPromptTs = 0L;
    private static final long PROMPT_DEBOUNCE_MS = 300L;


    private final BroadcastReceiver screenOffReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            if (Intent.ACTION_SCREEN_OFF.equals(i.getAction())) {
                pinCodeManager.clearAllTempUnlocks();
                lockUiShowing = false;
                lastPromptedPkg = null;
                Log.d(TAG, "Screen off -> cleared all temp unlocks");
            }
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        pinCodeManager = new PinCodeManager(this);
        registerReceiver(unlockReceiver, new IntentFilter(ACTION_UNLOCK));

        // Screen off -> revoke temp unlocks
        IntentFilter f = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenOffReceiver, f);

        // Foreground keep-alive
        Intent serviceIntent = new Intent(this, AppLockForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(serviceIntent);
        else startService(serviceIntent);
    }

    @Override public void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(unlockReceiver); } catch (Exception ignored) {}
        try { unregisterReceiver(screenOffReceiver); } catch (Exception ignored) {}
        stopService(new Intent(this, AppLockForegroundService.class));
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int type = event.getEventType();
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return;

        CharSequence pkgCs = event.getPackageName();
        if (pkgCs == null) return;

        String currentPkg = pkgCs.toString();

        // 1) Skip our own app to avoid self-lock or churn
        if (currentPkg.equals(getPackageName())) return;

        // 2) If lock UI already showing for this exact target, chill
        if (lockUiShowing && currentPkg.equals(lastPromptedPkg)) return;

        // 3) If we switched away from the last prompted app, revoke its temp unlock
        if (lastPromptedPkg != null && !currentPkg.equals(lastPromptedPkg)) {
            pinCodeManager.resetTempUnlock(lastPromptedPkg);
            lastPromptedPkg = null;
        }

        // 4) Check if target package is locked
        if (!pinCodeManager.isAppLocked(currentPkg)) return;

        // 5) Respect TTL-based temporary unlocks
        if (pinCodeManager.isAppTemporarilyUnlocked(currentPkg)) return;
        long now = System.currentTimeMillis();
        if (now - lastPromptTs < PROMPT_DEBOUNCE_MS && currentPkg.equals(lastPromptedPkg)) return;
        lastPromptTs = now;

        Log.d(TAG, "Locked app detected: " + currentPkg);

        lockUiShowing = true;
        lastPromptedPkg = currentPkg;

        // 6) Small delay so target window fully appears (OEM stability)
        handler.postDelayed(() -> {
            // Double-check it wasn't unlocked in the meantime
            if (!pinCodeManager.isAppTemporarilyUnlocked(currentPkg)) {
                Intent i = new Intent(AppLockService.this, EnterPinCodeActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        | Intent.FLAG_ACTIVITY_NO_HISTORY);
                i.putExtra("locked_package_name", currentPkg);
                startActivity(i);
            }
        }, 120);
    }

    @Override
    public void onInterrupt() { /* no-op */ }

    private final BroadcastReceiver unlockReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (!ACTION_UNLOCK.equals(intent.getAction())) return;
            lockUiShowing = false;
            // Keep lastPromptedPkg so we don't immediately re-trigger for the same app
            Log.d(TAG, "Received ACTION_UNLOCK for " + intent.getStringExtra("pkg"));
        }
    };
}
