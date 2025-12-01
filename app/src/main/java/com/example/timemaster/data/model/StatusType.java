package com.example.timemaster.data.model;

public final class  StatusType {
    public static final int NOT_CHECKED = -1;
    public static final int PRESENT = 0;
    public static final int LATE = 1;
    public static final int ABSENT = 2;
    public static final int EARLY_OUT = 3;
    public static final int LATE_AND_EARLY_OUT = 4;


    public static String toText(int t) {
        switch (t) {
            case NOT_CHECKED: return "Chưa điểm danh";
            case PRESENT: return "Đúng giờ";
            case LATE: return "Đi trễ";
            case ABSENT: return "Vắng";
            case EARLY_OUT: return "Về sớm";
            case LATE_AND_EARLY_OUT: return "Đi trễ và Về sớm";
            default: return "N/A";
        }
    }

    public static int toColor(int t) {
        switch (t) {
            case NOT_CHECKED: return android.graphics.Color.parseColor("#6B7280"); // Xám
            case PRESENT: return android.graphics.Color.parseColor("#059669"); // Xanh lá
            case LATE: return android.graphics.Color.parseColor("#D97706"); // Vàng
            case ABSENT: return android.graphics.Color.parseColor("#DC2626"); // Đỏ
            case EARLY_OUT: return android.graphics.Color.parseColor("#F59E0B"); // Cam
            case LATE_AND_EARLY_OUT: return android.graphics.Color.parseColor("#DC2626"); // Đỏ
            default: return android.graphics.Color.parseColor("#6B7280"); // Xám
        }
    }
}