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
    private double bloodGlucose;
    private double hba1c;
    private double cholesterol;
    private double triglyceride;
    private double hdlC;
    private double ldlC;
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

    public double getBloodGlucose()             { return bloodGlucose; }
    public void setBloodGlucose(double v)       { this.bloodGlucose = v; }

    public double getHba1c()                    { return hba1c; }
    public void setHba1c(double v)              { this.hba1c = v; }

    public double getCholesterol()              { return cholesterol; }
    public void setCholesterol(double v)        { this.cholesterol = v; }

    public double getTriglyceride()             { return triglyceride; }
    public void setTriglyceride(double v)       { this.triglyceride = v; }

    public double getHdlC()                     { return hdlC; }
    public void setHdlC(double v)               { this.hdlC = v; }

    public double getLdlC()                     { return ldlC; }
    public void setLdlC(double v)               { this.ldlC = v; }

    public LocalDateTime getMeasuredAt()             { return measuredAt; }
    public void setMeasuredAt(LocalDateTime v)       { this.measuredAt = v; }
}
