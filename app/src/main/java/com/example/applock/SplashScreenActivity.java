package com.example.applock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

public class SplashScreenActivity extends Activity {
    private static final String TAG = "AppLock:Splash";
    private static final int REQUEST_OVERLAY = 1001;
    private static final int REQUEST_ACCESSIBILITY = 1002;
    private static final int PERMISSION_CHECK_DELAY = 500;
    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        // Start checking permissions with a slight delay
        handler.postDelayed(this::checkPermissionsAndStart, 500);
    }

    private void checkPermissionsAndStart() {
        Log.d(TAG, "Checking permissions...");
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission();
        } else if (!isAccessibilityServiceEnabled()) {
            requestAccessibilityPermission();
        } else {
            startAppComponents();
        }
    }

    private void requestOverlayPermission() {
        Log.d(TAG, "Requesting overlay permission");
        new AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("Please enable 'Draw over other apps' permission for the app to work properly.")
            .setPositiveButton("Enable", (dialog, which) -> {
                Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
                );
                startActivityForResult(intent, REQUEST_OVERLAY);
            })
            .setCancelable(false)
            .show();
    }

    private void requestAccessibilityPermission() {
        Log.d(TAG, "Requesting accessibility permission");
        new AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("Please enable Accessibility Service for app locking to work.\n\n1. Tap 'Enable'\n2. Find 'App Lock' in the list\n3. Turn it ON\n4. Return to App Lock")
            .setPositiveButton("Enable", (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivityForResult(intent, REQUEST_ACCESSIBILITY);
            })
            .setCancelable(false)
            .show();
    }

    private boolean isAccessibilityServiceEnabled() {
        String service = getPackageName() + "/" + AppLockService.class.getName();
        String enabled = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        boolean isEnabled = enabled != null && enabled.contains(service);
        Log.d(TAG, "Accessibility service enabled: " + isEnabled);
        return isEnabled;
    }

    private void startAppComponents() {
        Log.d(TAG, "Starting app components");

        // Start services
        startService(new Intent(this, AppLockService.class));
        startService(new Intent(this, AppLockForegroundService.class));

        // Request battery optimization exclusion
        if (!isIgnoringBatteryOptimizations()) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            try {
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Could not request battery optimization", e);
            }
        }

        // Continue to main activity
        handler.postDelayed(() -> {
            Log.d(TAG, "Starting MainActivity");
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }, 1000);
    }

    private boolean isIgnoringBatteryOptimizations() {
        try {
            String packageName = getPackageName();
            return Settings.System.getInt(getContentResolver(),
                "app_standby_enabled", 1) == 0 ||
                Settings.System.getString(getContentResolver(),
                "battery_saver_device_specific_enabled_list").contains(packageName);
        } catch (Exception e) {
            Log.e(TAG, "Error checking battery optimizations", e);
            return false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recheck permissions after returning from settings
        handler.postDelayed(this::checkPermissionsAndStart, PERMISSION_CHECK_DELAY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode);

        handler.postDelayed(() -> {
            if (requestCode == REQUEST_OVERLAY) {
                if (Settings.canDrawOverlays(this)) {
                    Log.d(TAG, "Overlay permission granted");
                    checkPermissionsAndStart();
                } else {
                    Log.w(TAG, "Overlay permission denied");
                    Toast.makeText(this, "Permission required for app to work properly",
                        Toast.LENGTH_LONG).show();
                    requestOverlayPermission();
                }
            } else if (requestCode == REQUEST_ACCESSIBILITY) {
                if (isAccessibilityServiceEnabled()) {
                    Log.d(TAG, "Accessibility permission granted");
                    checkPermissionsAndStart();
                } else {
                    Log.w(TAG, "Accessibility permission denied");
                    Toast.makeText(this, "Permission required for app to work properly",
                        Toast.LENGTH_LONG).show();
                    requestAccessibilityPermission();
                }
            }
        }, PERMISSION_CHECK_DELAY);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
