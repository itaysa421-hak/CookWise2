package com.cookwise2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cookwise2.utils.PostsAdapter;
import com.cookwise2.utils.RecipePost;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private final List<RecipePost> myPosts = new ArrayList<>();
    private final List<RecipePost> savedPosts = new ArrayList<>();
    private final List<String> savedPostIds = new ArrayList<>();
    private String currentUid;
    private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        setupProfileInfo(); // הפונקציה שכבר תיקנו קודם
        initRecyclerView();
        setupTabs();

        // טעינה ראשונית של הנתונים
        fetchUserSavedIdsAndMyPosts();
    }

    private void initRecyclerView() {
        recyclerView = findViewById(R.id.recycler_profile);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // בהתחלה נציג רשימה ריקה, היא תתעדכן כשנמשוך נתונים
    }

    private void setupTabs() {
        tabLayout = findViewById(R.id.tabLayout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    // הצגת המתכונים שלי
                    updateAdapter(myPosts);
                } else {
                    // הצגת השמורים
                    if (savedPosts.isEmpty() && !savedPostIds.isEmpty()) {
                        loadSavedPosts(); // טעינה מה-DB רק אם עוד לא טענו
                    } else {
                        updateAdapter(savedPosts);
                    }
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void fetchUserSavedIdsAndMyPosts() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(currentUid).get().addOnSuccessListener(doc -> {
            List<String> ids = (List<String>) doc.get("savedPosts");
            savedPostIds.clear();

            if (ids != null) {
                // סינון: רק מזהים שאינם null ואינם ריקים
                for (String id : ids) {
                    if (id != null && !id.trim().isEmpty()) {
                        savedPostIds.add(id);
                    }
                }
            }

            // עכשיו המספר יהיה מדויק
            TextView tvSavedCount = findViewById(R.id.tv_saved_count);
            tvSavedCount.setText(String.valueOf(savedPostIds.size()));

            loadMyPosts();
        });
    }

    private void loadMyPosts() {
        FirebaseFirestore.getInstance().collection("posts")
                .whereEqualTo("ownerUid", currentUid)
                .get()
                .addOnSuccessListener(snapshots -> {
                    myPosts.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        RecipePost post = doc.toObject(RecipePost.class);
                        post.setId(doc.getId());
                        myPosts.add(post);
                    }
                    ((TextView) findViewById(R.id.tv_recipe_count)).setText(String.valueOf(myPosts.size()));

                    // כברירת מחדל מציגים את הטאב הראשון (My Recipes)
                    if (tabLayout.getSelectedTabPosition() == 0) {
                        updateAdapter(myPosts);
                    }
                });
    }

    private void loadSavedPosts() {
        // 1. הגנה בסיסית - האם הרשימה בכלל קיימת?
        if (savedPostIds == null || savedPostIds.isEmpty()) {
            updateAdapter(new ArrayList<>());
            return;
        }

        // 2. ניקוי הרשימה - מוודאים שאין ערכי null או מחרוזות ריקות
        List<String> cleanIds = new ArrayList<>();
        for (String id : savedPostIds) {
            if (id != null && !id.trim().isEmpty()) {
                cleanIds.add(id);
            }
        }

        // 3. בדיקה נוספת - האם נשארו מזהים תקינים אחרי הניקוי?
        if (cleanIds.isEmpty()) {
            updateAdapter(new ArrayList<>());
            return;
        }

        // 4. הגבלת כמות (מגבלת Firestore של 30 פריטים ב-whereIn)
        if (cleanIds.size() > 30) {
            cleanIds = cleanIds.subList(0, 30);
        }

        // 5. ביצוע השאילתה עם הרשימה הנקייה
        FirebaseFirestore.getInstance().collection("posts")
                .whereIn(FieldPath.documentId(), cleanIds)
                .get()
                .addOnSuccessListener(snapshots -> {
                    savedPosts.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        RecipePost post = doc.toObject(RecipePost.class);
                        post.setId(doc.getId());
                        savedPosts.add(post);
                    }
                    updateAdapter(savedPosts);
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileActivity", "Error loading saved posts", e);
                    Toast.makeText(this, "Error loading saved recipes", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateAdapter(List<RecipePost> listToShow) {
        PostsAdapter postsAdapter = new PostsAdapter(listToShow, savedPostIds, post -> {
            Intent intent = new Intent(this, RecipeDetailsActivity.class);
            intent.putExtra("RECIPE_POST", post);
            startActivity(intent);
        });

        // הגדרת המאזין לשינויים במספר השמורים
        postsAdapter.setOnSavedStatusChangedListener(new PostsAdapter.OnSavedStatusChangedListener() {
            @Override
            public void onSavedStatusChanged(int newCount) {
                // עדכון ה-TextView בזמן אמת
                TextView tvSavedCount = findViewById(R.id.tv_saved_count);
                tvSavedCount.setText(String.valueOf(newCount));
            }
        });

        // בדיקה אם אנחנו בטאב השני (השמורים)
        boolean isShowingSaved = (tabLayout.getSelectedTabPosition() == 1);
        postsAdapter.setIsSavedTab(isShowingSaved);

        recyclerView.setAdapter(postsAdapter);
    }

    private void setupProfileInfo() {
        // 1. גישה ל-Views
        ImageView ivProfile = findViewById(R.id.iv_profile_image);
        TextView tvName = findViewById(R.id.tv_profile_name);
        ImageButton btnBack = findViewById(R.id.btn_back);

        // 2. כפתור חזור פשוט
        btnBack.setOnClickListener(v -> finish());

        // 3. משיכת נתונים מ-Firestore
        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("users").document(currentUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // עדכון השם
                        String name = documentSnapshot.getString("nickname");
                        tvName.setText(name != null ? name : "Chef");

                        // עדכון התמונה בעזרת Glide (הנתיב ב-Supabase שמרנו לפי UID)
                        // הערה: אם שמרת את ה-URL בתוך ה-Document, משוך אותו כאן
                        String imageUrl = "https://your-supabase-url.com/storage/v1/object/public/images/profile-pics/" + currentUid + ".jpg";

                        Glide.with(this)
                                .load(imageUrl)
                                .placeholder(R.drawable.cookwise_logo) // תמונת ברירת מחדל
                                .circleCrop()
                                .into(ivProfile);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileActivity", "Error loading profile", e);
                    tvName.setText("Error loading profile");
                });
    }
}