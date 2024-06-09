package com.example.findit;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import android.provider.MediaStore;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;

public class SearchPageActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnUploadGallery, btnTakePicture, btnSearch;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private Bitmap imageBitmap;

    private ImageView imageView;

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
    }



    private void takePictureFromCamera()
    {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(takePictureIntent);
    }

    private void uploadImageToStorage()
    {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String email = user.getEmail();
            FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
            StorageReference storageRef = firebaseStorage.getReference().child("images/" + email + "/img.jpg");

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] imageBytes = stream.toByteArray();
            UploadTask uploadTask = storageRef.putBytes(imageBytes);

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



    @Override
    public void onClick(View v)
    {
        if (v.getId() == R.id.btnSearchID)
        {
            if (imageBitmap != null)
                uploadImageToStorage();
            else
                Toast.makeText(SearchPageActivity.this, "No image to upload", Toast.LENGTH_SHORT).show();
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

        return super.onOptionsItemSelected(item);
    }
}
