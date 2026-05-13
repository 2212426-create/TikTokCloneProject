package com.example.tiktokcloneproject.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.helper.StaticVariable;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

public class FollowingAdapter extends BaseAdapter {
    private Context context;
    private LayoutInflater inflater;
    private ArrayList<String> followingIdList;
    private ArrayList<String> followingUserNameList;

    public FollowingAdapter(Context context, ArrayList<String> followingIdList, ArrayList<String> followingUserNameList) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.followingIdList = followingIdList;
        this.followingUserNameList = followingUserNameList;
    }

    @Override
    public int getCount() {
        return followingIdList.size();
    }

    @Override
    public Object getItem(int i) {
        return followingIdList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = inflater.inflate(R.layout.layout_following_item, viewGroup, false);
        }

        ImageView imvAvatar = view.findViewById(R.id.imv_following_avatar);
        TextView tvUserName = view.findViewById(R.id.tv_following_userMame);
        
        tvUserName.setText(followingUserNameList.get(i));

        // ĐÃ SỬA: Xóa dấu / ở đầu path "user_avatars"
        StorageReference ref = FirebaseStorage.getInstance().getReference().child("user_avatars").child(followingIdList.get(i));

        ref.getBytes(StaticVariable.MAX_BYTES_AVATAR)
                .addOnSuccessListener(bytes -> {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    imvAvatar.setImageBitmap(bitmap);
                })
                .addOnFailureListener(e -> imvAvatar.setImageResource(R.drawable.default_avatar));

        return view;
    }
}
