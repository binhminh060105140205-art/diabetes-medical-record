package models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class DiabetesProfile {
    private int patientId;
    private String diabetesType;    // TYPE_1, TYPE_2, UNKNOWN
    private LocalDate diagnosisDate;
    private String treatmentMethod; // INSULIN, ORAL_MEDICATION, LIFESTYLE, COMBINATION
    private Double hba1cTarget;
    private LocalDateTime updatedAt;

    public DiabetesProfile() {}

    public DiabetesProfile(int patientId, String diabetesType, LocalDate diagnosisDate,
                            String treatmentMethod, Double hba1cTarget, LocalDateTime updatedAt) {
        this.patientId = patientId;
        this.diabetesType = diabetesType;
        this.diagnosisDate = diagnosisDate;
        this.treatmentMethod = treatmentMethod;
        this.hba1cTarget = hba1cTarget;
        this.updatedAt = updatedAt;
    }

    public int getPatientId()                      { return patientId; }
    public void setPatientId(int v)                { this.patientId = v; }
    public String getDiabetesType()                { return diabetesType; }
    public void setDiabetesType(String v)          { this.diabetesType = v; }
    public LocalDate getDiagnosisDate()            { return diagnosisDate; }
    public void setDiagnosisDate(LocalDate v)      { this.diagnosisDate = v; }
    public String getTreatmentMethod()             { return treatmentMethod; }
    public void setTreatmentMethod(String v)       { this.treatmentMethod = v; }
    public Double getHba1cTarget()                 { return hba1cTarget; }
    public void setHba1cTarget(Double v)           { this.hba1cTarget = v; }
    public LocalDateTime getUpdatedAt()            { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v)      { this.updatedAt = v; }

    public boolean isUnknown() { return diabetesType == null || "UNKNOWN".equals(diabetesType); }

    public String getDiabetesTypeLabel() {
        if ("TYPE_1".equals(diabetesType)) return "Đái tháo đường típ 1";
        if ("TYPE_2".equals(diabetesType)) return "Đái tháo đường típ 2";
        return "Chưa xác định loại";
    }

    public String getTreatmentMethodLabel() {
        if (treatmentMethod == null) return "Chưa xác định";
        return switch (treatmentMethod) {
            case "INSULIN" -> "Insulin";
            case "ORAL_MEDICATION" -> "Thuốc hạ đường huyết đường uống";
            case "LIFESTYLE" -> "Điều chỉnh ăn uống và vận động";
            case "COMBINATION" -> "Điều trị phối hợp";
            default -> "Chưa xác định";
        };
    }
}
