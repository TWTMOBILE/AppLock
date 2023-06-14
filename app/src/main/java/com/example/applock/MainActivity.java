package com.example.applock;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;


public class MainActivity extends AppCompatActivity {

    Button allAppsBtn, lockedAppsBtn, homeBtn, settingsBtn, notificationsBtn;



    @SuppressLint({"WrongViewCast", "MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        androidx.appcompat.widget.Toolbar toolbar = (Toolbar) findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);


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
}