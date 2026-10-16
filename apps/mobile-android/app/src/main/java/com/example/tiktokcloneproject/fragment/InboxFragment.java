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
import com.example.tiktokcloneproject.adapters.ActiveUserAdapter;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class InboxFragment extends Fragment {
    private RecyclerView recyclerView;
    private RecyclerView rvActiveUsers;
    private UserChatAdapter userChatAdapter;
    private List<User> mUsers;
    private ActiveUserAdapter activeUserAdapter;
    private List<User> activeUsers;
    private List<String> usersList;
    private Map<String, Long> userTimestamps;
    private Map<String, User> userById;
    private Map<String, User> activeUserMap;
    private Map<String, ValueEventListener> statusListeners;
    private DatabaseReference statusRef;
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

        View btnNotifications = view.findViewById(R.id.btnNotifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(getContext(), com.example.tiktokcloneproject.activity.InboxActivity.class);
                startActivity(intent);
            });
        }

        rvActiveUsers = view.findViewById(R.id.rvActiveUsers);
        if (rvActiveUsers != null) {
            rvActiveUsers.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            activeUsers = new ArrayList<>();
            activeUserAdapter = new ActiveUserAdapter(getContext(), activeUsers);
            rvActiveUsers.setAdapter(activeUserAdapter);
        }

        fUser = FirebaseAuth.getInstance().getCurrentUser();
        usersList = new ArrayList<>();
        userTimestamps = new HashMap<>();
        userById = new HashMap<>();
        activeUserMap = new HashMap<>();
        statusListeners = new HashMap<>();
        statusRef = FirebaseHelper.getDatabase().getReference("status");
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
                            Long ts = dataSnapshot.child("timestamp").getValue(Long.class);
                            if (ts == null) {
                                ts = dataSnapshot.child("lastTimestamp").getValue(Long.class);
                            }
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
            if (activeUsers != null) {
                activeUsers.clear();
                if (activeUserAdapter != null) activeUserAdapter.notifyDataSetChanged();
            }
            activeUserMap.clear();
            clearStatusListeners();
            updateEmptyView(true);
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final List<User> tempUsers = new ArrayList<>();
        final int[] processedCount = {0};
        userById.clear();

        for (String id : usersList) {
            if ("system_admin".equals(id)) {
                User systemUser = new User();
                systemUser.setUserId("system_admin");
                systemUser.setUsername("Hệ thống TopTop");
                systemUser.setAvatarUrl("");
                tempUsers.add(systemUser);
                userById.put("system_admin", systemUser);
                processedCount[0]++;
                if (processedCount[0] == usersList.size()) sortAndDisplay(tempUsers);
                continue;
            }

            db.collection("profiles").document(id).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    User user = new User();
                    user.setUserId(documentSnapshot.getId());
                    user.setUsername(documentSnapshot.getString("username"));
                    user.setAvatarUrl(documentSnapshot.getString("avatarUrl"));
                    tempUsers.add(user);
                    userById.put(id, user);
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
                syncStatusListeners();
            });
        }
    }

    private void syncStatusListeners() {
        if (statusRef == null || statusListeners == null) return;

        Iterator<Map.Entry<String, ValueEventListener>> iterator = statusListeners.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ValueEventListener> entry = iterator.next();
            String userId = entry.getKey();
            if (!usersList.contains(userId)) {
                statusRef.child(userId).removeEventListener(entry.getValue());
                iterator.remove();
                activeUserMap.remove(userId);
            }
        }

        for (String userId : usersList) {
            if ("system_admin".equals(userId)) continue;
            if (!statusListeners.containsKey(userId)) {
                ValueEventListener listener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Boolean isOnline = snapshot.child("online").getValue(Boolean.class);
                        User user = userById.get(userId);
                        if (Boolean.TRUE.equals(isOnline) && user != null) {
                            activeUserMap.put(userId, user);
                        } else {
                            activeUserMap.remove(userId);
                        }
                        updateActiveUsersList();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                };
                statusRef.child(userId).addValueEventListener(listener);
                statusListeners.put(userId, listener);
            }
        }
        updateActiveUsersList();
    }

    private void updateActiveUsersList() {
        if (activeUsers == null || activeUserAdapter == null) return;
        List<User> onlineUsers = new ArrayList<>(activeUserMap.values());
        Collections.sort(onlineUsers, (u1, u2) -> {
            Long t1 = userTimestamps.get(u1.getUserId());
            Long t2 = userTimestamps.get(u2.getUserId());
            return Long.compare(t2 != null ? t2 : 0, t1 != null ? t1 : 0);
        });
        activeUsers.clear();
        activeUsers.addAll(onlineUsers);
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> activeUserAdapter.notifyDataSetChanged());
        }
    }

    private void clearStatusListeners() {
        if (statusRef == null || statusListeners == null) return;
        for (Map.Entry<String, ValueEventListener> entry : statusListeners.entrySet()) {
            statusRef.child(entry.getKey()).removeEventListener(entry.getValue());
        }
        statusListeners.clear();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        clearStatusListeners();
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
