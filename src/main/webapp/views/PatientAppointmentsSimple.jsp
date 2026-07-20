<%@page contentType="text/html" pageEncoding="UTF-8"%><%@taglib prefix="c" uri="jakarta.tags.core"%><%@taglib prefix="fmt" uri="jakarta.tags.fmt"%><%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Lịch khám — DiaCare</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css?v=20260720-ui3">
</head>
<body>
<jsp:include page="header.jsp"/>
<jsp:include page="topnav.jsp"/>
<main class="page-wrapper">
    <div class="page-heading">
        <div>
            <div class="eyebrow">DÀNH CHO BỆNH NHÂN</div>
            <h1 class="page-title">Lịch khám</h1>
            <p class="text-muted">Kiểm tra lịch hiện có trước, sau đó chọn ngày và buổi mong muốn. Phòng khám sẽ xác nhận giờ chính xác.</p>
        </div>
        <a class="btn btn-light" href="${pageContext.request.contextPath}/PatientHistory">Xem hồ sơ khám</a>
    </div>

    <div class="workflow-guide patient-booking-guide"><div class="workflow-step active"><span>1</span><strong>Gửi yêu cầu</strong><small>Chọn ngày, buổi và lý do</small></div><div class="workflow-step"><span>2</span><strong>Phòng khám xác nhận</strong><small>Sắp xếp bác sĩ và giờ cụ thể</small></div><div class="workflow-step"><span>3</span><strong>Đến khám đúng giờ</strong><small>Mang theo giấy tờ và đơn thuốc cũ</small></div></div>

    <c:if test="${not empty sessionScope.appointmentFlash}">
        <div class="alert alert-info"><c:out value="${sessionScope.appointmentFlash}"/></div>
        <c:remove var="appointmentFlash" scope="session"/>
    </c:if>

    <div class="appointment-layout booking-simple">
        <section class="card booking-card">
            <div class="card-title">Gửi yêu cầu khám</div>
            <form method="post" action="${pageContext.request.contextPath}/PatientAppointments" class="compact-form">
                <div class="form-group">
                    <label class="required" for="preferredDate">1. Chọn ngày muốn đến</label>
                    <select id="preferredDate" class="form-control" name="preferredDate" required>
                        <option value="">Chọn ngày khám</option>
                        <c:forEach var="date" items="${appointmentDates}"><option value="${date.value}"><c:out value="${date.label}"/></option></c:forEach>
                    </select>
                    <small class="form-hint">Từ thứ Hai đến thứ Bảy, trong 90 ngày tới.</small>
                </div>

                <fieldset class="booking-fieldset">
                    <legend>2. Chọn buổi thuận tiện</legend>
                    <div class="time-slot-grid period-choice-grid">
                        <label class="choice-tile time-slot">
                            <input type="radio" name="preferredPeriod" value="MORNING" required>
                            <span>Buổi sáng<br><small>07:30–11:30</small></span>
                        </label>
                        <label class="choice-tile time-slot">
                            <input type="radio" name="preferredPeriod" value="AFTERNOON" required>
                            <span>Buổi chiều<br><small>13:00–17:00</small></span>
                        </label>
                    </div>
                </fieldset>

                <fieldset class="booking-fieldset">
                    <legend>3. Chọn lý do khám</legend>
                    <div class="reason-choice-grid">
                        <label class="choice-tile"><input type="radio" name="reason" value="Tái khám tiểu đường định kỳ" required><span>Tái khám định kỳ</span></label>
                        <label class="choice-tile"><input type="radio" name="reason" value="Kiểm tra đường huyết và HbA1c" required><span>Kiểm tra chỉ số</span></label>
                        <label class="choice-tile"><input type="radio" name="reason" value="Tư vấn thuốc hoặc insulin" required><span>Tư vấn thuốc</span></label>
                        <label class="choice-tile"><input type="radio" name="reason" value="Có triệu chứng bất thường" required><span>Có triệu chứng</span></label>
                        <label class="choice-tile"><input type="radio" name="reason" value="Khám lần đầu" required><span>Khám lần đầu</span></label>
                    </div>
                </fieldset>

                <button class="btn btn-primary btn-lg booking-submit" type="submit">Gửi yêu cầu</button>
                <p class="form-hint">Bạn chưa cần chọn bác sĩ hoặc giờ. Nhân viên phòng khám sẽ sắp xếp và xác nhận sau.</p>
            </form>
        </section>

        <section class="card appointment-list-card">
            <div class="card-title"><span>Lịch của tôi</span><span class="data-count">${fn:length(appointments)} lịch</span></div>
            <div class="appointment-list">
                <c:forEach var="a" items="${appointments}">
                    <article class="appointment-item">
                        <div class="appointment-date">
                            <strong>
                                <c:choose>
                                    <c:when test="${a.status=='REQUESTED'}">
                                        <fmt:formatDate value="${a.preferred_date}" pattern="dd/MM/yyyy"/>
                                        · ${a.preferred_period=='MORNING'?'Buổi sáng':'Buổi chiều'}
                                    </c:when>
                                    <c:otherwise><fmt:formatDate value="${a.appointment_at}" pattern="dd/MM/yyyy · HH:mm"/></c:otherwise>
                                </c:choose>
                            </strong>
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
                        </div>
                        <c:choose>
                            <c:when test="${a.status=='REQUESTED'}"><h3>Phòng khám đang sắp xếp bác sĩ</h3></c:when>
                            <c:otherwise><h3>BS. <c:out value="${a.doctor_name}"/></h3></c:otherwise>
                        </c:choose>
                        <p><c:out value="${a.reason}"/></p>
                        <c:if test="${a.status=='REQUESTED'||a.status=='BOOKED'||a.status=='CONFIRMED'}">
                            <form method="post" action="${pageContext.request.contextPath}/PatientAppointments" onsubmit="return confirm('Bạn chắc chắn không thể đến lịch này?')">
                                <input type="hidden" name="action" value="cancel">
                                <input type="hidden" name="appointmentId" value="${a.appointment_id}">
                                <button class="btn btn-danger btn-sm" type="submit">Hủy yêu cầu / lịch</button>
                            </form>
                        </c:if>
                    </article>
                </c:forEach>
                <c:if test="${empty appointments}"><div class="empty-state">Bạn chưa có lịch khám nào.</div></c:if>
            </div>
        </section>
    </div>
</main>
<jsp:include page="footer.jsp"/>
</body>
</html>
