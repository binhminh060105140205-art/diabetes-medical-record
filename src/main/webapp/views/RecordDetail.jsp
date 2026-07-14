<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Chi Tiết Hồ Sơ Bệnh Án</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body>
<jsp:include page="header.jsp"/>
<jsp:include page="topnav.jsp"/>
<div class="page-wrapper">
    <h1 class="page-title">📄 Hồ Sơ Bệnh Án #${detail.record.recordId}</h1>

    <!-- I. PATIENT INFO -->
    <div class="card">
        <div class="card-title">I. Thông tin bệnh nhân</div>
        <table style="box-shadow:none;">
            <tr><th style="width:220px">Họ và tên</th><td><strong>${detail.patient.fullName}</strong></td>
                <th style="width:180px">Ngày sinh</th><td>${detail.patient.dateOfBirth}</td></tr>
            <tr><th>Giới tính</th><td>${detail.patient.gender}</td>
                <th>Số điện thoại</th><td>${detail.patient.phone}</td></tr>
            <tr><th>Địa chỉ</th><td colspan="3">${detail.patient.address}</td></tr>
            <tr><th>Số BHYT</th><td>${detail.patient.healthInsuranceNo}</td>
                <th>Ngày khám</th><td>${detail.record.visitDate}</td></tr>
            <tr><th>Bác sĩ khám</th><td colspan="3">${detail.doctor.fullName} — ${detail.doctor.specialty}</td></tr>
        </table>
    </div>

    <!-- II. CLINICAL INFO -->
    <div class="card">
        <div class="card-title">II. Thông tin khám</div>
        <table style="box-shadow:none;">
            <tr><th style="width:220px">Lý do khám</th><td>${detail.record.reasonForVisit}</td></tr>
            <tr><th>Triệu chứng</th><td>${detail.record.symptoms}</td></tr>
            <tr><th>Tiền sử bệnh</th><td>${detail.record.medicalHistory}</td></tr>
            <tr><th>Thói quen sinh hoạt</th><td>${detail.record.lifestyleHabits}</td></tr>
            <tr><th>Khám lâm sàng</th><td>${detail.record.clinicalExam}</td></tr>
        </table>
    </div>

    <!-- III. HEALTH INDICATORS -->
    <c:if test="${not empty detail.indicator}">
    <div class="card">
        <div class="card-title">III. Chỉ số sức khỏe</div>
        <div class="indicator-grid">
            <div class="indicator-item">
                <div class="ind-label">Chiều cao</div>
                <div class="ind-value">${detail.indicator.height}</div>
                <div class="ind-unit">cm</div>
            </div>
            <div class="indicator-item">
                <div class="ind-label">Cân nặng</div>
                <div class="ind-value">${detail.indicator.weight}</div>
                <div class="ind-unit">kg</div>
            </div>
            <div class="indicator-item">
                <div class="ind-label">BMI</div>
                <div class="ind-value">${detail.indicator.bmi}</div>
                <div class="ind-unit">kg/m²</div>
            </div>
            <div class="indicator-item">
                <div class="ind-label">Huyết áp</div>
                <div class="ind-value">${detail.indicator.systolicBp}/${detail.indicator.diastolicBp}</div>
                <div class="ind-unit">mmHg</div>
            </div>
            <div class="indicator-item">
                <div class="ind-label">Nhịp tim</div>
                <div class="ind-value">${detail.indicator.heartRate}</div>
                <div class="ind-unit">lần/phút</div>
            </div>
            <div class="indicator-item">
                <div class="ind-label">Đường huyết</div>
                <div class="ind-value">${detail.indicator.bloodGlucose}</div>
                <div class="ind-unit">mg/dL</div>
            </div>
            <div class="indicator-item">
                <div class="ind-label">HbA1c</div>
                <div class="ind-value">${detail.indicator.hba1c}</div>
                <div class="ind-unit">%</div>
            </div>
            <div class="indicator-item">
                <div class="ind-label">Cholesterol</div>
                <div class="ind-value">${detail.indicator.cholesterol}</div>
                <div class="ind-unit">mg/dL</div>
            </div>
            <div class="indicator-item">
                <div class="ind-label">Triglyceride</div>
                <div class="ind-value">${detail.indicator.triglyceride}</div>
                <div class="ind-unit">mg/dL</div>
            </div>
            <div class="indicator-item">
                <div class="ind-label">HDL-C</div>
                <div class="ind-value">${detail.indicator.hdlC}</div>
                <div class="ind-unit">mg/dL</div>
            </div>
            <div class="indicator-item">
                <div class="ind-label">LDL-C</div>
                <div class="ind-value">${detail.indicator.ldlC}</div>
                <div class="ind-unit">mg/dL</div>
            </div>
        </div>
    </div>
    </c:if>

    <!-- IV. AI WARNING -->
    <c:if test="${not empty detail.warning}">
    <div class="card">
        <div class="card-title">IV. Đánh giá AI — Nguy cơ Tiểu đường</div>
        <div class="ai-panel ${detail.warning.riskLevel}">
            <h3>
                🤖 Mức nguy cơ:
                <span class="risk-badge risk-${detail.warning.riskLevel}">${detail.warning.riskLevel}</span>
                &nbsp;&nbsp; Điểm AI: <strong>${detail.warning.aiScore} / 100</strong>
            </h3>
            <hr style="margin:10px 0;border-color:rgba(0,0,0,.1);">
            <p><strong>⚠️ Cảnh báo:</strong></p>
            <pre>${detail.warning.warningMessage}</pre>
            <br>
            <p><strong>💡 Gợi ý xử lý:</strong></p>
            <pre>${detail.warning.suggestedAction}</pre>
            <br>
            <small class="text-muted">
                Thời gian phân tích: ${detail.warning.generatedAt} &nbsp;|&nbsp;
                <c:choose>
                    <c:when test="${detail.warning.reviewedByDoctor}">✅ Bác sĩ đã xem</c:when>
                    <c:otherwise>⏳ Chờ bác sĩ xem xét</c:otherwise>
                </c:choose>
            </small>
        </div>
        <p class="text-muted" style="font-size:12px;margin-top:6px;">
            * AI chỉ đóng vai trò hỗ trợ phân tích, không thay thế chẩn đoán lâm sàng của bác sĩ.
        </p>
    </div>
    </c:if>

    <!-- V. DOCTOR CONCLUSION -->
    <div class="card">
        <div class="card-title">V. Kết luận của Bác sĩ</div>
        <c:choose>
            <c:when test="${detail.record.status == 'COMPLETED'}">
                <table style="box-shadow:none;">
                    <tr><th style="width:220px">Biến chứng</th><td>${detail.record.complicationNote}</td></tr>
                    <tr><th>Chẩn đoán</th><td><strong>${detail.record.finalDiagnosis}</strong></td></tr>
                    <tr><th>Hướng điều trị</th><td>${detail.record.treatmentPlan}</td></tr>
                    <tr><th>Đơn thuốc</th><td>${detail.record.prescriptionNote}</td></tr>
                    <tr><th>Lời dặn</th><td>${detail.record.advice}</td></tr>
                    <tr><th>Ngày tái khám</th><td><strong>${detail.record.followUpDate}</strong></td></tr>
                    <tr><th>Ghi chú thêm</th><td>${detail.record.doctorNote}</td></tr>
                </table>
            </c:when>
            <c:otherwise>
                <div class="alert alert-info">
                    ⏳ Bác sĩ chưa hoàn tất kết luận.
                    <c:if test="${sessionScope.user.role == 'DOCTOR'}">
                        <a href="${pageContext.request.contextPath}/MedicalRecordForm?recordId=${detail.record.recordId}&tab=4"
                           class="btn btn-primary btn-sm" style="margin-left:10px;">Nhập kết luận</a>
                    </c:if>
                </div>
            </c:otherwise>
        </c:choose>
    </div>

    <div style="margin-top:16px;">
        <a href="javascript:history.back()" class="btn btn-outline" style="background:#6c757d;color:white;">← Quay lại</a>
        <c:if test="${sessionScope.user.role == 'DOCTOR' && detail.record.status == 'DRAFT'}">
            <a href="${pageContext.request.contextPath}/MedicalRecordForm?recordId=${detail.record.recordId}&tab=1"
               class="btn btn-warning" style="margin-left:8px;">✏️ Tiếp tục chỉnh sửa</a>
        </c:if>
        <a href="${pageContext.request.contextPath}/PatientHistory?patientId=${detail.patient.patientId}"
           class="btn btn-success" style="margin-left:8px;">📁 Lịch sử khám</a>
    </div>
</div>
<jsp:include page="footer.jsp"/>
<script src="${pageContext.request.contextPath}/static/js/main.js"></script>
</body>
</html>
