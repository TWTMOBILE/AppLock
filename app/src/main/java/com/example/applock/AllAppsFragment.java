package com.example.applock;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AllAppsFragment extends Fragment {

    private View view;
    private ListView listView;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_all_apps, container, false);
        listView = view.findViewById(R.id.listView);
        swipeRefreshLayout = view.findViewById(R.id.swipeToRefresh);
        listView.setTextFilterEnabled(true);

        swipeRefreshLayout.setOnRefreshListener(this::refreshIt);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshIt();
    }

    private void refreshIt() {
        LoadAppInfoTask loadAppInfoTask = new LoadAppInfoTask();
        loadAppInfoTask.execute(PackageManager.GET_META_DATA);
    }

    private class LoadAppInfoTask extends AsyncTask<Integer, Integer, List<AppInfo>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            swipeRefreshLayout.setRefreshing(true);
        }

        @Override
        protected List<AppInfo> doInBackground(Integer... integers) {
            List<AppInfo> apps = new ArrayList<>();
            PackageManager packageManager = requireActivity().getPackageManager();
            List<ApplicationInfo> infos = packageManager.getInstalledApplications(integers[0]);

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
