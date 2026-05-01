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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.bumptech.glide.Glide;
import com.cookwise2.utils.RecipePost;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Map;

public class RecipeDetailsActivity extends AppCompatActivity {

    private ImageView ivDetailImage;
    private TextView tvDetailTitle, tvDetailOwner, tvDetailIngredients, tvDetailDescription;
    private com.google.android.material.chip.ChipGroup cgDetailsCategories;
    private Toolbar toolbar;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabFavorite;
    private boolean isSaved = false;
    private String currentUserId;
    private android.widget.ImageButton btnDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        makeStatusBarTransparent();
        setContentView(R.layout.activity_recipe_details);

        initViews();
        setupToolbar();

        RecipePost post = (RecipePost) getIntent().getSerializableExtra("RECIPE_POST");

        if (post != null) {
            ViewCompat.setTransitionName(ivDetailImage, "recipe_image_transition");
            populateData(post);

            // בדיקה אם המשתמש הוא בעל הפוסט כדי להציג כפתור מחיקה
            if (FirebaseAuth.getInstance().getCurrentUser() != null &&
                    post.getOwnerUid().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {

                btnDelete.setVisibility(View.VISIBLE);
                btnDelete.setOnClickListener(v -> showDeleteBottomSheet(post.getId()));
            }

            tvDetailOwner.setOnClickListener(v -> {
                Intent intent = new Intent(RecipeDetailsActivity.this, ProfileActivity.class);
                intent.putExtra("EXTRA_USER_ID", post.getOwnerUid());
                startActivity(intent);
            });

            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                checkIfPostIsSaved(post.getId());

                fabFavorite.setOnClickListener(v -> {
                    toggleSavePost(post.getId());
                });
            }
        }

        CollapsingToolbarLayout collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setContentScrimColor(ContextCompat.getColor(this, android.R.color.transparent));
    }

    private void checkIfPostIsSaved(String postId) {
        FirebaseFirestore.getInstance().collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        java.util.List<String> savedPosts = (java.util.List<String>) documentSnapshot.get("savedPosts");
                        isSaved = savedPosts != null && savedPosts.contains(postId);
                        updateFabIcon();
                    }
                });
    }

    private void toggleSavePost(String postId) {
        com.google.firebase.firestore.DocumentReference userRef =
                FirebaseFirestore.getInstance().collection("users").document(currentUserId);

        if (isSaved) {
            userRef.update("savedPosts", com.google.firebase.firestore.FieldValue.arrayRemove(postId))
                    .addOnSuccessListener(aVoid -> {
                        isSaved = false;
                        updateFabIcon();
                        Toast.makeText(this, "Removed from saved", Toast.LENGTH_SHORT).show();
                    });
        } else {
            userRef.update("savedPosts", com.google.firebase.firestore.FieldValue.arrayUnion(postId))
                    .addOnSuccessListener(aVoid -> {
                        isSaved = true;
                        updateFabIcon();
                        Toast.makeText(this, "Added to saved!", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateFabIcon() {
        fabFavorite.setImageResource(isSaved ?
                R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_border);
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
        toolbar.setNavigationOnClickListener(v -> supportFinishAfterTransition());
    }

    private void initViews() {
        ivDetailImage = findViewById(R.id.ivDetailImage);
        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailOwner = findViewById(R.id.tvDetailOwner);
        tvDetailIngredients = findViewById(R.id.tvDetailIngredients);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        cgDetailsCategories = findViewById(R.id.cgDetailsCategories);
        toolbar = findViewById(R.id.toolbar);
        fabFavorite = findViewById(R.id.fab_favorite);
        btnDelete = findViewById(R.id.btnDeleteRecipe);
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
        chip.setChipStrokeColorResource(android.R.color.black);
        chip.setChipStrokeWidth(2f);
        chip.setTextSize(14);
        chip.setClickable(false);
        cgDetailsCategories.addView(chip);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        supportFinishAfterTransition();
    }

    /**
     * פתיחת דיאלוג מחיקה עם ה-IDs המתוקנים
     */
    private void showDeleteBottomSheet(String postId) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_delete_bottom_sheet, null);

        // שימוש ב-IDs המדויקים מה-XML החדש
        view.findViewById(R.id.card_confirm_delete).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            deleteRecipe(postId);
        });

        view.findViewById(R.id.card_cancel_delete).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
    }

    private void deleteRecipe(String postId) {
        FirebaseFirestore.getInstance().collection("posts").document(postId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Recipe deleted successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error deleting recipe", Toast.LENGTH_SHORT).show();
                });
    }
}
