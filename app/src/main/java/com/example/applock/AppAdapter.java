package com.example.applock;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
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

    private final LayoutInflater layoutInflater;
    private final PackageManager packageManager;
    private final List<AppInfo> apps;

    public AppAdapter(Context context, List<AppInfo> apps) {
        super(context, R.layout.app_item_layout, apps);
        layoutInflater = LayoutInflater.from(context);
        packageManager = context.getPackageManager();
        this.apps = apps;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        AppInfo current = apps.get(position);
        View view = convertView;
        if (view == null) {
            view = layoutInflater.inflate(R.layout.app_item_layout, parent, false);
        }

        TextView textViewTitle = view.findViewById(R.id.titleTextView);
        TextView textVersion = view.findViewById(R.id.versionId);
        ImageView imageView = view.findViewById(R.id.iconImage);
        SwitchCompat switchCompat = view.findViewById(R.id.switchCompat);

        textViewTitle.setText(current.label);

        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(current.info.packageName, 0);
            String versionInfo = packageInfo.versionName;
            if (!TextUtils.isEmpty(versionInfo)) {
                textVersion.setText(versionInfo);
            } else {
                textVersion.setText("N/A");
            }
        } catch (PackageManager.NameNotFoundException e) {
            textVersion.setText("N/A");
        }

        Drawable icon = current.info.loadIcon(packageManager);
        imageView.setImageDrawable(icon);

        // Set up switch if needed (add click listener or logic to handle switch state)

        return view;
    }
}
