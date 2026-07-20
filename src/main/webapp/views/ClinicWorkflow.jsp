<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Điều hành khám — DiaCare</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css?v=20260720-ux1">
</head>
<body>
<jsp:include page="header.jsp"/>
<jsp:include page="topnav.jsp"/>

<main class="page-wrapper workflow-page">
    <div class="page-heading">
        <div>
            <div class="eyebrow">VẬN HÀNH NGOẠI TRÚ</div>
            <h1 class="page-title">Điều hành khám</h1>
            <p class="text-muted">
                <c:choose>
                    <c:when test="${sessionScope.user.role=='DOCTOR'}">Xử lý lượt khám được phân công, chỉ định xét nghiệm và hoàn tất kết luận.</c:when>
                    <c:otherwise>Tiếp nhận theo thứ tự: xác nhận lịch, ghi nhận bệnh nhân đến khám, nhập sinh hiệu và trả kết quả xét nghiệm.</c:otherwise>
                </c:choose>
            </p>
        </div>
        <c:if test="${sessionScope.user.role=='STAFF'}">
            <div class="heading-actions">
                <a class="btn btn-light" href="${pageContext.request.contextPath}/PatientList">Tìm bệnh nhân</a>
                <a class="btn btn-primary" href="${pageContext.request.contextPath}/PatientForm">Tiếp nhận bệnh nhân mới</a>
            </div>
        </c:if>
    </div>

    <c:if test="${not empty workflowFlash}"><div class="alert alert-info"><c:out value="${workflowFlash}"/></div></c:if>

    <div class="workflow-guide" aria-label="Quy trình khám ngoại trú">
        <div class="workflow-step ${view=='appointments'?'active':''}"><span>1</span><strong>Xác nhận lịch</strong><small>Chọn bác sĩ và giờ khám</small></div>
        <div class="workflow-step ${view=='encounters'?'active':''}"><span>2</span><strong>Tiếp nhận đến khám & sinh hiệu</strong><small>Cấp số và tiếp nhận ban đầu</small></div>
        <div class="workflow-step ${view=='labs'||view=='clinical'?'active':''}"><span>3</span><strong>Khám & xét nghiệm</strong><small>Bác sĩ xử lý chuyên môn</small></div>
        <div class="workflow-step"><span>4</span><strong>Kết luận</strong><small>Hoàn tất hồ sơ và đơn thuốc</small></div>
    </div>

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
                <form method="post" action="${pageContext.request.contextPath}/ClinicWorkflow" class="form-grid disclosure-content">
                    <input type="hidden" name="action" value="createAppointment">
                    <div class="form-group">
                        <label class="required">Bệnh nhân</label>
                        <select class="form-control" name="patientId" required>
                            <option value="">Chọn bệnh nhân</option>
                            <c:forEach var="p" items="${patients}"><option value="${p.patientId}"><c:out value="${p.fullName}"/> — <c:out value="${p.phone}"/></option></c:forEach>
                        </select>
                    </div>
                    <div class="form-group">
                        <label class="required">Bác sĩ</label>
                        <select class="form-control" name="doctorId" required>
                            <option value="">Chọn bác sĩ</option>
                            <c:forEach var="d" items="${doctors}"><option value="${d.doctorId}"><c:out value="${d.fullName}"/> — <c:out value="${d.specialty}"/></option></c:forEach>
                        </select>
                    </div>
                    <div class="form-group">
                        <label class="required">Thời gian</label>
                        <input class="form-control appointment-time" type="datetime-local" name="appointmentAt" step="1800" required>
                        <small>Thứ 2–7, 07:30–11:30 và 13:00–17:00.</small>
                    </div>
                    <div class="form-group">
                        <label class="required">Lý do khám</label>
                        <input class="form-control" name="reason" minlength="5" maxlength="255" required>
                    </div>
                    <div class="form-group">
                        <label>Ghi chú</label>
                        <input class="form-control" name="note" maxlength="500">
                    </div>
                    <div class="form-actions"><button class="btn btn-primary" type="submit">Tạo lịch khám</button></div>
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
                                    <c:when test="${a.status=='REQUESTED'}"><strong>${a.preferred_date}</strong><small class="table-sub">${a.preferred_period=='MORNING'?'Buổi sáng':'Buổi chiều'}</small></c:when>
                                    <c:otherwise><strong>${a.appointment_at}</strong></c:otherwise>
                                </c:choose>
                            </td>
                            <td><strong><c:out value="${a.patient_name}"/></strong><small class="table-sub"><c:out value="${a.patient_phone}"/></small></td>
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
                                                <form method="post" action="${pageContext.request.contextPath}/ClinicWorkflow" class="request-assignment-form">
                                                    <input type="hidden" name="action" value="assignAppointmentRequest">
                                                    <input type="hidden" name="appointmentId" value="${a.appointment_id}">
                                                    <select class="form-control" name="doctorId" required>
                                                        <option value="">Chọn bác sĩ</option>
                                                        <c:forEach var="d" items="${doctors}"><option value="${d.doctorId}"><c:out value="${d.fullName}"/></option></c:forEach>
                                                    </select>
                                                    <input class="form-control appointment-time" type="datetime-local" name="appointmentAt" step="1800" value="${a.preferred_date}T${a.preferred_period=='MORNING'?'08:30':'13:30'}" required>
                                                    <button class="btn btn-primary btn-sm" type="submit">Xác nhận lịch</button>
                                                </form>
                                                <form method="post" action="${pageContext.request.contextPath}/ClinicWorkflow" class="danger-form" onsubmit="return confirm('Hủy yêu cầu đặt lịch này?')">
                                                    <input type="hidden" name="action" value="appointmentStatus">
                                                    <input type="hidden" name="appointmentId" value="${a.appointment_id}">
                                                    <button class="btn btn-danger btn-sm" name="status" value="CANCELLED">Hủy yêu cầu</button>
                                                </form>
                                            </div>
                                        </details>
                                    </c:when>
                                    <c:when test="${a.status=='BOOKED'||a.status=='CONFIRMED'}">
                                        <div class="table-actions">
                                            <form method="post" action="${pageContext.request.contextPath}/ClinicWorkflow" class="inline-form">
                                                <input type="hidden" name="action" value="checkIn">
                                                <input type="hidden" name="appointmentId" value="${a.appointment_id}">
                                                <button class="btn btn-success btn-sm" type="submit">Ghi nhận đến khám</button>
                                            </form>
                                            <details class="row-disclosure align-right">
                                                <summary class="btn btn-light btn-sm">Tùy chọn</summary>
                                                <div class="row-disclosure-panel">
                                                    <form method="post" action="${pageContext.request.contextPath}/ClinicWorkflow" class="compact-form">
                                                        <input type="hidden" name="action" value="rescheduleAppointment">
                                                        <input type="hidden" name="appointmentId" value="${a.appointment_id}">
                                                        <label>Đổi ngày giờ</label>
                                                        <input class="form-control appointment-time" type="datetime-local" name="appointmentAt" step="1800" value="${fn:replace(fn:substring(a.appointment_at,0,16),' ','T')}" required>
                                                        <input class="form-control" name="note" maxlength="500" placeholder="Lý do đổi lịch">
                                                        <button class="btn btn-primary btn-sm" type="submit">Lưu lịch mới</button>
                                                    </form>
                                                    <form method="post" action="${pageContext.request.contextPath}/ClinicWorkflow" class="danger-zone" onsubmit="return confirm('Xác nhận cập nhật trạng thái lịch?')">
                                                        <input type="hidden" name="action" value="appointmentStatus">
                                                        <input type="hidden" name="appointmentId" value="${a.appointment_id}">
                                                        <button class="btn btn-warning btn-sm" name="status" value="NO_SHOW">Vắng hẹn</button>
                                                        <button class="btn btn-danger btn-sm" name="status" value="CANCELLED">Hủy lịch</button>
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
                            <td><strong><c:out value="${e.patient_name}"/></strong><small class="table-sub"><c:out value="${e.patient_phone}"/></small></td>
                            <td><c:out value="${e.doctor_name}"/></td>
                            <td><span class="status-pill status-${e.status}">${e.status=='WAITING_TRIAGE'?'Chờ tiếp nhận':e.status=='WAITING_DOCTOR'?'Chờ khám':e.status=='IN_CONSULTATION'?'Đang khám':e.status=='WAITING_LAB'?'Chờ xét nghiệm':e.status=='LAB_COMPLETED'?'Chờ kết luận':'Chưa xác định'}</span></td>
                            <td><div class="table-actions">
                                <c:choose>
                                    <c:when test="${empty e.record_id}"><c:if test="${sessionScope.user.role=='STAFF'}"><a class="btn btn-primary btn-sm" href="${pageContext.request.contextPath}/MedicalRecordForm?patientId=${e.patient_id}&encounterId=${e.encounter_id}">Nhập thông tin & sinh hiệu</a></c:if></c:when>
                                    <c:otherwise><a class="btn btn-light btn-sm" href="${pageContext.request.contextPath}/MedicalRecordForm?recordId=${e.record_id}&tab=4">Mở bệnh án</a></c:otherwise>
                                </c:choose>
                                <c:if test="${sessionScope.user.role=='DOCTOR' && e.status!='IN_CONSULTATION' && e.status!='WAITING_LAB'}">
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
            <form method="get" class="search-bar">
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
                <form method="post" action="${pageContext.request.contextPath}/ClinicWorkflow" class="form-grid">
                    <input type="hidden" name="action" value="labOrder">
                    <div class="form-group"><label class="required">Lượt khám</label><select class="form-control" name="encounterId" required><option value="">Chọn lượt khám đang phụ trách</option><c:forEach var="e" items="${encounters}"><c:if test="${e.status!='COMPLETED'&&e.status!='CANCELLED'}"><option value="${e.encounter_id}">#${e.queue_number} — <c:out value="${e.patient_name}"/></option></c:if></c:forEach></select></div>
                    <div class="form-group"><label class="required">Xét nghiệm</label><select class="form-control" name="testCode" required><option value="GLU">Đường huyết</option><option value="HBA1C">Đường huyết trung bình HbA1c</option><option value="LIPID">Bộ xét nghiệm mỡ máu</option><option value="CRE">Creatinin đánh giá chức năng thận</option><option value="UACR">Tỷ lệ albumin và creatinin niệu</option></select></div>
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
            <div class="table-scroll">
                <table id="labTable">
                    <thead><tr><th>Bệnh nhân</th><th>Xét nghiệm</th><th>Ưu tiên</th><th>Kết quả</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
                    <tbody>
                    <c:forEach var="l" items="${labOrders}">
                        <tr data-search-row>
                            <td><strong><c:out value="${l.patient_name}"/></strong></td>
                            <td><strong><c:out value="${l.test_code}"/></strong><small class="table-sub"><c:out value="${l.test_name}"/></small></td>
                            <td><span class="status-pill ${l.priority=='URGENT'?'status-CRITICAL':''}">${l.priority=='URGENT'?'Khẩn':'Thông thường'}</span></td>
                            <td><c:choose><c:when test="${not empty l.result_value}"><strong><c:out value="${l.result_value}"/> <c:out value="${l.result_unit}"/></strong><small class="table-sub"><c:out value="${l.reference_range}"/> · <c:out value="${l.result_flag}"/></small></c:when><c:otherwise><span class="text-muted">Chưa có kết quả</span></c:otherwise></c:choose></td>
                            <td><span class="status-pill status-${l.status}">${l.status=='ORDERED'?'Chờ kết quả':l.status=='RESULTED'?'Đã có kết quả':l.status=='REVIEWED'?'Đã xem xét':l.status=='CANCELLED'?'Đã hủy':'Đang xử lý'}</span></td>
                            <td>
                                <c:if test="${(sessionScope.user.role=='STAFF'||sessionScope.user.role=='ADMIN')&&l.status!='REVIEWED'&&l.status!='CANCELLED'}">
                                    <details class="row-disclosure align-right">
                                        <summary class="btn btn-primary btn-sm">Nhập kết quả</summary>
                                        <form method="post" action="${pageContext.request.contextPath}/ClinicWorkflow" class="result-form row-disclosure-panel">
                                            <input type="hidden" name="action" value="labResult"><input type="hidden" name="labOrderId" value="${l.lab_order_id}">
                                            <label>Kết quả<input name="resultValue" placeholder="Ví dụ: 7.2" required></label>
                                            <label>Đơn vị<input name="resultUnit" placeholder="mmol/L"></label>
                                            <label>Tham chiếu<input name="referenceRange" placeholder="3.9–6.1"></label>
                                            <label>Đánh giá<select name="resultFlag"><option value="NORMAL">Bình thường</option><option value="LOW">Thấp</option><option value="HIGH">Cao</option><option value="CRITICAL">Nguy cấp</option></select></label>
                                            <button class="btn btn-success btn-sm" type="submit">Lưu kết quả</button>
                                        </form>
                                    </details>
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
</body>
</html>
