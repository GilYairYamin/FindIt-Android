package com.example.findit;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Handler to delay the transition by 3 seconds (3000 milliseconds)
        new Handler().postDelayed(() ->
        {
            // Intent to start the second activity
            Intent intent = new Intent(MainActivity.this, SearchPageActivity.class);
            startActivity(intent);
            finish(); // Finish the main activity
        }, 3000); // 3000 milliseconds delay
    }
}
