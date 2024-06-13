package com.example.findit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
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
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class SearchPageActivity extends AppCompatActivity implements LocationListener, View.OnClickListener {
    private static final String CHANNEL_ID = "recognizeImageChannel";
    private static final String CHANNEL_NAME = "Image Recognition Channel";
    private static final int NOTIFICATION_ID = 1;

    private Button btnUploadGallery, btnTakePicture, btnSearch;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private Bitmap imageBitmap;
    private ImageView imageView;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<String> galleryPermissionLauncher;
    private LocationManager locationManager;
    private String locationString = "Unknown";
    private String bestLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_page);

        init();
        createNotificationChannel();

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

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        requestSingleLocationUpdate();
                    } else {
                        Toast.makeText(this, "Location permission denied.", Toast.LENGTH_LONG).show();
                    }
                });

        galleryPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        openGallery();
                    } else {
                        Toast.makeText(this, "Gallery permission denied.", Toast.LENGTH_LONG).show();
                    }
                });

        locationManager = getSystemService(LocationManager.class);
    }

    private void createNotificationChannel() {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            NotificationChannel notificationChannel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(notificationChannel);

    }

    private void sendNotification(String title, String text) {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);

        Intent intent = new Intent(this, SearchPageActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .build();

        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void takePictureFromCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(takePictureIntent);
    }

    private void openGallery() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            galleryPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        }
    }

    private void uploadImageToStorage(String name, String location) {
        new Thread(() -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                Random random = new Random();
                int id = random.nextInt(1000000000);

                String email = user.getEmail();
                FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
                StorageReference storageRef = firebaseStorage.getReference().child("images/" + email + "/" + name + "_" + id + ".jpg");

                // Create custom metadata
                StorageMetadata metadata = new StorageMetadata.Builder()
                        .setCustomMetadata("location", location)
                        .setCustomMetadata("author", email)
                        .build();

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] imageBytes = stream.toByteArray();
                UploadTask uploadTask = storageRef.putBytes(imageBytes, metadata);

                uploadTask.addOnFailureListener(e -> runOnUiThread(() ->
                                Toast.makeText(SearchPageActivity.this, "Upload image failed! " + e.getMessage(), Toast.LENGTH_LONG).show()))
                        .addOnSuccessListener(taskSnapshot -> runOnUiThread(() ->
                                Toast.makeText(SearchPageActivity.this, "Upload image success.", Toast.LENGTH_LONG).show()));
            } else {
                runOnUiThread(() ->
                        Toast.makeText(SearchPageActivity.this, "User not authenticated", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void recognizeImage(Runnable onComplete) {
        new Thread(() -> {
            FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(imageBitmap);
            FirebaseVisionImageLabeler labeler = FirebaseVision.getInstance().getOnDeviceImageLabeler();

            labeler.processImage(image)
                    .addOnSuccessListener(firebaseVisionImageLabels -> {
                        if (firebaseVisionImageLabels.isEmpty()) {
                            sendNotification("Recognition Result", "No labels found.");
                            onComplete.run();
                            return;
                        }

                        // Find the label with the highest confidence
                        bestLabel = "Nothing Found";
                        float highestConfidence = 0;

                        for (FirebaseVisionImageLabel label : firebaseVisionImageLabels) {
                            if (label.getConfidence() > highestConfidence) {
                                highestConfidence = label.getConfidence();
                                bestLabel = label.getText();
                            }
                        }

                        sendNotification("Recognition Result", "Found object: " + bestLabel);
                        onComplete.run();
                    })
                    .addOnFailureListener(e -> {
                        sendNotification("Recognition Result", "Failed to get data.");
                        onComplete.run();
                    });
        }).start();
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

    private void resetPassword() {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null)
            FirebaseAuth.getInstance().sendPasswordResetEmail(currentUser.getEmail())
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful())
                            Toast.makeText(SearchPageActivity.this, "A password reset link has been successfully sent to the provided email address.", Toast.LENGTH_LONG).show();
                        else
                            Toast.makeText(SearchPageActivity.this, "Error sending reset email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    });
    }

    @SuppressLint("MissingPermission")
    private void requestSingleLocationUpdate() {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, null);
        } else {
            Toast.makeText(this, "GPS Disabled! Uploading with default location.", Toast.LENGTH_LONG).show();
            uploadImageToStorage(bestLabel, "Unknown");
        }
    }

    private boolean isPermissionGranted(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onLocationChanged(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        locationString = latitude + ", " + longitude;

        if (Geocoder.isPresent()) {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addressList = geocoder.getFromLocation(latitude, longitude, 1);
                if (addressList != null && !addressList.isEmpty()) {
                    Address address = addressList.get(0);
                    locationString = address.getAddressLine(0);
                }
            } catch (Exception e) {
                e.printStackTrace(); // Print the stack trace for debugging purposes
                Toast.makeText(this, "Failed to get address from location", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Geocoder not present", Toast.LENGTH_SHORT).show();
        }

        // Stop listening for location updates
        locationManager.removeUpdates(this);

        uploadImageToStorage(bestLabel, locationString);
    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText(this, "GPS Enabled!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(this, "GPS Disabled!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnSearchID) {
            if (imageBitmap != null) {
                recognizeImage(() -> {
                    if (!isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                    } else {
                        requestSingleLocationUpdate();
                    }
                });
            } else {
                Toast.makeText(SearchPageActivity.this, "No image to search.", Toast.LENGTH_SHORT).show();
            }
        }

        if (v.getId() == R.id.btnTakePictureID) {
            takePictureFromCamera();
        }

        if (v.getId() == R.id.btnUploadGalleryID) {
            openGallery();
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

        if (id == R.id.action_reset_password) {
            resetPassword();
            return true;
        }

        if (id == R.id.action_edit_profile) {
            startActivity(new Intent(SearchPageActivity.this, ProfilePageActivity.class));
            return true;
        }

        if (id == R.id.action_search_history) {
            startActivity(new Intent(SearchPageActivity.this, HistoryActivity.class));
            return true;
        }

        if (id == R.id.action_enable_location) {
            if (!isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            } else {
                Toast.makeText(this, "Location permission already granted", Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        if (id == R.id.action_enable_gallery) {
            if (!isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                galleryPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            } else {
                Toast.makeText(this, "Gallery permission already granted", Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
