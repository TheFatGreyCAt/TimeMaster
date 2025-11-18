package com.example.timemaster.ui.auth.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.timemaster.R;
import com.example.timemaster.ui.checkin.CheckInActivity;
import com.example.timemaster.ui.auth.register.RegisterActivity;
import com.example.timemaster.ui.dashboard.DashboardHostActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private TextInputLayout titleEmail, titlePassword;
    private TextInputEditText editEmail, editPassword;
    private MaterialButton btnLogin;

    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        initView();
        setupClickListener();
        configureGoogleSignIn();

        // Lấy email từ RegisterActivity (nếu có)
        String prefill = getIntent().getStringExtra(RegisterActivity.EXTRA_PREFILL_EMAIL);
        if (!TextUtils.isEmpty(prefill)) {
            editEmail.setText(prefill);
            editPassword.requestFocus();
        }
    }

    private void initView() {
        titleEmail = findViewById(R.id.title_email);
        titlePassword = findViewById(R.id.title_password);
        editEmail = findViewById(R.id.editText_email);
        editPassword = findViewById(R.id.editText_password);
        btnLogin = findViewById(R.id.button_login);
    }

    private void setupClickListener() {
        btnLogin.setOnClickListener(v -> handleLogin());
        findViewById(R.id.textView_forgot_password).setOnClickListener(v -> handleForgotPassword());
        findViewById(R.id.btn_google_login).setOnClickListener(v -> signInWithGoogle());

        findViewById(R.id.btn_back_welcome).setOnClickListener(v -> {
            startActivity(new Intent(this, CheckInActivity.class));
            finish();
        });
        findViewById(R.id.tab_register_Login).setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        // Bạn có thể thêm ProgressBar ở đây nếu muốn
    }

    private void handleLogin() {
        String email = editEmail.getText().toString().trim();
        String pass = editPassword.getText().toString().trim();

        if (!validateInput(email, pass)) return;

        setLoading(true);

        firebaseAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && firebaseAuth.getCurrentUser() != null) {
                        // Đăng nhập Auth thành công, giờ kiểm tra vai trò trong Firestore
                        checkUserRoleInFirestore(firebaseAuth.getCurrentUser());
                    } else {
                        setLoading(false);
                        String error = task.getException() != null ? task.getException().getMessage() : "Email hoặc mật khẩu không đúng";
                        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean validateInput(String email, String pass) {
        boolean isValid = true;
        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            titleEmail.setError("Vui lòng nhập email hợp lệ");
            isValid = false;
        } else {
            titleEmail.setError(null);
        }

        if (TextUtils.isEmpty(pass)) {
            titlePassword.setError("Vui lòng nhập mật khẩu");
            isValid = false;
        } else {
            titlePassword.setError(null);
        }
        return isValid;
    }

    private void handleForgotPassword() {
        String email = editEmail.getText().toString().trim();
        if (!validateInput(email, "not_empty")) return;

        firebaseAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> Toast.makeText(this, "Đã gửi email đặt lại mật khẩu. Vui lòng kiểm tra hộp thư.", Toast.LENGTH_LONG).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Gửi email thất bại: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show());
    }

    private void configureGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        try {
                            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            firebaseAuthWithGoogle(account.getIdToken());
                        } catch (ApiException e) {
                            Log.w(TAG, "Google Sign In thất bại", e);
                            setLoading(false);
                        }
                    } else {
                        setLoading(false);
                        Toast.makeText(this, "Đăng nhập Google bị hủy", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void signInWithGoogle() {
        setLoading(true);
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && firebaseAuth.getCurrentUser() != null) {
                        // Đăng nhập Google thành công, kiểm tra hoặc tạo mới trong Firestore
                        checkUserRoleInFirestore(firebaseAuth.getCurrentUser());
                    } else {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this, "Xác thực Google thất bại", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserRoleInFirestore(FirebaseUser user) {
        firestore.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Người dùng đã tồn tại, lấy vai trò và đi tiếp
                        String role = documentSnapshot.getString("role");
                        navigateToDashboard(role);
                    } else {
                        // Người dùng mới (qua Google), tạo tài liệu mới
                        saveNewGoogleUserToFirestore(user);
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Lỗi khi đọc dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveNewGoogleUserToFirestore(FirebaseUser user) {
        String finalDisplayName = !TextUtils.isEmpty(user.getDisplayName()) ? user.getDisplayName() : user.getEmail();

        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("email", user.getEmail());
        userProfile.put("displayName", finalDisplayName);
        userProfile.put("photoUrl", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null);
        userProfile.put("role", "user"); // Mặc định vai trò là user
        userProfile.put("loginMethod", "google");
        userProfile.put("createdAt", FieldValue.serverTimestamp());

        firestore.collection("users").document(user.getUid())
                .set(userProfile)
                .addOnSuccessListener(aVoid -> {
                    // Lưu thành công, đi tới dashboard với vai trò "user"
                    navigateToDashboard("user");
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this, "Lỗi khi lưu thông tin người dùng", Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToDashboard(String role) {
        String finalRole = TextUtils.isEmpty(role) ? "user" : role;
        // Lưu vai trò vào SharedPreferences nếu cần truy cập sau này
        // getSharedPreferences("tm_prefs", MODE_PRIVATE).edit().putString("role", finalRole).apply();

        Intent i = new Intent(this, DashboardHostActivity.class);
        i.putExtra(DashboardHostActivity.EXTRA_ROLE, finalRole);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
