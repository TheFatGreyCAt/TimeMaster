package com.example.timemaster.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.timemaster.R;
// Import các Fragment con
import com.example.timemaster.ui.dashboard.admin.ManagementFragment;
import com.example.timemaster.ui.dashboard.settings.SettingsFragment;
import com.example.timemaster.ui.dashboard.stats.AdminStatsFragment; // Đảm bảo bạn đã tạo file này
import com.example.timemaster.ui.dashboard.stats.UserStatsFragment; // Đây là file Biểu đồ tròn
import com.example.timemaster.ui.dashboard.user.UserStatusFragment; // Đây là file nút Check-in
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class DashboardFragment extends Fragment {

    private static final String ARG_ROLE = "arg_role";

    public enum Role {
        ADMIN, USER
    }

    private Role currentRole = Role.USER;
    private BottomNavigationView bottomNav;

    // Hàm khởi tạo để nhận Role từ LoginActivity
    public static DashboardFragment newInstance(Role role) {
        DashboardFragment fragment = new DashboardFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ROLE, role.name());
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Đảm bảo layout fragment_dashboard.xml có FrameLayout id là dashboard_content
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Lấy Role từ Bundle gửi sang
        if (getArguments() != null) {
            String roleString = getArguments().getString(ARG_ROLE, Role.USER.name());
            currentRole = Role.valueOf(roleString);
        }

        bottomNav = view.findViewById(R.id.bottom_nav);

        // 2. Setup Menu và Tab mặc định dựa trên Role
        setupBottomNavForRole();

        // 3. Xử lý sự kiện click menu
        bottomNav.setOnItemSelectedListener(item -> {
            switchChildFragment(item.getItemId());
            return true;
        });
    }

    private void setupBottomNavForRole() {
        bottomNav.getMenu().clear(); // Xóa menu cũ

        if (currentRole == Role.ADMIN) {
            // Nếu là Admin -> Load menu Admin & chọn tab Quản lý đầu tiên
            bottomNav.inflateMenu(R.menu.menu_admin_bottom);
            bottomNav.setSelectedItemId(R.id.nav_manage);
            switchChildFragment(R.id.nav_manage);
        } else {
            // Nếu là User -> Load menu User & chọn tab Status (Check-in) đầu tiên
            bottomNav.inflateMenu(R.menu.menu_user_bottom);
            bottomNav.setSelectedItemId(R.id.nav_status);
            switchChildFragment(R.id.nav_status);
        }
    }

    // === ĐÂY LÀ PHẦN BẠN YÊU CẦU ===
    private void switchChildFragment(int itemId) {
        Fragment child = null;

        if (currentRole == Role.ADMIN) {
            // --- MENU CỦA ADMIN ---
            if (itemId == R.id.nav_manage) {
                child = new ManagementFragment(); // Trang quản lý nhân viên
            }
            else if (itemId == R.id.nav_stats) {
                child = new AdminStatsFragment(); // Trang thống kê tổng (Admin)
            }
            else if (itemId == R.id.nav_settings) {
                child = new SettingsFragment(); // Trang cài đặt
            }
        } else {
            // --- MENU CỦA USER ---
            if (itemId == R.id.nav_status) {
                // Tab 1: Trang chủ (Nút Check-in to)
                child = new UserStatusFragment();
            }
            else if (itemId == R.id.nav_stats) {
                // Tab 2: Trang Thống kê (Biểu đồ tròn + Lịch sử)
                // Đây chính là file UserStatsFragment chứa PieChart bạn vừa làm
                child = new UserStatsFragment();
            }
            else if (itemId == R.id.nav_settings) {
                // Tab 3: Trang cài đặt
                child = new SettingsFragment();
            }
        }

        // Thực hiện chuyển đổi Fragment
        if (child != null) {
            getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.dashboard_content, child) // ID của FrameLayout trong XML
                    .commit();
        }
    }
}