package com.example.timemaster.app.Layer2.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.timemaster.R;
import com.example.timemaster.app.Layer2.dashboard.admin.AdminManagerFragment;
import com.example.timemaster.app.Layer2.dashboard.settings.SettingsFragment;
//import com.example.timemaster.app.Layer2.dashboard.stats.AdminStatsFragment;
import com.example.timemaster.app.Layer2.dashboard.stats.UserStatsFragment;
import com.example.timemaster.app.Layer2.dashboard.user.UserStatusFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class DashboardFragment extends Fragment {

    private static final String ARG_ROLE = "arg_role";

    public enum Role {
        ADMIN, USER
    }

    private Role currentRole = Role.USER;
    private BottomNavigationView bottomNav;

    public static DashboardFragment newInstance(Role role) {
        DashboardFragment f = new DashboardFragment();
        Bundle b = new Bundle();
        b.putString(ARG_ROLE, role.name());
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Lấy role từ arguments
        if (getArguments() != null) {
            String r = getArguments().getString(ARG_ROLE, Role.USER.name());
            currentRole = Role.valueOf(r);
        }

        bottomNav = view.findViewById(R.id.bottom_nav);

        setupBottomNavForRole();

        // chọn tab đầu tiên
        if (currentRole == Role.ADMIN) {
            bottomNav.setSelectedItemId(R.id.nav_manage);
            switchChildFragment(R.id.nav_manage);
        } else {
            bottomNav.setSelectedItemId(R.id.nav_status);
            switchChildFragment(R.id.nav_status);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            switchChildFragment(item.getItemId());
            return true;
        });
    }

    private void setupBottomNavForRole() {
        if (currentRole == Role.ADMIN) {
            // menu_admin_bottom.xml
            bottomNav.getMenu().clear();
            bottomNav.inflateMenu(R.menu.menu_admin_bottom);
        } else {
            // menu_user_bottom.xml
            bottomNav.getMenu().clear();
            bottomNav.inflateMenu(R.menu.menu_user_bottom);
        }
    }

    private void switchChildFragment(int itemId) {
        Fragment child;
        if (currentRole == Role.ADMIN) {
            if (itemId == R.id.nav_manage) {
                child = new AdminManagerFragment();
            } else if (itemId == R.id.nav_stats) {
                child = new AdminStatsFragment();
            } else {
                child = new SettingsFragment();
            }
        } else {
            if (itemId == R.id.nav_status) {
                child = new UserStatusFragment();
            } else if (itemId == R.id.nav_stats) {
                child = new UserStatsFragment();
            } else {
                child = new SettingsFragment();
            }
        }

        getChildFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.dashboard_content, child)
                .commit();
    }
}
