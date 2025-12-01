package com.example.timemaster.ui.dashboard.user;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.timemaster.R;
import com.example.timemaster.data.model.StatusType;
import com.example.timemaster.data.model.UserAttendance;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UserStatusFragment extends Fragment {

    // UI Components
    private TextView tvUsername, tvDate, tvGreeting;
    private TextView tvStatusLabel, tvStatus, tvCheckInTime, tvCheckOutTime, tvTotalHours;
    private ImageView ivCheckmark;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ListenerRegistration attendanceListener;

    // Cấu hình giờ làm việc (Bạn có thể đưa cái này vào Setting sau này)
    private static final int END_WORK_HOUR = 17; // 17h chiều là mốc xác định Vắng

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_user_status, container, false);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        initViews(view);
        setupHeaderData();

        // Fetch attendance data cho ngày hôm nay
        if (currentUser != null) {
            String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            fetchAttendanceData(currentUser.getUid(), todayDate);
        }

        return view;
    }

    private void initViews(View view) {
        tvUsername = view.findViewById(R.id.tvUsername);
        tvDate = view.findViewById(R.id.tvDate);
        tvGreeting = view.findViewById(R.id.tvGreeting);

        tvStatusLabel = view.findViewById(R.id.tvStatusLabel);
        tvStatus = view.findViewById(R.id.tvStatus);
        tvCheckInTime = view.findViewById(R.id.tvCheckInTime);
        tvCheckOutTime = view.findViewById(R.id.tvCheckOutTime);
        tvTotalHours = view.findViewById(R.id.tvTotalHours);

        ivCheckmark = view.findViewById(R.id.ivCheckmark);
        progressBar = view.findViewById(R.id.progressBar);
    }

    @SuppressLint("SetTextI18n")
    private void setupHeaderData() {
        // 1. Hiển thị ngày tháng
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));
        tvDate.setText(sdf.format(new Date()));

        // 2. Hiển thị tên & Lời chào
        if (currentUser != null) {
            // Lời chào theo giờ
            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            if (hour < 12) tvGreeting.setText("Chào buổi sáng,");
            else if (hour < 18) tvGreeting.setText("Chào buổi chiều,");
            else tvGreeting.setText("Chào buổi tối,");

            // Lấy fullName từ Firestore collection "users"
            db.collection("users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String fullName = documentSnapshot.getString("fullName");
                            if (fullName != null && !fullName.isEmpty()) {
                                tvUsername.setText(fullName + "!");
                            } else {
                                // Fallback to displayName nếu không có fullName
                                String name = currentUser.getDisplayName();
                                tvUsername.setText((name != null && !name.isEmpty()) ? name + "!" : "Bạn!");
                            }
                        } else {
                            tvUsername.setText("Bạn!");
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Nếu lỗi, sử dụng displayName
                        String name = currentUser.getDisplayName();
                        tvUsername.setText((name != null && !name.isEmpty()) ? name + "!" : "Bạn!");
                    });
        }
    }

    // Fetch dữ liệu từ Firebase
    private void fetchAttendanceData(String userId, String date) {
        // Lấy fullName từ users collection trước
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    String fullName = userDoc.exists() ? userDoc.getString("fullName") : "User";
                    if (fullName == null || fullName.isEmpty()) {
                        fullName = "User";
                    }

                    // Sau đó fetch attendance records
                    fetchAttendanceRecords(userId, date, fullName);
                })
                .addOnFailureListener(e -> {
                    // Nếu lỗi, vẫn fetch attendance với tên mặc định
                    fetchAttendanceRecords(userId, date, "User");
                });
    }

    private void fetchAttendanceRecords(String userId, String date, String fullName) {
        // Assign listener để có thể remove trong onDestroyView
        attendanceListener = db.collection("attendanceRecords")
                .whereEqualTo("uid", userId)
                .whereEqualTo("date", date)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        // Kiểm tra context trước khi hiển thị Toast
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Lỗi khi tải dữ liệu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        updateUINoData();
                        return;
                    }

                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        updateUINoData();
                        return;
                    }

                    List<UserAttendance.AttendanceRecord> records = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String checkType = doc.getString("checkType");
                        Long timestamp = doc.getLong("timestamp");
                        String dateStr = doc.getString("date");
                        String uid = doc.getString("uid");

                        if (timestamp != null && checkType != null) {
                            records.add(new UserAttendance.AttendanceRecord(
                                    checkType, timestamp, dateStr, uid
                            ));
                        }
                    }

                    UserAttendance attendance = new UserAttendance(userId, fullName, records);
                    updateUI(attendance);
                });
    }

    @SuppressLint("SetTextI18n")
    private void updateUI(UserAttendance attendance) {
        // Hiển thị thời gian check-in và check-out
        tvCheckInTime.setText(attendance.getCheckInTime());
        tvCheckOutTime.setText(attendance.getCheckOutTime());

        // Hiển thị tổng thời gian làm việc
        tvTotalHours.setText(attendance.getFormattedWorkingTime());

        // Cập nhật progress bar (giả sử 8 giờ = 100%)
        long totalMinutes = attendance.getTotalWorkingMinutes();
        int progress = Math.min(100, (int) ((totalMinutes * 100) / (8 * 60)));
        progressBar.setProgress(progress);

        // Cập nhật trạng thái
        updateStatusIndicator(attendance.getStatusType());
    }

    @SuppressLint("SetTextI18n")
    private void updateUINoData() {
        // Reset text giờ
        tvCheckInTime.setText("--:--");
        tvCheckOutTime.setText("--:--");
        tvTotalHours.setText("00:00");
        progressBar.setProgress(0);

        // Kiểm tra giờ hiện tại để quyết định VẮNG hay CHƯA ĐIỂM DANH
        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);

        if (currentHour >= END_WORK_HOUR) {
            // Đã hết ngày làm việc mà chưa có dữ liệu -> VẮNG
            updateStatusIndicator(StatusType.ABSENT);
        } else {
            // Chưa hết ngày -> CHƯA ĐIỂM DANH
            updateStatusIndicator(StatusType.NOT_CHECKED);
        }
    }

    private void updateStatusIndicator(int statusType) {
        String statusText = StatusType.toText(statusType);
        int colorRes = StatusType.toColor(statusType);

        tvStatus.setText(statusText);
        tvStatus.setTextColor(colorRes);

        // Đổi màu và icon
        ivCheckmark.setColorFilter(colorRes);

        // Chọn icon và background dựa trên status
        int iconRes;
        int bgDrawableRes;

        switch (statusType) {
            case StatusType.PRESENT:
                iconRes = R.drawable.ic_check;
                bgDrawableRes = R.drawable.circle_green_light_bg;
                break;

            case StatusType.LATE:
            case StatusType.EARLY_OUT:
            case StatusType.LATE_AND_EARLY_OUT:
                iconRes = R.drawable.ic_check;
                bgDrawableRes = R.drawable.circle_yellow_light_bg;
                break;

            case StatusType.ABSENT:
                iconRes = R.drawable.ic_close;
                bgDrawableRes = R.drawable.circle_red_light_bg;
                break;

            case StatusType.NOT_CHECKED:
            default:
                iconRes = R.drawable.ic_time;
                bgDrawableRes = R.drawable.circle_gray_light_bg;
                break;
        }

        ivCheckmark.setImageResource(iconRes);
        ivCheckmark.setBackgroundResource(bgDrawableRes);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Hủy listener để tránh memory leak
        if (attendanceListener != null) {
            attendanceListener.remove();
            attendanceListener = null;
        }
    }
}