package com.example.tiktokcloneproject.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.activity.ProfileActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class FollowerAdapter extends BaseAdapter {
    private Context context;
    private LayoutInflater inflater;
    private ArrayList<String> followerIdList;
    private ArrayList<String> followerUserNameList;

    public FollowerAdapter(Context context, ArrayList<String> followerIdList, ArrayList<String> followerUserNameList) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.followerIdList = followerIdList;
        this.followerUserNameList = followerUserNameList;
    }

    @Override
    public int getCount() {
        return followerIdList.size();
    }

    @Override
    public Object getItem(int i) {
        return followerIdList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = inflater.inflate(R.layout.layout_follower_item, viewGroup, false);
        }

        ImageView imvAvatar = view.findViewById(R.id.imv_follower_avatar);
        TextView tvUserName = view.findViewById(R.id.tv_followers_userMame);
        TextView tvRemove = view.findViewById(R.id.tv_remove_follower);

        String userId = followerIdList.get(i);
        tvUserName.setText(followerUserNameList.get(i));

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Tải avatarUrl từ profile thay vì Firebase Storage
        db.collection("profiles").document(userId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String avatarUrl = doc.getString("avatarUrl");
                if (avatarUrl != null) {
                    Glide.with(context)
                            .load(avatarUrl)
                            .placeholder(R.drawable.default_avatar)
                            .circleCrop()
                            .into(imvAvatar);
                } else {
                    imvAvatar.setImageResource(R.drawable.default_avatar);
                }
            }
        });

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        tvRemove.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
            builder.setTitle("Remove this follower");
            builder.setMessage("\"" + followerUserNameList.get(i) + "\" will no longer follow you.");
            builder.setPositiveButton("Remove", (dialog, which) -> {
                if (currentUser == null) return;
                db.collection("profiles").document(currentUser.getUid()).collection("followers").document(userId).delete();
                db.collection("profiles").document(userId).collection("following").document(currentUser.getUid()).delete();
                Toast.makeText(context, "Follower removed", Toast.LENGTH_SHORT).show();
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        });

        view.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProfileActivity.class);
            intent.putExtra("id", userId);
            context.startActivity(intent);
        });

        return view;
    }
}
