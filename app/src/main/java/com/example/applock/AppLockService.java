package com.example.applock;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppLockService extends AccessibilityService {

    private static final String TAG = "AppLockService";
    public static final String ACTION_UNLOCK = "com.example.applock.ACTION_UNLOCK";
    private static final long CHECK_INTERVAL = 200; // More frequent checks

    private PinCodeManager pinCodeManager;
    private boolean lockUiShowing = false;
    private String lastPromptedPkg = null;
    private PowerManager.WakeLock wakeLock;
    private final Set<String> activePackages = new HashSet<>();
    private final Handler handler = new Handler();
    private boolean isScreenOn = true;

    private final Runnable packageChecker = new Runnable() {
        @Override
        public void run() {
            checkCurrentApp();
            handler.postDelayed(this, CHECK_INTERVAL);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        pinCodeManager = new PinCodeManager(this);

        // Get wake lock to keep service running
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AppLock:ServiceWakeLock");
        wakeLock.acquire();

        // Register receivers
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_UNLOCK);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mainReceiver, filter);

        // Start foreground service
        Intent serviceIntent = new Intent(this, AppLockForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // Start continuous monitoring
        handler.post(packageChecker);
    }

    private void checkCurrentApp() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> tasks = am.getRunningAppProcesses();

        if (tasks != null) {
            for (ActivityManager.RunningAppProcessInfo task : tasks) {
                if (task.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    String pkg = task.processName;
                    handlePackageCheck(pkg);
                }
            }
        }
    }

    private void handlePackageCheck(String pkg) {
        if (pkg == null || pkg.equals(getPackageName())) {
            return;
        }

        // Track active packages
        activePackages.add(pkg);

        // Check if this package should be locked
        if (!pinCodeManager.isAppLocked(pkg)) {
            return;
        }

        // If already showing lock UI for this app, skip
        if (lockUiShowing && pkg.equals(lastPromptedPkg)) {
            return;
        }

        // If app is temporarily unlocked, skip
        if (pinCodeManager.isAppUnlocked() && pkg.equals(pinCodeManager.getUnlockedAppPackage())) {
            return;
        }

        // Show lock screen immediately
        showLockScreen(pkg);
    }

    private void showLockScreen(String pkg) {
        lockUiShowing = true;
        lastPromptedPkg = pkg;

        Intent i = new Intent(this, EnterPinCodeActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                | Intent.FLAG_ACTIVITY_NO_HISTORY
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        i.putExtra("locked_package_name", pkg);
        startActivity(i);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int type = event.getEventType();

        // Monitor both window state and content changes
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            && type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            return;
        }

        CharSequence pkgCs = event.getPackageName();
        if (pkgCs != null) {
            handlePackageCheck(pkgCs.toString());
        }
    }

    @Override
    public void onInterrupt() {
        // Restart the service if interrupted
        Intent intent = new Intent(this, AppLockService.class);
        startService(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(mainReceiver);
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        } catch (Exception ignored) {}

        handler.removeCallbacks(packageChecker);

        // Try to restart ourselves
        Intent intent = new Intent(this, AppLockService.class);
        startService(intent);

        stopService(new Intent(this, AppLockForegroundService.class));
    }

    private final BroadcastReceiver mainReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            switch (action) {
                case ACTION_UNLOCK:
                    lockUiShowing = false;
                    break;
                case Intent.ACTION_SCREEN_ON:
                    isScreenOn = true;
                    // Recheck current app when screen turns on
                    checkCurrentApp();
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    isScreenOn = false;
                    // Clear unlocked state when screen turns off
                    pinCodeManager.resetUnlockState();
                    break;
            }
        }
    };
}
