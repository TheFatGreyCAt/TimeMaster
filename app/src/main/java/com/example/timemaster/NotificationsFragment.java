package com.example.timemaster;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment {

    private RecyclerView rcvNotifications;
    private NotificationAdapter adapter;
    private ImageView btnBack;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        rcvNotifications = view.findViewById(R.id.rcv_notifications);
        btnBack = view.findViewById(R.id.btn_back_notification);

        // Nút Back
        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });

        // Setup List
        List<String> list = new ArrayList<>();
        list.add("Nguyễn Văn A đã cập nhật số điện thoại.");
        list.add("Hệ thống: Server bảo trì lúc 00:00.");
        list.add("Trần Thị B vừa gửi yêu cầu nghỉ phép.");

        adapter = new NotificationAdapter(list);
        rcvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        rcvNotifications.setAdapter(adapter);

        return view;
    }
}