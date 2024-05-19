package com.example.applock;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    Button allAppsBtn, lockedAppsBtn, homeBtn, settingsBtn, notificationsBtn;
    ListView listView;
    SwipeRefreshLayout swipeRefreshLayout;
    boolean mIncludeSystemApps;

    private static final int REQUEST_CODE_OVERLAY_PERMISSION = 1001;
    private static final int REQUEST_CODE_DEVICE_ADMIN = 1002;

    private DevicePolicyManager devicePolicyManager;
    private ComponentName deviceAdminComponent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AllAppsFragment allAppsFragment = new AllAppsFragment();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                startActivity(intent);
            }
        }

        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        deviceAdminComponent = new ComponentName(this, AppDeviceAdminReceiver.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION);
            }
        }

        requestBatteryOptimizationPermission();
        requestDeviceAdminPermission();

        allAppsBtn = findViewById(R.id.allAppsBtn);
        lockedAppsBtn = findViewById(R.id.lockedAppsBtn);
        homeBtn = findViewById(R.id.homeBtn);
        settingsBtn = findViewById(R.id.settingsBtn);
        notificationsBtn= findViewById(R.id.notificationsBtn);

        allAppsBtn.setOnClickListener(view -> replaceFragment(new AllAppsFragment()));

        lockedAppsBtn.setOnClickListener(view -> replaceFragment(new LockedAppsFragment()));

        homeBtn.setOnClickListener(view -> replaceFragment(new AllAppsFragment()));

        settingsBtn.setOnClickListener(view -> replaceFragment(new SettingsFragment()));

        notificationsBtn.setOnClickListener(view -> replaceFragment(new NotificationFragment()));
    }

    private void requestDeviceAdminPermission() {
        if (!devicePolicyManager.isAdminActive(deviceAdminComponent)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdminComponent);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "App needs device admin permission to prevent uninstallation.");
            startActivityForResult(intent, REQUEST_CODE_DEVICE_ADMIN);
        }
    }

    private void requestBatteryOptimizationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Overlay permission is needed", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void replaceFragment(Fragment fragment){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainerView, fragment);
        fragmentTransaction.commit();
    }
}
