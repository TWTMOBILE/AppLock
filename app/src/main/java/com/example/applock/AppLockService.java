package com.example.applock;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class AppLockService extends AccessibilityService {

    private static final String TAG = "AppLockService";
    public static final String ACTION_UNLOCK = "com.example.applock.ACTION_UNLOCK";
    private PinCodeManager pinCodeManager;
    private boolean isLockedActivityOpen = false;
    private String currentLockedApp = null;
    private Handler handler = new Handler();
    private Runnable checkAppRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        pinCodeManager = new PinCodeManager(this);
        registerReceiver(unlockReceiver, new IntentFilter(ACTION_UNLOCK));

        // Start the foreground service
        Intent serviceIntent = new Intent(this, AppLockForegroundService.class);
        startForegroundService(serviceIntent);

        // Initialize the periodic check runnable
        checkAppRunnable = new Runnable() {
            @Override
            public void run() {
                checkCurrentApp();
                handler.postDelayed(this, 1000); // Adjust the delay as necessary
            }
        };
        handler.post(checkAppRunnable);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            checkCurrentApp();
        }
    }

    @Override
    public void onInterrupt() {
        // Handle interruptions
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(unlockReceiver);

        // Stop the foreground service
        Intent serviceIntent = new Intent(this, AppLockForegroundService.class);
        stopService(serviceIntent);

        // Stop the periodic check runnable
        handler.removeCallbacks(checkAppRunnable);
    }

    private void checkCurrentApp() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode != null) {
            CharSequence packageName = rootNode.getPackageName();
            if (packageName != null) {
                String currentPackageName = packageName.toString();
                Log.d(TAG, "Detected package: " + currentPackageName);

                if (pinCodeManager.isAppLocked(currentPackageName)) {
                    if (!pinCodeManager.isAppUnlocked() || !currentPackageName.equals(pinCodeManager.getUnlockedAppPackage())) {
                        if (!isLockedActivityOpen || !currentPackageName.equals(currentLockedApp)) {
                            Log.d(TAG, "Package is locked: " + currentPackageName);
                            isLockedActivityOpen = true;
                            currentLockedApp = currentPackageName;

                            // Adding a small delay to ensure the locked activity is opened properly
                            handler.postDelayed(() -> {
                                if (!pinCodeManager.isAppUnlocked() || !currentPackageName.equals(pinCodeManager.getUnlockedAppPackage())) {
                                    Intent intent = new Intent(AppLockService.this, EnterPinCodeActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.putExtra("locked_package_name", currentPackageName);
                                    startActivity(intent);
                                }
                            }, 500);
                        }
                    }
                } else {
                    isLockedActivityOpen = false;
                }

                // Reset unlock state if the current app is different from the unlocked app
                if (!currentPackageName.equals(pinCodeManager.getUnlockedAppPackage())) {
                    pinCodeManager.resetUnlockState();
                }
            }
        }
    }

    private final BroadcastReceiver unlockReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_UNLOCK.equals(intent.getAction())) {
                isLockedActivityOpen = false;
                String lockedPackageName = pinCodeManager.getUnlockedAppPackage();
                Log.d(TAG, "Unlocked app: " + lockedPackageName);
            }
        }
    };

    public void setLockedActivityOpen(boolean isOpen) {
        this.isLockedActivityOpen = isOpen;
    }
}
