package com.example.findit;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class HistoryActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnReturn;
    private Intent returnIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        init();
    }

    private void init()
    {
        btnReturn = findViewById(R.id.btnReturnID);
        returnIntent = new Intent(HistoryActivity.this, SearchPageActivity.class);


        btnReturn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        startActivity(returnIntent);
    }
}