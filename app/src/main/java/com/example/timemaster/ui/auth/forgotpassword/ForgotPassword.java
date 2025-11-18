package com.example.timemaster.ui.auth.forgotpassword;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.timemaster.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPassword extends AppCompatActivity {
    public static final String EXTRA_PREFILL_EMAIL = "extra_prefill_email";

    private TextInputLayout tilEmail;
    private TextInputEditText edtEmail;
    private MaterialButton btnSend;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);

        firebaseAuth = FirebaseAuth.getInstance();

        tilEmail = findViewById(R.id.til_email_forgot);
        edtEmail = findViewById(R.id.edt_email_forgot);
        btnSend = findViewById(R.id.btn_send_reset);

        // Prefill email nếu LoginActivity gửi sang
        String prefill = getIntent().getStringExtra(EXTRA_PREFILL_EMAIL);
        if (!TextUtils.isEmpty(prefill)) {
            edtEmail.setText(prefill);
        }

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleSendReset();
            }
        });
    }

    private void handleSendReset() {
        String email = edtEmail.getText() != null ? edtEmail.getText().toString().trim() : "";

        // Validate
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Vui lòng nhập email");
            edtEmail.requestFocus();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Email không hợp lệ");
            edtEmail.requestFocus();
            return;
        }
        tilEmail.setError(null);

        setLoading(true);

        firebaseAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this,
                            "Đã gửi email đặt lại mật khẩu. Vui lòng kiểm tra hộp thư.",
                            Toast.LENGTH_LONG).show();
                    finish(); // quay lại Login khi gửi thành công
                })
                .addOnFailureListener(e -> {
                    String msg = (e != null && e.getLocalizedMessage() != null)
                            ? e.getLocalizedMessage()
                            : "Gửi email thất bại";
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    setLoading(false);
                });
    }

    private void setLoading(boolean loading) {
        btnSend.setEnabled(!loading);
        btnSend.setText(loading ? "Đang gửi…" : "Gửi email đặt lại");
        edtEmail.setEnabled(!loading);
        tilEmail.setEnabled(!loading);
    }
}