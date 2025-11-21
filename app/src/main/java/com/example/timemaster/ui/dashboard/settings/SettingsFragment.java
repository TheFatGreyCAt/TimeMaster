package com.example.timemaster.ui.dashboard.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.timemaster.R;
import com.example.timemaster.ui.auth.facerecognition.RegisterFaceActivity;
import com.example.timemaster.ui.auth.login.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsFragment extends Fragment {

    private FirebaseUser currentUser;
    private DocumentReference userRef;

    private TextView tvProfileName, tvProfileRole;
    private CircleImageView profileImage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_settings, container, false);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userRef = FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid());
        }

        tvProfileName = view.findViewById(R.id.tv_profile_name);
        tvProfileRole = view.findViewById(R.id.tv_profile_role);
        profileImage = view.findViewById(R.id.profile_image);

        loadUserProfile();

        view.findViewById(R.id.row_register_face).setOnClickListener(v -> {
            if (getActivity() != null) {
                startActivity(new Intent(getActivity(), RegisterFaceActivity.class));
            }
        });

        view.findViewById(R.id.btn_logout).setOnClickListener(v -> {
            if (getActivity() != null) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                getActivity().finish();
            }
        });

        return view;
    }

    private void loadUserProfile() {
        if (userRef != null) {
            userRef.get().addOnCompleteListener(task -> {
                if (getContext() == null) return; // Prevent crash if fragment is detached
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        String name = document.getString("displayName");
                        String role = document.getString("role");
                        tvProfileName.setText(name != null ? name : "N/A");
                        tvProfileRole.setText(role != null ? role.toUpperCase() : "");
                        // TODO: Load profile image using a library like Glide or Picasso
                    } else {
                        tvProfileName.setText("User not found");
                    }
                } else {
                    Toast.makeText(getContext(), "Failed to load profile.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
