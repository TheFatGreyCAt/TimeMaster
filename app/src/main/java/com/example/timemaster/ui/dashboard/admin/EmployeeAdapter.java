package com.example.timemaster.ui.dashboard.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timemaster.R;
import com.example.timemaster.data.model.Employee;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder> {

    private Context context;
    private List<Employee> mList;
    private IClickItemListener iClickItemListener;

    public interface IClickItemListener {
        void onClickItem(Employee employee, int position);
    }

    public EmployeeAdapter(Context context, List<Employee> mList, IClickItemListener listener) {
        this.context = context;
        this.mList = mList;
        this.iClickItemListener = listener;
    }

    public void setFilteredList(List<Employee> filteredList) {
        this.mList = filteredList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EmployeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_employee, parent, false);
        return new EmployeeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmployeeViewHolder holder, int position) {
        Employee employee = mList.get(position);
        if (employee == null) return;

        // Tên nhân viên
        holder.tvName.setText(employee.getName());

        // Dòng bé: Hiển thị "Quản lý" hoặc "Nhân viên" (đã xử lý ở Fragment)
        holder.tvDetails.setText(employee.getJobTitle());

        // Ảnh đại diện (nếu bạn có logic load ảnh thật thì thay ở đây, tạm thời dùng placeholder)
        holder.ivAvatar.setImageResource(R.drawable.ic_avatar);

        holder.itemView.setOnClickListener(v -> iClickItemListener.onClickItem(employee, position));
    }

    @Override
    public int getItemCount() {
        return mList != null ? mList.size() : 0;
    }

    public static class EmployeeViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivAvatar;
        TextView tvName, tvDetails;

        public EmployeeViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvName = itemView.findViewById(R.id.tv_name);
            tvDetails = itemView.findViewById(R.id.tv_details);
        }
    }
}