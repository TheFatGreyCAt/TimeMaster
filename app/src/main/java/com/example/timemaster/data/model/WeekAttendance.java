package com.example.timemaster.data.model;

import java.util.ArrayList;
import java.util.List;

public class WeekAttendance {

    // vẫn giữ field public cũ để không vỡ code cũ
    public List<DayAttendance> days = new ArrayList<>(); // 7 ngày, T2→CN

    public WeekAttendance() {
    }

    public WeekAttendance(List<DayAttendance> days) {
        this.days = days;
    }

    // <<< QUAN TRỌNG: getter dùng trong ViewModel >>>
    public List<DayAttendance> getDays() {
        return days;
    }

    public void setDays(List<DayAttendance> days) {
        this.days = days;
    }
}
