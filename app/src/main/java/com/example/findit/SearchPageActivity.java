package com.example.findit;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class SearchPageActivity extends AppCompatActivity implements View.OnClickListener {
    // UI elements
    private Button btnUploadGallery, btnTakePicture, btnSearch;

    private TextView tvLabelResult;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private Bitmap imageBitmap;
    private ImageView imageView;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<String> locationPermissionLauncher;
    private static final String PREFS_NAME = "FindItPrefs";
    private static final String KEY_PERMISSION_DIALOG_SHOWN = "locationPermissionDialogShown";
    private Uri imageUri;

    // Firebase instances
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestoreDB;

    private LabelResultReceiver labelResultReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_page);

        // Initialize UI elements and Firebase instances
        init();

        // Set the greeting text with the logged-in user's name
        setGreetingText();

        // Register for activity results
        registerActivityResults();

        // Register the BroadcastReceiver to receive label results
        registerLabelResultReceiver();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(labelResultReceiver);
    }

    /**
     * Register the BroadcastReceiver to receive label results
     */
    private void registerLabelResultReceiver()
    {
        labelResultReceiver = new LabelResultReceiver(tvLabelResult);
        IntentFilter filter = new IntentFilter("FindIt.LABEL_RESULT");
        registerReceiver(labelResultReceiver, filter);
    }


    /**
     * Initialize UI elements and set up click listeners
     */
    private void init() {
        btnUploadGallery = findViewById(R.id.btnUploadGalleryID);
        btnTakePicture = findViewById(R.id.btnTakePictureID);
        btnSearch = findViewById(R.id.btnSearchID);
        imageView = findViewById(R.id.imgViewID);
        tvLabelResult = findViewById(R.id.tvLabelResultID);

        btnUploadGallery.setOnClickListener(this);
        btnTakePicture.setOnClickListener(this);
        btnSearch.setOnClickListener(this);

        firebaseAuth = FirebaseAuth.getInstance();
        firestoreDB = FirebaseFirestore.getInstance();
    }

    /**
     * Register for activity results for camera and gallery
     */
    private void registerActivityResults() {
        // Register for camera activity result
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                this::handleCameraResult);

        // Register for gallery activity result
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::handleGalleryResult);

        // Register for location permission result
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                this::handleLocationPermissionResult);
    }

    /**
     * Handle the result of the camera activity
     *
     * @param isSuccess true if the image was successfully captured, false otherwise
     */
    private void handleCameraResult(boolean isSuccess) {
        if (isSuccess) {
            try {
                imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                imageView.setImageBitmap(imageBitmap);

                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                boolean savePicturesEnabled = prefs.getBoolean("save_pictures_enabled", true);

                if (!savePicturesEnabled)
                    // Delete the image from the gallery if the user chose not to save it
                    getContentResolver().delete(imageUri, null, null);
            } catch (IOException e) {
                Toast.makeText(this, "Failed to process the captured image: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Handle the result of the gallery activity
     *
     * @param result The result from the gallery activity
     */
    private void handleGalleryResult(ActivityResult result) {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            Uri selectedImageUri = result.getData().getData();
            if (selectedImageUri != null) {
                try {
                    imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                    imageView.setImageBitmap(imageBitmap);
                } catch (IOException e) {
                    Toast.makeText(this, "Failed to load image from gallery: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    /**
     * Handle the result of the location permission request
     *
     * @param isGranted true if the permission was granted, false otherwise
     */
    private void handleLocationPermissionResult(boolean isGranted) {
        if (isGranted)
            Toast.makeText(this, "Location permission granted.", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, "Location permission denied. Continuing without location.", Toast.LENGTH_SHORT).show();
    }

    /**
     * Open the gallery to select an image
     */
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    /**
     * Capture an image using the camera and save it to the gallery
     */
    private void takePictureFromCamera() {
        // Create an empty ContentValues object
        ContentValues values = new ContentValues();
        // Insert the new content into the MediaStore and get the URI
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (imageUri != null)
            cameraLauncher.launch(imageUri);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnSearchID)
            handleSearchButtonClick();
        else if (v.getId() == R.id.btnTakePictureID)
            takePictureFromCamera();
        else if (v.getId() == R.id.btnUploadGalleryID)
            openGallery();
    }

    /**
     * Handle the search button click event
     */
    private void handleSearchButtonClick() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean dialogShown = prefs.getBoolean(KEY_PERMISSION_DIALOG_SHOWN, false);

        if (!dialogShown) {
            // Show the dialog and request permission
            showLocationPermissionDialog();
            prefs.edit().putBoolean(KEY_PERMISSION_DIALOG_SHOWN, true).apply();
        } else
            handleSearch();
    }

    /**
     * Show a dialog explaining why location permission is needed and request the permission
     */
    private void showLocationPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Location permission needed")
                .setMessage("This app requires location permission to save the captured image location in the search history.\n\n" +
                        "Please allow location permission.")
                .setPositiveButton("OK", (dialog, which) -> locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION))
                .show();
    }

    /**
     * Handle the search operation by starting the LabelHandlerService with the image data
     */
    private void handleSearch() {
        if (imageBitmap != null) {
            Intent serviceIntent = new Intent(this, LabelHandlerService.class);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            serviceIntent.putExtra(LabelHandlerService.EXTRA_IMAGE, byteArray);
            startService(serviceIntent);
        } else
            Toast.makeText(SearchPageActivity.this, "No image to search.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_sign_out) {
            handleSignOut();
            return true;
        }

        if (id == R.id.action_search_history) {
            startActivity(new Intent(SearchPageActivity.this, HistoryActivity.class));
            return true;
        }

        if (id == R.id.action_settings) {
            startActivity(new Intent(SearchPageActivity.this, SettingsActivity.class));
            return true;
        }

        if (id == R.id.action_about) {
            showAboutDialog();
            return true;
        }

        if (id == R.id.action_exit) {
            showExitConfirmationDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Handle user sign-out and navigate to the login screen
     */
    private void handleSignOut() {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(SearchPageActivity.this, LoginActivity.class));
        finish();
    }

    /**
     * Show a confirmation dialog to exit the application
     */
    private void showExitConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Exit")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", (dialog, which) -> finish())
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Show an about dialog with information about the app
     */
    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("About FindIt")
                .setMessage(
                        "Creators: Omri Naor, Gil Yair Yamin, Netanel Birhauz\n\n" +
                                "OS: Android 11 (R) API Level 30\n\n" +
                                "Date: 21/07/2024")
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * Set the greeting text with the logged-in user's first name
     */
    private void setGreetingText() {
        TextView tvGreeting = findViewById(R.id.tvGreeting);
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            String email = currentUser.getEmail();
            if (email != null) {
                DocumentReference itemRef = firestoreDB.collection("Users").document(email);
                itemRef.get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String firstName = documentSnapshot.getString("firstName");
                        if (firstName != null && !firstName.isEmpty()) {
                            tvGreeting.setText("Logged in as " + firstName);
                        } else {
                            tvGreeting.setText("Logged in as an unknown user");
                        }
                    } else {
                        tvGreeting.setText("Logged in as an unknown user");
                    }
                }).addOnFailureListener(e -> {
                    tvGreeting.setText("Logged in as an unknown user");
                });
            } else {
                tvGreeting.setText("Logged in as an unknown user");
            }
        } else {
            tvGreeting.setText("Logged in as an unknown user");
        }
    }
}
