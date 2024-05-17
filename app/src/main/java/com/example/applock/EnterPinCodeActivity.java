package com.example.applock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EnterPinCodeActivity extends AppCompatActivity {

    private EditText editTextPin;
    private Button buttonSubmit;
    private PinCodeManager pinCodeManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_pin_code);

        pinCodeManager = new PinCodeManager(this);

        editTextPin = findViewById(R.id.editTextPin);
        buttonSubmit = findViewById(R.id.buttonSubmit);

        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String enteredPinCode = editTextPin.getText().toString().trim();
                Log.d("EnterPinCodeActivity", "Entered PIN code: " + enteredPinCode);
                if (pinCodeManager.validatePinCode(enteredPinCode)) {
                    Intent data = new Intent();
                    data.putExtra("pinCode", enteredPinCode);
                    setResult(Activity.RESULT_OK, data);
                    finish();
                } else {
                    Toast.makeText(EnterPinCodeActivity.this, "Incorrect PIN code", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
