package com.example.timemaster.model;

public class AttendanceHistory {
    private String time;
    private String date;
    private String status; // "Đúng giờ", "Đi trễ", "Về sớm", "Vắng mặt"

    public AttendanceHistory(String time, String date, String status) {
        this.time = time;
        this.date = date;
        this.status = status;
    }

    public String getTime() { return time; }
    public String getDate() { return date; }
    public String getStatus() { return status; }
}