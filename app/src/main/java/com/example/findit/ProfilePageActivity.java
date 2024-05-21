package com.example.findit;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ProfilePageActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String MYLOG = "mylog";
    private Button btnSave, btnCancel;
    private EditText txtFirstName, txtSecondName, txtEmail, txtCellphone, txtAddress;
    private ImageView profilePicture;

    private String firstName, secondName, email, address, cellphone;



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_page);

        init();
        getSharedPreferences();

        if (firstName != null)
            displaySavedSettings();
        else
            // Disable the "Cancel" button if it's the first time on this screen
            btnCancel.setEnabled(false);
    }

    private void displaySavedSettings()
    {
        txtFirstName.setText(firstName);
        txtSecondName.setText(secondName);
        txtEmail.setText(email);
        txtAddress.setText(address);
        txtCellphone.setText(String.valueOf(cellphone));
    }

    private void getSharedPreferences()
    {
        SharedPreferences sp = getSharedPreferences("profilePref", MODE_PRIVATE);
        firstName = sp.getString("firstName", null);
        secondName = sp.getString("secondName", null);
        email = sp.getString("email", null);
        cellphone = sp.getString("cellphone", null);
        address = sp.getString("address", null);
    }

    private boolean saveToSharedPreferences()
    {
        // Get the text from the EditText fields
        firstName = txtFirstName.getText().toString();
        secondName = txtSecondName.getText().toString();
        email = txtEmail.getText().toString();
        address = txtAddress.getText().toString();
        cellphone = txtCellphone.getText().toString();

        // Validate that the first name is not empty
        if (firstName.isEmpty())
        {
            Toast.makeText(this, "First Name is required", Toast.LENGTH_SHORT).show();
            return false; // Exit the method if validation fails
        }

        // Validate the email format
        if (!email.isEmpty() && !isValidEmail(email))
        {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return false; // Exit the method if validation fails
        }

        SharedPreferences sp = getSharedPreferences("profilePref", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        editor.putString("firstName", firstName);
        editor.putString("secondName", secondName);
        editor.putString("email", email);
        editor.putString("cellphone", cellphone);
        editor.putString("address", address);

        editor.apply();

        // Show a toast message indicating that the settings were saved
        Toast.makeText(this, "Profile settings saved", Toast.LENGTH_SHORT).show();
        return true;
    }

    private boolean isValidEmail(String email)
    {
        String emailPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
        return email.matches(emailPattern);
    }
    private void init()
    {
        btnSave = findViewById(R.id.btnSaveSettingsID);
        btnCancel = findViewById(R.id.btnCancelSettingsID);
        txtFirstName = findViewById(R.id.etxtFirstNameID);
        txtSecondName = findViewById(R.id.etxtSecondNameID);
        txtEmail = findViewById(R.id.etxtEmailID);
        txtCellphone = findViewById(R.id.etxtPhoneNumberID);
        txtAddress = findViewById(R.id.etxtAddressID);


        btnCancel.setOnClickListener(this);
        btnSave.setOnClickListener(this);
    }

    @Override
    public void onClick(View v)
    {
        boolean succeed = false;

        if (v.getId() == R.id.btnSaveSettingsID)
            succeed = saveToSharedPreferences();

        if (succeed || v.getId() == R.id.btnCancelSettingsID)
        {
            Intent btnsIntent = new Intent(ProfilePageActivity.this, SearchPageActivity.class);
            startActivity(btnsIntent);
        }
    }
}