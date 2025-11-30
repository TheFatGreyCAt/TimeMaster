package com.example.timemaster.data.repository;

import com.example.timemaster.data.model.WeekAttendance;

import java.util.List;

public interface AttendanceRepository {

    interface Callback {
        void onSuccess(List<WeekAttendance> weeks);
        void onError(Exception e);
    }

    void getLast4Weeks(Callback callback);
}
