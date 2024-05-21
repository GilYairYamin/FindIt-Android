package com.example.findit;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class SearchPageActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnProfileSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_page);

        btnProfileSettings = findViewById(R.id.btnProfileSettings);
        btnProfileSettings.setOnClickListener(this);
    }

    @Override
    public void onClick(View v)
    {
        Intent btnsIntent = new Intent(SearchPageActivity.this, ProfilePageActivity.class);
        startActivity(btnsIntent);
    }
}