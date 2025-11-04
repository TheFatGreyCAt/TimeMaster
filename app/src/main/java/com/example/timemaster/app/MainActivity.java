package com.example.timemaster.app;

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
import com.example.timemaster.model.TimeViewModel;
import com.google.firebase.Firebase;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
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

        // Khởi tạo ViewModel
        timeViewModel = new ViewModelProvider(this).get(TimeViewModel.class);

        initViews();
        setupObservers();
        setupClickListeners();
        fetchTimeFromServer();
    }

    private void initViews() {
        // Map với ID trong XML
        tvTime = findViewById(R.id.tvTime);
        tvDate = findViewById(R.id.tvDate);
        tvGuide = findViewById(R.id.tvGuide);
        tvLogin = findViewById(R.id.tvLogin);
        btnFaceId = findViewById(R.id.btnFaceId);
        btnFingerprint = findViewById(R.id.btnFingerprint);

        // ProgressBar (thêm vào XML nếu chưa có)
        progressLoading = findViewById(R.id.progressLoading);
        if (progressLoading == null) {
            Log.w(TAG, "progressLoading not found in layout, creating programmatically");
        }
    }

    private void setupObservers() {
        // Observe thời gian (10:20 SA)
        timeViewModel.getCurrentTime().observe(this, time -> {
            if (time != null) {
                tvTime.setText(time);
                Log.d(TAG, "Time updated: " + time);
            }
        });

        // Observe ngày (Thứ Năm, 23 tháng 10, 2025)
        timeViewModel.getCurrentDate().observe(this, date -> {
            if (date != null) {
                tvDate.setText(date);
                Log.d(TAG, "Date updated: " + date);
            }
        });

        // Observe loading state
        timeViewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading != null && progressLoading != null) {
                progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);

                // Disable buttons khi đang loading
                btnFaceId.setEnabled(!isLoading);
                btnFingerprint.setEnabled(!isLoading);
            }
        });

        // Observe error message
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
            openFaceRecognitionActivity();
        });

        btnFingerprint.setOnClickListener(v -> {
            Log.d(TAG, "Fingerprint button clicked");
            openFingerprintActivity();
        });

        // Xem xet xoa
        tvGuide.setOnClickListener(v -> {
            Log.d(TAG, "Guide button clicked");
            showGuide();
        });

        tvLogin.setOnClickListener(v -> {
            Log.d(TAG, "Login/Register clicked");
            openLoginActivity();
        });
    }

    private void fetchTimeFromServer() {
        Log.d(TAG, "Fetching time from WorldTime API...");
        timeViewModel.fetchWorldTime();
    }

    private void openFaceRecognitionActivity() {
        Intent intent = new Intent(MainActivity.this, FaceRecognitionActivity.class);

        // Truyền timestamp hiện tại sang màn hình chấm công
        long currentTimestamp = timeViewModel.getCurrentTimestamp();
        intent.putExtra("TIMESTAMP", currentTimestamp);
        intent.putExtra("METHOD", "FACEID");

        Log.d(TAG, "Opening FaceRecognitionActivity with timestamp: " + currentTimestamp);
        startActivity(intent);
    }

    private void openFingerprintActivity() {
        Toast.makeText(this, "Tính năng Fingerprint đang được phát triển", Toast.LENGTH_SHORT).show();

        // TODO: Tạo FingerprintActivity
        /*
        Intent intent = new Intent(MainActivity.this, FingerprintActivity.class);
        long currentTimestamp = timeViewModel.getCurrentTimestamp();
        intent.putExtra("TIMESTAMP", currentTimestamp);
        intent.putExtra("METHOD", "FINGERPRINT");
        startActivity(intent);
        */
    }

    private void showGuide() {
        Toast.makeText(this, "Hướng dẫn sử dụng TimeMaster", Toast.LENGTH_LONG).show();

        // TODO: Mở màn hình hướng dẫn
        /*
        Intent intent = new Intent(MainActivity.this, GuideActivity.class);
        startActivity(intent);
        */
    }

    private void openLoginActivity() {
        Toast.makeText(this, "Chức năng đăng nhập đang được phát triển", Toast.LENGTH_SHORT).show();

        // TODO: Mở màn hình đăng nhập
        /*
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        */
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Sync time
        Log.d(TAG, "onResume - Syncing time...");
        timeViewModel.syncTime();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }
}
