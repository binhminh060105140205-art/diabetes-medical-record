package models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Patient {
    private int patientId;
    private int userId;
    private String fullName;
    private LocalDate dateOfBirth;
    private String gender;
    private String phone;
    private String address;
    private String healthInsuranceNo;

    // ── CĂN CƯỚC CÔNG DÂN ────────────────────────────
    private String nationalId;        // Số CCCD / CMND
    private LocalDate nationalIdDate; // Ngày cấp
    private String nationalIdPlace;   // Nơi cấp
    // ──────────────────────────────────────────────────

    private int createdBy;
    private LocalDateTime createdAt;

    public Patient() {}

    // Constructor đầy đủ (khớp hoàn toàn với bảng Patients)
    public Patient(int patientId, int userId, String fullName, LocalDate dateOfBirth,
                   String gender, String phone, String address, String healthInsuranceNo,
                   String nationalId, LocalDate nationalIdDate, String nationalIdPlace,
                   int createdBy, LocalDateTime createdAt) {
        this.patientId         = patientId;
        this.userId            = userId;
        this.fullName          = fullName;
        this.dateOfBirth       = dateOfBirth;
        this.gender            = gender;
        this.phone             = phone;
        this.address           = address;
        this.healthInsuranceNo = healthInsuranceNo;
        this.nationalId        = nationalId;
        this.nationalIdDate    = nationalIdDate;
        this.nationalIdPlace   = nationalIdPlace;
        this.createdBy         = createdBy;
        this.createdAt         = createdAt;
    }

    public int getPatientId()              { return patientId; }
    public void setPatientId(int v)        { this.patientId = v; }
    public int getUserId()                 { return userId; }
    public void setUserId(int v)           { this.userId = v; }
    public String getFullName()            { return fullName; }
    public void setFullName(String v)      { this.fullName = v; }
    public LocalDate getDateOfBirth()      { return dateOfBirth; }
    public void setDateOfBirth(LocalDate v){ this.dateOfBirth = v; }
    public String getGender()              { return gender; }
    public void setGender(String v)        { this.gender = v; }
    public String getGenderLabel() {
        if (gender == null || gender.isBlank()) return "Chưa cập nhật";
        if ("Male".equalsIgnoreCase(gender) || "Nam".equalsIgnoreCase(gender)) return "Nam";
        if ("Female".equalsIgnoreCase(gender) || "Nữ".equalsIgnoreCase(gender)) return "Nữ";
        if ("Other".equalsIgnoreCase(gender) || "Khác".equalsIgnoreCase(gender)) return "Khác";
        return gender;
    }
    public String getPhone()               { return phone; }
    public void setPhone(String v)         { this.phone = v; }
    public String getAddress()             { return address; }
    public void setAddress(String v)       { this.address = v; }
    public String getHealthInsuranceNo()   { return healthInsuranceNo; }
    public void setHealthInsuranceNo(String v) { this.healthInsuranceNo = v; }

    public String getNationalId()              { return nationalId; }
    public void setNationalId(String v)        { this.nationalId = v; }
    public LocalDate getNationalIdDate()       { return nationalIdDate; }
    public void setNationalIdDate(LocalDate v) { this.nationalIdDate = v; }
    public String getNationalIdPlace()         { return nationalIdPlace; }
    public void setNationalIdPlace(String v)   { this.nationalIdPlace = v; }

    public int getCreatedBy()              { return createdBy; }
    public void setCreatedBy(int v)        { this.createdBy = v; }
    public LocalDateTime getCreatedAt()    { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }

    @Override
    public String toString() {
        return "Patient{" +
                "patientId=" + patientId +
                ", fullName='" + fullName + '\'' +
                ", dateOfBirth=" + dateOfBirth +
                ", gender='" + gender + '\'' +
                ", phone='" + phone + '\'' +
                ", healthInsuranceNo='" + healthInsuranceNo + '\'' +
                '}';
    }
}
