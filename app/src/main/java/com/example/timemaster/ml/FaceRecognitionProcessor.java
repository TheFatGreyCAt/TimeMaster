package com.example.timemaster.ml;

import com.google.mlkit.vision.face.Face;

public interface FaceRecognitionProcessor {
    void onFaceDetected(Face face, float[] embedding);
    void onFailure(Exception e);
}
