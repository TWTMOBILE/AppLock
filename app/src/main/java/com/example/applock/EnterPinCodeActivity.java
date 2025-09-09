package com.example.applock;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Block screenshots/recents preview
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        // Set window flags to ensure proper overlay and prevent bypass
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_SECURE |
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_SECURE |
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );

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

            // Package that triggered the lock (set by AppLockService)
            String pkg = getIntent().getStringExtra("locked_package_name");
            if (TextUtils.isEmpty(pkg)) {
                pkg = getPackageName(); // safe fallback; will immediately be ignored by service
            }

            // Mark this app as temporarily unlocked and notify the service
            pinCodeManager.markAppUnlocked(pkg);
            Intent b = new Intent(AppLockService.ACTION_UNLOCK);
            b.putExtra("pkg", pkg);
            sendBroadcast(b);

            // Return to the previously opened (locked) app
            setResult(RESULT_OK); // Notify caller (e.g., MainActivity) of success
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        // Do not reveal whatever is behind; send user to launcher
        setResult(RESULT_CANCELED);
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
        if (!pinCodeManager.isAppUnlocked()) {
            setResult(RESULT_CANCELED);
            finishAndRemoveTask();
        }
    }
}
