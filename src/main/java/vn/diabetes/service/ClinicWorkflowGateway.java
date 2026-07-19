package vn.diabetes.service;
import java.time.LocalDate;
import java.time.LocalDateTime;
public interface ClinicWorkflowGateway {
 void createAppointmentRequest(int patientId,LocalDate preferredDate,String preferredPeriod,String reason,String note,int actor);
 void assignAppointmentRequest(int appointmentId,int doctorId,LocalDateTime at,String note,int actor);
 void createAppointment(int patientId,int doctorId,LocalDateTime at,String reason,String note,int actor);
 void rescheduleAppointment(int appointmentId,LocalDateTime at,String note,int actor);
 void setAppointmentStatus(int appointmentId,String status,int actor);
 void cancelOwnAppointment(int appointmentId,int patientUserId,int actor);
 void checkIn(int appointmentId,int actor);
 void setEncounterStatus(int encounterId,String status,int actor);
 void addAllergy(int patientId,String allergen,String reaction,String severity,int actor);
 void addHistory(int patientId,String type,String name,java.sql.Date date,String status,String note,int actor);
 void createLabOrder(int encounterId,int doctorId,String code,String name,String priority,String note,int actor);
 void resultLab(int orderId,String value,String unit,String range,String flag,int actor);
}
