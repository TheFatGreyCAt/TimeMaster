package com.example.timemaster.ui.checkin;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.timemaster.data.repository.TimeRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TimeViewModel extends ViewModel {

    private static final String TAG = "TimeViewModel";

    private final TimeRepository timeRepository;

    private final MutableLiveData<String> currentTime = new MutableLiveData<>();
    private final MutableLiveData<String> currentDate = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private long serverReferenceTimestamp = 0L; // milliseconds
    private long deviceReferenceTimestamp = 0L; // milliseconds
    private boolean hasSynced = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable timeUpdateRunnable;

    public TimeViewModel(@NonNull TimeRepository timeRepository) {
        this.timeRepository = timeRepository;
        Log.d(TAG, "TimeViewModel created with repository");
        startAutoUpdate();
    }

    public void syncWithServerTime(OnServerTimeSynced callback) {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        timeRepository.syncWithServerTime(new TimeRepository.OnServerTimeSynced() {
            @Override
            public void onSuccess(long serverTimestampMillis) {
                isLoading.postValue(false);
                serverReferenceTimestamp = serverTimestampMillis;
                deviceReferenceTimestamp = System.currentTimeMillis();
                hasSynced = true;
                Log.d(TAG, "Server time synced: " + new Date(serverReferenceTimestamp));
                if (callback != null) {
                    callback.onSuccess(serverReferenceTimestamp);
                }
            }

            @Override
            public void onFailure(Exception e) {
                isLoading.postValue(false);
                errorMessage.postValue("Lỗi đồng bộ thời gian: " + e.getMessage());
                Log.e(TAG, "Error syncing server time: " + e.getMessage(), e);
                fallbackToDeviceTime(callback);
            }
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
    }

    private void stopAutoUpdate() {
        if (timeUpdateRunnable != null) {
            handler.removeCallbacks(timeUpdateRunnable);
        }
    }

    private void fallbackToDeviceTime(OnServerTimeSynced callback) {
        hasSynced = false;
        long localTime = System.currentTimeMillis();
        updateTimeDisplay(localTime);
        Log.w(TAG, "Using device time as fallback");
        if (callback != null) {
            callback.onSuccess(localTime);
        }
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

    public LiveData<String> getCurrentTime() { return currentTime; }
    public LiveData<String> getCurrentDate() { return currentDate; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopAutoUpdate();
        Log.d(TAG, "TimeViewModel cleared");
    }
}
