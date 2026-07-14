package models;

import java.time.LocalDateTime;

/**
 * [NEW - Upgrade V3]
 * Model cho bảng DeviceReadings.
 * Lưu dữ liệu thô từ thiết bị y tế (glucometer, smartwatch, v.v.)
 */
public class DeviceReading {
    private int id;
    private int patientId;
    private String deviceType;      // 'glucometer', 'smartwatch', 'bp_monitor', 'scale'
    private String rawJsonData;     // JSON gốc từ thiết bị
    private Double parsedGlucose;
    private Integer parsedHeartRate;
    private Integer parsedSystolicBp;
    private Integer parsedDiastolicBp;
    private Double parsedWeight;
    private Double parsedSpo2;
    private LocalDateTime measuredAt;
    private LocalDateTime createdAt;
    private boolean isAbnormal;
    private String abnormalNote;

    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }
    public int getPatientId()                   { return patientId; }
    public void setPatientId(int v)             { this.patientId = v; }
    public String getDeviceType()               { return deviceType; }
    public void setDeviceType(String v)         { this.deviceType = v; }
    public String getRawJsonData()              { return rawJsonData; }
    public void setRawJsonData(String v)        { this.rawJsonData = v; }
    public Double getParsedGlucose()            { return parsedGlucose; }
    public void setParsedGlucose(Double v)      { this.parsedGlucose = v; }
    public Integer getParsedHeartRate()         { return parsedHeartRate; }
    public void setParsedHeartRate(Integer v)   { this.parsedHeartRate = v; }
    public Integer getParsedSystolicBp()        { return parsedSystolicBp; }
    public void setParsedSystolicBp(Integer v)  { this.parsedSystolicBp = v; }
    public Integer getParsedDiastolicBp()       { return parsedDiastolicBp; }
    public void setParsedDiastolicBp(Integer v) { this.parsedDiastolicBp = v; }
    public Double getParsedWeight()             { return parsedWeight; }
    public void setParsedWeight(Double v)       { this.parsedWeight = v; }
    public Double getParsedSpo2()               { return parsedSpo2; }
    public void setParsedSpo2(Double v)         { this.parsedSpo2 = v; }
    public LocalDateTime getMeasuredAt()        { return measuredAt; }
    public void setMeasuredAt(LocalDateTime v)  { this.measuredAt = v; }
    public LocalDateTime getCreatedAt()         { return createdAt; }
    public void setCreatedAt(LocalDateTime v)   { this.createdAt = v; }
    public boolean isAbnormal()                 { return isAbnormal; }
    public void setAbnormal(boolean v)          { this.isAbnormal = v; }
    public String getAbnormalNote()             { return abnormalNote; }
    public void setAbnormalNote(String v)       { this.abnormalNote = v; }
}
