package com.example.timemaster.ui.dashboard.user;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.timemaster.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
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
            String name = currentUser.getDisplayName();
            tvUsername.setText((name != null && !name.isEmpty()) ? name + "!" : "Bạn!");

            // Lời chào theo giờ
            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            if (hour < 12) tvGreeting.setText("Chào buổi sáng,");
            else if (hour < 18) tvGreeting.setText("Chào buổi chiều,");
            else tvGreeting.setText("Chào buổi tối,");
        }
    }

    private void fetchAttendanceStatus() {
        if (currentUser == null) return;

        // Lấy ngày hôm nay (dạng String dd/MM/yyyy để query)
        // Lưu ý: Trong CheckInActivity bạn lưu date format nào thì ở đây dùng format đó
        // Giả sử bạn lưu field "date" là String "dd/MM/yyyy"
        String todayStr = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

        // Truy vấn bảng attendanceRecords
        db.collection("attendanceRecords")
                .whereEqualTo("uid", currentUser.getUid())
                .whereEqualTo("date", todayStr)
                .limit(1) // Chỉ lấy 1 bản ghi trong ngày
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Toast.makeText(getContext(), "Lỗi lấy dữ liệu điểm danh", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        // --- TRƯỜNG HỢP: ĐÃ CHECK-IN ---
                        DocumentSnapshot doc = snapshots.getDocuments().get(0);
                        processAttendanceData(doc);
                    } else {
                        // --- TRƯỜNG HỢP: CHƯA CÓ DỮ LIỆU ---
                        processNoAttendanceData();
                    }
                });
    }

    private void processAttendanceData(DocumentSnapshot doc) {
        Timestamp checkInTimestamp = doc.getTimestamp("checkInTime");
        Timestamp checkOutTimestamp = doc.getTimestamp("checkOutTime");

        // 1. Hiển thị giờ Check-in
        if (checkInTimestamp != null) {
            Date checkInDate = checkInTimestamp.toDate();
            String timeStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(checkInDate);
            tvCheckInTime.setText(timeStr);

            // --- KIỂM TRA ĐÚNG GIỜ HAY TRỄ ---
            Calendar calCheckIn = Calendar.getInstance();
            calCheckIn.setTime(checkInDate);

            // Tạo mốc thời gian quy định (Ví dụ 8:00 sáng hôm nay)
            Calendar calDeadline = Calendar.getInstance();
            calDeadline.setTime(checkInDate); // Lấy cùng ngày tháng năm
            calDeadline.set(Calendar.HOUR_OF_DAY, START_HOUR);
            calDeadline.set(Calendar.MINUTE, START_MINUTE);
            calDeadline.set(Calendar.SECOND, 0);

            if (calCheckIn.before(calDeadline) || calCheckIn.equals(calDeadline)) {
                // -> ĐÚNG GIỜ (XANH LÁ)
                setUIState(StatusState.ON_TIME);
            } else {
                // -> ĐI HỌC TRỄ (VÀNG)
                setUIState(StatusState.LATE);
            }
        }

        // 2. Hiển thị giờ Check-out
        if (checkOutTimestamp != null) {
            Date checkOutDate = checkOutTimestamp.toDate();
            String timeOutStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(checkOutDate);
            tvCheckOutTime.setText(timeOutStr);

            // Tính tổng giờ làm (nếu cần)
            // ... logic tính toán ...
        } else {
            tvCheckOutTime.setText("--:--");
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
                bgDrawableRes = R.drawable.circle_green_light_bg; // Cần tạo drawable này hoặc dùng shape
                statusText = "Đúng giờ";
                iconRes = R.drawable.ic_check; // Icon tích
                break;

            case LATE:
                colorRes = Color.parseColor("#D97706"); // Vàng cam
                bgDrawableRes = R.drawable.circle_yellow_light_bg; // Cần tạo
                statusText = "Điểm danh trễ";
                iconRes = R.drawable.ic_check; // Hoặc icon cảnh báo
                break;

            case ABSENT:
                colorRes = Color.parseColor("#DC2626"); // Đỏ
                bgDrawableRes = R.drawable.circle_red_light_bg; // Cần tạo
                statusText = "Vắng mặt";
                iconRes = R.drawable.ic_close; // Icon X
                break;

            case NOT_CHECKED_IN:
            default:
                colorRes = Color.parseColor("#6B7280"); // Xám
                bgDrawableRes = R.drawable.circle_gray_light_bg; // Cần tạo
                statusText = "Chưa điểm danh";
                iconRes = R.drawable.ic_time; // Icon đồng hồ
                break;
        }

        // Cập nhật UI
        tvStatus.setText(statusText);
        tvStatus.setTextColor(colorRes);

        // Đổi màu icon
        ivCheckmark.setImageResource(iconRes);
        ivCheckmark.setColorFilter(colorRes);

        // Đổi màu nền của icon (Nếu bạn dùng drawable shape)
        // Lưu ý: Cần tạo các file xml drawable cho background hình tròn nhạt màu tương ứng
        // Ở đây mình set tint tạm thời cho background nếu drawable hỗ trợ
        ivCheckmark.setBackgroundTintList(ColorStateList.valueOf(adjustAlpha(colorRes, 0.2f)));
    }

    // Hàm phụ trợ để làm nhạt màu cho background (Alpha 20%)
    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }
}