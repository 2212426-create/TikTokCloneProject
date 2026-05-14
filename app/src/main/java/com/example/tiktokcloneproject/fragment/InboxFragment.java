package com.example.tiktokcloneproject.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.adapters.UserChatAdapter;
import com.example.tiktokcloneproject.helper.FirebaseHelper;
import com.example.tiktokcloneproject.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InboxFragment extends Fragment {
    private RecyclerView recyclerView;
    private UserChatAdapter userChatAdapter;
    private List<User> mUsers;
    private List<String> usersList;
    private Map<String, Long> userTimestamps;
    private FirebaseUser fUser;
    private final String TAG = "InboxFragment";

    public static InboxFragment newInstance(String strArg) {
        InboxFragment fragment = new InboxFragment();
        Bundle args = new Bundle();
        args.putString("name", strArg);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inbox, container, false);

        recyclerView = view.findViewById(R.id.rvInboxChats);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        fUser = FirebaseAuth.getInstance().getCurrentUser();
        usersList = new ArrayList<>();
        userTimestamps = new HashMap<>();
        mUsers = new ArrayList<>();
        userChatAdapter = new UserChatAdapter(getContext(), mUsers);
        recyclerView.setAdapter(userChatAdapter);

        if (fUser != null) {
            DatabaseReference reference = FirebaseHelper.getDatabase().getReference("ChatList").child(fUser.getUid());
            reference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    usersList.clear();
                    userTimestamps.clear();
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        String userId = dataSnapshot.getKey();
                        if (userId != null) {
                            usersList.add(userId);
                            // Lấy timestamp để sắp xếp người nhắn tin mới nhất lên đầu
                            Long ts = dataSnapshot.child("lastTimestamp").getValue(Long.class);
                            userTimestamps.put(userId, ts != null ? ts : 0L);
                        }
                    }
                    loadChats();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "ChatList error: " + error.getMessage());
                }
            });
        }

        return view;
    }

    private void loadChats() {
        if (usersList.isEmpty()) {
            mUsers.clear();
            userChatAdapter.notifyDataSetChanged();
            updateEmptyView(true);
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final List<User> tempUsers = new ArrayList<>();
        final int[] processedCount = {0};

        for (String id : usersList) {
            db.collection("profiles").document(id).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // GIẢI PHÁP CHỐNG CRASH: Lấy thủ công từng trường, tránh dùng toObject(User.class)
                    User user = new User();
                    user.setUserId(documentSnapshot.getId());
                    user.setUsername(documentSnapshot.getString("username"));
                    
                    String avatar = documentSnapshot.getString("avatarUrl");
                    if (avatar == null) avatar = documentSnapshot.getString("avatarUri");
                    user.setAvatarUrl(avatar);
                    
                    tempUsers.add(user);
                }
                
                processedCount[0]++;
                if (processedCount[0] == usersList.size()) {
                    sortAndDisplay(tempUsers);
                }
            }).addOnFailureListener(e -> {
                processedCount[0]++;
                if (processedCount[0] == usersList.size()) sortAndDisplay(tempUsers);
            });
        }
    }

    private void sortAndDisplay(List<User> users) {
        // Sắp xếp: Ai vừa nhắn tin xong (timestamp lớn nhất) sẽ hiện lên đầu
        Collections.sort(users, (u1, u2) -> {
            Long t1 = userTimestamps.get(u1.getUserId());
            Long t2 = userTimestamps.get(u2.getUserId());
            return Long.compare(t2 != null ? t2 : 0, t1 != null ? t1 : 0);
        });

        mUsers.clear();
        mUsers.addAll(users);
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                userChatAdapter.notifyDataSetChanged();
                updateEmptyView(mUsers.isEmpty());
            });
        }
    }

    private void updateEmptyView(boolean isEmpty) {
        if (getView() != null) {
            View blankView = getView().findViewById(R.id.blank_notification);
            if (blankView != null) {
                blankView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            }
        }
    }
}
