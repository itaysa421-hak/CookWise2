package com.cookwise2;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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

    // רכיבי ממשק למצב Vision
    private TextInputLayout tilAiVision;
    private TextInputEditText etAiVision;

    // רכיבי ממשק למצב Pantry (שורות)
    private View nsvIngredients;
    private LinearLayout aiIngredientsContainer;

    // רכיבים כלליים
    private ProgressBar pbLoading;
    private MaterialButton btnGenerate;
    private MaterialButtonToggleGroup toggleAiMode;

    private boolean isPantryMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_generator);

        initViews();
        setupToggle();

        // הוספת שורה ראשונה כברירת מחדל
        addIngredientRow();

        btnGenerate.setOnClickListener(v -> generateRecipe());
    }

    private void initViews() {
        // אתחול רכיבי Vision
        tilAiVision = findViewById(R.id.tilAiVision);
        etAiVision = findViewById(R.id.etAiVision);

        // אתחול רכיבי Pantry
        nsvIngredients = findViewById(R.id.nsvIngredients);
        aiIngredientsContainer = findViewById(R.id.aiIngredientsContainer);
        MaterialButton btnAddRow = findViewById(R.id.btnAddIngredientRow);
        btnAddRow.setOnClickListener(v -> addIngredientRow());

        // אתחול כללי
        pbLoading = findViewById(R.id.pbLoading);
        btnGenerate = findViewById(R.id.btnGenerate);
        toggleAiMode = findViewById(R.id.toggleAiMode);
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

        pbLoading.setVisibility(View.VISIBLE);
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
                    pbLoading.setVisibility(View.GONE);
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

                setResult(RESULT_OK, intent);
                finish();

            } catch (Exception e) {
                pbLoading.setVisibility(View.GONE);
                btnGenerate.setEnabled(true);
                Toast.makeText(this, "Failed to parse AI response. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}