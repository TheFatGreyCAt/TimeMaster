package com.example.timemaster.ui.dashboard.user;

import android.annotation.SuppressLint;
import android.graphics.Color;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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
    private static final int START_HOUR = 8; // 8 giờ sáng là mốc đúng giờ
    private static final int START_MINUTE = 0;
    private static final int END_WORK_HOUR = 17; // 17h chiều là mốc xác định Vắng

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_user_status, container, false);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        initViews(view);
        setupHeaderData();
        fetchAttendanceStatus();

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

    private void fetchAttendanceStatus() {
        if (currentUser == null) return;

        // Lấy ngày hôm nay (dạng yyyy-MM-dd để query theo format mới)
        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        if (attendanceListener != null) {
            attendanceListener.remove();
        }

        // Truy vấn tất cả bản ghi điểm danh trong ngày hôm nay
        attendanceListener = db.collection("attendanceRecords")
                .whereEqualTo("uid", currentUser.getUid())
                .whereEqualTo("date", todayStr)
                .orderBy("timestamp", Query.Direction.DESCENDING) // Sắp xếp theo thời gian mới nhất
                .addSnapshotListener((snapshots, error) -> {
                    // Kiểm tra Fragment vẫn còn attached trước khi xử lý
                    if (!isAdded() || getContext() == null) return;

                    if (error != null) {
                        Toast.makeText(requireContext(), "Lỗi lấy dữ liệu điểm danh", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        // ĐÃ CÓ DỮ LIỆU ĐIỂM DANH
                        processAttendanceData(snapshots.getDocuments());
                    } else {
                        // CHƯA CÓ DỮ LIỆU
                        processNoAttendanceData();
                    }
                });
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


    @SuppressLint("SetTextI18n")
    private void processAttendanceData(java.util.List<DocumentSnapshot> docs) {
        // Tìm record CHECK_IN và CHECK_OUT mới nhất
        Long latestCheckInMillis = null;
        Long latestCheckOutMillis = null;

        for (DocumentSnapshot doc : docs) {
            String checkType = doc.getString("checkType");
            Long timestampMillis = doc.getLong("timestamp");

            if (timestampMillis == null) continue;

            if ("CHECK_IN".equals(checkType)) {
                if (latestCheckInMillis == null || timestampMillis > latestCheckInMillis) {
                    latestCheckInMillis = timestampMillis;
                }
            } else if ("CHECK_OUT".equals(checkType)) {
                if (latestCheckOutMillis == null || timestampMillis > latestCheckOutMillis) {
                    latestCheckOutMillis = timestampMillis;
                }
            }
        }

        // 1. Hiển thị giờ Check-in
        if (latestCheckInMillis != null) {
            Date checkInDate = new Date(latestCheckInMillis);
            String timeStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(checkInDate);
            tvCheckInTime.setText(timeStr);

            // --- KIỂM TRA ĐÚNG GIỜ / MUỘN / VẮNG ---
            Calendar calCheckIn = Calendar.getInstance();
            calCheckIn.setTime(checkInDate);

            int checkInHour = calCheckIn.get(Calendar.HOUR_OF_DAY);

            // Nếu điểm danh sau 17h -> VẮNG
            if (checkInHour >= END_WORK_HOUR) {
                setUIState(StatusState.ABSENT);
            } else {
                // Tạo mốc thời gian quy định (8:00 sáng hôm nay)
                Calendar calDeadline = Calendar.getInstance();
                calDeadline.setTime(checkInDate);
                calDeadline.set(Calendar.HOUR_OF_DAY, START_HOUR);
                calDeadline.set(Calendar.MINUTE, START_MINUTE);
                calDeadline.set(Calendar.SECOND, 0);

                if (calCheckIn.before(calDeadline) || calCheckIn.equals(calDeadline)) {
                    // -> ĐÚNG GIỜ (XANH LÁ)
                    setUIState(StatusState.ON_TIME);
                } else {
                    // -> ĐI MUỘN (VÀNG)
                    setUIState(StatusState.LATE);
                }
            }
        } else {
            tvCheckInTime.setText("--:--");
            setUIState(StatusState.NOT_CHECKED_IN);
        }

        // 2. Hiển thị giờ Check-out
        if (latestCheckOutMillis != null) {
            Date checkOutDate = new Date(latestCheckOutMillis);
            String timeOutStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(checkOutDate);
            tvCheckOutTime.setText(timeOutStr);

            // 3. Tính tổng giờ làm (checkout - checkin)
            if (latestCheckInMillis != null) {
                long diffMillis = latestCheckOutMillis - latestCheckInMillis;
                long hours = diffMillis / (1000 * 60 * 60);
                long minutes = (diffMillis % (1000 * 60 * 60)) / (1000 * 60);

                tvTotalHours.setText(hours + " giờ " + minutes + " phút");

                // Cập nhật progress bar (giả sử 8 giờ = 100%)
                int totalMinutes = (int) (hours * 60 + minutes);
                int progress = Math.min(100, (totalMinutes * 100) / (8 * 60));
                progressBar.setProgress(progress);
            } else {
                tvTotalHours.setText("0 giờ 0 phút");
                progressBar.setProgress(0);
            }
        } else {
            tvCheckOutTime.setText("--:--");
            tvTotalHours.setText("0 giờ 0 phút");
            progressBar.setProgress(0);
        }
    }

    private void processNoAttendanceData() {
        // Reset text giờ
        tvCheckInTime.setText("--:--");
        tvCheckOutTime.setText("--:--");

        // Kiểm tra giờ hiện tại để quyết định VẮNG hay CHƯA ĐIỂM DANH
        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);

        if (currentHour >= END_WORK_HOUR) {
            // Đã hết ngày làm việc mà chưa có dữ liệu -> VẮNG (ĐỎ)
            setUIState(StatusState.ABSENT);
        } else {
            // Chưa hết ngày -> CHƯA ĐIỂM DANH (XÁM)
            setUIState(StatusState.NOT_CHECKED_IN);
        }
    }

    // Enum cho các trạng thái để dễ quản lý
    private enum StatusState {
        NOT_CHECKED_IN, // Xám
        ON_TIME,        // Xanh lá
        LATE,           // Vàng
        ABSENT          // Đỏ
    }

    @SuppressLint("SetTextI18n")
    private void setUIState(StatusState state) {
        int colorRes;
        int bgDrawableRes;
        String statusText;
        int iconRes;

        switch (state) {
            case ON_TIME:
                colorRes = Color.parseColor("#059669"); // Xanh lá đậm
                bgDrawableRes = R.drawable.circle_green_light_bg;
                statusText = "Đúng giờ";
                iconRes = R.drawable.ic_check;
                break;

            case LATE:
                colorRes = Color.parseColor("#D97706"); // Vàng cam
                bgDrawableRes = R.drawable.circle_yellow_light_bg;
                statusText = "Đi muộn";
                iconRes = R.drawable.ic_check;
                break;

            case ABSENT:
                colorRes = Color.parseColor("#DC2626"); // Đỏ
                bgDrawableRes = R.drawable.circle_red_light_bg;
                statusText = "Vắng mặt";
                iconRes = R.drawable.ic_close;
                break;

            case NOT_CHECKED_IN:
            default:
                colorRes = Color.parseColor("#6B7280"); // Xám
                bgDrawableRes = R.drawable.circle_gray_light_bg;
                statusText = "Chưa điểm danh";
                iconRes = R.drawable.ic_time;
                break;
        }

        // Cập nhật UI
        tvStatus.setText(statusText);
        tvStatus.setTextColor(colorRes);

        // Đổi màu icon
        ivCheckmark.setImageResource(iconRes);
        ivCheckmark.setColorFilter(colorRes);

        // Đổi màu nền của icon
        ivCheckmark.setBackgroundResource(bgDrawableRes);
    }
}