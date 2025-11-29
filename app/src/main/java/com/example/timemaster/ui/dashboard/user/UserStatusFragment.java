package com.example.timemaster.ui.dashboard.user;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.ExperimentalGetImage;
import androidx.fragment.app.Fragment;

import com.example.timemaster.R;
import com.example.timemaster.ui.auth.login.FaceRecognitionActivity;

public class UserStatusFragment extends Fragment {

    @ExperimentalGetImage
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_user_status, container, false);

        // Face recognition attendance button
        view.findViewById(R.id.btnFace).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), FaceRecognitionActivity.class);
            intent.putExtra("isRegistration", false); // Attendance mode - compare with stored embeddings
            startActivity(intent);
        });

        // TODO: Add fingerprint button handler if needed
        // view.findViewById(R.id.btnFinger).setOnClickListener(v -> { ... });

        return view;
    }
}
