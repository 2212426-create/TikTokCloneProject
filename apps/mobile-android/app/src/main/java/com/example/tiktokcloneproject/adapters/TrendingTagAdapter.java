package com.example.tiktokcloneproject.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tiktokcloneproject.R;

import java.util.ArrayList;

public class TrendingTagAdapter extends RecyclerView.Adapter<TrendingTagAdapter.ViewHolder> {

    private ArrayList<String> mData;
    private OnTagClickListener mListener;

    public interface OnTagClickListener {
        void onTagClick(String tag);
    }

    public TrendingTagAdapter(ArrayList<String> data, OnTagClickListener listener) {
        this.mData = data;
        this.mListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.trending_tag_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String tag = mData.get(position);
        holder.tvTagName.setText(tag);
        holder.itemView.setOnClickListener(v -> {
            if (mListener != null) mListener.onTagClick(tag);
        });
    }

    @Override
    public int getItemCount() {
        return mData != null ? mData.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTagName;

        ViewHolder(View itemView) {
            super(itemView);
            tvTagName = itemView.findViewById(R.id.tvTagName);
        }
    }
}
