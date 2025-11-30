package com.example.timemaster;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotiViewHolder> {
    private List<String> mList;

    public NotificationAdapter(List<String> mList) {
        this.mList = mList;
    }

    @NonNull
    @Override
    public NotiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Dùng layout đơn giản có sẵn của Android
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new NotiViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotiViewHolder holder, int position) {
        holder.tvContent.setText(mList.get(position));
    }

    @Override
    public int getItemCount() { return mList.size(); }

    public static class NotiViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent;
        public NotiViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(android.R.id.text1);
        }
    }
}