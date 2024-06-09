package com.example.findit;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ProfilePageActivity extends AppCompatActivity implements View.OnClickListener{

    private Button btnSave, btnCancel;
    private EditText txtFirstName, txtLastName, txtEmail, txtCellphone, txtPassword;
    private ImageView profilePicture;

    private String firstName, secondName, email, password, cellphone;

    private User user;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestoreDB;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_page);

        init();

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if(currentUser != null)
        {
            disableEmailAndPasswordFields();
            displayUserProfile(currentUser.getEmail());
        }
    }

    private void disableEmailAndPasswordFields()
    {
        txtEmail.setEnabled(false);
        txtPassword.setEnabled(false);
    }

    private void displayUserProfile(String id)
    {
        DocumentReference itemRef = firestoreDB.collection("Users").document(id);

        itemRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists())
                    {
                        User user = documentSnapshot.toObject(User.class);
                        txtFirstName.setText(user.getFirstName());
                        txtLastName.setText(user.getLastName());
                        txtEmail.setText(user.getEmail());
                        txtCellphone.setText(user.getCellphone());
                        txtPassword.setText("******");
                    }

                    else
                        Toast.makeText(ProfilePageActivity.this, "Failed to find document.", Toast.LENGTH_LONG).show();

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfilePageActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }


    private boolean isValidEmail(String email)
    {
        String emailPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
        return email.matches(emailPattern);
    }
    private void init()
    {
        btnSave = findViewById(R.id.btnSaveRegisterID);
        btnCancel = findViewById(R.id.btnCancelRegisterID);
        txtFirstName = findViewById(R.id.etxtFirstNameID);
        txtLastName = findViewById(R.id.etxtLastNameID);
        txtEmail = findViewById(R.id.etxtEmailID);
        txtCellphone = findViewById(R.id.etxtPhoneNumberID);
        txtPassword = findViewById(R.id.etxtPasswordID);


        btnCancel.setOnClickListener(this);
        btnSave.setOnClickListener(this);

        firebaseAuth = FirebaseAuth.getInstance();
        firestoreDB = FirebaseFirestore.getInstance();
    }

    private void createUser()
    {
        if (txtEmail.getText().toString().isEmpty())
        {
            Toast.makeText(ProfilePageActivity.this, "Email address is mandatory.", Toast.LENGTH_LONG).show();
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
        if(currentUser == null)
            signUp();
        else
            updateUserFromDB();
    }

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
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ProfilePageActivity.this, "User added successfully.", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfilePageActivity.this, "Failed to add user.\n" + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

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
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ProfilePageActivity.this, "Profile updated successfully.", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(ProfilePageActivity.this, SearchPageActivity.class));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfilePageActivity.this, "Failed to update profile.\n" + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void signUp()
    {
        Task<AuthResult> signUpTask = firebaseAuth.createUserWithEmailAndPassword(txtEmail.getText().toString(), txtPassword.getText().toString());
        signUpTask.addOnSuccessListener(new OnSuccessListener<AuthResult>()
        {
            @Override
            public void onSuccess(AuthResult authResult)
            {
                Toast.makeText(ProfilePageActivity.this, "Registered Successfully.", Toast.LENGTH_LONG).show();
                addUserToDB();
                startActivity(new Intent(ProfilePageActivity.this, SearchPageActivity.class));
            }
        });
        signUpTask.addOnFailureListener(new OnFailureListener()
        {
            @Override
            public void onFailure(Exception e)
            {
                Toast.makeText(ProfilePageActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onClick(View v)
    {
        if (v.getId() == R.id.btnCancelRegisterID)
            finish();

        if (v.getId() == R.id.btnSaveRegisterID)
            createUser();
    }
}