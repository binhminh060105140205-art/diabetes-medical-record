package models;

import java.time.LocalDateTime;

/**
 * [NEW - Upgrade V3]
 * Model cho bảng HealthAlerts — cảnh báo rule-based thay AIWarnings.
 */
public class HealthAlert {
    private int alertId;
    private int patientId;
    private String indicatorType;   // 'glucose', 'blood_pressure', 'heart_rate', 'spo2'
    private double value;
    private double threshold;
    private String alertLevel;      // 'low', 'medium', 'high'
    private String alertMessage;
    private String dataSource;      // 'device' hoặc 'manual'
    private Integer sourceRecordId;
    private boolean isAcknowledged;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime createdAt;

    public int getAlertId()                         { return alertId; }
    public void setAlertId(int v)                   { this.alertId = v; }
    public int getPatientId()                       { return patientId; }
    public void setPatientId(int v)                 { this.patientId = v; }
    public String getIndicatorType()                { return indicatorType; }
    public void setIndicatorType(String v)          { this.indicatorType = v; }
    public double getValue()                        { return value; }
    public void setValue(double v)                  { this.value = v; }
    public double getThreshold()                    { return threshold; }
    public void setThreshold(double v)              { this.threshold = v; }
    public String getAlertLevel()                   { return alertLevel; }
    public void setAlertLevel(String v)             { this.alertLevel = v; }
    public String getAlertMessage()                 { return alertMessage; }
    public void setAlertMessage(String v)           { this.alertMessage = v; }
    public String getDataSource()                   { return dataSource; }
    public void setDataSource(String v)             { this.dataSource = v; }
    public Integer getSourceRecordId()              { return sourceRecordId; }
    public void setSourceRecordId(Integer v)        { this.sourceRecordId = v; }
    public boolean isAcknowledged()                 { return isAcknowledged; }
    public void setAcknowledged(boolean v)          { this.isAcknowledged = v; }
    public LocalDateTime getAcknowledgedAt()        { return acknowledgedAt; }
    public void setAcknowledgedAt(LocalDateTime v)  { this.acknowledgedAt = v; }
    public LocalDateTime getCreatedAt()             { return createdAt; }
    public void setCreatedAt(LocalDateTime v)       { this.createdAt = v; }

    /** Helper cho JSP */
    public String getLevelIcon() {
        if ("high".equals(alertLevel))   return "🚨";
        if ("medium".equals(alertLevel)) return "⚠️";
        return "ℹ️";
    }
    public String getLevelClass() {
        if ("high".equals(alertLevel))   return "alert-high";
        if ("medium".equals(alertLevel)) return "alert-medium";
        return "alert-low";
    }
}
