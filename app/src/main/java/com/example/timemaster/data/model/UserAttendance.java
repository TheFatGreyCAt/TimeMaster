package com.example.timemaster.data.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class UserAttendance {
    private static final Locale VI = new Locale("vi","VN");
    private static final SimpleDateFormat TF = new SimpleDateFormat("HH:mm", VI);

    private static final String START = "08:00";
    private static final String END   = "17:30";

    private final String name;
    private final String checkIn;   // "HH:mm" hoặc null
    private final String checkOut;  // "HH:mm" hoặc null
    private final int statusType;
    private final String statusText;

    public UserAttendance(String name, String checkIn, String checkOut) {
        this.name = name;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.statusType = evalStatus(checkIn, checkOut);
        this.statusText = StatusType.toText(this.statusType);
    }

    private int evalStatus(String in, String out) {
        // ABSENT nếu thiếu 1 trong 2 mốc
        if (in == null || out == null) return StatusType.ABSENT;

        try {
            long tIn  = TF.parse(in).getTime();
            long tOut = TF.parse(out).getTime();
            long tStart = TF.parse(START).getTime();
            long tEnd   = TF.parse(END).getTime();

            boolean late = tIn > tStart;
            boolean early = tOut < tEnd;

            if (late) return StatusType.LATE;         // Ưu tiên LATE nếu cả hai
            if (early) return StatusType.EARLY_OUT;
            return StatusType.PRESENT;

        } catch (ParseException e) {
            return StatusType.ABSENT; // fallback an toàn
        }
    }

    public String getName() { return name; }
    public String getCheckInTime() { return checkIn; }
    public String getCheckOutTime() { return checkOut; }
    public int getStatusType() { return statusType; }
    public String getStatusText() { return statusText; }
}
