<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Hồ Sơ Bệnh Án</title>
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css?v=20260720-ux1">
    <style>
        .role-bar{padding:10px 16px;border-radius:8px;margin-bottom:16px;font-size:14px;font-weight:600;}
        .role-staff {background:#cff4fc;color:#055160;border-left:4px solid #0dcaf0;}
        .role-doctor{background:#d1e7dd;color:#0f5132;border-left:4px solid #198754;}
        .tab-btn.locked{opacity:.45;cursor:not-allowed;pointer-events:none;}
        .tab-btn.done::after{content:' ✓';color:#198754;}
        .section-label{font-size:13px;font-weight:700;color:#6c757d;text-transform:uppercase;
                        letter-spacing:.5px;margin:14px 0 8px;padding-bottom:4px;
                        border-bottom:1px solid #e2e8f0;}
        .ind-readonly{background:#f8fafc!important;color:#374151;font-weight:600;}
    </style>
</head>
<body>
<jsp:include page="header.jsp"/>
<jsp:include page="topnav.jsp"/>
<div class="page-wrapper">
    <c:if test="${not empty sessionScope.recordFlash}"><div class="alert alert-danger"><c:out value="${sessionScope.recordFlash}"/></div><c:remove var="recordFlash" scope="session"/></c:if>
    <div class="page-heading">
        <div><div class="eyebrow">HỒ SƠ LƯỢT KHÁM</div><h1 class="page-title">Bệnh án ${not empty record?'#'.concat(record.recordId):'mới'}</h1><p class="text-muted">Chỉ nhập phần thuộc vai trò của bạn; dữ liệu đã lưu được giữ nguyên giữa các bước.</p></div>
        <a class="btn btn-light" href="${pageContext.request.contextPath}/ClinicWorkflow?view=encounters">Quay lại lượt khám</a>
    </div>
    <div class="patient-summary-bar">
        <strong><c:out value="${patient.fullName}"/></strong>
        <div class="patient-summary-meta"><span>SĐT: <c:out value="${patient.phone}" default="—"/></span><span>BHYT: <c:out value="${patient.healthInsuranceNo}" default="—"/></span><c:if test="${not empty assignedDoctor}"><span>Bác sĩ: <strong><c:out value="${assignedDoctor.fullName}"/></strong></span></c:if></div>
    </div>

    <%-- Hướng dẫn theo vai trò --%>
    <c:choose>
        <c:when test="${sessionScope.user.role == 'STAFF'}">
        <div class="role-bar role-staff">
            👩‍💼 <strong>Nhân viên tiếp nhận:</strong>
            Nhập thông tin ban đầu rồi đo sinh hiệu. Kết quả xét nghiệm được nhập tại mục Xét nghiệm để tránh trùng dữ liệu.
        </div>
        </c:when>
        <c:when test="${sessionScope.user.role == 'DOCTOR'}">
        <div class="role-bar role-doctor">
            🩺 <strong>Bác sĩ:</strong>
            Xem tóm tắt tiểu đường, nhật ký sức khỏe, kết quả xét nghiệm rồi nhập kết luận, đơn thuốc và ngày tái khám.
        </div>
        </c:when>
    </c:choose>

    <div class="record-progress" aria-label="Tiến độ bệnh án">
        <div class="record-step ${not empty record?'done':sessionScope.user.role=='STAFF'?'active':''}">1. Thông tin khám</div>
        <div class="record-step ${clinicalDone?'done':not empty record&&sessionScope.user.role=='STAFF'?'active':''}">2. Sinh hiệu</div>
        <div class="record-step ${record.status=='COMPLETED'?'done':sessionScope.user.role=='DOCTOR'?'active':''}">3. Kết luận bác sĩ</div>
    </div>

    <%-- TÓM TẮT HỒ SƠ TIỂU ĐƯỜNG — chỉ đọc, hiển thị cho nhân viên và bác sĩ --%>
    <c:if test="${not empty diabetesProfile}">
    <div class="card accent-card">
        <div class="card-title">🩸 Tóm tắt hồ sơ tiểu đường <c:if test="${sessionScope.user.role=='DOCTOR'}"><a class="btn btn-outline-dark btn-sm" href="${pageContext.request.contextPath}/DoctorPatientJournal?patientId=${patient.patientId}">Xem nhật ký 30 ngày</a></c:if></div>
        <div class="indicator-grid">
            <div class="indicator-item"><div class="ind-label">Loại tiểu đường</div>
                <div class="ind-value">${diabetesProfile.diabetesTypeLabel}</div></div>
            <div class="indicator-item"><div class="ind-label">Ngày phát hiện</div>
                <div class="ind-value">${not empty diabetesProfile.diagnosisDate?diabetesProfile.diagnosisDate:'—'}</div></div>
            <div class="indicator-item"><div class="ind-label">Phương pháp điều trị</div>
                <div class="ind-value">${diabetesProfile.treatmentMethodLabel}</div></div>
            <div class="indicator-item"><div class="ind-label">Mục tiêu HbA1c</div>
                <div class="ind-value">${not empty diabetesProfile.hba1cTarget?diabetesProfile.hba1cTarget:'—'}</div><div class="ind-unit">%</div></div>
            <div class="indicator-item"><div class="ind-label">HbA1c gần nhất</div>
                <div class="ind-value">${latestIndicator.hba1c}</div><div class="ind-unit">%</div></div>
            <div class="indicator-item"><div class="ind-label">Đường huyết gần nhất</div>
                <div class="ind-value">${latestIndicator.bloodGlucose}</div><div class="ind-unit">mg/dL</div></div>
            <div class="indicator-item"><div class="ind-label">Huyết áp gần nhất</div>
                <div class="ind-value">${latestIndicator.systolicBp}/${latestIndicator.diastolicBp}</div><div class="ind-unit">mmHg</div></div>
            <div class="indicator-item"><div class="ind-label">Chỉ số khối cơ thể gần nhất</div>
                <div class="ind-value">${latestIndicator.bmi}</div></div>
        </div>
    </div>
    </c:if>

    <%-- Lỗi kiểm tra dữ liệu từ máy chủ --%>
    <c:if test="${not empty serverErrors}">
    <div class="alert alert-danger">
        <strong>⚠ Dữ liệu không hợp lệ:</strong>
        <ul class="error-list">
            <c:forEach var="e" items="${serverErrors}"><li>${e}</li></c:forEach>
        </ul>
    </div>
    </c:if>

    <%-- Thanh chuyển bước --%>
    <div class="tab-bar">
        <button class="tab-btn ${sessionScope.user.role!='STAFF'?'locked':''} ${not empty record?'done':''}"
                onclick="showTab(1)" id="btn1">1. Thông tin khám</button>
        <button class="tab-btn ${sessionScope.user.role!='STAFF'?'locked':''} ${clinicalDone?'done':''}"
                onclick="showTab(2)" id="btn2">2. Sinh hiệu <small>(Nhân viên)</small></button>
        <button class="tab-btn ${sessionScope.user.role!='DOCTOR'?'locked':''}"
                onclick="showTab(4)" id="btn4">3. Kết luận <small>(Bác sĩ)</small></button>
    </div>

    <%-- ══ TAB 1: THÔNG TIN KHÁM (STAFF) ══════════════════════════════ --%>
    <div class="tab-panel" id="tab1">
    <c:choose>
    <c:when test="${sessionScope.user.role == 'STAFF'}">
        <div class="card">
            <div class="card-title">I. Thông tin khám ban đầu</div>
            <form action="${pageContext.request.contextPath}/MedicalRecordForm" method="post">
                <input type="hidden" name="action" value="saveBasic">
                <input type="hidden" name="patientId" value="${patient.patientId}">
                <input type="hidden" name="encounterId" value="${encounterId}">
                <div class="form-group"><label>Ngày giờ khám</label><input class="form-control" value="${not empty appointmentTime?appointmentTime:record.visitDate}" readonly><small>Ngày khám lấy từ lịch hẹn hoặc thời điểm ghi nhận đến khám và không chỉnh sửa tại bệnh án.</small></div>

                <div class="form-group">
                    <label class="required">Bác sĩ phụ trách</label>
                    <select name="doctorId" class="form-control" required>
                        <option value="">-- Chọn bác sĩ --</option>
                        <c:forEach var="doc" items="${doctors}">
                            <option value="${doc.doctorId}"
                                <c:if test="${record.doctorId==doc.doctorId}">selected</c:if>>
                                ${doc.fullName} — ${doc.specialty}
                            </option>
                        </c:forEach>
                    </select>
                </div>
                <div class="form-group">
                    <label class="required">Lý do đến khám</label>
                    <textarea name="reasonForVisit" class="form-control" required
                        placeholder="Bệnh nhân đến khám vì...">${record.reasonForVisit}</textarea>
                </div>
                <div class="form-group">
                    <label>Triệu chứng hiện tại</label>
                    <textarea name="symptoms" class="form-control"
                        placeholder="Khát nhiều, tiểu nhiều, mệt mỏi, sụt cân, nhìn mờ, tê chân tay...">${record.symptoms}</textarea>
                </div>
                <div class="form-group">
                    <label>Tiền sử bệnh</label>
                    <textarea name="medicalHistory" class="form-control"
                        placeholder="Tiểu đường, cao huyết áp, tim mạch, rối loạn mỡ máu...">${record.medicalHistory}</textarea>
                </div>
                <div class="form-group">
                    <label>Thói quen sinh hoạt</label>
                    <textarea name="lifestyleHabits" class="form-control"
                        placeholder="Ăn nhiều đồ ngọt, ít vận động, hút thuốc, uống rượu...">${record.lifestyleHabits}</textarea>
                </div>
                <div class="form-group">
                    <label>Ghi chú lâm sàng ban đầu</label>
                    <textarea name="clinicalExam" class="form-control"
                        placeholder="Quan sát tổng quát, thần sắc...">${record.clinicalExam}</textarea>
                </div>
                <div class="form-actions"><button type="submit" class="btn btn-primary">Lưu và chuyển sang nhập sinh hiệu</button></div>
            </form>
        </div>
    </c:when>
    <c:otherwise>
        <div class="card">
            <div class="card-title">I. Thông tin khám ban đầu <span class="text-muted">(Nhân viên đã nhập)</span></div>
            <c:choose>
            <c:when test="${not empty record}">
                <table class="detail-table">
                    <tr><th class="label-cell">Lý do khám</th><td>${record.reasonForVisit}</td></tr>
                    <tr><th>Triệu chứng</th><td>${record.symptoms}</td></tr>
                    <tr><th>Tiền sử</th><td>${record.medicalHistory}</td></tr>
                    <tr><th>Thói quen</th><td>${record.lifestyleHabits}</td></tr>
                    <tr><th>Lâm sàng</th><td>${record.clinicalExam}</td></tr>
                </table>
            </c:when>
            <c:otherwise>
                <div class="alert alert-info">Nhân viên chưa nhập thông tin khám.</div>
            </c:otherwise>
            </c:choose>
        </div>
    </c:otherwise>
    </c:choose>
    </div>

    <%-- ══ TAB 2: LÂM SÀNG (STAFF) ════════════════════════════════════ --%>
    <div class="tab-panel" id="tab2">
    <c:choose>
    <c:when test="${sessionScope.user.role == 'STAFF'}">
        <div class="card">
            <div class="card-title">II. Chỉ số lâm sàng <span class="role-inline-note">Nhân viên nhập</span></div>
            <div class="alert alert-info compact-alert">
                Nhân viên nhập các chỉ số đo trực tiếp tại phòng khám ở bước này.
                Kết quả xét nghiệm được nhập tại mục Xét nghiệm của luồng khám để tránh trùng dữ liệu.
            </div>
            <form action="${pageContext.request.contextPath}/MedicalRecordForm" method="post">
                <input type="hidden" name="action" value="saveClinical">
                <input type="hidden" name="recordId" value="${record.recordId}">

                <div class="form-row">
                    <div class="form-group">
                        <label>Chiều cao (cm) <span class="range-hint">50–250</span></label>
                        <input type="number" step="0.1" name="height" id="heightInput"
                               class="form-control" value="${indicator.height}"
                               oninput="calcBMI()" placeholder="165" min="50" max="250">
                    </div>
                    <div class="form-group">
                        <label>Cân nặng (kg) <span class="range-hint">10–300</span></label>
                        <input type="number" step="0.1" name="weight" id="weightInput"
                               class="form-control" value="${indicator.weight}"
                               oninput="calcBMI()" placeholder="65" min="10" max="300">
                    </div>
                    <div class="form-group">
                        <label>Chỉ số khối cơ thể (tự tính)</label>
                        <input type="text" id="bmiDisplay" class="form-control ind-readonly"
                               readonly value="${indicator.bmi}">
                    </div>
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label>Huyết áp tâm thu (mmHg) <span class="range-hint">60–250</span></label>
                        <input type="number" name="systolicBp" class="form-control"
                               value="${indicator.systolicBp}" placeholder="120" min="60" max="250">
                    </div>
                    <div class="form-group">
                        <label>Huyết áp tâm trương (mmHg) <span class="range-hint">40–150</span></label>
                        <input type="number" name="diastolicBp" class="form-control"
                               value="${indicator.diastolicBp}" placeholder="80" min="40" max="150">
                    </div>
                    <div class="form-group">
                        <label>Nhịp tim (lần/phút) <span class="range-hint">30–250</span></label>
                        <input type="number" name="heartRate" class="form-control"
                               value="${indicator.heartRate}" placeholder="75" min="30" max="250">
                    </div>
                    <div class="form-group">
                        <label>Nhiệt độ (°C) <span class="range-hint">34–42</span></label>
                        <input type="number" step="0.1" name="temperature" class="form-control"
                               value="${indicator.temperature}" placeholder="36.5" min="34" max="42">
                    </div>
                </div>
                <div class="form-actions"><button type="submit" class="btn btn-primary">Lưu sinh hiệu và chuyển chờ bác sĩ</button></div>
            </form>
        </div>
    </c:when>
    <c:otherwise>
        <div class="card">
            <div class="card-title">II. Chỉ số lâm sàng <span class="text-muted">(Nhân viên đã nhập)</span></div>
            <c:choose>
            <c:when test="${clinicalDone}">
                <div class="indicator-grid">
                    <div class="indicator-item"><div class="ind-label">Chiều cao</div><div class="ind-value">${indicator.height}</div><div class="ind-unit">cm</div></div>
                    <div class="indicator-item"><div class="ind-label">Cân nặng</div><div class="ind-value">${indicator.weight}</div><div class="ind-unit">kg</div></div>
                    <div class="indicator-item"><div class="ind-label">Chỉ số khối cơ thể</div><div class="ind-value">${indicator.bmi}</div><div class="ind-unit">kg/m²</div></div>
                    <div class="indicator-item"><div class="ind-label">Huyết áp</div><div class="ind-value">${indicator.systolicBp}/${indicator.diastolicBp}</div><div class="ind-unit">mmHg</div></div>
                    <div class="indicator-item"><div class="ind-label">Nhịp tim</div><div class="ind-value">${indicator.heartRate}</div><div class="ind-unit">lần/phút</div></div>
                    <div class="indicator-item"><div class="ind-label">Nhiệt độ</div><div class="ind-value">${indicator.temperature}</div><div class="ind-unit">°C</div></div>
                </div>
            </c:when>
            <c:otherwise>
                <div class="alert alert-info">Nhân viên chưa nhập chỉ số lâm sàng.</div>
            </c:otherwise>
            </c:choose>
        </div>
    </c:otherwise>
    </c:choose>
    </div>

    <%-- ══ TAB 4: KẾT LUẬN (DOCTOR) ══════════════════════════════════ --%>
    <div class="tab-panel" id="tab4">
    <c:choose>
    <c:when test="${sessionScope.user.role == 'DOCTOR'}">
        <div class="card">
            <div class="card-title">IV. Hồ sơ tiểu đường <span class="text-muted">(Bác sĩ xác nhận/cập nhật)</span></div>
            <form action="${pageContext.request.contextPath}/MedicalRecordForm" method="post">
                <input type="hidden" name="action" value="saveDiabetesProfile">
                <input type="hidden" name="recordId" value="${record.recordId}">
                <div class="form-row">
                    <div class="form-group">
                        <label>Loại tiểu đường (bác sĩ xác nhận)</label>
                        <select id="diabetesType" name="diabetesType" class="form-control" onchange="syncDiabetesTreatmentOptions()">
                            <option value="UNKNOWN" ${diabetesProfile.diabetesType=='UNKNOWN'?'selected':''}>Chưa xác định</option>
                            <option value="TYPE_1" ${diabetesProfile.diabetesType=='TYPE_1'?'selected':''}>Đái tháo đường típ 1</option>
                            <option value="TYPE_2" ${diabetesProfile.diabetesType=='TYPE_2'?'selected':''}>Đái tháo đường típ 2</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label>Ngày phát hiện bệnh</label>
                        <input type="date" name="diagnosisDate" class="form-control" value="${diabetesProfile.diagnosisDate}">
                    </div>
                    <div class="form-group">
                        <label>Phương pháp điều trị</label>
                        <select id="treatmentMethod" name="treatmentMethod" class="form-control">
                            <option value="">-- Chưa xác định --</option>
                            <option value="LIFESTYLE" ${diabetesProfile.treatmentMethod=='LIFESTYLE'?'selected':''}>Điều chỉnh ăn uống và vận động</option>
                            <option value="ORAL_MEDICATION" ${diabetesProfile.treatmentMethod=='ORAL_MEDICATION'?'selected':''}>Thuốc hạ đường huyết đường uống</option>
                            <option value="INSULIN" ${diabetesProfile.treatmentMethod=='INSULIN'?'selected':''}>Insulin</option>
                            <option value="COMBINATION" ${diabetesProfile.treatmentMethod=='COMBINATION'?'selected':''}>Điều trị phối hợp</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label>Mục tiêu HbA1c (%)</label>
                        <input type="number" min="4" max="15" step="0.1" name="hba1cTarget" class="form-control" value="${diabetesProfile.hba1cTarget}" placeholder="Do bác sĩ đặt">
                    </div>
                </div>
                <div class="diabetes-type-comparison" aria-label="Ảnh hưởng của loại đái tháo đường trong hệ thống">
                    <article data-diabetes-type="TYPE_1" class="diabetes-type-card ${diabetesProfile.diabetesType=='TYPE_1'?'active':''}"><strong>Đái tháo đường típ 1</strong><p>Hệ thống chỉ chấp nhận insulin hoặc điều trị phối hợp có insulin; nhắc theo dõi hạ đường huyết, liều và thời điểm tiêm.</p></article>
                    <article data-diabetes-type="TYPE_2" class="diabetes-type-card ${diabetesProfile.diabetesType=='TYPE_2'?'active':''}"><strong>Đái tháo đường típ 2</strong><p>Cho phép điều chỉnh lối sống, thuốc uống, insulin hoặc phối hợp; ưu tiên theo dõi cân nặng, huyết áp và mỡ máu.</p></article>
                </div>
                <p class="form-hint">Mục “ưu tiên típ 1/típ 2” trong hồ sơ bác sĩ chỉ hỗ trợ tra cứu, hiện chưa tự động phân bác sĩ khi xếp lịch.</p>
                <div class="form-actions"><button type="submit" class="btn btn-light">Lưu hồ sơ đái tháo đường</button></div>
            </form>
        </div>
        <div class="card">
            <div class="card-title">V. Kết luận của Bác sĩ</div>
            <c:if test="${diabetesProfile.diabetesType=='TYPE_1'}"><div class="alert alert-info"><strong>Gợi ý cho đái tháo đường típ 1:</strong> ghi loại insulin, liều, thời điểm dùng, số lần đo đường huyết và dấu hiệu hạ đường huyết. Bác sĩ vẫn là người quyết định điều trị.</div></c:if>
            <c:if test="${diabetesProfile.diabetesType=='TYPE_2'}"><div class="alert alert-info"><strong>Gợi ý cho đái tháo đường típ 2:</strong> ghi thuốc đang dùng, cân nặng, chỉ số khối cơ thể, huyết áp, mỡ máu và chế độ ăn vận động. Bác sĩ vẫn là người quyết định điều trị.</div></c:if>
            <c:if test="${not empty labSummary}"><div class="alert alert-info"><strong>Kết quả xét nghiệm:</strong> <c:out value="${labSummary}"/></div></c:if>
            <c:if test="${empty labSummary}"><div class="alert alert-info">Chưa có chỉ định hoặc kết quả xét nghiệm cho lượt khám này. Bác sĩ có thể kết luận nếu không cần xét nghiệm.</div></c:if>
            <form action="${pageContext.request.contextPath}/MedicalRecordForm" method="post">
                <input type="hidden" name="action" value="saveConclusion">
                <input type="hidden" name="recordId" value="${record.recordId}">
                <div class="form-group">
                    <label>Ghi chú biến chứng</label>
                    <textarea name="complicationNote" class="form-control"
                        placeholder="Hạ đường huyết, biến chứng thận, mắt, thần kinh...">${record.complicationNote}</textarea>
                </div>
                <div class="form-group">
                    <label class="required">Chẩn đoán cuối cùng</label>
                    <textarea name="finalDiagnosis" class="form-control" required
                        placeholder="Ví dụ: Đái tháo đường típ 2 kiểm soát chưa đạt, kèm rối loạn mỡ máu...">${record.finalDiagnosis}</textarea>
                </div>
                <div class="form-group">
                    <label>Hướng điều trị</label>
                    <textarea name="treatmentPlan" class="form-control"
                        placeholder="Điều chỉnh lối sống, thuốc uống, insulin...">${record.treatmentPlan}</textarea>
                </div>
                <div class="form-group">
                    <label>Đơn thuốc cơ bản</label>
                    <p class="text-muted">Chỉ nhập các thuốc cần thiết. Để trống dòng không sử dụng.</p>
                    <table class="prescription-table">
                        <thead><tr><th>Tên thuốc</th><th>Liều dùng</th><th>Số lần/ngày</th><th>Số ngày</th></tr></thead>
                        <tbody>
                        <c:forEach begin="0" end="2" var="i">
                            <tr>
                                <td><input name="medicineName" class="form-control" maxlength="150" value="${prescriptionItems[i].medicineName}" placeholder="Metformin 500mg"></td>
                                <td><input name="dosage" class="form-control" maxlength="100" value="${prescriptionItems[i].dosage}" placeholder="1 viên/lần"></td>
                                <td><input name="frequency" class="form-control" maxlength="100" value="${prescriptionItems[i].frequency}" placeholder="2 lần/ngày"></td>
                                <td><input name="durationDays" type="number" min="1" max="365" class="form-control" value="${prescriptionItems[i].durationDays}" placeholder="30"></td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                    <label class="top-gap">Ghi chú chung</label>
                    <textarea name="prescriptionNote" class="form-control" maxlength="500"
                        placeholder="Ví dụ: uống sau ăn">${record.prescriptionNote}</textarea>
                </div>
                <div class="form-group">
                    <label>Lời dặn bệnh nhân</label>
                    <textarea name="advice" class="form-control"
                        placeholder="Uống thuốc đúng giờ, kiểm tra đường huyết định kỳ...">${record.advice}</textarea>
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label>Ngày tái khám</label>
                        <input type="date" name="followUpDate" class="form-control"
                               value="${record.followUpDate}"
                               min="<%= java.time.LocalDate.now().plusDays(1) %>">
                    </div>
                    <div class="form-group">
                        <label>Ghi chú thêm</label>
                        <input type="text" name="doctorNote" class="form-control"
                               value="${record.doctorNote}">
                    </div>
                </div>
                <div class="form-actions conclusion-actions"><span class="form-hint">Sau khi hoàn tất, lượt khám và hàng đợi sẽ được đóng.</span><button type="submit" class="btn btn-success btn-lg">Hoàn tất hồ sơ bệnh án</button></div>
            </form>
        </div>
    </c:when>
    <c:otherwise>
        <div class="card">
            <div class="card-title">V. Kết luận của Bác sĩ</div>
            <c:choose>
            <c:when test="${record.status == 'COMPLETED'}">
                <table class="detail-table">
                    <tr><th class="label-cell">Chẩn đoán</th><td><strong>${record.finalDiagnosis}</strong></td></tr>
                    <tr><th>Hướng điều trị</th><td>${record.treatmentPlan}</td></tr>
                    <tr><th>Đơn thuốc</th><td>${record.prescriptionNote}</td></tr>
                    <tr><th>Lời dặn</th><td>${record.advice}</td></tr>
                    <tr><th>Tái khám</th><td><strong>${record.followUpDate}</strong></td></tr>
                </table>
            </c:when>
            <c:otherwise>
                <div class="alert alert-info">⏳ Chờ Bác sĩ nhập kết luận.</div>
            </c:otherwise>
            </c:choose>
        </div>
    </c:otherwise>
    </c:choose>
    </div>

    </div><%-- end tab4 --%>

</div><%-- end page-wrapper --%>

<script>
function showTab(n) {
    document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    var panel = document.getElementById('tab'+n);
    var button = document.getElementById('btn'+n);
    if (panel) panel.classList.add('active');
    if (button) button.classList.add('active');
    var u = new URL(window.location.href);
    u.searchParams.set('tab', n); window.history.replaceState({}, '', u);
}

function calcBMI() {
    var h = parseFloat(document.getElementById('heightInput') ? document.getElementById('heightInput').value : 0);
    var w = parseFloat(document.getElementById('weightInput') ? document.getElementById('weightInput').value : 0);
    var el = document.getElementById('bmiDisplay');
    if (el && h > 0 && w > 0) {
        var bmi = (w / ((h/100)*(h/100))).toFixed(1);
        el.value = bmi;
        el.style.color = bmi >= 30 ? '#dc3545' : bmi >= 25 ? '#fd7e14' : '#198754';
    }
}

function syncDiabetesTreatmentOptions() {
    var type = document.getElementById('diabetesType');
    var method = document.getElementById('treatmentMethod');
    if (!type || !method) return;
    var type1 = type.value === 'TYPE_1';
    ['LIFESTYLE', 'ORAL_MEDICATION'].forEach(function(value) {
        var option = method.querySelector('option[value="' + value + '"]');
        if (option) option.hidden = type1;
    });
    if (type1 && (method.value === 'LIFESTYLE' || method.value === 'ORAL_MEDICATION')) method.value = '';
    document.querySelectorAll('[data-diabetes-type]').forEach(function(panel) {
        panel.classList.toggle('active', panel.dataset.diabetesType === type.value);
    });
}

var urlTab = new URLSearchParams(window.location.search).get('tab');
var role = '${sessionScope.user.role}';
var serverErrTab = '${activeTab}';
var defaultTab = serverErrTab ? parseInt(serverErrTab)
               : urlTab ? parseInt(urlTab)
               : (role === 'DOCTOR' ? 4 : 1);
showTab(defaultTab);
syncDiabetesTreatmentOptions();
</script>
<jsp:include page="footer.jsp"/>
</body>
</html>
