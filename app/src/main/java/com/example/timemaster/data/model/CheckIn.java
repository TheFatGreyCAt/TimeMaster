package com.example.timemaster.data.model;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CheckIn {
    private String date;           // Ngày check-in (ví dụ: "Thứ 2, 27/10/2025")
    private String checkInTime;    // Giờ vào, ví dụ "08:05"
    private String checkOutTime;   // Giờ ra, ví dụ "17:25"
    private String status;         // Trạng thái diễn giải ("Đúng giờ", "Đi trễ", "Về sớm", "Vắng mặt")
    private int statusType;        // 0: Đúng giờ, 1: Đi trễ, 2: Về sớm, 3: Vắng mặt

    public CheckIn() {}

    public CheckIn(String date, String checkInTime, String checkOutTime) {
        this.date = date;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        // Xác định trạng thái và mã trạng thái khi tạo mới đối tượng
        calculateStatus();
    }

    // Hàm xác định trạng thái và mã trạng thái dựa trên giờ quy định 8:00 và 17:30
    public void calculateStatus() {
        if (checkInTime == null || checkOutTime == null ||
                checkInTime.isEmpty() || checkOutTime.isEmpty()) {
            status = "Vắng mặt";
            statusType = 3;
            return;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            Date ruleIn = sdf.parse("08:00");
            Date ruleOut = sdf.parse("17:30");
            Date actualIn = sdf.parse(checkInTime);
            Date actualOut = sdf.parse(checkOutTime);

            boolean late = actualIn.after(ruleIn);
            boolean early = actualOut.before(ruleOut);

            if (!late && !early) {
                status = "Đúng giờ";
                statusType = 0;
            } else if (late && !early) {
                status = "Đi trễ";
                statusType = 1;
            } else if (!late && early) {
                status = "Về sớm";
                statusType = 2;
            } else if (late && early) {
                status = "Đi trễ & Về sớm";
                statusType = 4;
            }
        } catch (Exception e) {
            status = "Lỗi dữ liệu";
            statusType = -1;
        }
    }

    public String getDate() {
        return date;
    }
    public void setDate(String date) {
        this.date = date;
    }

    public String getCheckInTime() {
        return checkInTime;
    }
    public void setCheckInTime(String checkInTime) {
        this.checkInTime = checkInTime;
        calculateStatus();
    }

    public String getCheckOutTime() {
        return checkOutTime;
    }
    public void setCheckOutTime(String checkOutTime) {
        this.checkOutTime = checkOutTime;
        calculateStatus();
    }

    public String getStatus() {
        return status;
    }
    public int getStatusType() {
        return statusType;
    }
}
