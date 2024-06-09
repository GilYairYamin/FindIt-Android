package com.example.findit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class SearchPageActivity extends AppCompatActivity implements LocationListener, View.OnClickListener {

    private Button btnUploadGallery, btnTakePicture, btnSearch;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private Bitmap imageBitmap;

    private ImageView imageView;

    private ActivityResultLauncher<String> requestPermissionLauncher;
    private LocationManager locationManager;

    private String locationString = "Unknown";

    private String bestLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_page);

        init();

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Bundle extras = result.getData().getExtras();
                            if (extras != null) {
                                imageBitmap = (Bitmap) extras.get("data");
                                imageView.setImageBitmap(imageBitmap);
                            }
                        }
                    }
                });

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted)
                        requestSingleLocationUpdate();
                    else
                        Toast.makeText(this, "Location permission denied. You may enable locations via the menu.", Toast.LENGTH_LONG).show();

                });

        locationManager = getSystemService(LocationManager.class);
    }




    private void takePictureFromCamera()
    {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(takePictureIntent);
    }

    private void uploadImageToStorage(String name)
    {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null)
        {
            Random random = new Random();
            int id = random.nextInt(1000000000);

            String email = user.getEmail();
            FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
            StorageReference storageRef = firebaseStorage.getReference().child("images/" + email + "/" + name + "_" + id + ".jpg");

            // Create custom metadata
            StorageMetadata metadata = new StorageMetadata.Builder()
                    .setCustomMetadata("location", locationString)
                    .setCustomMetadata("author", email)
                    .build();

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] imageBytes = stream.toByteArray();
            UploadTask uploadTask = storageRef.putBytes(imageBytes, metadata);


            uploadTask.addOnFailureListener(e -> {
                Toast.makeText(SearchPageActivity.this, "Upload image failed! " + e.getMessage(), Toast.LENGTH_LONG).show();
            }).addOnSuccessListener(taskSnapshot -> {
                Toast.makeText(SearchPageActivity.this, "Upload image success.", Toast.LENGTH_LONG).show();
            });
        }
        else
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();

    }


    private void recognizeImage()
    {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(imageBitmap);
        FirebaseVisionImageLabeler labeler = FirebaseVision.getInstance().getOnDeviceImageLabeler();

        labeler.processImage(image).addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
            @Override
            public void onSuccess(List<FirebaseVisionImageLabel> firebaseVisionImageLabels)
            {
                if (firebaseVisionImageLabels.isEmpty())
                {
                    Toast.makeText(SearchPageActivity.this, "No labels found.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Find the label with the highest confidence
                bestLabel = "Nothing Found";
                float highestConfidence = 0;

                for (FirebaseVisionImageLabel label : firebaseVisionImageLabels)
                {
                    if (label.getConfidence() > highestConfidence)
                    {
                        highestConfidence = label.getConfidence();
                        bestLabel = label.getText();
                    }
                }


                uploadImageToStorage(bestLabel);
                Toast.makeText(SearchPageActivity.this, "Best Label: " + bestLabel, Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e)
            {
                Toast.makeText(SearchPageActivity.this, "Failed to get data.", Toast.LENGTH_SHORT).show();
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

    private void resetPassword()
    {
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
    private void requestSingleLocationUpdate()
    {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, null);
        else
            Toast.makeText(this, "GPS Disabled!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onLocationChanged(Location location)
    {

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        locationString = latitude + ", " + longitude;

        if (Geocoder.isPresent())
        {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try
            {
                List<Address> addressList = geocoder.getFromLocation(latitude, longitude, 1);
                if (addressList != null && !addressList.isEmpty())
                {
                    Address address = addressList.get(0);

                    locationString = address.getAddressLine(0);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace(); // Print the stack trace for debugging purposes
                Toast.makeText(this, "Failed to get address from location", Toast.LENGTH_SHORT).show();
            }
        }
        else
            Toast.makeText(this, "Geocoder not present", Toast.LENGTH_SHORT).show();


        // Stop listening for location updates
        locationManager.removeUpdates(this);
    }

    @Override
    public void onProviderEnabled(String provider)
    {
        Toast.makeText(this, "GPS Enabled!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onProviderDisabled(String provider)
    {
        Toast.makeText(this, "GPS Disabled!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onClick(View v)
    {
        if (v.getId() == R.id.btnSearchID)
        {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);

            else
                requestSingleLocationUpdate();


            if (imageBitmap != null)
                recognizeImage();
            else
                Toast.makeText(SearchPageActivity.this, "No image to search.", Toast.LENGTH_SHORT).show();

        }

        if (v.getId() == R.id.btnTakePictureID)
            takePictureFromCamera();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == R.id.action_sign_out)
        {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(SearchPageActivity.this, LoginActivity.class));
            finish();
            return true;
        }

        if (id == R.id.action_reset_password)
        {
            resetPassword();
            return true;
        }

        if (id == R.id.action_edit_profile)
        {
            startActivity(new Intent(SearchPageActivity.this, ProfilePageActivity.class));
            return true;
        }

        if (id == R.id.action_search_history)
        {
            startActivity(new Intent(SearchPageActivity.this, HistoryActivity.class));
            return true;
        }

        if (id == R.id.action_enable_location)
        {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            else
                Toast.makeText(this, "Location permission already granted", Toast.LENGTH_SHORT).show();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}