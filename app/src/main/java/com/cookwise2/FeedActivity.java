package com.cookwise2;

import static com.google.firebase.firestore.DocumentChange.Type.ADDED;
import static com.google.firebase.firestore.DocumentChange.Type.MODIFIED;
import static com.google.firebase.firestore.DocumentChange.Type.REMOVED;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cookwise2.utils.GeminiManager;
import com.cookwise2.utils.PostsAdapter;
import com.cookwise2.utils.RecipePost;
import com.cookwise2.utils.UserAccount;
import com.cookwise2.utils.UserAdapter;
import com.cookwise2.utils.UserImageSelector;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FeedActivity extends AppCompatActivity {
    private String nickname;
    private RecyclerView recyclerView;
    private PostsAdapter postsAdapter;
    private List<RecipePost> posts = new ArrayList<>();
    private List<RecipePost> allPosts = new ArrayList<>();
    private UserAdapter userAdapter;
    private List<UserAccount> usersList = new ArrayList<>();
    private List<UserAccount> allUsers = new ArrayList<>();
    private String currentSearchQuery = "";
    private String currentFilterCategory = "All";
    private UserImageSelector userImageSelector;
    private Dialog scannerDialog;
    private List<String> savedPostIds = new ArrayList<>();
    private EditText etSearch;
    private ImageView ivUserImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_feed);

        ivUserImage = findViewById(R.id.ivUserImage);
        // הגדרת שם טרנזישן לתמונה האישית שלך למעלה
        ViewCompat.setTransitionName(ivUserImage, "profile_image_transition");

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        readUserData();
        fetchSavedPosts();

        TextView tvUsername = findViewById(R.id.tvUsername);
        tvUsername.setText(nickname);

        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String imageUrl = "https://wkxapzreydqpqsthggzk.supabase.co/storage/v1/object/public/my-bucket/images/profile-pics/" + currentUid + ".jpg";

        Glide.with(this)
                .load(imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .circleCrop()
                .into(ivUserImage);

        initRecyclerView();
        registerToNewPosts();
        fetchAllUsers();

        etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                applyFilters();
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        com.google.android.material.chip.ChipGroup chipGroup = findViewById(R.id.chipGroupFilters);
        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            com.google.android.material.chip.Chip chip = findViewById(checkedId);
            if (chip != null) {
                currentFilterCategory = chip.getText().toString();
                applyFilters();
            }
        });

        findViewById(R.id.buttonLogout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(FeedActivity.this, LoginActivity.class));
            finish();
        });

        findViewById(R.id.userProfileContainer).setOnClickListener(v -> openUserProfile(currentUid, ivUserImage));

        findViewById(R.id.button_addPost).setOnClickListener(v -> showAddOptionsDialog());

        userImageSelector = new UserImageSelector(this, null);
        userImageSelector.setOnImageSelectedListener(this::processImageWithAi);

        findViewById(R.id.button_move_to_profile).setOnClickListener(v -> openUserProfile(currentUid, null));

        setupFabScrollBehavior();
    }

    private void initRecyclerView() {
        recyclerView = findViewById(R.id.recycler_posts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        postsAdapter = new PostsAdapter(posts, savedPostIds, post -> {
            Intent intent = new Intent(FeedActivity.this, RecipeDetailsActivity.class);
            intent.putExtra("RECIPE_POST", post);
            int position = posts.indexOf(post);
            View itemView = recyclerView.getLayoutManager().findViewByPosition(position);
            if (itemView != null) {
                ImageView sharedImageView = itemView.findViewById(R.id.iv_post_image);
                if (sharedImageView != null) {
                    ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                            this, sharedImageView, "recipe_image_transition");
                    startActivity(intent, options.toBundle());
                } else { startActivity(intent); }
            } else { startActivity(intent); }
        });

        postsAdapter.setOnUserClickListener(uid -> openUserProfile(uid, null));

        userAdapter = new UserAdapter(usersList, uid -> {
            int position = -1;
            for (int i = 0; i < usersList.size(); i++) {
                if (usersList.get(i).getUid().equals(uid)) {
                    position = i;
                    break;
                }
            }
            View itemView = recyclerView.getLayoutManager().findViewByPosition(position);
            if (itemView != null) {
                ImageView avatar = itemView.findViewById(R.id.ivUserAvatar);
                openUserProfile(uid, avatar);
            } else { openUserProfile(uid, null); }
        });

        recyclerView.setAdapter(postsAdapter);
    }

    private void openUserProfile(String uid, View sharedView) {
        Intent intent = new Intent(FeedActivity.this, ProfileActivity.class);
        intent.putExtra("EXTRA_USER_ID", uid);
        if (sharedView != null) {
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    this, sharedView, "profile_image_transition");
            startActivity(intent, options.toBundle());
        } else {
            startActivity(intent);
        }
    }

    private void applyFilters() {
        if (currentFilterCategory.equalsIgnoreCase("Users")) {
            etSearch.setHint("Search Users...");
            showUsersResults();
        } else {
            etSearch.setHint("Search Recipe...");
            showPostsResults();
        }
    }

    private void showUsersResults() {
        List<UserAccount> filteredUsers = new ArrayList<>();
        for (UserAccount user : allUsers) {
            if (user.getNickname().toLowerCase().contains(currentSearchQuery.toLowerCase())) {
                filteredUsers.add(user);
            }
        }
        usersList.clear();
        usersList.addAll(filteredUsers);
        if (recyclerView.getAdapter() != userAdapter) recyclerView.setAdapter(userAdapter);
        userAdapter.notifyDataSetChanged();
    }

    private void showPostsResults() {
        List<RecipePost> filteredList = new ArrayList<>();
        for (RecipePost post : allPosts) {
            boolean matchesSearch = post.getTitle().toLowerCase().contains(currentSearchQuery.toLowerCase());
            boolean matchesCategory = currentFilterCategory.equals("All");
            if (!matchesCategory && post.getClassification() != null) {
                for (Object value : post.getClassification().values()) {
                    if (String.valueOf(value).equalsIgnoreCase(currentFilterCategory)) {
                        matchesCategory = true;
                        break;
                    }
                }
            }
            if (matchesSearch && matchesCategory) filteredList.add(post);
        }
        posts.clear();
        posts.addAll(filteredList);
        if (recyclerView.getAdapter() != postsAdapter) recyclerView.setAdapter(postsAdapter);
        postsAdapter.notifyDataSetChanged();
    }

    private void fetchAllUsers() {
        FirebaseFirestore.getInstance().collection("users")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    allUsers.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        UserAccount user = doc.toObject(UserAccount.class);
                        allUsers.add(new UserAccount(doc.getId(), user.getNickname()));
                    }
                    if (currentFilterCategory.equalsIgnoreCase("Users")) applyFilters();
                });
    }

    private void registerToNewPosts() {
        FirebaseFirestore.getInstance().collection("posts")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        RecipePost post = dc.getDocument().toObject(RecipePost.class);
                        post.setId(dc.getDocument().getId());
                        switch (dc.getType()) {
                            case ADDED: allPosts.add(0, post); break;
                            case MODIFIED:
                                for (int i = 0; i < allPosts.size(); i++) {
                                    if (allPosts.get(i).getId().equals(post.getId())) {
                                        allPosts.set(i, post);
                                        break;
                                    }
                                }
                                break;
                            case REMOVED: allPosts.removeIf(p -> p.getId().equals(post.getId())); break;
                        }
                    }
                    if (!currentFilterCategory.equalsIgnoreCase("Users")) applyFilters();
                });
    }

    private void processImageWithAi(Uri imageUri) {
        try {
            InputStream imageStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
            scannerDialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
            scannerDialog.setContentView(R.layout.dialog_ai_scanner);
            scannerDialog.setCancelable(false);
            ImageView ivScan = scannerDialog.findViewById(R.id.ivScanImage);
            View scannerLine = scannerDialog.findViewById(R.id.vScannerLine);
            TextView tvStatus = scannerDialog.findViewById(R.id.tvAiStatus);
            ivScan.setImageBitmap(bitmap);
            android.animation.ObjectAnimator animator = android.animation.ObjectAnimator.ofFloat(scannerLine, "translationY", 0f, 1100f);
            animator.setDuration(1500);
            animator.setRepeatMode(android.animation.ValueAnimator.REVERSE);
            animator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            animator.start();
            android.os.Handler dotsHandler = new android.os.Handler();
            Runnable dotsRunnable = new Runnable() {
                int count = 0;
                @Override public void run() {
                    String dots = ""; for (int i = 0; i < count; i++) dots += ".";
                    tvStatus.setText("AI ANALYZING" + dots);
                    count = (count + 1) % 4; dotsHandler.postDelayed(this, 500);
                }
            };
            dotsHandler.post(dotsRunnable);
            scannerDialog.show();
            String prompt = "Analyze this image of food. Identify the dish and provide a professional recipe. Return ONLY a JSON object: {\"title\": \"...\", \"ingredients\": [\"...\"], \"instructions\": \"...\"}";
            GeminiManager.getInstance().sendImageAndText(bitmap, prompt, this, new GeminiManager.GeminiCallback() {
                @Override public void onSuccess(String result) {
                    runOnUiThread(() -> {
                        dotsHandler.removeCallbacks(dotsRunnable);
                        if (scannerDialog != null) scannerDialog.dismiss();
                        handleAiResult(result, imageUri);
                    });
                }
                @Override public void onError(Throwable error) {
                    runOnUiThread(() -> {
                        dotsHandler.removeCallbacks(dotsRunnable);
                        if (scannerDialog != null) scannerDialog.dismiss();
                        Toast.makeText(FeedActivity.this, "Scan failed", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleAiResult(String rawJson, Uri imageUri) {
        try {
            String cleanJson = rawJson.replace("```json", "").replace("```", "").trim();
            JSONObject json = new JSONObject(cleanJson);
            Intent intent = new Intent(this, AddPostActivity.class);
            intent.putExtra("ai_title", json.getString("title"));
            intent.putExtra("ai_instructions", json.getString("instructions"));
            intent.putExtra("uri_image", imageUri);
            JSONArray ingArray = json.getJSONArray("ingredients");
            ArrayList<String> ingList = new ArrayList<>();
            for (int i = 0; i < ingArray.length(); i++) ingList.add(ingArray.getString(i));
            intent.putStringArrayListExtra("ai_ingredients", ingList);
            startActivity(intent);
        } catch (Exception e) { Toast.makeText(this, "AI error", Toast.LENGTH_SHORT).show(); }
    }

    private void readUserData() {
        SharedPreferences sp = getSharedPreferences("userInfo", MODE_PRIVATE);
        nickname = sp.getString("nickname", "N/A");
    }

    private void fetchSavedPosts() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .addSnapshotListener((ds, e) -> {
                    if (e != null || ds == null) return;
                    List<String> fetchedIds = (List<String>) ds.get("savedPosts");
                    if (fetchedIds != null) {
                        savedPostIds.clear(); savedPostIds.addAll(fetchedIds);
                        if (postsAdapter != null) postsAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void showAddOptionsDialog() {
        BottomSheetDialog bsd = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_add_options, null);
        view.findViewById(R.id.optionScanAi).setOnClickListener(v -> { bsd.dismiss(); userImageSelector.showImageSourceDialog(); });
        view.findViewById(R.id.optionManualAdd).setOnClickListener(v -> { bsd.dismiss(); startActivity(new Intent(this, AddPostActivity.class)); });
        bsd.setContentView(view); bsd.show();
    }

    private void setupFabScrollBehavior() {
        FloatingActionButton fab = findViewById(R.id.button_move_to_profile);
        com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton btnAdd = findViewById(R.id.button_addPost);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(RecyclerView rv, int dx, int dy) {
                if (dy > 0) { fab.hide(); btnAdd.shrink(); btnAdd.hide(); }
                else if (dy < 0) { fab.show(); btnAdd.show(); btnAdd.extend(); }
            }
        });
    }
}