package com.example.findit;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnReturn;
    private ListView listView;
    private List<ImageData> imageDataList;
    private ImageAdapter imageAdapter;
    private ImageView fullImageView;

    FirebaseAuth firebaseAuth;
    FirebaseStorage firebaseStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        init();

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            String userEmail = currentUser.getEmail();
            fetchImages(userEmail);
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void init() {
        btnReturn = findViewById(R.id.btnReturnID);
        listView = findViewById(R.id.lstViewID);
        fullImageView = findViewById(R.id.fullImageView);

        btnReturn.setOnClickListener(this);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();

        imageDataList = new ArrayList<>();
        imageAdapter = new ImageAdapter(this, imageDataList, this::showFullScreenImage);
        listView.setAdapter(imageAdapter);
    }

    private void fetchImages(String userEmail) {
        StorageReference imagesRef = firebaseStorage.getReference().child("images/" + userEmail);

        imagesRef.listAll()
                .addOnSuccessListener(listResult -> {
                    for (StorageReference item : listResult.getItems()) {
                        item.getDownloadUrl().addOnSuccessListener(uri -> {
                            item.getMetadata().addOnSuccessListener(metadata -> {
                                String name = item.getName().split("_")[0];
                                String location = metadata.getCustomMetadata("location");

                                // Get the creation date from metadata
                                long creationTimeMillis = metadata.getCreationTimeMillis();
                                String creationDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(creationTimeMillis));

                                imageDataList.add(new ImageData(name, uri.toString(), location, creationDate));
                                imageAdapter.notifyDataSetChanged();
                            }).addOnFailureListener(e -> Toast.makeText(HistoryActivity.this, "Failed to get metadata: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }).addOnFailureListener(e -> Toast.makeText(HistoryActivity.this, "Failed to get download URL: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(HistoryActivity.this, "Failed to list images: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnReturnID)
            if (fullImageView.getVisibility() == View.VISIBLE)
                fullImageView.setVisibility(View.GONE);


        startActivity(new Intent(HistoryActivity.this, SearchPageActivity.class));
    }

    public void showFullScreenImage(String imageUrl) {
        fullImageView.setVisibility(View.VISIBLE);
        Glide.with(this).load(imageUrl).into(fullImageView);
    }

    public void toggleFullScreenImage(View view) {
        if (fullImageView.getVisibility() == View.VISIBLE) {
            fullImageView.setVisibility(View.GONE);
        }
    }
}
