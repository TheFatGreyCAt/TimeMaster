package com.example.timemaster;

import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPhone, etDob, etAddress;
    private RadioGroup rgGender;
    private RadioButton rbMale, rbFemale;
    private Button btnSave;

    private FirebaseUser currentUser;
    private DocumentReference userRef;
    private String currentName = ""; // Lưu tên hiện tại

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }
        userRef = FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid());

        initViews();
        loadUserData();

        // Sự kiện bấm nút Lưu -> Gửi yêu cầu
        btnSave.setOnClickListener(v -> sendUpdateRequest());
    }

    private void initViews() {
        findViewById(R.id.btnBackProfile).setOnClickListener(v -> finish());
        etName = findViewById(R.id.et_fullname);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etDob = findViewById(R.id.et_dob);
        etAddress = findViewById(R.id.et_address);
        rgGender = findViewById(R.id.rg_gender);
        rbMale = findViewById(R.id.rb_male);
        rbFemale = findViewById(R.id.rb_female);
        btnSave = findViewById(R.id.btn_save_profile);
    }

    private void loadUserData() {
        userRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                // Lấy tên để so sánh (ưu tiên fullName)
                String name = doc.getString("fullName");
                if (name == null) name = doc.getString("displayName");
                currentName = name != null ? name : "";

                etName.setText(currentName);
                etEmail.setText(doc.getString("email"));
                etPhone.setText(doc.getString("phoneNumber"));
                etDob.setText(doc.getString("dob"));
                etAddress.setText(doc.getString("address"));

                String gender = doc.getString("gender");
                if ("Nam".equals(gender)) rbMale.setChecked(true);
                else if ("Nữ".equals(gender)) rbFemale.setChecked(true);
            }
        });
    }

    private void sendUpdateRequest() {
        String newName = etName.getText().toString().trim();
        if (newName.isEmpty()) {
            Toast.makeText(this, "Tên không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Đang gửi yêu cầu...");

        // 1. Gói dữ liệu MỚI
        Map<String, Object> newData = new HashMap<>();
        newData.put("fullName", newName);
        newData.put("phoneNumber", etPhone.getText().toString().trim());
        newData.put("dob", etDob.getText().toString().trim());
        newData.put("address", etAddress.getText().toString().trim());
        String gender = "Khác";
        if (rbMale.isChecked()) gender = "Nam";
        if (rbFemale.isChecked()) gender = "Nữ";
        newData.put("gender", gender);

        // 2. Gói YÊU CẦU (Request)
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("userId", currentUser.getUid());
        requestMap.put("oldName", currentName);
        requestMap.put("newName", newName);
        requestMap.put("newData", newData); // Gửi kèm dữ liệu mới
        requestMap.put("status", "pending");
        requestMap.put("timestamp", Timestamp.now());

        // 3. Gửi lên Firestore
        FirebaseFirestore.getInstance().collection("update_requests")
                .add(requestMap)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Đã gửi yêu cầu đến Admin!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                    btnSave.setText("Cập nhật thông tin");
                });
    }
}