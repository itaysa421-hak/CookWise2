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
import com.cookwise2.utils.GeminiManager;
import com.cookwise2.utils.SupabaseStorageHelper;
import com.cookwise2.utils.UserImageSelector;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
                    String newPostId = documentReference.getId(); // לוקחים את ה-ID האמיתי ש-Firestore יצר
                    Log.d(TAG, "DocumentSnapshot written with ID: " + newPostId);
                    Toast.makeText(AddPostActivity.this, "Post published successfully!", Toast.LENGTH_SHORT).show();
                    this.postId = newPostId;

                    geminiTagging(newPostId);

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
        this.postId = ownerId + "-" + String.valueOf(rnd.nextInt(1000000000));


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
    private Map<String, Object> parseJsonToMap(String jsonString) {
        Map<String, Object> map = new HashMap<>();
        try {
            // ניקוי תגיות Markdown אם Gemini הוסיף אותן (לפעמים הוא מוסיף ```json )
            String cleanedJson = jsonString.replaceAll("```json|```", "").trim();
            JSONObject jsonObject = new JSONObject(cleanedJson);
            Iterator<String> keys = jsonObject.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                map.put(key, jsonObject.get(key));
            }
        } catch (Exception e) {
            e.printStackTrace();
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

    private void geminiTagging(String documentId) {
        // בניית הפרומפט בעזרת הפונקציות שבנינו
        String recipeTitle = title.getText().toString();
        String recipeInstructions = content.getText().toString();
        ArrayList<String> ingredientsStr = collectIngredients();

        String prompt = getPrompt(recipeTitle, recipeInstructions, ingredientsStr);

        GeminiManager gemini = GeminiManager.getInstance();

        // מומלץ להציג ProgressBar כאן אם יש לך

        gemini.sendText(prompt, this, new GeminiManager.GeminiCallback() {
            @Override
            public void onSuccess(String result) {
                // 1. ניקוי התוצאה (מסיר תגיות Markdown של JSON אם קיימות)
                String cleanedJson = result.replace("```json", "").replace("```", "").trim();

                Log.d(TAG, "Cleaned JSON: " + cleanedJson);

                try {
                    // 2. המרה למפה (כאן יכולה להיות שגיאת Parsing אם ה-JSON לא תקין)
                    Map<String, Object> newMap = parseJsonToMap(cleanedJson);

                    if (newMap == null || newMap.isEmpty()) {
                        Log.e(TAG, "Parsing failed: Map is empty");
                        return;
                    }

                    // 3. עדכון Firestore
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    db.collection("posts").document(documentId)
                            .update("classification", newMap)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Classification updated successfully for post: " + postId);
                                // כאן אפשר להסתיר את ה-ProgressBar או לעדכן את ה-UI

                                Toast.makeText(AddPostActivity.this, "Classification updated successfully!", Toast.LENGTH_SHORT).show();
                                finish(); // Close this activity and return to FeedActivity


                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Firestore update failed", e);
                            });

                } catch (Exception e) {
                    Log.e(TAG, "Error in processing Gemini result", e);
                }
            }

            @Override
            public void onError(Throwable error) {
                Log.e(TAG, "onError: ", error);
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

}