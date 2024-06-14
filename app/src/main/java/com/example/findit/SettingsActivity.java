package com.example.findit;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class SettingsActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener, View.OnClickListener
{

    private Switch switchNotifications;
    private Button btnEnableGallery;
    private Button btnEnableLocation;
    private Button btnResetPassword;
    private Button btnClearHistory;
    private Button btnReturn;

    private ActivityResultLauncher<String> galleryPermissionLauncher;
    private ActivityResultLauncher<String> locationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        init();
    }

    /**
     * Initialize UI elements, permissions launchers, load preferences, and set listeners
     */
    private void init()
    {
        // Initialize UI elements
        switchNotifications = findViewById(R.id.switchNotificationsID);
        btnEnableGallery = findViewById(R.id.btnEnableGalleryID);
        btnEnableLocation = findViewById(R.id.btnEnableLocationID);
        btnResetPassword = findViewById(R.id.btnSettingsResetPasswordID);
        btnClearHistory = findViewById(R.id.btnClearHistoryID);
        btnReturn = findViewById(R.id.btnSettingsReturnID);

        // Initialize permission launchers
        galleryPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                this::onGalleryPermissionResult);

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                this::onLocationPermissionResult);

        // Load current notification preference
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", true);
        switchNotifications.setChecked(notificationsEnabled);

        // Set listeners
        switchNotifications.setOnCheckedChangeListener(this);
        btnEnableGallery.setOnClickListener(this);
        btnEnableLocation.setOnClickListener(this);
        btnResetPassword.setOnClickListener(this);
        btnClearHistory.setOnClickListener(this);
        btnReturn.setOnClickListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        if (buttonView.getId() == R.id.switchNotificationsID)
        {
            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("notifications_enabled", isChecked);
            editor.apply();
            Toast.makeText(SettingsActivity.this, isChecked ? "Notifications enabled." : "Notifications disabled.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View v)
    {
        if (v.getId() == R.id.btnEnableGalleryID)
        {
            enableGalleryPermission();
            return;
        }

        if (v.getId() == R.id.btnEnableLocationID)
        {
            enableLocationPermission();
            return;
        }

        if (v.getId() == R.id.btnSettingsResetPasswordID)
        {
            resetPassword();
            return;
        }

        if (v.getId() == R.id.btnClearHistoryID)
        {
            clearSearchHistory();
            return;
        }

        if (v.getId() == R.id.btnSettingsReturnID)
        {
            finish();
            return;
        }
    }

    /**
     * Enable gallery permission by requesting the required permission from the user
     */
    private void enableGalleryPermission()
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            galleryPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        else
        {
            Toast.makeText(SettingsActivity.this, "Gallery permission already granted.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Enable location permission by requesting the required permission from the user
     */
    private void enableLocationPermission()
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        else
        {
            Toast.makeText(SettingsActivity.this, "Location permission already granted.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handle the result of the gallery permission request
     *
     * @param isGranted true if the permission is granted, false otherwise
     */
    private void onGalleryPermissionResult(boolean isGranted)
    {
        if (isGranted)
        {
            Toast.makeText(SettingsActivity.this, "Gallery permission granted.", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Toast.makeText(SettingsActivity.this, "Gallery permission denied.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handle the result of the location permission request
     *
     * @param isGranted true if the permission is granted, false otherwise
     */
    private void onLocationPermissionResult(boolean isGranted)
    {
        if (isGranted)
        {
            Toast.makeText(SettingsActivity.this, "Location permission granted.", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Toast.makeText(SettingsActivity.this, "Location permission denied.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Send a password reset email to the currently logged-in user
     */
    private void resetPassword()
    {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null)
        {
            String email = currentUser.getEmail();
            if (email != null && !email.isEmpty())
            {
                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful())
                            {
                                Toast.makeText(SettingsActivity.this, "A password reset link has been sent to your email.", Toast.LENGTH_LONG).show();
                            }
                            else
                            {
                                String errorMessage = (task.getException() != null) ? task.getException().getMessage() : "Unknown error occurred";
                                Toast.makeText(SettingsActivity.this, "Error sending reset email: " + errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
            }
            else
            {
                Toast.makeText(SettingsActivity.this, "User email is not available.", Toast.LENGTH_SHORT).show();
            }
        }
        else
        {
            Toast.makeText(SettingsActivity.this, "No user is currently logged in.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Clear the search history by deleting all images from the logged-in user's storage
     */
    private void clearSearchHistory()
    {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null)
        {
            String email = currentUser.getEmail();
            if (email != null)
            {
                StorageReference userImagesRef = FirebaseStorage.getInstance().getReference().child("images/" + email);
                userImagesRef.listAll().addOnSuccessListener(listResult -> {
                    for (StorageReference item : listResult.getItems())
                    {
                        item.delete().addOnSuccessListener(aVoid -> {
                            Toast.makeText(SettingsActivity.this, "Search history cleared.", Toast.LENGTH_SHORT).show();
                        }).addOnFailureListener(e -> {
                            Toast.makeText(SettingsActivity.this, "Failed to delete image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }).addOnFailureListener(e -> {
                    Toast.makeText(SettingsActivity.this, "Failed to list images: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }
        else
        {
            Toast.makeText(SettingsActivity.this, "User not authenticated.", Toast.LENGTH_SHORT).show();
        }
    }
}
