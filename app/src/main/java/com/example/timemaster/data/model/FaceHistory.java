package com.example.timemaster.data.model;

public class FaceHistory {
    private String id;
    private String userId;
    private long timestamp;
    private float[] embeddings;
    private double confidence;
    private String status;  // "matched" or "rejected"

    public FaceHistory() {
        // Default constructor required for calls to DataSnapshot.getValue(FaceHistory.class)
    }

    public FaceHistory(String id, String userId, long timestamp, float[] embeddings, double confidence, String status) {
        this.id = id;
        this.userId = userId;
        this.timestamp = timestamp;
        this.embeddings = embeddings;
        this.confidence = confidence;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public float[] getEmbeddings() {
        return embeddings;
    }

    public void setEmbeddings(float[] embeddings) {
        this.embeddings = embeddings;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
