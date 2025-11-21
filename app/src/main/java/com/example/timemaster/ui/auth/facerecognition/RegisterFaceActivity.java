package com.example.timemaster.ui.auth.facerecognition;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.timemaster.R;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterFaceActivity extends AppCompatActivity {

    private static final String TAG = "RegisterFaceActivity";
    private static final int PERMISSION_REQUEST_CAMERA = 1001;

    private PreviewView previewView;
    private TextView tvInstruction;
    private ExecutorService cameraExecutor;
    private FaceDetector faceDetector;
    private FaceNetModel faceNetModel;
    private FirebaseFirestore firestore;

    private boolean isProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_face);

        previewView = findViewById(R.id.preview_view_register);
        tvInstruction = findViewById(R.id.tv_instruction);
        cameraExecutor = Executors.newSingleThreadExecutor();
        firestore = FirebaseFirestore.getInstance();

        try {
            faceNetModel = new FaceNetModel(this);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load FaceNet model.", e);
            Toast.makeText(this, "Không thể tải mô hình nhận dạng.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
        }
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();
        faceDetector = FaceDetection.getClient(options);

        imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
            if (isProcessing) {
                imageProxy.close();
                return;
            }
            @androidx.camera.core.ExperimentalGetImage
            android.media.Image mediaImage = imageProxy.getImage();
            if (mediaImage != null) {
                InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                faceDetector.process(image)
                        .addOnSuccessListener(faces -> {
                            processFaceForRegistration(faces, imageProxy);
                            imageProxy.close();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Face detection failed", e);
                            imageProxy.close();
                        });
            }
        });

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    private void processFaceForRegistration(List<Face> faces, ImageProxy imageProxy) {
        if (faces.isEmpty()) {
            runOnUiThread(() -> tvInstruction.setText("Không tìm thấy khuôn mặt"));
            return;
        }
        if (faces.size() > 1) {
            runOnUiThread(() -> tvInstruction.setText("Chỉ nên có một người trong khung hình"));
            return;
        }

        Face face = faces.get(0);

        boolean isLookingStraight = Math.abs(face.getHeadEulerAngleY()) < 10 && Math.abs(face.getHeadEulerAngleZ()) < 10;
        boolean areEyesOpen = face.getLeftEyeOpenProbability() != null && face.getLeftEyeOpenProbability() > 0.8
                            && face.getRightEyeOpenProbability() != null && face.getRightEyeOpenProbability() > 0.8;

        if (isLookingStraight && areEyesOpen) {
            isProcessing = true;
            runOnUiThread(() -> tvInstruction.setText("Khuôn mặt hợp lệ! Đang xử lý..."));
            captureAndSaveEmbedding(face, imageProxy);
        } else {
            runOnUiThread(() -> tvInstruction.setText("Vui lòng nhìn thẳng và mở mắt"));
        }
    }

    private void captureAndSaveEmbedding(Face face, ImageProxy imageProxy) {
        Bitmap faceBitmap = cropFaceFromImage(face, imageProxy);
        if (faceBitmap == null) {
            isProcessing = false;
            return;
        }

        float[] embedding = faceNetModel.getFaceEmbedding(faceBitmap);

        List<Float> embeddingList = new ArrayList<>();
        for (float f : embedding) {
            embeddingList.add(f);
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            isProcessing = false;
            return;
        }
        String userId = currentUser.getUid();
        Map<String, Object> data = new HashMap<>();
        data.put("faceEmbedding", embeddingList);

        firestore.collection("users").document(userId)
            .update(data)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Face embedding saved successfully!");
                runOnUiThread(() -> {
                    Toast.makeText(this, "Đã đăng ký khuôn mặt!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error saving embedding", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Đăng ký thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    isProcessing = false;
                });
            });
    }

    private Bitmap cropFaceFromImage(Face face, ImageProxy imageProxy) {
        @androidx.camera.core.ExperimentalGetImage
        Bitmap originalBitmap = imageProxy.toBitmap();
        Rect boundingBox = face.getBoundingBox();
        int left = Math.max(0, boundingBox.left);
        int top = Math.max(0, boundingBox.top);
        int width = boundingBox.width();
        int height = boundingBox.height();

        if (left + width > originalBitmap.getWidth()) width = originalBitmap.getWidth() - left;
        if (top + height > originalBitmap.getHeight()) height = originalBitmap.getHeight() - top;

        if (width <= 0 || height <= 0) return null;

        return Bitmap.createBitmap(originalBitmap, left, top, width, height);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA && allPermissionsGranted()) {
            startCamera();
        } else {
            Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        if (faceDetector != null) {
            faceDetector.close();
        }
    }
}
