package com.example.timemaster.ui.auth.login;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.timemaster.R;
import com.example.timemaster.ml.FaceNetModel;
import com.example.timemaster.ml.FaceRecognitionProcessor;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * FaceRecognitionActivity
 * - Registration: open camera -> capture -> preprocess (mirror) -> get embedding -> save to Firestore
 * - Attendance: capture -> preprocess -> get embedding -> compare with Firestore embeddings
 */
@ExperimentalGetImage
public class FaceRecognitionActivity extends AppCompatActivity implements FaceRecognitionProcessor {

    private static final String TAG = "FaceRecognitionActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    private androidx.camera.view.PreviewView previewView;
    private TextView statusTextView;
    private com.example.timemaster.utils.camerax.GraphicOverlay graphicOverlay;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageAnalysis imageAnalysis;
    private ExecutorService cameraExecutor;
    private FaceDetector faceDetector;
    private FaceNetModel faceNetModel;

    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;

    private boolean isRegistration = false; // set via Intent extra if desired
    private static final int cameraFacing = CameraSelector.LENS_FACING_FRONT; // use front camera
    private static final float RECOGNITION_THRESHOLD = 1.0f; // tune this value

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_recognition);

        previewView = findViewById(R.id.preview_view);
        statusTextView = findViewById(R.id.text_status);
        graphicOverlay = findViewById(R.id.graphic_overlay);

        // Optionally read mode from intent
        isRegistration = getIntent().getBooleanExtra("isRegistration", false);

        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        // Initialize face detector (ML Kit)
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .setMinFaceSize(0.15f)
                .enableTracking()
                .build();
        faceDetector = FaceDetection.getClient(options);

        cameraExecutor = Executors.newSingleThreadExecutor();

        try {
            faceNetModel = new FaceNetModel(this);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load FaceNet model", e);
            Toast.makeText(this, "Không thể tải mô hình nhận diện", Toast.LENGTH_LONG).show();
        }

        updateStatusText(isRegistration ? "Hướng khuôn mặt vào camera để đăng ký" : "Hướng khuôn mặt vào camera để điểm danh");

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void updateStatusText(String text) {
        runOnUiThread(() -> statusTextView.setText(text));
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Quyền camera bị từ chối.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();

                // Use ImageAnalysis for real-time face detection
                imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(cameraFacing)
                        .build();

                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Set up face analyzer with GraphicOverlay
                imageAnalysis.setAnalyzer(cameraExecutor, new com.example.timemaster.utils.camerax.FaceAnalyzer(
                        graphicOverlay,
                        faceNetModel,
                        this,
                        cameraFacing
                ));

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

                Log.d(TAG, "Camera started with front-facing camera and face detection");
                updateStatusText("Camera đã sẵn sàng - ML Kit đang phát hiện khuôn mặt...");

            } catch (Exception e) {
                Log.e(TAG, "Error starting camera", e);
                Toast.makeText(this, "Lỗi khởi tạo camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private volatile boolean isProcessing = false;


    private void registerFaceEmbedding(float[] embedding) {
        if (isProcessing) {
            Log.w(TAG, "registerFaceEmbedding: already processing, skipping");
            return;
        }
        isProcessing = true;

        Log.d(TAG, "=== registerFaceEmbedding: START ===");
        Log.d(TAG, "Embedding length: " + (embedding != null ? embedding.length : "null"));

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "registerFaceEmbedding: currentUser is NULL - user not logged in!");
            runOnUiThread(() -> {
                Toast.makeText(this, "Bạn phải đăng nhập để đăng ký khuôn mặt", Toast.LENGTH_LONG).show();
                updateStatusText("Lỗi: Chưa đăng nhập");
            });
            isProcessing = false;
            return;
        }

        Log.d(TAG, "Current user UID: " + currentUser.getUid());
        Log.d(TAG, "Current user email: " + currentUser.getEmail());
        Log.d(TAG, "Is email verified: " + currentUser.isEmailVerified());

        if (embedding == null || embedding.length == 0) {
            Log.e(TAG, "registerFaceEmbedding: Invalid embedding");
            isProcessing = false;
            return;
        }

        List<Double> embeddingList = new ArrayList<>();
        for (float v : embedding) embeddingList.add((double) v);

        Log.d(TAG, "Converted embedding to List<Double>, size: " + embeddingList.size());

        Map<String, Object> data = new HashMap<>();
        data.put("faceEmbedding", embeddingList);
        data.put("timestamp", System.currentTimeMillis());
        data.put("userEmail", currentUser.getEmail());

        Log.d(TAG, "Firestore path: face_embeddings/" + currentUser.getUid());
        Log.d(TAG, "Attempting to save to Firestore...");

        updateStatusText("Đang lưu dữ liệu khuôn mặt...");

        firestore.collection("face_embeddings").document(currentUser.getUid())
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "=== Firestore SAVE SUCCESS! ===");
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Đăng ký khuôn mặt thành công", Toast.LENGTH_SHORT).show();
                        updateStatusText("✅ Đã đăng ký thành công");
                    });
                    isProcessing = false;
                    // Đợi 1 giây rồi thoát
                    new android.os.Handler().postDelayed(this::finish, 1000);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "=== Firestore SAVE FAILED! ===");
                    Log.e(TAG, "Error class: " + e.getClass().getName());
                    Log.e(TAG, "Error message: " + e.getMessage());
                    if (e.getCause() != null) {
                        Log.e(TAG, "Error cause: " + e.getCause().getMessage());
                    }
                    e.printStackTrace();

                    runOnUiThread(() -> {
                        String errorMsg = "Lưu thất bại: " + e.getMessage();
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                        updateStatusText("❌ " + errorMsg);
                    });
                    isProcessing = false;
                });
    }

    private void recognizeFaceEmbedding(float[] embedding) {
        if (isProcessing) return;
        isProcessing = true;

        updateStatusText("Đang điểm danh... Đang so khớp dữ liệu...");

        firestore.collection("face_embeddings").get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        updateStatusText("Chưa có dữ liệu để so sánh");
                        Toast.makeText(this, "Chưa có dữ liệu khuôn mặt đăng ký", Toast.LENGTH_SHORT).show();
                        isProcessing = false;
                        return;
                    }

                    String bestId = null;
                    double bestDistance = Double.MAX_VALUE;

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        @SuppressWarnings("unchecked")
                        List<Double> stored = (List<Double>) doc.get("faceEmbedding");
                        if (stored == null) continue;

                        float[] storedArray = new float[stored.size()];
                        for (int i = 0; i < stored.size(); i++) storedArray[i] = stored.get(i).floatValue();

                        double dist = l2Distance(embedding, storedArray);
                        if (dist < bestDistance) {
                            bestDistance = dist;
                            bestId = doc.getId();
                        }
                    }

                    if (bestId != null && bestDistance <= RECOGNITION_THRESHOLD) {
                        updateStatusText(String.format(Locale.getDefault(), "Điểm danh thành công (id=%s, dist=%.3f)", bestId, bestDistance));
                        Toast.makeText(this, "Điểm danh thành công", Toast.LENGTH_SHORT).show();
                        isProcessing = false;
                        finish();
                    } else {
                        updateStatusText("Không khớp với dữ liệu đã lưu - thử lại");
                        Toast.makeText(this, "Không tìm thấy kết quả phù hợp", Toast.LENGTH_SHORT).show();
                        isProcessing = false;
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load stored embeddings", e);
                    Toast.makeText(this, "Lỗi khi lấy dữ liệu", Toast.LENGTH_SHORT).show();
                    isProcessing = false;
                });
    }

    private double l2Distance(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return Double.MAX_VALUE;
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            double d = a[i] - b[i];
            sum += d * d;
        }
        return Math.sqrt(sum);
    }

    @Override
    public void onFaceDetected(Face face, float[] embedding) {
        // Called by FaceAnalyzer when face is detected and embedding is generated
        Log.d(TAG, "onFaceDetected called!");
        Log.d(TAG, "Face bounds: " + (face != null ? face.getBoundingBox() : "null"));
        Log.d(TAG, "Embedding: " + (embedding != null ? "length=" + embedding.length : "null"));
        Log.d(TAG, "isRegistration mode: " + isRegistration);

        if (embedding == null || embedding.length == 0) {
            Log.w(TAG, "onFaceDetected: embedding is null or empty - ABORTING");
            updateStatusText("Lỗi: Không tạo được embedding");
            return;
        }

        Log.d(TAG, "Face detected with valid embedding, processing in mode: " + (isRegistration ? "REGISTRATION" : "ATTENDANCE"));
        runOnUiThread(() -> {
            if (isRegistration) {
                Log.d(TAG, "Calling registerFaceEmbedding...");
                registerFaceEmbedding(embedding);
            } else {
                Log.d(TAG, "Calling recognizeFaceEmbedding...");
                recognizeFaceEmbedding(embedding);
            }
        });
    }

    @Override
    public void onFailure(Exception e) {
        Log.e(TAG, "FaceRecognitionProcessor failure", e);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) cameraExecutor.shutdown();
        if (faceDetector != null) faceDetector.close();
    }
}
