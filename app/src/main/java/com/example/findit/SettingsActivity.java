package com.example.findit;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class SettingsActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener, View.OnClickListener
{

    private Switch switchNotifications;
    private Switch switchSavePictures;
    private Button btnEnablePermissions;
    private Button btnResetPassword;
    private Button btnClearHistory;
    private Button btnReturn;

    private Button btnEditProfile;

    private static final String PREFS_NAME = "FindItPrefs";


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        init();
    }

    private void init()
    {
        // Initialize UI elements
        switchNotifications = findViewById(R.id.switchNotificationsID);
        switchSavePictures = findViewById(R.id.switchSavePicturesID);
        btnEnablePermissions = findViewById(R.id.btnEnablePermissionsID);
        btnResetPassword = findViewById(R.id.btnSettingsResetPasswordID);
        btnClearHistory = findViewById(R.id.btnClearHistoryID);
        btnReturn = findViewById(R.id.btnSettingsReturnID);
        btnEditProfile = findViewById(R.id.btnEditProfileID);

        // Load current preferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", true);
        boolean savePicturesEnabled = prefs.getBoolean("save_pictures_enabled", true);

        switchNotifications.setChecked(notificationsEnabled);
        switchSavePictures.setChecked(savePicturesEnabled);

        // Set listeners
        switchNotifications.setOnCheckedChangeListener(this);
        switchSavePictures.setOnCheckedChangeListener(this);
        btnEnablePermissions.setOnClickListener(this);
        btnResetPassword.setOnClickListener(this);
        btnClearHistory.setOnClickListener(this);
        btnReturn.setOnClickListener(this);
        btnEditProfile.setOnClickListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (buttonView.getId() == R.id.switchNotificationsID)
        {
            editor.putBoolean("notifications_enabled", isChecked);
            Toast.makeText(SettingsActivity.this, isChecked ? "Notifications enabled." : "Notifications disabled.", Toast.LENGTH_SHORT).show();
        }

        else if (buttonView.getId() == R.id.switchSavePicturesID)
        {
            editor.putBoolean("save_pictures_enabled", isChecked);
            Toast.makeText(SettingsActivity.this, isChecked ? "Pictures will be saved in gallery." : "Pictures will not be saved in gallery.", Toast.LENGTH_SHORT).show();
        }

        editor.apply();
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


    private void showClearHistoryDialog()
    {
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to clear your search history?")
                .setPositiveButton("Clear History", (dialog, which) -> clearSearchHistory())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showNoInternetDialog()
    {
        new AlertDialog.Builder(this)
                .setMessage("No internet connection available. Please check your network settings.")
                .setPositiveButton("Ok", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onClick(View v)
    {
        if (v.getId() == R.id.btnEnablePermissionsID)
        {
            enablePermissions();
            return;
        }

        if (v.getId() == R.id.btnSettingsResetPasswordID)
        {
            resetPassword();
            return;
        }

        if (v.getId() == R.id.btnClearHistoryID)
        {
            if (isNetworkAvailable())
                showClearHistoryDialog();

            else
                showNoInternetDialog();

            return;
        }

        if (v.getId() == R.id.btnEditProfileID)
        {
            startActivity(new Intent(SettingsActivity.this, ProfilePageActivity.class));
            return;
        }

        if (v.getId() == R.id.btnSettingsReturnID)
        {
            startActivity(new Intent(SettingsActivity.this, SearchPageActivity.class));
        }
    }

    /**
     * Direct the user to the app settings in order to enable permissions
     */
    private void enablePermissions()
    {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
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
                Toast.makeText(SettingsActivity.this, "User email is not available.", Toast.LENGTH_SHORT).show();
        }
        else
            Toast.makeText(SettingsActivity.this, "No user is currently logged in.", Toast.LENGTH_SHORT).show();
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
                        item.delete().addOnSuccessListener(aVoid -> Toast.makeText(SettingsActivity.this, "Search history cleared.", Toast.LENGTH_SHORT).show()).addOnFailureListener(e -> Toast.makeText(SettingsActivity.this, "Failed to delete image: " + e.getMessage(), Toast.LENGTH_SHORT).show());

                }).addOnFailureListener(e -> Toast.makeText(SettingsActivity.this, "Failed to list images: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }
        else
            Toast.makeText(SettingsActivity.this, "User not authenticated.", Toast.LENGTH_SHORT).show();
    }
}
