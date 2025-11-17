package com.example.timemaster.model.model_admin;

public final class StatusType {
    public static final int PRESENT = 0;
    public static final int LATE = 1;
    public static final int ABSENT = 2;
    public static final int EARLY_OUT = 3;

    public static String toText(int t) {
        switch (t) {
            case PRESENT: return "Đúng giờ";
            case LATE: return "Đi trễ";
            case ABSENT: return "Vắng";
            case EARLY_OUT: return "Về sớm";
            default: return "N/A";
        }
    }
}
