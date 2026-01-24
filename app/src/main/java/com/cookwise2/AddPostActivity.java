package com.cookwise2;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Date;
import com.cookwise2.utils.RecipePost;
import com.google.firebase.firestore.FirebaseFirestore;

public class AddPostActivity extends AppCompatActivity {

    TextView title;
    TextView content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_post);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        title = findViewById(R.id.etPostTitle);
        content = findViewById(R.id.etPostContent);


        Button btnPublish = findViewById(R.id.btnPublish);
        btnPublish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendPost();
            }
        });
    }
    public void sendPost(){
        Log.d(TAG, "sendPost: start");
        RecipePost post = createRecipePost();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("posts")
                .add(post)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "DocumentSnapshot written with ID: " + documentReference.getId());
                    Toast.makeText(AddPostActivity.this, "Log saved successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // Close this activity and return to FeedActivity
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding document", e);
                    Toast.makeText(AddPostActivity.this, "Error saving log: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
        Log.d(TAG, "sendPost: done");


    }

    public RecipePost createRecipePost(){
        String titleStr = this.title.getText().toString();
        String description = this.content.getText().toString();
        String ownerId =  FirebaseAuth.getInstance().getCurrentUser().getUid();


        SharedPreferences sharedPreferences = getSharedPreferences("userInfo", MODE_PRIVATE);
        // nickname - "N/A" is a default value if nickname is not found in the file
        String nickname = sharedPreferences.getString("nickname", "N/A");

        Timestamp createdAt = new Timestamp(new Date());

        return new RecipePost(titleStr, description, ownerId, nickname, createdAt);




    }
}