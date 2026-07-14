package controllers;

import dal.*;
import models.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * [MODIFIED - Upgrade V3]
 *
 * Thay đổi:
 *   1. action=saveLog → chạy AlertEngine sau khi lưu, trả về alert_count trong JSON
 *   2. action=getAdvice → lưu risk_level + source_data_reference vào AIAdviceHistory
 *   3. action=acknowledgeAlert → đánh dấu cảnh báo đã xem (MỚI)
 *   4. loadPatientData() → thêm HealthAlerts, DeviceReadings; bỏ AIWarnings
 *   5. buildPrompt() → thêm cảnh báo hiện tại vào context cho AI
 */
@WebServlet("/PatientAI")
public class PatientAIController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;
        if (user == null || !"PATIENT".equals(user.getRole())) {
            response.sendRedirect(request.getContextPath() + "/Login"); return;
        }
        loadPatientData(request, user);
        request.getRequestDispatcher("views/PatientAI.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        HttpSession session = request.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;
        if (user == null || !"PATIENT".equals(user.getRole())) {
            response.getWriter().print("{\"error\":\"Chưa đăng nhập\"}"); return;
        }

        PatientDAO patDAO = new PatientDAO();
        Patient patient = patDAO.getByUserId(user.getUserId());
        if (patient == null) {
            response.getWriter().print("{\"error\":\"Không tìm thấy hồ sơ\"}"); return;
        }

        String action = request.getParameter("action");

        // ── LƯU CHỈ SỐ + CHẠY ALERT ENGINE ──────────────────────────────
        if ("saveLog".equals(action)) {
            PatientDailyLog log = new PatientDailyLog();
            log.setPatientId(patient.getPatientId());
            try { log.setBloodGlucose(Double.parseDouble(request.getParameter("bloodGlucose"))); } catch (Exception ignored) {}
            try { log.setSystolicBp(Integer.parseInt(request.getParameter("systolicBp"))); }      catch (Exception ignored) {}
            try { log.setDiastolicBp(Integer.parseInt(request.getParameter("diastolicBp"))); }    catch (Exception ignored) {}
            try { log.setWeight(Double.parseDouble(request.getParameter("weight"))); }            catch (Exception ignored) {}
            // [NEW V3] nhận thêm heartRate, spo2, mealType
            try { log.setHeartRate(Integer.parseInt(request.getParameter("heartRate"))); }        catch (Exception ignored) {}
            try { log.setSpo2(Double.parseDouble(request.getParameter("spo2"))); }               catch (Exception ignored) {}
            String meal = request.getParameter("mealType");
            if (meal != null && !meal.trim().isEmpty()) log.setMealType(meal.trim());
            String sym = request.getParameter("symptoms");
            if (sym != null && !sym.trim().isEmpty()) log.setSymptoms(sym.trim());
            String note = request.getParameter("note");
            if (note != null && !note.trim().isEmpty()) log.setNote(note.trim());

            PatientDailyLog saved = new PatientDailyLogDAO().save(log);

            // [NEW V3] Chạy AlertEngine rule-based
            List<HealthAlert> alerts = AlertEngine.analyzeLog(saved);
            HealthAlertDAO alertDAO = new HealthAlertDAO();
            int alertCount = 0;
            StringBuilder alertMsgs = new StringBuilder();
            for (HealthAlert a : alerts) {
                alertDAO.save(a);
                alertCount++;
                alertMsgs.append(a.getAlertMessage()).append("\\n");
            }

            StringBuilder resp = new StringBuilder("{\"success\":true,\"message\":\"Đã lưu chỉ số hôm nay\"");
            resp.append(",\"alerts_created\":").append(alertCount);
            resp.append(",\"has_alerts\":").append(alertCount > 0);
            if (alertCount > 0)
                resp.append(",\"alert_messages\":\"").append(escapeJson(alertMsgs.toString())).append("\"");
            resp.append("}");
            response.getWriter().print(resp.toString());

        // ── GỌI OPENAI SINH LỜI KHUYÊN ───────────────────────────────────
        } else if ("getAdvice".equals(action)) {
            String prompt = buildPrompt(patient);
            String advice = callOpenAI(prompt);

            if (advice.startsWith("ERROR:")) {
                response.getWriter().print("{\"error\":\"" + escapeJson(advice.substring(6)) + "\"}"); return;
            }

            // [NEW V3] Xác định risk_level từ số liệu (rule-based, không dùng AI)
            double[] avg7 = new PatientDailyLogDAO().getAvg7Days(patient.getPatientId());
            String riskLevel = detectRiskLevel(avg7);

            // Build source_data_reference
            List<PatientDailyLog> logs = new PatientDailyLogDAO().getRecent(patient.getPatientId(), 7);
            StringBuilder srcRef = new StringBuilder("{\"log_ids\":[");
            for (int i = 0; i < logs.size(); i++) {
                srcRef.append(logs.get(i).getLogId());
                if (i < logs.size()-1) srcRef.append(",");
            }
            srcRef.append("]}");

            // [NEW V3] Lưu AIAdviceHistory với đầy đủ trường
            AIAdviceHistory history = new AIAdviceHistory();
            history.setPatientId(patient.getPatientId());
            history.setAdviceContent(advice);
            history.setRiskLevel(riskLevel);
            history.setSourceDataReference(srcRef.toString());
            history.setAvgGlucose7d(avg7[0] > 0 ? avg7[0] : null);
            history.setAvgSystolic7d(avg7[1] > 0 ? (int) avg7[1] : null);
            new AIAdviceHistoryDAO().save(history);

            response.getWriter().print("{\"success\":true,\"advice\":\"" + escapeJson(advice)
                + "\",\"risk_level\":\"" + riskLevel + "\"}");

        // ── BÁO CÁO TUẦN (giữ nguyên từ V2) ─────────────────────────────
        } else if ("weekReport".equals(action)) {
            String prompt = buildWeekReportPrompt(patient);
            String report = callOpenAI(prompt);
            if (report.startsWith("ERROR:"))
                response.getWriter().print("{\"error\":\"" + escapeJson(report.substring(6)) + "\"}");
            else
                response.getWriter().print("{\"success\":true,\"report\":\"" + escapeJson(report) + "\"}");

        // ── [NEW V3] ĐÁNH DẤU CẢNH BÁO ĐÃ XEM ───────────────────────────
        } else if ("acknowledgeAlert".equals(action)) {
            try {
                int alertId = Integer.parseInt(request.getParameter("alertId"));
                if (!new HealthAlertDAO().acknowledgeForPatient(alertId, patient.getPatientId())) {
                    response.sendError(404); return;
                }
                response.getWriter().print("{\"success\":true}");
            } catch (Exception e) {
                response.getWriter().print("{\"error\":\"alertId không hợp lệ\"}");
            }
        }
    }

    // ── [NEW V3] XÁC ĐỊNH RISK LEVEL RULE-BASED ──────────────────────────
    private String detectRiskLevel(double[] avg7) {
        double avgGlucose = avg7[0];
        double avgSystolic = avg7[1];
        if (avgGlucose >= 180 || avgSystolic >= 160) return "high";
        if (avgGlucose >= 130 || avgSystolic >= 140) return "medium";
        return "low";
    }

    // ── BUILD PROMPT ──────────────────────────────────────────────────────
    private String buildPrompt(Patient patient) {
        StringBuilder sb = new StringBuilder();
        MedicalRecordDAO   recDAO  = new MedicalRecordDAO();
        HealthIndicatorDAO hiDAO   = new HealthIndicatorDAO();
        AIWarningDAO       warnDAO = new AIWarningDAO();
        PatientDailyLogDAO logDAO  = new PatientDailyLogDAO();
        AIAdviceHistoryDAO advDAO  = new AIAdviceHistoryDAO();

        sb.append("=== THÔNG TIN BỆNH NHÂN ===\n");
        sb.append("Họ tên: ").append(patient.getFullName()).append("\n");
        if (patient.getDateOfBirth() != null)
            sb.append("Tuổi: ").append(java.time.LocalDate.now().getYear() - patient.getDateOfBirth().getYear()).append("\n");
        sb.append("Giới tính: ").append(patient.getGender()).append("\n\n");

        List<MedicalRecord> records = recDAO.getByPatient(patient.getPatientId());
        if (!records.isEmpty()) {
            MedicalRecord r = records.get(0);
            sb.append("=== KẾT QUẢ KHÁM GẦN NHẤT (").append(r.getVisitDate()).append(") ===\n");
            if (r.getMedicalHistory()  != null) sb.append("Tiền sử: ").append(r.getMedicalHistory()).append("\n");
            if (r.getFinalDiagnosis()  != null) sb.append("Chẩn đoán: ").append(r.getFinalDiagnosis()).append("\n");
            if (r.getPrescriptionNote()!= null) sb.append("Thuốc bác sĩ kê: ").append(r.getPrescriptionNote()).append("\n");
            if (r.getAdvice()          != null) sb.append("Lời dặn bác sĩ: ").append(r.getAdvice()).append("\n");
            if (r.getFollowUpDate()    != null) sb.append("Lịch tái khám: ").append(r.getFollowUpDate()).append("\n");

            HealthIndicator hi = hiDAO.getByRecordId(r.getRecordId());
            if (hi != null) {
                sb.append("\n=== CHỈ SỐ XÉT NGHIỆM ===\n");
                if (hi.getBloodGlucose() > 0) sb.append("Đường huyết: ").append(hi.getBloodGlucose()).append(" mg/dL\n");
                if (hi.getHba1c()        > 0) sb.append("HbA1c: ").append(hi.getHba1c()).append("%\n");
                if (hi.getBmi()          > 0) sb.append("BMI: ").append(hi.getBmi()).append("\n");
                if (hi.getSystolicBp()   > 0) sb.append("Huyết áp: ").append(hi.getSystolicBp()).append("/").append(hi.getDiastolicBp()).append(" mmHg\n");
                if (hi.getCholesterol()  > 0) sb.append("Cholesterol: ").append(hi.getCholesterol()).append(" mg/dL\n");
            }

            // [V3] Bỏ AIWarning khỏi prompt (đã xóa Doctor AI)
        }

        PatientDailyLog yesterday = logDAO.getYesterdayLog(patient.getPatientId());
        double[] avg7 = logDAO.getAvg7Days(patient.getPatientId());

        sb.append("\n=== CHỈ SỐ TỰ ĐO 7 NGÀY GẦN NHẤT ===\n");
        if (avg7[0] > 0) sb.append("Đường huyết trung bình 7 ngày: ").append(String.format("%.1f", avg7[0])).append(" mg/dL\n");
        if (avg7[1] > 0) sb.append("Huyết áp tâm thu TB 7 ngày: ").append(String.format("%.0f", avg7[1])).append(" mmHg\n");
        if (yesterday != null) {
            sb.append("Hôm qua: ");
            if (yesterday.getBloodGlucose() != null) sb.append("ĐH=").append(yesterday.getBloodGlucose()).append(" ");
            if (yesterday.getSystolicBp()   != null) sb.append("HA=").append(yesterday.getSystolicBp()).append("/").append(yesterday.getDiastolicBp()).append(" ");
            if (yesterday.getSymptoms()     != null) sb.append("TC: ").append(yesterday.getSymptoms());
            sb.append("\n");
        }

        List<PatientDailyLog> logs = logDAO.getRecent(patient.getPatientId(), 7);
        for (PatientDailyLog log : logs) {
            sb.append("Ngày ").append(log.getLogDate()).append(": ");
            if (log.getBloodGlucose() != null) sb.append("ĐH=").append(log.getBloodGlucose()).append("mg/dL ");
            if (log.getSystolicBp()   != null) sb.append("HA=").append(log.getSystolicBp()).append("/").append(log.getDiastolicBp()).append(" ");
            if (log.getWeight()       != null) sb.append("Cân=").append(log.getWeight()).append("kg ");
            if (log.getSymptoms()     != null) sb.append("TC: ").append(log.getSymptoms());
            sb.append("\n");
        }

        // [NEW V3] Thêm cảnh báo hiện tại vào context
        List<HealthAlert> alerts = new HealthAlertDAO().getUnacknowledged(patient.getPatientId());
        if (!alerts.isEmpty()) {
            sb.append("\n=== CẢNH BÁO SỨC KHỎE CHƯA XỬ LÝ ===\n");
            for (HealthAlert a : alerts)
                sb.append("- [").append(a.getAlertLevel().toUpperCase()).append("] ").append(a.getAlertMessage()).append("\n");
        }

        List<AIAdviceHistory> recentAdv = advDAO.getRecent(patient.getPatientId(), 3);
        if (!recentAdv.isEmpty()) {
            sb.append("\n=== 3 LỜI KHUYÊN GẦN NHẤT (để tránh lặp lại) ===\n");
            for (AIAdviceHistory a : recentAdv) {
                sb.append("Ngày ").append(a.getAdviceDate()).append(": ")
                  .append(a.getAdviceContent() != null ? a.getAdviceContent().substring(0, Math.min(200, a.getAdviceContent().length())) : "").append("...\n");
            }
        }

        sb.append("\n=== YÊU CẦU ===\n");
        sb.append("Viết lời khuyên chăm sóc sức khỏe hôm nay bằng tiếng Việt, dễ hiểu cho người cao tuổi.\n");
        sb.append("NGHIÊM CẤM: kê thuốc, đổi liều, chẩn đoán bệnh.\n");
        sb.append("CHỈ ĐƯỢC: nhắc uống thuốc theo đơn bác sĩ, đo chỉ số, ăn uống lành mạnh, vận động nhẹ, liên hệ bác sĩ khi bất thường.\n");
        sb.append("Đừng lặp lại nội dung đã có trong 3 lời khuyên gần nhất.\n");
        sb.append("Trình bày theo mục với emoji, ngắn gọn, dễ đọc.\n");
        return sb.toString();
    }

    private String buildWeekReportPrompt(Patient patient) {
        PatientDailyLogDAO logDAO = new PatientDailyLogDAO();
        double[] avg7 = logDAO.getAvg7Days(patient.getPatientId());
        List<PatientDailyLog> logs = logDAO.getRecent(patient.getPatientId(), 7);
        StringBuilder sb = new StringBuilder();
        sb.append("=== BÁO CÁO SỨC KHỎE TUẦN — ").append(patient.getFullName()).append(" ===\n");
        if (avg7[0] > 0) sb.append("Đường huyết TB: ").append(String.format("%.1f", avg7[0])).append(" mg/dL\n");
        if (avg7[1] > 0) sb.append("Huyết áp tâm thu TB: ").append(String.format("%.0f", avg7[1])).append(" mmHg\n");
        sb.append("\nDữ liệu 7 ngày:\n");
        for (PatientDailyLog log : logs) {
            sb.append("- ").append(log.getLogDate()).append(": ");
            if (log.getBloodGlucose() != null) sb.append("ĐH=").append(log.getBloodGlucose()).append(" ");
            if (log.getSystolicBp()   != null) sb.append("HA=").append(log.getSystolicBp()).append("/").append(log.getDiastolicBp()).append(" ");
            if (log.getSymptoms()     != null) sb.append(log.getSymptoms());
            sb.append("\n");
        }
        sb.append("\nViết báo cáo tóm tắt sức khỏe tuần bằng tiếng Việt. Nhận xét xu hướng, điểm tích cực, điểm cần cải thiện. Ngắn gọn, dùng emoji.");
        return sb.toString();
    }

    private String callOpenAI(String prompt) {
        // Lấy API key từ session hoặc từ request parameter — key không lưu server
        // Với V3: key được JS lưu localStorage và gửi kèm mỗi request
        // Để backward compat, vẫn hỗ trợ cả hai cách
        return callOpenAIWithKey(System.getProperty("app.openai.api-key", ""), prompt);
    }

    private String callOpenAIWithKey(String apiKey, String prompt) {
        // Nếu không có key riêng, return error hướng dẫn
        if (apiKey == null || apiKey.trim().isEmpty()) {
            // Kiểm tra header
            return "ERROR:Vui lòng nhập OpenAI API Key (sk-...)";
        }
        try {
            URL url = new URL("https://api.openai.com/v1/chat/completions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);

            String systemMsg = "Bạn là trợ lý chăm sóc sức khỏe hỗ trợ bệnh nhân tiểu đường cao tuổi. " +
                "Bạn KHÔNG được kê thuốc, thay đổi liều thuốc, chẩn đoán bệnh hoặc xử lý cấp cứu. " +
                "Luôn nhắc bệnh nhân liên hệ bác sĩ khi có dấu hiệu bất thường.";
            String body = "{\"model\":\"gpt-4o-mini\",\"max_tokens\":1024," +
                "\"messages\":[" +
                "{\"role\":\"system\",\"content\":\"" + escapeJson(systemMsg) + "\"}," +
                "{\"role\":\"user\",\"content\":\"" + escapeJson(prompt) + "\"}" +
                "]}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            InputStream is = code == 200 ? conn.getInputStream() : conn.getErrorStream();
            StringBuilder resp = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line; while ((line = br.readLine()) != null) resp.append(line);
            }
            String json = resp.toString();
            if (code != 200) {
                if (json.contains("invalid_api_key") || json.contains("Incorrect API key"))
                    return "ERROR:API Key không hợp lệ. Key OpenAI bắt đầu bằng sk-...";
                if (json.contains("rate_limit"))
                    return "ERROR:Đã vượt giới hạn API. Vui lòng thử lại sau ít phút.";
                return "ERROR:Lỗi API (code " + code + ")";
            }

            int idx = json.indexOf("\"content\":");
            if (idx < 0) return "ERROR:Không đọc được phản hồi từ AI";
            int start = json.indexOf("\"", idx + 10) + 1;
            int end = start;
            while (end < json.length()) {
                if (json.charAt(end) == '"' && json.charAt(end-1) != '\\') break;
                end++;
            }
            if (end <= start) return "ERROR:Định dạng phản hồi không hợp lệ";
            return json.substring(start, end)
                .replace("\\n", "\n").replace("\\\"", "\"").replace("\\/", "/");
        } catch (java.net.SocketTimeoutException e) {
            return "ERROR:Hết thời gian chờ. Vui lòng thử lại.";
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    private void loadPatientData(HttpServletRequest request, User user) {
        PatientDAO patDAO = new PatientDAO();
        Patient patient = patDAO.getByUserId(user.getUserId());
        if (patient == null) return;

        PatientDailyLogDAO logDAO    = new PatientDailyLogDAO();
        AIAdviceHistoryDAO adviceDAO = new AIAdviceHistoryDAO();
        MedicalRecordDAO   recDAO    = new MedicalRecordDAO();
        // [NEW V3]
        HealthAlertDAO   alertDAO  = new HealthAlertDAO();
        DeviceReadingDAO deviceDAO = new DeviceReadingDAO();

        request.setAttribute("patient",              patient);
        request.setAttribute("todayLog",             logDAO.getTodayLog(patient.getPatientId()));
        request.setAttribute("recentLogs",           logDAO.getRecent(patient.getPatientId(), 7));
        request.setAttribute("todayAdvice",          adviceDAO.getTodayAdvice(patient.getPatientId()));
        request.setAttribute("adviceHistory",        adviceDAO.getRecent(patient.getPatientId(), 5));
        // [NEW V3] Thay latestWarning → unacknowledgedAlerts
        request.setAttribute("unacknowledgedAlerts", alertDAO.getUnacknowledged(patient.getPatientId()));
        request.setAttribute("alertCount",           alertDAO.countUnacknowledged(patient.getPatientId()));
        request.setAttribute("recentDeviceReadings", deviceDAO.getRecent(patient.getPatientId(), 5));

        List<MedicalRecord> records = recDAO.getByPatient(patient.getPatientId());
        if (!records.isEmpty()) request.setAttribute("latestRecord", records.get(0));
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","").replace("\t"," ");
    }
}
