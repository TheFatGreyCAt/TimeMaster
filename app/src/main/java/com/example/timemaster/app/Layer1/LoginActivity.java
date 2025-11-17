package com.example.timemaster.app.Layer1;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.timemaster.R;
import com.example.timemaster.app.Layer1.MainActivity;
import com.example.timemaster.app.Layer1.RegisterActivity;
import com.example.timemaster.app.Layer2.DashboardHostActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
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

    // Firebase
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    // Tabs
    private LinearLayout tabLayout;
    private TextView tabLogin;
    private TextView tabRegister;

    // UI
    private TextInputLayout titleEmail;
    private TextInputLayout titlePassword;
    private TextInputEditText editEmail;
    private TextInputEditText editPassword;
    private TextView tvForgotPassword;

    private ImageButton btnBackMain;
    private MaterialButton btnLogin;
    private MaterialCardView cardFingerprint;
    private MaterialCardView cardFace;

    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        initView();
        navigateFromSignup();
        setupClickListener();

        // Mặc định tab Đăng nhập đang active
        setLoginTabActive(true);
        configureGoogleSignIn();
    }

    public void initView() {
        tabLayout = findViewById(R.id.tab_layout_Log);
        tabLogin = findViewById(R.id.tab_login_Login);
        tabRegister = findViewById(R.id.tab_register_Login);

        titleEmail = findViewById(R.id.title_email);
        titlePassword = findViewById(R.id.title_password);
        editEmail = findViewById(R.id.editText_email);
        editPassword = findViewById(R.id.editText_password);

        tvForgotPassword = findViewById(R.id.textView_forgot_password);
        btnLogin = findViewById(R.id.button_login);
        btnBackMain = findViewById(R.id.btn_back_welcome);

        cardFingerprint = findViewById(R.id.card_fingerprint);
        cardFace = findViewById(R.id.card_face);
    }

    private void navigateFromSignup() {
        // prefill email
        String prefill = getIntent().getStringExtra(RegisterActivity.EXTRA_PREFILL_EMAIL);
        if (!TextUtils.isEmpty(prefill) && editEmail != null) {
            editEmail.setText(prefill);
            if (editPassword != null) {
                editPassword.requestFocus();
            }
        }
    }

    private void setLoginTabActive(boolean isLoginActive) {
        // Tab “Đăng nhập” đang active: đã set bg trong XML: bg_tab_selected
        // Có thể bổ sung hiệu ứng/đổi nền nếu cần
        tabLogin.setEnabled(!isLoginActive);     // để hiệu ứng ripple hợp lý
        tabRegister.setEnabled(isLoginActive);
    }

    private void handleLogin() {
        String email = editEmail != null ? editEmail.getText().toString().trim() : "";
        String pass  = editPassword != null ? editPassword.getText().toString().trim() : "";

        boolean isValid = true;

        // Validate email
        if (TextUtils.isEmpty(email)) {
            titleEmail.setError("Vui lòng nhập email");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            titleEmail.setError("Email không hợp lệ");
            isValid = false;
        } else {
            titleEmail.setError(null);
        }

        // Validate password
        if (TextUtils.isEmpty(pass)) {
            titlePassword.setError("Vui lòng nhập mật khẩu");
            isValid = false;
        } else {
            titlePassword.setError(null);
        }

        if (!isValid) { return; }

        // Chống bấm nhiều lần
        if (btnLogin != null) {
            btnLogin.setEnabled(false);
        }

        firebaseAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    if (btnLogin != null) {
                        btnLogin.setEnabled(true);
                    }

                    if (!task.isSuccessful()) {
                        String msg = (task.getException() != null && task.getException().getLocalizedMessage() != null)
                                ? task.getException().getLocalizedMessage()
                                : "Đăng nhập thất bại";
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Thành công -> sang HomeActivity
                    Intent intent = new Intent(this, MainActivity.class); // đổi lớp đích nếu bạn dùng activity khác
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                });
    }

    private void handleForgotPassword() {
        String email = editEmail != null ? editEmail.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            if (titleEmail != null) {
                titleEmail.setError("Hãy nhập email trước khi khôi phục mật khẩu");
            }
            if (editEmail != null) {
                editEmail.requestFocus();
            }
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            if (titleEmail != null) {
                titleEmail.setError("Email không hợp lệ");
            }
            if (editEmail != null) {
                editEmail.requestFocus();
            }
            return;
        }

        if (titleEmail != null) {
            titleEmail.setError(null);
        }

        firebaseAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Đã gửi email đặt lại mật khẩu. Vui lòng kiểm tra hộp thư.", Toast.LENGTH_LONG).show())
                .addOnFailureListener(e -> {
                    String msg = (e.getLocalizedMessage() != null) ? e.getLocalizedMessage() : "Gửi email thất bại";
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                });
    }

    private void setupClickListener() {
        if (tabLogin != null) {
            tabLogin.setOnClickListener(v -> {});
        }

        if (tabRegister != null) {
            tabRegister.setOnClickListener(v -> {
                Intent intent = new Intent(this, RegisterActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            });
        }

        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> handleForgotPassword());
        }

        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> handleLogin());
        }

        if (btnBackMain != null) {
            btnBackMain.setOnClickListener(v -> {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        if (cardFingerprint != null) {
            cardFingerprint.setOnClickListener(v ->
                    Toast.makeText(this, "Fingerprint: sẽ tích hợp sau", Toast.LENGTH_SHORT).show());
        }
        if (cardFace != null) {
            cardFace.setOnClickListener(v ->
                    Toast.makeText(this, "FaceID: sẽ tích hợp sau", Toast.LENGTH_SHORT).show());
        }
    }

    private void onUserDocLoaded(DocumentSnapshot doc) {
        String role = null;
        if (doc != null && doc.exists()) {
            role = doc.getString("role");
        }
        cacheAndGo(role);
    }

    private void cacheAndGo(String role) {
        String finalRole = TextUtils.isEmpty(role) ? "user" : role;
        getSharedPreferences("tm_prefs", MODE_PRIVATE).edit().putString("role", finalRole).apply();

        Intent i = new Intent(this, DashboardHostActivity.class);
        i.putExtra(DashboardHostActivity.EXTRA_ROLE, finalRole);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void configureGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Yêu cầu gg trả về token
                .requestEmail()
                .build();

        googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(data);
                        handleGoogleSignInResult(task);
                    } else {
                        Toast.makeText(this, "Đăng nhập Google bị hủy", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void signInWithGoogle() {
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            Log.d(TAG, "Google Sign In thành công với email: " + account.getEmail());
            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException e) {
            Log.w(TAG, "Google Sign In thất bại", e);
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            checkAndSaveUserToFirestore(user);
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Xác thực thất bại", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkAndSaveUserToFirestore(FirebaseUser user) {
        firestore.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        onUserDocLoaded(documentSnapshot);
                    } else {
                        saveGoogleUserToFirestore(user);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi khi kiểm tra thông tin người dùng", Toast.LENGTH_SHORT).show());
    }

    private void saveGoogleUserToFirestore(FirebaseUser user) {
        String userId = user.getUid();
        String email = user.getEmail();
        String displayName = user.getDisplayName();
        String photoUrl = (user.getPhotoUrl() != null) ? user.getPhotoUrl().toString() : null;
        String finalDisplayName = !TextUtils.isEmpty(displayName) ? displayName : email;

        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("email", email);
        userProfile.put("displayName", finalDisplayName);
        userProfile.put("photoUrl", photoUrl);
        userProfile.put("role", "user");
        userProfile.put("loginMethod", "google");
        userProfile.put("createdAt", FieldValue.serverTimestamp());

        firestore.collection("users").document(userId)
                .set(userProfile)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                    cacheAndGo("user");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(LoginActivity.this, "Lỗi khi lưu thông tin người dùng", Toast.LENGTH_SHORT).show());
    }
}
