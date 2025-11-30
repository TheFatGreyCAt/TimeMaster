package com.example.timemaster.ui.auth.register;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.timemaster.R;
import com.example.timemaster.ui.checkin.CheckInActivity;
import com.example.timemaster.ui.auth.login.LoginActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    // Key truyền email để LoginActivity prefill sau khi đăng ký
    public static final String EXTRA_PREFILL_EMAIL = "prefill_email";

    // Firebase
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    // UI input
    private TextInputLayout title_email_regis;
    private TextInputLayout title_password_regis;
    private TextInputLayout title_confirm_password_regis;
    private TextInputLayout title_username_regis;

    private TextInputEditText editText_email_regis;
    private TextInputEditText editText_password_regis;
    private TextInputEditText editText_confirm_password_regis;
    private TextInputEditText editText_username_regis;

    // UI độ mạnh mật khẩu
    private TextView textView_strength_value;
    private TextView textView_strength_label;
    private ProgressBar tab_strength;

    // Button
    private MaterialButton button_register;
    private ImageButton btnBackMain;
    private TextView tabLogin;
    private TextView tabRegister;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Firebase init
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        initView();
        setupPasswordStrength();
        setupClickListener();
    }

    private void initView() {
        // Ánh xạ view
        title_email_regis = findViewById(R.id.title_email_regis);
        title_password_regis = findViewById(R.id.title_password_regis);
        title_confirm_password_regis = findViewById(R.id.title_confirm_password_regis);
        title_username_regis = findViewById(R.id.title_username_regis);

        editText_email_regis = findViewById(R.id.editText_email_regis);
        editText_password_regis = findViewById(R.id.editText_password_regis);
        editText_confirm_password_regis = findViewById(R.id.editText_confirm_password_regis);
        editText_username_regis = findViewById(R.id.editText_username_regis);

        textView_strength_label = findViewById(R.id.textView_strength_label);
        textView_strength_value = findViewById(R.id.textView_strength_value);
        tab_strength = findViewById(R.id.tab_strength);

        btnBackMain = findViewById(R.id.btn_back_welcome);
        button_register = findViewById(R.id.button_register);
        tabLogin = findViewById(R.id.tab_login_regis);
        tabRegister = findViewById(R.id.tab_register_regis);

        // Ẩn thanh độ mạnh khi chưa nhập
        tab_strength.setVisibility(View.GONE);
        textView_strength_value.setVisibility(View.GONE);
        textView_strength_label.setVisibility(View.GONE);
    }

    private void setupPasswordStrength() {
        // Lắng nghe mật khẩu để tính độ mạnh
        editText_password_regis.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePasswordStrength(s.toString());
                validateConfirmPassword();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Kiểm tra khi gõ ô xác nhận mật khẩu
        editText_confirm_password_regis.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateConfirmPassword();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void updatePasswordStrength(String password) {
        String p = password == null ? "" : password.trim();

        // Nếu chưa nhập → ẩn cụm strength
        if (p.isEmpty()) {
            tab_strength.setVisibility(View.GONE);
            textView_strength_value.setVisibility(View.GONE);
            textView_strength_label.setVisibility(View.GONE);
            return;
        }

        // Có nhập → hiển thị cụm strength
        tab_strength.setVisibility(View.VISIBLE);
        textView_strength_value.setVisibility(View.VISIBLE);
        textView_strength_label.setVisibility(View.VISIBLE);

        int score = 0;
        if (p.length() >= 8) score += 25;
        if (p.matches(".*[A-Z].*")) score += 20;
        if (p.matches(".*[a-z].*")) score += 20;
        if (p.matches(".*\\d.*"))   score += 20;
        if (p.matches(".*[^A-Za-z0-9].*")) score += 15;
        score = Math.min(score, 100);

        tab_strength.setProgress(score);

        String label;
        int color;
        if (score < 35) {
            label = "Yếu";
            color = 0xFFE11D48; // Đỏ
        } else if (score < 70) {
            label = "Trung bình";
            color = 0xFFF59E0B; // Cam
        } else {
            label = "Mạnh";
            color = 0xFF16A34A; // Xanh lá
        }

        textView_strength_value.setText(label);
        textView_strength_value.setTextColor(color);
        tab_strength.setProgressTintList(android.content.res.ColorStateList.valueOf(color));
    }

    private void validateConfirmPassword() {
        String password = editText_password_regis.getText().toString().trim();
        String confirmPassword = editText_confirm_password_regis.getText().toString().trim();

        if (confirmPassword.isEmpty()) {
            title_confirm_password_regis.setError(null);
            return;
        }

        if (!confirmPassword.equals(password)) {
            title_confirm_password_regis.setError("Mật khẩu không khớp");
        } else {
            title_confirm_password_regis.setError(null);
        }
    }

    private void setupClickListener() {
        // Quay về LoginActivity
        tabLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            finish();
        });

        // Tab Register đang active
        tabRegister.setOnClickListener(v -> {});

        // Nút đăng ký
        button_register.setOnClickListener(v -> handleRegister());

        // Nút Back
        btnBackMain.setOnClickListener(v -> {
            Intent intent = new Intent(this, CheckInActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    // --- LOGIC QUAN TRỌNG ĐỂ CẬP NHẬT TÊN ---
    private void handleRegister() {
        String email = editText_email_regis.getText().toString().trim();
        String password = editText_password_regis.getText().toString().trim();
        String confirmPassword = editText_confirm_password_regis.getText().toString().trim();
        String fullName = editText_username_regis.getText().toString().trim();

        // 1. Validate Input
        boolean isValid = true;
        if (fullName.isEmpty()) {
            title_username_regis.setError("Vui lòng nhập họ và tên");
            isValid = false;
        } else {
            title_username_regis.setError(null);
        }

        if (email.isEmpty()) {
            title_email_regis.setError("Vui lòng nhập email");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            title_email_regis.setError("Vui lòng nhập đúng định dạng email");
            isValid = false;
        } else {
            title_email_regis.setError(null);
        }

        if (password.isEmpty()) {
            title_password_regis.setError("Vui lòng nhập mật khẩu");
            isValid = false;
        } else if (password.length() < 6) {
            title_password_regis.setError("Mật khẩu tối thiểu 6 ký tự");
            isValid = false;
        } else {
            title_password_regis.setError(null);
        }

        if (confirmPassword.isEmpty()) {
            title_confirm_password_regis.setError("Vui lòng nhập lại mật khẩu");
            isValid = false;
        } else if (!confirmPassword.equals(password)) {
            title_confirm_password_regis.setError("Mật khẩu không khớp");
            isValid = false;
        } else {
            title_confirm_password_regis.setError(null);
        }

        if (!isValid) return;

        // 2. Tiến hành đăng ký
        button_register.setEnabled(false); // Khoá nút để tránh click nhiều lần

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        button_register.setEnabled(true);
                        showFirebaseError(task.getException());
                        return;
                    }

                    // Đăng ký thành công -> Lấy user
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null) {
                        String userEmail = user.getEmail();
                        // Xác định role dựa trên domain email
                        String role = (userEmail != null && userEmail.endsWith("@timemaster.com")) ? "admin" : "user";

                        // --- QUAN TRỌNG: Cập nhật tên hiển thị NGAY LẬP TỨC ---
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(fullName)
                                .build();

                        // Chờ update profile xong mới lưu DB và chuyển màn hình
                        user.updateProfile(profileUpdates).addOnCompleteListener(profileTask -> {
                            // Dù update profile thành công hay lỗi nhẹ, vẫn tiếp tục lưu Firestore
                            saveUserToFirestore(user.getUid(), userEmail, role, fullName);
                        });
                    }
                });
    }

    private void saveUserToFirestore(String userId, String email, String role, String fullName) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("email", email);
        profile.put("role", role);
        profile.put("fullName", fullName);
        profile.put("createdAt", Timestamp.now());
        profile.put("updatedAt", Timestamp.now());

        firestore.collection("users")
                .document(userId)
                .set(profile)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();

                    // Đăng xuất để người dùng đăng nhập lại (theo luồng app của bạn)
                    // Vì đã updateProfile ở trên, nên lần đăng nhập tới User sẽ có tên luôn.
                    FirebaseAuth.getInstance().signOut();

                    navigateToLogin(email);
                })
                .addOnFailureListener(e -> {
                    button_register.setEnabled(true);
                    Toast.makeText(this, "Lỗi lưu dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToLogin(String emailToPrefill) {
        Intent intent = new Intent(this, LoginActivity.class);
        if (emailToPrefill != null) {
            intent.putExtra(EXTRA_PREFILL_EMAIL, emailToPrefill);
        }
        startActivity(intent);
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        finish();
    }

    private void showFirebaseError(Exception e) {
        String msg = "Đăng ký thất bại";
        if (e instanceof FirebaseAuthUserCollisionException) {
            msg = "Email đã được sử dụng";
        } else if (e instanceof FirebaseAuthWeakPasswordException) {
            msg = "Mật khẩu quá yếu";
        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
            msg = "Email không hợp lệ";
        } else if (e != null && e.getMessage() != null) {
            msg = e.getMessage();
        }
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}