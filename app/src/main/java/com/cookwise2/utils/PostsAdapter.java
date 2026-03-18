package com.cookwise2.utils;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cookwise2.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.PostViewHolder> {

    private List<RecipePost> posts;
    private OnItemClickListener listener;
    private OnUserClickListener userClickListener; // מאזין חדש
    private List<String> savedPostIds;
    private boolean isSavedTab = false;

    public interface OnItemClickListener {
        void onItemClick(RecipePost post);
    }

    // ממשק חדש למעבר לפרופיל
    public interface OnUserClickListener {
        void onUserClick(String uid);
    }

    public PostsAdapter(List<RecipePost> posts, List<String> savedPostIds, OnItemClickListener listener) {
        this.posts = posts;
        this.savedPostIds = savedPostIds;
        this.listener = listener;
    }

    // Setter למאזין המשתמש
    public void setOnUserClickListener(OnUserClickListener userClickListener) {
        this.userClickListener = userClickListener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        RecipePost post = posts.get(position);

        holder.tvTitle.setText(post.getTitle());
        holder.tvOwner.setText(post.getOwnerNickname());
        holder.tvDate.setText(timestampToString(post.getCreatedAt()));

        // לחיצה על שם המשתמש למעבר לפרופיל
        holder.tvOwner.setOnClickListener(v -> {
            if (userClickListener != null) {
                userClickListener.onUserClick(post.getOwnerUid());
            }
        });

        Map<String, Object> tags = post.getClassification();
        if (tags != null) {
            holder.tvCuisine.setText(getStringFromMap(tags, "cuisine", "General"));
            holder.tvDifficulty.setText(getStringFromMap(tags, "difficulty", "Easy"));
            holder.tvTime.setText(getStringFromMap(tags, "estimated_time", "30 min"));
        } else {
            holder.tvCuisine.setText("Recipe");
            holder.tvDifficulty.setText("Easy");
            holder.tvTime.setText("---");
        }

        String imageUrl = post.getImageUrl();
        Glide.with(holder.itemView.getContext())
                .load(imageUrl != null && !imageUrl.isEmpty() ? imageUrl : R.drawable.generic_recipe_image_background)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(holder.ivPostImage);

        boolean isSaved = savedPostIds != null && savedPostIds.contains(post.getId());
        holder.btnSave.setImageResource(isSaved ?
                R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_border);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(post);
        });

        holder.btnSave.setOnClickListener(v -> {
            toggleSavePost(post.getId(), position);
        });
    }

    private String getStringFromMap(Map<String, Object> map, String key, String defaultValue) {
        if (map != null && map.containsKey(key) && map.get(key) != null) {
            return String.valueOf(map.get(key));
        }
        return defaultValue;
    }

    @Override
    public int getItemCount() {
        return posts != null ? posts.size() : 0;
    }

    private String timestampToString(Timestamp timestamp) {
        if (timestamp == null) return "";
        Date date = timestamp.toDate();
        if (DateUtils.isToday(date.getTime())) {
            return "Today, " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
        } else {
            return new SimpleDateFormat("MMM d", Locale.getDefault()).format(date);
        }
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvOwner, tvDate, tvTime, tvDifficulty, tvCuisine;
        ImageView ivPostImage;
        ImageButton btnSave;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_post_title);
            tvOwner = itemView.findViewById(R.id.tv_post_owner);
            tvDate = itemView.findViewById(R.id.tv_post_created_at);
            tvTime = itemView.findViewById(R.id.tv_post_time);
            tvDifficulty = itemView.findViewById(R.id.tv_post_difficulty);
            tvCuisine = itemView.findViewById(R.id.tv_post_cuisine);
            ivPostImage = itemView.findViewById(R.id.iv_post_image);
            btnSave = itemView.findViewById(R.id.btn_save_post);
        }
    }

    public void toggleSavePost(String postId, int position) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DocumentReference userRef = FirebaseFirestore.getInstance().collection("users").document(currentUserId);

        boolean isSaved = savedPostIds != null && savedPostIds.contains(postId);

        if (isSaved) {
            if (savedPostIds != null) savedPostIds.remove(postId);
            userRef.update("savedPosts", FieldValue.arrayRemove(postId));
        } else {
            if (savedPostIds != null) savedPostIds.add(postId);
            userRef.update("savedPosts", FieldValue.arrayUnion(postId));
        }

        if (savedStatusListener != null) {
            savedStatusListener.onSavedStatusChanged(savedPostIds.size());
        }

        if (isSavedTab && isSaved) {
            posts.remove(position);
            notifyItemRemoved(position);
        } else {
            notifyItemChanged(position);
        }
    }

    public void setIsSavedTab(boolean isSavedTab) {
        this.isSavedTab = isSavedTab;
    }

    public interface OnSavedStatusChangedListener {
        void onSavedStatusChanged(int newCount);
    }

    private OnSavedStatusChangedListener savedStatusListener;

    public void setOnSavedStatusChangedListener(OnSavedStatusChangedListener listener) {
        this.savedStatusListener = listener;
    }
}