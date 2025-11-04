package com.example.timemaster.model;

import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.timemaster.network.RetrofitClient;

import java.text.SimpleDateFormat;
import java.util.Locale;
import android.os.Handler;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TimeViewModel extends ViewModel {
    private static final String TAG = "TimeViewModel";

    // UI Livedata
    private final MutableLiveData<String> currentTime = new MutableLiveData<>("--:--:--");
    private final MutableLiveData<String> currentDate = new MutableLiveData<>("--:--:--");
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);

    // Timestamp from server
    private long serverTimestamp = 0L;
    private long localReferenceTime = 0L;

    // Handler realtime clock
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable clockRunnable;

    public MutableLiveData<String> getCurrentTime() {
        return currentTime;
    }

    public MutableLiveData<String> getCurrentDate() {
        return currentDate;
    }

    public MutableLiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public MutableLiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void fetchWorldTime() {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        Call<WorldTimeResponse> call = RetrofitClient.getWorldTimeService().getWorldTime();
        call.enqueue(new Callback<WorldTimeResponse>() {
            @Override
            public void onResponse(Call<WorldTimeResponse> call, Response<WorldTimeResponse> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    WorldTimeResponse data = response.body();
                    serverTimestamp = data.getUnixtime() * 1000;
                    localReferenceTime = System.currentTimeMillis();

                    Log.d(TAG, "WorldTime timestamp: " + serverTimestamp);
                    startRealtimeClock();
                }
                else {
                    String error = "Lỗi Server: " + response.code();
                    errorMessage.setValue(error);
                    Log.e(TAG, error);
                    useFallbackTime();
                }
            }

            @Override
            public void onFailure(Call<WorldTimeResponse> call, Throwable t) {
                isLoading.setValue(false);

                String error = "Lỗi kết nối: " + t.getMessage();
                errorMessage.setValue(error);
                Log.e(TAG, error, t);

                useFallbackTime();
            }
        });
    }

    private void startRealtimeClock() {
        stopRealtimeClock();

        clockRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsedTime = System.currentTimeMillis() - localReferenceTime;
                long currentTimestamp = serverTimestamp + elapsedTime;

                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd:MM:yyyy", Locale.getDefault());

                currentTime.setValue(timeFormat.format(currentTimestamp));
                currentDate.setValue(dateFormat.format(currentTimestamp));
                handler.postDelayed(this, 1000);
            }
        };
    }

    private void stopRealtimeClock() {
        if (clockRunnable != null) {
            handler.removeCallbacks(clockRunnable);
        }
    }

    private void useFallbackTime() {
        serverTimestamp = System.currentTimeMillis();
        localReferenceTime = serverTimestamp;
        startRealtimeClock();
    }

    public long getCurrentTimestamp() {
        long elapsedTime = System.currentTimeMillis() - localReferenceTime;
        return serverTimestamp + elapsedTime;
    }

    public void syncTime() {
        fetchWorldTime();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopRealtimeClock();
    }

}

