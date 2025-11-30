package com.example.timemaster.ui.dashboard.stats;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.timemaster.data.model.DayAttendance;
import com.example.timemaster.data.model.StatusType;
import com.example.timemaster.data.model.UserAttendance;
import com.example.timemaster.data.model.WeekAttendance;
import com.example.timemaster.data.repository.AttendanceRepository;
import com.example.timemaster.data.repository.AttendanceRepositoryFirestore;

import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AdminStatsViewModel extends ViewModel {

    private final AttendanceRepository repository =
            new AttendanceRepositoryFirestore();

    private final MutableLiveData<List<WeekAttendance>> weeksLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading =
            new MutableLiveData<>(false);
    private final MutableLiveData<String> error =
            new MutableLiveData<>(null);

    public LiveData<List<WeekAttendance>> getWeeksLiveData() {
        return weeksLiveData;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    // trạng thái UI hiện tại
    private int currentWeekIndex = 3; // 0..3 (3 = tuần hiện tại)
    private int currentDayIndex = 0;  // 0..6 (Thứ 2..CN)
    private int currentPage = 0;
    private int pageSize = 5;

    private String searchText = "";
    private Integer filterStatusType = null; // null = ALL

    private final SimpleDateFormat WEEK_DF =
            new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN"));

    // =========================
    //  Load dữ liệu từ Firestore
    // =========================
    public void loadData() {
        loading.postValue(true);
        error.postValue(null);

        repository.getLast4Weeks(new AttendanceRepository.Callback() {
            @Override
            public void onSuccess(List<WeekAttendance> weeks) {
                loading.postValue(false);
                if (weeks != null && !weeks.isEmpty()) {
                    // Mặc định là tuần hiện tại
                    currentWeekIndex = weeks.size() - 1;

                    // Mặc định là ngày hôm nay
                    Calendar today = Calendar.getInstance();
                    // DAY_OF_WEEK: CN=1, T2=2, ..., T7=7
                    int dayOfWeek = today.get(Calendar.DAY_OF_WEEK);
                    // Chuyển đổi sang index 0-6 (T2-CN)
                    currentDayIndex = (dayOfWeek == Calendar.SUNDAY) ? 6 : dayOfWeek - 2;

                } else {
                    currentWeekIndex = 0;
                    currentDayIndex = 0;
                }
                weeksLiveData.postValue(weeks);
            }

            @Override
            public void onError(Exception e) {
                loading.postValue(false);
                error.postValue(e.getMessage());
            }
        });
    }

    // ================
    //  Bộ lọc & phân trang
    // ================

    public void setCurrentWeekIndex(int index) {
        List<WeekAttendance> weeks = weeksLiveData.getValue();
        if (weeks == null || weeks.isEmpty()) {
            currentWeekIndex = 0;
        } else {
            if (index < 0) index = 0;
            if (index >= weeks.size()) index = weeks.size() - 1;
            currentWeekIndex = index;
        }
        currentPage = 0;
    }

    public int getCurrentWeekIndex() {
        return currentWeekIndex;
    }

    public void setCurrentDayIndex(int index) {
        currentDayIndex = index;
        currentPage = 0;
    }

    public int getCurrentDayIndex() {
        return currentDayIndex;
    }

    public void setPageSize(int size) {
        if (size <= 0) size = 1;
        this.pageSize = size;
        currentPage = 0;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setSearchText(String text) {
        searchText = normalize(text);
        currentPage = 0;
    }

    public void setFilterStatusType(Integer statusType) {
        filterStatusType = statusType;
        currentPage = 0;
    }

    public Integer getFilterStatusType() {
        return filterStatusType;
    }

    public void nextPage() {
        currentPage++;
    }

    public void prevPage() {
        if (currentPage > 0) currentPage--;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    // =========================
    //  LABEL tuần: "dd/MM/yyyy - dd/MM/yyyy"
    // =========================
    public String getWeekLabel() {
        List<WeekAttendance> weeks = weeksLiveData.getValue();
        if (weeks == null || weeks.isEmpty()) return "";
        if (currentWeekIndex < 0 || currentWeekIndex >= weeks.size()) return "";

        WeekAttendance week = weeks.get(currentWeekIndex);
        if (week.getDays() == null || week.getDays().isEmpty()) return "";

        DayAttendance first = week.getDays().get(0);
        DayAttendance last = week.getDays().get(week.getDays().size() - 1);
        if (first.getDate() == null || last.getDate() == null) return "";

        return WEEK_DF.format(first.getDate()) + " - " + WEEK_DF.format(last.getDate());
    }

    // =========================
    //  COUNTS theo tuần / theo ngày
    // =========================

    /** Đếm toàn bộ tuần hiện tại (không áp dụng search/filter)
     *  return [present, late, earlyOut, absent]
     */
    public int[] getWeeklyCounts() {
        int present = 0, late = 0, early = 0, absent = 0;

        List<WeekAttendance> weeks = weeksLiveData.getValue();
        if (weeks == null || weeks.isEmpty()) return new int[]{0, 0, 0, 0};
        if (currentWeekIndex < 0 || currentWeekIndex >= weeks.size())
            return new int[]{0, 0, 0, 0};

        WeekAttendance week = weeks.get(currentWeekIndex);
        if (week.getDays() == null) return new int[]{0, 0, 0, 0};

        for (DayAttendance day : week.getDays()) {
            if (day.getAttendances() == null) continue;
            for (UserAttendance ua : day.getAttendances()) {
                int st = ua.getStatusType();
                if (st == StatusType.PRESENT) present++;
                else if (st == StatusType.LATE) late++;
                else if (st == StatusType.EARLY_OUT) early++;
                else if (st == StatusType.ABSENT) absent++;
            }
        }
        return new int[]{present, late, early, absent};
    }

    /** Đếm trong NGÀY hiện tại (có áp dụng search + filter)
     *  return [present, late, absent, earlyOut]
     */
    public int[] getCurrentCounts() {
        List<UserAttendance> list = getCurrentDayAllFiltered();
        int present = 0, late = 0, absent = 0, early = 0;

        for (UserAttendance ua : list) {
            int st = ua.getStatusType();
            if (st == StatusType.PRESENT) present++;
            else if (st == StatusType.LATE) late++;
            else if (st == StatusType.ABSENT) absent++;
            else if (st == StatusType.EARLY_OUT) early++;
        }
        return new int[]{present, late, absent, early};
    }

    // =========================
    //  Data cho bảng theo ngày
    // =========================

    public List<UserAttendance> getCurrentPageData() {
        List<UserAttendance> filtered = getCurrentDayAllFiltered();
        if (filtered.isEmpty()) return new ArrayList<>();

        int from = currentPage * pageSize;
        if (from >= filtered.size()) return new ArrayList<>();
        int to = Math.min(from + pageSize, filtered.size());
        return filtered.subList(from, to);
    }

    public int getTotalPagesForCurrentDay() {
        List<UserAttendance> filtered = getCurrentDayAllFiltered();
        if (filtered.isEmpty()) return 0;
        return (filtered.size() + pageSize - 1) / pageSize;
    }

    // =========================
    //  Helpers nội bộ
    // =========================

    private List<UserAttendance> getCurrentDayAllFiltered() {
        List<WeekAttendance> weeks = weeksLiveData.getValue();
        List<UserAttendance> result = new ArrayList<>();
        if (weeks == null || weeks.isEmpty()) return result;

        if (currentWeekIndex < 0 ||
                currentWeekIndex >= weeks.size()) return result;
        WeekAttendance week = weeks.get(currentWeekIndex);

        if (week.getDays() == null ||
                currentDayIndex < 0 ||
                currentDayIndex >= week.getDays().size()) return result;
        DayAttendance day = week.getDays().get(currentDayIndex);

        if (day.getAttendances() == null) return result;

        for (UserAttendance ua : day.getAttendances()) {
            if (!matchSearch(ua)) continue;
            if (!matchStatus(ua)) continue;
            result.add(ua);
        }

        // Sắp xếp theo thời gian check-in giảm dần (người mới nhất lên đầu)
        result.sort((u1, u2) -> Long.compare(u2.getCheckInTimestamp(), u1.getCheckInTimestamp()));

        return result;
    }

    private boolean matchSearch(UserAttendance ua) {
        if (searchText == null || searchText.isEmpty()) return true;
        String name = normalize(ua.getName());
        return name.contains(searchText);
    }

    private boolean matchStatus(UserAttendance ua) {
        if (filterStatusType == null) return true;
        return ua.getStatusType() == filterStatusType;
    }

    private String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD);
        n = n.replaceAll("\\p{M}", "");
        return n.toLowerCase(Locale.ROOT);
    }
}
