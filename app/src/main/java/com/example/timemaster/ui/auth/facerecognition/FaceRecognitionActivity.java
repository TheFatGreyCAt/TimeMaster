package com.example.timemaster.ui.auth.facerecognition;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.example.timemaster.ui.dashboard.DashboardHostActivity;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FaceRecognitionActivity extends AppCompatActivity {

    private static final String TAG = "FaceRecognitionActivity";
    private static final int PERMISSION_REQUEST_CAMERA = 1001;

    private PreviewView previewView;
    private TextView tvStatus;
    private ExecutorService cameraExecutor;
    private FaceDetector faceDetector;
    private FaceNetModel faceNetModel;
    private FaceRecognizer faceRecognizer;

    private boolean isProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_recognition);

        previewView = findViewById(R.id.preview_view);
        tvStatus = findViewById(R.id.tv_status);
        cameraExecutor = Executors.newSingleThreadExecutor();

        try {
            faceNetModel = new FaceNetModel(this);
            faceRecognizer = new FaceRecognizer();
        } catch (Exception e) {
            Log.e(TAG, "Failed to load models.", e);
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
                            if (!faces.isEmpty()) {
                                isProcessing = true;
                                runOnUiThread(() -> tvStatus.setText("Đang nhận dạng..."));
                                processFaceForRecognition(faces.get(0), imageProxy);
                            }
                            imageProxy.close();
                        })
                        .addOnFailureListener(e -> imageProxy.close());
            }
        });

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    private void processFaceForRecognition(Face face, ImageProxy imageProxy) {
        Bitmap faceBitmap = cropFaceFromImage(face, imageProxy);
        if (faceBitmap == null) {
            resetProcessing();
            return;
        }

        float[] newEmbedding = faceNetModel.getFaceEmbedding(faceBitmap);

        faceRecognizer.recognizeFace(newEmbedding, new FaceRecognizer.RecognitionListener() {
            @Override
            public void onRecognitionSuccess(String role) {
                runOnUiThread(() -> {
                    Toast.makeText(FaceRecognitionActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                    navigateToDashboard(role);
                });
            }

            @Override
            public void onRecognitionFailure(String message) {
                runOnUiThread(() -> tvStatus.setText(message));
                resetProcessing();
            }
        });
    }

    private void navigateToDashboard(String role) {
        Intent i = new Intent(this, DashboardHostActivity.class);
        i.putExtra(DashboardHostActivity.EXTRA_ROLE, role);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void resetProcessing() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> isProcessing = false, 1000);
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
