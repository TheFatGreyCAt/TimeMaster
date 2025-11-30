package com.example.timemaster.data.repository;

import com.example.timemaster.data.model.DayAttendance;
import com.example.timemaster.data.model.UserAttendance;
import com.example.timemaster.data.model.WeekAttendance;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
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
                .whereGreaterThanOrEqualTo("timestamp", startDate.getTime())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<DocumentSnapshot> docs = querySnapshot.getDocuments();
                    // Lấy danh sách UID không trùng lặp
                    Set<String> uids = new HashSet<>();
                    for (DocumentSnapshot doc : docs) {
                        String uid = doc.getString("uid");
                        if (uid != null) {
                            uids.add(uid);
                        }
                    }

                    if (uids.isEmpty()) {
                        List<WeekAttendance> weeks = buildWeeksFromSnapshots(docs, startCurrentWeek, new HashMap<>());
                        callback.onSuccess(weeks);
                        return;
                    }

                    // Lấy thông tin user từ collection "users"
                    db.collection("users").whereIn(FieldPath.documentId(), new ArrayList<>(uids)).get()
                            .addOnSuccessListener(userDocs -> {
                                Map<String, String> userNames = new HashMap<>();
                                for (DocumentSnapshot userDoc : userDocs) {
                                    String uid = userDoc.getId();
                                    String fullName = userDoc.getString("fullName");
                                    if (uid != null && fullName != null) {
                                        userNames.put(uid, fullName);
                                    }
                                }
                                List<WeekAttendance> weeks = buildWeeksFromSnapshots(docs, startCurrentWeek, userNames);
                                callback.onSuccess(weeks);
                            })
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    private List<WeekAttendance> buildWeeksFromSnapshots(List<DocumentSnapshot> docs,
                                                         Date startCurrentWeek, Map<String, String> userNames) {

        // Gom các doc theo ngày yyyyMMdd
        SimpleDateFormat keyFormat =
                new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        Map<String, List<DocumentSnapshot>> byDate = new HashMap<>();

        for (DocumentSnapshot doc : docs) {
            // Lấy date từ field "date" (chuỗi yyyy-MM-dd)
            String dateStr = doc.getString("date");
            if (dateStr == null) continue;

            String key = dateStr.replace("-", "");  // "2025-11-30" -> "20251130"
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

                // Gom các document cùng user trong ngày
                // Map<uid, {checkIn, checkOut, name}>
                Map<String, Map<String, String>> userDataMap = new HashMap<>();

                for (DocumentSnapshot doc : dayDocs) {
                    String uid = doc.getString("uid");
                    String time = doc.getString("time");  // "09:30:00"
                    String checkType = doc.getString("checkType");  // "CHECK_IN" hoặc "CHECK_OUT"

                    if (time == null || uid == null) continue;

                    // Convert "09:30:00" -> "09:30"
                    String timeStr = time.length() >= 5 ? time.substring(0, 5) : time;

                    if (!userDataMap.containsKey(uid)) {
                        Map<String, String> data = new HashMap<>();
                        String name = userNames.getOrDefault(uid, "Tên không xác định");
                        data.put("name", name);
                        data.put("checkIn", null);
                        data.put("checkOut", null);
                        userDataMap.put(uid, data);
                    }

                    // Gán check-in hoặc check-out dựa trên checkType
                    Map<String, String> userData = userDataMap.get(uid);
                    if ("CHECK_IN".equals(checkType)) {
                        userData.put("checkIn", timeStr);
                    } else if ("CHECK_OUT".equals(checkType)) {
                        userData.put("checkOut", timeStr);
                    }
                }

                // Tạo UserAttendance từ data đã gom
                List<UserAttendance> uaList = new ArrayList<>();
                for (Map.Entry<String, Map<String, String>> entry : userDataMap.entrySet()) {
                    String uid = entry.getKey();
                    Map<String, String> userData = entry.getValue();
                    String name = userData.get("name");
                    String checkIn = userData.get("checkIn");
                    String checkOut = userData.get("checkOut");

                    uaList.add(new UserAttendance(
                            uid,
                            name,
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
