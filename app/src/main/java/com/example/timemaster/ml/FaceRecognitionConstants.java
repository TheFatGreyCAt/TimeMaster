package com.example.timemaster.ml;

public class FaceRecognitionConstants {
    // Face Detection Thresholds
    public static final double FACE_DETECTION_THRESHOLD = 0.6;
    public static final int MIN_FACE_SIZE = 200;  // pixels
    public static final float MAX_HEAD_EULER_ANGLE = 20.0f; // degrees, increased for flexibility
    public static final float MIN_EYE_OPEN_PROBABILITY = 0.4f;

    // Face Embeddings
    public static final int FACE_EMBEDDING_DIMENSION = 512; // Corrected from 128 to 512
    public static final int FACE_IMAGE_SIZE = 160;  // 160x160 pixels

    // TensorFlow Lite Model
    public static final String TF_LITE_MODEL_PATH = "mobile_face_net.tflite";
    public static final String FACE_EMBEDDER_MODEL_PATH = "face_embedder.tflite";

    // Timing
    public static final long DUPLICATE_ATTENDANCE_WINDOW_MS = 60_000;  // 1 minute
    public static final long CAPTURE_BUTTON_DISABLE_MS = 3_000;  // 3 seconds
    public static final long REGISTRATION_IMAGE_DELAY_MS = 500;  // 500ms between captures

    // Registration
    public static final int REGISTRATION_IMAGE_COUNT = 10;
    public static final double MIN_REGISTRATION_CONFIDENCE = 0.75;
}
