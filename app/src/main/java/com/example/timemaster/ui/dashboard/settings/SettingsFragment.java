package com.example.timemaster.ui.dashboard.settings;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.ExperimentalGetImage;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;

import de.hdodenhof.circleimageview.CircleImageView;

@ExperimentalGetImage
public class SettingsFragment extends Fragment {

    private FirebaseUser currentUser;
    private DocumentReference userRef;
    private StorageReference storageRef;

    private TextView tvProfileName, tvProfileRole;
    private CircleImageView profileImage;
    private RelativeLayout rowChangePassword;
    private RelativeLayout rowEditProfile;

    // Khai báo ActivityResultLauncher
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private Uri currentPhotoUri;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_settings, container, false);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // Khởi tạo DocumentReference tới tài liệu người dùng trong collection "users"
            userRef = FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid());
            // Khởi tạo Storage Reference
            storageRef = FirebaseStorage.getInstance().getReference().child("profile_images").child(currentUser.getUid());
        }

        initViews(view);
        registerImagePickerLauncher(); // Khởi tạo Launcher
        loadUserProfile();
        setupClicks(view);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Tải lại thông tin người dùng mỗi khi Fragment hiển thị trở lại
        // Điều này đảm bảo tên và ảnh được cập nhật nếu người dùng chỉnh sửa
        loadUserProfile();
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

        // Click vào ảnh đại diện để thay đổi
        profileImage.setOnClickListener(v -> showImagePickerDialog());

        // Gọi handleLogout để xử lý việc đăng xuất an toàn
        view.findViewById(R.id.btn_logout).setOnClickListener(v -> handleLogout());
    }

    // --- I. PROFILE LOGIC
    private void loadUserProfile() {

        // 1. Tải nhanh thông tin từ Firebase Authentication (tên hoặc Email)
        if (currentUser != null) {
            String quickName = currentUser.getDisplayName();
            String quickEmail = currentUser.getEmail();

            if (quickName != null && !quickName.isEmpty()) {
                tvProfileName.setText(quickName);
            } else if (quickEmail != null) {
                tvProfileName.setText(quickEmail); // Tạm thời hiện email nếu chưa có tên
            }
        }

        // 2. Tải thông tin chi tiết từ Firestore (lấy trường 'fullName' và 'photoUrl')
        if (userRef != null) {
            userRef.get().addOnCompleteListener(task -> {
                // Kiểm tra Fragment còn gắn vào Activity trước khi tương tác với View
                if (getActivity() == null || getContext() == null) return;

                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();

                    if (document.exists()) {
                        String name = document.getString("fullName");
                        String role = document.getString("role");
                        String photoUrl = document.getString("photoUrl");

                        // Cập nhật tên và vai trò
                        tvProfileName.setText(name != null ? name : "Người dùng");
                        tvProfileRole.setText(role != null ? role.toUpperCase() : "");

                        // Tải ảnh bằng Glide
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(photoUrl)
                                    .placeholder(R.drawable.ic_avatar) // Ảnh tạm thời
                                    .error(R.drawable.ic_avatar)      // Ảnh khi lỗi
                                    .into(profileImage);
                        } else {
                            // Nếu không có URL, đặt ảnh mặc định
                            profileImage.setImageResource(R.drawable.ic_avatar);
                        }

                    } else {
                        // Nếu không tìm thấy document, hiển thị thông báo
                        if (currentUser != null && currentUser.getDisplayName() == null) {
                            tvProfileName.setText("User data not found");
                        }
                    }
                } else {
                    Toast.makeText(getContext(), "Failed to load profile.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // --- CHỨC NĂNG CHỤP/CHỌN VÀ TẢI ẢNH ---

    private void registerImagePickerLauncher() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            uploadImageToFirebase(imageUri);
                        }
                    } else if (result.getResultCode() == getActivity().RESULT_OK && currentPhotoUri != null) {
                        // Xử lý ảnh chụp từ Camera
                        uploadImageToFirebase(currentPhotoUri);
                    } else {
                        Toast.makeText(getContext(), "Hủy bỏ chọn ảnh.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void showImagePickerDialog() {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Chọn ảnh đại diện");
        builder.setItems(new CharSequence[]{"Chụp ảnh mới", "Chọn từ Thư viện"}, (dialog, which) -> {
            if (which == 0) {
                // Chụp ảnh
                dispatchTakePictureIntent();
            } else {
                // Chọn từ thư viện
                Intent galleryIntent = new Intent(Intent.ACTION_PICK);
                galleryIntent.setType("image/*");
                imagePickerLauncher.launch(galleryIntent);
            }
        });
        builder.show();
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {

            // Tạo một tệp tạm thời để lưu ảnh
            File photoFile = null;
            try {
                // Sử dụng getExternalCacheDir() để lưu tạm
                File imagesDir = new File(requireContext().getExternalCacheDir(), "images");
                if (!imagesDir.exists()) imagesDir.mkdirs();
                photoFile = new File(imagesDir, System.currentTimeMillis() + ".jpg");

            } catch (Exception ex) {
                Toast.makeText(getContext(), "Lỗi tạo file: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null) {
                // NOTE: Dùng FileProvider (tốt hơn) thay vì Uri.fromFile()
                // Nhưng để đơn giản, ta dùng Uri.fromFile() cho file tạm trong Cache
                currentPhotoUri = Uri.fromFile(photoFile);
                takePictureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                imagePickerLauncher.launch(takePictureIntent);
            }
        } else {
            Toast.makeText(getContext(), "Thiết bị không hỗ trợ chụp ảnh.", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadImageToFirebase(Uri imageUri) {
        if (storageRef == null || getContext() == null) return;

        // Hiển thị ảnh vừa chọn lên UI ngay lập tức
        Glide.with(this).load(imageUri).into(profileImage);

        Toast.makeText(getContext(), "Đang tải ảnh lên...", Toast.LENGTH_SHORT).show();

        // Tải ảnh lên Storage
        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Lấy URL của ảnh sau khi tải lên
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();

                        // Cập nhật URL vào Firestore (dùng trường 'photoUrl')
                        updateProfileUrlInFirestore(downloadUrl);
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Tải ảnh thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateProfileUrlInFirestore(String downloadUrl) {
        if (userRef == null || getContext() == null) return;

        // Cập nhật URL trong Firestore
        userRef.update("photoUrl", downloadUrl)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Cập nhật ảnh đại diện thành công!", Toast.LENGTH_SHORT).show();
                    // Vì đã gọi loadUserProfile() trong onResume(), nên không cần gọi lại ở đây
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi cập nhật Firestore.", Toast.LENGTH_SHORT).show();
                });
    }

    //  II. CHANGE PASSWORD
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