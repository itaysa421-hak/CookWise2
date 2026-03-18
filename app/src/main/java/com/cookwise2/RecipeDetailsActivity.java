package com.cookwise2;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.cookwise2.utils.RecipePost;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import java.util.ArrayList;
import java.util.Map;

public class RecipeDetailsActivity extends AppCompatActivity {

    private ImageView ivDetailImage;
    private TextView tvDetailTitle, tvDetailOwner, tvDetailIngredients, tvDetailDescription;
    private com.google.android.material.chip.ChipGroup cgDetailsCategories;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // הפיכת הסטטוס בר לשקוף למראה מודרני (תמונה שנכנסת תחתיו)
        makeStatusBarTransparent();

        setContentView(R.layout.activity_recipe_details);

        initViews();
        setupToolbar();

        RecipePost post = (RecipePost) getIntent().getSerializableExtra("RECIPE_POST");

        if (post != null) {
            populateData(post);

            tvDetailOwner.setOnClickListener(v -> {
                Intent intent = new Intent(RecipeDetailsActivity.this, ProfileActivity.class);
                intent.putExtra("EXTRA_USER_ID", post.getOwnerUid());
                startActivity(intent);
            });
        }
        CollapsingToolbarLayout collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setContentScrimColor(ContextCompat.getColor(this, android.R.color.transparent));
    }

    private void makeStatusBarTransparent() {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        window.setStatusBarColor(Color.TRANSPARENT);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        // פונקציית החזור
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        ivDetailImage = findViewById(R.id.ivDetailImage);
        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailOwner = findViewById(R.id.tvDetailOwner);
        tvDetailIngredients = findViewById(R.id.tvDetailIngredients);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        cgDetailsCategories = findViewById(R.id.cgDetailsCategories);
        toolbar = findViewById(R.id.toolbar);
    }

    private void populateData(RecipePost post) {
        tvDetailTitle.setText(post.getTitle());
        tvDetailOwner.setText("By " + post.getOwnerNickname());
        tvDetailOwner.setPaintFlags(tvDetailOwner.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvDetailDescription.setText(post.getDescription());
        tvDetailIngredients.setText(formatIngredientsList(post.getGroceries()));

        displayCategories(post.getClassification());

        String imageUrl = post.getImageUrl();
        Glide.with(this)
                .load(imageUrl != null && !imageUrl.isEmpty() ? imageUrl : R.drawable.generic_recipe_image_background)
                .centerCrop()
                .into(ivDetailImage);
    }

    private String formatIngredientsList(ArrayList<String> groceries) {
        if (groceries == null || groceries.isEmpty()) return "No ingredients listed.";
        StringBuilder builder = new StringBuilder();
        for (String item : groceries) {
            builder.append("  •  ").append(item).append("\n");
        }
        return builder.toString().trim();
    }

    private void displayCategories(java.util.Map<String, Object> classification) {
        cgDetailsCategories.removeAllViews();
        if (classification == null) return;

        for (Map.Entry<String, Object> entry : classification.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value == null || String.valueOf(value).equalsIgnoreCase("none")) continue;

            String displayText = "";
            switch (key) {
                case "spiciness":
                    if (value instanceof Number) {
                        int level = ((Number) value).intValue();
                        if (level > 0) displayText = getSpicinessText(level);
                    }
                    break;
                case "estimated_time": displayText = value + " min 🕒"; break;
                case "health_score": displayText = "Health: " + value + " 🌱"; break;
                default: displayText = String.valueOf(value); break;
            }

            if (!displayText.isEmpty()) {
                addElegantChip(displayText);
            }
        }
    }

    private String getSpicinessText(int level) {
        if (level <= 1) return "Mild 🌶️";
        if (level == 2) return "Spicy 🌶️🌶️";
        return "Extra Hot! 🌶️🌶️🌶️";
    }

    private void addElegantChip(String text) {
        com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
        chip.setText(text);
        chip.setChipCornerRadius(24f);
        chip.setChipBackgroundColorResource(android.R.color.white);
        chip.setChipStrokeColorResource(android.R.color.black); // וודא שיש לך צבע כזה //FIXME
        chip.setChipStrokeWidth(2f);
        chip.setTextSize(14);
        chip.setClickable(false);
        cgDetailsCategories.addView(chip);
    }
}