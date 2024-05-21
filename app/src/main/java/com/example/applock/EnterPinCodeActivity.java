package com.example.applock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class EnterPinCodeActivity extends Activity {

    private EditText pinCodeEditText;
    private Button submitButton;
    private PinCodeManager pinCodeManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_pin_code);

        pinCodeEditText = findViewById(R.id.editTextPin);
        submitButton = findViewById(R.id.buttonSubmit);
        pinCodeManager = new PinCodeManager(this);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String enteredPin = pinCodeEditText.getText().toString();
                if (pinCodeManager.validatePinCode(enteredPin)) {
                    Intent intent = new Intent(AppLockService.ACTION_UNLOCK);
                    sendBroadcast(intent);

                    pinCodeManager.setAppUnlocked(true);
                    String lockedPackageName = getIntent().getStringExtra("locked_package_name");
                    pinCodeManager.setUnlockedAppPackage(lockedPackageName);
                    finish();
                } else {
                    pinCodeEditText.setError("Incorrect PIN");
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        notifyServiceActivityClosed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        notifyServiceActivityClosed();
    }

    private void notifyServiceActivityClosed() {
        Intent intent = new Intent(AppLockService.ACTION_UNLOCK);
        sendBroadcast(intent);
    }
}
