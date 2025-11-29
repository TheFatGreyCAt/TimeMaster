package com.example.timemaster.data.model;

public class FaceEmbedding {
    private String userId;
    private float[] embeddings;  // 128D vector
    private long registeredAt;
    private boolean isActive;

    public FaceEmbedding() {
        // Default constructor required for calls to DataSnapshot.getValue(FaceEmbedding.class)
    }

    public FaceEmbedding(String userId, float[] embeddings, long registeredAt, boolean isActive) {
        this.userId = userId;
        this.embeddings = embeddings;
        this.registeredAt = registeredAt;
        this.isActive = isActive;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public float[] getEmbeddings() {
        return embeddings;
    }

    public void setEmbeddings(float[] embeddings) {
        this.embeddings = embeddings;
    }

    public long getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(long registeredAt) {
        this.registeredAt = registeredAt;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
