package com.example.timemaster.app.Layer1;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.timemaster.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FaceRecognitionActivity extends AppCompatActivity {

    private static final String TAG = "FaceRecognitionActivity";

    private FloatingActionButton btnCapture;
    private Button btnBack;
    private long receivedTimestamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_face_recognition);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Nhận timestamp từ MainActivity
        receivedTimestamp = getIntent().getLongExtra("TIMESTAMP", System.currentTimeMillis());
        Log.d(TAG, "Received timestamp: " + receivedTimestamp);

        initViews();
    }

    private void initViews() {
        btnCapture = findViewById(R.id.btnCapture);
        btnBack = findViewById(R.id.btnBack);

        btnCapture.setOnClickListener(view -> onCaptureClicked());

        btnBack.setOnClickListener(v -> finish()); // Quay lại MainActivity
    }

    private void onCaptureClicked() {
        long currentTimestamp = System.currentTimeMillis();

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String formattedTime = sdf.format(new Date(currentTimestamp));

        Log.d(TAG, "Check-in timestamp: " + currentTimestamp + " (" + formattedTime + ")");

        String message = "Chấm công thành công!\n" + formattedTime;
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        // TODO: Gửi dữ liệu lên server

        // Quay lại MainActivity sau khi chấm công
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
