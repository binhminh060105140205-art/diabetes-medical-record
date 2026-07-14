package dal;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class ClinicWorkflowDAO extends DBContext {
    private List<Map<String,Object>> query(String sql, Object... args) {
        List<Map<String,Object>> rows = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            bind(ps, args);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                while (rs.next()) {
                    Map<String,Object> row = new LinkedHashMap<>();
                    for (int i=1;i<=md.getColumnCount();i++) row.put(md.getColumnLabel(i), rs.getObject(i));
                    rows.add(row);
                }
            }
        } catch (SQLException e) { throw new IllegalStateException("Database query failed", e); }
        return rows;
    }

    private int update(String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) { bind(ps,args); return ps.executeUpdate(); }
    }
    private void bind(PreparedStatement ps, Object... args) throws SQLException {
        for (int i=0;i<args.length;i++) {
            Object v=args[i];
            if (v instanceof LocalDateTime t) ps.setTimestamp(i+1, Timestamp.valueOf(t)); else ps.setObject(i+1,v);
        }
    }

    public List<Map<String,Object>> appointments() {
        return query("""
          SELECT a.*, p.full_name patient_name, p.phone patient_phone, u.full_name doctor_name
          FROM appointments a JOIN patients p ON p.patient_id=a.patient_id
          JOIN doctors d ON d.doctor_id=a.doctor_id JOIN users u ON u.user_id=d.user_id
          ORDER BY a.appointment_at DESC LIMIT 50""");
    }
    public List<Map<String,Object>> appointmentsForDoctor(int doctorId) {
        return query("""
          SELECT a.*, p.full_name patient_name, p.phone patient_phone, u.full_name doctor_name
          FROM appointments a JOIN patients p ON p.patient_id=a.patient_id
          JOIN doctors d ON d.doctor_id=a.doctor_id JOIN users u ON u.user_id=d.user_id
          WHERE a.doctor_id=? ORDER BY a.appointment_at DESC LIMIT 50""", doctorId);
    }
    public void createAppointment(int patientId,int doctorId,LocalDateTime at,String reason,String note,int actor) {
        if (at == null || at.isBefore(LocalDateTime.now())) throw new IllegalArgumentException("Thời gian hẹn phải ở tương lai");
        try { update("INSERT INTO appointments(patient_id,doctor_id,appointment_at,reason,note,created_by) VALUES(?,?,?,?,?,?)",
                patientId,doctorId,at,reason,note,actor); audit(actor,"CREATE","APPOINTMENT",null,reason); }
        catch(SQLException e){ throw new IllegalStateException("Không thể tạo lịch hẹn; hãy kiểm tra khung giờ trùng",e); }
    }
    public void checkIn(int appointmentId,int actor) {
        try {
            connection.setAutoCommit(false);
            Map<String,Object> ap;
            List<Map<String,Object>> rows=query("SELECT patient_id,doctor_id,status FROM appointments WHERE appointment_id=? FOR UPDATE",appointmentId);
            if(rows.isEmpty()) throw new SQLException("Lịch hẹn không tồn tại"); ap=rows.get(0);
            if("CHECKED_IN".equals(ap.get("status"))) throw new SQLException("Lịch hẹn đã check-in");
            update("UPDATE appointments SET status='CHECKED_IN' WHERE appointment_id=?",appointmentId);
            int encounterId;
            try(PreparedStatement ps=connection.prepareStatement("INSERT INTO encounters(appointment_id,patient_id,doctor_id,created_by) VALUES(?,?,?,?) RETURNING encounter_id")){
                ps.setInt(1,appointmentId); ps.setObject(2,ap.get("patient_id")); ps.setObject(3,ap.get("doctor_id")); ps.setInt(4,actor);
                try(ResultSet rs=ps.executeQuery()){rs.next();encounterId=rs.getInt(1);}
            }
            int queueNo;
            try(PreparedStatement ps=connection.prepareStatement("SELECT COALESCE(MAX(queue_number),0)+1 FROM queue_entries WHERE queued_at::date=CURRENT_DATE")){
                try(ResultSet rs=ps.executeQuery()){rs.next();queueNo=rs.getInt(1);}
            }
            update("INSERT INTO queue_entries(encounter_id,doctor_id,queue_number) VALUES(?,?,?)",encounterId,ap.get("doctor_id"),queueNo);
            audit(actor,"CHECK_IN","ENCOUNTER",String.valueOf(encounterId),"queue="+queueNo);
            connection.commit();
        } catch(Exception e){ try{connection.rollback();}catch(SQLException ignored){} throw new IllegalStateException("Check-in thất bại",e); }
        finally { try{connection.setAutoCommit(true);}catch(SQLException ignored){} }
    }
    public List<Map<String,Object>> encounters() {
        return query("""
          SELECT e.*,p.full_name patient_name,p.phone patient_phone,u.full_name doctor_name,
                 q.queue_id,q.queue_number,q.priority queue_priority,q.status queue_status,m.record_id
          FROM encounters e JOIN patients p ON p.patient_id=e.patient_id
          JOIN doctors d ON d.doctor_id=e.doctor_id JOIN users u ON u.user_id=d.user_id
          LEFT JOIN queue_entries q ON q.encounter_id=e.encounter_id
          LEFT JOIN medicalrecords m ON m.encounter_id=e.encounter_id
          ORDER BY CASE WHEN e.status='COMPLETED' THEN 1 ELSE 0 END,e.check_in_at DESC LIMIT 50""");
    }
    public List<Map<String,Object>> encountersForDoctor(int doctorId) {
        return query("""
          SELECT e.*,p.full_name patient_name,p.phone patient_phone,u.full_name doctor_name,
                 q.queue_id,q.queue_number,q.priority queue_priority,q.status queue_status,m.record_id
          FROM encounters e JOIN patients p ON p.patient_id=e.patient_id
          JOIN doctors d ON d.doctor_id=e.doctor_id JOIN users u ON u.user_id=d.user_id
          LEFT JOIN queue_entries q ON q.encounter_id=e.encounter_id
          LEFT JOIN medicalrecords m ON m.encounter_id=e.encounter_id
          WHERE e.doctor_id=?
          ORDER BY CASE WHEN e.status='COMPLETED' THEN 1 ELSE 0 END,e.check_in_at DESC LIMIT 50""", doctorId);
    }
    public void setEncounterStatus(int encounterId,String status,int actor) {
        Set<String> allowed=Set.of("WAITING_TRIAGE","WAITING_DOCTOR","IN_CONSULTATION","WAITING_LAB","LAB_COMPLETED","COMPLETED","CANCELLED");
        if(!allowed.contains(status)) throw new IllegalArgumentException("Trạng thái không hợp lệ");
        try {
            String extra="IN_CONSULTATION".equals(status)?",consultation_started_at=CURRENT_TIMESTAMP":"COMPLETED".equals(status)?",completed_at=CURRENT_TIMESTAMP":"";
            update("UPDATE encounters SET status=?"+extra+" WHERE encounter_id=?",status,encounterId);
            String qs="IN_CONSULTATION".equals(status)?"IN_SERVICE":"COMPLETED".equals(status)?"COMPLETED":null;
            if(qs!=null) update("UPDATE queue_entries SET status=?,called_at=COALESCE(called_at,CURRENT_TIMESTAMP) WHERE encounter_id=?",qs,encounterId);
            audit(actor,"STATUS","ENCOUNTER",String.valueOf(encounterId),status);
        } catch(SQLException e){throw new IllegalStateException(e);}
    }
    public List<Map<String,Object>> allergies(int patientId){return query("SELECT * FROM patient_allergies WHERE patient_id=? ORDER BY noted_at DESC",patientId);}
    public List<Map<String,Object>> histories(int patientId){return query("SELECT * FROM patient_medical_histories WHERE patient_id=? ORDER BY noted_at DESC",patientId);}
    public void addAllergy(int patientId,String allergen,String reaction,String severity,int actor){
        try{update("INSERT INTO patient_allergies(patient_id,allergen,reaction,severity,noted_by) VALUES(?,?,?,?,?) ON CONFLICT(patient_id,allergen) DO UPDATE SET reaction=EXCLUDED.reaction,severity=EXCLUDED.severity,status='ACTIVE'",patientId,allergen,reaction,severity,actor);audit(actor,"UPSERT","ALLERGY",null,allergen);}catch(SQLException e){throw new IllegalStateException(e);}}
    public void addHistory(int patientId,String type,String name,java.sql.Date date,String status,String note,int actor){
        try{update("INSERT INTO patient_medical_histories(patient_id,history_type,condition_name,diagnosed_date,status,note,noted_by) VALUES(?,?,?,?,?,?,?)",patientId,type,name,date,status,note,actor);audit(actor,"CREATE","MEDICAL_HISTORY",null,name);}catch(SQLException e){throw new IllegalStateException(e);}}
    public List<Map<String,Object>> labOrders(){return query("""
      SELECT l.*,p.full_name patient_name,u.full_name doctor_name FROM lab_orders l
      JOIN patients p ON p.patient_id=l.patient_id JOIN doctors d ON d.doctor_id=l.doctor_id
      JOIN users u ON u.user_id=d.user_id ORDER BY l.ordered_at DESC LIMIT 50""");}
    public List<Map<String,Object>> labOrdersForDoctor(int doctorId){return query("""
      SELECT l.*,p.full_name patient_name,u.full_name doctor_name FROM lab_orders l
      JOIN patients p ON p.patient_id=l.patient_id JOIN doctors d ON d.doctor_id=l.doctor_id
      JOIN users u ON u.user_id=d.user_id WHERE l.doctor_id=? ORDER BY l.ordered_at DESC LIMIT 50""",doctorId);}
    public void createLabOrder(int encounterId,int doctorId,String code,String name,String priority,String note,int actor){
        try{
            int changed=update("""
              INSERT INTO lab_orders(encounter_id,patient_id,doctor_id,test_code,test_name,priority,clinical_note)
              SELECT encounter_id,patient_id,doctor_id,?,?,?,? FROM encounters
              WHERE encounter_id=? AND doctor_id=? AND status NOT IN ('COMPLETED','CANCELLED')""",
              code,name,priority,note,encounterId,doctorId);
            if(changed!=1) throw new SQLException("Lượt khám không hợp lệ hoặc đã kết thúc");
            update("UPDATE encounters SET status='WAITING_LAB' WHERE encounter_id=? AND doctor_id=?",encounterId,doctorId);
            audit(actor,"CREATE","LAB_ORDER",null,code);
        }catch(SQLException e){throw new IllegalStateException(e.getMessage(),e);}}
    public void resultLab(int orderId,String value,String unit,String range,String flag,int actor){
        try{
            int changed=update("UPDATE lab_orders SET result_value=?,result_unit=?,reference_range=?,result_flag=?,resulted_by=?,resulted_at=CURRENT_TIMESTAMP,status='RESULTED' WHERE lab_order_id=? AND status NOT IN ('REVIEWED','CANCELLED')",value,unit,range,flag,actor,orderId);
            if(changed!=1) throw new SQLException("Chỉ định không tồn tại hoặc không thể trả kết quả");
            update("""
              UPDATE encounters e SET status='LAB_COMPLETED'
              WHERE e.encounter_id=(SELECT encounter_id FROM lab_orders WHERE lab_order_id=?)
              AND NOT EXISTS (SELECT 1 FROM lab_orders l WHERE l.encounter_id=e.encounter_id AND l.status NOT IN ('RESULTED','REVIEWED','CANCELLED'))""",orderId);
            audit(actor,"RESULT","LAB_ORDER",String.valueOf(orderId),value+" "+unit);
        }catch(SQLException e){throw new IllegalStateException(e.getMessage(),e);}}
    public Integer doctorIdForUser(int userId){List<Map<String,Object>> r=query("SELECT doctor_id FROM doctors WHERE user_id=?",userId);return r.isEmpty()?null:((Number)r.get(0).get("doctor_id")).intValue();}
    public boolean doctorOwnsEncounter(int doctorId,int encounterId){return !query("SELECT 1 ok FROM encounters WHERE encounter_id=? AND doctor_id=?",encounterId,doctorId).isEmpty();}
    public boolean doctorHasPatient(int doctorId,int patientId){return !query("SELECT 1 ok FROM encounters WHERE doctor_id=? AND patient_id=? LIMIT 1",doctorId,patientId).isEmpty();}
    private void audit(int actor,String action,String type,String id,String details) throws SQLException {update("INSERT INTO audit_logs(user_id,action,entity_type,entity_id,details) VALUES(?,?,?,?,?)",actor,action,type,id,details);}
}
