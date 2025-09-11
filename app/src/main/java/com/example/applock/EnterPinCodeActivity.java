package com.example.applock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class EnterPinCodeActivity extends AppCompatActivity {

    private EditText pinCodeEditText;
    private PinCodeManager pinCodeManager;
    private static final long DEFAULT_GRACE_MS = 15_000L; // 15s grace


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Block screenshots/recents preview
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_enter_pin_code);

        pinCodeManager = new PinCodeManager(this);

        pinCodeEditText = findViewById(R.id.editTextPin);
        Button submitButton = findViewById(R.id.buttonSubmit);

        submitButton.setOnClickListener(v -> {
            String enteredPin = pinCodeEditText.getText().toString();
            if (!pinCodeManager.validatePinCode(enteredPin)) {
                pinCodeEditText.setError("Incorrect PIN");
                return;
            }
            String pkg = getIntent().getStringExtra("locked_package_name");
            if (TextUtils.isEmpty(pkg)) pkg = getPackageName();

            // Grant a short-lived temp unlock (prevents immediate loop)
            pinCodeManager.unlockAppTemporarily(pkg, DEFAULT_GRACE_MS);

            Intent b = new Intent(AppLockService.ACTION_UNLOCK);
            b.putExtra("pkg", pkg);
            sendBroadcast(b);

            setResult(Activity.RESULT_OK);
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        // Do not reveal whatever is behind; send user to launcher
        setResult(Activity.RESULT_CANCELED);
        super.onBackPressed();
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(home);
        finishAndRemoveTask();
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        // If user leaves without unlocking, close this screen
        String pkg = getIntent().getStringExtra("locked_package_name");
        if (TextUtils.isEmpty(pkg)) {
            pkg = getPackageName();
        }
        if (!pinCodeManager.isAppTemporarilyUnlocked(pkg)) {
            setResult(Activity.RESULT_CANCELED);
            finishAndRemoveTask();
        }
    }
}
