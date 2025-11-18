package com.example.timemaster.ui.dashboard.stats;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.timemaster.R;
import com.example.timemaster.data.model.CheckIn;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class UserStatsFragment extends Fragment {

    private int currentWeekOffset = 0; // 0: tuần này, -1: tuần trước ...
    private final int MAX_PAST_WEEKS = 3;
    private TextView tvWeekLabel;
    private ImageButton btnPrevWeek, btnNextWeek;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_user_stats, container, false);
        tvWeekLabel = view.findViewById(R.id.tv_week_label);
        btnPrevWeek = view.findViewById(R.id.btn_prev_week);
        btnNextWeek = view.findViewById(R.id.btn_next_week);

        LinearLayout llCheckinContainer = view.findViewById(R.id.ll_checkin_container);
        PieChart pieChart = view.findViewById(R.id.pieChart);

        btnPrevWeek.setOnClickListener(v -> {
            if (currentWeekOffset > -MAX_PAST_WEEKS) {
                currentWeekOffset--;
                updateWeekData(llCheckinContainer, pieChart);
            }
        });
        btnNextWeek.setOnClickListener(v -> {
            if (currentWeekOffset < 0) {
                currentWeekOffset++;
                updateWeekData(llCheckinContainer, pieChart);
            }
        });

        // Lần đầu load tuần hiện tại
        updateWeekData(llCheckinContainer, pieChart);

        return view;
    }

    private void updateWeekData(LinearLayout llCheckinContainer, PieChart pieChart) {
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.add(Calendar.WEEK_OF_YEAR, currentWeekOffset);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String startOfWeek = dateFmt.format(cal.getTime());
        cal.add(Calendar.DAY_OF_WEEK, 6);
        String endOfWeek = dateFmt.format(cal.getTime());

        tvWeekLabel.setText(startOfWeek + " - " + endOfWeek);

        btnPrevWeek.setEnabled(currentWeekOffset > -MAX_PAST_WEEKS);
        btnNextWeek.setEnabled(currentWeekOffset < 0);

        // Dữ liệu lịch sử demo khác nhau cho mỗi tuần
        List<CheckIn> checkInList = getDemoCheckInListForWeek(currentWeekOffset);

        // Render check-in list
        llCheckinContainer.removeAllViews();
        for (CheckIn item : checkInList) {
            View checkinView = LayoutInflater.from(getContext()).inflate(R.layout.item_checkin_card, llCheckinContainer, false);
            TextView tvDayOfWeek = checkinView.findViewById(R.id.tv_day_of_week);
            TextView tvCheckin = checkinView.findViewById(R.id.tv_check_in);
            TextView tvCheckout = checkinView.findViewById(R.id.tv_check_out);
            TextView tvStatus = checkinView.findViewById(R.id.tv_status);

            tvDayOfWeek.setText(item.getDate());
            tvCheckin.setText(item.getCheckInTime() == null ? "—" : item.getCheckInTime());
            tvCheckout.setText(item.getCheckOutTime() == null ? "—" : item.getCheckOutTime());

            int statusType = item.getStatusType();
            switch (statusType) {
                case 0:
                    tvStatus.setText("Đúng giờ");
                    tvStatus.setBackgroundResource(R.drawable.bg_badge_present);
                    tvStatus.setTextColor(Color.parseColor("#077342"));
                    break;
                case 1:
                    tvStatus.setText("Đi trễ");
                    tvStatus.setBackgroundResource(R.drawable.bg_badge_late);
                    tvStatus.setTextColor(Color.parseColor("#FFA000"));
                    break;
                case 2:
                    tvStatus.setText("Về sớm");
                    tvStatus.setBackgroundResource(R.drawable.bg_badge_earlyout);
                    tvStatus.setTextColor(Color.parseColor("#1186B4"));
                    break;
                case 3:
                    tvStatus.setText("Vắng mặt");
                    tvStatus.setBackgroundResource(R.drawable.bg_badge_absent);
                    tvStatus.setTextColor(Color.parseColor("#D90429"));
                    break;
                default:
                    tvStatus.setText("—");
                    tvStatus.setBackgroundResource(R.drawable.bg_badge_absent);
                    tvStatus.setTextColor(Color.GRAY);
                    break;
            }
            llCheckinContainer.addView(checkinView);
        }


        // Biểu đồ theo tuần
        int dungGio = 0, diTre = 0, veSom = 0, vangMat = 0;
        for (CheckIn item : checkInList) {
            switch (item.getStatusType()) {
                case 0: dungGio++; break;
                case 1: diTre++; break;
                case 2: veSom++; break;
                case 3: vangMat++; break;
                case 4: diTre++; veSom++; break;
            }
        }
        ArrayList<PieEntry> entries = new ArrayList<>();
        if (dungGio > 0) entries.add(new PieEntry(dungGio, "Đúng giờ"));
        if (diTre > 0) entries.add(new PieEntry(diTre, "Đi trễ"));
        if (veSom > 0) entries.add(new PieEntry(veSom, "Về sớm"));
        if (vangMat > 0) entries.add(new PieEntry(vangMat, "Vắng mặt"));

        PieDataSet set = new PieDataSet(entries, "");
        set.setColors(new int[]{
                Color.parseColor("#43E97B"),
                Color.parseColor("#FFC542"),
                Color.parseColor("#4D8AF0"),
                Color.parseColor("#FF647C")
        });

        set.setValueFormatter(new ValueFormatter() {
            @Override
            public String getPieLabel(float value, PieEntry pieEntry) {
                return String.valueOf((int) value);
            }
        });
        set.setValueTextSize(13f);

        PieData data = new PieData(set);
        pieChart.setData(data);
        pieChart.setUsePercentValues(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.setHoleRadius(45f);
        pieChart.setTransparentCircleAlpha(50);
        pieChart.setCenterText("Check-in");
        pieChart.setCenterTextSize(16f);
        pieChart.invalidate();
    }

    private int getCurrentIsoWeekNumber() {
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        return cal.get(Calendar.WEEK_OF_YEAR);
    }

    private List<CheckIn> getDemoCheckInListForWeek(int weekOffset) {
        List<CheckIn> list = new ArrayList<>();

        // Tuần hiện tại là weekOffset = 0, tuần trước là -1, v.v.
        if (weekOffset == 0) {
            list.add(new CheckIn("Thứ 2", "08:00", "17:30"));
            list.add(new CheckIn("Thứ 3", "08:12", "17:40"));
            list.add(new CheckIn("Thứ 4", null, null));
            list.add(new CheckIn("Thứ 5", "07:55", "17:20"));
            list.add(new CheckIn("Thứ 6", "08:15", "17:10"));
            list.add(new CheckIn("Thứ 7", "08:00", "17:30"));
            list.add(new CheckIn("CN", "08:00", "17:30"));
        } else if (weekOffset == -1) {
            list.add(new CheckIn("Thứ 2", "08:03", "17:32"));
            list.add(new CheckIn("Thứ 3", "07:50", "17:30"));
            list.add(new CheckIn("Thứ 4", "07:56", "17:19"));
            list.add(new CheckIn("Thứ 5", "08:41", "17:36"));
            list.add(new CheckIn("Thứ 6", "08:31", "16:54"));
            list.add(new CheckIn("Thứ 7", null, null));
            list.add(new CheckIn("CN", null, null));
        } else if (weekOffset == -2) {
            list.add(new CheckIn("Thứ 2", "08:00", "17:23"));
            list.add(new CheckIn("Thứ 3", "08:20", "17:12"));
            list.add(new CheckIn("Thứ 4", "08:16", "17:32"));
            list.add(new CheckIn("Thứ 5", "09:00", "17:40"));
            list.add(new CheckIn("Thứ 6", null, null));
            list.add(new CheckIn("Thứ 7", "08:00", "17:29"));
            list.add(new CheckIn("CN", "07:59", "17:34"));
        } else if (weekOffset == -3) {
            list.add(new CheckIn("Thứ 2", "07:58", "17:40"));
            list.add(new CheckIn("Thứ 3", null, null));
            list.add(new CheckIn("Thứ 4", "08:02", "17:30"));
            list.add(new CheckIn("Thứ 5", null, null));
            list.add(new CheckIn("Thứ 6", "08:11", "17:30"));
            list.add(new CheckIn("Thứ 7", "08:19", "17:35"));
            list.add(new CheckIn("CN", "08:05", "16:59"));
        }
        return list;
    }
}
