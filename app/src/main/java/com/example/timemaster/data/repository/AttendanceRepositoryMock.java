package com.example.timemaster.data.repository;

import com.example.timemaster.data.model.DayAttendance;
import com.example.timemaster.data.model.UserAttendance;
import com.example.timemaster.data.model.WeekAttendance;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Dữ liệu mẫu cố định (không random) cho 4 tuần gần nhất.
 * - 10 nhân viên cố định
 * - Mỗi ngày 10 dòng: đủ trang (pageSize=5) -> 2 trang / ngày
 * - Chủ Nhật (d=6) cho nhiều người vắng để demo
 * - Trạng thái được SUY từ giờ vào/ra bởi UserAttendance (Cách A)
 */
public class AttendanceRepositoryMock implements AttendanceRepository {

    private static final String[] NAMES = new String[] {
            "Nguyễn Văn A",
            "Trần Văn B",
            "Lê Thị C",
            "Phạm Thị D",
            "Hoàng Văn E",
            "Ngô Thị F",
            "Đỗ Văn G",
            "Bùi Thị H",
            "Vũ Văn I",
            "Phan Thị K"
    };

    @Override
    public List<WeekAttendance> getLast4Weeks() {
        List<WeekAttendance> weeks = new ArrayList<>();

        // Lấy thứ Hai của tuần hiện tại
        Calendar base = Calendar.getInstance();
        base.set(Calendar.HOUR_OF_DAY, 0);
        base.set(Calendar.MINUTE, 0);
        base.set(Calendar.SECOND, 0);
        base.set(Calendar.MILLISECOND, 0);
        base.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        // Tuần 0 = tuần hiện tại, 1..3 = tuần trước
        Calendar cursor = (Calendar) base.clone();
        for (int w = 0; w < 4; w++) {
            WeekAttendance week = new WeekAttendance();
            week.days = new ArrayList<>();

            Calendar dayCal = (Calendar) cursor.clone();
            for (int d = 0; d < 7; d++) {
                DayAttendance day = new DayAttendance();
                day.date = dayCal.getTime();
                day.attendances = new ArrayList<>();

                // 10 dòng / ngày (2 trang x 5)
                // tạo pattern khác nhau theo d để demo đầy đủ:
                switch (d) {
                    case 0: // Thứ 2: đủ 10 người, có trễ và về sớm
                        add(day, 0,  "08:00", "17:30"); // đúng giờ
                        add(day, 1,  "08:13", "17:00"); // trễ
                        add(day, 2,   null,    null   ); // vắng
                        add(day, 3,  "08:00", "16:40"); // về sớm
                        add(day, 4,  "08:01", "17:30"); // trễ nhẹ
                        add(day, 5,  "08:00", "17:30");
                        add(day, 6,  "08:10", "17:20"); // trễ + về sớm (ưu tiên "trễ" theo UserAttendance)
                        add(day, 7,  "08:00", "17:05"); // về sớm
                        add(day, 8,  "08:25", "17:30"); // trễ
                        add(day, 9,  "08:00", "17:30");
                        break;

                    case 1: // Thứ 3: có vài người vắng
                        add(day, 0,  "08:05", "17:30");
                        add(day, 1,  "09:01", "17:28"); // trễ
                        add(day, 2,   null,    null   ); // vắng
                        add(day, 3,  "08:00", "17:05"); // về sớm
                        add(day, 4,  "08:00", "17:30");
                        add(day, 5,  "08:02", "17:50"); // muộn 2p nhưng ra muộn -> vẫn tính "trễ" do vào > 08:00
                        add(day, 6,   null,    null   ); // vắng
                        add(day, 7,  "08:20", "17:25"); // trễ + về sớm (ưu tiên "trễ")
                        add(day, 8,  "08:00", "17:30");
                        add(day, 9,  "08:18", "17:30"); // trễ
                        break;

                    case 2: // Thứ 4: đa số đi làm
                        add(day, 0,  "08:00", "17:25"); // về sớm
                        add(day, 1,  "08:25", "17:15"); // trễ + về sớm (ưu tiên "trễ")
                        add(day, 2,  "08:00", "17:30");
                        add(day, 3,   null,    null   ); // vắng
                        add(day, 4,  "08:12", "17:30"); // trễ
                        add(day, 5,  "08:00", "17:30");
                        add(day, 6,  "08:07", "17:30"); // trễ
                        add(day, 7,  "08:00", "16:45"); // về sớm
                        add(day, 8,  "08:00", "17:30");
                        add(day, 9,  "08:15", "17:10"); // trễ + về sớm
                        break;

                    case 3: // Thứ 5: có nhiều muộn/ra sớm
                        add(day, 0,   null,    null   ); // vắng
                        add(day, 1,  "08:45", "16:59"); // trễ + về sớm
                        add(day, 2,  "08:00", "17:00"); // về sớm
                        add(day, 3,  "09:20", "17:40"); // trễ
                        add(day, 4,  "08:00", "17:30");
                        add(day, 5,  "08:30", "17:30"); // trễ
                        add(day, 6,  "08:00", "17:20"); // về sớm
                        add(day, 7,  "08:03", "17:30"); // trễ
                        add(day, 8,  "08:00", "17:30");
                        add(day, 9,  "08:10", "17:30"); // trễ
                        break;

                    case 4: // Thứ 6: nhiều người đủ giờ
                        add(day, 0,  "08:00", "17:10"); // về sớm
                        add(day, 1,  "08:02", "17:50"); // trễ
                        add(day, 2,   null,    null   ); // vắng
                        add(day, 3,  "08:20", "17:25"); // trễ + về sớm
                        add(day, 4,  "08:00", "17:30");
                        add(day, 5,  "08:00", "17:30");
                        add(day, 6,  "08:05", "17:30"); // trễ
                        add(day, 7,  "08:00", "17:30");
                        add(day, 8,  "08:00", "17:30");
                        add(day, 9,  "08:16", "17:18"); // trễ + về sớm
                        break;

                    case 5: // Thứ 7: có vài người nghỉ
                        add(day, 0,  "08:00", "16:25"); // về sớm
                        add(day, 1,   null,    null   ); // vắng
                        add(day, 2,  "08:37", "17:35"); // trễ
                        add(day, 3,  "08:40", "16:40"); // trễ + về sớm
                        add(day, 4,  "08:00", "17:30");
                        add(day, 5,  "08:00", "17:30");
                        add(day, 6,  "08:03", "17:30"); // trễ
                        add(day, 7,  "08:00", "17:30");
                        add(day, 8,   null,    null   ); // vắng
                        add(day, 9,  "08:12", "17:30"); // trễ
                        break;

                    case 6: // Chủ nhật: đa số vắng (demo)
                        add(day, 0,   null,    null   );
                        add(day, 1,   null,    null   );
                        add(day, 2,   null,    null   );
                        add(day, 3,   null,    null   );
                        add(day, 4,   null,    null   );
                        add(day, 5,  "08:10", "11:30"); // làm nửa ngày (trễ + về sớm)
                        add(day, 6,  "08:00", "12:00"); // về sớm
                        add(day, 7,   null,    null   );
                        add(day, 8,   null,    null   );
                        add(day, 9,   null,    null   );
                        break;
                }

                week.days.add(day);
                dayCal.add(Calendar.DATE, 1);
            }

            weeks.add(week);
            // Lùi 1 tuần
            cursor.add(Calendar.DATE, -7);
        }

        return weeks;
    }

    // helper: thêm một dòng theo index name và giờ vào/ra
    private void add(DayAttendance day, int nameIndex, String in, String out) {
        day.attendances.add(new UserAttendance(NAMES[nameIndex], in, out));
    }
}
