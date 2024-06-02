package com.example.findit;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class SearchPageActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnUploadGallery, btnTakePicture, btnSignout, btnResetPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_page);

        init();
    }

    private void init()
    {
        btnUploadGallery = findViewById(R.id.btnUploadGalleryID);
        btnTakePicture = findViewById(R.id.btnTakePictureID);
        btnSignout = findViewById(R.id.btnSignOutID);
        btnResetPassword = findViewById(R.id.btnResetPasswordID);



        btnUploadGallery.setOnClickListener(this);
        btnTakePicture.setOnClickListener(this);
        btnSignout.setOnClickListener(this);
        btnResetPassword.setOnClickListener(this);
    }

    private void resetPassword()
    {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if(currentUser != null)
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
        if (v.getId() == R.id.btnUploadGalleryID)
            startActivity(new Intent(SearchPageActivity.this, ProfilePageActivity.class));

        if (v.getId() == R.id.btnSignOutID)
        {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(SearchPageActivity.this, LoginActivity.class));
        }

        if (v.getId() == R.id.btnResetPasswordID)
            resetPassword();
    }
}