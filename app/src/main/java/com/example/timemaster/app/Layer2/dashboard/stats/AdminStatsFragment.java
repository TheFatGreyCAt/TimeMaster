//package com.example.timemaster.app.Layer2.dashboard.stats;
//
//import android.app.DatePickerDialog;
//import android.graphics.Color;
//import android.os.Bundle;
//import android.text.Editable;
//import java.text.Normalizer;
//import android.text.TextWatcher;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.EditText;
//import android.widget.ImageButton;
//import android.widget.LinearLayout;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//
//import com.example.timemaster.R;
////import com.example.timemaster.model.data.AttendanceRepository;
//import com.example.timemaster.model.model_admin.DayAttendance;
//import com.example.timemaster.model.model_admin.StatusType;
//import com.example.timemaster.model.model_admin.UserAttendance;
//import com.example.timemaster.model.model_admin.WeekAttendance;
//import com.github.mikephil.charting.charts.PieChart;
//import com.github.mikephil.charting.data.PieData;
//import com.github.mikephil.charting.data.PieDataSet;
//import com.github.mikephil.charting.data.PieEntry;
//import com.github.mikephil.charting.formatter.ValueFormatter;
//
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Calendar;
//import java.util.List;
//import java.util.Locale;
//import java.util.regex.Pattern;
//
//public class AdminStatsFragment extends Fragment {
//
//    //region ===== Constants & Formatters =====
//    private static final int PAGE_SIZE = 5;
//    private static final Locale VI = new Locale("vi","VN");
//    private static final SimpleDateFormat DF = new SimpleDateFormat("dd/MM/yyyy", VI);
//    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
//
//    // Filter modes for status box toggle
//    private static final int FILTER_ALL     = -1;
//    private static final int FILTER_PRESENT = StatusType.PRESENT;
//    private static final int FILTER_LATE    = StatusType.LATE;
//    private static final int FILTER_ABSENT  = StatusType.ABSENT;
//    //endregion
//
//    //region ===== UI refs =====
//    private LinearLayout layoutPickDate;
//    private TextView tvSelectedDate, tvWeekLabel;
//
//    private ImageButton btnPrevWeek, btnNextWeek;
//    private ImageButton btnPrevPage, btnNextPage;
//
//    private EditText edSearch;
//
//    private PieChart pieChart;
//
//    private LinearLayout llUserList;
//    private TextView tvPageIndicator, tvPresentCount, tvLateCount, tvAbsentCount;
//
//    private SyncedHorizontalScrollView headerScroll, listScroll;
//
//    // Status chips (boxes)
//    private View cardPresent, cardLate, cardAbsent;
//    //endregion
//
//    //region ===== State =====
//    private final Calendar selectedDate = Calendar.getInstance();
//    private List<WeekAttendance> weeks = new ArrayList<>();
//    private int currentWeek = 0;   // 0 = tuần hiện tại; 1..3 tuần trước
//    private int currentDay  = 0;   // 0..6 = T2..CN
//    private int currentPage = 0;
//    private String currentQuery = "";
//
//    // status filter toggle; default show all
//    private int currentStatusFilter = FILTER_ALL;
//    //endregion
//
//    //region ===== Lifecycle =====
//    @Nullable @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        View v = inflater.inflate(R.layout.frag_admin_stats, container, false);
//
//        bindViews(v);
//        wireInteractions();
//
//        // pair scroll
//        headerScroll.setPartner(listScroll);
//        listScroll.setPartner(headerScroll);
//
//        // initial date label
//        tvSelectedDate.setText(DF.format(selectedDate.getTime()));
//
//        // load demo weeks (repo local – sau này thay Firestore)
//        weeks = new AttendanceRepository().getLast4Weeks();
//
//        // restore state (if any)
//        if (savedInstanceState != null) restoreInstanceState(savedInstanceState);
//
//        renderWeek();
//        updateStatusChipUI(); // reflect currentStatusFilter
//
//        return v;
//    }
//
//    @Override
//    public void onSaveInstanceState(@NonNull Bundle out) {
//        super.onSaveInstanceState(out);
//        out.putInt("cur_week", currentWeek);
//        out.putInt("cur_day", currentDay);
//        out.putInt("cur_page", currentPage);
//        out.putInt("cur_filter", currentStatusFilter);
//        out.putString("query", currentQuery);
//        out.putLong("sel_date", selectedDate.getTimeInMillis());
//    }
//    //endregion
//
//    //region ===== View Binding & Listeners =====
//    private void bindViews(View v) {
//        layoutPickDate  = v.findViewById(R.id.layout_pick_date);
//        tvSelectedDate  = v.findViewById(R.id.tv_selected_date);
//        tvWeekLabel     = v.findViewById(R.id.tv_week_label);
//
//        btnPrevWeek     = v.findViewById(R.id.btn_prev_week);
//        btnNextWeek     = v.findViewById(R.id.btn_next_week);
//        btnPrevPage     = v.findViewById(R.id.btn_prev_page);
//        btnNextPage     = v.findViewById(R.id.btn_next_page);
//
//        edSearch        = v.findViewById(R.id.ed_search);
//
//        pieChart        = v.findViewById(R.id.pieChart);
//
//        llUserList      = v.findViewById(R.id.ll_user_list);
//        tvPageIndicator = v.findViewById(R.id.tv_page_indicator);
//
//        tvPresentCount  = v.findViewById(R.id.tv_present_count);
//        tvLateCount     = v.findViewById(R.id.tv_late_count);
//        tvAbsentCount   = v.findViewById(R.id.tv_absent_count);
//
//        headerScroll    = v.findViewById(R.id.header_scroll);
//        listScroll      = v.findViewById(R.id.list_scroll);
//
//        // 3 box trạng thái (LinearLayout)
//        cardPresent     = v.findViewById(R.id.card_present);
//        cardLate        = v.findViewById(R.id.card_late);
//        cardAbsent      = v.findViewById(R.id.card_absent);
//    }
//
//    private void wireInteractions() {
//        layoutPickDate.setOnClickListener(v -> openDatePicker());
//
//        btnPrevWeek.setOnClickListener(v -> {
//            if (currentWeek < weeks.size() - 1) {
//                currentWeek++; currentDay = 0; currentPage = 0;
//                renderWeek();
//            }
//        });
//        btnNextWeek.setOnClickListener(v -> {
//            if (currentWeek > 0) {
//                currentWeek--; currentDay = 0; currentPage = 0;
//                renderWeek();
//            }
//        });
//
//        btnPrevPage.setOnClickListener(v -> {
//            if (currentPage > 0) { currentPage--; renderDayPage(); }
//        });
//        btnNextPage.setOnClickListener(v -> {
//            int totalPage = getTotalPage(getFilteredToday());
//            if (currentPage < totalPage - 1) { currentPage++; renderDayPage(); }
//        });
//
//        if (edSearch != null) {
//            edSearch.addTextChangedListener(new TextWatcher() {
//                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
//                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
//                    currentQuery = s == null ? "" : s.toString().trim();
//                    currentPage = 0;
//                    renderDayPage();
//                }
//                @Override public void afterTextChanged(Editable s) {}
//            });
//        }
//
//        // === TOGGLE FILTERS ===
//        cardPresent.setOnClickListener(v -> toggleStatusFilter(FILTER_PRESENT));
//        cardLate.setOnClickListener(v -> toggleStatusFilter(FILTER_LATE));
//        cardAbsent.setOnClickListener(v -> toggleStatusFilter(FILTER_ABSENT));
//    }
//    //endregion
//
//    //region ===== Rendering =====
//    private void renderWeek() {
//        if (weeks == null || weeks.isEmpty()) {
//            renderEmptyWeek(); return;
//        }
//        WeekAttendance week = safeGetWeek(currentWeek);
//
//        if (week.days != null && week.days.size() >= 7) {
//            tvWeekLabel.setText(DF.format(week.days.get(0).date) + " - " + DF.format(week.days.get(6).date));
//        } else {
//            tvWeekLabel.setText("Tuần không đủ dữ liệu");
//        }
//
//        updateWeeklyPieChart(week);
//        updateWeeklyCounters(week);
//        renderDayPage();
//
//        btnPrevWeek.setEnabled(currentWeek < weeks.size() - 1);
//        btnNextWeek.setEnabled(currentWeek > 0);
//    }
//
//    private void renderDayPage() {
//        llUserList.removeAllViews();
//
//        List<UserAttendance> filtered = getFilteredToday();
//        if (filtered.isEmpty()) {
//            showEmptyRow(currentQuery.isEmpty() && currentStatusFilter == FILTER_ALL
//                    ? "Không có dữ liệu trong ngày này"
//                    : "Không có nhân viên phù hợp");
//            tvPageIndicator.setText("0/0");
//            btnPrevPage.setEnabled(false);
//            btnNextPage.setEnabled(false);
//            return;
//        }
//
//        int totalPage = getTotalPage(filtered);
//        currentPage = clamp(currentPage, 0, Math.max(totalPage - 1, 0));
//
//        int from = currentPage * PAGE_SIZE;
//        int to   = Math.min(from + PAGE_SIZE, filtered.size());
//        List<UserAttendance> page = filtered.subList(from, to);
//
//        LayoutInflater inflater = LayoutInflater.from(requireContext());
//        for (UserAttendance u : page) {
//            if (u == null) continue;
//            View row = inflater.inflate(R.layout.item_user_status_row, llUserList, false);
//
//            ((TextView) row.findViewById(R.id.tv_name)).setText(u.getName());
//            ((TextView) row.findViewById(R.id.tv_check_in)).setText(u.getCheckInTime() != null ? u.getCheckInTime() : "N/A");
//            ((TextView) row.findViewById(R.id.tv_check_out)).setText(u.getCheckOutTime() != null ? u.getCheckOutTime() : "N/A");
//
//            TextView tvStatus = row.findViewById(R.id.tv_status);
//            tvStatus.setText(u.getStatusText());
//            switch (u.getStatusType()) {
//                case StatusType.PRESENT:   tvStatus.setBackgroundResource(R.drawable.bg_badge_present);  break;
//                case StatusType.LATE:      tvStatus.setBackgroundResource(R.drawable.bg_badge_late);     break;
//                case StatusType.ABSENT:    tvStatus.setBackgroundResource(R.drawable.bg_badge_absent);   break;
//                case StatusType.EARLY_OUT: tvStatus.setBackgroundResource(R.drawable.bg_badge_earlyout); break;
//            }
//            llUserList.addView(row);
//        }
//
//        tvPageIndicator.setText((currentPage + 1) + "/" + totalPage);
//        btnPrevPage.setEnabled(currentPage > 0);
//        btnNextPage.setEnabled(currentPage < totalPage - 1);
//    }
//
//    private void renderEmptyWeek() {
//        tvWeekLabel.setText("Không có dữ liệu tuần");
//        pieChart.clear(); pieChart.invalidate();
//        tvPresentCount.setText("0"); tvLateCount.setText("0"); tvAbsentCount.setText("0");
//
//        llUserList.removeAllViews();
//        showEmptyRow("Không có dữ liệu");
//        tvPageIndicator.setText("0/0");
//
//        btnPrevWeek.setEnabled(false); btnNextWeek.setEnabled(false);
//        btnPrevPage.setEnabled(false); btnNextPage.setEnabled(false);
//    }
//    //endregion
//
//    //region ===== Chart & Counters =====
//    private void updateWeeklyPieChart(WeekAttendance week) {
//        int present = 0, late = 0, absent = 0, early = 0;
//        if (week != null && week.days != null) {
//            for (DayAttendance d : week.days) {
//                if (d == null || d.attendances == null) continue;
//                for (UserAttendance u : d.attendances) {
//                    if (u == null) continue;
//                    switch (u.getStatusType()) {
//                        case StatusType.PRESENT:   present++; break;
//                        case StatusType.LATE:      late++;    break;
//                        case StatusType.ABSENT:    absent++;  break;
//                        case StatusType.EARLY_OUT: early++;   break;
//                    }
//                }
//            }
//        }
//        int total = present + late + absent + early;
//
//        pieChart.setNoDataText("Chưa có dữ liệu tuần này");
//        pieChart.setNoDataTextColor(Color.GRAY);
//        if (total == 0) { pieChart.clear(); pieChart.invalidate(); return; }
//
//        List<PieEntry> entries = new ArrayList<>();
//        entries.add(new PieEntry(present, "Đúng giờ"));
//        entries.add(new PieEntry(late,    "Đi trễ"));
//        entries.add(new PieEntry(early,   "Về sớm"));
//        entries.add(new PieEntry(absent,  "Vắng"));
//
//        PieDataSet set = new PieDataSet(entries, "");
//        set.setColors(
//                Color.parseColor("#43E97B"),
//                Color.parseColor("#FFC542"),
//                Color.parseColor("#0AA2C0"),
//                Color.parseColor("#FF647C")
//        );
//        set.setValueTextSize(14f);
//        set.setValueFormatter(new ValueFormatter() {
//            @Override public String getPieLabel(float value, PieEntry pieEntry) {
//                return String.valueOf((int) value);
//            }
//        });
//
//        PieData data = new PieData(set);
//        pieChart.setData(data);
//        pieChart.getDescription().setEnabled(false);
//        pieChart.getLegend().setEnabled(true);
//        pieChart.setUsePercentValues(false);
//        pieChart.setDrawEntryLabels(true);
//        pieChart.setEntryLabelTextSize(12f);
//        pieChart.setHoleRadius(52f);
//        pieChart.setTransparentCircleRadius(57f);
//        pieChart.setCenterText("Tổng: " + total);
//        pieChart.setCenterTextSize(14f);
//        pieChart.invalidate();
//    }
//
//    private void updateWeeklyCounters(WeekAttendance week) {
//        int present = 0, late = 0, absent = 0;
//        if (week != null && week.days != null) {
//            for (DayAttendance d : week.days) {
//                if (d == null || d.attendances == null) continue;
//                for (UserAttendance u : d.attendances) {
//                    if (u == null) continue;
//                    switch (u.getStatusType()) {
//                        case StatusType.PRESENT: present++; break;
//                        case StatusType.LATE:    late++;    break;
//                        case StatusType.ABSENT:  absent++;  break;
//                    }
//                }
//            }
//        }
//        tvPresentCount.setText(String.valueOf(present));
//        tvLateCount.setText(String.valueOf(late));
//        tvAbsentCount.setText(String.valueOf(absent));
//    }
//    //endregion
//
//    //region ===== Date Picker =====
//    private void openDatePicker() {
//        int y = selectedDate.get(Calendar.YEAR);
//        int m = selectedDate.get(Calendar.MONTH);
//        int d = selectedDate.get(Calendar.DAY_OF_MONTH);
//
//        DatePickerDialog dialog = new DatePickerDialog(requireContext(),
//                (view, year, month, dayOfMonth) -> {
//                    selectedDate.set(year, month, dayOfMonth);
//                    tvSelectedDate.setText(DF.format(selectedDate.getTime()));
//                    // Nếu muốn nhảy tới đúng ngày trong data: goToDate(selectedDate);
//                }, y, m, d);
//        dialog.show();
//    }
//    //endregion
//
//    //region ===== Filtering (name + status) & Utils =====
//    private List<UserAttendance> getFilteredToday() {
//        DayAttendance today = safeGetDay(currentWeek, currentDay);
//        List<UserAttendance> all = (today.attendances != null) ? today.attendances : new ArrayList<>();
//
//        // 1) Lọc theo tên
//        List<UserAttendance> byName = new ArrayList<>();
//        if (currentQuery == null || currentQuery.isEmpty()) {
//            byName = all;
//        } else {
//            for (UserAttendance u : all) {
//                if (u == null) continue;
//                if (containsName(u.getName(), currentQuery)) byName.add(u);
//            }
//        }
//
//        // 2) Lọc theo trạng thái (toggle); FILTER_ALL = không lọc
//        if (currentStatusFilter == FILTER_ALL) return byName;
//
//        List<UserAttendance> byStatus = new ArrayList<>();
//        for (UserAttendance u : byName) {
//            if (u == null) continue;
//            if (u.getStatusType() == currentStatusFilter) byStatus.add(u);
//        }
//        return byStatus;
//    }
//
//    private void toggleStatusFilter(int targetFilter) {
//        // nếu đang bật cùng filter → tắt (về ALL); ngược lại → bật filter đó
//        currentStatusFilter = (currentStatusFilter == targetFilter) ? FILTER_ALL : targetFilter;
//        currentPage = 0;
//        updateStatusChipUI();
//        renderDayPage();
//    }
//
//    private void updateStatusChipUI() {
//        // Hiệu ứng nhẹ: chip đang bật = alpha 1.0; chip khác = 0.85
//        setChipAlpha(cardPresent, currentStatusFilter == FILTER_PRESENT ? 1f : 0.85f);
//        setChipAlpha(cardLate,    currentStatusFilter == FILTER_LATE    ? 1f : 0.85f);
//        setChipAlpha(cardAbsent,  currentStatusFilter == FILTER_ABSENT  ? 1f : 0.85f);
//    }
//
//    private void setChipAlpha(View v, float alpha) {
//        if (v != null) v.setAlpha(alpha);
//    }
//
//    private boolean containsName(String name, String query) {
//        if (query == null || query.isEmpty()) return true;
//        return normalizeVN(name).contains(normalizeVN(query));
//    }
//
//    private String normalizeVN(String s) {
//        if (s == null) return "";
//        String tmp = Normalizer.normalize(s, Normalizer.Form.NFD);
//        tmp = DIACRITICS.matcher(tmp).replaceAll("");
//        tmp = tmp.replace('đ','d').replace('Đ','D');
//        return tmp.toLowerCase(Locale.ROOT).trim();
//    }
//
//    private int getTotalPage(List<UserAttendance> list) {
//        return (list.size() + PAGE_SIZE - 1) / PAGE_SIZE;
//    }
//
//    private int clamp(int v, int min, int max) {
//        return Math.max(min, Math.min(v, max));
//    }
//
//    private WeekAttendance safeGetWeek(int index) {
//        if (weeks == null || weeks.isEmpty()) return new WeekAttendance();
//        int i = clamp(index, 0, weeks.size()-1);
//        WeekAttendance w = weeks.get(i);
//        return w != null ? w : new WeekAttendance();
//    }
//
//    private DayAttendance safeGetDay(int weekIdx, int dayIdx) {
//        WeekAttendance w = safeGetWeek(weekIdx);
//        if (w.days == null || w.days.isEmpty()) return new DayAttendance();
//        int d = clamp(dayIdx, 0, w.days.size()-1);
//        DayAttendance day = w.days.get(d);
//        return day != null ? day : new DayAttendance();
//    }
//
//    private void showEmptyRow(String msg) {
//        TextView empty = new TextView(requireContext());
//        empty.setText(msg);
//        empty.setTextColor(Color.GRAY);
//        empty.setPadding(16,16,16,16);
//        llUserList.addView(empty);
//    }
//
//    private void restoreInstanceState(@NonNull Bundle in) {
//        currentWeek = in.getInt("cur_week", 0);
//        currentDay  = in.getInt("cur_day", 0);
//        currentPage = in.getInt("cur_page", 0);
//        currentStatusFilter = in.getInt("cur_filter", FILTER_ALL);
//        currentQuery = in.getString("query", "");
//        long millis = in.getLong("sel_date", System.currentTimeMillis());
//        selectedDate.setTimeInMillis(millis);
//        tvSelectedDate.setText(DF.format(selectedDate.getTime()));
//        if (edSearch != null) edSearch.setText(currentQuery);
//    }
//
//    // (Tuỳ chọn) tìm và nhảy tới ngày đã chọn trong 4 tuần đang nạp
////    private void goToDate(Calendar target) {
////        if (weeks == null || weeks.isEmpty()) return;
////        SimpleDateFormat key = new SimpleDateFormat("yyyyMMdd", Locale.US);
////        String targetKey = key.format(target.getTime());
////        for (int w = 0; w < weeks.size(); w++) {
////            WeekAttendance wk = weeks.get(w);
////            if (wk == null || wk.days == null) continue;
////            for (int d = 0; d < wk.days.size(); d++) {
////                DayAttendance day = wk.days.get(d);
////                if (day == null || day.date == null) continue;
////                if (key.format(day.date).equals(targetKey)) {
////                    currentWeek = w; currentDay = d; currentPage = 0;
////                    renderWeek(); return;
////                }
////            }
////        }
////    }
//    //endregion
//}
