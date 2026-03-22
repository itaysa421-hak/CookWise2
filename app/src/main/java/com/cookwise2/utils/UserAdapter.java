package com.cookwise2.utils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.cookwise2.R;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private List<UserAccount> users;
    private OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(String uid);
    }

    public UserAdapter(List<UserAccount> users, OnUserClickListener listener) {
        this.users = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserAccount user = users.get(position);
        holder.name.setText(user.getNickname());

        Glide.with(holder.itemView.getContext())
                .load(user.getProfileImageUrl())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .circleCrop()
                .into(holder.avatar);

        holder.itemView.setOnClickListener(v -> listener.onUserClick(user.getUid()));
    }

    @Override
    public int getItemCount() { return users.size(); }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView avatar;
        TextView name;
        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.ivUserAvatar);
            name = itemView.findViewById(R.id.tvUserNickname);
        }
    }
}