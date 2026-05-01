package com.ptithcm.ptitmeet.models;

import com.google.gson.annotations.SerializedName;

public class User {
    // Sử dụng @SerializedName để map chính xác với tên key JSON từ Spring Boot trả về
    @SerializedName("id")
    private Long id;

    @SerializedName("email")
    private String email;

    @SerializedName("fullName")
    private String fullName;

    @SerializedName("avatarUrl")
    private String avatarUrl;

    // Các hàm Getter và Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}