package com.example.applock;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;

import java.util.List;

public class AppAdapter extends ArrayAdapter<AppInfo> {

    private static final String TAG = "AppAdapter";
    private final LayoutInflater layoutInflater;
    private final PackageManager packageManager;
    private final List<AppInfo> apps;
    private final PinCodeManager pinCodeManager;
    private final Context context;
    private boolean isSwitchProgrammatic = false;

    public AppAdapter(Context context, List<AppInfo> apps) {
        super(context, R.layout.app_item_layout, apps);
        layoutInflater = LayoutInflater.from(context);
        packageManager = context.getPackageManager();
        this.apps = apps;
        this.context = context;
        this.pinCodeManager = new PinCodeManager(context);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.app_item_layout, parent, false);
            holder = new ViewHolder();
            holder.textViewTitle = convertView.findViewById(R.id.titleTextView);
            holder.textVersion = convertView.findViewById(R.id.versionId);
            holder.imageView = convertView.findViewById(R.id.iconImage);
            holder.switchCompat = convertView.findViewById(R.id.switchCompat);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        AppInfo current = apps.get(position);

        holder.textViewTitle.setText(current.label);

        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(current.info.packageName, 0);
            String versionInfo = packageInfo.versionName;
            holder.textVersion.setText(TextUtils.isEmpty(versionInfo) ? "N/A" : versionInfo);
        } catch (PackageManager.NameNotFoundException e) {
            holder.textVersion.setText("N/A");
        }

        Drawable icon = current.info.loadIcon(packageManager);
        holder.imageView.setImageDrawable(icon);

        // Remove any previous listener to avoid multiple calls
        holder.switchCompat.setOnCheckedChangeListener(null);

        // Set the switch state based on saved preferences
        boolean isLocked = pinCodeManager.isAppLocked(current.info.packageName);
        holder.switchCompat.setChecked(isLocked);
        Log.d(TAG, "Package: " + current.info.packageName + " is locked: " + isLocked);

        // Add switch change listener
        holder.switchCompat.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isSwitchProgrammatic) {
                return;
            }
            isSwitchProgrammatic = true;
            if (isChecked) {
                pinCodeManager.lockApp(current.info.packageName);
                Log.d(TAG, "Locked app: " + current.info.packageName);
            } else {
                pinCodeManager.unlockApp(current.info.packageName);
                Log.d(TAG, "Unlocked app: " + current.info.packageName);
            }
            isSwitchProgrammatic = false;
        });

        return convertView;
    }

    static class ViewHolder {
        TextView textViewTitle;
        TextView textVersion;
        ImageView imageView;
        SwitchCompat switchCompat;
    }
}
