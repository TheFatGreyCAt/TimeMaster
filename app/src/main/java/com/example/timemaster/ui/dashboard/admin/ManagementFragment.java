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
import com.example.timemaster.AuditLogActivity; // Import Activity Nhật ký mới
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

    private ImageView ivNotification; // Cái chuông
    private View viewBadge;           // Chấm đỏ thông báo

    // Thêm nút xem nhật ký (bạn có thể thêm 1 icon vào layout xml hoặc dùng tạm icon có sẵn)
    // Ở đây tôi giả sử bạn thêm 1 ImageView id là iv_history vào toolbar trong XML
    // Nếu chưa có, bạn hãy thêm vào frag_admin_manage.xml cạnh cái chuông
    private ImageView ivHistory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_admin_manage, container, false);

        db = FirebaseFirestore.getInstance();
        initViews(view);
        setupDataFromFirebase(); // Hàm này sẽ tự động cập nhật SĐT/Email khi DB thay đổi
        setupEvents();
        setupNotificationBadge(); // Lắng nghe chấm đỏ

        return view;
    }

    private void initViews(View view) {
        etSearch = view.findViewById(R.id.et_search);
        rcvEmployees = view.findViewById(R.id.recycler_view_employees);
        ivNotification = view.findViewById(R.id.iv_notification);
        viewBadge = view.findViewById(R.id.view_badge);

        // Bạn cần thêm ImageView id="@+id/iv_history" vào file frag_admin_manage.xml
        // cạnh cái chuông để làm nút xem nhật ký.
        // Nếu không muốn sửa XML, có thể tạm bỏ qua dòng này.
        ivHistory = view.findViewById(R.id.iv_history);

        rcvEmployees.setLayoutManager(new LinearLayoutManager(getContext()));
        mListEmployee = new ArrayList<>();

        employeeAdapter = new EmployeeAdapter(getContext(), mListEmployee, (employee, position) -> {
            // Khi click vào nhân viên, truyền object Employee (đã có SĐT/Email) sang Detail
            Intent intent = new Intent(getContext(), EmployeeDetailActivity.class);
            intent.putExtra("employee_data", employee);
            startActivity(intent);
        });
        rcvEmployees.setAdapter(employeeAdapter);
    }

    private void setupDataFromFirebase() {
        // Lắng nghe Realtime -> Khi User/Admin update Info, App tự cập nhật List này
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

                    // --- CẬP NHẬT ĐỌC PHONE VÀ EMAIL TẠI ĐÂY ---
                    // Dòng code này đảm bảo dữ liệu mới nhất được lấy về
                    emp.setPhone(doc.getString("phoneNumber"));
                    emp.setEmail(doc.getString("email"));
                    // --------------------------------------------

                    String role = doc.getString("role");
                    if ("admin".equalsIgnoreCase(role)) {
                        emp.setJobTitle("Quản lý");
                    } else {
                        emp.setJobTitle("Nhân viên");
                    }
                    // Tạm fix avatar
                    emp.setAvatarResId(R.drawable.ic_avatar);

                    mListEmployee.add(emp);
                }
                filterEmployee(etSearch.getText().toString());
            }
        });
    }

    private void setupNotificationBadge() {
        // Hiện chấm đỏ khi có yêu cầu update profile đang chờ duyệt
        db.collection("update_requests")
                .whereEqualTo("status", "pending")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) return;
                    if (snapshots != null && !snapshots.isEmpty()) {
                        if (viewBadge != null) viewBadge.setVisibility(View.VISIBLE);
                    } else {
                        if (viewBadge != null) viewBadge.setVisibility(View.GONE);
                    }
                });
    }

    private void setupEvents() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { filterEmployee(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        if (ivNotification != null) {
            ivNotification.setOnClickListener(v -> {
                startActivity(new Intent(getContext(), AdminApprovalActivity.class));
            });
        }

        // Sự kiện mở trang Nhật ký
        if (ivHistory != null) {
            ivHistory.setOnClickListener(v -> {
                startActivity(new Intent(getContext(), AuditLogActivity.class));
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