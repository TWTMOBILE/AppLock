package com.example.applock;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class AllAppsFragment extends Fragment {

    private static final int REQUEST_PIN_CODE = 1234;
    private View view;
    private ListView listView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private PinCodeManager pinCodeManager;
    private boolean isPinVerified = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_all_apps, container, false);
        listView = view.findViewById(R.id.listView);
        swipeRefreshLayout = view.findViewById(R.id.swipeToRefresh);
        listView.setTextFilterEnabled(true);

        pinCodeManager = new PinCodeManager(requireContext());

        swipeRefreshLayout.setOnRefreshListener(this::refreshIt);

        // Check if the pin code is set, if not, prompt user to set it
        if (!pinCodeManager.isPinCodeSet()) {
            setPinCode();
        } else if (!isPinVerified) {
            checkPinCode();
        } else {
            // Continue with app loading
            refreshIt();
        }

        return view;
    }

    private void setPinCode() {
        // Launch activity to set PIN code
        Intent intent = new Intent(requireContext(), SetPinCodeActivity.class);
        startActivityForResult(intent, REQUEST_PIN_CODE);
    }

    private void checkPinCode() {
        // Launch activity to enter PIN code
        Intent intent = new Intent(requireContext(), EnterPinCodeActivity.class);
        startActivityForResult(intent, REQUEST_PIN_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PIN_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                // PIN code successfully entered or set
                String pinCode = data.getStringExtra("pinCode");
                if (pinCode != null && !pinCode.isEmpty()) {
                    pinCodeManager.savePinCode(pinCode);
                }
                isPinVerified = true;
                // Continue with app loading
                refreshIt();
            } else {
                // Handle if PIN code not entered correctly or cancelled
                // For example, you might show a message or take appropriate action
            }
        }
    }

    private void refreshIt() {
        LoadAppInfoTask loadAppInfoTask = new LoadAppInfoTask();
        loadAppInfoTask.execute();
    }

    private class LoadAppInfoTask extends AsyncTask<Void, Void, List<AppInfo>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            swipeRefreshLayout.setRefreshing(true);
        }

        @Override
        protected List<AppInfo> doInBackground(Void... voids) {
            List<AppInfo> apps = new ArrayList<>();
            PackageManager packageManager = requireActivity().getPackageManager();
            List<ApplicationInfo> infos = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

            for (ApplicationInfo info : infos) {
                try {
                    // Check if the app is launchable
                    Intent launchIntent = packageManager.getLaunchIntentForPackage(info.packageName);
                    if (launchIntent != null) {
                        // Retrieve app label and icon
                        CharSequence label = info.loadLabel(packageManager);
                        Drawable icon = info.loadIcon(packageManager);

                        // Create AppInfo object
                        AppInfo app = new AppInfo();
                        app.info = info;
                        app.label = label != null ? label.toString() : info.packageName; // Use package name if label is null
                        app.icon = icon;
                        apps.add(app);
                    }
                } catch (Exception e) {
                    // Handle exceptions, such as SecurityExceptions due to package visibility restrictions
                    Log.e("LoadAppInfoTask", "Error loading app info: " + e.getMessage());
                }
            }

            return apps;
        }

        @Override
        protected void onPostExecute(List<AppInfo> appInfos) {
            super.onPostExecute(appInfos);
            listView.setAdapter(new AppAdapter(requireContext(), appInfos));
            swipeRefreshLayout.setRefreshing(false);
            Snackbar.make(listView, appInfos.size() + " launchable applications loaded", Snackbar.LENGTH_LONG).show();
        }
    }
}
