package com.cookwise2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.view.ViewCompat;
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
    private LinearLayout layoutEmptyState;
    private TextView tvEmptyMessage;
    private final List<RecipePost> myPosts = new ArrayList<>();
    private final List<RecipePost> savedPosts = new ArrayList<>();
    private final List<String> currentUserSavedIds = new ArrayList<>();
    private String profileUid;
    private boolean isMyOwnProfile;
    private TabLayout tabLayout;
    private ImageView ivProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        ivProfile = findViewById(R.id.iv_profile_image);
        // הגדרת שם טרנזישן תואם לפיד
        ViewCompat.setTransitionName(ivProfile, "profile_image_transition");

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
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        tvEmptyMessage = findViewById(R.id.tv_empty_message);

        if (!isMyOwnProfile) tabLayout.setVisibility(View.GONE);

        setupProfileInfo();
        initRecyclerView();
        setupTabs();
        fetchCurrentUserSavedIds();
    }

    private void initRecyclerView() {
        recyclerView = findViewById(R.id.recycler_profile);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) updateAdapter(myPosts);
                else {
                    if (savedPosts.isEmpty() && !currentUserSavedIds.isEmpty()) loadSavedPosts();
                    else updateAdapter(savedPosts);
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
                for (String id : ids) if (id != null) currentUserSavedIds.add(id);
            }
            ((TextView) findViewById(R.id.tv_saved_count)).setText(String.valueOf(currentUserSavedIds.size()));
            loadProfilePosts();
        });
    }

    private void loadProfilePosts() {
        FirebaseFirestore.getInstance().collection("posts").whereEqualTo("ownerUid", profileUid).get().addOnSuccessListener(snapshots -> {
            myPosts.clear();
            for (QueryDocumentSnapshot doc : snapshots) {
                RecipePost post = doc.toObject(RecipePost.class);
                post.setId(doc.getId());
                myPosts.add(post);
            }
            ((TextView) findViewById(R.id.tv_recipe_count)).setText(String.valueOf(myPosts.size()));
            if (tabLayout.getSelectedTabPosition() == 0) updateAdapter(myPosts);
        });
    }

    private void loadSavedPosts() {
        if (currentUserSavedIds.isEmpty()) { updateAdapter(new ArrayList<>()); return; }
        FirebaseFirestore.getInstance().collection("posts").whereIn(FieldPath.documentId(), currentUserSavedIds).get().addOnSuccessListener(snapshots -> {
            savedPosts.clear();
            for (QueryDocumentSnapshot doc : snapshots) {
                RecipePost post = doc.toObject(RecipePost.class);
                post.setId(doc.getId());
                savedPosts.add(post);
            }
            if (tabLayout.getSelectedTabPosition() == 1) updateAdapter(savedPosts);
        });
    }

    private void updateAdapter(List<RecipePost> listToShow) {
        if (listToShow.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
            tvEmptyMessage.setText(tabLayout.getSelectedTabPosition() == 1 ? "No saved recipes yet" : (isMyOwnProfile ? "No posts yet" : "Chef has no posts"));
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
            PostsAdapter adapter = new PostsAdapter(listToShow, currentUserSavedIds, post -> {
                Intent intent = new Intent(this, RecipeDetailsActivity.class);
                intent.putExtra("RECIPE_POST", post);
                int pos = listToShow.indexOf(post);
                View v = recyclerView.getLayoutManager().findViewByPosition(pos);
                if (v != null) {
                    ImageView iv = v.findViewById(R.id.iv_post_image);
                    ActivityOptionsCompat opt = ActivityOptionsCompat.makeSceneTransitionAnimation(this, iv, "recipe_image_transition");
                    startActivity(intent, opt.toBundle());
                } else startActivity(intent);
            });
            adapter.setOnUserClickListener(uid -> {
                if (!uid.equals(profileUid)) {
                    Intent intent = new Intent(this, ProfileActivity.class);
                    intent.putExtra("EXTRA_USER_ID", uid);
                    startActivity(intent);
                }
            });
            recyclerView.setAdapter(adapter);
        }
    }

    private void setupProfileInfo() {
        TextView tvName = findViewById(R.id.tv_profile_name);
        findViewById(R.id.btn_back).setOnClickListener(v -> supportFinishAfterTransition());
        FirebaseFirestore.getInstance().collection("users").document(profileUid).get().addOnSuccessListener(ds -> {
            if (ds.exists()) {
                tvName.setText(ds.getString("nickname"));
                String url = "https://wkxapzreydqpqsthggzk.supabase.co/storage/v1/object/public/my-bucket/images/profile-pics/" + profileUid + ".jpg";
                Glide.with(this).load(url).placeholder(R.drawable.cookwise_logo).circleCrop().into(ivProfile);
            }
        });
    }

    @Override
    public void onBackPressed() {
        // קריאה למימוש המקורי כדי למנוע את השגיאה ולשמור על התנהגות תקינה
        super.onBackPressed();
        // מבטיח שהטרנזישן יתבצע גם בלחיצה על כפתור החזור של המכשיר
        supportFinishAfterTransition();
    }}