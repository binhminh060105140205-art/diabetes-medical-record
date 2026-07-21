package dal;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import models.Doctor;
import models.LabResultImportRow;
import models.Patient;
import vn.diabetes.validation.AppointmentRules;

public class ClinicWorkflowDAO extends DBContext implements vn.diabetes.service.ClinicWorkflowGateway {
    private static final Set<String> NON_RESCHEDULABLE_APPOINTMENT_STATUSES =
            Set.of("REQUESTED", "CHECKED_IN", "COMPLETED", "CANCELLED", "NO_SHOW");
    private static final Set<String> CHECK_IN_STATUSES = Set.of("BOOKED", "CONFIRMED");
    private static final Set<String> ENCOUNTER_STATUSES = Set.of(
            "WAITING_TRIAGE", "WAITING_DOCTOR", "IN_CONSULTATION", "WAITING_LAB",
            "LAB_COMPLETED", "COMPLETED", "CANCELLED");

    private List<Map<String, Object>> query(String sql, Object... arguments) {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            bind(ps, arguments);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData metadata = rs.getMetaData();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int index = 1; index <= metadata.getColumnCount(); index++) {
                        row.put(metadata.getColumnLabel(index), rs.getObject(index));
                    }
                    rows.add(row);
                }
            }
        } catch (SQLException error) {
            throw databaseError("query clinic workflow", error);
        }
        return rows;
    }

    private int update(String sql, Object... arguments) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, arguments);
            return statement.executeUpdate();
        }
    }

    private void bind(PreparedStatement statement, Object... arguments) throws SQLException {
        for (int index = 0; index < arguments.length; index++) {
            Object value = arguments[index];
            if (value instanceof LocalDateTime dateTime) {
                statement.setTimestamp(index + 1, Timestamp.valueOf(dateTime));
            } else {
                statement.setObject(index + 1, value);
            }
        }
    }

    private void inTransaction(String failureMessage, TransactionWork work) {
        try {
            connection.setAutoCommit(false);
            work.run();
            connection.commit();
        } catch (Exception error) {
            rollbackQuietly();
            if (error instanceof IllegalArgumentException validationError) {
                throw validationError;
            }
            throw new IllegalStateException(failureMessage, error);
        } finally {
            restoreAutoCommit();
        }
    }

    @FunctionalInterface
    private interface TransactionWork {
        void run() throws Exception;
    }

    /** Loads the patient id and appointment history in one database round-trip. */
    public PatientAppointmentPageData loadPatientAppointmentPage(int userId) {
        String sql = """
          WITH subject AS (SELECT patient_id FROM patients WHERE user_id=?)
          SELECT s.patient_id,a.appointment_id,a.appointment_at,
                 a.preferred_date,a.preferred_period,a.doctor_id,u.full_name doctor_name,
                 d.specialty,a.reason,a.note,a.status
          FROM subject s LEFT JOIN LATERAL (
            SELECT * FROM appointments WHERE patient_id=s.patient_id
            ORDER BY preferred_date DESC,appointment_at DESC NULLS FIRST LIMIT 30
          ) a ON TRUE
          LEFT JOIN doctors d ON d.doctor_id=a.doctor_id LEFT JOIN users u ON u.user_id=d.user_id
          ORDER BY a.preferred_date DESC,a.appointment_at DESC NULLS LAST
          """;
        List<Map<String,Object>> appointments = new ArrayList<>();
        Integer patientId = null;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rows = ps.executeQuery()) {
                while (rows.next()) {
                    patientId = rows.getInt("patient_id");
                    if (rows.getObject("appointment_id") != null) {
                        Map<String,Object> appointment = new LinkedHashMap<>();
                        appointment.put("appointment_id", rows.getObject("appointment_id"));
                        appointment.put("appointment_at", rows.getObject("appointment_at"));
                        appointment.put("preferred_date", rows.getObject("preferred_date"));
                        appointment.put("preferred_period", rows.getString("preferred_period"));
                        appointment.put("doctor_id", rows.getObject("doctor_id"));
                        appointment.put("doctor_name", rows.getString("doctor_name"));
                        appointment.put("specialty", rows.getString("specialty"));
                        appointment.put("reason", rows.getString("reason"));
                        appointment.put("note", rows.getString("note"));
                        appointment.put("status", rows.getString("status"));
                        appointments.add(appointment);
                    }
                }
            }
        } catch (SQLException error) {
            throw databaseError("load patient appointment page", error);
        }
        return new PatientAppointmentPageData(patientId, appointments);
    }

    public Integer patientIdForUser(int userId) {
        List<Map<String, Object>> rows = query(
                "SELECT patient_id FROM patients WHERE user_id=?", userId);
        return rows.isEmpty() ? null : ((Number) rows.get(0).get("patient_id")).intValue();
    }

    public record PatientAppointmentPageData(Integer patientId,
            List<Map<String,Object>> appointments) {}

    public List<Map<String,Object>> assignedAppointmentsForDoctor(int doctorId) {
        return query("""
                SELECT a.appointment_id,a.appointment_at,a.preferred_date,a.preferred_period,
                       a.status,a.reason,a.note,p.patient_id,p.full_name patient_name,p.phone,
                       COALESCE(dp.diabetes_type,'UNKNOWN') diabetes_type
                FROM appointments a
                JOIN patients p ON p.patient_id=a.patient_id
                LEFT JOIN diabetes_profiles dp ON dp.patient_id=p.patient_id
                WHERE a.doctor_id=?
                  AND a.status IN ('BOOKED','CONFIRMED','CHECKED_IN')
                  AND (a.appointment_at IS NULL OR a.appointment_at>=CURRENT_DATE)
                ORDER BY a.appointment_at NULLS LAST,a.preferred_date,a.appointment_id
                LIMIT 30""", doctorId);
    }

    /** Loads patients, doctors and the latest appointments in one Aiven round-trip. */
    public AppointmentOperationsPageData loadAppointmentOperationsPage() {
        String sql = """
          SELECT 'PATIENT' row_type,p.patient_id,NULL::INTEGER doctor_id,p.full_name display_name,
                 p.phone,NULL::VARCHAR specialty,NULL::VARCHAR diabetes_focus,
                 COALESCE(dp.diabetes_type,'UNKNOWN') diabetes_type,
                 NULL::INTEGER appointment_id,NULL::TIMESTAMP appointment_at,
                 NULL::DATE preferred_date,NULL::VARCHAR preferred_period,
                 NULL::VARCHAR reason,NULL::VARCHAR note,NULL::VARCHAR status,
                 NULL::VARCHAR patient_name,NULL::VARCHAR patient_phone,NULL::VARCHAR doctor_name,
                 p.created_at sort_date
          FROM patients p JOIN users pu ON pu.user_id=p.user_id AND pu.status='ACTIVE'
          LEFT JOIN diabetes_profiles dp ON dp.patient_id=p.patient_id
          UNION ALL
          SELECT 'DOCTOR',NULL,d.doctor_id,u.full_name,NULL,d.specialty,d.diabetes_focus,
                 NULL::VARCHAR,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL
          FROM doctors d JOIN users u ON u.user_id=d.user_id AND u.status='ACTIVE'
          WHERE d.diabetes_focus IN ('TYPE_1','TYPE_2','BOTH')
          UNION ALL
          SELECT 'APPOINTMENT',a.patient_id,a.doctor_id,NULL,p.phone,NULL,NULL,
                 COALESCE(dp.diabetes_type,'UNKNOWN'),
                 a.appointment_id,a.appointment_at,a.preferred_date,a.preferred_period,
                 a.reason,a.note,a.status,p.full_name,p.phone,u.full_name,
                 COALESCE(a.appointment_at,a.preferred_date::timestamp)
          FROM (
            SELECT * FROM appointments
            ORDER BY preferred_date DESC,appointment_at DESC NULLS FIRST LIMIT 50
          ) a JOIN patients p ON p.patient_id=a.patient_id
          LEFT JOIN diabetes_profiles dp ON dp.patient_id=p.patient_id
          LEFT JOIN doctors d ON d.doctor_id=a.doctor_id LEFT JOIN users u ON u.user_id=d.user_id
          ORDER BY row_type,sort_date DESC NULLS LAST,display_name
          """;
        List<Patient> patients = new ArrayList<>();
        List<Doctor> doctors = new ArrayList<>();
        List<Map<String,Object>> appointments = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql); ResultSet rows = ps.executeQuery()) {
            while (rows.next()) {
                switch (rows.getString("row_type")) {
                    case "PATIENT" -> {
                        Patient patient = new Patient();
                        patient.setPatientId(rows.getInt("patient_id"));
                        patient.setFullName(rows.getString("display_name"));
                        patient.setPhone(rows.getString("phone"));
                        patient.setDiabetesType(rows.getString("diabetes_type"));
                        patients.add(patient);
                    }
                    case "DOCTOR" -> {
                        Doctor doctor = new Doctor();
                        doctor.setDoctorId(rows.getInt("doctor_id"));
                        doctor.setFullName(rows.getString("display_name"));
                        doctor.setSpecialty(rows.getString("specialty"));
                        doctor.setDiabetesFocus(rows.getString("diabetes_focus"));
                        doctors.add(doctor);
                    }
                    default -> {
                        Map<String,Object> appointment = new LinkedHashMap<>();
                        appointment.put("appointment_id", rows.getObject("appointment_id"));
                        appointment.put("appointment_at", rows.getObject("appointment_at"));
                        appointment.put("preferred_date", rows.getObject("preferred_date"));
                        appointment.put("preferred_period", rows.getString("preferred_period"));
                        appointment.put("patient_id", rows.getObject("patient_id"));
                        appointment.put("doctor_id", rows.getObject("doctor_id"));
                        appointment.put("patient_name", rows.getString("patient_name"));
                        appointment.put("patient_phone", rows.getString("patient_phone"));
                        appointment.put("diabetes_type", rows.getString("diabetes_type"));
                        appointment.put("doctor_name", rows.getString("doctor_name"));
                        appointment.put("reason", rows.getString("reason"));
                        appointment.put("note", rows.getString("note"));
                        appointment.put("status", rows.getString("status"));
                        appointments.add(appointment);
                    }
                }
            }
        } catch (SQLException error) {
            throw databaseError("load appointment operations page", error);
        }
        return new AppointmentOperationsPageData(patients, doctors, appointments);
    }

    public record AppointmentOperationsPageData(List<Patient> patients, List<Doctor> doctors,
            List<Map<String,Object>> appointments) {}

    public void createAppointmentRequest(int patientId, LocalDate preferredDate,
            String preferredPeriod, String reason, String note, int actor) {
        inTransaction("Không thể gửi yêu cầu khám.", () -> {
            try (PreparedStatement lock = connection.prepareStatement("""
                    SELECT p.patient_id FROM patients p
                    JOIN users u ON u.user_id=p.user_id
                    WHERE p.patient_id=? AND u.status='ACTIVE'
                    FOR UPDATE OF p""")) {
                lock.setInt(1, patientId);
                try (ResultSet rows = lock.executeQuery()) {
                    if (!rows.next()) throw new IllegalArgumentException("Bệnh nhân không tồn tại hoặc tài khoản đã bị khóa.");
                }
            }
            Map<String,Object> requestCapacity = query("""
                    SELECT COUNT(*) FILTER (WHERE preferred_date=?) same_day,
                           COUNT(*) FILTER (WHERE preferred_date>=CURRENT_DATE) active_future
                    FROM appointments
                    WHERE patient_id=?
                      AND status IN ('REQUESTED','BOOKED','CONFIRMED','CHECKED_IN')""",
                    java.sql.Date.valueOf(preferredDate), patientId).get(0);
            AppointmentRules.validatePatientRequestCapacity(
                    ((Number) requestCapacity.get("same_day")).longValue(),
                    ((Number) requestCapacity.get("active_future")).longValue());
            update("""
                    INSERT INTO appointments(patient_id,preferred_date,preferred_period,reason,note,status,created_by)
                    VALUES(?,?,?,?,?,'REQUESTED',?)""",
                    patientId, java.sql.Date.valueOf(preferredDate), preferredPeriod, reason, note, actor);
            audit(actor, "REQUEST", "APPOINTMENT", null,
                    preferredDate + " " + preferredPeriod + " - " + reason);
        });
    }

    public void assignAppointmentRequest(int appointmentId, int doctorId,
            LocalDateTime at, String note, int actor) {
        inTransaction("Không thể xác nhận yêu cầu khám.", () -> {
            List<Map<String,Object>> rows = query("""
                    SELECT patient_id,preferred_date,preferred_period,status
                    FROM appointments WHERE appointment_id=? FOR UPDATE""", appointmentId);
            if (rows.isEmpty()) throw new IllegalArgumentException("Yêu cầu khám không tồn tại.");
            Map<String,Object> request = rows.get(0);
            if (!"REQUESTED".equals(String.valueOf(request.get("status")))) {
                throw new IllegalArgumentException("Yêu cầu này đã được xử lý.");
            }
            Object rawPreferredDate = request.get("preferred_date");
            LocalDate preferredDate = rawPreferredDate instanceof LocalDate localDate
                    ? localDate
                    : ((java.sql.Date) rawPreferredDate).toLocalDate();
            AppointmentRules.validateAssignmentMatchesRequest(
                    at, preferredDate, String.valueOf(request.get("preferred_period")));
            int patientId = ((Number) request.get("patient_id")).intValue();
            lockAndValidateCapacity(patientId, doctorId, at, appointmentId);
            update("""
                    UPDATE appointments
                    SET doctor_id=?,appointment_at=?,status='CONFIRMED',
                        note=CASE WHEN ? IS NULL OR ?='' THEN note ELSE ? END
                    WHERE appointment_id=?""", doctorId, at, note, note, note, appointmentId);
            audit(actor, "CONFIRM", "APPOINTMENT", String.valueOf(appointmentId), at.toString());
        });
    }

    public void createAppointment(int patientId, int doctorId, LocalDateTime at,
            String reason, String note, int actor) {
        AppointmentRules.validate(at, AppointmentRules.nowInVietnam());
        inTransaction("Không thể tạo lịch hẹn.", () -> {
            lockAndValidateCapacity(patientId, doctorId, at, null);
            String period = AppointmentRules.periodOf(at.toLocalTime());
            update("""
                    INSERT INTO appointments(patient_id,doctor_id,appointment_at,preferred_date,
                                             preferred_period,reason,note,created_by)
                    VALUES(?,?,?,?,?,?,?,?)""",
                    patientId, doctorId, at, java.sql.Date.valueOf(at.toLocalDate()),
                    period, reason, note, actor);
            audit(actor, "CREATE", "APPOINTMENT", null, reason);
        });
    }

    public void rescheduleAppointment(int appointmentId, LocalDateTime newTime,
            String note, int actor) {
        AppointmentRules.validate(newTime, AppointmentRules.nowInVietnam());
        inTransaction("Không thể đổi lịch hẹn.", () -> {
            List<Map<String, Object>> rows = query("""
                    SELECT patient_id,doctor_id,status FROM appointments
                    WHERE appointment_id=? FOR UPDATE""", appointmentId);
            if (rows.isEmpty()) throw new IllegalArgumentException("Lịch hẹn không tồn tại.");
            Map<String, Object> appointment = rows.get(0);
            String status = String.valueOf(appointment.get("status"));
            if (NON_RESCHEDULABLE_APPOINTMENT_STATUSES.contains(status)) {
                throw new IllegalArgumentException("Lịch hẹn này không thể đổi giờ.");
            }
            int patientId = ((Number) appointment.get("patient_id")).intValue();
            int doctorId = ((Number) appointment.get("doctor_id")).intValue();
            lockAndValidateCapacity(patientId, doctorId, newTime, appointmentId);
            update("""
                    UPDATE appointments SET appointment_at=?,status='CONFIRMED',
                        note=CASE WHEN ? IS NULL OR ?='' THEN note ELSE ? END
                    WHERE appointment_id=?""",
                    newTime, note, note, note, appointmentId);
            audit(actor, "RESCHEDULE", "APPOINTMENT",
                    String.valueOf(appointmentId), newTime.toString());
        });
    }

    public void setAppointmentStatus(int appointmentId, String status, int actor) {
        if (!Set.of("CONFIRMED", "CANCELLED", "NO_SHOW").contains(status)) {
            throw new IllegalArgumentException("Trạng thái lịch hẹn không hợp lệ.");
        }
        try {
            String timeRule = "NO_SHOW".equals(status)
                    ? " AND appointment_at<=CURRENT_TIMESTAMP" : "";
            String assignmentRule = "CONFIRMED".equals(status)
                    ? " AND doctor_id IS NOT NULL AND appointment_at IS NOT NULL"
                    : "";
            int changed = update("""
                    UPDATE appointments SET status=? WHERE appointment_id=?
                    AND status NOT IN ('CHECKED_IN','COMPLETED','CANCELLED','NO_SHOW')"""
                    + timeRule + assignmentRule, status, appointmentId);
            if (changed != 1) {
                throw new IllegalArgumentException("NO_SHOW".equals(status)
                        ? "Chỉ đánh dấu vắng hẹn sau khi đã qua giờ khám."
                        : "Lịch hẹn không tồn tại hoặc đã được xử lý.");
            }
            audit(actor, "STATUS", "APPOINTMENT", String.valueOf(appointmentId), status);
        } catch (SQLException error) {
            throw new IllegalStateException("Không thể cập nhật lịch hẹn.", error);
        }
    }

    public void cancelOwnAppointment(int appointmentId, int patientUserId, int actor) {
        try {
            int changed = update("""
                    UPDATE appointments a SET status='CANCELLED'
                    FROM patients p
                    WHERE a.patient_id=p.patient_id AND a.appointment_id=? AND p.user_id=?
                      AND a.status IN ('REQUESTED','BOOKED','CONFIRMED')
                      AND (a.status='REQUESTED' OR a.appointment_at>CURRENT_TIMESTAMP)""",
                    appointmentId, patientUserId);
            if (changed != 1) throw new IllegalArgumentException(
                    "Chỉ có thể hủy lịch sắp tới thuộc tài khoản của bạn.");
            audit(actor, "CANCEL", "APPOINTMENT",
                    String.valueOf(appointmentId), "patient self-cancel");
        } catch (SQLException error) {
            throw new IllegalStateException("Không thể hủy lịch hẹn.", error);
        }
    }

    private void lockAndValidateCapacity(int patientId, int doctorId, LocalDateTime at,
            Integer excludedId) throws SQLException {
        try (PreparedStatement lock = connection.prepareStatement("""
                SELECT d.doctor_id,d.diabetes_focus,
                       COALESCE(dp.diabetes_type,'UNKNOWN') diabetes_type
                FROM doctors d
                JOIN users du ON du.user_id=d.user_id AND du.status='ACTIVE'
                CROSS JOIN patients p
                LEFT JOIN users pu ON pu.user_id=p.user_id
                LEFT JOIN diabetes_profiles dp ON dp.patient_id=p.patient_id
                WHERE d.doctor_id=? AND p.patient_id=?
                  AND COALESCE(pu.status,'ACTIVE')='ACTIVE'
                FOR UPDATE OF d,p""")) {
            lock.setInt(1, doctorId);
            lock.setInt(2, patientId);
            try (ResultSet rs = lock.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Bệnh nhân hoặc bác sĩ không tồn tại hay đã ngừng hoạt động.");
                }
                String diabetesType = rs.getString("diabetes_type");
                String doctorFocus = rs.getString("diabetes_focus");
                if (!"UNKNOWN".equals(diabetesType) && !"BOTH".equals(doctorFocus)
                        && !diabetesType.equals(doctorFocus)) {
                    throw new IllegalArgumentException("Bệnh nhân "
                            + ("TYPE_1".equals(diabetesType) ? "típ 1" : "típ 2")
                            + " cần được phân công bác sĩ phù hợp với loại đái tháo đường.");
                }
            }
        }
        String excluded = excludedId == null ? "" : " AND appointment_id<>?";
        java.sql.Date date = java.sql.Date.valueOf(at.toLocalDate());
        String period = AppointmentRules.periodOf(at.toLocalTime());
        List<Object> arguments = new ArrayList<>(List.of(
                doctorId, at, patientId, date, doctorId, date, period, doctorId, date));
        if (excludedId != null) arguments.add(excludedId);
        Map<String, Object> capacity = query("""
                SELECT COUNT(*) FILTER (WHERE doctor_id=? AND appointment_at=?) doctor_slot,
                       COUNT(*) FILTER (WHERE patient_id=? AND preferred_date=?) patient_day,
                       COUNT(*) FILTER (WHERE doctor_id=? AND preferred_date=? AND preferred_period=?) doctor_period,
                       COUNT(*) FILTER (WHERE doctor_id=? AND preferred_date=?) doctor_day
                FROM appointments
                WHERE status NOT IN ('CANCELLED','NO_SHOW')""" + excluded,
                arguments.toArray()).get(0);
        AppointmentRules.validateCapacity(
                ((Number) capacity.get("doctor_slot")).longValue(),
                ((Number) capacity.get("patient_day")).longValue(),
                ((Number) capacity.get("doctor_period")).longValue(),
                ((Number) capacity.get("doctor_day")).longValue());
    }

    public void checkIn(int appointmentId, int actor) {
        inTransaction("Ghi nhận đến khám thất bại", () -> {
            Map<String, Object> appointment = lockAppointment(appointmentId);
            validateCheckIn(appointment);

            update("UPDATE appointments SET status='CHECKED_IN' WHERE appointment_id=?", appointmentId);
            int encounterId = insertEncounter(appointmentId, appointment, actor);
            int queueNo = nextQueueNumber();
            update(
                    "INSERT INTO queue_entries(encounter_id,doctor_id,queue_number) VALUES(?,?,?)",
                    encounterId, appointment.get("doctor_id"), queueNo);
            audit(actor, "CHECK_IN", "ENCOUNTER", String.valueOf(encounterId),
                    "queue=" + queueNo);
        });
    }

    private Map<String, Object> lockAppointment(int appointmentId) {
        List<Map<String, Object>> rows = query(
                "SELECT patient_id,doctor_id,appointment_at,status FROM appointments WHERE appointment_id=? FOR UPDATE",
                appointmentId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Lịch hẹn không tồn tại.");
        }
        return rows.get(0);
    }

    private void validateCheckIn(Map<String, Object> appointment) {
        String status = String.valueOf(appointment.get("status"));
        if (!CHECK_IN_STATUSES.contains(status) || appointment.get("doctor_id") == null) {
            throw new IllegalArgumentException(
                    "Lịch phải được phân bác sĩ và xác nhận trước khi ghi nhận người bệnh đến khám.");
        }
        Object rawAppointmentAt = appointment.get("appointment_at");
        LocalDateTime appointmentAt = rawAppointmentAt instanceof Timestamp timestamp
                ? timestamp.toLocalDateTime() : (LocalDateTime) rawAppointmentAt;
        AppointmentRules.validateCheckInDate(
                appointmentAt, AppointmentRules.nowInVietnam().toLocalDate());
    }

    private int insertEncounter(
            int appointmentId, Map<String, Object> appointment, int actor) throws SQLException {
        String sql = """
                INSERT INTO encounters(appointment_id,patient_id,doctor_id,created_by)
                VALUES(?,?,?,?) RETURNING encounter_id""";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, appointmentId);
            statement.setObject(2, appointment.get("patient_id"));
            statement.setObject(3, appointment.get("doctor_id"));
            statement.setInt(4, actor);
            try (ResultSet rows = statement.executeQuery()) {
                rows.next();
                return rows.getInt(1);
            }
        }
    }

    /**
     * Serialize queue-number allocation so two concurrent check-ins cannot receive
     * the same number. The lock is held only for the current short transaction.
     */
    private int nextQueueNumber() throws SQLException {
        try (Statement lock = connection.createStatement()) {
            lock.execute("LOCK TABLE queue_entries IN SHARE ROW EXCLUSIVE MODE");
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COALESCE(MAX(queue_number),0)+1
                FROM queue_entries
                WHERE queued_at>=CURRENT_DATE
                  AND queued_at<CURRENT_DATE+INTERVAL '1 day'""")) {
            try (ResultSet rows = statement.executeQuery()) {
                rows.next();
                return rows.getInt(1);
            }
        }
    }

    private void rollbackQuietly() {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // Preserve the original database exception.
        }
    }

    private void restoreAutoCommit() {
        try {
            connection.setAutoCommit(true);
        } catch (SQLException ignored) {
            // Connection lifecycle is handled by DBContext/request filter.
        }
    }
    public List<Map<String,Object>> encounters() {
        return query("""
          SELECT e.*,p.full_name patient_name,p.phone patient_phone,u.full_name doctor_name,
                 COALESCE(dp.diabetes_type,'UNKNOWN') diabetes_type,
                 q.queue_id,q.queue_number,q.priority queue_priority,q.status queue_status,m.record_id
          FROM encounters e JOIN patients p ON p.patient_id=e.patient_id
          LEFT JOIN diabetes_profiles dp ON dp.patient_id=p.patient_id
          JOIN doctors d ON d.doctor_id=e.doctor_id JOIN users u ON u.user_id=d.user_id
          LEFT JOIN queue_entries q ON q.encounter_id=e.encounter_id
          LEFT JOIN medicalrecords m ON m.encounter_id=e.encounter_id
          WHERE e.status NOT IN ('COMPLETED','CANCELLED')
          ORDER BY e.check_in_at DESC LIMIT 50""");
    }
    public List<Map<String,Object>> encountersForDoctor(int doctorId) {
        return query("""
          SELECT e.*,p.full_name patient_name,p.phone patient_phone,u.full_name doctor_name,
                 COALESCE(dp.diabetes_type,'UNKNOWN') diabetes_type,
                 q.queue_id,q.queue_number,q.priority queue_priority,q.status queue_status,m.record_id
          FROM encounters e JOIN patients p ON p.patient_id=e.patient_id
          LEFT JOIN diabetes_profiles dp ON dp.patient_id=p.patient_id
          JOIN doctors d ON d.doctor_id=e.doctor_id JOIN users u ON u.user_id=d.user_id
          LEFT JOIN queue_entries q ON q.encounter_id=e.encounter_id
          LEFT JOIN medicalrecords m ON m.encounter_id=e.encounter_id
          WHERE e.doctor_id=? AND e.status NOT IN ('COMPLETED','CANCELLED')
          ORDER BY e.check_in_at DESC LIMIT 50""", doctorId);
    }
    public void setEncounterStatus(int encounterId,String status,int actor) {
        if (!ENCOUNTER_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ");
        }
        inTransaction("Không thể cập nhật trạng thái lượt khám.", () -> {
            List<Map<String, Object>> rows = query(
                    "SELECT status FROM encounters WHERE encounter_id=? FOR UPDATE", encounterId);
            if (rows.isEmpty()) throw new IllegalArgumentException("Lượt khám không tồn tại.");
            String current = String.valueOf(rows.get(0).get("status"));
            if (!validEncounterTransition(current, status)) {
                throw new IllegalArgumentException(
                        "Không thể chuyển lượt khám từ " + current + " sang " + status + ".");
            }
            String timestampUpdate = encounterTimestampUpdate(status);
            if (update("UPDATE encounters SET status=?" + timestampUpdate
                    + " WHERE encounter_id=?", status, encounterId) != 1) {
                throw new IllegalArgumentException("Lượt khám không tồn tại.");
            }

            if ("COMPLETED".equals(status) || "CANCELLED".equals(status)) {
                update("""
                        UPDATE appointments a SET status=?
                        FROM encounters e
                        WHERE e.encounter_id=? AND a.appointment_id=e.appointment_id""",
                        status, encounterId);
            }

            String queueStatus = queueStatusFor(status);
            if (queueStatus != null) {
                update("""
                        UPDATE queue_entries
                        SET status=?,called_at=COALESCE(called_at,CURRENT_TIMESTAMP)
                        WHERE encounter_id=?""", queueStatus, encounterId);
            }
            audit(actor, "STATUS", "ENCOUNTER", String.valueOf(encounterId), status);
        });
    }

    private boolean validEncounterTransition(String current, String target) {
        if (current.equals(target)) return true;
        return switch (target) {
            case "WAITING_DOCTOR" -> "WAITING_TRIAGE".equals(current);
            case "IN_CONSULTATION" -> Set.of("WAITING_DOCTOR", "LAB_COMPLETED").contains(current);
            case "WAITING_LAB" -> "IN_CONSULTATION".equals(current);
            case "LAB_COMPLETED" -> "WAITING_LAB".equals(current);
            case "COMPLETED" -> Set.of(
                    "WAITING_DOCTOR", "IN_CONSULTATION", "LAB_COMPLETED").contains(current);
            case "CANCELLED" -> !Set.of("COMPLETED", "CANCELLED").contains(current);
            default -> false;
        };
    }

    private String encounterTimestampUpdate(String status) {
        return switch (status) {
            case "IN_CONSULTATION" -> ",consultation_started_at=CURRENT_TIMESTAMP";
            case "COMPLETED" -> ",completed_at=CURRENT_TIMESTAMP";
            default -> "";
        };
    }

    private String queueStatusFor(String encounterStatus) {
        return switch (encounterStatus) {
            case "IN_CONSULTATION" -> "IN_SERVICE";
            case "COMPLETED" -> "COMPLETED";
            default -> null;
        };
    }

    /** Loads the selected patient, allergies and history in one database round-trip. */
    public ClinicalPatientData loadClinicalPatient(int patientId) {
        String sql = """
                WITH subject AS (
                  SELECT patient_id,full_name,phone FROM patients WHERE patient_id=?
                )
                SELECT 'PATIENT' row_type,s.patient_id,s.full_name,s.phone,
                       NULL::INTEGER item_id,NULL::VARCHAR item_name,NULL::VARCHAR item_type,
                       NULL::VARCHAR item_status,NULL::VARCHAR item_note,NULL::TIMESTAMPTZ item_date
                FROM subject s
                UNION ALL
                SELECT 'ALLERGY',s.patient_id,s.full_name,s.phone,a.allergy_id,a.allergen,NULL,
                       a.severity,a.reaction,a.noted_at
                FROM subject s JOIN patient_allergies a ON a.patient_id=s.patient_id
                UNION ALL
                SELECT 'HISTORY',s.patient_id,s.full_name,s.phone,h.history_id,h.condition_name,
                       h.history_type,h.status,h.note,h.noted_at
                FROM subject s JOIN patient_medical_histories h ON h.patient_id=s.patient_id
                ORDER BY row_type,item_date DESC NULLS LAST
                """;
        Patient patient = null;
        List<Map<String, Object>> allergies = new ArrayList<>();
        List<Map<String, Object>> histories = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, patientId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    if (patient == null) {
                        patient = new Patient();
                        patient.setPatientId(rows.getInt("patient_id"));
                        patient.setFullName(rows.getString("full_name"));
                        patient.setPhone(rows.getString("phone"));
                    }
                    if ("ALLERGY".equals(rows.getString("row_type"))) {
                        Map<String, Object> allergy = new LinkedHashMap<>();
                        allergy.put("allergy_id", rows.getObject("item_id"));
                        allergy.put("allergen", rows.getString("item_name"));
                        allergy.put("severity", rows.getString("item_status"));
                        allergy.put("reaction", rows.getString("item_note"));
                        allergies.add(allergy);
                    } else if ("HISTORY".equals(rows.getString("row_type"))) {
                        Map<String, Object> history = new LinkedHashMap<>();
                        history.put("history_id", rows.getObject("item_id"));
                        history.put("condition_name", rows.getString("item_name"));
                        history.put("history_type", rows.getString("item_type"));
                        history.put("status", rows.getString("item_status"));
                        history.put("note", rows.getString("item_note"));
                        histories.add(history);
                    }
                }
            }
        } catch (SQLException error) {
            throw databaseError("load clinical patient", error);
        }
        return new ClinicalPatientData(patient, allergies, histories);
    }

    public record ClinicalPatientData(Patient patient, List<Map<String, Object>> allergies,
            List<Map<String, Object>> histories) {}

    public void addAllergy(
            int patientId, String allergen, String reaction, String severity, int actor) {
        try {
            update("""
                    INSERT INTO patient_allergies(patient_id,allergen,reaction,severity,noted_by)
                    VALUES(?,?,?,?,?)
                    ON CONFLICT(patient_id,allergen) DO UPDATE
                    SET reaction=EXCLUDED.reaction,severity=EXCLUDED.severity,status='ACTIVE'""",
                    patientId, allergen, reaction, severity, actor);
            audit(actor, "UPSERT", "ALLERGY", null, allergen);
        } catch (SQLException error) {
            throw new IllegalStateException("Không thể lưu thông tin dị ứng.", error);
        }
    }

    public void addHistory(
            int patientId, String type, String name, java.sql.Date date,
            String status, String note, int actor) {
        try {
            update("""
                    INSERT INTO patient_medical_histories(
                        patient_id,history_type,condition_name,diagnosed_date,status,note,noted_by)
                    VALUES(?,?,?,?,?,?,?)""",
                    patientId, type, name, date, status, note, actor);
            audit(actor, "CREATE", "MEDICAL_HISTORY", null, name);
        } catch (SQLException error) {
            throw new IllegalStateException("Không thể lưu tiền sử bệnh.", error);
        }
    }
    public List<Map<String, Object>> labOrders() {
        return query("""
                SELECT l.*,m.record_id,p.full_name patient_name,u.full_name doctor_name,
                       COALESCE(dp.diabetes_type,'UNKNOWN') diabetes_type FROM lab_orders l
                JOIN patients p ON p.patient_id=l.patient_id
                LEFT JOIN diabetes_profiles dp ON dp.patient_id=p.patient_id
                JOIN doctors d ON d.doctor_id=l.doctor_id
                JOIN users u ON u.user_id=d.user_id
                LEFT JOIN medicalrecords m ON m.encounter_id=l.encounter_id
                WHERE l.status<>'CANCELLED'
                ORDER BY l.ordered_at DESC LIMIT 50""");
    }

    public List<Map<String, Object>> labOrdersForDoctor(int doctorId) {
        return query("""
                SELECT l.*,m.record_id,p.full_name patient_name,u.full_name doctor_name,
                       COALESCE(dp.diabetes_type,'UNKNOWN') diabetes_type FROM lab_orders l
                JOIN patients p ON p.patient_id=l.patient_id
                LEFT JOIN diabetes_profiles dp ON dp.patient_id=p.patient_id
                JOIN doctors d ON d.doctor_id=l.doctor_id
                JOIN users u ON u.user_id=d.user_id
                LEFT JOIN medicalrecords m ON m.encounter_id=l.encounter_id
                WHERE l.doctor_id=? AND l.status<>'CANCELLED'
                ORDER BY l.ordered_at DESC LIMIT 50""", doctorId);
    }

    /** Lists one option per active medical record that still has structured tests to result. */
    public List<Map<String, Object>> labImportRecords() {
        return query("""
                SELECT m.record_id,p.full_name patient_name,
                       COALESCE(dp.diabetes_type,'UNKNOWN') diabetes_type,
                       STRING_AGG(DISTINCT l.test_code, ', ' ORDER BY l.test_code) pending_tests
                FROM lab_orders l
                JOIN encounters e ON e.encounter_id=l.encounter_id
                JOIN medicalrecords m ON m.encounter_id=e.encounter_id
                JOIN patients p ON p.patient_id=e.patient_id
                LEFT JOIN diabetes_profiles dp ON dp.patient_id=p.patient_id
                WHERE l.status IN ('ORDERED','COLLECTED')
                  AND l.test_code IN ('GLU','GLU_FASTING','HBA1C','LIPID')
                  AND e.status NOT IN ('COMPLETED','CANCELLED')
                GROUP BY m.record_id,p.full_name,dp.diabetes_type
                ORDER BY m.record_id DESC LIMIT 50""");
    }

    public void createLabOrder(
            int encounterId, int doctorId, String code, String name,
            String priority, String note, int actor) {
        inTransaction("Không thể tạo chỉ định xét nghiệm.", () -> {
            List<Map<String, Object>> encounters = query("""
                    SELECT patient_id,status FROM encounters
                    WHERE encounter_id=? AND doctor_id=? FOR UPDATE""", encounterId, doctorId);
            if (encounters.isEmpty() || !Set.of("IN_CONSULTATION", "WAITING_LAB")
                    .contains(String.valueOf(encounters.get(0).get("status")))) {
                throw new IllegalArgumentException(
                        "Bác sĩ cần bắt đầu khám trước khi tạo chỉ định xét nghiệm.");
            }
            if (!query("""
                    SELECT 1 ok FROM lab_orders
                    WHERE encounter_id=? AND test_code=? AND status<>'CANCELLED' LIMIT 1""",
                    encounterId, code).isEmpty()) {
                throw new IllegalArgumentException(
                        "Xét nghiệm này đã được chỉ định cho lượt khám.");
            }
            update("""
                    INSERT INTO lab_orders(
                        encounter_id,patient_id,doctor_id,test_code,test_name,priority,clinical_note)
                    VALUES(?,?,?,?,?,?,?)""",
                    encounterId, encounters.get(0).get("patient_id"), doctorId,
                    code, name, priority, note);
            update("UPDATE encounters SET status='WAITING_LAB' WHERE encounter_id=? AND doctor_id=?",
                    encounterId, doctorId);
            audit(actor, "CREATE", "LAB_ORDER", null, code);
        });
    }

    public void resultLab(
            int orderId, String value, String unit, String range, String flag, int actor) {
        try {
            int changed = update("""
                    UPDATE lab_orders
                    SET result_value=?,result_unit=?,reference_range=?,result_flag=?,resulted_by=?,
                        resulted_at=CURRENT_TIMESTAMP,status='RESULTED'
                    WHERE lab_order_id=? AND status IN ('ORDERED','COLLECTED')""",
                    value, unit, range, flag, actor, orderId);
            if (changed != 1) {
                throw new IllegalArgumentException("Chỉ định không tồn tại hoặc không thể trả kết quả");
            }
            audit(actor, "RESULT", "LAB_ORDER", String.valueOf(orderId), value + " " + unit);
        } catch (SQLException error) {
            throw new IllegalStateException("Không thể lưu kết quả xét nghiệm.", error);
        }
    }

    /** Maps the structured result form to the doctor's existing orders for the visit. */
    public void resultStructuredLabs(int recordId, int actor, Double bloodGlucose, Double hba1c,
            Double cholesterol, Double triglyceride, Double hdlC, Double ldlC) {
        int encounterId = openEncounterForRecord(recordId);
        LabResultImportRow row = new LabResultImportRow(1, recordId, bloodGlucose, hba1c,
                cholesterol, triglyceride, hdlC, ldlC);
        inTransaction("Không thể lưu kết quả xét nghiệm.", () -> {
            validateImportOrders(row, encounterId);
            saveImportedHealthIndicators(row, actor);
            saveStructuredResults(recordId, encounterId, actor, bloodGlucose, hba1c,
                    cholesterol, triglyceride, hdlC, ldlC);
        });
    }

    /** Imports all rows in one transaction so a bad database mapping cannot leave a half-imported file. */
    public int importStructuredLabResults(List<LabResultImportRow> rows, int actor) {
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("File chưa có dòng kết quả để import.");
        }
        inTransaction("Không thể import kết quả xét nghiệm.", () -> {
            for (LabResultImportRow row : rows) {
                int encounterId;
                try {
                    encounterId = openEncounterForRecord(row.recordId());
                    validateImportOrders(row, encounterId);
                    saveImportedHealthIndicators(row, actor);
                    saveStructuredResults(row.recordId(), encounterId, actor,
                            row.bloodGlucose(), row.hba1c(), row.cholesterol(),
                            row.triglyceride(), row.hdlC(), row.ldlC());
                } catch (IllegalArgumentException error) {
                    throw new IllegalArgumentException(
                            "Dòng " + row.lineNumber() + ": " + error.getMessage(), error);
                }
            }
            audit(actor, "IMPORT", "LAB_RESULT_FILE", null,
                    "Import " + rows.size() + " dòng kết quả xét nghiệm");
        });
        return rows.size();
    }

    private int openEncounterForRecord(int recordId) {
        List<Map<String, Object>> records = query("""
                SELECT r.encounter_id FROM medicalrecords r
                JOIN encounters e ON e.encounter_id=r.encounter_id
                WHERE r.record_id=? AND e.status='WAITING_LAB' AND EXISTS (
                    SELECT 1 FROM lab_orders l WHERE l.encounter_id=r.encounter_id
                      AND l.status IN ('ORDERED','COLLECTED'))""", recordId);
        if (records.isEmpty() || records.get(0).get("encounter_id") == null) {
            throw new IllegalArgumentException(
                    "Bác sĩ chưa tạo chỉ định xét nghiệm hoặc chỉ định đã được xác nhận.");
        }
        return ((Number) records.get(0).get("encounter_id")).intValue();
    }

    private void validateImportOrders(LabResultImportRow row, int encounterId) {
        Set<String> codes = new HashSet<>();
        for (Map<String, Object> order : query("""
                SELECT test_code FROM lab_orders
                WHERE encounter_id=? AND status IN ('ORDERED','COLLECTED')""", encounterId)) {
            codes.add(String.valueOf(order.get("test_code")).toUpperCase(Locale.ROOT));
        }
        if (row.bloodGlucose() != null && !hasAny(codes, "GLU", "GLU_FASTING")) {
            throw new IllegalArgumentException("Chưa có chỉ định Đường huyết cho bệnh án này.");
        }
        if (row.hba1c() != null && !codes.contains("HBA1C")) {
            throw new IllegalArgumentException("Chưa có chỉ định HbA1c cho bệnh án này.");
        }
        if ((row.cholesterol() != null || row.triglyceride() != null
                || row.hdlC() != null || row.ldlC() != null) && !codes.contains("LIPID")) {
            throw new IllegalArgumentException("Chưa có chỉ định bộ mỡ máu cho bệnh án này.");
        }
    }

    private boolean hasAny(Set<String> values, String... expected) {
        for (String value : expected) if (values.contains(value)) return true;
        return false;
    }

    private void saveImportedHealthIndicators(LabResultImportRow row, int actor)
            throws SQLException {
        String sql = """
                INSERT INTO healthindicators(
                    record_id,entered_by_staff,blood_glucose,hba1c,cholesterol,triglyceride,hdl_c,ldl_c)
                VALUES(?,?,?,?,?,?,?,?)
                ON CONFLICT(record_id) DO UPDATE SET
                    entered_by_staff=EXCLUDED.entered_by_staff,
                    blood_glucose=COALESCE(EXCLUDED.blood_glucose,healthindicators.blood_glucose),
                    hba1c=COALESCE(EXCLUDED.hba1c,healthindicators.hba1c),
                    cholesterol=COALESCE(EXCLUDED.cholesterol,healthindicators.cholesterol),
                    triglyceride=COALESCE(EXCLUDED.triglyceride,healthindicators.triglyceride),
                    hdl_c=COALESCE(EXCLUDED.hdl_c,healthindicators.hdl_c),
                    ldl_c=COALESCE(EXCLUDED.ldl_c,healthindicators.ldl_c),
                    measured_at=CURRENT_TIMESTAMP""";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, row.recordId());
            statement.setInt(2, actor);
            setImportNumber(statement, 3, row.bloodGlucose());
            setImportNumber(statement, 4, row.hba1c());
            setImportNumber(statement, 5, row.cholesterol());
            setImportNumber(statement, 6, row.triglyceride());
            setImportNumber(statement, 7, row.hdlC());
            setImportNumber(statement, 8, row.ldlC());
            statement.executeUpdate();
        }
    }

    private void setImportNumber(PreparedStatement statement, int index, Double value)
            throws SQLException {
        if (value == null) statement.setNull(index, Types.NUMERIC);
        else statement.setDouble(index, value);
    }

    private void saveStructuredResults(int recordId, int encounterId, int actor,
            Double bloodGlucose, Double hba1c, Double cholesterol, Double triglyceride,
            Double hdlC, Double ldlC) throws SQLException {
        saveStructuredOrder(encounterId, actor, bloodGlucose, "mmol/L", "3.9-7.0",
                flag(bloodGlucose, 3.9, 7.0), "GLU", "GLU_FASTING");
        saveStructuredOrder(encounterId, actor, hba1c, "%", "4.0-6.5",
                flag(hba1c, 4.0, 6.5), "HBA1C");

        String lipidValue = lipidSummary(cholesterol, triglyceride, hdlC, ldlC);
        if (lipidValue != null) {
            update("""
                    UPDATE lab_orders SET result_value=?,result_unit='mmol/L',
                        reference_range='Theo từng chỉ số',result_flag=?,resulted_by=?,
                        resulted_at=CURRENT_TIMESTAMP,status='RESULTED'
                    WHERE encounter_id=? AND test_code='LIPID'
                      AND status IN ('ORDERED','COLLECTED')""",
                    lipidValue, lipidFlag(cholesterol, triglyceride, hdlC, ldlC), actor, encounterId);
        }
        audit(actor, "RESULT", "LAB_ORDER", String.valueOf(encounterId),
                "Nhập bộ kết quả xét nghiệm từ bệnh án #" + recordId);
    }

    /** The encounter only becomes lab-complete after its assigned doctor reviews every result. */
    public void reviewLabResults(int encounterId, int doctorId, int actor) {
        inTransaction("Không thể xác nhận kết quả xét nghiệm.", () -> {
            List<Map<String, Object>> state = query("""
                    SELECT COUNT(*) FILTER (WHERE status IN ('ORDERED','COLLECTED')) pending,
                           COUNT(*) FILTER (WHERE status IN ('RESULTED','REVIEWED')) available
                    FROM lab_orders WHERE encounter_id=? AND doctor_id=?
                      AND status<>'CANCELLED'""", encounterId, doctorId);
            Number pending = (Number) state.get(0).get("pending");
            Number available = (Number) state.get(0).get("available");
            if (available.longValue() == 0) {
                throw new IllegalArgumentException("Chưa có kết quả xét nghiệm để xác nhận");
            }
            if (pending.longValue() > 0) {
                throw new IllegalArgumentException("Vẫn còn chỉ định chưa có kết quả");
            }
            update("""
                    UPDATE lab_orders SET status='REVIEWED'
                    WHERE encounter_id=? AND doctor_id=? AND status='RESULTED'""",
                    encounterId, doctorId);
            update("""
                    UPDATE encounters SET status='LAB_COMPLETED'
                    WHERE encounter_id=? AND doctor_id=? AND status<>'COMPLETED'""",
                    encounterId, doctorId);
            audit(actor, "REVIEW", "LAB_ORDER", String.valueOf(encounterId),
                    "Bác sĩ xác nhận kết quả xét nghiệm");
        });
    }

    public boolean hasUnreviewedLabOrders(int encounterId) {
        return !query("""
                SELECT 1 ok FROM lab_orders
                WHERE encounter_id=? AND status IN ('ORDERED','COLLECTED','RESULTED')
                LIMIT 1""", encounterId).isEmpty();
    }

    public boolean hasOpenLabOrdersForRecord(int recordId) {
        return !query("""
                SELECT 1 ok FROM medicalrecords r JOIN lab_orders l ON l.encounter_id=r.encounter_id
                WHERE r.record_id=? AND l.status IN ('ORDERED','COLLECTED') LIMIT 1""",
                recordId).isEmpty();
    }

    private void saveStructuredOrder(int encounterId, int actor, Double value, String unit,
            String range, String resultFlag, String... codes) throws SQLException {
        if (value == null) return;
        String placeholders = String.join(",", Collections.nCopies(codes.length, "?"));
        List<Object> parameters = new ArrayList<>();
        parameters.add(String.valueOf(value));
        parameters.add(unit);
        parameters.add(range);
        parameters.add(resultFlag);
        parameters.add(actor);
        parameters.add(encounterId);
        parameters.addAll(Arrays.asList(codes));
        String sql = "UPDATE lab_orders SET result_value=?,result_unit=?,reference_range=?,"
                + "result_flag=?,resulted_by=?,resulted_at=CURRENT_TIMESTAMP,status='RESULTED' "
                + "WHERE encounter_id=? AND test_code IN (" + placeholders + ") "
                + "AND status IN ('ORDERED','COLLECTED')";
        update(sql, parameters.toArray());
    }

    private String flag(Double value, double low, double high) {
        if (value == null) return "NORMAL";
        if (value < low) return "LOW";
        if (value > high) return "HIGH";
        return "NORMAL";
    }

    private String lipidSummary(Double cholesterol, Double triglyceride, Double hdlC, Double ldlC) {
        List<String> values = new ArrayList<>();
        if (cholesterol != null) values.add("Cholesterol " + cholesterol);
        if (triglyceride != null) values.add("Triglyceride " + triglyceride);
        if (hdlC != null) values.add("HDL-C " + hdlC);
        if (ldlC != null) values.add("LDL-C " + ldlC);
        return values.isEmpty() ? null : String.join("; ", values);
    }

    private String lipidFlag(Double cholesterol, Double triglyceride, Double hdlC, Double ldlC) {
        if ((cholesterol != null && cholesterol > 5.2)
                || (triglyceride != null && triglyceride > 1.7)
                || (ldlC != null && ldlC > 3.4)
                || (hdlC != null && hdlC < 1.0)) return "HIGH";
        return "NORMAL";
    }

    public Integer doctorIdForUser(int userId) {
        List<Map<String, Object>> rows = query("SELECT doctor_id FROM doctors WHERE user_id=?", userId);
        return rows.isEmpty() ? null : ((Number) rows.get(0).get("doctor_id")).intValue();
    }

    public int[] intakeAssignmentForEncounter(int encounterId) {
        List<Map<String, Object>> rows = query(
                "SELECT patient_id,doctor_id,status FROM encounters WHERE encounter_id=?", encounterId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Lượt khám không tồn tại");
        }
        Map<String, Object> assignment = rows.get(0);
        if (!"WAITING_TRIAGE".equals(String.valueOf(assignment.get("status")))) {
            throw new IllegalArgumentException(
                    "Lượt khám không còn ở bước tiếp nhận ban đầu");
        }
        return new int[] {
                ((Number) assignment.get("patient_id")).intValue(),
                ((Number) assignment.get("doctor_id")).intValue()
        };
    }

    public LocalDateTime appointmentTimeForEncounter(int encounterId) {
        List<Map<String, Object>> rows = query("""
                SELECT a.appointment_at FROM encounters e
                LEFT JOIN appointments a ON a.appointment_id=e.appointment_id
                WHERE e.encounter_id=?""", encounterId);
        if (rows.isEmpty() || rows.get(0).get("appointment_at") == null) {
            return null;
        }
        Object value = rows.get(0).get("appointment_at");
        return value instanceof Timestamp timestamp
                ? timestamp.toLocalDateTime()
                : (LocalDateTime) value;
    }

    public boolean doctorOwnsEncounter(int doctorId, int encounterId) {
        return !query(
                "SELECT 1 ok FROM encounters WHERE encounter_id=? AND doctor_id=?",
                encounterId, doctorId).isEmpty();
    }

    public boolean doctorHasPatient(int doctorId, int patientId) {
        return !query(
                "SELECT 1 ok FROM encounters WHERE doctor_id=? AND patient_id=? LIMIT 1",
                doctorId, patientId).isEmpty();
    }

    private void audit(int actor, String action, String type, String id, String details)
            throws SQLException {
        update("INSERT INTO audit_logs(user_id,action,entity_type,entity_id,details) VALUES(?,?,?,?,?)",
                actor, action, type, id, details);
    }
}
