package com.example.timemaster.data.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CheckIn {
    private String id;
    private String userId;
    private String date;
    private long timeIn;
    private long timeOut;
    private String status;
    private int statusType; // Re-added
    private double confidence;
    private String imageRef;

    public CheckIn() {
        // Default constructor
    }

    // Constructor for demo data in UserStatsFragment
    public CheckIn(String date, String checkInTimeStr, String checkOutTimeStr) {
        this.date = date;
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        try {
            if (checkInTimeStr != null && !checkInTimeStr.isEmpty()) {
                this.timeIn = sdf.parse(checkInTimeStr).getTime();
            } else {
                this.timeIn = 0;
            }
            if (checkOutTimeStr != null && !checkOutTimeStr.isEmpty()) {
                this.timeOut = sdf.parse(checkOutTimeStr).getTime();
            } else {
                this.timeOut = 0;
            }
        } catch (ParseException e) {
            this.timeIn = 0;
            this.timeOut = 0;
        }
        calculateStatus();
    }

    // --- Getters & Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public long getTimeIn() { return timeIn; }
    public void setTimeIn(long timeIn) { this.timeIn = timeIn; }
    public long getTimeOut() { return timeOut; }
    public void setTimeOut(long timeOut) { this.timeOut = timeOut; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public String getImageRef() { return imageRef; }
    public void setImageRef(String imageRef) { this.imageRef = imageRef; }

    // --- Compatibility Methods ---
    public int getStatusType() {
        return statusType;
    }

    public String getCheckInTime() {
        if (timeIn == 0) return null;
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timeIn));
    }

    public String getCheckOutTime() {
        if (timeOut == 0) return null;
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timeOut));
    }

    // --- Business Logic ---
    public void calculateStatus() {
        if (timeIn == 0) {
            status = "Vắng mặt";
            statusType = 3;
            return;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            Date ruleIn = sdf.parse("08:00");
            Date ruleOut = sdf.parse("17:30");

            Date actualIn = sdf.parse(getCheckInTime());

            if (actualIn.after(ruleOut)) {
                status = "Vắng mặt";
                statusType = 3;
                return;
            }

            String checkOutTimeStr = getCheckOutTime();
            Date actualOut = (checkOutTimeStr == null) ? null : sdf.parse(checkOutTimeStr);

            boolean late = actualIn.after(ruleIn);

            if (actualOut == null) { // Not checked out yet
                status = late ? "Đi trễ" : "Đúng giờ";
                statusType = late ? 1 : 0;
                return;
            }

            boolean early = actualOut.before(ruleOut);

            if (late && early) {
                status = "Đi trễ & Về sớm";
                statusType = 4;
            } else if (late) {
                status = "Đi trễ";
                statusType = 1;
            } else if (early) {
                status = "Về sớm";
                statusType = 2;
            } else {
                status = "Đúng giờ";
                statusType = 0;
            }
        } catch (Exception e) {
            status = "Lỗi dữ liệu";
            statusType = -1;
        }
    }
}
