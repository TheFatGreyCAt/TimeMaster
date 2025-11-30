package com.example.timemaster.data.repository;

import com.example.timemaster.data.model.DayAttendance;
import com.example.timemaster.data.model.UserAttendance;
import com.example.timemaster.data.model.WeekAttendance;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.*;

public class AttendanceRepositoryFirestore implements AttendanceRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    public void getLast4Weeks(Callback callback) {
        // Chuẩn hoá về 00:00 hôm nay
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        // Tìm Thứ 2 tuần hiện tại
        int dayOfWeek = today.get(Calendar.DAY_OF_WEEK); // Chủ nhật = 1
        int diffToMonday = (dayOfWeek == Calendar.SUNDAY) ? -6 : (Calendar.MONDAY - dayOfWeek);
        today.add(Calendar.DAY_OF_MONTH, diffToMonday);
        Date startCurrentWeek = today.getTime();

        // startDate = thứ 2 của tuần -3 (tức là 4 tuần gần nhất: tuần -3, -2, -1, 0)
        Calendar start4Weeks = (Calendar) today.clone();
        start4Weeks.add(Calendar.DAY_OF_MONTH, -21);
        Date startDate = start4Weeks.getTime();

        db.collection("attendanceRecords")
                .whereGreaterThanOrEqualTo("date", startDate)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<DocumentSnapshot> docs = querySnapshot.getDocuments();
                    List<WeekAttendance> weeks =
                            buildWeeksFromSnapshots(docs, startCurrentWeek);
                    callback.onSuccess(weeks);
                })
                .addOnFailureListener(callback::onError);
    }

    private List<WeekAttendance> buildWeeksFromSnapshots(List<DocumentSnapshot> docs,
                                                         Date startCurrentWeek) {

        // Gom các doc theo ngày yyyyMMdd
        SimpleDateFormat keyFormat =
                new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        Map<String, List<DocumentSnapshot>> byDate = new HashMap<>();

        for (DocumentSnapshot doc : docs) {
            Timestamp ts = doc.getTimestamp("date");
            if (ts == null) continue;
            String key = keyFormat.format(ts.toDate());
            if (!byDate.containsKey(key)) byDate.put(key, new ArrayList<>());
            byDate.get(key).add(doc);
        }

        List<WeekAttendance> result = new ArrayList<>();

        // weekStart = thứ 2 của tuần -3
        Calendar weekStart = Calendar.getInstance();
        weekStart.setTime(startCurrentWeek);
        weekStart.add(Calendar.DAY_OF_MONTH, -21);

        for (int w = 0; w < 4; w++) {
            List<DayAttendance> days = new ArrayList<>();
            Calendar dayCal = (Calendar) weekStart.clone();

            for (int d = 0; d < 7; d++) {
                Date dayDate = dayCal.getTime();
                String key = keyFormat.format(dayDate);
                List<DocumentSnapshot> dayDocs =
                        byDate.getOrDefault(key, Collections.emptyList());

                List<UserAttendance> uaList = new ArrayList<>();
                for (DocumentSnapshot doc : dayDocs) {
                    String name = doc.getString("fullName");
                    Timestamp ciTs = doc.getTimestamp("checkIn");
                    Timestamp coTs = doc.getTimestamp("checkOut");

                    String checkIn = null;
                    String checkOut = null;
                    if (ciTs != null) {
                        checkIn = new SimpleDateFormat("HH:mm", Locale.getDefault())
                                .format(ciTs.toDate());
                    }
                    if (coTs != null) {
                        checkOut = new SimpleDateFormat("HH:mm", Locale.getDefault())
                                .format(coTs.toDate());
                    }

                    uaList.add(new UserAttendance(
                            name != null ? name : "",
                            checkIn,
                            checkOut
                    ));
                }

                days.add(new DayAttendance(dayDate, uaList));
                dayCal.add(Calendar.DAY_OF_MONTH, 1);
            }

            result.add(new WeekAttendance(days));
            weekStart.add(Calendar.DAY_OF_MONTH, 7);
        }

        return result;
    }
}
