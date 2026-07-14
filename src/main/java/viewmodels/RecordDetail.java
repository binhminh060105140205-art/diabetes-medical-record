package viewmodels;

import models.*;

public class RecordDetail {
    private MedicalRecord record;
    private Patient patient;
    private Doctor doctor;
    private HealthIndicator indicator;
    private AIWarning warning;

    public RecordDetail() {}

    public MedicalRecord getRecord()               { return record; }
    public void setRecord(MedicalRecord v)         { this.record = v; }

    public Patient getPatient()                    { return patient; }
    public void setPatient(Patient v)              { this.patient = v; }

    public Doctor getDoctor()                      { return doctor; }
    public void setDoctor(Doctor v)                { this.doctor = v; }

    public HealthIndicator getIndicator()          { return indicator; }
    public void setIndicator(HealthIndicator v)    { this.indicator = v; }

    public AIWarning getWarning()                  { return warning; }
    public void setWarning(AIWarning v)            { this.warning = v; }
}
