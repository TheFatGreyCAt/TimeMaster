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

    // TUNABLE PARAMETERS: adjust to find the best balance between FPR and FNR
    private static final double RECOGNITION_THRESHOLD = 0.85; // stricter than before (was 1.0)
    private static final double MIN_DISTANCE_MARGIN = 0.25; // required (secondBest - best) margin to accept a match
    private static final double NEARBY_DISTANCE_THRESHOLD = 1.2; // for sanity counting of nearby candidates

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
                updateStatusText("Camera đã sẵn sàng - hệ thống đang phát hiện khuôn mặt...");

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
                    Log.d(TAG, "Firestore SAVE SUCCESS!");
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Đăng ký khuôn mặt thành công", Toast.LENGTH_SHORT).show();
                        updateStatusText("Đã đăng ký thành công");
                    });
                    isProcessing = false;
                    // Đợi 1 giây rồi thoát
                    new android.os.Handler().postDelayed(this::finish, 1000);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore SAVE FAILED!");
                    Log.e(TAG, "Error class: " + e.getClass().getName());
                    Log.e(TAG, "Error message: " + e.getMessage());
                    if (e.getCause() != null) {
                        Log.e(TAG, "Error cause: " + e.getCause().getMessage());
                    }
                    e.printStackTrace();

                    runOnUiThread(() -> {
                        String errorMsg = "Lưu thất bại: " + e.getMessage();
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                        updateStatusText(errorMsg);
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
                    double secondBestDistance = Double.MAX_VALUE;

                    int candidateCount = 0; // number of embeddings within a reasonable distance

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        @SuppressWarnings("unchecked")
                        List<Double> stored = (List<Double>) doc.get("faceEmbedding");
                        if (stored == null) continue;

                        float[] storedArray = new float[stored.size()];
                        for (int i = 0; i < stored.size(); i++) storedArray[i] = stored.get(i).floatValue();

                        double dist = l2Distance(embedding, storedArray);

                        // Count candidates within a loose nearby threshold for diagnostics
                        if (dist <= NEARBY_DISTANCE_THRESHOLD) candidateCount++;

                        if (dist < bestDistance) {
                            secondBestDistance = bestDistance;
                            bestDistance = dist;
                            bestId = doc.getId();
                        } else if (dist < secondBestDistance) {
                            secondBestDistance = dist;
                        }
                    }

                    // Evaluate acceptance with margin and threshold rules
                    String decisionReason = "NO_MATCH";
                    boolean accept = false;

                    if (bestId != null && bestDistance <= RECOGNITION_THRESHOLD) {
                        double margin = (secondBestDistance == Double.MAX_VALUE) ? Double.MAX_VALUE : (secondBestDistance - bestDistance);

                        Log.d(TAG, "Best distance=" + bestDistance + ", secondBest=" + secondBestDistance + ", margin=" + margin + ", candidatesNearby=" + candidateCount);

                        // If there is no second best (only one candidate), require a stronger guard
                        if (secondBestDistance == Double.MAX_VALUE) {
                            // Accept only if best distance is significantly below threshold (conservative)
                            if (bestDistance <= (RECOGNITION_THRESHOLD * 0.9)) {
                                accept = true;
                                decisionReason = "SINGLE_CANDIDATE_CONFIDENT";
                            } else {
                                accept = false;
                                decisionReason = "SINGLE_CANDIDATE_NOT_CONFIDENT";
                            }
                        } else {
                            if (margin >= MIN_DISTANCE_MARGIN) {
                                accept = true;
                                decisionReason = "MARGIN_PASSED";
                            } else {
                                // margin too small -> ambiguous
                                accept = false;
                                decisionReason = "MARGIN_TOO_SMALL";
                            }
                        }
                    } else {
                        // Best distance exceeds threshold
                        decisionReason = "DISTANCE_TOO_LARGE";
                        accept = false;
                    }

                    if (accept) {
                        Log.d(TAG, "FACE RECOGNITION SUCCESS (accept). ID=" + bestId + ", dist=" + bestDistance + ", reason=" + decisionReason);
                        saveAttendanceRecord(bestId, bestDistance, secondBestDistance, decisionReason, candidateCount);
                    } else {
                        Log.w(TAG, "FACE RECOGNITION REJECTED. bestId=" + bestId + ", bestDistance=" + bestDistance + ", secondBest=" + secondBestDistance + ", reason=" + decisionReason);
                        updateStatusText("Không khớp với dữ liệu đã lưu - " + friendlyReason(decisionReason));
                        Toast.makeText(this, "Không tìm thấy kết quả phù hợp: " + friendlyReason(decisionReason), Toast.LENGTH_SHORT).show();
                        isProcessing = false;
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load stored embeddings", e);
                    Toast.makeText(this, "Lỗi khi lấy dữ liệu", Toast.LENGTH_SHORT).show();
                    isProcessing = false;
                });
    }

    private String friendlyReason(String code) {
        switch (code) {
            case "MARGIN_TOO_SMALL": return "Kết quả không rõ ràng (nhiều ứng viên giống nhau)";
            case "SINGLE_CANDIDATE_NOT_CONFIDENT": return "Không đủ độ tin cậy";
            case "DISTANCE_TOO_LARGE": return "Khoảng cách nhận diện quá lớn";
            default: return "Không khớp";
        }
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

    /**
     * Lưu thông tin điểm danh vào Firestore collection "attendanceRecords"
     * Logic: Lần đầu trong ngày = CHECK_IN, lần sau = CHECK_OUT, có thể nhiều lần
     * KHÔNG CẦN ĐĂNG NHẬP - Chỉ cần nhận diện khuôn mặt
     * @param userId ID của user được nhận diện
     * @param confidence Độ chính xác (L2 distance, càng nhỏ càng tốt)
     */
    private void saveAttendanceRecord(String userId, double confidence, double secondBest, String decisionReason, int candidateCount) {
        Log.d(TAG, "saveAttendanceRecord: START");
        Log.d(TAG, "User ID: " + userId);
        Log.d(TAG, "Confidence (distance): " + confidence);
        Log.d(TAG, "Second best distance: " + secondBest);
        Log.d(TAG, "Decision reason: " + decisionReason);

        // Lấy ngày hiện tại
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new java.util.Date());

        Log.d(TAG, "Checking last attendance record for today: " + today);
        updateStatusText("Đang kiểm tra trạng thái điểm danh...");

        // Query record cuối cùng trong ngày hôm nay
        firestore.collection("attendanceRecords")
                .whereEqualTo("uid", userId)
                .whereEqualTo("date", today)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    String checkType;

                    if (querySnapshot.isEmpty()) {
                        // Chưa có record nào trong ngày -> CHECK_IN
                        checkType = "CHECK_IN";
                        Log.d(TAG, "No record today -> CHECK_IN");
                    } else {
                        // Có record -> Kiểm tra type của record cuối cùng
                        DocumentSnapshot lastRecord = querySnapshot.getDocuments().get(0);
                        String lastCheckType = lastRecord.getString("checkType");

                        if ("CHECK_IN".equals(lastCheckType)) {
                            // Record cuối là CHECK_IN -> Bây giờ là CHECK_OUT
                            checkType = "CHECK_OUT";
                            Log.d(TAG, "Last record was CHECK_IN -> Now CHECK_OUT");
                        } else {
                            // Record cuối là CHECK_OUT -> Bây giờ là CHECK_IN (check-in lại)
                            checkType = "CHECK_IN";
                            Log.d(TAG, "Last record was CHECK_OUT -> Now CHECK_IN (again)");
                        }
                    }

                    // Lưu record mới với checkType đã xác định
                    saveAttendanceRecordWithType(userId, confidence, secondBest, decisionReason, candidateCount, checkType);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check last record, defaulting to CHECK_IN", e);
                    // Nếu lỗi khi query, mặc định là CHECK_IN
                    saveAttendanceRecordWithType(userId, confidence, secondBest, decisionReason, candidateCount, "CHECK_IN");
                });
    }

    private void saveAttendanceRecordWithType(String userId, double confidence, double secondBest, String decisionReason, int candidateCount, String checkType) {
        Log.d(TAG, "=== saveAttendanceRecordWithType ===");
        Log.d(TAG, "User ID: " + userId);
        Log.d(TAG, "Check Type: " + checkType);

        // Lấy email từ face_embeddings collection dựa trên userId
        firestore.collection("face_embeddings").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String userEmail = documentSnapshot.getString("userEmail");

                    if (userEmail == null || userEmail.isEmpty()) {
                        Log.e(TAG, "Cannot get userEmail from face_embeddings");
                        userEmail = "unknown@example.com"; // Fallback
                    }

                    Log.d(TAG, "Retrieved user email: " + userEmail);

                    final String userEmailFinal = userEmail; // make effectively final for nested lambdas

                    // Now try to fetch fullName from users collection for friendly message
                    firestore.collection("users").document(userId)
                            .get()
                            .addOnSuccessListener(userDoc -> {
                                String fullName = userDoc != null ? userDoc.getString("fullName") : null;
                                if (fullName == null || fullName.isEmpty()) {
                                    // Fallback: use email's local part or a generic label
                                    final String fallbackName = (userEmailFinal != null && userEmailFinal.contains("@")) ? userEmailFinal.split("@")[0] : "User";
                                    fullName = fallbackName;
                                }
                                // Tiến hành lưu attendance record với fullName
                                saveAttendanceRecordToFirestore(userId, userEmailFinal, fullName, confidence, secondBest, decisionReason, candidateCount, checkType);
                            })
                            .addOnFailureListener(e -> {
                                Log.w(TAG, "Failed to fetch user fullName, using fallback", e);
                                final String fallbackName = (userEmailFinal != null && userEmailFinal.contains("@")) ? userEmailFinal.split("@")[0] : "User";
                                saveAttendanceRecordToFirestore(userId, userEmailFinal, fallbackName, confidence, secondBest, decisionReason, candidateCount, checkType);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get user email, using fallback", e);
                    // Nếu không lấy được email, vẫn lưu với email mặc định
                    String fallbackEmail = "unknown@example.com";
                    String fallbackName = "User";
                    saveAttendanceRecordToFirestore(userId, fallbackEmail, fallbackName, confidence, secondBest, decisionReason, candidateCount, checkType);
                });
    }

    // Modified signature: added 'fullName' parameter
    private void saveAttendanceRecordToFirestore(String userId, String userEmail, String fullName, double confidence, double secondBest, String decisionReason, int candidateCount, String checkType) {
        long timestamp = System.currentTimeMillis();
        String recordId = firestore.collection("attendanceRecords").document().getId();

        Map<String, Object> attendanceData = new HashMap<>();
        attendanceData.put("uid", userId);
        attendanceData.put("userEmail", userEmail);
        attendanceData.put("fullName", fullName);
        attendanceData.put("timestamp", timestamp);
        attendanceData.put("date", new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new java.util.Date(timestamp)));
        attendanceData.put("time", new java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new java.util.Date(timestamp)));
        attendanceData.put("checkType", checkType);  // CHECK_IN hoặc CHECK_OUT
        attendanceData.put("method", "FACE_RECOGNITION");
        attendanceData.put("confidence", confidence);
        attendanceData.put("status", "PRESENT");
        attendanceData.put("deviceInfo", android.os.Build.MODEL);

        // Additional audit fields to help diagnose false positives
        attendanceData.put("matchDecisionReason", decisionReason);
        attendanceData.put("secondBestDistance", secondBest);
        attendanceData.put("nearbyCandidateCount", candidateCount);

        Log.d(TAG, "Attendance record ID: " + recordId);
        Log.d(TAG, "Saving to Firestore...");

        String statusMessage = checkType.equals("CHECK_IN") ? "Đang lưu check-in..." : "Đang lưu check-out...";
        updateStatusText(statusMessage);

        firestore.collection("attendanceRecords").document(recordId)
                .set(attendanceData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "=== ATTENDANCE RECORD SAVED SUCCESSFULLY ===");

                    // New welcome message using fullName
                    runOnUiThread(() -> {
                        String message = String.format(Locale.getDefault(), "Chào mừng, %s đã điểm danh thành công", fullName);
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                        updateStatusText(message);
                    });
                    isProcessing = false;

                    // Đợi 1.5 giây rồi thoát
                    new android.os.Handler().postDelayed(this::finish, 1500);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "=== FAILED TO SAVE ATTENDANCE RECORD ===");
                    Log.e(TAG, "Error: " + e.getMessage());
                    e.printStackTrace();

                    runOnUiThread(() -> {
                        String errorMsg = "Lưu điểm danh thất bại: " + e.getMessage();
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                        updateStatusText(errorMsg);
                    });
                    isProcessing = false;
                });
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
