package com.example.timemaster.model;

public class User {
    private String uid;
    private String displayName;
    private String email;
    // Thêm các trường khác nếu cần, ví dụ:
    // private String position; // Chức vụ
    // private String employeeId; // Mã nhân viên

    // Constructor rỗng là bắt buộc cho việc đọc dữ liệu từ Firebase/Firestore
    public User() {
    }

    public User(String uid, String displayName, String email) {
        this.uid = uid;
        this.displayName = displayName;
        this.email = email;
    }

    // --- Getters ---
    public String getUid() {
        return uid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    // --- Setters ---
    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
