package com.example.timemaster.data.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DayAttendance {
    public Date date;
    public List<UserAttendance> attendances = new ArrayList<>();
}
