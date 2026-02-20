package com.cookwise2;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.cookwise2.utils.GeminiManager;
import com.cookwise2.utils.SupabaseStorageHelper;
import com.cookwise2.utils.UserImageSelector;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.cookwise2.utils.RecipePost;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

public class AddPostActivity extends AppCompatActivity {

    TextView title;
    TextView content;
    MaterialCardView cardRecipeImage;
    private ImageView ivRecipeImage;
    UserImageSelector userImageSelector;

    private LinearLayout ingredientsContainer;
    private Button btnAddIngredient;
    private LinearLayout imagePlaceholderContainer;


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
        imagePlaceholderContainer = findViewById(R.id.imagePlaceholderContainer);
        content = findViewById(R.id.etPostContent);
        ingredientsContainer = findViewById(R.id.ingredientsContainer);
        btnAddIngredient = findViewById(R.id.btnAddIngredient);

        ivRecipeImage = findViewById(R.id.ivRecipeImage);
        // בתוך ה-onCreate, אחרי שזיהית את הכפתור:
        View btnAi = findViewById(R.id.btnAiAssistant);

        MaterialCardView cardAi = findViewById(R.id.cardAiContainer);

// יצירת אנימטור שמשנה את צבע המסגרת (Stroke) בין כחול לסגול/ורוד
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(),
                Color.parseColor("#6495ED"), // CornflowerBlue
                Color.parseColor("#A855F7"), // Purple AI
                Color.parseColor("#EC4899"), // Pink AI
                Color.parseColor("#6495ED")); // חזרה לכחול

        colorAnimation.setDuration(3000); // 3 שניות לסיבוב צבע מלא
        colorAnimation.setRepeatCount(ValueAnimator.INFINITE);

        colorAnimation.addUpdateListener(animator -> {
            cardAi.setStrokeColor((int) animator.getAnimatedValue());
            // הוספת "זהירה" עדינה בעזרת שינוי ה-Elevation
            float phase = animator.getAnimatedFraction();
            cardAi.setCardElevation(4f + (float) Math.sin(phase * Math.PI * 2) * 2f);

        });
        colorAnimation.start();

        if (getIntent().hasExtra("ai_title")) {
            title.setText(getIntent().getStringExtra("ai_title"));
            content.setText(getIntent().getStringExtra("ai_instructions"));
            ivRecipeImage.setImageURI(getIntent().getParcelableExtra("uri_image"));
            imagePlaceholderContainer.setVisibility(View.GONE);


            ArrayList<String> ingredients = getIntent().getStringArrayListExtra("ai_ingredients");
            if (ingredients != null) {
                for (String ing : ingredients) {
                    addIngredientRowFromAi(ing); // הפונקציה שכבר כתבנו קודם
                }
            }
        }

        btnAi.setOnClickListener(v -> {
            Intent intent = new Intent(AddPostActivity.this, AiGeneratorActivity.class);
            aiLauncher.launch(intent);
        });

        userImageSelector = new UserImageSelector(this, ivRecipeImage);
        cardRecipeImage = findViewById(R.id.cardRecipeImage);
        cardRecipeImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userImageSelector.showImageSourceDialog();
                imagePlaceholderContainer.setVisibility(View.GONE);

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
    private final ActivityResultLauncher<Intent> aiLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();

                    // 1. מילוי כותרת
                    String aiTitle = data.getStringExtra("ai_title");
                    if (aiTitle != null) {
                        ((EditText)findViewById(R.id.etPostTitle)).setText(aiTitle);
                    }

                    // 2. מילוי הוראות הכנה
                    String aiInstructions = data.getStringExtra("ai_instructions");
                    if (aiInstructions != null) {
                        ((EditText)findViewById(R.id.etPostContent)).setText(aiInstructions);
                    }

                    // 3. מילוי מצרכים (דורש לולאה כי זו רשימה)
                    ArrayList<String> aiIngredients = data.getStringArrayListExtra("ai_ingredients");
                    if (aiIngredients != null) {
                        LinearLayout container = findViewById(R.id.ingredientsContainer);
                        container.removeAllViews(); // מנקה את מה שהיה קודם
                        for (String ingredient : aiIngredients) {
                            addIngredientRowFromAi(ingredient);
                        }
                    }
                }
            }
    );

    public void sendPost() {
        Log.d(TAG, "sendPost: start");



        createPostAndThenUploadImage();
    }

    public RecipePost createRecipePost(){
        String titleStr = this.title.getText().toString();
        String description = this.content.getText().toString();
        String ownerId =  FirebaseAuth.getInstance().getCurrentUser().getUid();
        ArrayList<String> groceries = collectIngredients();




        SharedPreferences sharedPreferences = getSharedPreferences("userInfo", MODE_PRIVATE);
        // nickname - "N/A" is a default value if nickname is not found in the file
        String nickname = sharedPreferences.getString("nickname", "N/A");

        Timestamp createdAt = new Timestamp(new Date());

        return new RecipePost(titleStr, description, groceries , ownerId, nickname, createdAt);
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
    private void uploadImageAndThenPublish(String newPostId) {
        File imageFile = userImageSelector.createImageFile();

        // אם המשתמש לא בחר תמונה, עוברים ישר לפרסום הפוסט
        if (imageFile == null) {
            return;
        }

        String filename = "images/post-pic/" + newPostId + ".jpg";
        Log.i(TAG, "Uploading file to Supabase: " + filename);

        SupabaseStorageHelper.uploadPicture(imageFile, filename, new SupabaseStorageHelper.OnResultCallback() {
            @Override
            public void onResult(boolean success, String url, String error) {
                if (success) {
                    Log.i(TAG, "Image uploaded successfully: " + url);
                    Map<String, Object> update = new HashMap<>();
                    update.put("imageUrl", url);
                    FirebaseFirestore.getInstance()
                            .collection("posts")
                            .document(newPostId).update(update);

                } else {
                    Log.e(TAG, "Supabase upload failed: " + error);
                    // גם אם התמונה נכשלה, כנראה תרצה שהפוסט יעלה בכל זאת
                }
            }
        });
    }
    private void createPostAndThenUploadImage() {
        RecipePost post = createRecipePost();
        // כאן תוכל להוסיף את ה-imageUrl לאובייקט הפוסט אם הוספת שדה כזה במחלקה

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("posts")
                .add(post)
                .addOnSuccessListener(documentReference -> {
                    String newPostId = documentReference.getId();

                    uploadImageAndThenPublish(newPostId);
                    geminiTaggingInBackground(newPostId, post);


                    Toast.makeText(AddPostActivity.this, "המתכון עלה בהצלחה!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(AddPostActivity.this, FeedActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding document", e);
                    Toast.makeText(AddPostActivity.this, "שגיאה בפרסום הפוסט", Toast.LENGTH_SHORT).show();
                });
    }
    private Map<String, Object> parseJsonToMap(String jsonString) {
        Map<String, Object> map = new HashMap<>();
        try {
            String cleanedJson = jsonString.replaceAll("```json|```", "").trim();
            JSONObject jsonObject = new JSONObject(cleanedJson);
            Iterator<String> keys = jsonObject.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                Object value = jsonObject.get(key);

                // המרה לסוגים בסיסיים כדי ש-Firestore לא יתבלבל
                if (value instanceof String) {
                    map.put(key, value.toString());
                } else if (value instanceof Integer) {
                    map.put(key, (Integer) value);
                } else if (value instanceof Boolean) {
                    map.put(key, (Boolean) value);
                } else if (value instanceof Double) {
                    map.put(key, (Double) value);
                } else if (value instanceof Long) {
                    map.put(key, (Long) value);
                } else {
                    // לכל מקרה אחר, שמור כטקסט
                    map.put(key, value.toString());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Parsing error: " + e.getMessage());
        }
        return map;
    }
    private String groceriesToString(ArrayList<String> groceries)
    {
        String str = "";
        if(groceries ==  null)
            return str;
        for (int i = 0; i < groceries.size() ; i++){
            str += groceries.get(i);
            if(i < groceries.size() - 1)
                str += ", ";
        }
        return str;
    }

    private void geminiTaggingInBackground(String documentId, RecipePost post) {
        // הכנת הפרומפט מהאובייקט שכבר קיים (לא מה-EditText כי הם יושמדו)
        String prompt = getPrompt(post.getTitle(), post.getDescription(), post.getGroceries());

        GeminiManager gemini = GeminiManager.getInstance();

        // חשוב: משתמשים ב-getApplicationContext()
        gemini.sendText(prompt, getApplicationContext(), new GeminiManager.GeminiCallback() {
            @Override
            public void onSuccess(String result) {
                String cleanedJson = result.replace("```json", "").replace("```", "").trim();
                try {
                    Map<String, Object> newMap = parseJsonToMap(cleanedJson);
                    if (newMap != null && !newMap.isEmpty()) {
                        FirebaseFirestore.getInstance().collection("posts").document(documentId)
                                .update("classification", newMap)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "AI Tagging completed for: " + documentId))
                                .addOnFailureListener(e -> Log.e(TAG, "AI Tagging failed in background", e));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing background JSON", e);
                }
            }

            @Override
            public void onError(Throwable error) {
                Log.e(TAG, "Gemini background error: ", error);
            }
        });
    }


    private String getPrompt(String title, String description, ArrayList<String> groceries){

        String ingredients = groceriesToString(groceries);
        String prompt = "Act as a professional culinary data analyst. Analyze the following recipe details and return EXACTLY one JSON object.\n" +
                "\n" +
                "RECIPE DETAILS:\n" +
                "Title: ["+title+"]\n" +
                "Ingredients: ["+ingredients+"]\n" +
                "Instructions: ["+description+"]\n" +
                "\n" +
                "STRICT JSON SCHEMA:\n" +
                "{\n" +
                "  \"difficulty\": \"Easy\" | \"Medium\" | \"Hard\" | \"none\",\n" +
                "  \"estimated_time\": number | \"none\", \n" +
                "  \"meal_type\": \"Breakfast\" | \"Lunch\" | \"Dinner\" | \"Dessert\" | \"Snack\" | \"none\",\n" +
                "  \"dietary_info\": \"Vegan\" | \"Vegetarian\" | \"Gluten-Free\" | \"Dairy-Free\" | \"Meat\" | \"none\",\n" +
                "  \"spiciness\": 0 | 1 | 2 | 3 | \"none\", \n" +
                "  \"cooking_method\": \"Baking\" | \"Frying\" | \"Slow Cooking\" | \"No-Cook\" | \"Boiling\" | \"none\",\n" +
                "  \"cuisine\": \"Italian\" | \"Asian\" | \"Mediterranean\" | \"Middle Eastern\" | \"American\" | \"Other\" | \"none\",\n" +
                "  \"budget_friendly\": \"Cheap\" | \"Standard\" | \"Expensive\" | \"none\",\n" +
                "  \"health_score\": number | \"none\", \n" +
                "  \"target_audience\": \"Kids\" | \"Hosting\" | \"Students\" | \"Quick Meal\" | \"none\"\n" +
                "}\n" +
                "\n" +
                "IMPORTANT RULES:\n" +
                "1. Return ONLY the JSON object. No preamble, no markdown code blocks, and no closing remarks.\n" +
                "2. CRITICAL: If you cannot confidently determine a value for a field based on the provided information, do NOT guess. Set the value to \"none\".\n" +
                "3. Analyze the \"Instructions\" carefully to determine the \"cooking_method\" and \"estimated_time\".\n" +
                "4. Use English for all string values.\n" +
                "5. Ensure the output is valid JSON.";
        return prompt;

    }
    private void addIngredientRowFromAi(String ingredientName) {
        LinearLayout container = findViewById(R.id.ingredientsContainer);

        // ניצול הלוגיקה הקיימת שלך להוספת שורה (בהנחה שיש לך Layout לשורה)
        LayoutInflater inflater = LayoutInflater.from(this);
        View rowView = inflater.inflate(R.layout.row_ingredient, null); // וודא שזה השם של ה-XML של השורה

        EditText etName = rowView.findViewById(R.id.etIngredientName);
        etName.setText(ingredientName);

        // חיבור כפתור המחיקה לשורה החדשה
        rowView.findViewById(R.id.btnRemoveIngredient).setOnClickListener(v ->
                container.removeView(rowView)
        );

        container.addView(rowView);
    }

}