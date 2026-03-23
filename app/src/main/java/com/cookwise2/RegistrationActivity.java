package com.cookwise2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.cookwise2.utils.RegistrationManager;
import com.cookwise2.utils.UserImageSelector;

public class RegistrationActivity extends AppCompatActivity {
    private static final String TAG = "RegistrationActivity";

    EditText emailEditText;
    EditText passwordEditText;

    EditText nicknameEditText;
    UserImageSelector userImageSelector;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        emailEditText = findViewById(R.id.et_Email);
        passwordEditText = findViewById(R.id.et_password);
        nicknameEditText = findViewById(R.id.et_nickname);

        ImageView profilePictureImageView = findViewById(R.id.iv_profile_picture);
        userImageSelector = new UserImageSelector(this, profilePictureImageView);
        profilePictureImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userImageSelector.showImageSourceDialog();
            }
        });

        Button registerButton = findViewById(R.id.btn_register);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerButtonClick();
            }
        });

        TextView moveToLogin = findViewById(R.id.move_to_login);
        moveToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });


    }
    private void registerButtonClick() {
        Log.d(TAG, "Register button clicked");

        RegistrationManager registrationManager = new RegistrationManager(RegistrationActivity.this);
        registrationManager.startRegistration(
                emailEditText.getText().toString(),
                passwordEditText.getText().toString(),
                userImageSelector.createImageFile(),
                nicknameEditText.getText().toString(),
                new RegistrationManager.OnResultCallback(){
                    @Override
                    public void onResult(boolean success, String message) {
                        if (success) {
                            Toast.makeText(RegistrationActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                        }
                        else {
                            Toast.makeText(RegistrationActivity.this, "Registration failed: " + message, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}