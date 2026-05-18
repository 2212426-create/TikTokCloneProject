package com.example.tiktokcloneproject.adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tiktokcloneproject.activity.ProfileActivity;
import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.model.User;

import java.util.ArrayList;
import java.util.List;
import com.bumptech.glide.Glide;
import de.hdodenhof.circleimageview.CircleImageView;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.userItems> implements Filterable {
    private List<User> listUser;
    private List<User> listUserOld;
    private Context mainContext;




    public UserAdapter(Context context,List<User> listUser)
    {
        mainContext =context;
        this.listUser=listUser;
        this.listUserOld=listUser;
    }



    @NonNull
    @Override
    public userItems onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        mainContext = parent.getContext();
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user,parent,false);
        return new userItems(view);
    }

    @Override
    public void onBindViewHolder(@NonNull userItems holder, int position) {
        User user= listUser.get(position);
        if (user ==null) {
            return;
        }
        holder.text_Username.setText(user.getUsername());
        
        // Load avatar using Glide
        if (mainContext != null) {
            Glide.with(mainContext)
                 .load(user.getAvatarUrl() != null ? user.getAvatarUrl() : R.drawable.default_avatar)
                 .placeholder(R.drawable.default_avatar)
                 .error(R.drawable.default_avatar)
                 .into(holder.imv_user_avatar);
        }

        holder.layout_items.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(mainContext, ProfileActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("id",user.getUserId());
                intent.putExtras(bundle);
                mainContext.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        if (listUser != null)
        { return listUser.size();
        }
        return 0;
    }



    public class userItems extends RecyclerView.ViewHolder{
        private TextView text_Username;
        private LinearLayout layout_items;
        private CircleImageView imv_user_avatar;


        public userItems(@NonNull View itemView) {
            super(itemView);
            text_Username=(TextView) itemView.findViewById(R.id.text_Username);
            layout_items=(LinearLayout) itemView.findViewById(R.id.layout_items);
            imv_user_avatar=(CircleImageView) itemView.findViewById(R.id.imv_user_avatar);

        }
    }

    public void release()
    {
        mainContext = null;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String srtSearch=charSequence.toString();
                if (srtSearch.isEmpty()) {
                    listUser=listUserOld;

                }
                else {
                    List<User> list=new ArrayList<>();
                    for (User user : listUserOld){
                        if (user.getUsername() != null && user.getUsername().toLowerCase().contains(srtSearch.toLowerCase())){
                            list.add(user);
                        }
                    }
                    listUser=list;

                }

                FilterResults filterResults=new FilterResults();
                filterResults.values=listUser;

                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                Object values = filterResults.values;
                if (values instanceof List<?>) {
                    List<?> rawList = (List<?>) values;
                    List<User> filteredUsers = new ArrayList<>();
                    for (Object item : rawList) {
                        if (item instanceof User) {
                            filteredUsers.add((User) item);
                        }
                    }
                    listUser = filteredUsers;
                } else {
                    listUser = new ArrayList<>();
                }
                notifyDataSetChanged();
            }
        };
    }
}
