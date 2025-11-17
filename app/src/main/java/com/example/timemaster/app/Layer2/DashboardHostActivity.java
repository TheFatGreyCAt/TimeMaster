package com.example.timemaster.app.Layer2;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.timemaster.R;
import com.example.timemaster.app.Layer2.dashboard.DashboardFragment;

public class DashboardHostActivity extends AppCompatActivity {

    public static final String EXTRA_ROLE = "extra_role"; // "admin" | "user"

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_host); // layout bên dưới

        if (savedInstanceState == null) {
            String roleStr = getIntent().getStringExtra(EXTRA_ROLE);
            DashboardFragment.Role role = "admin".equalsIgnoreCase(roleStr)
                    ? DashboardFragment.Role.ADMIN
                    : DashboardFragment.Role.USER;

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.host_container, DashboardFragment.newInstance(role))
                    .commit();
        }
    }
}
