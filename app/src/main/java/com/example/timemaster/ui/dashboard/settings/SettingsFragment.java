package com.example.timemaster.ui.dashboard.settings;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.ExperimentalGetImage;
import androidx.fragment.app.Fragment;

import com.example.timemaster.EditProfileActivity;
import com.example.timemaster.R;
import com.example.timemaster.ui.auth.login.LoginActivity;
import com.example.timemaster.ui.auth.login.FaceRecognitionActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import de.hdodenhof.circleimageview.CircleImageView;

@ExperimentalGetImage
public class SettingsFragment extends Fragment {

    private FirebaseUser currentUser;
    private DocumentReference userRef;

    private TextView tvProfileName, tvProfileRole;
    private CircleImageView profileImage;
    private RelativeLayout rowChangePassword;
    private RelativeLayout rowEditProfile;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_settings, container, false);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userRef = FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid());
        }

        initViews(view);

        loadUserProfile();

        setupClicks(view);

        return view;
    }

    public void initViews(View view) {
        tvProfileName = view.findViewById(R.id.tv_profile_name);
        tvProfileRole = view.findViewById(R.id.tv_profile_role);
        profileImage = view.findViewById(R.id.profile_image);
        rowChangePassword = view.findViewById(R.id.row_change_password);
        rowEditProfile = view.findViewById(R.id.row_edit_profile);
    }

    public void setupClicks(View view) {
        rowEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            startActivity(intent);
        });

        rowChangePassword.setOnClickListener(v -> handleChangePassword());

        view.findViewById(R.id.row_register_face).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), FaceRecognitionActivity.class);
            intent.putExtra("isRegistration", true); // Registration mode - save/update face embedding
            startActivity(intent);
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
    }

    // --- I. PROFILE LOGIC
    private void loadUserProfile() {

        if (currentUser != null) {
            String quickName = currentUser.getDisplayName();
            String quickEmail = currentUser.getEmail();

            if (quickName != null && !quickName.isEmpty()) {
                tvProfileName.setText(quickName);
            } else if (quickEmail != null) {
                tvProfileName.setText(quickEmail); // Tạm thời hiện email nếu chưa có tên
            }
        }

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
    } // check SettingFragment cua Loc

    // --- II. CHANGE PASSWORD LOGIC ---
    private void handleChangePassword() {
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Bạn cần đăng nhập để thực hiện.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = getLayoutInflater();

        View dialogView = inflater.inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);
        builder.setTitle("Đổi mật khẩu");
        // builder.setCancelable(false); // Có thể bỏ dòng này để user bấm ra ngoài là tắt

        TextInputEditText etOldPass = dialogView.findViewById(R.id.et_old_password_dialog);
        TextInputEditText etNewPass = dialogView.findViewById(R.id.et_new_password_dialog);
        TextInputEditText etConfirmPass = dialogView.findViewById(R.id.et_confirm_password_dialog);

        builder.setPositiveButton("Lưu thay đổi", null);
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());

        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        Button btnSave = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        btnSave.setOnClickListener(v -> {
            String oldPass = etOldPass.getText().toString().trim();
            String newPass = etNewPass.getText().toString().trim();
            String confirmPass = etConfirmPass.getText().toString().trim();

            if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }
            if (newPass.length() < 6) {
                Toast.makeText(requireContext(), "Mật khẩu mới quá ngắn", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newPass.equals(confirmPass)) {
                Toast.makeText(requireContext(), "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show();
                return;
            }

            reauthenticateAndChangePassword(oldPass, newPass, alertDialog);
        });
    }

    private void reauthenticateAndChangePassword(String oldPassword, String newPassword, AlertDialog dialog) {
        if (currentUser == null || currentUser.getEmail() == null) return;

        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), oldPassword);

        currentUser.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                currentUser.updatePassword(newPassword).addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        Toast.makeText(requireContext(), "Đổi mật khẩu thành công!", Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(requireContext(), "Lỗi hệ thống, thử lại sau.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(requireContext(), "Mật khẩu cũ không chính xác.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- III. LOGOUT LOGIC ---
    private void handleLogout() {
        if (getActivity() != null) {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        }
    }
}
