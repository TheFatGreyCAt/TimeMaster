package com.example.timemaster.ui.dashboard.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timemaster.AdminApprovalActivity;
import com.example.timemaster.R;
import com.example.timemaster.data.model.Employee;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ManagementFragment extends Fragment {

    private RecyclerView rcvEmployees;
    private EmployeeAdapter employeeAdapter;
    private List<Employee> mListEmployee;
    private EditText etSearch;
    private FirebaseFirestore db;

    // Khai báo biến cái chuông
    private ImageView ivNotification;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_admin_manage, container, false);

        db = FirebaseFirestore.getInstance();
        initViews(view);
        setupDataFromFirebase();
        setupEvents();

        return view;
    }

    private void initViews(View view) {
        etSearch = view.findViewById(R.id.et_search);
        rcvEmployees = view.findViewById(R.id.recycler_view_employees);

        // --- 1. Ánh xạ cái chuông từ XML cũ của bạn ---
        ivNotification = view.findViewById(R.id.iv_notification);

        rcvEmployees.setLayoutManager(new LinearLayoutManager(getContext()));
        mListEmployee = new ArrayList<>();

        employeeAdapter = new EmployeeAdapter(getContext(), mListEmployee, (employee, position) -> {
            Intent intent = new Intent(getContext(), EmployeeDetailActivity.class);
            intent.putExtra("employee_data", employee);
            startActivity(intent);
        });
        rcvEmployees.setAdapter(employeeAdapter);
    }

    private void setupDataFromFirebase() {
        db.collection("users").addSnapshotListener((value, error) -> {
            if (error != null) return;
            if (value != null) {
                mListEmployee.clear();
                for (DocumentSnapshot doc : value.getDocuments()) {
                    Employee emp = new Employee();
                    emp.setId(doc.getId());

                    String name = doc.getString("fullName");
                    if (name == null) name = doc.getString("displayName");
                    emp.setName(name != null ? name : "Chưa đặt tên");

                    String role = doc.getString("role");
                    // Logic hiển thị chức vụ
                    if ("admin".equalsIgnoreCase(role)) {
                        emp.setJobTitle("Quản lý");
                    } else {
                        emp.setJobTitle("Nhân viên");
                    }

                    mListEmployee.add(emp);
                }
                filterEmployee(etSearch.getText().toString());
            }
        });
    }

    private void setupEvents() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterEmployee(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        // --- 2. Xử lý ấn vào cái chuông ---
        if (ivNotification != null) {
            ivNotification.setOnClickListener(v -> {
                // Chuyển sang màn hình Duyệt yêu cầu
                // Giờ đây nó đã hiểu AdminApprovalActivity là gì nhờ dòng import bên trên
                Intent intent = new Intent(getContext(), AdminApprovalActivity.class);
                startActivity(intent);
            });
        }
    }

    private void filterEmployee(String keyword) {
        List<Employee> filteredList = new ArrayList<>();
        if (keyword == null || keyword.isEmpty()) {
            filteredList.addAll(mListEmployee);
        } else {
            String lower = keyword.toLowerCase().trim();
            for (Employee item : mListEmployee) {
                if (item.getName().toLowerCase().contains(lower)) {
                    filteredList.add(item);
                }
            }
        }
        if (employeeAdapter != null) employeeAdapter.setFilteredList(filteredList);
    }
}