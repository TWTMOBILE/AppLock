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
            holder.textVersion   = convertView.findViewById(R.id.versionId); // make sure this id exists
            holder.imageView     = convertView.findViewById(R.id.iconImage);
            holder.switchCompat  = convertView.findViewById(R.id.switchCompat);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        AppInfo current = apps.get(position);

        // Icon
        holder.imageView.setImageDrawable(current.info.loadIcon(packageManager));

        // App name
        CharSequence label = current.info.loadLabel(packageManager);
        holder.textViewTitle.setText(TextUtils.isEmpty(label) ? current.info.packageName : label);

        // Version (optional)
        String versionText = "";
        try {
            PackageInfo pi;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                pi = packageManager.getPackageInfo(
                        current.info.packageName,
                        PackageManager.PackageInfoFlags.of(0)
                );
            } else {
                pi = packageManager.getPackageInfo(current.info.packageName, 0);
            }
            versionText = pi.versionName != null ? "v" + pi.versionName : "";
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "getPackageInfo failed for " + current.info.packageName, e);
        }
        if (holder.textVersion != null) holder.textVersion.setText(versionText);

        // Switch (your existing logic)
        holder.switchCompat.setOnCheckedChangeListener(null);
        boolean isLocked = pinCodeManager.isAppLocked(current.info.packageName);
        holder.switchCompat.setChecked(isLocked);
        holder.switchCompat.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isSwitchProgrammatic) return;
            isSwitchProgrammatic = true;
            if (isChecked) {
                pinCodeManager.lockApp(current.info.packageName);
            } else {
                pinCodeManager.unlockApp(current.info.packageName);
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