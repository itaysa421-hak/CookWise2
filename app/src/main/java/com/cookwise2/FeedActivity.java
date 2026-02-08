package com.cookwise2;

import static com.google.firebase.firestore.DocumentChange.Type.ADDED;
import static com.google.firebase.firestore.DocumentChange.Type.MODIFIED;
import static com.google.firebase.firestore.DocumentChange.Type.REMOVED;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cookwise2.utils.PostsAdapter;
import com.cookwise2.utils.RecipePost;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class FeedActivity extends AppCompatActivity {
    private static final String TAG = "FeedActivity";

    String nickname;

    private RecyclerView recyclerView;
    private PostsAdapter postsAdapter;
    private List<RecipePost> posts;
    private List<RecipePost> allPosts; // הרשימה המקורית המלאה
    private String currentSearchQuery = "";
    private String currentFilterCategory = "All";


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

        Button buttonLogout = findViewById(R.id.buttonLogout);
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
        List<RecipePost> filteredList = new ArrayList<>();

        for (RecipePost post : allPosts) {
            // 1. בדיקת חיפוש טקסט (על הכותרת)
            boolean matchesSearch = post.getTitle().toLowerCase().contains(currentSearchQuery.toLowerCase());

            // 2. בדיקת קטגוריה (AI Classification)
            boolean matchesCategory = false;
            if (currentFilterCategory.equals("All")) {
                matchesCategory = true;
            } else {
                java.util.Map<String, Object> tags = post.getClassification();
                if (tags != null) {
                    // בדיקת התאמה לפי הקטגוריות שהגדרנו ב-Chips
                    String dietary = String.valueOf(tags.get("dietary_info"));
                    String difficulty = String.valueOf(tags.get("difficulty"));
                    String mealType = String.valueOf(tags.get("meal_type"));

                    matchesCategory = currentFilterCategory.equals(dietary) ||
                            currentFilterCategory.equals(difficulty) ||
                            currentFilterCategory.equals(mealType);
                }
            }

            if (matchesSearch && matchesCategory) {
                filteredList.add(post);
            }
        }

        // עדכון האדפטור עם הרשימה החדשה
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
                        switch (dc.getType()) {
                            case ADDED:
                                allPosts.add(0, post); // הוספה להתחלה
                                break;
                            case MODIFIED:
                                // עדכון פוסט קיים ברשימה המקורית
                                for (int i = 0; i < allPosts.size(); i++) {
                                    if (allPosts.get(i).getPostId().equals(post.getPostId())) {
                                        allPosts.set(i, post);
                                        break;
                                    }
                                }
                                break;
                            case REMOVED:
                                allPosts.removeIf(p -> p.getPostId().equals(post.getPostId()));
                                break;
                        }
                    }
                    applyFilters(); // בכל שינוי ב-DB, נפעיל מחדש את הסינון הנוכחי
                });
    }




}