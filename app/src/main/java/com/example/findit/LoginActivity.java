package com.example.findit;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener
{
    // UI elements
    private Button btnLogin, btnSignup, btnResetPassword;
    private EditText etxtEmail, etxtPassword;

    // Firebase authentication instance
    FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize UI elements and Firebase instance
        init();

        firebaseAuth = FirebaseAuth.getInstance();

        // Check if a user is already logged in
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null)
        {
            startActivity(new Intent(LoginActivity.this, SearchPageActivity.class));
            finish();
        }
    }

    /**
     * Initialize UI elements and set up click listeners
     */
    private void init()
    {
        btnLogin = findViewById(R.id.btnLoginID);
        btnSignup = findViewById(R.id.btnSignupID);
        btnResetPassword = findViewById(R.id.btnResetPasswordID);
        etxtEmail = findViewById(R.id.etxtEmailID);
        etxtPassword = findViewById(R.id.etxtPasswordID);

        btnLogin.setOnClickListener(this);
        btnResetPassword.setOnClickListener(this);
        btnSignup.setOnClickListener(this);
    }

    /**
     * Authenticates the user with the provided email and password.
     * If the email or password fields are empty, shows a toast message to inform the user.
     * Displays a toast message indicating whether the sign-in was successful or if there was an error.
     */
    private void signIn()
    {
        String email = etxtEmail.getText().toString();
        String password = etxtPassword.getText().toString();

        if (email.isEmpty())
        {
            Toast.makeText(LoginActivity.this, "Email address is missing.", Toast.LENGTH_LONG).show();
            return;
        }

        if (password.isEmpty())
        {
            Toast.makeText(LoginActivity.this, "Password is missing.", Toast.LENGTH_LONG).show();
            return;
        }

        // Authenticate the user with Firebase
        Task<AuthResult> signInTask = firebaseAuth.signInWithEmailAndPassword(email, password);

        signInTask.addOnCompleteListener(task ->
        {
            if (task.isSuccessful())
            {
                Intent intent = new Intent(LoginActivity.this, SearchPageActivity.class);
                startActivity(intent);
                finish();
            }
            else
            {
                // Handle potential NullPointerException from getMessage()
                String errorMessage = (task.getException() != null) ? task.getException().getMessage() : "Unknown error occurred";
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }


    /**
     * Sends a password reset email to the specified email address.
     * If the email field is empty, shows a toast message to inform the user.
     * Displays a toast message indicating whether the email was sent successfully or if there was an error.
     */
    private void resetPassword()
    {
        String email = etxtEmail.getText().toString();

        if (email.isEmpty())
        {
            Toast.makeText(LoginActivity.this, "To reset your password, please enter your email address and try again.", Toast.LENGTH_LONG).show();
            return;
        }

        // Send a password reset email
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnCompleteListener(task ->
                {
                    if (task.isSuccessful())
                    {
                        Toast.makeText(LoginActivity.this, "A password reset link has been successfully sent to the provided email address.", Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        // Handle potential NullPointerException from getMessage()
                        String errorMessage = (task.getException() != null) ? task.getException().getMessage() : "Unknown error occurred";
                        Toast.makeText(LoginActivity.this, "Error sending reset email: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }


    @Override
    public void onClick(View v)
    {
        if (v.getId() == R.id.btnLoginID)
        {
            signIn();
        }

        if (v.getId() == R.id.btnResetPasswordID)
        {
            resetPassword();
        }

        if (v.getId() == R.id.btnSignupID)
        {
            startActivity(new Intent(LoginActivity.this, ProfilePageActivity.class));
            finish();
        }
    }
}
