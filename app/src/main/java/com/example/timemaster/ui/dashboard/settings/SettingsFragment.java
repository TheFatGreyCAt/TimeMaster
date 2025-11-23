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
import android.widget.Button;
import android.widget.EditText;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.Executor;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsFragment extends Fragment {

    private FirebaseUser currentUser;
    private DocumentReference userRef;

    private TextView tvProfileName, tvProfileRole;
    private CircleImageView profileImage;
    private FingerprintPrefs fingerprintPrefs;

    private RelativeLayout row_register_fingerprint;

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

        // Xử lí đăng kí bằng vân tay
        fingerprintPrefs = new FingerprintPrefs(requireContext());
        row_register_fingerprint = view.findViewById(R.id.row_register_fingerprint);

        row_register_fingerprint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startFingerprintRegistration();
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

    // Hàm đăng kí vân tay
    private void startFingerprintRegistration() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(requireContext(), "Bạn cần đăng nhập trước đi đăng kí vân tay", Toast.LENGTH_SHORT).show();
            return;
        }

        BiometricManager biometricManager = BiometricManager.from(requireContext());
        int canAuth = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL);

        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(requireContext(), "Không hỗ trợ đăng kí vân tay", Toast.LENGTH_SHORT).show();
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
        }
    }

    // Tìm provider chính - password hoặc google
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
                        return;
                    }
                    showBiometricForPasswordUser(user, password);
                    })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // Vân tay cho tài khoản đăng nhập bằng email + password
    private void showBiometricForPasswordUser(FirebaseUser user, String passwordPlain) {
        Executor executor = ContextCompat.getMainExecutor(requireContext());

        BiometricPrompt.AuthenticationCallback callback = new BiometricPrompt.AuthenticationCallback() {
           @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
               super.onAuthenticationSucceeded(result);

               // Lưu email + password đã mã hóa
               fingerprintPrefs.savePasswordUser(user, user.getEmail(), passwordPlain);

               Toast.makeText(requireContext(), "Đăng kí vân tay thành công", Toast.LENGTH_SHORT).show();
           }

           @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
               super.onAuthenticationError(errorCode, errString);
               Toast.makeText(requireContext(), "Lỗi xác thực: " + errString, Toast.LENGTH_SHORT).show();
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

    // Vân tay cho tài khoản đăng nhập bằng google
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

