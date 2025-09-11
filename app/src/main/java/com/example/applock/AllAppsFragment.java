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

    private LoadAppInfoTask currentTask;
    private AppAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_all_apps, container, false);
        listView = view.findViewById(R.id.listView);
        swipeRefreshLayout = view.findViewById(R.id.swipeToRefresh);
        listView.setTextFilterEnabled(true);

        // Optional: default Android color set
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
        );

        pinCodeManager = new PinCodeManager(requireContext());

        // Pull-to-refresh listener
        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshIt();
        });

        // Initial load
        refreshIt();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Cancel any running task to avoid leaks/crashes
        if (currentTask != null) {
            currentTask.cancel(true);
            currentTask = null;
        }
    }

    private void setPinCode() {
        Intent intent = new Intent(requireContext(), SetPinCodeActivity.class);
        startActivityForResult(intent, REQUEST_PIN_CODE);
    }

    private void checkPinCode() {
        Intent intent = new Intent(requireContext(), EnterPinCodeActivity.class);
        startActivityForResult(intent, REQUEST_PIN_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PIN_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                String pinCode = data.getStringExtra("pinCode");
                if (pinCode != null && !pinCode.isEmpty()) {
                    pinCodeManager.savePinCode(pinCode);
                }
                isPinVerified = true;
                refreshIt();
            } else {
                // Optionally notify user or close fragment
                Snackbar.make(listView, "PIN required to proceed", Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private void refreshIt() {
        if (currentTask != null) {
            currentTask.cancel(true);
        }
        currentTask = new LoadAppInfoTask();
        currentTask.execute();
    }

    private class LoadAppInfoTask extends AsyncTask<Void, Void, List<AppInfo>> {
        @Override
        protected List<AppInfo> doInBackground(Void... voids) {
            PackageManager pm = requireContext().getPackageManager();
            List<AppInfo> apps = new ArrayList<>();

            List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            for (ApplicationInfo packageInfo : packages) {
                // Skip system apps unless they're on sdcard
                if ((packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                        && (packageInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0) {
                    continue;
                }

                // Skip our own app
                if (packageInfo.packageName.equals(requireContext().getPackageName())) {
                    continue;
                }

                AppInfo appInfo = new AppInfo();
                appInfo.info = packageInfo;
                appInfo.isLocked = pinCodeManager.isAppLocked(packageInfo.packageName);
                apps.add(appInfo);
            }

            return apps;
        }

        @Override
        protected void onPostExecute(List<AppInfo> appInfos) {
            if (isAdded()) {
                adapter = new AppAdapter(requireContext(), appInfos);
                listView.setAdapter(adapter);
                swipeRefreshLayout.setRefreshing(false);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh list to update lock states
        refreshIt();
    }
}
