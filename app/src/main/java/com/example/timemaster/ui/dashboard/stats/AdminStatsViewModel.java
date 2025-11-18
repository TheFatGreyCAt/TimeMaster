package com.example.timemaster.ui.dashboard.stats;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

import com.example.timemaster.data.repository.AttendanceRepository;
import com.example.timemaster.data.model.DayAttendance;
import com.example.timemaster.data.model.StatusType;
import com.example.timemaster.data.model.UserAttendance;
import com.example.timemaster.data.model.WeekAttendance;

import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * ViewModel lưu toàn bộ state của màn Admin Stats:
 * - weeks (4 tuần gần nhất)
 * - currentWeek/currentDay/currentPage
 * - searchQuery + statusFilter
 * - pageSize (mặc định 5)
 */
public class AdminStatsViewModel extends ViewModel {

    // ==== Consts / format ====
    private static final Locale VI = new Locale("vi", "VN");
    private static final SimpleDateFormat DF = new SimpleDateFormat("dd/MM/yyyy", VI);
    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    // ==== Data source ====
    private final AttendanceRepository repo;

    // ==== State ====
    private List<WeekAttendance> weeks = new ArrayList<>();
    private int currentWeek = 0;   // 0: tuần hiện tại; 1..3: tuần trước
    private int currentDay  = 0;   // 0..6: Thứ 2..CN
    private int currentPage = 0;   // phân trang list theo ngày
    private int pageSize    = 5;

    private String searchQuery = "";
    /** null = không lọc, nếu != null: lọc theo StatusType.PRESENT/LATE/ABSENT/EARLY_OUT */
    private Integer statusFilter = null;

    public AdminStatsViewModel(@NonNull AttendanceRepository repo) {
        this.repo = repo;
        loadWeeks();
    }

    public void loadWeeks() {
        weeks = repo.getLast4Weeks();
        currentWeek = 0;
        currentDay = 0;
        currentPage = 0;
    }

    // ===== Navigate week =====
    public boolean canGoPrevWeek() { return currentWeek < weeks.size() - 1; }
    public boolean canGoNextWeek() { return currentWeek > 0; }

    public void goPrevWeek() {
        if (canGoPrevWeek()) {
            currentWeek++;
            resetDayAndPage();
        }
    }

    public void goNextWeek() {
        if (canGoNextWeek()) {
            currentWeek--;
            resetDayAndPage();
        }
    }

    private void resetDayAndPage() {
        currentDay = 0;
        currentPage = 0;
        // giữ nguyên search/statusFilter
    }

    // ===== Day / Page =====
    public DayAttendance getCurrentDay() { return weeks.get(currentWeek).days.get(currentDay); }

    /** Danh sách sau khi áp dụng search + statusFilter */
    public List<UserAttendance> getFilteredListForCurrentDay() {
        List<UserAttendance> src = getCurrentDay().attendances != null ? getCurrentDay().attendances : new ArrayList<>();
        if ((searchQuery == null || searchQuery.isEmpty()) && statusFilter == null) {
            return src;
        }
        String q = normalizeVN(searchQuery);
        List<UserAttendance> out = new ArrayList<>();
        for (UserAttendance u : src) {
            if (u == null) continue;
            boolean okByName = (q.isEmpty()) || normalizeVN(u.getName()).contains(q);
            boolean okByStatus = (statusFilter == null) || u.getStatusType() == statusFilter;
            if (okByName && okByStatus) out.add(u);
        }
        return out;
    }

    public int getTotalPagesForCurrentDay() {
        List<UserAttendance> list = getFilteredListForCurrentDay();
        if (list.isEmpty()) return 0;
        return (list.size() + pageSize - 1) / pageSize;
    }

    public List<UserAttendance> getCurrentPageItems() {
        List<UserAttendance> list = getFilteredListForCurrentDay();
        if (list.isEmpty()) return list;
        int total = getTotalPagesForCurrentDay();
        if (currentPage > total - 1) currentPage = Math.max(0, total - 1);
        int from = currentPage * pageSize;
        int to = Math.min(from + pageSize, list.size());
        return list.subList(from, to);
    }

    public void nextPage() {
        int total = getTotalPagesForCurrentDay();
        if (currentPage < total - 1) currentPage++;
    }

    public void prevPage() {
        if (currentPage > 0) currentPage--;
    }

    // ===== Counters / Charts for week =====
    public int[] getWeeklyCounts() {
        // [present, late, absent] (3 ô đếm)
        int present = 0, late = 0, absent = 0;
        WeekAttendance w = weeks.get(currentWeek);
        for (DayAttendance d : w.days) {
            if (d.attendances == null) continue;
            for (UserAttendance u : d.attendances) {
                if (u == null) continue;
                switch (u.getStatusType()) {
                    case StatusType.PRESENT: present++; break;
                    case StatusType.LATE:    late++;    break;
                    case StatusType.ABSENT:  absent++;  break;
                    default: /* EARLY_OUT */ break;     // không đếm trong 3 ô
                }
            }
        }
        return new int[]{present, late, absent};
    }

    public int[] getWeeklyChartCounts() {
        // [present, late, early, absent] cho PieChart
        int present = 0, late = 0, early = 0, absent = 0;
        WeekAttendance w = weeks.get(currentWeek);
        for (DayAttendance d : w.days) {
            if (d.attendances == null) continue;
            for (UserAttendance u : d.attendances) {
                if (u == null) continue;
                switch (u.getStatusType()) {
                    case StatusType.PRESENT:   present++; break;
                    case StatusType.LATE:      late++;    break;
                    case StatusType.EARLY_OUT: early++;   break;
                    case StatusType.ABSENT:    absent++;  break;
                }
            }
        }
        return new int[]{present, late, early, absent};
    }

    public String getWeekLabel() {
        WeekAttendance w = weeks.get(currentWeek);
        return DF.format(w.days.get(0).date) + " - " + DF.format(w.days.get(6).date);
    }

    // ===== Search / Filter =====
    public void setSearchQuery(String q) {
        searchQuery = (q == null) ? "" : q.trim();
        currentPage = 0;
    }

    public String getSearchQuery() { return searchQuery; }

    /** Toggle filter theo status: nếu đang filter cùng loại -> bỏ lọc */
    public void toggleStatusFilter(int statusType) {
        if (statusFilter != null && statusFilter == statusType) {
            statusFilter = null;  // bỏ lọc
        } else {
            statusFilter = statusType;
        }
        currentPage = 0;
    }

    public Integer getStatusFilter() { return statusFilter; }

    // ===== Helpers =====
    private static String normalizeVN(String s) {
        if (s == null) return "";
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        temp = DIACRITICS.matcher(temp).replaceAll("");
        temp = temp.replace('đ', 'd').replace('Đ', 'D');
        return temp.toLowerCase(Locale.ROOT).trim();
    }

    // ===== Expose for Fragment =====
    public int getCurrentWeekIndex() { return currentWeek; }
    public int getWeeksCount() { return weeks.size(); }
    public int getCurrentDayIndex() { return currentDay; }
    public void setCurrentDayIndex(int index0to6) {
        if (index0to6 < 0 || index0to6 > 6) return;
        currentDay = index0to6;
        currentPage = 0;
    }
    public int getCurrentPage() { return currentPage; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) {
        if (pageSize > 0) this.pageSize = pageSize;
        currentPage = 0;
    }
}
