package com.example.timemaster.ui.dashboard.stats;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.timemaster.R;
import com.example.timemaster.data.model.DayAttendance;
import com.example.timemaster.data.model.StatusType;
import com.example.timemaster.data.model.UserAttendance;
import com.example.timemaster.data.model.WeekAttendance;
import com.example.timemaster.ui.widget.SyncedHorizontalScrollView;
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

public class AdminStatsFragment extends Fragment {

    // ==== Views (UI) ====
    private LinearLayout layoutPickDate;
    private TextView textViewSelectedDate;
    private TextView tvWeekLabel;

    private SyncedHorizontalScrollView headerScroll, listScroll;
    private LinearLayout llUserList;

    private ImageButton btnPrevWeek, btnNextWeek, btnPrevPage, btnNextPage;
    private TextView tvPageIndicator;

    private TextView tvPresentCount, tvLateCount, tvAbsentCount;
    private LinearLayout cardPresent, cardLate, cardAbsent;

    private EditText edSearch;

    private PieChart pieChart;

    // ==== VM / State ====
    private AdminStatsViewModel vm;
    private final Locale VI = new Locale("vi", "VN");
    private final SimpleDateFormat DF = new SimpleDateFormat("dd/MM/yyyy", VI);
    private Calendar selectedDate = Calendar.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.frag_admin_stats, container, false);

        // ---- Bind views ----
        layoutPickDate       = v.findViewById(R.id.layout_pick_date);
        textViewSelectedDate = v.findViewById(R.id.tv_selected_date);
        tvWeekLabel          = v.findViewById(R.id.tv_week_label);

        headerScroll         = v.findViewById(R.id.header_scroll);
        listScroll           = v.findViewById(R.id.list_scroll);
        llUserList           = v.findViewById(R.id.ll_user_list);

        btnPrevWeek          = v.findViewById(R.id.btn_prev_week);
        btnNextWeek          = v.findViewById(R.id.btn_next_week);
        btnPrevPage          = v.findViewById(R.id.btn_prev_page);
        btnNextPage          = v.findViewById(R.id.btn_next_page);
        tvPageIndicator      = v.findViewById(R.id.tv_page_indicator);

        tvPresentCount       = v.findViewById(R.id.tv_present_count);
        tvLateCount          = v.findViewById(R.id.tv_late_count);
        tvAbsentCount        = v.findViewById(R.id.tv_absent_count);

        cardPresent          = v.findViewById(R.id.card_present);
        cardLate             = v.findViewById(R.id.card_late);
        cardAbsent           = v.findViewById(R.id.card_absent);

        edSearch             = v.findViewById(R.id.ed_search);
        pieChart             = v.findViewById(R.id.pieChart);

        // ---- Đồng bộ cuộn ngang header <-> list ----
        headerScroll.setPartner(listScroll);
        listScroll.setPartner(headerScroll);

        // ---- ViewModel ----
        vm = new ViewModelProvider(this).get(AdminStatsViewModel.class);
        vm.setPageSize(5);

        // ---- Quan sát dữ liệu ----
        vm.getWeeksLiveData().observe(getViewLifecycleOwner(), weeks -> renderAll());
        vm.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            // có progressBar thì show/hide ở đây
        });
        vm.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null && getContext() != null) {
                Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + err, Toast.LENGTH_SHORT).show();
            }
        });

        // ---- Chọn ngày ----
        textViewSelectedDate.setText(DF.format(selectedDate.getTime()));
        layoutPickDate.setOnClickListener(view -> {
            int y = selectedDate.get(Calendar.YEAR);
            int m = selectedDate.get(Calendar.MONTH);
            int d = selectedDate.get(Calendar.DAY_OF_MONTH);
            DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                    (w, year, month, dayOfMonth) -> {
                        selectedDate.set(year, month, dayOfMonth);
                        textViewSelectedDate.setText(DF.format(selectedDate.getTime()));
                        onDateSelected();
                    }, y, m, d);
            dialog.show();
        });

        // ---- Tìm kiếm theo tên ----
        if (edSearch != null) {
            edSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                    vm.setSearchText(s == null ? "" : s.toString());
                    renderDayPage();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        // ---- Điều hướng tuần ----
        btnPrevWeek.setOnClickListener(vw -> {
            vm.setCurrentWeekIndex(vm.getCurrentWeekIndex() - 1);
            renderAll();
        });
        btnNextWeek.setOnClickListener(vw -> {
            vm.setCurrentWeekIndex(vm.getCurrentWeekIndex() + 1);
            renderAll();
        });

        // ---- Phân trang ----
        btnPrevPage.setOnClickListener(vw -> {
            vm.prevPage();
            renderDayPage();
        });
        btnNextPage.setOnClickListener(vw -> {
            vm.nextPage();
            renderDayPage();
        });

        // ---- Bộ lọc trạng thái: bật/tắt riêng từng ô ----
        cardPresent.setOnClickListener(vw -> {
            Integer cur = vm.getFilterStatusType();
            if (cur != null && cur == StatusType.PRESENT) {
                vm.setFilterStatusType(null);          // ấn lần 2 -> bỏ lọc
            } else {
                vm.setFilterStatusType(StatusType.PRESENT);
            }
            renderDayPage();
            highlightFilter();
        });

        cardLate.setOnClickListener(vw -> {
            Integer cur = vm.getFilterStatusType();
            if (cur != null && cur == StatusType.LATE) {
                vm.setFilterStatusType(null);
            } else {
                vm.setFilterStatusType(StatusType.LATE);
            }
            renderDayPage();
            highlightFilter();
        });

        cardAbsent.setOnClickListener(vw -> {
            Integer cur = vm.getFilterStatusType();
            if (cur != null && cur == StatusType.ABSENT) {
                vm.setFilterStatusType(null);
            } else {
                vm.setFilterStatusType(StatusType.ABSENT);
            }
            renderDayPage();
            highlightFilter();
        });

        // ---- Load dữ liệu ban đầu ----
        vm.loadData();

        return v;
    }

    // ---- Xử lý khi người dùng chọn ngày mới từ DatePicker ----
    private void onDateSelected() {
        List<WeekAttendance> weeks = vm.getWeeksLiveData().getValue();
        if (weeks == null) return;

        boolean dateFound = false;
        for (int i = 0; i < weeks.size(); i++) {
            WeekAttendance week = weeks.get(i);
            if (week.getDays() == null || week.getDays().isEmpty()) continue;

            for (int j = 0; j < week.getDays().size(); j++) {
                DayAttendance day = week.getDays().get(j);
                if (day.getDate() == null) continue;

                Calendar dayCal = Calendar.getInstance();
                dayCal.setTime(day.getDate());

                if (dayCal.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                        dayCal.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)) {

                    vm.setCurrentWeekIndex(i);
                    vm.setCurrentDayIndex(j);
                    renderAll(); // Render lại toàn bộ vì tuần có thể thay đổi
                    dateFound = true;
                    break;
                }
            }
            if (dateFound) break;
        }

        if (!dateFound) {
            Toast.makeText(getContext(), "Không có dữ liệu cho ngày đã chọn. Vui lòng chọn trong 4 tuần gần nhất.", Toast.LENGTH_LONG).show();
        }
    }

    // =========================
    //  Render helpers
    // =========================
    private void renderAll() {
        if (vm.getWeeksLiveData().getValue() == null ||
                vm.getWeeksLiveData().getValue().isEmpty()) {

            llUserList.removeAllViews();
            pieChart.clear();
            pieChart.invalidate();
            tvWeekLabel.setText("Chưa có dữ liệu");
            tvPresentCount.setText("0");
            tvLateCount.setText("0");
            tvAbsentCount.setText("0");
            tvPageIndicator.setText("0/0");
            btnPrevWeek.setEnabled(false);
            btnNextWeek.setEnabled(false);
            btnPrevPage.setEnabled(false);
            btnNextPage.setEnabled(false);
            return;
        }

        // label tuần dạng: 01/11/2025 - 07/11/2025
        tvWeekLabel.setText(vm.getWeekLabel());

        int weekCount = vm.getWeeksLiveData().getValue().size();
        btnPrevWeek.setEnabled(vm.getCurrentWeekIndex() > 0);
        btnNextWeek.setEnabled(vm.getCurrentWeekIndex() < weekCount - 1);

        // Counters + Pie theo TUẦN
        renderWeeklyCounters();
        renderWeeklyPieChart();

        // Bảng theo NGÀY hiện tại
        renderDayPage();

        highlightFilter();
    }

    private void renderWeeklyCounters() {
        // [present, late, earlyOut, absent]
        int[] c = vm.getWeeklyCounts();
        int present = c[0];
        int late = c[1];
        int early = c[2];
        int absent = c[3];

        // tuỳ bạn muốn hiện cái gì lên 3 ô, ở đây mình giữ:
        tvPresentCount.setText(String.valueOf(present));
        tvLateCount.setText(String.valueOf(late));
        tvAbsentCount.setText(String.valueOf(absent + early)); // hoặc chỉ absent
    }

    private void renderWeeklyPieChart() {
        int[] c = vm.getWeeklyCounts(); // [present, late, earlyOut, absent]
        int present = c[0];
        int late = c[1];
        int early = c[2];
        int absent = c[3];

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
        entries.add(new PieEntry(absent,  "Vắng"));

        PieDataSet set = new PieDataSet(entries, "");
        set.setColors(new int[] {
                Color.parseColor("#43E97B"),
                Color.parseColor("#FFC542"),
                Color.parseColor("#0AA2C0"),
                Color.parseColor("#FF647C")
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

    private void renderDayPage() {
        llUserList.removeAllViews();

        List<UserAttendance> page = vm.getCurrentPageData();
        int totalPages = vm.getTotalPagesForCurrentDay();

        if (page.isEmpty()) {
            tvPageIndicator.setText("0/0");
            btnPrevPage.setEnabled(false);
            btnNextPage.setEnabled(false);
            TextView empty = new TextView(requireContext());
            empty.setText("Không có dữ liệu trong ngày này");
            empty.setTextColor(Color.GRAY);
            empty.setPadding(16, 16, 16, 16);
            llUserList.addView(empty);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (UserAttendance u : page) {
            View row = inflater.inflate(R.layout.item_user_status_row, llUserList, false);
            ((TextView) row.findViewById(R.id.tv_name))
                    .setText(u.getName());
            ((TextView) row.findViewById(R.id.tv_check_in))
                    .setText(u.getCheckInTime() != null ? u.getCheckInTime() : "N/A");
            ((TextView) row.findViewById(R.id.tv_check_out))
                    .setText(u.getCheckOutTime() != null ? u.getCheckOutTime() : "N/A");

            TextView tvStatus = row.findViewById(R.id.tv_status);
            tvStatus.setText(u.getStatusText());
            switch (u.getStatusType()) {
                case StatusType.PRESENT:
                    tvStatus.setBackgroundResource(R.drawable.bg_badge_present);
                    break;
                case StatusType.LATE:
                    tvStatus.setBackgroundResource(R.drawable.bg_badge_late);
                    break;
                case StatusType.ABSENT:
                    tvStatus.setBackgroundResource(R.drawable.bg_badge_absent);
                    break;
                case StatusType.EARLY_OUT:
                    tvStatus.setBackgroundResource(R.drawable.bg_badge_earlyout);
                    break;
            }
            llUserList.addView(row);
        }

        tvPageIndicator.setText((vm.getCurrentPage() + 1) + "/" + totalPages);
        btnPrevPage.setEnabled(vm.getCurrentPage() > 0);
        btnNextPage.setEnabled(vm.getCurrentPage() < totalPages - 1);
    }

    private void highlightFilter() {
        Integer f = vm.getFilterStatusType();
        cardPresent.setAlpha(1f);
        cardLate.setAlpha(1f);
        cardAbsent.setAlpha(1f);
        if (f == null) return;

        float active = 1f, inactive = 0.35f;
        switch (f) {
            case StatusType.PRESENT:
                cardPresent.setAlpha(active);
                cardLate.setAlpha(inactive);
                cardAbsent.setAlpha(inactive);
                break;
            case StatusType.LATE:
                cardPresent.setAlpha(inactive);
                cardLate.setAlpha(active);
                cardAbsent.setAlpha(inactive);
                break;
            case StatusType.ABSENT:
                cardPresent.setAlpha(inactive);
                cardLate.setAlpha(inactive);
                cardAbsent.setAlpha(active);
                break;
            default:
                break;
        }
    }
}
