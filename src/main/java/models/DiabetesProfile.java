package models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

    public String getCarePathTitle() {
        if ("TYPE_1".equals(diabetesType)) return "Quy trình điều trị típ 1";
        if ("TYPE_2".equals(diabetesType)) return "Quy trình điều trị típ 2";
        return "Cần bác sĩ xác nhận loại đái tháo đường";
    }

    public String getCarePathSummary() {
        if ("TYPE_1".equals(diabetesType)) {
            return "Trọng tâm là insulin, theo dõi đường huyết sát và phòng hạ đường huyết hoặc nhiễm toan ceton.";
        }
        if ("TYPE_2".equals(diabetesType)) {
            return "Trọng tâm là lối sống, thuốc phù hợp và kiểm soát đồng thời cân nặng, huyết áp, mỡ máu.";
        }
        return "Chưa áp dụng phác đồ theo típ cho đến khi bác sĩ xác nhận chẩn đoán.";
    }

    public List<String> getCarePathSteps() {
        if ("TYPE_1".equals(diabetesType)) {
            return List.of(
                    "Xác nhận phác đồ có insulin và hướng dẫn thời điểm sử dụng.",
                    "Theo dõi đường huyết theo kế hoạch; ghi nhận bữa ăn, vận động và triệu chứng.",
                    "Đánh giá hạ đường huyết, ketone hoặc dấu hiệu nhiễm toan khi có nguy cơ.",
                    "Bác sĩ xem kết quả, điều chỉnh điều trị và hẹn tái khám.");
        }
        if ("TYPE_2".equals(diabetesType)) {
            return List.of(
                    "Đánh giá ăn uống, vận động, cân nặng và nguy cơ tim mạch.",
                    "Chọn lối sống, thuốc uống, insulin hoặc điều trị phối hợp theo bác sĩ.",
                    "Theo dõi HbA1c, đường huyết, huyết áp, chức năng thận và mỡ máu.",
                    "Đánh giá đáp ứng để duy trì hoặc tăng bậc điều trị khi cần.");
        }
        return List.of(
                "Bác sĩ xác nhận típ 1, típ 2 hoặc cần đánh giá thêm.",
                "Sau khi phân loại, hệ thống mới áp dụng luồng điều trị và theo dõi phù hợp.");
    }

    public String getCarePathCode() {
        if ("TYPE_1".equals(diabetesType)) return "type-1";
        if ("TYPE_2".equals(diabetesType)) return "type-2";
        return "unknown";
    }
}
