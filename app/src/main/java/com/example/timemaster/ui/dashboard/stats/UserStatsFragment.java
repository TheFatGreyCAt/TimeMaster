package com.example.timemaster.ui.dashboard.stats;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.timemaster.R;
import com.example.timemaster.data.model.CheckIn;
import com.example.timemaster.data.repository.UserAttendanceRepository;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class UserStatsFragment extends Fragment {

    // Views
    private TextView tvWeekLabel;
    private ImageButton btnPrevWeek, btnNextWeek;
    private PieChart pieChart;
    private LinearLayout llCheckinContainer;

    private TextView tvFullName, tvUsername;


    // Repo + state
    private final UserAttendanceRepository repository = new UserAttendanceRepository();
    private Calendar currentWeekStart;            // Monday of current week
    private final Locale VI = new Locale("vi", "VN");
    private final SimpleDateFormat LABEL_DF = new SimpleDateFormat("dd/MM/yyyy", VI);

    private List<CheckIn> currentWeekData = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.frag_user_stats, container, false);

        tvWeekLabel        = v.findViewById(R.id.tv_week_label);
        btnPrevWeek        = v.findViewById(R.id.btn_prev_week);
        btnNextWeek        = v.findViewById(R.id.btn_next_week);
        pieChart           = v.findViewById(R.id.pieChart);
        llCheckinContainer = v.findViewById(R.id.ll_checkin_container);
        tvFullName = v.findViewById(R.id.tv_full_name);
        tvUsername = v.findViewById(R.id.tv_username);

        initCurrentWeekStart();
        setupWeekButtons();

        loadUserInfo();
        loadWeekData();

        return v;
    }

    // Hàm load user ìnfo
    private void loadUserInfo() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            if (tvFullName != null) tvFullName.setText("Khách");
            if (tvUsername != null) tvUsername.setText("");
            return;
        }

        // Hiển thị tạm thời tên từ FirebaseAuth (nếu có)
        if (tvFullName != null) {
            String displayName = user.getDisplayName();
            tvFullName.setText(displayName != null && !displayName.isEmpty()
                    ? displayName
                    : "Người dùng");
        }
        if (tvUsername != null) {
            String email = user.getEmail();
            tvUsername.setText(email != null ? email : "");
        }

        // Nếu bạn có collection "users" lưu fullName, username thì đọc lên cho “xịn” hơn
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String fullName = doc.getString("fullName");
                        String username = doc.getString("username");

                        if (fullName != null && !fullName.isEmpty() && tvFullName != null) {
                            tvFullName.setText(fullName);
                        }
                        if (username != null && !username.isEmpty() && tvUsername != null) {
                            tvUsername.setText(username);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // có thể log lỗi, nhưng UI vẫn dùng tên từ FirebaseAuth nên không sao
                });
    }


    // ==== Khởi tạo tuần hiện tại (Thứ 2) ====
    private void initCurrentWeekStart() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        int dow = cal.get(Calendar.DAY_OF_WEEK);
        int diffToMonday = (dow == Calendar.SUNDAY)
                ? -6
                : (Calendar.MONDAY - dow);
        cal.add(Calendar.DAY_OF_MONTH, diffToMonday);

        currentWeekStart = cal;
    }

    private void setupWeekButtons() {
        btnPrevWeek.setOnClickListener(v -> {
            currentWeekStart.add(Calendar.DAY_OF_MONTH, -7);
            loadWeekData();
        });

        btnNextWeek.setOnClickListener(v -> {
            currentWeekStart.add(Calendar.DAY_OF_MONTH, 7);
            loadWeekData();
        });
    }

    // ==== Load dữ liệu tuần hiện tại từ Firestore ====
    private void loadWeekData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        updateWeekLabel();

        repository.getWeekForUser(user.getUid(), currentWeekStart.getTime(),
                new UserAttendanceRepository.OnWeekLoaded() {
                    @Override
                    public void onSuccess(List<CheckIn> weekData) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            currentWeekData = weekData != null ? weekData : new ArrayList<>();
                            renderPieChart();
                            renderCheckinList();
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            currentWeekData = new ArrayList<>();
                            renderPieChart();
                            renderCheckinList();
                            if (getContext() != null) {
                                Toast.makeText(getContext(),
                                        "Lỗi tải dữ liệu: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
    }

    private void updateWeekLabel() {
        Calendar endCal = (Calendar) currentWeekStart.clone();
        endCal.add(Calendar.DAY_OF_MONTH, 6);
        String startStr = LABEL_DF.format(currentWeekStart.getTime());
        String endStr   = LABEL_DF.format(endCal.getTime());
        tvWeekLabel.setText(startStr + " - " + endStr);
    }

    // ==== Render PieChart dựa trên currentWeekData ====
    private void renderPieChart() {
        if (pieChart == null) return;

        int present = 0;
        int late = 0;
        int early = 0;
        int absent = 0;

        for (CheckIn ci : currentWeekData) {
            int t = ci.getStatusType();
            // 0: Đúng giờ, 1: Đi trễ, 2: Về sớm, 3: Vắng mặt, 4: Đi trễ & Về sớm
            if (t == 0) present++;
            else if (t == 1) late++;
            else if (t == 2) early++;
            else if (t == 3) absent++;
            else if (t == 4) {
                late++;
                early++;
            }
        }

        int total = present + late + early + absent;
        pieChart.setNoDataText("Chưa có dữ liệu tuần này");
        pieChart.setNoDataTextColor(Color.GRAY);

        if (total == 0) {
            pieChart.clear();
            pieChart.invalidate();
            return;
        }

        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(present, "Đúng giờ"));
        entries.add(new PieEntry(late,    "Đi trễ"));
        entries.add(new PieEntry(early,   "Về sớm"));
        entries.add(new PieEntry(absent,  "Vắng mặt"));

        PieDataSet set = new PieDataSet(entries, "");
        set.setColors(new int[] {
                Color.parseColor("#43E97B"), // present
                Color.parseColor("#FFC542"), // late
                Color.parseColor("#0AA2C0"), // early
                Color.parseColor("#FF647C")  // absent
        });
        set.setValueTextSize(14f);
        set.setValueFormatter(new ValueFormatter() {
            @Override public String getPieLabel(float value, PieEntry pieEntry) {
                return String.valueOf((int) value);
            }
        });

        PieData data = new PieData(set);
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(true);
        pieChart.setUsePercentValues(false);
        pieChart.setDrawEntryLabels(true);
        pieChart.setEntryLabelTextSize(12f);
        pieChart.setHoleRadius(52f);
        pieChart.setTransparentCircleRadius(57f);
        pieChart.setCenterText("Tổng: " + total);
        pieChart.setCenterTextSize(14f);
        pieChart.invalidate();
    }

    // ==== Render danh sách check-in vào ll_checkin_container ====
    private void renderCheckinList() {
        if (llCheckinContainer == null || getContext() == null) return;

        llCheckinContainer.removeAllViews();

        if (currentWeekData == null || currentWeekData.isEmpty()) {
            TextView tv = new TextView(getContext());
            tv.setText("Không có dữ liệu chấm công trong tuần này");
            tv.setTextColor(Color.GRAY);
            tv.setPadding(16, 16, 16, 16);
            llCheckinContainer.addView(tv);
            return;
        }

        // Mỗi CheckIn = 1 dòng: Ngày | In | Out | Trạng thái
        for (CheckIn ci : currentWeekData) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(16, 16, 16, 16);

            // ngày
            TextView tvDate = new TextView(getContext());
            tvDate.setText(ci.getDate());
            tvDate.setTextSize(14f);
            tvDate.setTextColor(Color.parseColor("#232323"));

            // giờ in/out
            TextView tvTimes = new TextView(getContext());
            String in  = ci.getCheckInTime();
            String out = ci.getCheckOutTime();
            String times = "Giờ vào: " + (in  != null && !in.isEmpty()  ? in  : "—")
                    + "   ·   Giờ ra: " + (out != null && !out.isEmpty() ? out : "—");
            tvTimes.setText(times);
            tvTimes.setTextSize(13f);
            tvTimes.setTextColor(Color.parseColor("#555555"));

            // trạng thái
            TextView tvStatus = new TextView(getContext());
            tvStatus.setText(ci.getStatus());
            tvStatus.setTextSize(13f);

            int t = ci.getStatusType();
            int color;
            if (t == 0) { // Đúng giờ
                color = Color.parseColor("#43E97B");
            } else if (t == 1 || t == 4) { // Đi trễ (+ có thể về sớm)
                color = Color.parseColor("#FFC542");
            } else if (t == 2) { // Về sớm
                color = Color.parseColor("#0AA2C0");
            } else if (t == 3) { // Vắng mặt
                color = Color.parseColor("#FF647C");
            } else {
                color = Color.DKGRAY;
            }
            tvStatus.setTextColor(color);

            // gộp vào row
            row.addView(tvDate);
            row.addView(tvTimes);
            row.addView(tvStatus);

            // thêm divider nhỏ
            View divider = new View(getContext());
            LinearLayout.LayoutParams lpDiv =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, 1);
            lpDiv.setMargins(0, 8, 0, 8);
            divider.setLayoutParams(lpDiv);
            divider.setBackgroundColor(Color.parseColor("#DDDDDD"));

            llCheckinContainer.addView(row);
            llCheckinContainer.addView(divider);
        }
    }
}
