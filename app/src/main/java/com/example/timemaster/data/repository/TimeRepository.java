package com.example.timemaster.data.repository;

public interface TimeRepository {
    void syncWithServerTime(OnServerTimeSynced callback);

    interface OnServerTimeSynced {
        void onSuccess(long serverTimestampMillis);
        void onFailure(Exception e);
    }
}
