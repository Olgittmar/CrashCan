package com.olgitt.olgitt.crashcan;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

public class CrashActivity extends AppCompatActivity {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash);

        Intent intent = getIntent();
        String message = intent.getStringExtra("error");

        Log.e("\nFrom CrashReport:", message);
        TextView textView = findViewById(R.id.textView2);
        textView.setText(message);
    }
}
