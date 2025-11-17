package com.example.timemaster.model;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class TimeViewModel extends ViewModel {

    private static final String TAG = "TimeViewModel";

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final MutableLiveData<String> currentTime = new MutableLiveData<>();
    private final MutableLiveData<String> currentDate = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private long serverReferenceTimestamp = 0L; // milliseconds
    private long deviceReferenceTimestamp = 0L; // milliseconds
    private boolean hasSynced = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable timeUpdateRunnable;

    public TimeViewModel() {
        Log.d(TAG, "TimeViewModel created");
        startAutoUpdate();
    }

    public void syncWithServerTime(OnServerTimeSynced callback) {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        final DocumentReference docRef = firestore.collection("_server_time").document("temp");
        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", FieldValue.serverTimestamp());
        Log.d(TAG, "Fetching server time from Firestore...");

        docRef.set(data).addOnSuccessListener(aVoid -> {
            docRef.get().addOnSuccessListener(documentSnapshot -> {
                isLoading.setValue(false);

                if (documentSnapshot.exists()) {
                    Timestamp serverTimestamp = documentSnapshot.getTimestamp("timestamp");
                    if (serverTimestamp != null) {
                        serverReferenceTimestamp = serverTimestamp.toDate().getTime();
                        deviceReferenceTimestamp = System.currentTimeMillis();
                        hasSynced = true;
                        Log.d(TAG, "Server time synced: " + new Date(serverReferenceTimestamp));
                        if (callback != null) callback.onSuccess(serverReferenceTimestamp);
                    } else {
                        Log.e(TAG, "Server timestamp is null");
                        fallbackToDeviceTime(callback);
                    }
                    docRef.delete();
                } else {
                    Log.e(TAG, "Document does not exist");
                    fallbackToDeviceTime(callback);
                }
            }).addOnFailureListener(e -> {
                isLoading.setValue(false);
                Log.e(TAG, "Error reading server time: " + e.getMessage(), e);
                fallbackToDeviceTime(callback);
            });
        }).addOnFailureListener(e -> {
            isLoading.setValue(false);
            Log.e(TAG, "Error writing server time: " + e.getMessage(), e);
            fallbackToDeviceTime(callback);
        });
    }

    public interface OnServerTimeSynced {
        void onSuccess(long serverTimestampMillis);
    }

    private long getCurrentSyncedTime() {
        if (hasSynced) {
            long localNow = System.currentTimeMillis();
            return serverReferenceTimestamp + (localNow - deviceReferenceTimestamp);
        } else {
            return System.currentTimeMillis();
        }
    }

    private void startAutoUpdate() {
        timeUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                long ts = getCurrentSyncedTime();
                updateTimeDisplay(ts);
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(timeUpdateRunnable);
        Log.d(TAG, "Auto-update started (display only, 1 fetch per check-in/check-out)");
    }

    private void stopAutoUpdate() {
        if (timeUpdateRunnable != null) {
            handler.removeCallbacks(timeUpdateRunnable);
            Log.d(TAG, "Auto-update stopped");
        }
    }

    private void fallbackToDeviceTime(OnServerTimeSynced callback) {
        hasSynced = false;
        long localTime = System.currentTimeMillis();
        updateTimeDisplay(localTime);
        Log.w(TAG, "Using device time as fallback (only display!)");
        if (callback != null) callback.onSuccess(localTime);
    }

    private void updateTimeDisplay(long timestamp) {
        try {
            Date date = new Date(timestamp);

            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", new Locale("vi", "VN"));
            timeFormat.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            String formattedTime = timeFormat.format(date);
            currentTime.postValue(formattedTime);

            SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd 'tháng' MM, yyyy", new Locale("vi", "VN"));
            dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            String formattedDate = dateFormat.format(date);
            currentDate.postValue(formattedDate);

        } catch (Exception e) {
            Log.e(TAG, "Error formatting time: " + e.getMessage(), e);
        }
    }

    public LiveData<String> getCurrentTime() {
        return currentTime;
    }

    public LiveData<String> getCurrentDate() {
        return currentDate;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public long getCurrentTimestamp() {
        return getCurrentSyncedTime();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopAutoUpdate();
        Log.d(TAG, "TimeViewModel cleared");
    }
}
