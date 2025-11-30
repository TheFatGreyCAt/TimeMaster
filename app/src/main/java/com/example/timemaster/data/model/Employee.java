package com.example.timemaster.data.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

import java.io.Serializable;

public class Employee implements Serializable {
    private String id;
    private String name;
    private String jobTitle;
    private int avatarResId;
    private String phone;
    private String email;

    public Employee() {
        this.avatarResId = com.example.timemaster.R.drawable.ic_avatar;
    }

    public Employee(String id, String name, String jobTitle, int avatarResId, String phone, String email) {
        this.id = id;
        this.name = name;
        this.jobTitle = jobTitle;
        this.avatarResId = avatarResId;
        this.phone = phone;
        this.email = email;
    }


    @Exclude
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @PropertyName("displayName")
    public String getName() { return name; }
    @PropertyName("displayName")
    public void setName(String name) { this.name = name; }

    @PropertyName("phoneNumber")
    public String getPhone() { return phone; }
    @PropertyName("phoneNumber")
    public void setPhone(String phone) { this.phone = phone; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @Exclude
    public int getAvatarResId() { return avatarResId; }
    @Exclude
    public void setAvatarResId(int avatarResId) { this.avatarResId = avatarResId; }
}