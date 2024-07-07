package com.example.findit;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity implements View.OnClickListener
{
    // UI elements
    private Button btnReturn;
    private ListView listView;
    private List<ImageData> imageDataList;
    private ImageAdapter imageAdapter;
    private ImageView fullImageView;
    private TextView noInternetTextView;

    // Firebase instances
    FirebaseAuth firebaseAuth;
    FirebaseStorage firebaseStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Initialize UI elements and Firebase instances
        init();

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null)
        {
            String userEmail = currentUser.getEmail();
            if (isNetworkAvailable())
                fetchImages(userEmail);
            else
                noInternetTextView.setVisibility(View.VISIBLE);
        }

        else
        {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Initialize UI elements and Firebase instances.
     */
    private void init()
    {
        btnReturn = findViewById(R.id.btnReturnID);
        listView = findViewById(R.id.lstViewID);
        fullImageView = findViewById(R.id.fullImageView);
        noInternetTextView = findViewById(R.id.noInternetTextView);

        btnReturn.setOnClickListener(this);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();

        imageDataList = new ArrayList<>();
        imageAdapter = new ImageAdapter(this, imageDataList, this::showFullScreenImage);
        listView.setAdapter(imageAdapter);
    }

    /**
     * Check if the network is available.
     *
     * @return true if the network is available, false otherwise.
     */
    private boolean isNetworkAvailable()
    {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = connectivityManager.getActiveNetwork();
        if (network != null)
        {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }

        return false;
    }

    /**
     * Fetch images from Firebase Storage and populate the list.
     *
     * @param userEmail The email of the logged-in user.
     */
    private void fetchImages(String userEmail)
    {
        StorageReference imagesRef = firebaseStorage.getReference().child("images/" + userEmail);

        imagesRef.listAll()
                .addOnSuccessListener(listResult -> {
                    for (StorageReference item : listResult.getItems())
                    {
                        item.getDownloadUrl().addOnSuccessListener(uri ->
                                item.getMetadata().addOnSuccessListener(metadata -> {
                                    String name = item.getName().split("_")[0];
                                    String location = metadata.getCustomMetadata("location");

                                    // Get the creation date from metadata
                                    long creationTimeMillis = metadata.getCreationTimeMillis();
                                    String creationDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(creationTimeMillis));

                                    imageDataList.add(new ImageData(name, uri.toString(), location, creationDate));
                                    imageAdapter.notifyDataSetChanged();
                                }).addOnFailureListener(e -> Toast.makeText(HistoryActivity.this, "Failed to get metadata: " + e.getMessage(), Toast.LENGTH_SHORT).show())).addOnFailureListener(e -> Toast.makeText(HistoryActivity.this, "Failed to get download URL: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(HistoryActivity.this, "Failed to list images: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onClick(View v)
    {
        if (v.getId() == R.id.btnReturnID)
        {
            if (fullImageView.getVisibility() == View.VISIBLE)
                fullImageView.setVisibility(View.GONE);

            else
            {
                startActivity(new Intent(HistoryActivity.this, SearchPageActivity.class));
                finish();
            }
        }
    }

    /**
     * Show the selected image in full screen.
     *
     * @param imageUrl The URL of the image to be displayed.
     */
    public void showFullScreenImage(String imageUrl)
    {
        fullImageView.setVisibility(View.VISIBLE);
        Glide.with(this).load(imageUrl).into(fullImageView);
        fullImageView.setOnClickListener(this::toggleFullScreenImage);
    }

    /**
     * Toggle the visibility of the full screen image.
     *
     * @param view The view that was clicked.
     */
    public void toggleFullScreenImage(View view)
    {
        if (fullImageView.getVisibility() == View.VISIBLE)
            fullImageView.setVisibility(View.GONE);
    }
}
