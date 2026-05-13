package com.example.tiktokcloneproject.adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.helper.StaticVariable;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

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

        tvUserName.setText(followerUserNameList.get(i));

        // ĐÃ SỬA: Xóa dấu / ở đầu path "user_avatars"
        StorageReference ref = FirebaseStorage.getInstance().getReference().child("user_avatars").child(followerIdList.get(i));

        ref.getBytes(StaticVariable.MAX_BYTES_AVATAR)
                .addOnSuccessListener(bytes -> {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    imvAvatar.setImageBitmap(bitmap);
                })
                .addOnFailureListener(e -> imvAvatar.setImageResource(R.drawable.default_avatar));

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        tvRemove.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
            builder.setTitle("Remove this follower");
            builder.setMessage("\"" + followerUserNameList.get(i) + "\" will no longer follow you.");
            builder.setPositiveButton("Remove", (dialog, which) -> {
                if (currentUser == null) return;
                db.collection("profiles").document(currentUser.getUid()).collection("followers").document(followerIdList.get(i)).delete();
                db.collection("profiles").document(followerIdList.get(i)).collection("following").document(currentUser.getUid()).delete();
                Toast.makeText(context, "Follower removed", Toast.LENGTH_SHORT).show();
            });
            builder.show();
        });

        return view;
    }
}
