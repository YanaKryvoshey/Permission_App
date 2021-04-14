package com.example.permission_app;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

public class SecondActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        TextView second_LBL_enter = findViewById(R.id.second_LBL_enter);
        ImageView second_IMG_confetti = findViewById(R.id.second_IMG_confetti);
    }
}