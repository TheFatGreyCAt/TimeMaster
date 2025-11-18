package com.example.timemaster.data.repository;

import com.example.timemaster.data.model.DayAttendance;
import com.example.timemaster.data.model.StatusType;
import com.example.timemaster.data.model.UserAttendance;
import com.example.timemaster.data.model.WeekAttendance;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

public class AttendanceRepositoryFirestore implements AttendanceRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override public List<WeekAttendance> getLast4Weeks() {
        // GỢI Ý cấu trúc Firestore (tuỳ bạn chốt):
        // attendance/{yyyyMMdd}/users/{uid} => { name, checkIn, checkOut, statusType }
        // 1) Tính ngày thứ Hai các tuần (hiện tại + 3 trước)
        // 2) Với mỗi tuần, fetch 7 ngày song song, ghép thành WeekAttendance
        // Vì Fragment đang gọi đồng bộ, ở đây minh hoạ cách “đồng bộ hóa” đơn giản (CountDownLatch).
        List<WeekAttendance> result = new ArrayList<>();

        Calendar monday = Calendar.getInstance();
        monday.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        for (int w = 0; w < 4; w++) {
            WeekAttendance wk = new WeekAttendance();
            wk.days = new ArrayList<>();
            Calendar day = (Calendar) monday.clone();

            for (int d = 0; d < 7; d++) {
                String key = formatKey(day.getTime()); // yyyyMMdd
                DayAttendance da = new DayAttendance();
                da.date = day.getTime();
                da.attendances = new ArrayList<>();

                CountDownLatch latch = new CountDownLatch(1);
                db.collection("attendance").document(key).collection("users")
                        .get()
                        .addOnSuccessListener(snap -> {
                            for (DocumentSnapshot doc : snap) {
                                String name = doc.getString("name");
                                String ci = doc.getString("checkIn");
                                String co = doc.getString("checkOut");
                                // Lớp UserAttendance sẽ tự tính toán trạng thái
                                da.attendances.add(new UserAttendance(name, ci, co));
                            }
                            latch.countDown();
                        })
                        .addOnFailureListener(e -> latch.countDown());

                try { latch.await(); } catch (InterruptedException ignored) {}
                wk.days.add(da);

                day.add(Calendar.DATE, 1);
            }

            result.add(wk);
            monday.add(Calendar.DATE, -7);
        }

        return result;
    }

    private String formatKey(Date date) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd", Locale.US);
        return sdf.format(date);
    }
}
