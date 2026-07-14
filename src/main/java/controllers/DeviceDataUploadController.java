package controllers;

import dal.*;
import models.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.security.MessageDigest;

/**
 * [NEW - Upgrade V3]
 * REST API nhận dữ liệu từ thiết bị y tế bên ngoài.
 *
 * POST /api/device-data/upload
 *
 * JSON glucometer:   {"device_type":"glucometer","patient_id":1,"glucose_mgdl":145.0,"timestamp":"2026-06-28T08:30:00"}
 * JSON smartwatch:   {"device_type":"smartwatch","patient_id":1,"heart_rate":92,"spo2":97.5,"timestamp":"..."}
 * JSON bp_monitor:   {"device_type":"bp_monitor","patient_id":1,"systolic":138,"diastolic":88,"heart_rate":76,"timestamp":"..."}
 * JSON scale:        {"device_type":"scale","patient_id":1,"weight_kg":72.5,"timestamp":"..."}
 */
@WebServlet("/api/device-data/upload")
public class DeviceDataUploadController extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        request.setCharacterEncoding("UTF-8");

        String configuredKey = System.getenv("DEVICE_API_KEY");
        if (configuredKey == null || configuredKey.isBlank()) {
            sendError(response, 503, "Device API chưa được cấu hình"); return;
        }
        String suppliedKey = request.getHeader("X-Device-Key");
        if (suppliedKey == null || !MessageDigest.isEqual(
                configuredKey.getBytes(StandardCharsets.UTF_8), suppliedKey.getBytes(StandardCharsets.UTF_8))) {
            sendError(response, 401, "Device API key không hợp lệ"); return;
        }

        String rawJson;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8))) {
            rawJson = reader.lines().collect(Collectors.joining("\n")).trim();
        }

        if (rawJson == null || rawJson.isEmpty()) {
            sendError(response, 400, "Request body rỗng"); return;
        }

        int patientId = parseIntField(rawJson, "patient_id");
        if (patientId <= 0) { sendError(response, 400, "Thiếu patient_id"); return; }

        Patient patient = new PatientDAO().getById(patientId);
        if (patient == null) { sendError(response, 404, "Không tìm thấy bệnh nhân id=" + patientId); return; }

        String deviceType = parseStringField(rawJson, "device_type");
        if (deviceType == null || deviceType.isEmpty()) { sendError(response, 400, "Thiếu device_type"); return; }

        DeviceReading reading = new DeviceReading();
        reading.setPatientId(patientId);
        reading.setDeviceType(deviceType);
        reading.setRawJsonData(rawJson);

        switch (deviceType.toLowerCase()) {
            case "glucometer" -> parseGlucometer(rawJson, reading);
            case "smartwatch" -> parseSmartwatch(rawJson, reading);
            case "bp_monitor" -> parseBpMonitor(rawJson, reading);
            case "scale"      -> parseScale(rawJson, reading);
            default           -> { parseGlucometer(rawJson, reading); parseSmartwatch(rawJson, reading); parseBpMonitor(rawJson, reading); parseScale(rawJson, reading); }
        }

        String ts = parseStringField(rawJson, "timestamp");
        try {
            reading.setMeasuredAt(ts != null ? LocalDateTime.parse(ts, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) : LocalDateTime.now());
        } catch (Exception e) { reading.setMeasuredAt(LocalDateTime.now()); }

        // Chạy AlertEngine
        List<HealthAlert> alerts = AlertEngine.analyzeDeviceReading(reading);
        if (!alerts.isEmpty()) {
            reading.setAbnormal(true);
            StringBuilder sb = new StringBuilder();
            for (HealthAlert a : alerts) sb.append(a.getAlertMessage()).append("; ");
            reading.setAbnormalNote(sb.toString().trim());
        }

        DeviceReading saved = new DeviceReadingDAO().save(reading);

        HealthAlertDAO alertDAO = new HealthAlertDAO();
        for (HealthAlert a : alerts) { a.setSourceRecordId(saved.getId()); alertDAO.save(a); }

        // Response JSON
        StringBuilder resp = new StringBuilder("{");
        resp.append("\"success\":true,\"reading_id\":").append(saved.getId());
        resp.append(",\"patient_id\":").append(patientId);
        resp.append(",\"device_type\":\"").append(escapeJson(deviceType)).append("\"");
        resp.append(",\"is_abnormal\":").append(saved.isAbnormal());
        resp.append(",\"alerts_created\":").append(alerts.size());
        if (!alerts.isEmpty()) {
            resp.append(",\"alert_messages\":[");
            for (int i=0;i<alerts.size();i++) {
                resp.append("\"").append(escapeJson(alerts.get(i).getAlertMessage())).append("\"");
                if (i < alerts.size()-1) resp.append(",");
            }
            resp.append("]");
        }
        resp.append(",\"message\":\"Đã nhận dữ liệu thiết bị thành công\"}");
        response.setStatus(200);
        response.getWriter().print(resp.toString());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().print("{\"status\":\"Device Data API available\"}");
    }

    private void parseGlucometer(String json, DeviceReading dr) {
        double mgdl = parseDoubleField(json, "glucose_mgdl");
        if (mgdl > 0) { dr.setParsedGlucose(mgdl); return; }
        double mmol = parseDoubleField(json, "glucose_mmol");
        if (mmol > 0) dr.setParsedGlucose(Math.round(mmol * 18.0182 * 10.0) / 10.0);
    }
    private void parseSmartwatch(String json, DeviceReading dr) {
        int hr = parseIntField(json, "heart_rate"); if (hr > 0) dr.setParsedHeartRate(hr);
        double spo2 = parseDoubleField(json, "spo2"); if (spo2 > 0) dr.setParsedSpo2(spo2);
        double bg = parseDoubleField(json, "glucose_mgdl"); if (bg > 0) dr.setParsedGlucose(bg);
    }
    private void parseBpMonitor(String json, DeviceReading dr) {
        int sbp = parseIntField(json, "systolic");   if (sbp > 0) dr.setParsedSystolicBp(sbp);
        int dbp = parseIntField(json, "diastolic");  if (dbp > 0) dr.setParsedDiastolicBp(dbp);
        int hr  = parseIntField(json, "heart_rate"); if (hr  > 0) dr.setParsedHeartRate(hr);
    }
    private void parseScale(String json, DeviceReading dr) {
        double w = parseDoubleField(json, "weight_kg"); if (w > 0) dr.setParsedWeight(w);
    }

    private String parseStringField(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search); if (idx < 0) return null;
        int colon = json.indexOf(":", idx + search.length()); if (colon < 0) return null;
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        if (start >= json.length() || json.charAt(start) != '"') return null;
        int end = json.indexOf('"', start + 1); if (end < 0) return null;
        return json.substring(start + 1, end);
    }
    private double parseDoubleField(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search); if (idx < 0) return 0;
        int colon = json.indexOf(":", idx + search.length()); if (colon < 0) return 0;
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.')) end++;
        if (start == end) return 0;
        try { return Double.parseDouble(json.substring(start, end)); } catch (NumberFormatException e) { return 0; }
    }
    private int parseIntField(String json, String key) { return (int) parseDoubleField(json, key); }
    private void sendError(HttpServletResponse r, int code, String msg) throws IOException {
        r.setStatus(code); r.getWriter().print("{\"success\":false,\"error\":\"" + escapeJson(msg) + "\"}");
    }
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","").replace("\t"," ");
    }
}
