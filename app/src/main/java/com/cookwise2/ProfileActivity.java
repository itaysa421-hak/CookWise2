package com.cookwise2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
    private final List<String> currentUserSavedIds = new ArrayList<>();
    private String profileUid; // ה-UID של הפרופיל שאותו מציגים
    private boolean isMyOwnProfile;
    private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // בדיקה: האם הגענו לפרופיל של מישהו אחר או לפרופיל האישי?
        String targetUid = getIntent().getStringExtra("EXTRA_USER_ID");
        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (targetUid != null && !targetUid.equals(myUid)) {
            profileUid = targetUid;
            isMyOwnProfile = false;
        } else {
            profileUid = myUid;
            isMyOwnProfile = true;
        }

        tabLayout = findViewById(R.id.tabLayout);
        if (!isMyOwnProfile) {
            tabLayout.setVisibility(View.GONE); // מסתירים טאבים בפרופיל של אחרים
        }

        setupProfileInfo();
        initRecyclerView();
        setupTabs();

        // טעינת מזהי השמורים של המשתמש המחובר (כדי שהכפתור יראה נכון בפיד שלהם)
        fetchCurrentUserSavedIds();
    }

    private void initRecyclerView() {
        recyclerView = findViewById(R.id.recycler_profile);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    updateAdapter(myPosts);
                } else {
                    if (savedPosts.isEmpty() && !currentUserSavedIds.isEmpty()) {
                        loadSavedPosts();
                    } else {
                        updateAdapter(savedPosts);
                    }
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void fetchCurrentUserSavedIds() {
        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("users").document(myUid).get().addOnSuccessListener(doc -> {
            List<String> ids = (List<String>) doc.get("savedPosts");
            currentUserSavedIds.clear();
            if (ids != null) {
                for (String id : ids) {
                    if (id != null) currentUserSavedIds.add(id);
                }
            }
            // אחרי שיש לנו את השמורים שלנו, נטען את המתכונים של הפרופיל הנוכחי
            loadProfilePosts();
        });
    }

    private void loadProfilePosts() {
        FirebaseFirestore.getInstance().collection("posts")
                .whereEqualTo("ownerUid", profileUid)
                .get()
                .addOnSuccessListener(snapshots -> {
                    myPosts.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        RecipePost post = doc.toObject(RecipePost.class);
                        post.setId(doc.getId());
                        myPosts.add(post);
                    }
                    ((TextView) findViewById(R.id.tv_recipe_count)).setText(String.valueOf(myPosts.size()));
                    updateAdapter(myPosts);
                });
    }

    private void loadSavedPosts() {
        if (currentUserSavedIds.isEmpty()) {
            updateAdapter(new ArrayList<>());
            return;
        }

        FirebaseFirestore.getInstance().collection("posts")
                .whereIn(FieldPath.documentId(), currentUserSavedIds)
                .get()
                .addOnSuccessListener(snapshots -> {
                    savedPosts.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        RecipePost post = doc.toObject(RecipePost.class);
                        post.setId(doc.getId());
                        savedPosts.add(post);
                    }
                    updateAdapter(savedPosts);
                });
    }

    private void updateAdapter(List<RecipePost> listToShow) {
        // שים לב: אנחנו תמיד מעבירים את currentUserSavedIds כדי שנוכל לשמור מתכונים של אחרים מהפרופיל שלהם
        PostsAdapter postsAdapter = new PostsAdapter(listToShow, currentUserSavedIds, post -> {
            Intent intent = new Intent(this, RecipeDetailsActivity.class);
            intent.putExtra("RECIPE_POST", post);
            startActivity(intent);
        });

        // טיפול בלחיצה על שם משתמש בתוך הפרופיל (אם לוחצים על שם אחר בתוך רשימה)
        postsAdapter.setOnUserClickListener(uid -> {
            if (!uid.equals(profileUid)) {
                Intent intent = new Intent(this, ProfileActivity.class);
                intent.putExtra("EXTRA_USER_ID", uid);
                startActivity(intent);
            }
        });

        recyclerView.setAdapter(postsAdapter);
    }

    private void setupProfileInfo() {
        ImageView ivProfile = findViewById(R.id.iv_profile_image);
        TextView tvName = findViewById(R.id.tv_profile_name);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        FirebaseFirestore.getInstance().collection("users").document(profileUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("nickname");
                        tvName.setText(name != null ? name : "Chef");

                        String imageUrl = "https://wkxapzreydqpqsthggzk.supabase.co/storage/v1/object/public/my-bucket/images/profile-pics/" + profileUid + ".jpg";

                        Glide.with(this)
                                .load(imageUrl)
                                .placeholder(R.drawable.cookwise_logo)
                                .circleCrop()
                                .into(ivProfile);
                    }
                });
    }
}