package models;

import java.time.LocalDateTime;

public class HealthIndicator {
    private int indicatorId;
    private int recordId;
    private int enteredByStaff;
    private double height;
    private double weight;
    private double bmi;
    private int systolicBp;
    private int diastolicBp;
    private int heartRate;
    private double temperature;
    private Double bloodGlucose;
    private Double hba1c;
    private Double cholesterol;
    private Double triglyceride;
    private Double hdlC;
    private Double ldlC;
    private LocalDateTime measuredAt;

    public HealthIndicator() {}

    public int getIndicatorId()                 { return indicatorId; }
    public void setIndicatorId(int v)           { this.indicatorId = v; }

    public int getRecordId()                    { return recordId; }
    public void setRecordId(int v)              { this.recordId = v; }

    public int getEnteredByStaff()              { return enteredByStaff; }
    public void setEnteredByStaff(int v)        { this.enteredByStaff = v; }

    public double getHeight()                   { return height; }
    public void setHeight(double v)             { this.height = v; }

    public double getWeight()                   { return weight; }
    public void setWeight(double v)             { this.weight = v; }

    public double getBmi()                      { return bmi; }
    public void setBmi(double v)                { this.bmi = v; }

    public int getSystolicBp()                  { return systolicBp; }
    public void setSystolicBp(int v)            { this.systolicBp = v; }

    public int getDiastolicBp()                 { return diastolicBp; }
    public void setDiastolicBp(int v)           { this.diastolicBp = v; }

    public int getHeartRate()                   { return heartRate; }
    public void setHeartRate(int v)             { this.heartRate = v; }

    public double getTemperature()              { return temperature; }
    public void setTemperature(double v)        { this.temperature = v; }

    public Double getBloodGlucose()             { return bloodGlucose; }
    public void setBloodGlucose(Double v)       { this.bloodGlucose = v; }

    public Double getHba1c()                    { return hba1c; }
    public void setHba1c(Double v)              { this.hba1c = v; }

    public Double getCholesterol()              { return cholesterol; }
    public void setCholesterol(Double v)        { this.cholesterol = v; }

    public Double getTriglyceride()             { return triglyceride; }
    public void setTriglyceride(Double v)       { this.triglyceride = v; }

    public Double getHdlC()                     { return hdlC; }
    public void setHdlC(Double v)               { this.hdlC = v; }

    public Double getLdlC()                     { return ldlC; }
    public void setLdlC(Double v)               { this.ldlC = v; }

    public LocalDateTime getMeasuredAt()             { return measuredAt; }
    public void setMeasuredAt(LocalDateTime v)       { this.measuredAt = v; }
}
