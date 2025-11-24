package com.example.timemaster.ui.dashboard.stats;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.timemaster.R;
import com.example.timemaster.model.AttendanceHistory;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<AttendanceHistory> list;

    public HistoryAdapter(List<AttendanceHistory> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attendance_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AttendanceHistory item = list.get(position);
        holder.tvTime.setText(item.getTime() + ", " + item.getDate());
        holder.tvStatus.setText(item.getStatus());

        // Xử lý màu sắc dựa trên trạng thái (Logic Xịn)
        switch (item.getStatus()) {
            case "Đúng giờ":
                holder.cardStatus.setCardBackgroundColor(Color.parseColor("#E8F5E9")); // Nền xanh nhạt
                holder.tvStatus.setTextColor(Color.parseColor("#43A047")); // Chữ xanh đậm
                break;
            case "Đi trễ":
                holder.cardStatus.setCardBackgroundColor(Color.parseColor("#FFF8E1")); // Nền vàng nhạt
                holder.tvStatus.setTextColor(Color.parseColor("#FBC02D")); // Chữ vàng đậm
                break;
            case "Về sớm":
                holder.cardStatus.setCardBackgroundColor(Color.parseColor("#E3F2FD")); // Nền xanh dương nhạt
                holder.tvStatus.setTextColor(Color.parseColor("#1E88E5")); // Chữ xanh dương
                break;
            case "Vắng mặt":
                holder.cardStatus.setCardBackgroundColor(Color.parseColor("#FFEBEE")); // Nền đỏ nhạt
                holder.tvStatus.setTextColor(Color.parseColor("#E53935")); // Chữ đỏ
                break;
        }
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvStatus;
        CardView cardStatus;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_history_time);
            tvStatus = itemView.findViewById(R.id.tv_history_status);
            cardStatus = (CardView) tvStatus.getParent();
        }
    }
}