<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Điều hành khám — DiaCare</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css?v=20260722-appointment4">
</head>
<body>
<jsp:include page="header.jsp"/>
<jsp:include page="topnav.jsp"/>

<main class="page-wrapper workflow-page">
    <div class="page-heading">
        <div>
            <div class="eyebrow">VẬN HÀNH NGOẠI TRÚ</div>
            <h1 class="page-title">Điều hành khám</h1>
        </div>
    </div>

    <c:if test="${not empty workflowFlash}"><div class="alert alert-info"><c:out value="${workflowFlash}"/></div></c:if>

    <nav class="module-tabs" aria-label="Các khu vực điều hành">
        <c:if test="${sessionScope.user.role=='STAFF'||sessionScope.user.role=='ADMIN'}">
            <a class="${view=='appointments'?'active':''}" href="${pageContext.request.contextPath}/ClinicWorkflow?view=appointments">Lịch hẹn</a>
        </c:if>
        <a class="${view=='encounters'?'active':''}" href="${pageContext.request.contextPath}/ClinicWorkflow?view=encounters">Lượt khám</a>
        <c:if test="${sessionScope.user.role=='DOCTOR'}">
            <a class="${view=='clinical'?'active':''}" href="${pageContext.request.contextPath}/ClinicWorkflow?view=clinical">Dị ứng & tiền sử</a>
        </c:if>
        <a class="${view=='labs'?'active':''}" href="${pageContext.request.contextPath}/ClinicWorkflow?view=labs">Xét nghiệm</a>
    </nav>

    <c:if test="${view=='appointments'}">
        <c:if test="${sessionScope.user.role=='STAFF'||sessionScope.user.role=='ADMIN'}">
            <details class="card disclosure-card">
                <summary>
                    <span><strong>Tạo lịch trực tiếp tại quầy</strong><small>Dùng khi bệnh nhân liên hệ trực tiếp và đã thống nhất bác sĩ, giờ khám.</small></span>
                    <span class="btn btn-light btn-sm">Mở biểu mẫu</span>
                </summary>
                <form method="post" action="${pageContext.request.contextPath}/ClinicWorkflow" class="appointment-booking-form disclosure-content" data-appointment-form data-gated-submit>
                    <input type="hidden" name="action" value="createAppointment">
                    <div class="appointment-form-grid">
                        <div class="form-group">
                            <label for="appointmentPatientSearch">Tìm bệnh nhân</label>
                            <input id="appointmentPatientSearch" class="form-control select-filter-input" type="search"
                                   data-select-filter="appointmentPatientId" autocomplete="off"
                                   placeholder="Nhập tên, số điện thoại hoặc loại tiểu đường">
                            <label class="required" for="appointmentPatientId">Bệnh nhân</label>
                            <select id="appointmentPatientId" class="form-control" name="patientId" required data-diabetes-patient>
                                <option value="">Chọn bệnh nhân</option>
                                <c:forEach var="p" items="${patients}"><option value="${p.patientId}" data-diabetes-type="${p.diabetesType}"><c:out value="${p.fullName}"/> — ${p.diabetesTypeLabel} — <c:out value="${p.phone}"/></option></c:forEach>
                            </select>
                            <small data-select-filter-status="appointmentPatientId">Gõ để thu hẹp danh sách bệnh nhân.</small>
                        </div>
                        <div class="form-group">
                            <label class="required">Bác sĩ điều trị tiểu đường</label>
                            <select class="form-control" name="doctorId" required data-diabetes-doctor>
                                <option value="">Chọn bác sĩ</option>
                                <c:forEach var="d" items="${doctors}"><option value="${d.doctorId}" data-diabetes-focus="${d.diabetesFocus}"><c:out value="${d.fullName}"/> — <c:out value="${d.diabetesFocusLabel}"/></option></c:forEach>
                            </select>
                            <small data-diabetes-routing>Chọn bệnh nhân để hệ thống lọc bác sĩ phù hợp.</small>
                        </div>
                        <div class="form-group appointment-form-wide">
                            <label class="required" for="appointmentAt">Ngày và giờ khám</label>
                            <input id="appointmentAt" class="form-control appointment-datetime-input" type="datetime-local"
                                   name="appointmentAt" min="${appointmentMinDateTime}" max="${appointmentMaxDateTime}"
                                   step="1800" required>
                            <small>Chọn theo khung 30 phút, từ 07:30–11:30 hoặc 13:00–17:00; phòng khám nghỉ Chủ nhật.</small>
                        </div>
                        <div class="form-group appointment-form-wide">
                            <label class="required">Lý do khám</label>
                            <input class="form-control" name="reason" minlength="5" maxlength="255" placeholder="Ví dụ: Tái khám tiểu đường định kỳ" required>
                        </div>
                        <div class="form-group appointment-form-wide">
                            <label>Ghi chú thêm</label>
                            <input class="form-control" name="note" maxlength="500" placeholder="Thông tin cần lưu ý khi sắp xếp lịch">
                        </div>
                    </div>
                    <div class="appointment-form-footer"><p>Kiểm tra đúng bệnh nhân, bác sĩ, ngày và khung giờ trước khi tạo lịch.</p><button class="btn btn-primary" type="submit">Tạo lịch khám</button></div>
                </form>
            </details>
        </c:if>

        <section class="card">
            <div class="section-header">
                <div><h2>Yêu cầu và lịch đã xác nhận</h2><p>Ưu tiên các dòng “Chờ xác nhận”, sau đó ghi nhận bệnh nhân đã đến khám.</p></div>
                <span class="data-count">${fn:length(appointments)} lịch đang hiển thị</span>
            </div>
            <div class="operations-toolbar">
                <label class="table-filter">
                    <span class="sr-only">Tìm lịch hẹn</span>
                    <input type="search" data-table-filter="appointmentTable" placeholder="Tìm theo bệnh nhân, điện thoại, bác sĩ hoặc trạng thái">
                </label>
            </div>
            <div class="table-scroll">
                <table id="appointmentTable">
                    <thead><tr><th>Ngày / giờ</th><th>Bệnh nhân</th><th>Bác sĩ</th><th>Lý do</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
                    <tbody>
                    <c:forEach var="a" items="${appointments}">
                        <tr data-search-row>
                            <td>
                                <c:choose>
                                    <c:when test="${empty a.appointment_at}"><strong><fmt:formatDate value="${a.preferred_date}" pattern="dd/MM/yyyy"/></strong><small class="table-sub">${a.preferred_period=='MORNING'?'Buổi sáng':'Buổi chiều'}</small></c:when>
                                    <c:otherwise><strong><fmt:formatDate value="${a.appointment_at}" pattern="dd/MM/yyyy · HH:mm"/></strong></c:otherwise>
                                </c:choose>
                            </td>
                            <td><strong><c:out value="${a.patient_name}"/></strong><small class="table-sub">${a.diabetes_type=='TYPE_1'?'Típ 1':a.diabetes_type=='TYPE_2'?'Típ 2':'Chưa phân loại'} · <c:out value="${a.patient_phone}"/></small></td>
                            <td><c:out value="${a.doctor_name}" default="Chưa phân công"/></td>
                            <td><c:out value="${a.reason}"/></td>
                            <td>
                                <span class="status-pill status-${a.status}">
                                    <c:choose>
                                        <c:when test="${a.status=='REQUESTED'}">Chờ xác nhận</c:when>
                                        <c:when test="${a.status=='BOOKED'}">Đã đặt</c:when>
                                        <c:when test="${a.status=='CONFIRMED'}">Đã xác nhận</c:when>
                                        <c:when test="${a.status=='CHECKED_IN'}">Đã đến khám</c:when>
                                        <c:when test="${a.status=='COMPLETED'}">Đã hoàn thành</c:when>
                                        <c:when test="${a.status=='NO_SHOW'}">Vắng hẹn</c:when>
                                        <c:when test="${a.status=='CANCELLED'}">Đã hủy</c:when>
                                        <c:otherwise>Chưa xác định</c:otherwise>
                                    </c:choose>
                                </span>
                            </td>
                            <td>
                                <c:choose>
                                    <c:when test="${a.status=='REQUESTED'}">
                                        <details class="row-disclosure">
                                            <summary class="btn btn-primary btn-sm">Phân công lịch</summary>
                                            <div class="row-disclosure-panel">
                                                <form method="post" action="${pageContext.request.contextPath}/ClinicWorkflow" class="request-assignment-form" data-appointment-form data-gated-submit data-diabetes-type="${a.diabetes_type}">
                                                    <input type="hidden" name="action" value="assignAppointmentRequest">
                                                    <input type="hidden" name="appointmentId" value="${a.appointment_id}">
                                                    <input type="hidden" name="appointmentDate" value="${a.preferred_date}">
                                                    <select class="form-control" name="doctorId" required>
                                                        <option value="">Chọn bác sĩ</option>
                                                        <c:forEach var="d" items="${doctors}"><option value="${d.doctorId}" data-diabetes-focus="${d.diabetesFocus}"><c:out value="${d.fullName}"/> — <c:out value="${d.diabetesFocusLabel}"/></option></c:forEach>
                                                    </select>
                                                    <div class="requested-appointment-date"><small>Ngày bệnh nhân chọn</small><strong>${a.preferred_date} · ${a.preferred_period=='MORNING'?'Buổi sáng':'Buổi chiều'}</strong></div>
                                                    <select class="form-control" name="appointmentTime" required>
                                                        <option value="">Chọn giờ khám</option>
                                                        <c:forEach var="slot" items="${appointmentTimeSlots}"><c:if test="${slot.period==a.preferred_period}"><option value="${slot.value}">${slot.label}</option></c:if></c:forEach>
                                                    </select>
                                                    <button class="btn btn-primary btn-sm" type="submit">Xác nhận lịch</button>
                                                </form>
                                                <form method="post" action="${pageContext.request.contextPath}/ClinicWorkflow" class="danger-form" onsubmit="return confirm('Hủy yêu cầu đặt lịch này?')">
                                                    <input type="hidden" name="action" value="appointmentStatus">
                                                    <input type="hidden" name="appointmentId" value="${a.appointment_id}">
                                                    <button type="submit" class="btn btn-danger btn-sm" name="status" value="CANCELLED">Hủy yêu cầu</button>
                                                </form>
                                            </div>
                                        </details>
                                    </c:when>
                                    <c:when test="${a.status=='BOOKED'||a.status=='CONFIRMED'}">
                                        <div class="table-actions">
                                            <c:choose>
                                                <c:when test="${fn:substring(a.appointment_at,0,10)==appointmentToday}">
                                                    <form method="post" action="${pageContext.request.contextPath}/ClinicWorkflow" class="inline-form">
                                                        <input type="hidden" name="action" value="checkIn">
                                                        <input type="hidden" name="appointmentId" value="${a.appointment_id}">
                                                        <button type="submit" class="btn btn-success btn-sm">Ghi nhận đến khám</button>
                                                    </form>
                                                </c:when>
                                                <c:otherwise><span class="text-muted">Tiếp nhận đúng ngày hẹn</span></c:otherwise>
                                            </c:choose>
                                            <details class="row-disclosure align-right">
                                                <summary class="btn btn-light btn-sm">Tùy chọn</summary>
                                                <div class="row-disclosure-panel">
                                                    <form method="post" action="${pageContext.request.contextPath}/ClinicWorkflow" class="compact-form" data-appointment-form>
                                                        <input type="hidden" name="action" value="rescheduleAppointment">
                                                        <input type="hidden" name="appointmentId" value="${a.appointment_id}">
                                                        <label>Ngày khám mới</label>
                                                        <select class="form-control" name="appointmentDate" required>
                                                            <option value="">Chọn ngày khám</option>
                                                            <c:forEach var="date" items="${appointmentDates}"><option value="${date.value}" ${date.value==fn:substring(a.appointment_at,0,10)?'selected':''}><c:out value="${date.label}"/></option></c:forEach>
                                                        </select>
                                                        <label>Khung giờ mới</label>
                                                        <select class="form-control" name="appointmentTime" required>
                                                            <option value="">Chọn khung giờ</option>
                                                            <c:forEach var="slot" items="${appointmentTimeSlots}"><option value="${slot.value}" ${slot.value==fn:substring(a.appointment_at,11,16)?'selected':''}>${slot.label} · ${slot.period=='MORNING'?'Buổi sáng':'Buổi chiều'}</option></c:forEach>
                                                        </select>
                                                        <input class="form-control" name="note" maxlength="500" placeholder="Lý do đổi lịch">
                                                        <button class="btn btn-primary btn-sm" type="submit">Lưu lịch mới</button>
                                                    </form>
                                                    <form method="post" action="${pageContext.request.contextPath}/ClinicWorkflow" class="danger-zone" onsubmit="return confirm('Xác nhận cập nhật trạng thái lịch?')">
                                                        <input type="hidden" name="action" value="appointmentStatus">
                                                        <input type="hidden" name="appointmentId" value="${a.appointment_id}">
                                                        <button type="submit" class="btn btn-warning btn-sm" name="status" value="NO_SHOW">Vắng hẹn</button>
                                                        <button type="submit" class="btn btn-danger btn-sm" name="status" value="CANCELLED">Hủy lịch</button>
                                                    </form>
                                                </div>
                                            </details>
                                        </div>
                                    </c:when>
                                    <c:otherwise><span class="text-muted">Không còn thao tác</span></c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty appointments}"><tr><td colspan="6" class="empty-state">Chưa có lịch hẹn.</td></tr></c:if>
                    <tr data-filter-empty="appointmentTable" hidden><td colspan="6" class="empty-filter">Không có lịch phù hợp với nội dung tìm kiếm.</td></tr>
                    </tbody>
                </table>
            </div>
        </section>
    </c:if>

    <c:if test="${view=='encounters'}">
        <section class="card">
            <div class="section-header">
                <div><h2>Lượt khám đang xử lý</h2><p>Danh sách chỉ hiển thị lượt đang hoạt động; số thứ tự là thứ tự phục vụ trong ngày.</p></div>
                <span class="data-count">${fn:length(encounters)} lượt đang chờ</span>
            </div>
            <div class="operations-toolbar">
                <label class="table-filter"><span class="sr-only">Tìm lượt khám</span><input type="search" data-table-filter="encounterTable" placeholder="Tìm bệnh nhân, bác sĩ hoặc trạng thái"></label>
                <c:if test="${sessionScope.user.role=='STAFF'}"><span class="data-count">Ưu tiên: Chờ tiếp nhận → Chờ khám</span></c:if>
            </div>
            <div class="table-scroll">
                <table id="encounterTable">
                    <thead><tr><th>STT</th><th>Bệnh nhân</th><th>Bác sĩ</th><th>Trạng thái</th><th>Việc cần làm</th></tr></thead>
                    <tbody>
                    <c:forEach var="e" items="${encounters}">
                        <tr data-search-row>
                            <td><span class="queue-number">${e.queue_number}</span></td>
                            <td><strong><c:out value="${e.patient_name}"/></strong><small class="table-sub">${e.diabetes_type=='TYPE_1'?'Típ 1':e.diabetes_type=='TYPE_2'?'Típ 2':'Chưa phân loại'} · <c:out value="${e.patient_phone}"/></small></td>
                            <td><c:out value="${e.doctor_name}"/></td>
                            <td><span class="status-pill status-${e.status}">${e.status=='WAITING_TRIAGE'?'Chờ tiếp nhận':e.status=='WAITING_DOCTOR'?'Chờ khám':e.status=='IN_CONSULTATION'?'Đang khám':e.status=='WAITING_LAB'?'Chờ xét nghiệm':e.status=='LAB_COMPLETED'?'Chờ kết luận':'Chưa xác định'}</span></td>
                            <td><div class="table-actions">
                                <c:choose>
                                    <c:when test="${empty e.record_id}"><c:if test="${sessionScope.user.role=='STAFF'}"><a class="btn btn-primary btn-sm" href="${pageContext.request.contextPath}/MedicalRecordForm?patientId=${e.patient_id}&encounterId=${e.encounter_id}">Nhập thông tin & sinh hiệu</a></c:if></c:when>
                                    <c:otherwise><a class="btn btn-light btn-sm" href="${pageContext.request.contextPath}/MedicalRecordForm?recordId=${e.record_id}&tab=4">${sessionScope.user.role=='ADMIN'?'Xem bệnh án':'Mở bệnh án'}</a></c:otherwise>
                                </c:choose>
                                <c:if test="${sessionScope.user.role=='DOCTOR' && (e.status=='WAITING_DOCTOR'||e.status=='LAB_COMPLETED')}">
                                    <form method="post" action="${pageContext.request.contextPath}/ClinicWorkflow" class="inline-form">
                                        <input type="hidden" name="action" value="status"><input type="hidden" name="encounterId" value="${e.encounter_id}"><input type="hidden" name="status" value="IN_CONSULTATION">
                                        <button class="btn btn-primary btn-sm" type="submit">${e.status=='LAB_COMPLETED'?'Xem kết quả & kết luận':'Bắt đầu khám'}</button>
                                    </form>
                                </c:if>
                            </div></td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty encounters}"><tr><td colspan="5" class="empty-state success-empty"><strong>Không có lượt khám đang chờ</strong><span>Các công việc hiện tại đã được xử lý.</span></td></tr></c:if>
                    <tr data-filter-empty="encounterTable" hidden><td colspan="5" class="empty-filter">Không có lượt khám phù hợp.</td></tr>
                    </tbody>
                </table>
            </div>
        </section>
    </c:if>

    <c:if test="${view=='clinical'}">
        <section class="card">
            <div class="section-header"><div><h2>Mở hồ sơ lâm sàng bệnh nhân</h2><p>Tra cứu dị ứng và tiền sử trước khi đưa ra chỉ định điều trị.</p></div></div>
            <form method="get" class="search-bar" data-gated-submit>
                <input type="hidden" name="view" value="clinical">
                <select class="form-control" name="patientId" required>
                    <option value="">Chọn bệnh nhân</option>
                    <c:forEach var="p" items="${patients}"><option value="${p.patientId}" ${selectedPatient.patientId==p.patientId?'selected':''}><c:out value="${p.fullName}"/> — <c:out value="${p.phone}"/></option></c:forEach>
                </select>
                <button class="btn btn-primary" type="submit">Mở hồ sơ</button>
            </form>
        </section>
        <c:if test="${not empty selectedPatient}">
            <div class="patient-summary-bar"><strong><c:out value="${selectedPatient.fullName}"/></strong><div class="patient-summary-meta"><span>SĐT: <c:out value="${selectedPatient.phone}"/></span><span>BHYT: <c:out value="${selectedPatient.healthInsuranceNo}"/></span></div></div>
            <div class="two-column">
                <section class="card">
                    <div class="section-header"><div><h2>Dị ứng</h2><p>Thông tin trùng tác nhân sẽ được cập nhật thay vì tạo mới.</p></div></div>
                    <c:forEach var="a" items="${allergies}"><div class="clinical-item"><strong><c:out value="${a.allergen}"/></strong><span>${a.severity=='MILD'?'Nhẹ':a.severity=='MODERATE'?'Trung bình':a.severity=='SEVERE'?'Nặng':'Chưa xác định'}</span><p><c:out value="${a.reaction}"/></p></div></c:forEach>
                    <c:if test="${empty allergies}"><div class="empty-filter">Chưa ghi nhận dị ứng.</div></c:if>
                    <details class="inline-disclosure"><summary class="btn btn-light">Thêm hoặc cập nhật dị ứng</summary>
                        <form method="post" action="${pageContext.request.contextPath}/ClinicWorkflow" class="compact-form disclosure-content">
                            <input type="hidden" name="action" value="allergy"><input type="hidden" name="patientId" value="${selectedPatient.patientId}">
                            <input class="form-control" name="allergen" placeholder="Tác nhân dị ứng" required><input class="form-control" name="reaction" placeholder="Phản ứng">
                            <select class="form-control" name="severity"><option value="UNKNOWN">Chưa xác định</option><option value="MILD">Nhẹ</option><option value="MODERATE">Trung bình</option><option value="SEVERE">Nặng</option></select>
                            <button class="btn btn-primary" type="submit">Lưu dị ứng</button>
                        </form>
                    </details>
                </section>
                <section class="card">
                    <div class="section-header"><div><h2>Tiền sử bệnh</h2><p>Ghi nhận tiền sử cá nhân, gia đình, phẫu thuật và lối sống.</p></div></div>
                    <c:forEach var="h" items="${histories}"><div class="clinical-item"><strong><c:out value="${h.condition_name}"/></strong><span>${h.history_type=='PERSONAL'?'Cá nhân':h.history_type=='FAMILY'?'Gia đình':h.history_type=='SURGICAL'?'Phẫu thuật':'Lối sống'}</span><p><c:out value="${h.note}"/></p></div></c:forEach>
                    <c:if test="${empty histories}"><div class="empty-filter">Chưa ghi nhận tiền sử.</div></c:if>
                    <details class="inline-disclosure"><summary class="btn btn-light">Thêm tiền sử</summary>
                        <form method="post" action="${pageContext.request.contextPath}/ClinicWorkflow" class="compact-form disclosure-content">
                            <input type="hidden" name="action" value="history"><input type="hidden" name="patientId" value="${selectedPatient.patientId}">
                            <select class="form-control" name="historyType"><option value="PERSONAL">Cá nhân</option><option value="FAMILY">Gia đình</option><option value="SURGICAL">Phẫu thuật</option><option value="LIFESTYLE">Lối sống</option></select>
                            <input class="form-control" name="conditionName" placeholder="Tên bệnh hoặc tình trạng" required><input class="form-control" type="date" name="diagnosedDate">
                            <select class="form-control" name="historyStatus"><option value="ACTIVE">Đang theo dõi</option><option value="RESOLVED">Đã ổn định</option></select>
                            <textarea class="form-control" name="historyNote" placeholder="Ghi chú"></textarea><button class="btn btn-primary" type="submit">Lưu tiền sử</button>
                        </form>
                    </details>
                </section>
            </div>
        </c:if>
    </c:if>

    <c:if test="${view=='labs'}">
        <c:if test="${sessionScope.user.role=='DOCTOR'}">
            <section class="card">
                <div class="section-header"><div><h2>Tạo chỉ định xét nghiệm</h2><p>Chỉ định được gắn với đúng lượt khám đang phụ trách.</p></div></div>
                <form method="post" action="${pageContext.request.contextPath}/ClinicWorkflow" class="form-grid" data-gated-submit>
                    <input type="hidden" name="action" value="labOrder">
                    <div class="form-group"><label class="required">Lượt khám</label><select class="form-control" name="encounterId" required><option value="">Chọn lượt đang khám</option><c:forEach var="e" items="${encounters}"><c:if test="${e.status=='IN_CONSULTATION'||e.status=='WAITING_LAB'}"><option value="${e.encounter_id}">#${e.queue_number} — <c:out value="${e.patient_name}"/> — ${e.diabetes_type=='TYPE_1'?'Típ 1':e.diabetes_type=='TYPE_2'?'Típ 2':'Chưa phân loại'}</option></c:if></c:forEach></select></div>
                    <div class="form-group"><label class="required">Xét nghiệm</label><select class="form-control" name="testCode" required><option value="GLU">Đường huyết</option><option value="HBA1C">Đường huyết trung bình HbA1c</option><option value="KETONE">Ketone máu/nước tiểu (ưu tiên típ 1 khi có nguy cơ)</option><option value="LIPID">Bộ xét nghiệm mỡ máu (ưu tiên típ 2)</option><option value="CRE">Creatinin đánh giá chức năng thận</option><option value="UACR">Tỷ lệ albumin và creatinin niệu</option></select></div>
                    <input type="hidden" name="testName" value="catalog">
                    <div class="form-group"><label>Ưu tiên</label><select class="form-control" name="priority"><option value="ROUTINE">Thông thường</option><option value="URGENT">Khẩn</option></select></div>
                    <div class="form-group"><label>Ghi chú lâm sàng</label><input class="form-control" name="clinicalNote" maxlength="500"></div>
                    <div class="form-actions"><button class="btn btn-primary" type="submit">Tạo chỉ định</button></div>
                </form>
            </section>
        </c:if>

        <section class="card">
            <div class="section-header"><div><h2>Chỉ định và kết quả xét nghiệm</h2><p>${sessionScope.user.role=='DOCTOR'?'Theo dõi kết quả để tiếp tục kết luận.':'Ưu tiên chỉ định chưa có kết quả và mức khẩn.'}</p></div><span class="data-count">${fn:length(labOrders)} chỉ định</span></div>
            <div class="operations-toolbar"><label class="table-filter"><span class="sr-only">Tìm xét nghiệm</span><input type="search" data-table-filter="labTable" placeholder="Tìm bệnh nhân, mã xét nghiệm, ưu tiên hoặc trạng thái"></label></div>
            <c:if test="${sessionScope.user.role=='STAFF'||sessionScope.user.role=='ADMIN'}">
                <div class="lab-import-panel">
                    <form method="post" enctype="multipart/form-data"
                          action="${pageContext.request.contextPath}/LabResultImport"
                          class="lab-import-form" data-lab-import-form>
                        <label class="lab-import-field" for="labImportRecord">
                            <span>Bệnh nhân / bệnh án</span>
                            <select class="form-control" id="labImportRecord" name="recordId" required data-lab-import-record>
                                <option value="">Chọn bệnh án đang chờ kết quả</option>
                                <c:forEach var="record" items="${labImportRecords}">
                                    <option value="${record.record_id}"><c:out value="${record.patient_name}"/> — Bệnh án #${record.record_id} — <c:out value="${record.pending_tests}"/></option>
                                </c:forEach>
                            </select>
                        </label>
                        <label class="lab-import-field" for="labImportFile">
                            <span>File kết quả</span>
                            <input class="form-control" id="labImportFile" type="file" name="resultFile"
                                   accept=".txt,.csv,text/plain,text/csv" required data-lab-import-file>
                        </label>
                        <a class="btn btn-light" href="${pageContext.request.contextPath}/static/templates/lab-results-import.txt" download="lab-results-happy-case.txt">File mẫu</a>
                        <button class="btn btn-primary" type="submit" disabled data-lab-import-submit>Import kết quả</button>
                    </form>
                </div>
            </c:if>
            <div class="table-scroll">
                <table id="labTable">
                    <thead><tr><th>Bệnh nhân</th><th>Xét nghiệm</th><th>Ưu tiên</th><th>Kết quả</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
                    <tbody>
                    <c:forEach var="l" items="${labOrders}">
                        <tr data-search-row>
                            <td><strong><c:out value="${l.patient_name}"/></strong><small class="table-sub">${l.diabetes_type=='TYPE_1'?'Típ 1':l.diabetes_type=='TYPE_2'?'Típ 2':'Chưa phân loại'}<c:if test="${not empty l.record_id}"> · Bệnh án #${l.record_id}</c:if></small></td>
                            <td><strong><c:out value="${l.test_code}"/></strong><small class="table-sub"><c:out value="${l.test_name}"/></small></td>
                            <td><span class="status-pill ${l.priority=='URGENT'?'status-CRITICAL':''}">${l.priority=='URGENT'?'Khẩn':'Thông thường'}</span></td>
                            <td><c:choose><c:when test="${not empty l.result_value}"><strong><c:out value="${l.result_value}"/> <c:out value="${l.result_unit}"/></strong><small class="table-sub"><c:out value="${l.reference_range}"/> · <c:out value="${l.result_flag}"/></small></c:when><c:otherwise><span class="text-muted">Chưa có kết quả</span></c:otherwise></c:choose></td>
                            <td><span class="status-pill status-${l.status}">${l.status=='ORDERED'?'Chờ kết quả':l.status=='RESULTED'?'Đã có kết quả':l.status=='REVIEWED'?'Đã xem xét':l.status=='CANCELLED'?'Đã hủy':'Đang xử lý'}</span></td>
                            <td>
                                <c:if test="${sessionScope.user.role=='DOCTOR' && not empty l.record_id}">
                                    <a class="btn btn-light btn-sm" href="${pageContext.request.contextPath}/MedicalRecordForm?recordId=${l.record_id}&tab=3">Mở phiếu xét nghiệm</a>
                                </c:if>
                                <c:if test="${sessionScope.user.role=='STAFF' && not empty l.record_id && (l.test_code=='GLU'||l.test_code=='GLU_FASTING'||l.test_code=='HBA1C'||l.test_code=='LIPID') && (l.status=='ORDERED'||l.status=='COLLECTED')}">
                                    <a class="btn btn-primary btn-sm" href="${pageContext.request.contextPath}/MedicalRecordForm?recordId=${l.record_id}&tab=3">Nhập theo phiếu xét nghiệm</a>
                                </c:if>
                                <c:if test="${(sessionScope.user.role=='STAFF'||sessionScope.user.role=='ADMIN') && (empty l.record_id || !(l.test_code=='GLU'||l.test_code=='GLU_FASTING'||l.test_code=='HBA1C'||l.test_code=='LIPID')) && (l.status=='ORDERED'||l.status=='COLLECTED')}">
                                    <button class="btn btn-primary btn-sm" type="button"
                                            onclick="document.getElementById('lab-result-${l.lab_order_id}').showModal()">Nhập kết quả</button>
                                    <dialog class="lab-result-dialog" id="lab-result-${l.lab_order_id}"
                                            aria-labelledby="lab-result-title-${l.lab_order_id}">
                                        <div class="lab-result-dialog-header">
                                            <div>
                                                <span>NHẬP KẾT QUẢ XÉT NGHIỆM</span>
                                                <strong id="lab-result-title-${l.lab_order_id}"><c:out value="${l.test_code}"/> · <c:out value="${l.patient_name}"/></strong>
                                                <small><c:out value="${l.test_name}"/></small>
                                            </div>
                                            <button class="dialog-close" type="button"
                                                    onclick="this.closest('dialog').close()" aria-label="Đóng form">×</button>
                                        </div>
                                        <form method="post" action="${pageContext.request.contextPath}/ClinicWorkflow" class="result-form lab-result-form">
                                            <input type="hidden" name="action" value="labResult">
                                            <input type="hidden" name="labOrderId" value="${l.lab_order_id}">
                                            <div class="form-group">
                                                <label class="required">Kết quả</label>
                                                <input class="form-control" name="resultValue" maxlength="100"
                                                       placeholder="Ví dụ: 7.2" required autofocus>
                                            </div>
                                            <div class="form-group">
                                                <label>Đơn vị (Unit)</label>
                                                <input class="form-control" name="resultUnit" maxlength="30"
                                                       placeholder="Ví dụ: mmol/L, mg/dL hoặc %">
                                            </div>
                                            <div class="form-group">
                                                <label>Khoảng tham chiếu</label>
                                                <input class="form-control" name="referenceRange" maxlength="100"
                                                       placeholder="Ví dụ: 3.9–6.1 mmol/L">
                                            </div>
                                            <div class="form-group">
                                                <label>Đánh giá kết quả</label>
                                                <select class="form-control" name="resultFlag">
                                                    <option value="NORMAL">Bình thường</option>
                                                    <option value="LOW">Thấp</option>
                                                    <option value="HIGH">Cao</option>
                                                    <option value="CRITICAL">Nguy cấp</option>
                                                </select>
                                            </div>
                                            <div class="lab-result-note">Giữ nguyên các đơn vị chuẩn như <strong>mmol/L</strong>, <strong>mg/dL</strong>, <strong>%</strong>. Kiểm tra lại kết quả trước khi lưu.</div>
                                            <div class="lab-result-actions">
                                                <button class="btn btn-light" type="button" onclick="this.closest('dialog').close()">Hủy</button>
                                                <button class="btn btn-success" type="submit">Lưu kết quả</button>
                                            </div>
                                        </form>
                                    </dialog>
                                </c:if>
                            </td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty labOrders}"><tr><td colspan="6" class="empty-state">Chưa có chỉ định xét nghiệm.</td></tr></c:if>
                    <tr data-filter-empty="labTable" hidden><td colspan="6" class="empty-filter">Không có xét nghiệm phù hợp.</td></tr>
                    </tbody>
                </table>
            </div>
        </section>
    </c:if>
</main>

<jsp:include page="footer.jsp"/>
<script src="${pageContext.request.contextPath}/static/js/diabetes-routing.js?v=20260721-web-audit1" defer></script>
<script src="${pageContext.request.contextPath}/static/js/lab-result-import.js?v=20260722-lab-compact1" defer></script>
</body>
</html>
