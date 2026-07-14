package dal;

import models.NextAppointment;
import java.sql.*;
import java.time.LocalDate;

public class NextAppointmentDAO extends DBContext {
    PreparedStatement stm;
    ResultSet rs;

    private NextAppointment mapRow(ResultSet rs) throws SQLException {
        NextAppointment a = new NextAppointment();
        a.setAppointmentId(rs.getInt("appointment_id"));
        a.setPatientId(rs.getInt("patient_id"));
        Date d = rs.getDate("appointment_date");
        if (d != null) a.setAppointmentDate(d.toLocalDate());
        a.setSource(rs.getString("source"));
        Timestamp t = rs.getTimestamp("created_at");
        if (t != null) a.setCreatedAt(t.toLocalDateTime());
        return a;
    }

    /** Lịch tái khám sắp tới (>= hôm nay), ưu tiên DOCTOR > AUTO */
    public NextAppointment getUpcoming(int patientId) {
        String sql = "SELECT * FROM NextAppointment WHERE patient_id=?"
                   + " AND appointment_date >= CURRENT_DATE"
                   + " ORDER BY CASE source WHEN 'DOCTOR' THEN 0 ELSE 1 END, appointment_date ASC LIMIT 1";
        try {
            stm = connection.prepareStatement(sql);
            stm.setInt(1, patientId);
            rs = stm.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { System.out.println("NextAppt.getUpcoming: " + e.getMessage()); }
        return null;
    }

    /** Nếu chưa có lịch tương lai → tự tạo AUTO +7 ngày */
    public NextAppointment ensureAutoAppointment(int patientId) {
        NextAppointment ex = getUpcoming(patientId);
        if (ex != null) return ex;
        LocalDate next7 = LocalDate.now().plusDays(7);
        String sql = "INSERT INTO NextAppointment(patient_id,appointment_date,source) VALUES(?,?,?)";
        try {
            stm = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stm.setInt(1, patientId);
            stm.setDate(2, Date.valueOf(next7));
            stm.setString(3, "AUTO");
            stm.executeUpdate();
            rs = stm.getGeneratedKeys();
            if (rs.next()) {
                NextAppointment a = new NextAppointment();
                a.setAppointmentId(rs.getInt(1));
                a.setPatientId(patientId);
                a.setAppointmentDate(next7);
                a.setSource("AUTO");
                return a;
            }
        } catch (SQLException e) { System.out.println("NextAppt.ensureAuto: " + e.getMessage()); }
        return null;
    }
}
