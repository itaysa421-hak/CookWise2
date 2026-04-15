package com.cookwise2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "loginActivity";

    FirebaseAuth auth;

    EditText emailEditText;
    EditText passwordEditText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            Log.i("LoginActivity", "User already signed in, navigating to FeedActivity");
            Intent intent = new Intent(LoginActivity.this, FeedActivity.class);
            startActivity(intent);
            finish();
        }

        emailEditText = findViewById(R.id.et_Email);
        passwordEditText = findViewById(R.id.et_password);
        Button btn_login = findViewById(R.id.btn_login);
        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogin();
            }
        });


        TextView move_to_Registration = findViewById(R.id.move_to_Registration);
        move_to_Registration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent  = new Intent(LoginActivity.this, RegistrationActivity.class);
                startActivity(intent);
                finish();
            }
        });

    }
    private void performLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validate input
        if (email.isEmpty() || password.isEmpty()) {
            Log.w("LoginActivity", "Empty email and/or password field");
            Toast.makeText(LoginActivity.this, "Please fill in all fields", Toast.LENGTH_LONG).show();
            return;
        }
        // Perform Firebase authentication
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.i("LoginActivity", "signInWithEmail:success");
                        getUserDataFromFirestore();
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("LoginActivity", "signInWithEmail:failure", task.getException());

                        String errorMessage = "Authentication failed. ";

                        if (task.getException() != null) {
                            errorMessage += task.getException().getMessage();
                        }

                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void startFeedActivity(boolean sendToast) {
        if(sendToast)
            Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();

        // Navigate to FeedActivity
        Intent intent = new Intent(LoginActivity.this, FeedActivity.class);
        startActivity(intent);
        finish();
    }

    private void getUserDataFromFirestore() {
        String userId = auth.getCurrentUser().getUid();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("users").document(userId).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                // User data exists, you can use it
                                String nickname = document.getString("nickname");
                                Log.d(TAG, "getUserDataFromFirestore onComplete: nickname: " + nickname);

                                saveUserDataLocally(nickname);

                                Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();

                                // Navigate to FeedActivity
                                startFeedActivity(true);

                            } else {
                                // User data doesn't exist, handle accordingly
                                Log.d(TAG, "getUserData onComplete: user data doesn't exist");
                                Toast.makeText(LoginActivity.this, "Error getting user data", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            // Handle errors
                            Log.d(TAG, "getUserData onComplete: error: " + task.getException().getMessage());
                            Toast.makeText(LoginActivity.this, "Error getting user data", Toast.LENGTH_LONG).show();
                        }
                    }

                });
    }

    private void saveUserDataLocally(String nickname){
        SharedPreferences sharedPreferences = getSharedPreferences("userInfo", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("nickname", nickname);
        editor.apply();
    }


}