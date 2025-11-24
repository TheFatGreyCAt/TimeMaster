package com.example.timemaster.ui.dashboard.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.timemaster.R;
import com.example.timemaster.model.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ManagementFragment extends Fragment {

    private RecyclerView recyclerView;
    private EmployeeAdapter employeeAdapter;
    private List<User> userList;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate layout cho Fragment này
        View view = inflater.inflate(R.layout.frag_admin_manage, container, false);

        // Ánh xạ RecyclerView từ layout XML
        // Đảm bảo ID này khớp với ID trong frag_admin_manage.xml
        recyclerView = view.findViewById(R.id.recycler_view_employees);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Khởi tạo danh sách và adapter
        userList = new ArrayList<>();
        employeeAdapter = new EmployeeAdapter(userList);
        recyclerView.setAdapter(employeeAdapter);

        // Khởi tạo Firestore
        db = FirebaseFirestore.getInstance();

        // Lấy dữ liệu nhân viên
        fetchEmployees();

        return view;
    }

    private void fetchEmployees() {
        db.collection("users") // Giả sử collection của bạn tên là "users"
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        userList.clear(); // Xóa dữ liệu cũ
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            User user = document.toObject(User.class);
                            userList.add(user);
                        }
                        employeeAdapter.notifyDataSetChanged(); // Cập nhật RecyclerView
                    } else {
                        // Xử lý lỗi
                    }
                });
    }
}
