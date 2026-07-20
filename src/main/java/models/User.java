package models;

import java.util.Date;

public class User {
    private int userId;
    private String username;
    private String password;
    private String fullName;
    private String phone;
    private String role;
    private String status;
    private String email;
    private Date dob;
    private String gender;
    private String address;
    private String cccd;
    private Date createdAt;

    public User() {
    }

    public User(int userId, String username, String password, String fullName, String phone, String role, String status, String email, Date dob, String gender, String address, String cccd) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.phone = phone;
        this.role = role;
        this.status = status;
        this.email = email;
        this.dob = dob;
        this.gender = gender;
        this.address = address;
        this.cccd = cccd;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Date getDob() {
        return dob;
    }

    public void setDob(Date dob) {
        this.dob = dob;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getGenderLabel() {
        if (gender == null || gender.isBlank()) return "Chưa cập nhật";
        if ("Male".equalsIgnoreCase(gender) || "Nam".equalsIgnoreCase(gender)) return "Nam";
        if ("Female".equalsIgnoreCase(gender) || "Nữ".equalsIgnoreCase(gender)) return "Nữ";
        if ("Other".equalsIgnoreCase(gender) || "Khác".equalsIgnoreCase(gender)) return "Khác";
        return gender;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCccd() {
        return cccd;
    }

    public void setCccd(String cccd) {
        this.cccd = cccd;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
