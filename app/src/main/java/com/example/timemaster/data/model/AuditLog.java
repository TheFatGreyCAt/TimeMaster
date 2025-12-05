package com.example.timemaster.data.model;

import com.google.firebase.Timestamp;

public class AuditLog {
    private String logId;
    private String action;      // Ví dụ: "Xóa nhân viên", "Sửa thông tin"
    private String details;     // Chi tiết: "Đã xóa nhân viên Nguyễn Văn A"
    private String adminEmail;  // Email người thực hiện
    private Timestamp timestamp; // Thời gian

    public AuditLog() {} // Constructor rỗng cho Firestore

    public AuditLog(String action, String details, String adminEmail, Timestamp timestamp) {
        this.action = action;
        this.details = details;
        this.adminEmail = adminEmail;
        this.timestamp = timestamp;
    }

    // Getter & Setter
    public String getLogId() { return logId; }
    public void setLogId(String logId) { this.logId = logId; }
    public String getAction() { return action; }
    public String getDetails() { return details; }
    public String getAdminEmail() { return adminEmail; }
    public Timestamp getTimestamp() { return timestamp; }
}