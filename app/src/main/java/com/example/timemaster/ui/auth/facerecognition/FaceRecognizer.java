package com.example.timemaster.ui.auth.facerecognition;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FaceRecognizer {

    private static final String TAG = "FaceRecognizer";
    private final FirebaseFunctions functions;

    public interface RecognitionListener {
        void onRecognitionSuccess(String role);
        void onRecognitionFailure(String message);
    }

    public FaceRecognizer() {
        this.functions = FirebaseFunctions.getInstance();
    }

    public void recognizeFace(float[] newEmbedding, RecognitionListener listener) {
        // Chuyển mảng float[] thành List để gửi đi
        List<Float> embeddingList = new ArrayList<>();
        for (float f : newEmbedding) {
            embeddingList.add(f);
        }

        // Chuẩn bị dữ liệu để gửi lên function
        Map<String, Object> data = new HashMap<>();
        data.put("embedding", embeddingList);

        // Gọi function có tên là "findUserByFace"
        callCloudFunction("findUserByFace", data)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Function trả về kết quả thành công
                        HttpsCallableResult result = task.getResult();
                        if (result != null && result.getData() instanceof Map) {
                            Map<String, Object> resultData = (Map<String, Object>) result.getData();
                            String role = (String) resultData.get("role");
                            listener.onRecognitionSuccess(role);
                        } else {
                            listener.onRecognitionFailure("Dữ liệu trả về không hợp lệ.");
                        }
                    } else {
                        // Function trả về lỗi (ví dụ: not-found)
                        Exception e = task.getException();
                        String errorMessage = e != null ? e.getMessage() : "Lỗi không xác định";
                        Log.w(TAG, "Error calling cloud function: " + errorMessage);
                        // Cắt bỏ phần tiền tố không cần thiết từ thông báo lỗi của Firebase
                        if (errorMessage != null && errorMessage.contains(":")) {
                            errorMessage = errorMessage.substring(errorMessage.indexOf(":") + 1).trim();
                        }
                        listener.onRecognitionFailure(errorMessage);
                    }
                });
    }

    private Task<HttpsCallableResult> callCloudFunction(String functionName, Map<String, Object> data) {
        return functions
                .getHttpsCallable(functionName)
                .call(data);
    }
}
