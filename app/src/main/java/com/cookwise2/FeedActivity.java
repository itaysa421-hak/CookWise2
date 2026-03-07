package com.cookwise2;

import static com.google.firebase.firestore.DocumentChange.Type.ADDED;
import static com.google.firebase.firestore.DocumentChange.Type.MODIFIED;
import static com.google.firebase.firestore.DocumentChange.Type.REMOVED;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cookwise2.utils.GeminiManager;
import com.cookwise2.utils.PostsAdapter;
import com.cookwise2.utils.RecipePost;
import com.cookwise2.utils.UserImageSelector;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FeedActivity extends AppCompatActivity {
    private static final String TAG = "FeedActivity";

    String nickname;

    private RecyclerView recyclerView;
    private PostsAdapter postsAdapter;
    private List<RecipePost> posts;
    private List<RecipePost> allPosts; // הרשימה המקורית המלאה
    private String currentSearchQuery = "";
    private String currentFilterCategory = "All";
    UserImageSelector userImageSelector;
    private Dialog scannerDialog;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_feed);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        readUserData();
        TextView tvHelloUser = findViewById(R.id.tv_hello_user);
        tvHelloUser.setText("Hello, " + nickname + " 👋");

        posts = new ArrayList<>();

        initRecyclerView();
        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                applyFilters();
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        com.google.android.material.chip.ChipGroup chipGroup = findViewById(R.id.chipGroupFilters);
        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            com.google.android.material.chip.Chip chip = findViewById(checkedId);
            if (chip != null) {
                currentFilterCategory = chip.getText().toString();
                applyFilters();
            }
        });
        registerToNewPosts();

        ImageButton buttonLogout = findViewById(R.id.buttonLogout);
        buttonLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(FeedActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        Button buttonAddPost = findViewById(R.id.button_addPost);
        buttonAddPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FeedActivity.this, AddPostActivity.class);
                startActivity(intent);
            }
        });


// 2. חיבור הכפתור ב-onCreate
        userImageSelector = new UserImageSelector(this, null);
        userImageSelector.setOnImageSelectedListener(new UserImageSelector.ImageSelectionListener() {
            @Override
            public void onImageSelected(Uri uri) {
                // הקוד הזה ירוץ רק אחרי שהמשתמש בחר תמונה וה-URI זמין
                Log.d(TAG, "Image selected! URI: " + uri);
                processImageWithAi(uri);
            }
        });
        FloatingActionButton fabScanner = findViewById(R.id.fabAiScanner);
        fabScanner.setOnClickListener(v -> {
            userImageSelector.showImageSourceDialog();


        });

        //הפונקציה להעלמת הכפתורים הצפים
        setupFabScrollBehavior();

    }

    private void processImageWithAi(Uri imageUri) {
        try {
            InputStream imageStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(imageStream);

            scannerDialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
            scannerDialog.setContentView(R.layout.dialog_ai_scanner);
            scannerDialog.setCancelable(false);

            android.widget.ImageView ivScan = scannerDialog.findViewById(R.id.ivScanImage);
            View scannerLine = scannerDialog.findViewById(R.id.vScannerLine);
            TextView tvStatus = scannerDialog.findViewById(R.id.tvAiStatus);

            ivScan.setImageBitmap(bitmap);

            // אנימציית הלייזר
            android.animation.ObjectAnimator animator = android.animation.ObjectAnimator.ofFloat(
                    scannerLine, "translationY", 0f, 1100f); // הגדלתי מעט את הטווח שיעבור את כל הכרטיס
            animator.setDuration(1500);
            animator.setRepeatMode(android.animation.ValueAnimator.REVERSE);
            animator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            animator.start();

            // --- התוספת החדשה: אנימציית הנקודות המהבהבות ---
            android.os.Handler dotsHandler = new android.os.Handler();
            Runnable dotsRunnable = new Runnable() {
                int count = 0;
                @Override
                public void run() {
                    String dots = "";
                    for (int i = 0; i < count; i++) dots += ".";
                    tvStatus.setText("AI ANALYZING" + dots);
                    count = (count + 1) % 4; // מחזוריות של 0, 1, 2, 3
                    dotsHandler.postDelayed(this, 500); // עדכון כל חצי שנייה
                }
            };
            dotsHandler.post(dotsRunnable);

            scannerDialog.show();

            String prompt = "Analyze this image of food. Identify the dish and provide a professional recipe. " +
                    "Return ONLY a JSON object: {\"title\": \"...\", \"ingredients\": [\"...\"], \"instructions\": \"...\"}";

            GeminiManager.getInstance().sendImageAndText(bitmap, prompt, this, new GeminiManager.GeminiCallback() {
                @Override
                public void onSuccess(String result) {
                    runOnUiThread(() -> {
                        dotsHandler.removeCallbacks(dotsRunnable); // עצירת האנימציה
                        if (scannerDialog != null && scannerDialog.isShowing()) scannerDialog.dismiss();
                        handleAiResult(result, imageUri);
                    });
                }

                @Override
                public void onError(Throwable error) {
                    runOnUiThread(() -> {
                        dotsHandler.removeCallbacks(dotsRunnable); // עצירת האנימציה
                        if (scannerDialog != null && scannerDialog.isShowing()) scannerDialog.dismiss();
                        Toast.makeText(FeedActivity.this, "AI Scan failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });

        } catch (Exception e) {
            if (scannerDialog != null && scannerDialog.isShowing()) scannerDialog.dismiss();
            e.printStackTrace();
        }
    }
    private void handleAiResult(String rawJson, Uri imageUri) {
        try {
            // ניקוי התוצאה (במידה וג'ימיני מוסיף תגיות Markdown)
            String cleanJson = rawJson.replace("```json", "").replace("```", "").trim();
            JSONObject json = new JSONObject(cleanJson);

            Intent intent = new Intent(this, AddPostActivity.class);
            intent.putExtra("ai_title", json.getString("title"));
            intent.putExtra("ai_instructions", json.getString("instructions"));
            intent.putExtra("uri_image", imageUri);

            JSONArray ingArray = json.getJSONArray("ingredients");
            ArrayList<String> ingList = new ArrayList<>();
            for (int i = 0; i < ingArray.length(); i++) {
                ingList.add(ingArray.getString(i));
            }
            intent.putStringArrayListExtra("ai_ingredients", ingList);

            startActivity(intent);

        } catch (Exception e) {
            Toast.makeText(this, "AI response was not clear enough, try another photo", Toast.LENGTH_SHORT).show();
        }
    }
    private void readUserData(){
        Log.d(TAG, "readUserData: start");
        //about to read data from userInfo.xml
        SharedPreferences sharedPreferences = getSharedPreferences("userInfo", MODE_PRIVATE);

        // nickname - "N/A" is a default value if nickname is not found in the file
        nickname = sharedPreferences.getString("nickname", "N/A");
        Log.d(TAG, "readUserData: nickname: " + nickname);

    }
    private void initRecyclerView()
    {
        recyclerView = findViewById(R.id.recycler_posts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        postsAdapter = new PostsAdapter(posts, new PostsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(RecipePost post) {
                Intent intent = new Intent(FeedActivity.this, RecipeDetailsActivity.class);
                intent.putExtra("RECIPE_POST", post); // מעביר את כל המתכון למסך הבא
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(postsAdapter);
    }

    private void applyFilters() {
        if (allPosts == null) return;

        List<RecipePost> filteredList = new ArrayList<>();

        for (RecipePost post : allPosts) {
            // 1. בדיקת חיפוש טקסט חופשי (כותרת)
            boolean matchesSearch = post.getTitle().toLowerCase().contains(currentSearchQuery.toLowerCase());

            // 2. בדיקת קטגוריה גנרית
            boolean matchesCategory = false;

            if (currentFilterCategory.equals("All")) {
                matchesCategory = true;
            } else {
                Map<String, Object> tags = post.getClassification();
                if (tags != null) {
                    // אנחנו רצים על כל הערכים שג'ימיני נתן (Vegan, Easy, Italian וכו')
                    for (Object value : tags.values()) {
                        String tagValue = String.valueOf(value);

                        // השוואה לטקסט של הצ'יפ הנבחר (ללא הבדל ברישיות)
                        if (tagValue.equalsIgnoreCase(currentFilterCategory)) {
                            matchesCategory = true;
                            break; // מצאנו התאמה אחת, מספיק לנו
                        }
                    }
                }
            }

            if (matchesSearch && matchesCategory) {
                filteredList.add(post);
            }
        }

        // עדכון ה-RecyclerView דרך האדפטור
        posts.clear();
        posts.addAll(filteredList);
        postsAdapter.notifyDataSetChanged();
    }

    private void registerToNewPosts() {
        allPosts = new ArrayList<>(); // אתחול הרשימה המקורית
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("posts")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        RecipePost post = dc.getDocument().toObject(RecipePost.class);
                        post.setId(dc.getDocument().getId());

                        switch (dc.getType()) {
                            case ADDED:
                                allPosts.add(0, post); // הוספה להתחלה
                                break;
                            case MODIFIED:
                                // עדכון פוסט קיים ברשימה המקורית
                                for (int i = 0; i < allPosts.size(); i++) {
                                    if (allPosts.get(i).getId().equals(post.getId())) {
                                        allPosts.set(i, post);
                                        break;
                                    }
                                }
                                break;
                            case REMOVED:
                                allPosts.removeIf(p -> p.getId().equals(post.getId()));
                                break;
                        }
                    }
                    applyFilters(); // בכל שינוי ב-DB, נפעיל מחדש את הסינון הנוכחי
                });
    }
    private void setupFabScrollBehavior() {
        FloatingActionButton fabAiScanner = findViewById(R.id.fabAiScanner);
        com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton btnAddPost = findViewById(R.id.button_addPost);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                // dy > 0 אומר שהמשתמש גולל למטה
                if (dy > 0) {
                    if (fabAiScanner.isShown()) {
                        fabAiScanner.hide();
                    }
                    if (btnAddPost.isExtended()) {
                        btnAddPost.shrink(); // קודם כל נצמצם את הכפתור רק לאייקון
                    }
                    // אחרי חצי שנייה של גלילה, נחביא אותו לגמרי
                    btnAddPost.hide();
                }

                // dy < 0 אומר שהמשתמש גולל למעלה
                else if (dy < 0) {
                    if (!fabAiScanner.isShown()) {
                        fabAiScanner.show();
                    }
                    if (!btnAddPost.isShown()) {
                        btnAddPost.show();
                        btnAddPost.extend(); // נחזיר את הטקסט "Add Post"
                    }
                }
            }
        });
    }




}