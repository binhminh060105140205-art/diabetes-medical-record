package models;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Doctor {
    private int doctorId;
    private int userId;
    private String specialty;
    private String licenseNo;
    private LocalDate licenseIssueDate;
    private LocalDate licenseExpireDate;
    private String licenseIssuedBy;
    private String degree;
    private BigDecimal consultationFee;
    private String diabetesFocus = "GENERAL";

    // Đường dẫn (tên file) ảnh lưu trên server — xem thêm util.FileStorageUtil
    private String faceImagePath;
    private String cccdImagePath;
    private String licenseImagePath;

    // denormalized fields for display (lấy từ JOIN với Users, không có cột riêng trong Doctors)
    private String fullName;
    private String phone;
    private String email;

    public Doctor() {}

    // Constructor đầy đủ (khớp hoàn toàn với bảng Doctors)
    public Doctor(int doctorId, int userId, String specialty, String licenseNo,
                  LocalDate licenseIssueDate, LocalDate licenseExpireDate,
                  String licenseIssuedBy, String degree, BigDecimal consultationFee) {
        this.doctorId          = doctorId;
        this.userId            = userId;
        this.specialty         = specialty;
        this.licenseNo         = licenseNo;
        this.licenseIssueDate  = licenseIssueDate;
        this.licenseExpireDate = licenseExpireDate;
        this.licenseIssuedBy   = licenseIssuedBy;
        this.degree            = degree;
        this.consultationFee   = consultationFee;
    }

    // Getters & Setters
    public int getDoctorId()               { return doctorId; }
    public void setDoctorId(int v)         { this.doctorId = v; }

    public int getUserId()                 { return userId; }
    public void setUserId(int v)           { this.userId = v; }

    public String getSpecialty()           { return specialty; }
    public void setSpecialty(String v)     { this.specialty = v; }

    public String getLicenseNo()           { return licenseNo; }
    public void setLicenseNo(String v)     { this.licenseNo = v; }

    public LocalDate getLicenseIssueDate()        { return licenseIssueDate; }
    public void setLicenseIssueDate(LocalDate v)  { this.licenseIssueDate = v; }

    public LocalDate getLicenseExpireDate()       { return licenseExpireDate; }
    public void setLicenseExpireDate(LocalDate v) { this.licenseExpireDate = v; }

    public String getLicenseIssuedBy()            { return licenseIssuedBy; }
    public void setLicenseIssuedBy(String v)      { this.licenseIssuedBy = v; }

    public String getDegree()              { return degree; }
    public void setDegree(String v)        { this.degree = v; }

    public BigDecimal getConsultationFee()           { return consultationFee; }
    public void setConsultationFee(BigDecimal v)     { this.consultationFee = v; }

    public String getDiabetesFocus()                 { return diabetesFocus; }
    public void setDiabetesFocus(String v)           { this.diabetesFocus = v; }

    public String getDiabetesFocusLabel() {
        if (diabetesFocus == null) return "Chuyên khoa hỗ trợ";
        return switch (diabetesFocus) {
            case "TYPE_1" -> "Ưu tiên Type 1";
            case "TYPE_2" -> "Ưu tiên Type 2";
            case "BOTH" -> "Type 1 & Type 2";
            default -> "Chuyên khoa hỗ trợ";
        };
    }

    public String getFaceImagePath()          { return faceImagePath; }
    public void setFaceImagePath(String v)    { this.faceImagePath = v; }

    public String getCccdImagePath()          { return cccdImagePath; }
    public void setCccdImagePath(String v)    { this.cccdImagePath = v; }

    public String getLicenseImagePath()       { return licenseImagePath; }
    public void setLicenseImagePath(String v) { this.licenseImagePath = v; }

    public String getFullName()            { return fullName; }
    public void setFullName(String v)      { this.fullName = v; }

    public String getPhone()               { return phone; }
    public void setPhone(String v)         { this.phone = v; }

    public String getEmail()               { return email; }
    public void setEmail(String v)         { this.email = v; }

    @Override
    public String toString() {
        return "Doctor{" +
                "doctorId=" + doctorId +
                ", fullName='" + fullName + '\'' +
                ", specialty='" + specialty + '\'' +
                ", licenseNo='" + licenseNo + '\'' +
                ", degree='" + degree + '\'' +
                ", consultationFee=" + consultationFee +
                '}';
    }
}
