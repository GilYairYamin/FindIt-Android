package com.example.findit;

import android.content.Intent;
import android.content.SharedPreferences;
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
            // Check if it's the first time by looking for the first name in SharedPreferences
            SharedPreferences sp = getSharedPreferences("profilePref", MODE_PRIVATE);
            String firstName = sp.getString("firstName", null);

            Intent intent;
            if (firstName == null) // If first name exists, go to SearchPageActivity
                intent = new Intent(MainActivity.this, ProfilePageActivity.class);
            else  // If first name doesn't exist, go to ProfilePageActivity
                intent = new Intent(MainActivity.this, SearchPageActivity.class);

            startActivity(intent);
            finish(); // Finish the main activity
        }, 3000); // 3000 milliseconds delay
    }
}
