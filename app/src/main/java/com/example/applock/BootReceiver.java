package com.example.applock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {

            // Start our services
            Intent serviceIntent = new Intent(context, AppLockService.class);
            serviceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            Intent fgService = new Intent(context, AppLockForegroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(fgService);
            } else {
                context.startService(fgService);
            }

            // Force accessibility service to be enabled
            String services = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (services == null) services = "";

            String ourService = context.getPackageName() + "/" + AppLockService.class.getName();
            if (!services.contains(ourService)) {
                services = services + ":" + ourService;
                Settings.Secure.putString(context.getContentResolver(),
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, services);
                Settings.Secure.putString(context.getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_ENABLED, "1");
            }
        }
    }
}
