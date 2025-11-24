package com.example.timemaster.ui.dashboard.stats;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timemaster.R;
import com.example.timemaster.model.AttendanceHistory;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class UserStatsFragment extends Fragment {

    private PieChart pieChart;
    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private List<AttendanceHistory> historyList;

    // UI Profile
    private TextView tvName, tvHandle;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Đảm bảo file layout tên là fragment_user_stats (hoặc tên bạn đã đặt cho cái XML số 1)
        return inflater.inflate(R.layout.frag_user_status, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 0. Setup Profile (Khớp ID với XML số 1)
        tvName = view.findViewById(R.id.tv_user_name);     // Sửa ID cho khớp
        tvHandle = view.findViewById(R.id.tv_user_handle); // Sửa ID cho khớp
        loadUserProfile();

        // 1. Setup Biểu đồ
        pieChart = view.findViewById(R.id.pieChart);
        setupPieChart();
        loadChartData();

        // 2. Setup Danh sách
        recyclerView = view.findViewById(R.id.recycler_history);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Data giả
        historyList = new ArrayList<>();
        historyList.add(new AttendanceHistory("08:30", "23/10/2023", "Đúng giờ"));
        historyList.add(new AttendanceHistory("08:45", "22/10/2023", "Đi trễ"));
        historyList.add(new AttendanceHistory("17:00", "21/10/2023", "Về sớm"));
        historyList.add(new AttendanceHistory("08:25", "20/10/2023", "Đúng giờ"));
        historyList.add(new AttendanceHistory("09:00", "19/10/2023", "Vắng mặt"));

        adapter = new HistoryAdapter(historyList);
        recyclerView.setAdapter(adapter);
    }

    private void loadUserProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String name = user.getDisplayName();
            String email = user.getEmail();

            tvName.setText(name != null ? name : "Người dùng");
            tvHandle.setText(email != null ? "@" + email.split("@")[0] : "@user");
        }
    }

    private void setupPieChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(false);
        pieChart.getLegend().setEnabled(false);
        pieChart.setTouchEnabled(false);
        pieChart.animateY(1000);
    }

    private void loadChartData() {
        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(0.7f, ""));
        entries.add(new PieEntry(0.1f, ""));
        entries.add(new PieEntry(0.2f, ""));

        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.parseColor("#4DB6AC"));
        colors.add(Color.parseColor("#80CBC4"));
        colors.add(Color.parseColor("#B2DFDB"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setSliceSpace(2f);

        PieData data = new PieData(dataSet);
        data.setDrawValues(false);

        pieChart.setData(data);
        pieChart.invalidate();
    }
}