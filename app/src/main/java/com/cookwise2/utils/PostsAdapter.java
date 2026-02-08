package com.cookwise2.utils;

import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cookwise2.R;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.PostViewHolder> {

    private static final String TAG = "PostsAdapter";
    private List<RecipePost> posts;
    // שלב 1: הגדרת המשתנה של המאזין
    private OnItemClickListener listener;

    // שלב 2: הגדרת ה-Interface ללחיצה
    public interface OnItemClickListener {
        void onItemClick(RecipePost post);
    }

    // שלב 3: עדכון הבנאי (Constructor) שיקבל גם את המאזין
    public PostsAdapter(List<RecipePost> posts, OnItemClickListener listener) {
        this.posts = posts;
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
        Log.d(TAG, "onBindViewHolder: adding post item #" + position);

        RecipePost post = posts.get(position);

        holder.titleTextView.setText(post.getTitle());
        holder.descriptionTextView.setText(post.getDescription());
        holder.groceries.setText(listGroceriesToString(post));
        holder.ownerTextView.setText(post.getOwnerNickname());
        holder.createdAtTextView.setText(timestampToString(post.getCreatedAt()));

        if(post.getImageUrl() == null)
        {


            //לעשות אחרי זה

        }
        else {


            // שליפת ה-URL ישירות מהאובייקט (שמגיע מה-Firestore)
            String potPictureUrl = post.getImageUrl();

            Log.d("GlideDebug", "Loading URL: " + potPictureUrl);

            Glide.with(holder.itemView.getContext()) // עדיף להשתמש ב-Context של ה-View
                    .load(potPictureUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.stat_notify_error) // חשוב כדי לראות אם יש שגיאה
                    .centerCrop()
                    .into(holder.iv_post_image);
        }


        // שלב 4: הגדרת לחיצה על כל פריט ברשימה
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(post);
            }
        });
    }

    @Override
    public int getItemCount() {
        Log.d(TAG, "getItemCount: " + posts.size());
        return posts.size();
    }

    public String listGroceriesToString(RecipePost post){
        ArrayList<String> arrayPost = post.getGroceries();
        String str = "";

        if(arrayPost ==  null)
            return str;
        for (int i = 0; i < arrayPost.size() ; i++){
            str += arrayPost.get(i);
            if(i < arrayPost.size() - 1)
                str += ", ";
        }
        return str;
    }

    private String timestampToString(Timestamp timestamp) {
        Date messageDate = timestamp.toDate();
        boolean isToday = DateUtils.isToday(messageDate.getTime());

        SimpleDateFormat fmt;
        if (isToday) {
            fmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
        } else {
            fmt = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        }
        return fmt.format(messageDate);
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView descriptionTextView;
        TextView groceries;
        TextView ownerTextView;
        TextView createdAtTextView;
        ImageView iv_post_image;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.tv_post_title);
            descriptionTextView = itemView.findViewById(R.id.tv_post_description);
            groceries = itemView.findViewById(R.id.tv_post_ingredients);
            ownerTextView = itemView.findViewById(R.id.tv_post_owner);
            createdAtTextView = itemView.findViewById(R.id.tv_post_created_at);
            iv_post_image = itemView.findViewById(R.id.iv_post_image);
        }
    }
}