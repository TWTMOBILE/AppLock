package com.example.applock;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class AllAppsFragment extends Fragment {

    View view;
    ListView listView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_all_apps, container, false);

        listView = view.findViewById(R.id.listView);

        return view;
    }

    public void getAllApps(View view) throws PackageManager.NameNotFoundException{
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null); mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        // Get list of all the apps installed
        List<ResolveInfo> ril = getActivity().getPackageManager().queryIntentActivities(mainIntent, 0);
        List<String> componentList = new ArrayList<String>();
        String name = null;
        int i = 0;

        // Get Size of ril and create a list
        String[] apps = new String[ril.size()];
        for (ResolveInfo ri : ril){
            if (ri.activityInfo != null) {
                // Get package
                Resources res = getActivity().getPackageManager().getResourcesForApplication(ri.activityInfo.applicationInfo);
                // If activity label res is found
                if (ri.activityInfo.labelRes != 0){
                    name = res.getString(ri.activityInfo.labelRes);
                } else {
                    name = ri.activityInfo.applicationInfo.loadLabel(getActivity().getPackageManager()).toString();
                }
                apps[i] = name;
                i++;
            }
        }
        // Set all the apps name in listview
        listView.setAdapter(new ArrayAdapter<String>(getActivity().getApplicationContext(), android.R.layout.simple_list_item_1, apps));
    }

    @Override
    public void onStart() {
        super.onStart();
    }
}