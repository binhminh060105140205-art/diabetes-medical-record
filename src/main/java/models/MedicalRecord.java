package models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MedicalRecord {
    private static final DateTimeFormatter VISIT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private int recordId;
    private int patientId;
    private int doctorId;
    private int createdByStaff;
    private int encounterId;
    private LocalDateTime visitDate;
    private String reasonForVisit;
    private String symptoms;
    private String medicalHistory;
    private String lifestyleHabits;
    private String clinicalExam;
    private String complicationNote;
    private String finalDiagnosis;
    private String treatmentPlan;
    private String prescriptionNote;
    private String advice;
    private LocalDate followUpDate;
    private String doctorNote;
    private String status; // DRAFT / COMPLETED
    private LocalDateTime createdAt;

    public MedicalRecord() {}

    // Getters & Setters
    public int getRecordId()                    { return recordId; }
    public void setRecordId(int v)              { this.recordId = v; }

    public int getPatientId()                   { return patientId; }
    public void setPatientId(int v)             { this.patientId = v; }

    public int getDoctorId()                    { return doctorId; }
    public void setDoctorId(int v)              { this.doctorId = v; }

    public int getCreatedByStaff()              { return createdByStaff; }
    public void setCreatedByStaff(int v)        { this.createdByStaff = v; }
    public int getEncounterId()                 { return encounterId; }
    public void setEncounterId(int v)           { this.encounterId = v; }

    public LocalDateTime getVisitDate()              { return visitDate; }
    public void setVisitDate(LocalDateTime v)        { this.visitDate = v; }
    public String getVisitDateLabel() {
        return visitDate == null ? "—" : visitDate.format(VISIT_DATE_FORMAT);
    }

    public String getReasonForVisit()           { return reasonForVisit; }
    public void setReasonForVisit(String v)     { this.reasonForVisit = v; }

    public String getSymptoms()                 { return symptoms; }
    public void setSymptoms(String v)           { this.symptoms = v; }

    public String getMedicalHistory()           { return medicalHistory; }
    public void setMedicalHistory(String v)     { this.medicalHistory = v; }

    public String getLifestyleHabits()          { return lifestyleHabits; }
    public void setLifestyleHabits(String v)    { this.lifestyleHabits = v; }

    public String getClinicalExam()             { return clinicalExam; }
    public void setClinicalExam(String v)       { this.clinicalExam = v; }

    public String getComplicationNote()         { return complicationNote; }
    public void setComplicationNote(String v)   { this.complicationNote = v; }

    public String getFinalDiagnosis()           { return finalDiagnosis; }
    public void setFinalDiagnosis(String v)     { this.finalDiagnosis = v; }

    public String getTreatmentPlan()            { return treatmentPlan; }
    public void setTreatmentPlan(String v)      { this.treatmentPlan = v; }

    public String getPrescriptionNote()         { return prescriptionNote; }
    public void setPrescriptionNote(String v)   { this.prescriptionNote = v; }

    public String getAdvice()                   { return advice; }
    public void setAdvice(String v)             { this.advice = v; }

    public LocalDate getFollowUpDate()          { return followUpDate; }
    public void setFollowUpDate(LocalDate v)    { this.followUpDate = v; }

    public String getDoctorNote()               { return doctorNote; }
    public void setDoctorNote(String v)         { this.doctorNote = v; }

    public String getStatus()                   { return status; }
    public void setStatus(String v)             { this.status = v; }

    public LocalDateTime getCreatedAt()              { return createdAt; }
    public void setCreatedAt(LocalDateTime v)        { this.createdAt = v; }
}
