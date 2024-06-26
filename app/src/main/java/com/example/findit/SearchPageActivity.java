package com.example.findit;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class SearchPageActivity extends AppCompatActivity implements View.OnClickListener {
    // UI elements
    private Button btnUploadGallery, btnTakePicture, btnSearch;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private Bitmap imageBitmap;
    private ImageView imageView;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<String> galleryPermissionLauncher;
    private ActivityResultLauncher<String> locationPermissionLauncher;
    private static final String PREFS_NAME = "FindItPrefs";
    private static final String KEY_PERMISSION_DIALOG_SHOWN = "locationPermissionDialogShown";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_page);

        // Initialize UI elements and setup listeners
        init();

        // Register for camera activity result
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        if (extras != null) {
                            imageBitmap = (Bitmap) extras.get("data");
                            imageView.setImageBitmap(imageBitmap);
                        }
                    }
                });

        // Register for gallery activity result
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            try {
                                InputStream imageStream = getContentResolver().openInputStream(selectedImageUri);
                                imageBitmap = BitmapFactory.decodeStream(imageStream);
                                imageView.setImageBitmap(imageBitmap);
                            } catch (Exception e) {
                                Toast.makeText(this, "Failed to load image from gallery: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });

        // Register for gallery permission result
        galleryPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openGallery();
                    } else {
                        Toast.makeText(this, "Gallery permission denied.", Toast.LENGTH_SHORT).show();
                    }
                });

        // Register for location permission result
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        handleSearch();
                    } else {
                        Toast.makeText(this, "Location permission denied.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void init() {
        btnUploadGallery = findViewById(R.id.btnUploadGalleryID);
        btnTakePicture = findViewById(R.id.btnTakePictureID);
        btnSearch = findViewById(R.id.btnSearchID);
        imageView = findViewById(R.id.imgViewID);

        btnUploadGallery.setOnClickListener(this);
        btnTakePicture.setOnClickListener(this);
        btnSearch.setOnClickListener(this);
    }

    private void openGallery() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            galleryPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        }
    }

    private void takePictureFromCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(takePictureIntent);
    }

    /**
     * Shows a dialog explaining why the location permission is needed, and then requests the permission.
     */
    private void showLocationPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Location permission needed")
                .setMessage("This app requires location permission to save the captured image location in the search history.\n\n" +
                        "Please allow location permission.")
                .setPositiveButton("OK", (dialog, which) -> locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION))
                .show();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnSearchID) {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            boolean dialogShown = prefs.getBoolean(KEY_PERMISSION_DIALOG_SHOWN, false);

            if (!dialogShown) {
                // Show the dialog and request permission
                showLocationPermissionDialog();
                prefs.edit().putBoolean(KEY_PERMISSION_DIALOG_SHOWN, true).apply();
            } else {
                // Check if permission is granted
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                } else {
                    handleSearch();
                }
            }
        }

        if (v.getId() == R.id.btnTakePictureID) {
            takePictureFromCamera();
        }

        if (v.getId() == R.id.btnUploadGalleryID) {
            openGallery();
        }
    }

    /**
     * Handles the search operation by starting the LabelHandlerService with the image data.
     */
    private void handleSearch() {
        if (imageBitmap != null) {
            Intent serviceIntent = new Intent(this, LabelHandlerService.class);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            imageBitmap.compress(CompressFormat.JPEG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            serviceIntent.putExtra(LabelHandlerService.EXTRA_IMAGE, byteArray);
            startService(serviceIntent);
        } else {
            Toast.makeText(SearchPageActivity.this, "No image to search.", Toast.LENGTH_SHORT).show();
        }
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
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(SearchPageActivity.this, LoginActivity.class));
            finish();
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
     * Shows a dialog to confirm if the user wants to exit the app.
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
     * Shows a dialog with information about the app.
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
}
