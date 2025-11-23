package com.example.timemaster.ui.dashboard.settings;

import android.app.AlertDialog;
import android.content.Intent;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.timemaster.R;
import com.example.timemaster.ui.auth.facerecognition.RegisterFaceActivity;
import com.example.timemaster.ui.auth.fingerprint.FingerprintPrefs;
import com.example.timemaster.ui.auth.login.LoginActivity;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsFragment extends Fragment {

    private FirebaseUser currentUser;
    private DocumentReference userRef;

    private TextView tvProfileName, tvProfileRole;
    private ImageView profileImage;
    private TextInputEditText etUsername;

    private RelativeLayout rowChangePassword;
    private SwitchMaterial switchFingerprint;
    private SwitchMaterial switchFace;

    private FingerprintPrefs fingerprintPrefs;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Giả định layout XML là frag_settings
        View view = inflater.inflate(R.layout.frag_settings, container, false);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userRef = FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid());
        }

        // 1. Khởi tạo Component
        tvProfileName = view.findViewById(R.id.tv_profile_name);
        tvProfileRole = view.findViewById(R.id.tv_profile_role);
        profileImage = view.findViewById(R.id.profile_image);

        etUsername = view.findViewById(R.id.et_username);

        rowChangePassword = view.findViewById(R.id.row_change_password);
        switchFingerprint = view.findViewById(R.id.row_register_fingerprint);
        switchFace = view.findViewById(R.id.row_register_face);


        loadUserProfile();
        updateBiometricSwitches();

        // 2. Xử lý Sự kiện

        // 2.1. Đăng xuất
        view.findViewById(R.id.btn_logout).setOnClickListener(v -> handleLogout());

        // 2.2. Đổi Tên / Lưu thay đổi
        view.findViewById(R.id.btn_save).setOnClickListener(v -> handleSaveName());

        // 2.3. Đổi Mật khẩu
        rowChangePassword.setOnClickListener(v -> handleChangePassword());

        // 2.4. Đăng ký/Hủy Vân tay
        fingerprintPrefs = new FingerprintPrefs(requireContext());
        switchFingerprint.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                startFingerprintRegistration();
            } else {
                fingerprintPrefs.clear();
                Toast.makeText(requireContext(), "Đã hủy đăng ký vân tay.", Toast.LENGTH_SHORT).show();
            }
        });

        // 2.5. Đăng ký/Hủy Khuôn mặt
        switchFace.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Chuyển đến màn hình đăng ký khuôn mặt
                startActivity(new Intent(requireActivity(), RegisterFaceActivity.class));
            } else {
                // TODO: Logic hủy đăng ký khuôn mặt (Nếu có)
                Toast.makeText(requireContext(), "Đã hủy đăng ký khuôn mặt.", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    // I. LOGIC PROFILE
    private void loadUserProfile() {
        if (userRef != null) {
            userRef.get().addOnCompleteListener(task -> {
                if (getContext() == null) return;
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        String name = document.getString("displayName");
                        String role = document.getString("role");

                        tvProfileName.setText(name != null ? name : "N/A");
                        tvProfileRole.setText(role != null ? role.toUpperCase() : "");
                        etUsername.setText(name != null ? name : "");

                        // TODO: Load profile image
                    } else {
                        tvProfileName.setText("User not found");
                    }
                } else {
                    Toast.makeText(getContext(), "Failed to load profile.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateBiometricSwitches() {
        if (fingerprintPrefs == null) {
            fingerprintPrefs = new FingerprintPrefs(requireContext());
        }
        boolean isFingerprintRegistered = fingerprintPrefs.isEnabled();
        switchFingerprint.setChecked(isFingerprintRegistered);

        // Cần thêm logic kiểm tra đăng ký khuôn mặt
    }


    // II. LOGIC CHỨC NĂNG CƠ BẢN
    private void handleLogout() {
        if (getActivity() != null) {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        }
    }

    private void handleSaveName() {
        String newName = etUsername.getText().toString().trim();
        if (newName.isEmpty()) {
            Toast.makeText(requireContext(), "Tên không được để trống.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userRef != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("displayName", newName);

            userRef.update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(requireContext(), "Cập nhật tên thành công!", Toast.LENGTH_SHORT).show();
                        tvProfileName.setText(newName);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "Lỗi khi cập nhật tên.", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void handleChangePassword() {
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Bạn cần đăng nhập để đổi mật khẩu.", Toast.LENGTH_SHORT).show();
            return;
        }

        EditText inputOld = new EditText(requireContext());
        inputOld.setHint("Mật khẩu cũ");
        inputOld.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        EditText inputNew = new EditText(requireContext());
        inputNew.setHint("Mật khẩu mới");
        inputNew.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(requireContext())
                .setTitle("Đổi mật khẩu")
                .setView(inputOld)
                .setNeutralButton("Mật khẩu mới", (dialog, which) -> {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Nhập mật khẩu mới")
                            .setView(inputNew)
                            .setPositiveButton("Đổi", (d, w) -> {
                                String oldPassword = inputOld.getText().toString().trim();
                                String newPassword = inputNew.getText().toString().trim();

                                if (oldPassword.isEmpty() || newPassword.isEmpty() || newPassword.length() < 6) {
                                    Toast.makeText(requireContext(), "Mật khẩu không hợp lệ.", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                reauthenticateAndChangePassword(oldPassword, newPassword);
                            })
                            .setNegativeButton("Hủy", null)
                            .show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void reauthenticateAndChangePassword(String oldPassword, String newPassword) {
        if (currentUser == null || currentUser.getEmail() == null) {
            Toast.makeText(requireContext(), "Lỗi: Tài khoản không hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), oldPassword);

        currentUser.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        currentUser.updatePassword(newPassword)
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        Toast.makeText(requireContext(), "Đổi mật khẩu thành công!", Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(requireContext(), "Lỗi khi đổi mật khẩu mới.", Toast.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        Toast.makeText(requireContext(), "Mật khẩu cũ không chính xác.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    // III. LOGIC VÂN TAY (Biometric)
    private void startFingerprintRegistration() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(requireContext(), "Bạn cần đăng nhập trước đi đăng kí vân tay", Toast.LENGTH_SHORT).show();
            switchFingerprint.setChecked(false);
            return;
        }

        BiometricManager biometricManager = BiometricManager.from(requireContext());
        int canAuth = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL);

        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(requireContext(), "Không hỗ trợ đăng kí vân tay", Toast.LENGTH_SHORT).show();
            switchFingerprint.setChecked(false);
            return;
        }

        String provider = detectProvider(user);

        if ("password".equals(provider)) {
            askPasswordThenShowBiometric(user);
        }
        else if ("google".equals(provider)) {
            showBiometricForGoogle(user);
        }
        else {
            Toast.makeText(requireContext(), "Kiểu đăng nhập không hợp lệ", Toast.LENGTH_SHORT).show();
            switchFingerprint.setChecked(false);
        }
    }

    private String detectProvider(FirebaseUser user) {
        String provider = null;
        for (UserInfo info : user.getProviderData()) {
            String pid = info.getProviderId();
            if ("password".equals(pid)) {
                provider = "password";
                break;
            }

            if ("google.com".equals(pid)) {
                provider = "google";
            }
        }
        return provider;
    }

    private void askPasswordThenShowBiometric(FirebaseUser user) {
        if (user.getEmail() == null) {
            Toast.makeText(requireContext(), "Email không hợp lệ", Toast.LENGTH_SHORT).show();
            switchFingerprint.setChecked(false);
            return;
        }

        EditText inp = new EditText(requireContext());
        inp.setHint("Nhập lại mật khẩu");
        inp.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận mật khẩu")
                .setMessage("Nhập lại mật khẩu")
                .setView(inp)
                .setPositiveButton("Tiếp tục", (dialog, which) -> {
                    String password = inp.getText().toString().trim();
                    if (password.isEmpty()) {
                        Toast.makeText(requireContext(), "Mật khẩu không được để trống", Toast.LENGTH_SHORT).show();
                        switchFingerprint.setChecked(false);
                        return;
                    }
                    showBiometricForPasswordUser(user, password);
                })
                .setNegativeButton("Hủy", (dialog, which) -> switchFingerprint.setChecked(false))
                .show();
    }

    private void showBiometricForPasswordUser(FirebaseUser user, String passwordPlain) {
        Executor executor = ContextCompat.getMainExecutor(requireContext());

        BiometricPrompt.AuthenticationCallback callback = new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                fingerprintPrefs.savePasswordUser(user, user.getEmail(), passwordPlain);
                Toast.makeText(requireContext(), "Đăng kí vân tay thành công", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(requireContext(), "Lỗi xác thực: " + errString, Toast.LENGTH_SHORT).show();
                switchFingerprint.setChecked(false);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(requireContext(), "Vân tay không khớp, thử lại", Toast.LENGTH_SHORT).show();
            }
        };

        BiometricPrompt biometricPrompt = new BiometricPrompt(requireActivity(), executor, callback);

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Đăng kí vân tay")
                .setSubtitle("Xác thực vân tay để lưu cấu hình đăng nhập cho tài khoản này")
                .setNegativeButtonText("Hủy")
                .build();
        biometricPrompt.authenticate(promptInfo);
    }

    private void showBiometricForGoogle(FirebaseUser user) {
        Executor executor = ContextCompat.getMainExecutor(requireContext());

        BiometricPrompt.AuthenticationCallback callback = new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                fingerprintPrefs.saveGoogleUser(user);
                Toast.makeText(requireContext(), "Đăng kí vân tay thành công", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(requireContext(), "Lỗi xác thực: " + errString, Toast.LENGTH_SHORT).show();
                switchFingerprint.setChecked(false);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(requireContext(), "Vân tay không khớp, thử lại", Toast.LENGTH_SHORT).show();
            }
        };

        BiometricPrompt biometricPrompt = new BiometricPrompt(requireActivity(), executor, callback);

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Đăng kí vân tay")
                .setSubtitle("Xác thực vân tay để bật đăng nhập bằng vân tay cho tài khoản Google")
                .setNegativeButtonText("Hủy")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }
}