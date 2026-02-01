package com.cookwise2;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.cookwise2.utils.SupabaseStorageHelper;
import com.cookwise2.utils.UserImageSelector;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import com.cookwise2.utils.RecipePost;
import com.google.firebase.firestore.FirebaseFirestore;

public class AddPostActivity extends AppCompatActivity {

    TextView title;
    TextView content;
    MaterialCardView cardRecipeImage;
    private ImageView ivRecipeImage;
    UserImageSelector userImageSelector;

    private LinearLayout ingredientsContainer;
    private Button btnAddIngredient;
    private String postId;


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
        ingredientsContainer = findViewById(R.id.ingredientsContainer);
        btnAddIngredient = findViewById(R.id.btnAddIngredient);

        ivRecipeImage = findViewById(R.id.ivRecipeImage);


        userImageSelector = new UserImageSelector(this, ivRecipeImage);
        cardRecipeImage = findViewById(R.id.cardRecipeImage);
        cardRecipeImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userImageSelector.showImageSourceDialog();
            }
        });


        btnAddIngredient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewRow();
            }
        });


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
        ArrayList<String> groceries = collectIngredients();

        Random rnd = new Random();
        postId = ownerId + "-" + String.valueOf(rnd.nextInt(1000000000));


        SharedPreferences sharedPreferences = getSharedPreferences("userInfo", MODE_PRIVATE);
        // nickname - "N/A" is a default value if nickname is not found in the file
        String nickname = sharedPreferences.getString("nickname", "N/A");

        Timestamp createdAt = new Timestamp(new Date());
        uploadProfilePictureToSupabase();
        return new RecipePost(postId,titleStr, description, groceries , ownerId, nickname, createdAt);




    }
    private void addNewRow() {
        // 1. ניפוח (Inflate) של קובץ השורה הבודדת
        LayoutInflater inflater = LayoutInflater.from(this);
        View rowView = inflater.inflate(R.layout.row_ingredient, null);

        // 2. מציאת כפתור המחיקה בתוך השורה החדשה שנוצרה
        ImageButton btnRemove = rowView.findViewById(R.id.btnRemoveIngredient);

        // 3. הגדרת פעולת המחיקה
        btnRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ingredientsContainer.removeView(rowView);
            }
        });

        // 4. הוספת השורה למכולה הראשית במסך
        ingredientsContainer.addView(rowView);
    }
    private ArrayList<String> collectIngredients() {
        ArrayList<String> ingredientsList = new ArrayList<>();

        // מעבר על כל ה"ילדים" (השורות) בתוך ה-Container
        for (int i = 0; i < ingredientsContainer.getChildCount(); i++) {
            View row = ingredientsContainer.getChildAt(i);
            EditText etIngredient = row.findViewById(R.id.etIngredientName);

            String ingredientText = etIngredient.getText().toString().trim();

            if (!ingredientText.isEmpty()) {
                ingredientsList.add(ingredientText);
            }
        }

        return ingredientsList;

        // עכשיו יש לך רשימה מוכנה! אפשר לשלוח אותה ל-Firebase או להציג אותה
        // Log.d("RecipeApp", "Ingredients: " + ingredientsList.toString());
    }
    private void uploadProfilePictureToSupabase() {
        File imageFile = userImageSelector.createImageFile();

        if (imageFile == null) {
            Log.d(TAG, "uploadProfilePictureToSupabase: no image file provided");

            return;
        }

        String filename = "images/post-pic/" + postId + ".jpg";
        Log.i(TAG, "Uploading file to Supabase: " + filename);

        SupabaseStorageHelper.uploadPicture(imageFile, filename, new SupabaseStorageHelper.OnResultCallback() {
            @Override
            public void onResult(boolean success, String url, String error) {
                if (success) {
                    Log.i(TAG, "Profile picture uploaded successfully to Supabase. Public URL: " + url);
                } else {
                    Log.e(TAG, "Supabase upload failed: " + error);
                }
            }
        });
    }
}