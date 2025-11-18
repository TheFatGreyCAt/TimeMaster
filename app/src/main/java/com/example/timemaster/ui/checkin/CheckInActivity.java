package com.example.timemaster.ui.checkin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.timemaster.R;
import com.example.timemaster.ui.auth.facerecognition.FaceRecognitionActivity;
import com.example.timemaster.ui.auth.login.LoginActivity;

public class CheckInActivity extends AppCompatActivity {

    private static final String TAG = "CheckInActivity";
    private TimeViewModel timeViewModel;
    private TextView tvTime;
    private TextView tvDate;
    private TextView tvGuide;
    private TextView tvLogin;
    private Button btnFaceId;
    private Button btnFingerprint;
    private ProgressBar progressLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_check_in);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Khởi tạo ViewModel bằng Factory
        TimeViewModelFactory factory = new TimeViewModelFactory();
        timeViewModel = new ViewModelProvider(this, factory).get(TimeViewModel.class);

        initViews();
        setupObservers();
        setupClickListeners();
    }

    private void initViews() {
        tvTime = findViewById(R.id.tvTime);
        tvDate = findViewById(R.id.tvDate);
        tvGuide = findViewById(R.id.tvGuide);
        tvLogin = findViewById(R.id.tvLogin);
        btnFaceId = findViewById(R.id.btnFaceId);
        btnFingerprint = findViewById(R.id.btnFingerprint);
        progressLoading = findViewById(R.id.progressLoading);
        if (progressLoading == null) {
            Log.w(TAG, "progressLoading not found in layout, creating programmatically");
        }
    }

    private void setupObservers() {
        timeViewModel.getCurrentTime().observe(this, time -> {
            if (time != null) {
                tvTime.setText(time);
                Log.d(TAG, "Time updated: " + time);
            }
        });

        timeViewModel.getCurrentDate().observe(this, date -> {
            if (date != null) {
                tvDate.setText(date);
                Log.d(TAG, "Date updated: " + date);
            }
        });

        timeViewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading != null && progressLoading != null) {
                progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                btnFaceId.setEnabled(!isLoading);
                btnFingerprint.setEnabled(!isLoading);
            }
        });

        timeViewModel.getErrorMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error: " + message);
            }
        });
    }

    private void setupClickListeners() {
        btnFaceId.setOnClickListener(v -> {
            Log.d(TAG, "FaceID button clicked");
            timeViewModel.syncWithServerTime(serverTimestamp -> {
                openFaceRecognitionActivity(serverTimestamp);
            });
        });

        btnFingerprint.setOnClickListener(v -> {
            Log.d(TAG, "Fingerprint button clicked");
            timeViewModel.syncWithServerTime(serverTimestamp -> {
                openFingerprintActivity(serverTimestamp);
            });
        });

        tvGuide.setOnClickListener(v -> {
            Log.d(TAG, "Guide button clicked");
            showGuide();
        });

        tvLogin.setOnClickListener(v -> {
            Log.d(TAG, "Login/Register clicked");
            openLoginActivity();
        });
    }

    private void openFaceRecognitionActivity(long serverTimestamp) {
        Intent intent = new Intent(CheckInActivity.this, FaceRecognitionActivity.class);
        intent.putExtra("TIMESTAMP", serverTimestamp);
        intent.putExtra("METHOD", "FACEID");
        Log.d(TAG, "Opening FaceRecognitionActivity with timestamp: " + serverTimestamp);
        startActivity(intent);
    }

    private void openFingerprintActivity(long serverTimestamp) {
        // Có thể truyền serverTimestamp cho attendance tại đây
        Toast.makeText(this, "Tính năng Fingerprint đang được phát triển", Toast.LENGTH_SHORT).show();
        // và thực hiện lưu điểm danh/fingerprint với timestamp server nếu đã hoàn thiện backend
    }

    private void showGuide() {
        Toast.makeText(this, "Hướng dẫn sử dụng TimeMaster", Toast.LENGTH_LONG).show();
    }

    private void openLoginActivity() {
        Intent intent = new Intent(CheckInActivity.this, LoginActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }
}
