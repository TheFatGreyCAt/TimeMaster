package com.example.timemaster.data.repository;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class TimeRepositoryImpl implements TimeRepository {

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    @Override
    public void syncWithServerTime(OnServerTimeSynced callback) {
        final DocumentReference docRef = firestore.collection("_server_time").document("temp");
        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", FieldValue.serverTimestamp());

        docRef.set(data).addOnSuccessListener(aVoid -> {
            docRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Timestamp serverTimestamp = documentSnapshot.getTimestamp("timestamp");
                    if (serverTimestamp != null && callback != null) {
                        callback.onSuccess(serverTimestamp.toDate().getTime());
                    } else if (callback != null) {
                        callback.onFailure(new Exception("Server timestamp is null"));
                    }
                    docRef.delete();
                } else if (callback != null) {
                    callback.onFailure(new Exception("Document does not exist"));
                }
            }).addOnFailureListener(e -> {
                if (callback != null) callback.onFailure(e);
            });
        }).addOnFailureListener(e -> {
            if (callback != null) callback.onFailure(e);
        });
    }
}
