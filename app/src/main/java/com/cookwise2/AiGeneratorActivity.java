package com.cookwise2;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.cookwise2.utils.GeminiManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;

public class AiGeneratorActivity extends AppCompatActivity {

    private TextInputLayout tilAiVision;
    private TextInputEditText etAiVision;
    private View nsvIngredients;
    private LinearLayout aiIngredientsContainer;
    private MaterialButton btnGenerate;
    private MaterialButtonToggleGroup toggleAiMode;

    private boolean isPantryMode = false;

    // רכיבי ה-Overlay המשודרגים
    private View loadingOverlay;
    private TextView tvLoadingMessage;
    private ProgressBar pbSpinner; // רפרנס לספינר בשביל שינוי צבע
    private ValueAnimator colorAnimator; // אנימטור לשינוי צבעים
    private Handler loadingHandler = new Handler();
    private int phraseIndex = 0;
    private String[] loadingPhrases = {
            "Preheating the neural networks...",
            "Chopping digital onions (no tears!)",
            "Seasoning the data with logic...",
            "Consulting with the AI Head Chef...",
            "Plating your masterpiece..."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_generator);

        initViews();
        setupToggle();
        addIngredientRow();

        btnGenerate.setOnClickListener(v -> generateRecipe());
    }

    private void initViews() {
        tilAiVision = findViewById(R.id.tilAiVision);
        etAiVision = findViewById(R.id.etAiVision);
        nsvIngredients = findViewById(R.id.nsvIngredients);
        aiIngredientsContainer = findViewById(R.id.aiIngredientsContainer);

        Button btnAddRow = findViewById(R.id.btnAddIngredientRow);
        btnAddRow.setOnClickListener(v -> addIngredientRow());

        btnGenerate = findViewById(R.id.btnGenerate);
        toggleAiMode = findViewById(R.id.toggleAiMode);

        loadingOverlay = findViewById(R.id.loadingOverlay);
        tvLoadingMessage = findViewById(R.id.tvLoadingMessage);

        // מציאת הספינר בתוך ה-Overlay
        // שים לב: וודא שב-XML של ה-loadingOverlay ה-ID של ה-ProgressBar הוא pbSpinner
        pbSpinner = loadingOverlay.findViewById(R.id.pbSpinner);
    }

    private void setupToggle() {
        toggleAiMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnModePantry) {
                    isPantryMode = true;
                    nsvIngredients.setVisibility(View.VISIBLE);
                    tilAiVision.setVisibility(View.GONE);
                } else {
                    isPantryMode = false;
                    nsvIngredients.setVisibility(View.GONE);
                    tilAiVision.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void addIngredientRow() {
        View rowView = getLayoutInflater().inflate(R.layout.row_ingredient, null);
        rowView.findViewById(R.id.btnRemoveIngredient).setOnClickListener(v -> {
            if (aiIngredientsContainer.getChildCount() > 1) {
                aiIngredientsContainer.removeView(rowView);
            } else {
                Toast.makeText(this, "At least one ingredient is required", Toast.LENGTH_SHORT).show();
            }
        });
        aiIngredientsContainer.addView(rowView);
    }

    private void generateRecipe() {
        String finalInput = "";
        if (isPantryMode) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < aiIngredientsContainer.getChildCount(); i++) {
                View row = aiIngredientsContainer.getChildAt(i);
                EditText et = row.findViewById(R.id.etIngredientName);
                String item = et.getText().toString().trim();
                if (!item.isEmpty()) sb.append(item).append(", ");
            }
            finalInput = sb.toString();
        } else {
            finalInput = etAiVision.getText().toString().trim();
        }

        if (finalInput.isEmpty()) {
            Toast.makeText(this, "Please provide some details!", Toast.LENGTH_SHORT).show();
            return;
        }

        startAiLoading();
        btnGenerate.setEnabled(false);

        String prompt = buildPrompt(finalInput);
        GeminiManager.getInstance().sendText(prompt, this, new GeminiManager.GeminiCallback() {
            @Override
            public void onSuccess(String result) {
                parseAndReturn(result);
            }

            @Override
            public void onError(Throwable error) {
                runOnUiThread(() -> {
                    stopAiLoading();
                    btnGenerate.setEnabled(true);
                    Toast.makeText(AiGeneratorActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private String buildPrompt(String input) {
        String modeDescription = isPantryMode ?
                "using ONLY these ingredients (you may use basic staples like oil, salt, water): " :
                "based on this vision: ";

        return "Act as a professional chef. Create a recipe " + modeDescription + "'" + input + "'. " +
                "Return ONLY a valid JSON object with the following structure: " +
                "{\"title\": \"...\", \"ingredients\": [\"...\", \"...\"], \"instructions\": \"...\"}. " +
                "Do not include any Markdown or extra text.";
    }

    private void parseAndReturn(String rawJson) {
        runOnUiThread(() -> {
            try {
                String cleanJson = rawJson.replace("```json", "").replace("```", "").trim();
                JSONObject json = new JSONObject(cleanJson);

                Intent intent = new Intent();
                intent.putExtra("ai_title", json.getString("title"));
                intent.putExtra("ai_instructions", json.getString("instructions"));

                JSONArray ingArray = json.getJSONArray("ingredients");
                ArrayList<String> ingList = new ArrayList<>();
                for (int i = 0; i < ingArray.length(); i++) {
                    ingList.add(ingArray.getString(i));
                }
                intent.putStringArrayListExtra("ai_ingredients", ingList);

                stopAiLoading();
                setResult(RESULT_OK, intent);
                finish();

            } catch (Exception e) {
                stopAiLoading();
                btnGenerate.setEnabled(true);
                Toast.makeText(this, "Failed to parse AI response. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startAiLoading() {
        loadingOverlay.setVisibility(View.VISIBLE);
        phraseIndex = 0;

        // אנימציית שינוי צבעים לספינר (מחליף צבעים בצורה חלקה)
        int color1 = Color.parseColor("#6495ED"); // CornflowerBlue
        int color2 = Color.parseColor("#818CF8"); // Indigo/Purple
        int color3 = Color.parseColor("#2DD4BF"); // Teal/Mint

        colorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), color1, color2, color3, color1);
        colorAnimator.setDuration(2000); // מחזור צבעים של 4 שניות
        colorAnimator.setRepeatCount(ValueAnimator.INFINITE);
        colorAnimator.addUpdateListener(animator -> {
            int color = (int) animator.getAnimatedValue();
            if (pbSpinner != null) {
                pbSpinner.setIndeterminateTintList(ColorStateList.valueOf(color));
            }
        });
        colorAnimator.start();

        updateLoadingText();
    }

    private void updateLoadingText() {
        if (loadingOverlay != null && loadingOverlay.getVisibility() == View.VISIBLE) {
            // אנימציית Fade Out לטקסט הישן
            tvLoadingMessage.animate().alpha(0f).setDuration(500).withEndAction(() -> {
                // שינוי הטקסט כשהוא שקוף
                tvLoadingMessage.setText(loadingPhrases[phraseIndex % loadingPhrases.length]);
                phraseIndex++;
                // אנימציית Fade In לטקסט החדש
                tvLoadingMessage.animate().alpha(1f).setDuration(500).start();
            }).start();

            // הגדלנו ל-4 שניות לכל משפט כדי לתת למשתמש זמן לקרוא
            loadingHandler.postDelayed(this::updateLoadingText, 4000);
        }
    }

    private void stopAiLoading() {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(View.GONE);
        }
        if (colorAnimator != null) {
            colorAnimator.cancel();
        }
        loadingHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAiLoading();
    }
}