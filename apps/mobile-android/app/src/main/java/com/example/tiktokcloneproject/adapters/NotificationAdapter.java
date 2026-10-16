package com.example.tiktokcloneproject.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.tiktokcloneproject.R;
import com.example.tiktokcloneproject.helper.StaticVariable;
import com.example.tiktokcloneproject.model.Notification;

import java.util.ArrayList;

public class NotificationAdapter extends ArrayAdapter<Notification> {

    private ArrayList<Notification> notifications;
    private Context context;

    public NotificationAdapter(@NonNull Context context, int resource, ArrayList<Notification> notifications) {
        super(context, resource, notifications);
        this.notifications = notifications;
        this.context = context;
    }

    @Override
    public View getView(int position, @Nullable View convertView, ViewGroup parent) {
        View row = convertView;

        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(R.layout.notification_row, null);

        }

        TextView username = (TextView) row.findViewById(R.id.txvUsername);
        TextView content = (TextView) row.findViewById(R.id.txvContent);
        TextView time = (TextView) row.findViewById(R.id.txvTime);

        username.setText("@" + notifications.get(position).getFromUsername());
        content.setText(handleAction(notifications.get(position).getAction()));
        time.setText(handleTime(notifications.get(position).getTimestamp()));

        return (row);
    }

    private String handleTime(long timeInMilliseconds) {
        long difference_In_Time = System.currentTimeMillis() - timeInMilliseconds;
        long difference_In_Minutes
                = (difference_In_Time
                / (1000 * 60));
        long difference_In_Hours
                = (difference_In_Time
                / (1000 * 60 * 60));
        long difference_In_Days
                = (difference_In_Time
                / (1000 * 60 * 60 * 24));
        if(difference_In_Minutes <= 60) {
            return difference_In_Minutes + "m";
        }
        else if(difference_In_Hours <= 24) {
            return difference_In_Hours + "h";
        }
        else {
            return difference_In_Days + "d";
        }
    }

    private String handleAction(String action) {
        if (action != null && action.startsWith("APPEAL_REQUEST")) {
            String[] parts = action.split("\\|");
            String reason = parts.length > 2 ? parts[2] : "Vi phạm tiêu chuẩn cộng đồng";
            return "Video bị tố cáo vì: " + reason + ". Bấm vào đây để kháng cáo.";
        }
        switch (action) {
            case StaticVariable.COMMENT:
                return context.getString(R.string.template_comment);
            case StaticVariable.FOLLOW:
                return context.getString(R.string.template_follow);
            case StaticVariable.LIKE:
                return context.getString(R.string.template_like);
            case StaticVariable.CHAT:
                return context.getString(R.string.template_chat);
        }
        return "";
    }



}
