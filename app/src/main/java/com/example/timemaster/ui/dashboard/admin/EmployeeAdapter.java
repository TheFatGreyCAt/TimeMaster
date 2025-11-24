package com.example.timemaster.ui.dashboard.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.timemaster.R;
import com.example.timemaster.model.User; // Đảm bảo import đúng
import java.util.List;

public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder> {

    private List<User> userList;

    public EmployeeAdapter(List<User> userList) {
        this.userList = userList;
    }

    @NonNull
    @Override
    public EmployeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_employee, parent, false);
        return new EmployeeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmployeeViewHolder holder, int position) {
        User user = userList.get(position);
        holder.tvName.setText(user.getDisplayName());
        holder.tvEmail.setText(user.getEmail());
        // holder.tvDetails.setText(...); // Cập nhật cho tv_details nếu cần
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class EmployeeViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvEmail;
        TextView tvDetails; // Thêm nếu bạn muốn dùng

        public EmployeeViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ view từ item_employee.xml
            tvName = itemView.findViewById(R.id.tv_name);
            tvEmail = itemView.findViewById(R.id.tv_email); // ID đã được thêm trong XML
            tvDetails = itemView.findViewById(R.id.tv_details);
        }
    }
}
