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


public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnLogin, btnSignup, btnResetPassword;
    private EditText etxtEmail, etxtPassword;
    FirebaseAuth firebaseAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        init();

        firebaseAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if(currentUser != null)
            startActivity(new Intent(LoginActivity.this, SearchPageActivity.class));
    }

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

        Task<AuthResult> signInTask = firebaseAuth.signInWithEmailAndPassword(email, password);

        signInTask.addOnCompleteListener(task ->
        {
            if(task.isSuccessful())
                startActivity(new Intent(LoginActivity.this, SearchPageActivity.class));
            else
                Toast.makeText(LoginActivity.this, "" + task.getException().getMessage(), Toast.LENGTH_LONG).show();

        });
    }

    private void resetPassword()
    {
        String email = etxtEmail.getText().toString();

        if (email.isEmpty())
        {
            Toast.makeText(LoginActivity.this, "To reset your password, please enter your email address and try again.", Toast.LENGTH_LONG).show();
            return;
        }

        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful())
                        Toast.makeText(LoginActivity.this, "A password reset link has been successfully sent to the provided email address.", Toast.LENGTH_LONG).show();
                    else
                        Toast.makeText(LoginActivity.this, "Error sending reset email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();

                });
    }

    @Override
    public void onClick(View v)
    {
        if(v.getId() == R.id.btnLoginID)
                signIn();

        if (v.getId() == R.id.btnResetPasswordID)
            resetPassword();

        if (v.getId() == R.id.btnSignupID)
            startActivity(new Intent(LoginActivity.this, ProfilePageActivity.class));
    }
}