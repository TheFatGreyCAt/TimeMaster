package com.example.timemaster.data.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class UserAttendance {
    private static final Locale VI = new Locale("vi","VN");
    private static final SimpleDateFormat TF = new SimpleDateFormat("HH:mm", VI);

    private static final String START = "08:00";
    private static final String END   = "17:30";

    private final String uid;
    private final String name;
    private final String checkIn;   // "HH:mm" hoặc null
    private final String checkOut;  // "HH:mm" hoặc null
    private final int statusType;
    private final String statusText;

    public UserAttendance(String uid, String name, String checkIn, String checkOut) {
        this.uid = uid;
        this.name = name;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.statusType = evalStatus(checkIn, checkOut);
        this.statusText = StatusType.toText(this.statusType);
    }

    private int evalStatus(String in, String out) {
        // Vắng mặt nếu không chấm công cả vào và ra
        if (in == null || out == null) return StatusType.ABSENT;

        try {
            long tIn  = TF.parse(in).getTime();
            long tOut = TF.parse(out).getTime();
            long tStart = TF.parse(START).getTime();
            long tEnd   = TF.parse(END).getTime();

            boolean lateIn = tIn > tStart;
            boolean earlyOut = tOut < tEnd;

            // Ưu tiên Về sớm: nếu về sớm hơn 17h30, trạng thái là Về sớm
            if (earlyOut) {
                return StatusType.EARLY_OUT;
            }

            // Nếu không về sớm, kiểm tra xem có đi trễ không
            if (lateIn) {
                return StatusType.LATE;
            }

            // Nếu không về sớm và không đi trễ, thì là Đúng giờ
            return StatusType.PRESENT;

        } catch (ParseException e) {
            return StatusType.ABSENT; // fallback an toàn
        }
    }

    public String getUid() { return uid; }
    public String getName() { return name; }
    public String getCheckInTime() { return checkIn; }
    public String getCheckOutTime() { return checkOut; }
    public int getStatusType() { return statusType; }
    public String getStatusText() { return statusText; }
    public long getCheckInTimestamp() {
        if (checkIn == null) return 0;
        try {
            return TF.parse(checkIn).getTime();
        } catch (ParseException e) {
            return 0;
        }
    }
}
