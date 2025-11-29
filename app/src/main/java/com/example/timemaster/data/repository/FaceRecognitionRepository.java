package com.example.timemaster.data.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FaceRecognitionRepository {

    private final FirebaseFunctions functions;

    public FaceRecognitionRepository() {
        functions = FirebaseFunctions.getInstance();
    }

    /**
     * Calls the 'registerFaceEmbeddings' Cloud Function.
     * @param embeddings A list of 3 face embeddings.
     * @return The result from the Cloud Function.
     */
    public Task<HttpsCallableResult> registerFaceEmbeddings(List<float[]> embeddings) {
        Map<String, Object> data = new HashMap<>();
        // Convert float[] to List<Float> for JSON compatibility
        List<List<Float>> embeddingsAsLists = new ArrayList<>();
        for (float[] emb : embeddings) {
            List<Float> list = new ArrayList<>();
            for (float val : emb) {
                list.add(val);
            }
            embeddingsAsLists.add(list);
        }
        data.put("embeddings", embeddingsAsLists);

        return functions
                .getHttpsCallable("registerFaceEmbeddings")
                .call(data);
    }

    /**
     * Calls the 'recordAttendance' Cloud Function for recognition.
     * @param embedding The new face embedding to recognize.
     * @param type The type of attendance ("check-in" or "check-out").
     * @param kioskId The ID of the device.
     * @return A map containing the result message.
     */
    public Task<Map<String, Object>> recordAttendance(float[] embedding, String type, String kioskId) {
        Map<String, Object> data = new HashMap<>();
        // Convert float[] to List<Float> for JSON compatibility
        List<Float> embeddingAsList = new ArrayList<>();
        for (float val : embedding) {
            embeddingAsList.add(val);
        }
        data.put("embedding", embeddingAsList);
        data.put("type", type);
        data.put("kioskId", kioskId);

        return functions
                .getHttpsCallable("recordAttendance")
                .call(data)
                .continueWith(task -> (Map<String, Object>) task.getResult().getData());
    }
}
