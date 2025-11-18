package com.example.timemaster.data.repository;

import com.example.timemaster.data.model.WeekAttendance;

import java.util.List;

public interface AttendanceRepository {
    /** Lấy 4 tuần: tuần hiện tại + 3 tuần trước, mỗi tuần 7 ngày. */
    List<WeekAttendance> getLast4Weeks();
}
