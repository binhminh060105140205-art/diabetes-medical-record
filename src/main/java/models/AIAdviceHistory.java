package models;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [MODIFIED - Upgrade V3]
 * Thêm: riskLevel, recommendationType, sourceDataReference, avgGlucose7d, avgSystolic7d
 */
public class AIAdviceHistory {
    private int adviceId;
    private int patientId;
    private LocalDate adviceDate;
    private String adviceContent;
    private LocalDateTime createdAt;

    // [NEW V3]
    private String riskLevel;              // 'low', 'medium', 'high'
    private String recommendationType;    // 'diet','exercise','medication_reminder','general'
    private String sourceDataReference;   // JSON: {"log_ids":[101,102]}
    private Double avgGlucose7d;          // snapshot trung bình đường huyết 7 ngày
    private Integer avgSystolic7d;        // snapshot trung bình huyết áp 7 ngày

    public AIAdviceHistory() {}

    public int getAdviceId()                        { return adviceId; }
    public void setAdviceId(int v)                  { this.adviceId = v; }
    public int getPatientId()                       { return patientId; }
    public void setPatientId(int v)                 { this.patientId = v; }
    public LocalDate getAdviceDate()                { return adviceDate; }
    public void setAdviceDate(LocalDate v)          { this.adviceDate = v; }
    public String getAdviceContent()                { return adviceContent; }
    public void setAdviceContent(String v)          { this.adviceContent = v; }
    public LocalDateTime getCreatedAt()             { return createdAt; }
    public void setCreatedAt(LocalDateTime v)       { this.createdAt = v; }

    // [NEW V3]
    public String getRiskLevel()                    { return riskLevel; }
    public void setRiskLevel(String v)              { this.riskLevel = v; }
    public String getRecommendationType()           { return recommendationType; }
    public void setRecommendationType(String v)     { this.recommendationType = v; }
    public String getSourceDataReference()          { return sourceDataReference; }
    public void setSourceDataReference(String v)    { this.sourceDataReference = v; }
    public Double getAvgGlucose7d()                 { return avgGlucose7d; }
    public void setAvgGlucose7d(Double v)           { this.avgGlucose7d = v; }
    public Integer getAvgSystolic7d()               { return avgSystolic7d; }
    public void setAvgSystolic7d(Integer v)         { this.avgSystolic7d = v; }

    /** Helper cho JSP */
    public String getRiskLevelIcon() {
        if ("high".equals(riskLevel))   return "🔴";
        if ("medium".equals(riskLevel)) return "🟡";
        return "🟢";
    }
}
