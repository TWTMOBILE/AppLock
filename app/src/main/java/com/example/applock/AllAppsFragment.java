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
            // If PIN isn’t set/verified yet, route to PIN and stop spinner
            if (!pinCodeManager.isPinCodeSet()) {
                swipeRefreshLayout.setRefreshing(false);
                setPinCode();
                return;
            }
            if (!isPinVerified) {
                swipeRefreshLayout.setRefreshing(false);
                checkPinCode();
                return;
            }
            refreshIt();
        });

        // Initial flow
        if (!pinCodeManager.isPinCodeSet()) {
            setPinCode();
        } else if (!isPinVerified) {
            checkPinCode();
            refreshIt(); // kick an initial load; if PIN screen overlays, that’s fine
        } else {
            refreshIt();
        }

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
        // Ensure single loader
        if (currentTask != null) {
            currentTask.cancel(true);
            currentTask = null;
        }
        currentTask = new LoadAppInfoTask();
        currentTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private class LoadAppInfoTask extends AsyncTask<Void, Void, List<AppInfo>> {

        private Exception error;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);
        }

        @Override
        protected List<AppInfo> doInBackground(Void... voids) {
            List<AppInfo> apps = new ArrayList<>();
            try {
                PackageManager packageManager = requireContext().getPackageManager();
                List<ApplicationInfo> infos =
                        packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

                for (ApplicationInfo info : infos) {
                    if (isCancelled()) break;

                    try {
                        Intent launchIntent = packageManager.getLaunchIntentForPackage(info.packageName);
                        if (launchIntent != null) {
                            // Build AppInfo object as per your model
                            Drawable icon = info.loadIcon(packageManager);
                            AppInfo app = new AppInfo();
                            app.info = info;
                            app.icon = icon;
                            apps.add(app);
                        }
                    } catch (Exception inner) {
                        Log.e("LoadAppInfoTask", "Error loading app: " + info.packageName + " -> " + inner.getMessage());
                    }
                }
            } catch (Exception e) {
                error = e;
            }
            return apps;
        }

        @Override
        protected void onPostExecute(List<AppInfo> appInfos) {
            super.onPostExecute(appInfos);
            if (!isAdded()) return;

            if (adapter == null) {
                adapter = new AppAdapter(requireContext(), appInfos);
                listView.setAdapter(adapter);
            } else {
                adapter.clear();
                adapter.addAll(appInfos);
                adapter.notifyDataSetChanged();
            }

            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);

            if (error != null) {
                Snackbar.make(listView, "Refresh failed: " + error.getMessage(), Snackbar.LENGTH_LONG).show();
            } else {
                Snackbar.make(listView, appInfos.size() + " launchable applications loaded", Snackbar.LENGTH_LONG).show();
            }
            currentTask = null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            if (!isAdded()) return;
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            currentTask = null;
        }
    }
}
