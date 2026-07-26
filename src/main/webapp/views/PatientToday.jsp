<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Sức khỏe hôm nay — DiaCare</title><link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css?v=20260722-web-audit2"><link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/patient-care-path.css?v=20260721-patient1"></head>
<body><jsp:include page="header.jsp"/><jsp:include page="topnav.jsp"/>
<main class="page-wrapper patient-today-page"><div class="page-heading"><div><div class="eyebrow">THEO DÕI HẰNG NGÀY</div><h1 class="page-title">Sức khỏe hôm nay</h1></div><a class="btn btn-light" href="#daily-health-entry">📝 Nhập chỉ số</a></div>
<c:if test="${not empty msg}"><div class="alert alert-info"><c:out value="${msg}"/></div></c:if>
<c:if test="${not empty diabetesProfile}">
<section class="patient-overview">
    <div>
        <span>HỒ SƠ ĐÁI THÁO ĐƯỜNG DO BÁC SĨ XÁC NHẬN</span>
        <div>${diabetesProfile.diabetesTypeLabel}</div>
        <p>Điều trị hiện tại: ${diabetesProfile.treatmentMethodLabel}<c:if test="${not empty diabetesProfile.diagnosisDate}"> · Phát hiện: ${diabetesProfile.diagnosisDate}</c:if><c:if test="${not empty diabetesProfile.hba1cTarget}"> · Mục tiêu HbA1c: ${diabetesProfile.hba1cTarget}%</c:if></p>
    </div>
</section>
<jsp:include page="diabetesCarePath.jsp"/>
</c:if>
<c:if test="${not empty patient}">
<section class="card ai-advice-card" id="daily-advice">
    <div class="ai-advice-heading">
        <div><div class="eyebrow">TRỢ LÝ SỨC KHỎE</div><h2>Lời khuyên hôm nay</h2><p>Gợi ý dễ thực hiện về theo dõi, thuốc, dinh dưỡng, vận động và dấu hiệu cần liên hệ bác sĩ.</p></div>
        <span class="ai-advice-icon" aria-hidden="true">🩺</span>
    </div>
    <label class="ai-consent"><input type="checkbox" id="aiConsent"> <span>🔒 Tôi đồng ý dùng dữ liệu sức khỏe đã ẩn danh để tạo lời khuyên hỗ trợ.</span></label>
    <%-- Nút này gọi POST /api/patient/ai-advice; controller kiểm tra đồng ý rồi service tạo lời khuyên an toàn. --%>
    <div class="ai-advice-actions"><button type="button" class="btn btn-primary" id="aiAdviceButton" data-create-advice>✨ Tạo lời khuyên chi tiết</button></div>
    <div class="ai-advice-result" id="aiAdviceResult" hidden aria-live="polite">
        <div class="ai-advice-result-head"><strong id="aiAdviceSummary"></strong><span id="aiAdviceSeverity" class="ai-severity"></span></div>
        <div id="aiAdviceItems" class="ai-advice-groups"></div><p id="aiAdviceDoctor" class="ai-doctor-note" hidden></p><small id="aiAdviceSource"></small>
    </div>
</section>

</c:if>
<c:if test="${not empty patient}"><section class="card daily-entry-card daily-entry-full" id="daily-health-entry"><div class="card-title">Nhập chỉ số hôm nay <c:if test="${not empty todayLog}"><span class="status-pill status-COMPLETED">Đã nhập</span></c:if></div>
<div class="form-group"><label>Thời điểm đo đường huyết</label><select id="log_meal" class="form-control"><option value="">Chọn thời điểm</option><option value="FASTING" ${todayLog.mealType=='FASTING'?'selected':''}>Lúc đói</option><option value="AFTER_MEAL" ${todayLog.mealType=='AFTER_MEAL'?'selected':''}>Sau ăn</option><option value="BEDTIME" ${todayLog.mealType=='BEDTIME'?'selected':''}>Trước khi ngủ</option><option value="OTHER" ${todayLog.mealType=='OTHER'?'selected':''}>Khác</option></select></div>
<div class="form-group"><label>Đường huyết (mg/dL)</label><input type="number" min="20" max="600" step="0.1" id="log_bg" class="form-control" value="${todayLog.bloodGlucose}" placeholder="Ví dụ: 105"></div>
<div class="two-column"><div class="form-group"><label>Huyết áp tâm thu</label><input type="number" min="60" max="260" id="log_sbp" class="form-control" value="${todayLog.systolicBp}" placeholder="120"></div><div class="form-group"><label>Huyết áp tâm trương</label><input type="number" min="30" max="180" id="log_dbp" class="form-control" value="${todayLog.diastolicBp}" placeholder="80"></div></div>
<div class="form-group"><label>Nhịp tim (lần/phút)</label><input type="number" min="30" max="220" id="log_hr" class="form-control" value="${todayLog.heartRate}" placeholder="75"></div>
<div class="form-group"><label>Cân nặng (kg)</label><input type="number" min="20" max="300" step="0.1" id="log_weight" class="form-control" value="${todayLog.weight}" placeholder="65"></div>
<div class="form-group"><label>Triệu chứng hôm nay</label><div class="checkbox-grid"><label><input type="checkbox" name="symptom" value="Không có triệu chứng"> Không có</label><label><input type="checkbox" name="symptom" value="Mệt mỏi"> Mệt mỏi</label><label><input type="checkbox" name="symptom" value="Khát nhiều"> Khát nhiều</label><label><input type="checkbox" name="symptom" value="Chóng mặt"> Chóng mặt</label><label><input type="checkbox" name="symptom" value="Run tay/vã mồ hôi"> Run tay/vã mồ hôi</label><c:if test="${diabetesProfile.diabetesType=='TYPE_1'}"><label><input type="checkbox" name="symptom" value="Buồn nôn hoặc đau bụng"> Buồn nôn/đau bụng</label><label><input type="checkbox" name="symptom" value="Thở nhanh hoặc thở sâu"> Thở nhanh/thở sâu</label></c:if><c:if test="${diabetesProfile.diabetesType=='TYPE_2'}"><label><input type="checkbox" name="symptom" value="Tê bì bàn chân"> Tê bì bàn chân</label><label><input type="checkbox" name="symptom" value="Phù chân"> Phù chân</label></c:if></div></div>
<div class="form-group"><label>Ghi chú</label><textarea id="log_note" class="form-control" maxlength="1000" placeholder="Thông tin khác muốn bác sĩ biết"><c:out value="${todayLog.note}"/></textarea></div>
<%-- Nút lưu gửi dữ liệu sang PatientHealthController; kết quả JSON được hiện ngay dưới nút. --%>
<button type="button" class="btn btn-primary" id="saveLogBtn" data-save-health-log>Lưu chỉ số hôm nay</button><div id="logResult" class="daily-log-message" role="status" aria-live="polite" hidden></div></section></c:if>
</main><span id="savedSymptoms" hidden><c:out value="${todayLog.symptoms}"/></span><jsp:include page="footer.jsp"/>
<script src="${pageContext.request.contextPath}/static/js/patient-today.js?v=20260726-review2"></script>
</script></body></html>
