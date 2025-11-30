package com.example.timemaster.data.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DayAttendance {

    public Date date;
    public List<UserAttendance> attendances = new ArrayList<>();

    public DayAttendance() {
    }

    public DayAttendance(Date date, List<UserAttendance> attendances) {
        this.date = date;
        this.attendances = attendances;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    // <<< getter dùng trong ViewModel >>>
    public List<UserAttendance> getAttendances() {
        return attendances;
    }

    public void setAttendances(List<UserAttendance> attendances) {
        this.attendances = attendances;
    }
}
