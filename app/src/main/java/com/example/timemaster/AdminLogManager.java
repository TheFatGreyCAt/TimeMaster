package com.example.timemaster;

import com.example.timemaster.data.model.AuditLog;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminLogManager {

    public static void log(String action, String details) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String email = (user != null) ? user.getEmail() : "Unknown Admin";

        AuditLog log = new AuditLog(action, details, email, Timestamp.now());

        FirebaseFirestore.getInstance()
                .collection("admin_logs")
                .add(log)
                .addOnSuccessListener(documentReference -> {
                    // Ghi log thành công ngầm, không cần báo UI
                });
    }
}