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

public class SetPinCodeActivity extends AppCompatActivity {

    private EditText editTextPin;
    private Button buttonSave;
    private PinCodeManager pinCodeManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_pin_code);

        pinCodeManager = new PinCodeManager(this);

        editTextPin = findViewById(R.id.editTextPin);
        buttonSave = findViewById(R.id.buttonSave);

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pinCode = editTextPin.getText().toString().trim();
                if (pinCode.length() >= 4) {
                    Log.d("SetPinCodeActivity", "Saving PIN code: " + pinCode);
                    pinCodeManager.savePinCode(pinCode);
                    Intent data = new Intent();
                    data.putExtra("pinCode", pinCode);
                    setResult(Activity.RESULT_OK, data);
                    finish();
                } else {
                    Toast.makeText(SetPinCodeActivity.this, "PIN code must be at least 4 digits long", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
