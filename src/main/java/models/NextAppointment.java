package models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class NextAppointment {
    private int appointmentId;
    private int patientId;
    private LocalDate appointmentDate;
    private String source;   // "AUTO" | "DOCTOR"
    private LocalDateTime createdAt;

    public NextAppointment() {}

    public int getAppointmentId()               { return appointmentId; }
    public void setAppointmentId(int v)         { this.appointmentId = v; }
    public int getPatientId()                   { return patientId; }
    public void setPatientId(int v)             { this.patientId = v; }
    public LocalDate getAppointmentDate()       { return appointmentDate; }
    public void setAppointmentDate(LocalDate v) { this.appointmentDate = v; }
    public String getSource()                   { return source; }
    public void setSource(String v)             { this.source = v; }
    public LocalDateTime getCreatedAt()         { return createdAt; }
    public void setCreatedAt(LocalDateTime v)   { this.createdAt = v; }
}
