package com.example.timemaster.ml;

import java.util.List;

public class EmbeddingsUtils {

    /**
     * Tính cosine similarity giữa 2 vectors
     */
    public static double cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1.length != vec2.length) {
            throw new IllegalArgumentException("Vectors must have same length");
        }

        float dotProduct = 0f;
        float magnitude1 = 0f;
        float magnitude2 = 0f;

        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            magnitude1 += vec1[i] * vec1[i];
            magnitude2 += vec2[i] * vec2[i];
        }

        magnitude1 = (float) Math.sqrt(magnitude1);
        magnitude2 = (float) Math.sqrt(magnitude2);

        if (magnitude1 == 0f || magnitude2 == 0f) {
            return 0.0;
        }

        return dotProduct / (magnitude1 * magnitude2);
    }

    /**
     * L2 Normalization của embeddings
     */
    public static float[] normalizeEmbeddings(float[] embeddings) {
        float norm = 0f;

        for (float value : embeddings) {
            norm += value * value;
        }

        norm = (float) Math.sqrt(norm);

        if (norm == 0f) {
            return embeddings;
        }

        float[] normalized = new float[embeddings.length];
        for (int i = 0; i < embeddings.length; i++) {
            normalized[i] = embeddings[i] / norm;
        }

        return normalized;
    }

    /**
     * Tính average embeddings từ danh sách các embeddings
     */
    public static float[] averageEmbeddings(List<float[]> embeddingsList) {
        if (embeddingsList.isEmpty()) {
            return null;
        }

        int dimension = embeddingsList.get(0).length;
        float[] average = new float[dimension];

        for (float[] embeddings : embeddingsList) {
            for (int i = 0; i < dimension; i++) {
                average[i] += embeddings[i];
            }
        }

        for (int i = 0; i < dimension; i++) {
            average[i] /= embeddingsList.size();
        }

        return normalizeEmbeddings(average);
    }
}
