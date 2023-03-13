package com.example.applock;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Button allAppsBtn, lockedAppsBtn, homeBtn, settingsBtn, notificationsBtn;
    ListView listView;
    SwipeRefreshLayout swipeRefreshLayout;
    boolean mIncludeSystemApps;

    @SuppressLint({"WrongViewCast", "MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = (ListView) findViewById(R.id.listView);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeToRefresh);
        listView.setTextFilterEnabled(true);
        androidx.appcompat.widget.Toolbar toolbar = (Toolbar) findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                LoadAppInfoTask loadAppInfoTask = new LoadAppInfoTask();
                loadAppInfoTask.execute(PackageManager.GET_META_DATA);
            }
        });

        allAppsBtn = findViewById(R.id.allAppsBtn);
        lockedAppsBtn = findViewById(R.id.lockedAppsBtn);
        homeBtn = findViewById(R.id.homeBtn);
        settingsBtn = findViewById(R.id.settingsBtn);
        notificationsBtn= findViewById(R.id.notificationsBtn);

        allAppsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                replaceFragment(new AllAppsFragment());
            }
        });

        lockedAppsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                replaceFragment(new LockedAppsFragment());
            }
        });

        homeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                replaceFragment(new AllAppsFragment());
            }
        });
        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                replaceFragment(new SettingsFragment());
            }
        });
        notificationsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                replaceFragment(new NotificationFragment());
            }
        });



    }
    private void replaceFragment(Fragment fragment){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainerView, fragment);
        fragmentTransaction.commit();
    }


    class LoadAppInfoTask extends AsyncTask<Integer, Integer, List<AppInfo>>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            swipeRefreshLayout.setRefreshing(true);
        }

        @Override
        protected List<AppInfo> doInBackground(Integer... params) {
            List<AppInfo> apps = new ArrayList<>();
            PackageManager packageManager = getPackageManager();

            List<ApplicationInfo> infos = packageManager.getInstalledApplications(params[0]);

            for(ApplicationInfo info:infos){
                if(mIncludeSystemApps && (info.flags & ApplicationInfo.FLAG_SYSTEM)==1){
                    continue;
                }
                AppInfo app = new AppInfo();
                app.info = info;
                app.label = (String) info.loadLabel(packageManager);
                apps.add(app);
            }

            //sort the data
            return apps;
        }

        @Override
        protected void onPostExecute(List<AppInfo> appInfos) {
            super.onPostExecute(appInfos);
            listView.setAdapter(new AppAdapter(MainActivity.this,appInfos));
            swipeRefreshLayout.setRefreshing(false);
            Snackbar.make(listView,appInfos.size() + " applications loaded", Snackbar.LENGTH_LONG).show();
        }
    }
}