<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Chi Tiết Hồ Sơ Bệnh Án</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css?v=20260719-ai1">
</head>
<body>
<jsp:include page="header.jsp"/>
<jsp:include page="topnav.jsp"/>
<div class="page-wrapper">
    <h1 class="page-title">📄 Hồ Sơ Bệnh Án #${detail.record.recordId}</h1>

    <c:if test="${not empty diabetesProfile}">
    <div class="card" style="border-left:4px solid #0d6efd;">
        <div class="card-title">🩸 Hồ sơ tiểu đường hiện tại</div>
        <table style="box-shadow:none;">
            <tr><th style="width:220px">Loại tiểu đường</th><td><strong>${diabetesProfile.diabetesTypeLabel}</strong></td>
                <th style="width:180px">Ngày phát hiện</th><td>${not empty diabetesProfile.diagnosisDate?diabetesProfile.diagnosisDate:'—'}</td></tr>
            <tr><th>Phương pháp điều trị</th><td>${diabetesProfile.treatmentMethodLabel}</td>
                <th>Mục tiêu HbA1c</th><td>${not empty diabetesProfile.hba1cTarget?diabetesProfile.hba1cTarget:'—'}<c:if test="${not empty diabetesProfile.hba1cTarget}">%</c:if></td></tr>
        </table>
    </div>
    </c:if>

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

    <!-- IV. DOCTOR CONCLUSION -->
    <div class="card">
        <div class="card-title">IV. Kết luận của Bác sĩ</div>
        <c:choose>
            <c:when test="${detail.record.status == 'COMPLETED'}">
                <table style="box-shadow:none;">
                    <tr><th style="width:220px">Biến chứng</th><td>${detail.record.complicationNote}</td></tr>
                    <tr><th>Chẩn đoán</th><td><strong>${detail.record.finalDiagnosis}</strong></td></tr>
                    <tr><th>Hướng điều trị</th><td>${detail.record.treatmentPlan}</td></tr>
                    <tr><th>Đơn thuốc</th><td>
                        <c:choose><c:when test="${not empty prescriptionItems}">
                            <c:forEach items="${prescriptionItems}" var="item">
                                <div><strong><c:out value="${item.medicineName}"/></strong> — <c:out value="${item.dosage}"/>
                                    <c:if test="${not empty item.frequency}">, <c:out value="${item.frequency}"/></c:if>
                                    <c:if test="${not empty item.durationDays}"> trong ${item.durationDays} ngày</c:if></div>
                            </c:forEach>
                        </c:when><c:otherwise><c:out value="${detail.record.prescriptionNote}"/></c:otherwise></c:choose>
                    </td></tr>
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
<script src="${pageContext.request.contextPath}/static/js/main.js?v=20260719-ai1"></script>
</body>
</html>
