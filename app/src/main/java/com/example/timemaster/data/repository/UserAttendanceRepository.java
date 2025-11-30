package com.example.timemaster.data.repository;

import androidx.annotation.NonNull;

import com.example.timemaster.data.model.CheckIn;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lấy dữ liệu chấm công theo tuần cho 1 user từ Firestore.
 *
 * Collection: attendanceRecords
 *   - uid: String
 *   - date: Timestamp (ngày làm, chuẩn hoá 00:00)
 *   - checkIn: Timestamp (có thể null)
 *   - checkOut: Timestamp (có thể null)
 */
public class UserAttendanceRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final Locale VI = new Locale("vi", "VN");
    private final SimpleDateFormat KEY_DF =
            new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
    private final SimpleDateFormat TIME_DF =
            new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SimpleDateFormat DISPLAY_DF =
            new SimpleDateFormat("EEEE, dd/MM/yyyy", VI);

    public interface OnWeekLoaded {
        void onSuccess(List<CheckIn> weekData);   // luôn 7 phần tử: Thứ 2 -> CN
        void onError(Exception e);
    }

    /**
     * Lấy tuần chứa anchorDate (Thứ 2 → Chủ nhật) cho user uid.
     * Nếu 1 ngày không có record => CheckIn sẽ "Vắng mặt" nhờ calculateStatus().
     */
    public void getWeekForUser(String uid, Date anchorDate, OnWeekLoaded callback) {
        // 1. Tìm thứ 2 của tuần chứa anchorDate
        Calendar cal = Calendar.getInstance();
        cal.setTime(anchorDate);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK); // CN = 1
        int diffToMonday = (dayOfWeek == Calendar.SUNDAY)
                ? -6
                : (Calendar.MONDAY - dayOfWeek);
        cal.add(Calendar.DAY_OF_MONTH, diffToMonday);
        Date startOfWeek = cal.getTime();

        Calendar endCal = (Calendar) cal.clone();
        endCal.add(Calendar.DAY_OF_MONTH, 7);
        Date endOfWeek = endCal.getTime();

        // 2. Query Firestore: tất cả record của user trong tuần này
        db.collection("attendanceRecords")
                .whereEqualTo("uid", uid)
                .whereGreaterThanOrEqualTo("timestamp", startOfWeek.getTime())
                .whereLessThan("timestamp", endOfWeek.getTime())
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot snapshots) {
                        List<DocumentSnapshot> docs = snapshots.getDocuments();

                        // Gom các document theo ngày + checkType
                        // Map<yyyyMMdd><"CHECK_IN"|"CHECK_OUT"> = HH:mm
                        Map<String, Map<String, String>> byDateAndType = new HashMap<>();
                        for (DocumentSnapshot doc : docs) {
                            // update: date -> timestamp
                            String dateStr = doc.getString("date");  // "2025-11-30"
                            String time = doc.getString("time");      // "09:30:00"
                            String checkType = doc.getString("checkType");  // "CHECK_IN" hoặc "CHECK_OUT"

                            if (dateStr == null || time == null) continue;

                            String timeStr = time.length() >= 5 ? time.substring(0, 5) : time;  // "09:30"
                            String dayKey = dateStr.replace("-", "");  // "20251130"

                            if (!byDateAndType.containsKey(dayKey)) {
                                byDateAndType.put(dayKey, new HashMap<>());
                            }
                            if (checkType != null) {
                                byDateAndType.get(dayKey).put(checkType, timeStr);
                            }
                        }

                        // 3. Tạo đủ 7 CheckIn cho Thứ 2 → CN
                        List<CheckIn> result = new ArrayList<>();
                        for (int i = 0; i < 7; i++) {
                            Calendar d = (Calendar) cal.clone();
                            d.add(Calendar.DAY_OF_MONTH, i);
                            Date date = d.getTime();

                            String key = KEY_DF.format(date);
                            String displayDate = DISPLAY_DF.format(date); // "Thứ hai, 01/12/2025"

                            Map<String, String> typeData = byDateAndType.get(key);

                            String in = null;
                            String out = null;

                            if (typeData != null) {
                                in = typeData.get("CHECK_IN");
                                out = typeData.get("CHECK_OUT");
                            }

                            // Dùng đúng constructor CheckIn(date, checkInTime, checkOutTime)
                            CheckIn ci = new CheckIn(displayDate, in, out);
                            // calculateStatus() tự chạy trong constructor

                            result.add(ci);
                        }

                        callback.onSuccess(result);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callback.onError(e);
                    }
                });
    }

    /** helper: dùng cho tuần hiện tại */
    public void getCurrentWeekForUser(String uid, OnWeekLoaded callback) {
        getWeekForUser(uid, new Date(), callback);
    }
}
