package com.example.timemaster.ui.dashboard.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.timemaster.R;
import com.example.timemaster.data.model.Employee;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class EmployeeDetailActivity extends AppCompatActivity {

    private EditText edtName, edtJob, edtPhone, edtEmail;
    private Button btnSave, btnDelete;
    private CircleImageView imgAvatar;
    private TextView tvTitle;
    private FirebaseFirestore db;

    private Employee mEmployee;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_detail);

        db = FirebaseFirestore.getInstance();
        initViews();

        Intent intent = getIntent();
        if (intent.hasExtra("employee_data")) {
            // --- Chế độ SỬA/CHI TIẾT ---
            isEditMode = true;
            mEmployee = (Employee) intent.getSerializableExtra("employee_data");
            fillData();
            btnDelete.setVisibility(View.VISIBLE);
            tvTitle.setText("Chi tiết nhân viên");
            btnSave.setText("Lưu thay đổi"); // Đặt lại text cho chế độ sửa
        } else {
            // --- Chế độ THÊM MỚI ---
            isEditMode = false;
            mEmployee = new Employee(); // Object rỗng
            btnDelete.setVisibility(View.GONE);
            btnSave.setText("Thêm nhân viên");
            tvTitle.setText("Thêm nhân viên mới");
        }

        setupEvents();
    }

    private void initViews() {
        // Nút Back (đảm bảo bạn đã thêm id btnBack vào XML như hướng dẫn trước)
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        tvTitle = findViewById(R.id.tvTitle);
        imgAvatar = findViewById(R.id.img_detail_avatar);
        edtName = findViewById(R.id.edt_detail_name);
        edtJob = findViewById(R.id.edt_detail_job);
        edtPhone = findViewById(R.id.edt_detail_phone);
        edtEmail = findViewById(R.id.edt_detail_email);
        btnSave = findViewById(R.id.btn_save_changes);
        btnDelete = findViewById(R.id.btn_delete_employee);
    }

    private void fillData() {
        if (mEmployee != null) {
            edtName.setText(mEmployee.getName());
            edtJob.setText(mEmployee.getJobTitle());
            edtPhone.setText(mEmployee.getPhone());
            edtEmail.setText(mEmployee.getEmail());
            // Cần xử lý tải ảnh từ URL nếu bạn lưu URL trên Firebase,
            // ở đây tạm dùng Avatar mặc định nếu có ResId
            imgAvatar.setImageResource(mEmployee.getAvatarResId());
        }
    }

    private void setupEvents() {
        btnSave.setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            String job = edtJob.getText().toString().trim();
            String phone = edtPhone.getText().toString().trim();
            String email = edtEmail.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "Tên không được để trống", Toast.LENGTH_SHORT).show();
                return;
            }

            // Tạo Map dữ liệu chuẩn Firebase
            // Lưu ý: Key phải khớp với cấu trúc bạn đã lưu trong collection 'users'
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("displayName", name);  // Key thường dùng cho tên trên Firebase
            userMap.put("jobTitle", job);
            userMap.put("phoneNumber", phone);
            userMap.put("email", email);
            // Các trường khác như dob, address, v.v. nếu có

            btnSave.setEnabled(false); // Chặn bấm liên tục
            btnSave.setText("Đang lưu...");

            if (isEditMode && mEmployee.getId() != null) {
                // --- UPDATE ---
                db.collection("users").document(mEmployee.getId())
                        .update(userMap)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                            finish(); // Quay lại màn hình danh sách
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            btnSave.setEnabled(true);
                            btnSave.setText("Lưu thay đổi");
                        });
            } else {
                // --- ADD NEW ---
                db.collection("users")
                        .add(userMap) // Firebase tự động tạo ID mới
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(this, "Thêm mới thành công!", Toast.LENGTH_SHORT).show();
                            finish(); // Quay lại màn hình danh sách
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Lỗi thêm mới: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            btnSave.setEnabled(true);
                            btnSave.setText("Thêm nhân viên");
                        });
            }
        });

        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Xác nhận xóa")
                    .setMessage("Bạn chắc chắn muốn xóa nhân viên này?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        if (mEmployee.getId() != null) {
                            // --- DELETE ---
                            db.collection("users").document(mEmployee.getId())
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Đã xóa nhân viên!", Toast.LENGTH_SHORT).show();
                                        finish(); // Quay lại màn hình danh sách
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Lỗi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
    }
}