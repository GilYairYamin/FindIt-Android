package com.example.findit;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

public class ProfilePageActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int REQUEST_CODE_READ_EXTERNAL_STORAGE = 100;

    private Button btnSave, btnCancel;
    private EditText txtFirstName, txtLastName, txtEmail, txtCellphone, txtPassword;
    private ImageView profilePicture;

    private User user;
    private Uri profileImageUri;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestoreDB;
    FirebaseStorage firebaseStorage;

    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_page);

        init();

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if (uri != null) {
                            profileImageUri = uri;
                            profilePicture.setImageURI(uri);
                        }
                    }
                });

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            disableEmailAndPasswordFields();
            displayUserProfile(currentUser.getEmail());
        }
    }

    private void disableEmailAndPasswordFields() {
        txtEmail.setEnabled(false);
        txtPassword.setEnabled(false);
    }

    private void displayUserProfile(String email) {
        DocumentReference itemRef = firestoreDB.collection("Users").document(email);

        itemRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        txtFirstName.setText(user.getFirstName());
                        txtLastName.setText(user.getLastName());
                        txtEmail.setText(user.getEmail());
                        txtCellphone.setText(user.getCellphone());
                        txtPassword.setText("******");

                        // Load the profile picture
                        StorageReference profilePicRef = firebaseStorage.getReference()
                                .child("profiles/" + email + "/profile_picture");
                        profilePicRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            // Load the image using an image loading library like Glide or Picasso
                            // For example, using Glide:
                            Glide.with(this).load(uri).into(profilePicture);
                        }).addOnFailureListener(e -> {
                            // Handle any errors
                            Toast.makeText(this, "Failed to load profile picture.", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        Toast.makeText(ProfilePageActivity.this, "Failed to find document.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfilePageActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private boolean isValidEmail(String email) {
        String emailPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
        return email.matches(emailPattern);
    }

    private void init() {
        btnSave = findViewById(R.id.btnSaveRegisterID);
        btnCancel = findViewById(R.id.btnCancelRegisterID);
        txtFirstName = findViewById(R.id.etxtFirstNameID);
        txtLastName = findViewById(R.id.etxtLastNameID);
        txtEmail = findViewById(R.id.etxtEmailID);
        txtCellphone = findViewById(R.id.etxtPhoneNumberID);
        txtPassword = findViewById(R.id.etxtPasswordID);
        profilePicture = findViewById(R.id.imgProfilePicID);

        btnCancel.setOnClickListener(this);
        btnSave.setOnClickListener(this);
        profilePicture.setOnClickListener(this);

        firebaseAuth = FirebaseAuth.getInstance();
        firestoreDB = FirebaseFirestore.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
    }

    private void createUser() {
        if (txtEmail.getText().toString().isEmpty()) {
            Toast.makeText(ProfilePageActivity.this, "Email address is mandatory.", Toast.LENGTH_LONG).show();
            return;
        }

        if (!isValidEmail(txtEmail.getText().toString())) {
            Toast.makeText(ProfilePageActivity.this, "Invalid email address.", Toast.LENGTH_LONG).show();
            return;
        }

        if (txtPassword.getText().toString().isEmpty()) {
            Toast.makeText(ProfilePageActivity.this, "Password is missing.", Toast.LENGTH_LONG).show();
            return;
        }

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null)
            signUp();
        else
            updateUserFromDB();
    }

    private void addUserToDB() {
        user = new User(txtEmail.getText().toString());
        user.setCellphone(txtCellphone.getText().toString());
        user.setFirstName(txtFirstName.getText().toString());
        user.setLastName(txtLastName.getText().toString());

        // Define the document ID using the user's email
        String documentId = user.getEmail();

        // Save data with the defined document ID
        DocumentReference userRef = firestoreDB.collection("Users").document(documentId);
        userRef.set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ProfilePageActivity.this, "User added successfully.", Toast.LENGTH_LONG).show();
                    uploadProfilePicture();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfilePageActivity.this, "Failed to add user.\n" + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateUserFromDB() {
        // Create a User object with the updated details
        user = new User(txtEmail.getText().toString());
        user.setCellphone(txtCellphone.getText().toString());
        user.setFirstName(txtFirstName.getText().toString());
        user.setLastName(txtLastName.getText().toString());

        // Convert the User object to a Map
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("cellphone", user.getCellphone());
        userMap.put("firstName", user.getFirstName());
        userMap.put("secondName", user.getLastName());

        // Reference to the document to be updated
        DocumentReference itemRef = firestoreDB.collection("Users").document(user.getEmail());

        // Update the document
        itemRef.set(userMap, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ProfilePageActivity.this, "Profile updated successfully.", Toast.LENGTH_LONG).show();
                    uploadProfilePicture();
                    startActivity(new Intent(ProfilePageActivity.this, SearchPageActivity.class));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfilePageActivity.this, "Failed to update profile.\n" + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void uploadProfilePicture() {
        if (profileImageUri != null) {
            String email = txtEmail.getText().toString();
            StorageReference profilePicRef = firebaseStorage.getReference()
                    .child("profiles/" + email + "/profile_picture");

            profilePicRef.putFile(profileImageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Toast.makeText(ProfilePageActivity.this, "Profile picture uploaded successfully.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(ProfilePageActivity.this, "Failed to upload profile picture.\n" + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void signUp() {
        Task<AuthResult> signUpTask = firebaseAuth.createUserWithEmailAndPassword(txtEmail.getText().toString(), txtPassword.getText().toString());
        signUpTask.addOnSuccessListener(new OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {
                Toast.makeText(ProfilePageActivity.this, "Registered Successfully.", Toast.LENGTH_LONG).show();
                addUserToDB();
                startActivity(new Intent(ProfilePageActivity.this, SearchPageActivity.class));
            }
        });
        signUpTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ProfilePageActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnCancelRegisterID) {
            FirebaseUser currentUser = firebaseAuth.getCurrentUser();
            if (currentUser != null)
                startActivity(new Intent(ProfilePageActivity.this, SearchPageActivity.class));
            else
                startActivity(new Intent(ProfilePageActivity.this, LoginActivity.class));

            finish();
        }

        if (v.getId() == R.id.btnSaveRegisterID) {
            createUser();
        }

        if (v.getId() == R.id.imgProfilePicID) {
            requestStoragePermission();
        }
    }

    private void requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_READ_EXTERNAL_STORAGE);
        } else {
            openGallery();
        }
    }

    private void openGallery() {
        pickImageLauncher.launch("image/*");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
