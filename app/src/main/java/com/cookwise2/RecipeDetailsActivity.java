package com.cookwise2;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.cookwise2.utils.RecipePost;
import com.cookwise2.utils.SupabaseStorageHelper;
import java.util.ArrayList;
import java.util.Map;

public class RecipeDetailsActivity extends AppCompatActivity {

    private ImageView ivDetailImage;
    private TextView tvDetailTitle, tvDetailOwner, tvDetailIngredients, tvDetailDescription;
    private com.google.android.material.chip.ChipGroup cgDetailsCategories;

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
        cgDetailsCategories = findViewById(R.id.cgDetailsCategories);
    }

    private void populateData(RecipePost post) {
        // הצבת טקסטים בסיסיים
        tvDetailTitle.setText(post.getTitle());
        tvDetailOwner.setText("By " + post.getOwnerNickname());
        tvDetailDescription.setText(post.getDescription());

        // המרת רשימת המצרכים לפורמט יפה עם בולטים
        tvDetailIngredients.setText(formatIngredientsList(post.getGroceries()));

        displayCategories(post.getClassification());

        String imageUrl = post.getImageUrl();
        if(imageUrl == null){
            Glide.with(this)
                    .load(R.drawable.generic_recipe_image_background).
                    placeholder(android.R.drawable.ic_menu_gallery)
                    .centerCrop()
                    .into(ivDetailImage);


        }
        else{
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .centerCrop()
                    .into(ivDetailImage);
        }



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
    private void displayCategories(java.util.Map<String, Object> classification) {
        cgDetailsCategories.removeAllViews();
        if (classification == null) return;

        for (Map.Entry<String, Object> entry : classification.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // דילוג על ערכים ריקים או "none"
            if (value == null || String.valueOf(value).equalsIgnoreCase("none")) continue;

            String displayText = "";
            int iconRes = 0; // אופציונלי: להוסיף אייקונים

            // לוגיקה מיוחדת לפי סוג הקטגוריה
            switch (key) {
                case "spiciness":
                    int spicyLevel = ((Number) value).intValue();
                    if (spicyLevel == 0) continue; // לא מציגים "לא חריף" כדי לשמור על ניקיון
                    displayText = getSpicinessText(spicyLevel);
                    break;

                case "estimated_time":
                    displayText = value + " min 🕒";
                    break;

                case "health_score":
                    displayText = "Health: " + value + "/100 🌱";
                    break;

                default:
                    // לכל שאר הקטגוריות (Vegan, Easy, וכו') פשוט מציגים את המילה
                    displayText = String.valueOf(value);
                    break;
            }

            if (!displayText.isEmpty()) {
                addElegantChip(displayText);
            }
        }
    }

    // פונקציית עזר לתרגום חריפות
    private String getSpicinessText(int level) {
        if (level <= 1) return "Mild 🌶️";
        if (level == 2) return "Spicy 🌶️🌶️";
        return "Extra Hot! 🌶️🌶️🌶️";
    }

    // יצירת צ'יפ במראה עדין
    private void addElegantChip(String text) {
        com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
        chip.setText(text);

        // מראה עדין: רק מסגרת, בלי רקע כהה
        chip.setChipCornerRadius(20f);
        chip.setChipBackgroundColorResource(android.R.color.transparent);
        chip.setChipStrokeColorResource(android.R.color.darker_gray);
        chip.setChipStrokeWidth(1f);
        chip.setTextSize(13f);
        chip.setTextColor(android.graphics.Color.parseColor("#555555")); // אפור כהה עדין

        chip.setClickable(false);
        cgDetailsCategories.addView(chip);
    }

}