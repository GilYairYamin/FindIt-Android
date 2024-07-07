package com.example.findit;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
    private void signIn() {
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
                if (errorMessage.toLowerCase().startsWith("a network"))
                    Toast.makeText(LoginActivity.this, "Network problem. \nPlease check your connection.", Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }


    /**
     * Displays an AlertDialog for resetting the password.
     * If the email field is empty, shows a toast message to inform the user.
     * Sends a password reset email to the specified email address.
     * Displays a toast message indicating whether the email was sent successfully or if there was an error.
     */
    private void showResetPasswordDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.reset_password, null);
        builder.setView(dialogView);

        EditText dialogEmailEditText = dialogView.findViewById(R.id.emailEditTextID);
        Button dialogResetPasswordButton = dialogView.findViewById(R.id.resetPasswordButtonID);

        dialogResetPasswordButton.setOnClickListener(v -> {
            String email = dialogEmailEditText.getText().toString();

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
        });

        builder.setPositiveButton("OK", (dialog, id) -> dialog.dismiss());
        builder.setNegativeButton("Cancel", (dialog, id) -> dialog.dismiss());


        AlertDialog dialog = builder.create();


        dialog.show();
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
            showResetPasswordDialog();
        }

        if (v.getId() == R.id.btnSignupID)
        {
            startActivity(new Intent(LoginActivity.this, ProfilePageActivity.class));
            finish();
        }
    }
}
