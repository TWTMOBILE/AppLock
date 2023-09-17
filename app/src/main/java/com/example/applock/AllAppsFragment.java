package com.example.applock;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.example.applock.Model.AppInfo;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import com.example.applock.Model.AppInfo;

public class AllAppsFragment extends Fragment{

    View view;
    ListView listView;
    SwipeRefreshLayout swipeRefreshLayout;
    boolean mIncludeSystemApps;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_all_apps, container, false);
        listView = view.findViewById(R.id.listView);
//        androidx.appcompat.widget.Toolbar toolbar = (androidx.appcompat.widget.Toolbar) view.findViewById(R.id.toolBar);
//        Application application = getActivity().getApplication();
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeToRefresh);
        listView.setTextFilterEnabled(true);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshIt();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        LoadAppInfoTask loadAppInfoTask = new LoadAppInfoTask();
        loadAppInfoTask.execute(PackageManager.GET_META_DATA);
    }

    private void refreshIt() {
        LoadAppInfoTask loadAppInfoTask = new LoadAppInfoTask();
        loadAppInfoTask.execute(PackageManager.GET_META_DATA);
    }

    class LoadAppInfoTask extends AsyncTask<Integer, Integer, List<AppInfo>>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            swipeRefreshLayout.setRefreshing(true);
        }

        @Override
        protected List<AppInfo> doInBackground(Integer... integers) {

            List<AppInfo> apps = new ArrayList<>();
            PackageManager packageManager = getActivity().getPackageManager();
            List<ApplicationInfo> infos = packageManager.getInstalledApplications(integers[0]);

            for (ApplicationInfo info:infos){
                if (!mIncludeSystemApps && (info.flags & ApplicationInfo.FLAG_SYSTEM) == 1){
                    continue;
                }
                AppInfo app = new AppInfo();
                app.info = info;
                app.label = (String) info.loadLabel(packageManager);
                apps.add(app);
            }

            return apps;
        }

        @Override
        protected void onPostExecute(List<AppInfo> appInfos) {
            super.onPostExecute(appInfos);
            listView.setAdapter((ListAdapter) new com.example.myapplication.Adapter.AppAdapter(requireContext(), appInfos));
            swipeRefreshLayout.setRefreshing(false);
            Snackbar.make(listView, appInfos.size() + " applications loaded", Snackbar.LENGTH_LONG).show();
        }
    }

    public void getAllApps() throws PackageManager.NameNotFoundException{
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        //Get list of all the apps installed
        List<ResolveInfo> ril = getActivity().getPackageManager().queryIntentActivities(mainIntent, 0);
        List<String> componentList = new ArrayList<>();
        String name = null;
        int i = 0;

        // Get size of ril and create a list
        String[] apps = new String[ril.size()];
        for (ResolveInfo ri:ril){
            if (ri.activityInfo != null){
                // Get package
                 Resources res = getActivity().getPackageManager().getResourcesForApplication(ri.activityInfo.applicationInfo);
                 //If activity label res is found
                if (ri.activityInfo.labelRes != 0){
                    name = res.getString(ri.activityInfo.labelRes);
                } else {
                    name = ri.activityInfo.applicationInfo.loadLabel(getActivity().getPackageManager()).toString();
                }
                apps[i] = name;
                i++;
            }
        }
        // Set all the apps name in listView
        listView.setAdapter(new ArrayAdapter<String>(getActivity().getApplicationContext(), android.R.layout.simple_list_item_1));
    }

    @Override
    public void onStart() {
        super.onStart();
    }
}