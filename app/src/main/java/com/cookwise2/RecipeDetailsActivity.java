package com.cookwise2;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.cookwise2.utils.RecipePost;
import com.cookwise2.utils.SupabaseStorageHelper;
import java.util.ArrayList;

public class RecipeDetailsActivity extends AppCompatActivity {

    private ImageView ivDetailImage;
    private TextView tvDetailTitle, tvDetailOwner, tvDetailIngredients, tvDetailDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_details);

        // 1. אתחול הרכיבים מה-XML
        initViews();

        // 2. קבלת הנתונים מה-Intent
        RecipePost post = (RecipePost) getIntent().getSerializableExtra("RECIPE_POST");

        // 3. הצגת הנתונים אם האובייקט אינו null
        if (post != null) {
            populateData(post);
        }
    }

    private void initViews() {
        ivDetailImage = findViewById(R.id.ivDetailImage);
        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailOwner = findViewById(R.id.tvDetailOwner);
        tvDetailIngredients = findViewById(R.id.tvDetailIngredients);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
    }

    private void populateData(RecipePost post) {
        // הצבת טקסטים בסיסיים
        tvDetailTitle.setText(post.getTitle());
        tvDetailOwner.setText("By " + post.getOwnerNickname());
        tvDetailDescription.setText(post.getDescription());

        // המרת רשימת המצרכים לפורמט יפה עם בולטים
        tvDetailIngredients.setText(formatIngredientsList(post.getGroceries()));

        // טעינת תמונת הפרופיל/פוסט באמצעות Glide (לפי הלוגיקה שלך מה-Adapter)
        String profilePicturePath = "images/post-pic/" + post.getPostId() + ".jpg";
        String imageUrl = SupabaseStorageHelper.getFileSupabaseUrl(profilePicturePath);

        Glide.with(this)
                .load(imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(ivDetailImage);
    }

    /**
     * הופכת את רשימת המצרכים לטקסט מסודר שבו כל מצרך מתחיל ב-• ובשורה חדשה
     */
    private String formatIngredientsList(ArrayList<String> groceries) {
        if (groceries == null || groceries.isEmpty()) return "No ingredients listed.";

        StringBuilder builder = new StringBuilder();
        for (String item : groceries) {
            builder.append("• ").append(item).append("\n");
        }
        return builder.toString().trim();
    }
}