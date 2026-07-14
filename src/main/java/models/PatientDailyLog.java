package models;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [MODIFIED - Upgrade V3]
 * Thêm: heartRate, spo2, mealType
 */
public class PatientDailyLog {
    private int logId;
    private int patientId;
    private LocalDate logDate;
    private Double bloodGlucose;
    private Integer systolicBp;
    private Integer diastolicBp;
    private Double weight;
    private String symptoms;
    private String note;
    private LocalDateTime createdAt;

    // [NEW V3]
    private Integer heartRate;   // nhịp tim (bpm)
    private Double spo2;         // độ bão hòa oxy (%)
    private String mealType;     // 'fasting','before_meal','after_meal','bedtime'

    public PatientDailyLog() {}

    public int getLogId()                       { return logId; }
    public void setLogId(int v)                 { this.logId = v; }
    public int getPatientId()                   { return patientId; }
    public void setPatientId(int v)             { this.patientId = v; }
    public LocalDate getLogDate()               { return logDate; }
    public void setLogDate(LocalDate v)         { this.logDate = v; }
    public Double getBloodGlucose()             { return bloodGlucose; }
    public void setBloodGlucose(Double v)       { this.bloodGlucose = v; }
    public Integer getSystolicBp()              { return systolicBp; }
    public void setSystolicBp(Integer v)        { this.systolicBp = v; }
    public Integer getDiastolicBp()             { return diastolicBp; }
    public void setDiastolicBp(Integer v)       { this.diastolicBp = v; }
    public Double getWeight()                   { return weight; }
    public void setWeight(Double v)             { this.weight = v; }
    public String getSymptoms()                 { return symptoms; }
    public void setSymptoms(String v)           { this.symptoms = v; }
    public String getNote()                     { return note; }
    public void setNote(String v)               { this.note = v; }
    public LocalDateTime getCreatedAt()         { return createdAt; }
    public void setCreatedAt(LocalDateTime v)   { this.createdAt = v; }

    // [NEW V3]
    public Integer getHeartRate()               { return heartRate; }
    public void setHeartRate(Integer v)         { this.heartRate = v; }
    public Double getSpo2()                     { return spo2; }
    public void setSpo2(Double v)               { this.spo2 = v; }
    public String getMealType()                 { return mealType; }
    public void setMealType(String v)           { this.mealType = v; }

    public String getMealTypeLabel() {
        if (mealType == null) return "";
        return switch (mealType) {
            case "fasting"     -> "Lúc đói";
            case "before_meal" -> "Trước bữa ăn";
            case "after_meal"  -> "Sau bữa ăn 2h";
            case "bedtime"     -> "Trước ngủ";
            default -> mealType;
        };
    }
}
