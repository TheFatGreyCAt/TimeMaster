package com.example.timemaster.data.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UserAttendance {
    private static final Locale VI = new Locale("vi","VN");
    private static final SimpleDateFormat TF = new SimpleDateFormat("HH:mm", VI);

    private static final String START_TIME = "08:00";
    private static final String END_TIME = "17:00";

    private final String uid;
    private final String name;
    private String checkIn;
    private String checkOut;
    private int statusType;
    private String statusText;
    private long totalWorkingMinutes;

    // Constructor nhận danh sách records từ Firebase
    public UserAttendance(String uid, String name, List<AttendanceRecord> records) {
        this.uid = uid;
        this.name = name;
        processRecords(records);
    }

    public UserAttendance(String uid, String name, String checkIn, String checkOut) {
        this.uid = uid;
        this.name = name;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.statusType = evalStatus(checkIn, checkOut);
        this.statusText = StatusType.toText(this.statusType);

        // Tính working time từ string time
        this.totalWorkingMinutes = calculateWorkingTimeFromStrings(checkIn, checkOut);
    }

    // Xử lý danh sách records để tìm check-in/check-out mới nhất
    private void processRecords(List<AttendanceRecord> records) {
        if (records == null || records.isEmpty()) {
            this.checkIn = null;
            this.checkOut = null;
            this.statusType = StatusType.NOT_CHECKED;
            this.statusText = StatusType.toText(this.statusType);
            this.totalWorkingMinutes = 0;
            return;
        }

        AttendanceRecord lastCheckIn = null;
        AttendanceRecord lastCheckOut = null;

        // Duyệt qua tất cả records để tìm check-in và check-out mới nhất
        for (AttendanceRecord record : records) {
            if ("CHECK_IN".equals(record.checkType)) {
                if (lastCheckIn == null || record.timestamp > lastCheckIn.timestamp) {
                    lastCheckIn = record;
                }
            } else if ("CHECK_OUT".equals(record.checkType)) {
                if (lastCheckOut == null || record.timestamp > lastCheckOut.timestamp) {
                    lastCheckOut = record;
                }
            }
        }

        // Chuyển đổi timestamp sang HH:mm
        this.checkIn = lastCheckIn != null ? formatTime(lastCheckIn.timestamp) : null;
        this.checkOut = lastCheckOut != null ? formatTime(lastCheckOut.timestamp) : null;

        // Tính toán trạng thái và thời gian làm việc
        this.statusType = evalStatus(this.checkIn, this.checkOut);
        this.statusText = StatusType.toText(this.statusType);
        this.totalWorkingMinutes = calculateWorkingTime(lastCheckIn, lastCheckOut);
    }

    private String formatTime(long timestamp) {
        return TF.format(new Date(timestamp));
    }

    private int evalStatus(String in, String out) {
        try {
            long tStart = TF.parse(START_TIME).getTime();
            long tEnd = TF.parse(END_TIME).getTime();

            if (in == null) {
                return StatusType.NOT_CHECKED;
            }

            long tIn = TF.parse(in).getTime();

            // Check-in sau 17h -> Vắng
            if (tIn > tEnd) {
                return StatusType.ABSENT;
            }

            // Chưa có check-out
            if (out == null) {
                if (tIn <= tStart) {
                    return StatusType.PRESENT;
                } else if (tIn <= tEnd) {
                    return StatusType.LATE;
                }
            } else {
                // Có cả check-in và check-out
                long tOut = TF.parse(out).getTime();

                boolean onTime = tIn <= tStart;
                boolean late = tIn > tStart && tIn <= tEnd;
                boolean earlyOut = tOut < tEnd;

                if (late && earlyOut) {
                    return StatusType.LATE_AND_EARLY_OUT;
                } else if (late) {
                    return StatusType.LATE;
                } else if (onTime && earlyOut) {
                    return StatusType.EARLY_OUT;
                } else {
                    return StatusType.PRESENT;
                }
            }

            return StatusType.NOT_CHECKED;

        } catch (ParseException e) {
            return StatusType.ABSENT;
        }
    }

    private long calculateWorkingTime(AttendanceRecord checkIn, AttendanceRecord checkOut) {
        if (checkIn == null || checkOut == null) {
            return 0;
        }
        return (checkOut.timestamp - checkIn.timestamp) / (1000 * 60); // Phút
    }

    private long calculateWorkingTimeFromStrings(String checkIn, String checkOut) {
        if (checkIn == null || checkOut == null || "--:--".equals(checkIn) || "--:--".equals(checkOut)) {
            return 0;
        }
        try {
            long tIn = TF.parse(checkIn).getTime();
            long tOut = TF.parse(checkOut).getTime();
            return (tOut - tIn) / (1000 * 60); // Phút
        } catch (ParseException e) {
            return 0;
        }
    }

    public String getFormattedWorkingTime() {
        long hours = totalWorkingMinutes / 60;
        long minutes = totalWorkingMinutes % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", hours, minutes);
    }

    // Getters
    public String getUid() { return uid; }
    public String getName() { return name; }
    public String getCheckInTime() { return checkIn != null ? checkIn : "--:--"; }
    public String getCheckOutTime() { return checkOut != null ? checkOut : "--:--"; }
    public int getStatusType() { return statusType; }
    public String getStatusText() { return statusText; }
    public long getTotalWorkingMinutes() { return totalWorkingMinutes; }

    // Method cho sorting trong Admin dashboard
    public long getCheckInTimestamp() {
        if (checkIn == null || "--:--".equals(checkIn)) {
            return 0;
        }
        try {
            return TF.parse(checkIn).getTime();
        } catch (ParseException e) {
            return 0;
        }
    }

    // Inner class để map dữ liệu Firebase
    public static class AttendanceRecord {
        public String checkType;
        public long timestamp;
        public String date;
        public String uid;

        public AttendanceRecord() {}

        public AttendanceRecord(String checkType, long timestamp, String date, String uid) {
            this.checkType = checkType;
            this.timestamp = timestamp;
            this.date = date;
            this.uid = uid;
        }
    }
}
