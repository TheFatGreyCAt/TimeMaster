package com.example.timemaster.ui.dashboard.user;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.timemaster.R;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class UserStatusFragment extends Fragment {

    private TextView tvUsername;
    private MaterialCardView btnCheckIn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // SỬA LẠI DÒNG NÀY CHO KHỚP VỚI TÊN FILE CỦA BẠN
        return inflater.inflate(R.layout.frag_user_status, container, false);
    }

    @SuppressLint("WrongViewCast")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ View (Đảm bảo trong file frag_user_status.xml có các ID này nhé)
        // Nếu file XML của bạn chưa có nội dung, hãy copy đoạn code XML (nút Check-in to) 
        // ở câu trả lời trước vào file frag_user_status.xml

        tvUsername = view.findViewById(R.id.tv_username);
        btnCheckIn = view.findViewById(R.id.tv_check_in);

        // Hiển thị tên user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    }
}