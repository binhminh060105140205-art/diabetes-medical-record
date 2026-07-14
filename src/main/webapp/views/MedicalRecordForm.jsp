<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Hồ Sơ Bệnh Án</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
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
    <div style="display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:10px;margin-bottom:4px;">
        <h1 class="page-title" style="margin:0;">📋 Hồ Sơ Bệnh Án</h1>
        <div>
            <strong>${patient.fullName}</strong>
            <span class="text-muted"> | BHYT: ${patient.healthInsuranceNo} | SĐT: ${patient.phone}</span>
            <c:if test="${not empty assignedDoctor}">
                <span class="text-muted"> | BS: <strong>${assignedDoctor.fullName}</strong></span>
            </c:if>
        </div>
    </div>

    <%-- Hướng dẫn theo role --%>
    <c:choose>
        <c:when test="${sessionScope.user.role == 'STAFF'}">
        <div class="role-bar role-staff">
            👩‍💼 <strong>Nhân viên tiếp nhận:</strong>
            Tab 1 — Nhập thông tin khám ban đầu.
            Tab 2 — Nhập chỉ số lâm sàng (chiều cao, cân nặng, huyết áp, nhịp tim).
            Tab 3 — Nhập chỉ số xét nghiệm từ phòng lab (Glucose, HbA1c, Cholesterol...). Sau khi lưu AI tự phân tích.
            Tab 4 — Do Bác sĩ nhập kết luận.
        </div>
        </c:when>
        <c:when test="${sessionScope.user.role == 'DOCTOR'}">
        <div class="role-bar role-doctor">
            🩺 <strong>Bác sĩ:</strong>
            Tab 4 — Nhập kết luận, đơn thuốc và lịch tái khám.
        </div>
        </c:when>
    </c:choose>

    <%-- Server-side validation errors --%>
    <c:if test="${not empty serverErrors}">
    <div class="alert alert-danger">
        <strong>⚠ Dữ liệu không hợp lệ:</strong>
        <ul style="margin:6px 0 0 18px;">
            <c:forEach var="e" items="${serverErrors}"><li>${e}</li></c:forEach>
        </ul>
    </div>
    </c:if>

    <%-- TAB BAR --%>
    <div class="tab-bar">
        <button class="tab-btn ${sessionScope.user.role!='STAFF'?'locked':''} ${not empty record?'done':''}"
                onclick="showTab(1)" id="btn1">1️⃣ Thông tin khám</button>
        <button class="tab-btn ${sessionScope.user.role!='STAFF'?'locked':''} ${clinicalDone?'done':''}"
                onclick="showTab(2)" id="btn2">2️⃣ Lâm sàng <small>(Staff)</small></button>
        <button class="tab-btn ${sessionScope.user.role!='STAFF'?'locked':''} ${labDone?'done':''}"
                onclick="showTab(3)" id="btn3">3️⃣ Xét nghiệm <small>(Staff)</small></button>
        <button class="tab-btn ${sessionScope.user.role!='DOCTOR'?'locked':''}"
                onclick="showTab(4)" id="btn5">5️⃣ Kết luận <small>(Bác sĩ)</small></button>
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
                <button type="submit" class="btn btn-primary">💾 Lưu → Sang Tab 2</button>
            </form>
        </div>
    </c:when>
    <c:otherwise>
        <div class="card">
            <div class="card-title">I. Thông tin khám ban đầu <span class="text-muted">(Staff đã nhập)</span></div>
            <c:choose>
            <c:when test="${not empty record}">
                <table style="box-shadow:none;">
                    <tr><th style="width:200px">Lý do khám</th><td>${record.reasonForVisit}</td></tr>
                    <tr><th>Triệu chứng</th><td>${record.symptoms}</td></tr>
                    <tr><th>Tiền sử</th><td>${record.medicalHistory}</td></tr>
                    <tr><th>Thói quen</th><td>${record.lifestyleHabits}</td></tr>
                    <tr><th>Lâm sàng</th><td>${record.clinicalExam}</td></tr>
                </table>
            </c:when>
            <c:otherwise>
                <div class="alert alert-info">⏳ Staff chưa nhập thông tin khám.</div>
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
            <div class="card-title">II. Chỉ số lâm sàng <span style="font-size:13px;color:#0dcaf0;">(Nhân viên nhập)</span></div>
            <div class="alert alert-info" style="margin-bottom:14px;font-size:13px;">
                ✏️ Tab này Staff nhập các chỉ số đo trực tiếp tại phòng khám.
                Chỉ số xét nghiệm (Glucose, HbA1c...) sẽ do Bác sĩ nhập ở Tab 3.
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
                        <label>BMI (tự tính)</label>
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
                <button type="submit" class="btn btn-primary">💾 Lưu lâm sàng → Chuyển cho Bác sĩ</button>
            </form>
        </div>
    </c:when>
    <c:otherwise>
        <div class="card">
            <div class="card-title">II. Chỉ số lâm sàng <span class="text-muted">(Staff đã nhập)</span></div>
            <c:choose>
            <c:when test="${clinicalDone}">
                <div class="indicator-grid">
                    <div class="indicator-item"><div class="ind-label">Chiều cao</div><div class="ind-value">${indicator.height}</div><div class="ind-unit">cm</div></div>
                    <div class="indicator-item"><div class="ind-label">Cân nặng</div><div class="ind-value">${indicator.weight}</div><div class="ind-unit">kg</div></div>
                    <div class="indicator-item"><div class="ind-label">BMI</div><div class="ind-value">${indicator.bmi}</div><div class="ind-unit">kg/m²</div></div>
                    <div class="indicator-item"><div class="ind-label">Huyết áp</div><div class="ind-value">${indicator.systolicBp}/${indicator.diastolicBp}</div><div class="ind-unit">mmHg</div></div>
                    <div class="indicator-item"><div class="ind-label">Nhịp tim</div><div class="ind-value">${indicator.heartRate}</div><div class="ind-unit">lần/phút</div></div>
                    <div class="indicator-item"><div class="ind-label">Nhiệt độ</div><div class="ind-value">${indicator.temperature}</div><div class="ind-unit">°C</div></div>
                </div>
            </c:when>
            <c:otherwise>
                <div class="alert alert-info">⏳ Staff chưa nhập chỉ số lâm sàng.</div>
            </c:otherwise>
            </c:choose>
        </div>
    </c:otherwise>
    </c:choose>
    </div>

    <%-- ══ TAB 3: XÉT NGHIỆM (STAFF) ════════════════════════════════ --%>
    <div class="tab-panel" id="tab3">
    <c:choose>
    <c:when test="${sessionScope.user.role == 'STAFF'}">
        <div class="card">
            <div class="card-title" style="display: flex; justify-content: space-between; align-items: center;">
                <span>III. Chỉ số xét nghiệm <span style="font-size:13px;color:#0dcaf0;">(Nhân viên nhập từ kết quả lab)</span></span>
            </div>
            <div class="alert alert-success" style="margin-bottom:14px;font-size:13px;">
                🩺 Bác sĩ nhập kết quả xét nghiệm từ phòng lab. AI sẽ tự phân tích ngay sau khi lưu.
            </div>
            <form action="${pageContext.request.contextPath}/MedicalRecordForm" method="post">
                <input type="hidden" name="action" value="saveLabIndicators">
                <input type="hidden" name="recordId" value="${record.recordId}">

                <div id="warningBox" class="warning-box"></div>

                <div class="form-row">
                    <div class="form-group">
                        <label>Đường huyết lúc đói (mg/dL) <span class="range-hint">BT &lt;100</span></label>
                        <input type="number" step="0.1" name="bloodGlucose" class="form-control"
                               value="${indicator.bloodGlucose}" placeholder="90" min="20" max="600"
                               oninput="hintBG(this)">
                        <span id="hint_bg" class="err-msg" style="color:#888;"></span>
                    </div>
                    <div class="form-group">
                        <label>HbA1c (%) <span class="range-hint">BT &lt;5.7 | Hợp lệ: 3–20%</span></label>
                        <input type="number" step="0.1" name="hba1c" class="form-control"
                               value="${indicator.hba1c}" placeholder="5.5" min="3" max="20"
                               oninput="hintHba1c(this)">
                        <span id="hint_hba1c" class="err-msg" style="color:#888;"></span>
                    </div>
                    <div class="form-group">
                        <label>Cholesterol (mg/dL) <span class="range-hint">BT &lt;200</span></label>
                        <input type="number" step="0.1" name="cholesterol" class="form-control"
                               value="${indicator.cholesterol}" placeholder="180" min="50" max="700">
                    </div>
                    <div class="form-group">
                        <label>Triglyceride (mg/dL) <span class="range-hint">BT &lt;150</span></label>
                        <input type="number" step="0.1" name="triglyceride" class="form-control"
                               value="${indicator.triglyceride}" placeholder="130" min="20" max="2000">
                    </div>
                    <div class="form-group">
                        <label>HDL-C (mg/dL) <span class="range-hint">BT &gt;40</span></label>
                        <input type="number" step="0.1" name="hdlC" class="form-control"
                               value="${indicator.hdlC}" placeholder="55" min="10" max="150">
                    </div>
                    <div class="form-group">
                        <label>LDL-C (mg/dL) <span class="range-hint">BT &lt;100</span></label>
                        <input type="number" step="0.1" name="ldlC" class="form-control"
                               value="${indicator.ldlC}" placeholder="100" min="10" max="400">
                    </div>
                </div>

                <div class="alert alert-success" style="margin-top:8px;font-size:13px;">
                    📊 Sau khi bấm Lưu, AI sẽ <strong>tự động phân tích</strong> ngầm và lưu kết quả. Bác sĩ sẽ xem trong hồ sơ chi tiết.
                </div>
                <button type="submit" class="btn btn-success" style="font-size:15px;">
                    💾 Lưu chỉ số xét nghiệm →
                </button>
            </form>
        </div>
    </c:when>
    <c:otherwise>
        <div class="card">
            <div class="card-title">III. Chỉ số xét nghiệm</div>
            <c:choose>
            <c:when test="${labDone}">
                <div class="indicator-grid">
                    <div class="indicator-item"><div class="ind-label">Đường huyết</div><div class="ind-value">${indicator.bloodGlucose}</div><div class="ind-unit">mg/dL</div></div>
                    <div class="indicator-item"><div class="ind-label">HbA1c</div><div class="ind-value">${indicator.hba1c}</div><div class="ind-unit">%</div></div>
                    <div class="indicator-item"><div class="ind-label">Cholesterol</div><div class="ind-value">${indicator.cholesterol}</div><div class="ind-unit">mg/dL</div></div>
                    <div class="indicator-item"><div class="ind-label">Triglyceride</div><div class="ind-value">${indicator.triglyceride}</div><div class="ind-unit">mg/dL</div></div>
                    <div class="indicator-item"><div class="ind-label">HDL-C</div><div class="ind-value">${indicator.hdlC}</div><div class="ind-unit">mg/dL</div></div>
                    <div class="indicator-item"><div class="ind-label">LDL-C</div><div class="ind-value">${indicator.ldlC}</div><div class="ind-unit">mg/dL</div></div>
                </div>
            </c:when>
            <c:otherwise>
                <div class="alert alert-info">⏳ Chờ Bác sĩ nhập kết quả xét nghiệm.</div>
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
            <div class="card-title">V. Kết luận của Bác sĩ</div>
            <c:if test="${not empty warning}">
            <div class="ai-panel ${warning.riskLevel}" style="padding:10px 16px;margin-bottom:16px;">
                <small>🤖 AI: <strong><span class="risk-badge risk-${warning.riskLevel}">${warning.riskLevel}</span></strong>
                — Điểm: ${warning.aiScore} | ${warning.warningMessage}</small>
            </div>
            </c:if>
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
                        placeholder="VD: Tiểu đường type 2 kiểm soát kém, kèm rối loạn mỡ máu...">${record.finalDiagnosis}</textarea>
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
                    <label style="margin-top:10px">Ghi chú chung</label>
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
                <button type="submit" class="btn btn-success" style="font-size:15px;padding:12px 28px;">
                    ✅ Hoàn tất hồ sơ bệnh án
                </button>
            </form>
        </div>
    </c:when>
    <c:otherwise>
        <div class="card">
            <div class="card-title">V. Kết luận của Bác sĩ</div>
            <c:choose>
            <c:when test="${record.status == 'COMPLETED'}">
                <table style="box-shadow:none;">
                    <tr><th style="width:200px">Chẩn đoán</th><td><strong>${record.finalDiagnosis}</strong></td></tr>
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

<script src="${pageContext.request.contextPath}/static/js/main.js"></script>
<script>
function showTab(n) {
    document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    var panel = document.getElementById('tab'+n);
    var btns  = document.querySelectorAll('.tab-btn');
    if (panel) panel.classList.add('active');
    if (btns[n-1]) btns[n-1].classList.add('active');
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

function hintBG(el) {
    var v = parseFloat(el.value);
    var hint = document.getElementById('hint_bg');
    if (!hint || !v) return;
    if (v < 70) hint.innerHTML = '<span style="color:#dc2626">⚠ Thấp — có thể hạ đường huyết</span>';
    else if (v <= 99) hint.innerHTML = '<span style="color:#16a34a">✓ Bình thường</span>';
    else if (v <= 125) hint.innerHTML = '<span style="color:#d97706">⚠ Tiền tiểu đường</span>';
    else hint.innerHTML = '<span style="color:#dc2626">⛔ Cao — nguy cơ tiểu đường</span>';
}

function hintHba1c(el) {
    var v = parseFloat(el.value);
    var hint = document.getElementById('hint_hba1c');
    if (!hint || !v) return;
    if (v < 3 || v > 20) hint.innerHTML = '<span style="color:#dc2626">⛔ Ngoài khoảng hợp lệ (3%–20%)</span>';
    else if (v < 5.7) hint.innerHTML = '<span style="color:#16a34a">✓ Bình thường</span>';
    else if (v < 6.5) hint.innerHTML = '<span style="color:#d97706">⚠ Tiền tiểu đường</span>';
    else hint.innerHTML = '<span style="color:#dc2626">⛔ Tiểu đường — cần điều trị</span>';
}

var urlTab = new URLSearchParams(window.location.search).get('tab');
var role = '${sessionScope.user.role}';
var serverErrTab = '${activeTab}';
var defaultTab = serverErrTab ? parseInt(serverErrTab)
               : urlTab ? parseInt(urlTab)
               : (role === 'DOCTOR' ? 4 : 1);
showTab(defaultTab);
</script>
<jsp:include page="footer.jsp"/>
</body>
</html>
