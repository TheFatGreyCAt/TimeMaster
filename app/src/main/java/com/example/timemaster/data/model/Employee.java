package com.example.timemaster.data.model;

import java.io.Serializable;

public class Employee implements Serializable {
    private String id;
    private String name;
    private String jobTitle;
    private int avatarResId; // Tạm thời dùng resource ID

    // --- Thêm các trường này ---
    private String email;
    private String phone;
    // ---------------------------

    public Employee() { }

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public int getAvatarResId() { return avatarResId; }
    public void setAvatarResId(int avatarResId) { this.avatarResId = avatarResId; }

    // Getter Setter mới
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}