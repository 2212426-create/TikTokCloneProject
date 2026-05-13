package com.example.tiktokcloneproject.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.helper.StaticVariable;
import com.example.tiktokcloneproject.model.Comment;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

public class CommentAdapter extends ArrayAdapter<Comment> {
    private Context context;
    private int resource;
    private List<Comment> objects;

    public CommentAdapter(@NonNull Context context, int resource, @NonNull List<Comment> objects) {
        super(context, resource, objects);
        this.context = context;
        this.resource = resource;
        this.objects = objects;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(resource, parent, false);
        }

        Comment comment = objects.get(position);
        ImageView imvAvatar = convertView.findViewById(R.id.imvAvatarInComment);
        TextView tvUsername = convertView.findViewById(R.id.txvUsernameInComment);
        TextView tvContent = convertView.findViewById(R.id.txvComment);

        tvContent.setText(comment.getContent());

        FirebaseFirestore.getInstance().collection("users").document(comment.getAuthorId())
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        tvUsername.setText(task.getResult().getString("username"));
                    }
                });

        StorageReference ref = FirebaseStorage.getInstance().getReference().child("user_avatars").child(comment.getAuthorId());
        ref.getBytes(StaticVariable.MAX_BYTES_AVATAR).addOnSuccessListener(bytes -> {
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            imvAvatar.setImageBitmap(bitmap);
        }).addOnFailureListener(e -> imvAvatar.setImageResource(R.drawable.default_avatar));

        return convertView;
    }
}
