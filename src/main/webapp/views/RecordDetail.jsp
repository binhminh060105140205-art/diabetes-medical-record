<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Chi Tiết Hồ Sơ Bệnh Án</title>
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css?v=20260721-web-audit1">
</head>
<body>
<jsp:include page="header.jsp"/>
<jsp:include page="topnav.jsp"/>
<div class="page-wrapper">
    <div class="page-heading"><div><div class="eyebrow">CHI TIẾT LẦN KHÁM</div><h1 class="page-title">Bệnh án #${detail.record.recordId}</h1><p class="text-muted">Ngày khám ${detail.record.visitDateLabel} · <span class="status-pill status-${detail.record.status}">${detail.record.status=='COMPLETED'?'Đã hoàn tất':'Đang xử lý'}</span></p></div><div class="heading-actions"><a href="${pageContext.request.contextPath}/PatientHistory?patientId=${detail.patient.patientId}" class="btn btn-light">Lịch sử khám</a></div></div>
    <div class="patient-summary-bar"><strong><c:out value="${detail.patient.fullName}"/></strong><div class="patient-summary-meta"><span>SĐT: <c:out value="${detail.patient.phone}" default="—"/></span><span>BHYT: <c:out value="${detail.patient.healthInsuranceNo}" default="—"/></span><span>Bác sĩ: <c:out value="${detail.doctor.fullName}"/></span></div></div>

    <c:if test="${not empty diabetesProfile}">
    <div class="card accent-card">
        <div class="card-title">Hồ sơ tiểu đường hiện tại</div>
        <table class="detail-table">
            <tr><th class="label-cell">Loại tiểu đường</th><td><strong>${diabetesProfile.diabetesTypeLabel}</strong></td>
                <th class="label-cell label-cell-small">Ngày phát hiện</th><td>${not empty diabetesProfile.diagnosisDate?diabetesProfile.diagnosisDate:'—'}</td></tr>
            <tr><th>Phương pháp điều trị</th><td>${diabetesProfile.treatmentMethodLabel}</td>
                <th>Mục tiêu HbA1c</th><td>${not empty diabetesProfile.hba1cTarget?diabetesProfile.hba1cTarget:'—'}<c:if test="${not empty diabetesProfile.hba1cTarget}">%</c:if></td></tr>
        </table>
    </div>
    </c:if>

    <!-- I. PATIENT INFO -->
    <div class="card">
        <div class="card-title">I. Thông tin bệnh nhân</div>
        <table class="detail-table">
            <tr><th class="label-cell">Họ và tên</th><td><strong><c:out value="${detail.patient.fullName}"/></strong></td>
                <th class="label-cell label-cell-small">Ngày sinh</th><td>${detail.patient.dateOfBirth}</td></tr>
            <tr><th>Giới tính</th><td><c:out value="${detail.patient.genderLabel}"/></td>
                <th>Số điện thoại</th><td><c:out value="${detail.patient.phone}"/></td></tr>
            <tr><th>Địa chỉ</th><td colspan="3"><c:out value="${detail.patient.address}"/></td></tr>
            <tr><th>Số BHYT</th><td><c:out value="${detail.patient.healthInsuranceNo}"/></td>
                <th>Ngày khám</th><td>${detail.record.visitDateLabel}</td></tr>
            <tr><th>Bác sĩ khám</th><td colspan="3"><c:out value="${detail.doctor.fullName}"/> — <c:out value="${detail.doctor.specialty}"/></td></tr>
        </table>
    </div>

    <!-- II. CLINICAL INFO -->
    <div class="card">
        <div class="card-title">II. Thông tin khám</div>
        <table class="detail-table">
            <tr><th class="label-cell">Lý do khám</th><td><c:out value="${detail.record.reasonForVisit}"/></td></tr>
            <tr><th>Triệu chứng</th><td><c:out value="${detail.record.symptoms}"/></td></tr>
            <tr><th>Tiền sử bệnh</th><td><c:out value="${detail.record.medicalHistory}"/></td></tr>
            <tr><th>Thói quen sinh hoạt</th><td><c:out value="${detail.record.lifestyleHabits}"/></td></tr>
            <tr><th>Khám lâm sàng</th><td><c:out value="${detail.record.clinicalExam}"/></td></tr>
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
                <div class="ind-label">BMI <span>(chỉ số khối cơ thể)</span></div>
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
                <div class="ind-value">${not empty detail.indicator.bloodGlucose?detail.indicator.bloodGlucose:'—'}</div>
                <div class="ind-unit"><c:if test="${not empty detail.indicator.bloodGlucose}">mmol/L</c:if></div>
            </div>
            <div class="indicator-item">
                <div class="ind-label">HbA1c</div>
                <div class="ind-value">${not empty detail.indicator.hba1c?detail.indicator.hba1c:'—'}</div>
                <div class="ind-unit"><c:if test="${not empty detail.indicator.hba1c}">%</c:if></div>
            </div>
            <div class="indicator-item">
                <div class="ind-label">Cholesterol</div>
                <div class="ind-value">${not empty detail.indicator.cholesterol?detail.indicator.cholesterol:'—'}</div>
                <div class="ind-unit"><c:if test="${not empty detail.indicator.cholesterol}">mmol/L</c:if></div>
            </div>
            <div class="indicator-item">
                <div class="ind-label">Triglyceride</div>
                <div class="ind-value">${not empty detail.indicator.triglyceride?detail.indicator.triglyceride:'—'}</div>
                <div class="ind-unit"><c:if test="${not empty detail.indicator.triglyceride}">mmol/L</c:if></div>
            </div>
            <div class="indicator-item">
                <div class="ind-label">HDL-C</div>
                <div class="ind-value">${not empty detail.indicator.hdlC?detail.indicator.hdlC:'—'}</div>
                <div class="ind-unit"><c:if test="${not empty detail.indicator.hdlC}">mmol/L</c:if></div>
            </div>
            <div class="indicator-item">
                <div class="ind-label">LDL-C</div>
                <div class="ind-value">${not empty detail.indicator.ldlC?detail.indicator.ldlC:'—'}</div>
                <div class="ind-unit"><c:if test="${not empty detail.indicator.ldlC}">mmol/L</c:if></div>
            </div>
        </div>
    </div>
    </c:if>

    <!-- IV. DOCTOR CONCLUSION -->
    <div class="card">
        <div class="card-title">IV. Kết luận của Bác sĩ</div>
        <c:choose>
            <c:when test="${detail.record.status == 'COMPLETED'}">
                <table class="detail-table">
                    <tr><th class="label-cell">Biến chứng</th><td><c:out value="${detail.record.complicationNote}"/></td></tr>
                    <tr><th>Chẩn đoán</th><td><strong><c:out value="${detail.record.finalDiagnosis}"/></strong></td></tr>
                    <tr><th>Hướng điều trị</th><td><c:out value="${detail.record.treatmentPlan}"/></td></tr>
                    <tr><th>Đơn thuốc</th><td>
                        <c:choose><c:when test="${not empty prescriptionItems}">
                            <c:forEach items="${prescriptionItems}" var="item">
                                <div><strong><c:out value="${item.medicineName}"/></strong> — <c:out value="${item.dosage}"/>
                                    <c:if test="${not empty item.frequency}">, <c:out value="${item.frequency}"/></c:if>
                                    <c:if test="${not empty item.durationDays}"> trong ${item.durationDays} ngày</c:if></div>
                            </c:forEach>
                        </c:when><c:otherwise><c:out value="${detail.record.prescriptionNote}"/></c:otherwise></c:choose>
                    </td></tr>
                    <tr><th>Lời dặn</th><td><c:out value="${detail.record.advice}"/></td></tr>
                    <tr><th>Ngày tái khám</th><td><strong>${detail.record.followUpDate}</strong></td></tr>
                    <tr><th>Ghi chú thêm</th><td><c:out value="${detail.record.doctorNote}"/></td></tr>
                </table>
            </c:when>
            <c:otherwise>
                <div class="alert alert-info">
                    ⏳ Bác sĩ chưa hoàn tất kết luận.
                    <c:if test="${sessionScope.user.role == 'DOCTOR'}">
                        <a href="${pageContext.request.contextPath}/MedicalRecordForm?recordId=${detail.record.recordId}&tab=4"
                           class="btn btn-primary btn-sm">Nhập kết luận</a>
                    </c:if>
                </div>
            </c:otherwise>
        </c:choose>
    </div>

</div>
<jsp:include page="footer.jsp"/>
</body>
</html>
