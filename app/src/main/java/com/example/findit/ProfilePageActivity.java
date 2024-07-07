package com.example.findit;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class ProfilePageActivity extends AppCompatActivity implements View.OnClickListener
{
    // UI elements
    private Button btnSave, btnCancel;
    private EditText txtFirstName, txtLastName, txtEmail, txtCellphone, txtPassword;
    private ImageView profilePicture;

    private User user;
    private Uri profileImageUri;

    // Firebase instances
    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestoreDB;
    FirebaseStorage firebaseStorage;

    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_page);

        // Initialize UI elements and Firebase instances
        init();

        // Register an ActivityResultLauncher for selecting an image from the gallery
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null)
                    {
                        profileImageUri = uri;
                        profilePicture.setImageURI(uri);
                    }
                });

        // Check if there is a currently logged-in user and display their profile
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null)
        {
            disableEmailAndPasswordFields();
            displayUserProfile(currentUser.getEmail());
        }
    }

    /**
     * Initialize UI elements and set up click listeners
     */
    private void init()
    {
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

    /**
     * Disable email and password fields when displaying user profile
     */
    private void disableEmailAndPasswordFields()
    {
        txtEmail.setEnabled(false);
        txtPassword.setEnabled(false);
    }

    /**
     * Display the user profile by fetching data from Firestore
     *
     * @param email The email of the user whose profile is to be displayed
     */
    private void displayUserProfile(String email)
    {
        DocumentReference itemRef = firestoreDB.collection("Users").document(email);

        itemRef.get()
                .addOnSuccessListener(documentSnapshot ->
                {
                    if (documentSnapshot.exists())
                    {
                        User user = documentSnapshot.toObject(User.class);
                        txtFirstName.setText(user.getFirstName());
                        txtLastName.setText(user.getLastName());
                        txtEmail.setText(user.getEmail());
                        txtCellphone.setText(user.getCellphone());
                        txtPassword.setText("******");

                        // Load the profile picture from Firebase Storage
                        StorageReference profilePicRef = firebaseStorage.getReference()
                                .child("profiles/" + email + "/profile_picture");
                        profilePicRef.getDownloadUrl().addOnSuccessListener(uri ->
                        {
                            // Load the image using Glide
                            Glide.with(this).load(uri).into(profilePicture);
                        }).addOnFailureListener(e ->
                        {
                            // Handle any errors
                            Toast.makeText(this, "Profile picture wasn't found.", Toast.LENGTH_SHORT).show();
                        });
                    }
                    else
                    {
                        Toast.makeText(ProfilePageActivity.this, "Failed to find document.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(ProfilePageActivity.this, e.getMessage(), Toast.LENGTH_LONG).show());
    }

    /**
     * Check if the given email is valid
     *
     * @param email The email to validate
     * @return true if the email is valid, false otherwise
     */
    private boolean isValidEmail(String email)
    {
        String emailPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
        return email.matches(emailPattern);
    }

    /**
     * Create a new user profile or update an existing one
     */
    private void createUser()
    {
        if (txtEmail.getText().toString().isEmpty())
        {
            Toast.makeText(ProfilePageActivity.this, "Email address is mandatory.", Toast.LENGTH_LONG).show();
            return;
        }

        if (txtFirstName.getText().toString().isEmpty())
        {
            Toast.makeText(ProfilePageActivity.this, "First name is mandatory.", Toast.LENGTH_LONG).show();
            return;
        }

        if (!isValidEmail(txtEmail.getText().toString()))
        {
            Toast.makeText(ProfilePageActivity.this, "Invalid email address.", Toast.LENGTH_LONG).show();
            return;
        }

        if (txtPassword.getText().toString().isEmpty())
        {
            Toast.makeText(ProfilePageActivity.this, "Password is missing.", Toast.LENGTH_LONG).show();
            return;
        }

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null)
        {
            signUp();
        }
        else
        {
            updateUserFromDB();
        }
    }

    /**
     * Add a new user to the Firestore database
     */
    private void addUserToDB()
    {
        user = new User(txtEmail.getText().toString());
        user.setCellphone(txtCellphone.getText().toString());
        user.setFirstName(txtFirstName.getText().toString());
        user.setLastName(txtLastName.getText().toString());

        // Define the document ID using the user's email
        String documentId = user.getEmail();

        // Save data with the defined document ID
        DocumentReference userRef = firestoreDB.collection("Users").document(documentId);
        userRef.set(user)
                .addOnSuccessListener(aVoid ->
                {
                    uploadProfilePicture();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(ProfilePageActivity.this, "Failed to add user.\n" + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    /**
     * Update user profile in the Firestore database
     */
    private void updateUserFromDB()
    {
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
                .addOnSuccessListener(aVoid ->
                {
                    Toast.makeText(ProfilePageActivity.this, "Profile updated successfully.", Toast.LENGTH_LONG).show();
                    uploadProfilePicture();
                    startActivity(new Intent(ProfilePageActivity.this, SearchPageActivity.class));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(ProfilePageActivity.this, "Failed to update profile.\n" + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    /**
     * Upload the profile picture to Firebase Storage
     */
    private void uploadProfilePicture()
    {
        if (profileImageUri != null)
        {
            String email = txtEmail.getText().toString();
            StorageReference profilePicRef = firebaseStorage.getReference()
                    .child("profiles/" + email + "/profile_picture");

            profilePicRef.putFile(profileImageUri)
                    .addOnFailureListener(e -> Toast.makeText(ProfilePageActivity.this, "Failed to upload profile picture.\n" + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    /**
     * Sign up a new user with Firebase Authentication
     */
    private void signUp()
    {
        Task<AuthResult> signUpTask = firebaseAuth.createUserWithEmailAndPassword(txtEmail.getText().toString(), txtPassword.getText().toString());
        signUpTask.addOnSuccessListener(authResult -> {
            Toast.makeText(ProfilePageActivity.this, "Registered Successfully.", Toast.LENGTH_LONG).show();
            addUserToDB();
            startActivity(new Intent(ProfilePageActivity.this, SearchPageActivity.class));
        });
        signUpTask.addOnFailureListener(e -> Toast.makeText(ProfilePageActivity.this, e.getMessage(), Toast.LENGTH_LONG).show());
    }

    @Override
    public void onClick(View v)
    {
        if (v.getId() == R.id.btnCancelRegisterID)
        {
            FirebaseUser currentUser = firebaseAuth.getCurrentUser();
            if (currentUser != null)
            {
                startActivity(new Intent(ProfilePageActivity.this, SettingsActivity.class));
            }
            else
            {
                startActivity(new Intent(ProfilePageActivity.this, LoginActivity.class));
            }

            finish();
        }

        if (v.getId() == R.id.btnSaveRegisterID)
        {
            createUser();
        }

        if (v.getId() == R.id.imgProfilePicID)
        {
            openGallery();
        }
    }

    /**
     * Open the gallery to select an image
     */
    private void openGallery()
    {
        pickImageLauncher.launch("image/*");
    }
}
