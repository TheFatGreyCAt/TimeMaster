package com.example.timemaster.utils.camerax;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.ImageAnalysis;

import com.example.timemaster.ml.FaceNetModel;
import com.example.timemaster.ml.FaceRecognitionProcessor;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;
import java.util.Locale;

public class FaceAnalyzer implements ImageAnalysis.Analyzer {

    private static final String TAG = "FaceAnalyzer";
    private static final float MIN_FACE_SIZE_RATIO = 0.4f; // Face must occupy at least 40% of the guide frame width
    private static final long ANALYSIS_INTERVAL_MS = 300; // Analyze every 300ms to optimize performance
    private static final float MIN_EYE_OPEN_PROBABILITY = 0.5f; // Liveness detection - eyes must be open
    private static final float MAX_HEAD_ANGLE = 15.0f; // Maximum head rotation angle in degrees
    private static final int VALID_FACE_COUNT_THRESHOLD = 3; // Require 3 consecutive valid detections

    private final GraphicOverlay graphicOverlay;
    private final FaceDetector detector;
    private final FaceNetModel faceNetModel;
    private final FaceRecognitionProcessor listener;
    private final int cameraFacing;

    private long lastAnalysisTime = 0;
    private int validFaceCount = 0;
    private boolean isProcessing = false;
    private String currentMessage = "Đặt khuôn mặt vào khung hình";
    private boolean needsUpdateSourceInfo = true;
    private ImageProxy currentImageProxy; // Store current ImageProxy for bitmap conversion

    public FaceAnalyzer(GraphicOverlay graphicOverlay, FaceNetModel faceNetModel, FaceRecognitionProcessor listener, int cameraFacing) {
        this.graphicOverlay = graphicOverlay;
        this.faceNetModel = faceNetModel;
        this.listener = listener;
        this.cameraFacing = cameraFacing;

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL) // Enable classification for liveness detection
                .setMinFaceSize(0.15f)
                .enableTracking() // Enable face tracking for better performance
                .build();
        this.detector = FaceDetection.getClient(options);
    }

    @Override
    @SuppressLint("UnsafeOptInUsageError")
    public void analyze(@NonNull ImageProxy imageProxy) {
        // Update image source info on first frame
        if (needsUpdateSourceInfo) {
            Image mediaImage = imageProxy.getImage();
            if (mediaImage != null) {
                graphicOverlay.setImageSourceInfo(
                    mediaImage.getWidth(),
                    mediaImage.getHeight(),
                    cameraFacing
                );
                needsUpdateSourceInfo = false;
            }
        }

        // Do not proceed if the overlay is not ready
        if (graphicOverlay.getWidth() == 0 || graphicOverlay.getHeight() == 0) {
            imageProxy.close();
            return;
        }

        // Throttle analysis to optimize performance
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAnalysisTime < ANALYSIS_INTERVAL_MS || isProcessing) {
            imageProxy.close();
            return;
        }
        lastAnalysisTime = currentTime;
        isProcessing = true;

        Image mediaImage = imageProxy.getImage();
        if (mediaImage == null) {
            imageProxy.close();
            isProcessing = false;
            return;
        }

        // Store ImageProxy for bitmap conversion
        currentImageProxy = imageProxy;

        InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

        detector.process(image)
                .addOnSuccessListener(faces -> processFaces(faces, imageProxy))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Face detection failed", e);
                    listener.onFailure(e);
                    resetValidation();
                })
                .addOnCompleteListener(task -> {
                    imageProxy.close();
                    currentImageProxy = null;
                    isProcessing = false;
                });
    }

    /**
     * Process detected faces and perform validation
     */
    @androidx.camera.core.ExperimentalGetImage
    private void processFaces(List<Face> faces, ImageProxy imageProxy) {
        graphicOverlay.clear();

        // Only process when there is exactly 1 face
        if (faces.size() != 1) {
            if (faces.size() > 1) {
                currentMessage = "Chỉ một người trong khung hình";
            } else {
                currentMessage = "Không phát hiện khuôn mặt";
            }
            updateUI(null, false, currentMessage);
            resetValidation();
            return;
        }

        Face face = faces.get(0);
        FaceValidationResult validation = validateFace(face);

        if (validation.isValid) {
            validFaceCount++;

            if (validFaceCount >= VALID_FACE_COUNT_THRESHOLD) {
                // Face has been valid for enough consecutive frames, proceed with recognition
                processFaceRecognition(face, imageProxy);
                validFaceCount = 0; // Reset after successful recognition
            } else {
                // Face is valid but need more consecutive detections
                currentMessage = String.format(Locale.getDefault(), "Giữ nguyên... %d/%d", validFaceCount, VALID_FACE_COUNT_THRESHOLD);
                updateUI(face, true, currentMessage);
            }
        } else {
            // Face is not valid, show the reason
            currentMessage = validation.message;
            updateUI(face, false, currentMessage);
            resetValidation();
        }
    }

    /**
     * Validate face position, size, and liveness
     */
    private FaceValidationResult validateFace(Face face) {
        float overlayWidth = graphicOverlay.getWidth();
        float overlayHeight = graphicOverlay.getHeight();
        float validWidth = overlayWidth * 0.75f;
        float validHeight = overlayHeight * 0.5f;

        RectF validArea = new RectF(
            (overlayWidth - validWidth) / 2,
            (overlayHeight - validHeight) / 2,
            (overlayWidth + validWidth) / 2,
            (overlayHeight + validHeight) / 2
        );

        // Transform face bounding box to overlay coordinates
        RectF faceRect = new RectF(face.getBoundingBox());
        graphicOverlay.getTransformMatrix().mapRect(faceRect);

        // Check if the face is within the valid area
        if (!validArea.contains(faceRect)) {
            return new FaceValidationResult(false, "Đưa khuôn mặt vào khung hình");
        }

        // Check if the face is large enough
        if (faceRect.width() < validWidth * MIN_FACE_SIZE_RATIO) {
            return new FaceValidationResult(false, "Di chuyển gần hơn");
        }

        // Check head rotation angles
        Float headEulerAngleY = face.getHeadEulerAngleY(); // Yaw (left-right rotation)
        Float headEulerAngleZ = face.getHeadEulerAngleZ(); // Roll (tilt)

        if (headEulerAngleY != null && Math.abs(headEulerAngleY) > MAX_HEAD_ANGLE) {
            return new FaceValidationResult(false, "Nhìn thẳng vào camera");
        }

        if (headEulerAngleZ != null && Math.abs(headEulerAngleZ) > MAX_HEAD_ANGLE) {
            return new FaceValidationResult(false, "Giữ đầu thẳng");
        }

        // Liveness detection - check if eyes are open
        Float leftEyeOpenProbability = face.getLeftEyeOpenProbability();
        Float rightEyeOpenProbability = face.getRightEyeOpenProbability();

        if (leftEyeOpenProbability != null && rightEyeOpenProbability != null) {
            if (leftEyeOpenProbability < MIN_EYE_OPEN_PROBABILITY ||
                rightEyeOpenProbability < MIN_EYE_OPEN_PROBABILITY) {
                return new FaceValidationResult(false, "Mở to mắt");
            }
        }

        return new FaceValidationResult(true, "Giữ nguyên tư thế");
    }

    /**
     * Process face recognition after validation
     */
    @androidx.camera.core.ExperimentalGetImage
    private void processFaceRecognition(Face face, ImageProxy imageProxy) {
        try {
            Bitmap bitmap = imageProxyToBitmap(imageProxy);
            if (bitmap == null) {
                listener.onFailure(new Exception("Cannot convert ImageProxy to Bitmap"));
                resetValidation();
                return;
            }

            // Mirror bitmap if using front camera
            if (cameraFacing == androidx.camera.core.CameraSelector.LENS_FACING_FRONT) {
                android.graphics.Matrix matrix = new android.graphics.Matrix();
                matrix.preScale(-1.0f, 1.0f);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
            }

            float[] embedding = faceNetModel.getFaceEmbedding(bitmap, new RectF(face.getBoundingBox()));
            if (embedding != null) {
                listener.onFaceDetected(face, embedding);
                currentMessage = "Đang nhận diện...";
                updateUI(face, true, currentMessage);
            } else {
                listener.onFailure(new Exception("Failed to generate face embedding"));
                resetValidation();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in processFaceRecognition", e);
            listener.onFailure(e);
            resetValidation();
        }
    }

    /**
     * Convert ImageProxy to Bitmap
     */
    @androidx.camera.core.ExperimentalGetImage
    private Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
        try {
            Image image = imageProxy.getImage();
            if (image == null) {
                return null;
            }

            // Convert YUV_420_888 to NV21
            byte[] nv21 = yuv420ToNv21(image);

            // Use YuvImage to decode NV21 to Bitmap
            android.graphics.YuvImage yuvImage = new android.graphics.YuvImage(
                nv21,
                android.graphics.ImageFormat.NV21,
                image.getWidth(),
                image.getHeight(),
                null
            );

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            yuvImage.compressToJpeg(
                new android.graphics.Rect(0, 0, image.getWidth(), image.getHeight()),
                100,
                out
            );

            byte[] imageBytes = out.toByteArray();
            Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

            // Apply rotation if needed
            int rotation = imageProxy.getImageInfo().getRotationDegrees();
            if (rotation != 0) {
                android.graphics.Matrix matrix = new android.graphics.Matrix();
                matrix.postRotate(rotation);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }

            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error converting ImageProxy to Bitmap", e);
            return null;
        }
    }

    /**
     * Convert YUV_420_888 Image to NV21 byte array
     */
    @androidx.camera.core.ExperimentalGetImage
    private byte[] yuv420ToNv21(Image image) {
        Image.Plane[] planes = image.getPlanes();
        Image.Plane yPlane = planes[0];
        Image.Plane uPlane = planes[1];
        Image.Plane vPlane = planes[2];

        java.nio.ByteBuffer yBuffer = yPlane.getBuffer();
        java.nio.ByteBuffer uBuffer = uPlane.getBuffer();
        java.nio.ByteBuffer vBuffer = vPlane.getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

        // Y
        yBuffer.get(nv21, 0, ySize);

        // U and V are swapped for NV21
        int position = ySize;
        for (int i = 0; i < vSize; i++) {
            nv21[position++] = vBuffer.get(i);
            nv21[position++] = uBuffer.get(i);
        }

        return nv21;
    }

    /**
     * Update UI with face graphic and message
     */
    private void updateUI(Face face, boolean isValid, String message) {
        FaceGraphic faceGraphic = new FaceGraphic(graphicOverlay, message);
        graphicOverlay.add(faceGraphic);
        faceGraphic.updateFace(face, isValid);
    }

    /**
     * Reset validation counter
     */
    private void resetValidation() {
        validFaceCount = 0;
    }

    /**
     * Release resources
     */
    public void release() {
        detector.close();
    }

    /**
     * Helper class to hold validation result
     */
    private static class FaceValidationResult {
        final boolean isValid;
        final String message;

        FaceValidationResult(boolean isValid, String message) {
            this.isValid = isValid;
            this.message = message;
        }
    }
}
