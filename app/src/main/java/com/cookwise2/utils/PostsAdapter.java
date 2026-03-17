package com.cookwise2.utils;

import android.text.format.DateUtils;
import android.util.Log;
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
    private List<String> savedPostIds;

    public interface OnItemClickListener {
        void onItemClick(RecipePost post);
    }

    public PostsAdapter(List<RecipePost> posts, List<String> savedPostIds, OnItemClickListener listener) {
        this.posts = posts;
        this.savedPostIds = savedPostIds;
        this.listener = listener;
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

        // שליפת נתונים מה-Classification (תגיות ה-AI)
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

        // טעינת תמונה
        String imageUrl = post.getImageUrl();
        Glide.with(holder.itemView.getContext())
                .load(imageUrl != null ? imageUrl : R.drawable.generic_recipe_image_background)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(holder.ivPostImage);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(post);
        });

        boolean isSaved = savedPostIds != null && savedPostIds.contains(post.getId());
        holder.btnSave.setImageResource(isSaved ?
                android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);

        holder.btnSave.setOnClickListener(v -> {
            toggleSavePost(post.getId());
            // עדכון מקומי מהיר ב-UI (אופטימי)
            if (isSaved) savedPostIds.remove(post.getId());
            else savedPostIds.add(post.getId());
            notifyItemChanged(position);
        });


    }

    // פונקציית עזר לשליפת ערך ממפה עם ברירת מחדל
    private String getStringFromMap(Map<String, Object> map, String key, String defaultValue) {
        if (map.containsKey(key) && map.get(key) != null) {
            return String.valueOf(map.get(key));
        }
        return defaultValue;
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    private String timestampToString(Timestamp timestamp) {
        if (timestamp == null) return "";
        Date date = timestamp.toDate();
        if (DateUtils.isToday(date.getTime())) {
            return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
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
    public void toggleSavePost(String postId) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DocumentReference userRef = FirebaseFirestore.getInstance().collection("users").document(userId);

        // אם הפוסט כבר קיים - הסר אותו, אם לא - הוסף (Atomic update)
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            List<String> savedPosts = (List<String>) documentSnapshot.get("savedPosts");
            if (savedPosts != null && savedPosts.contains(postId)) {
                userRef.update("savedPosts", FieldValue.arrayRemove(postId));
            } else {
                userRef.update("savedPosts", FieldValue.arrayUnion(postId));
            }
        });
    }
}