<%@page contentType="text/html" pageEncoding="UTF-8"%><%@taglib prefix="c" uri="jakarta.tags.core"%><%@taglib prefix="fmt" uri="jakarta.tags.fmt"%><%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Lịch khám — DiaCare</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css?v=20260721-web-audit1">
</head>
<body>
<jsp:include page="header.jsp"/>
<jsp:include page="topnav.jsp"/>
<main class="page-wrapper">
    <div class="page-heading">
        <div>
            <div class="eyebrow">DÀNH CHO BỆNH NHÂN</div>
            <h1 class="page-title">Lịch khám</h1>
        </div>
    </div>

    <c:if test="${not empty sessionScope.appointmentFlash}">
        <div class="alert alert-info"><c:out value="${sessionScope.appointmentFlash}"/></div>
        <c:remove var="appointmentFlash" scope="session"/>
    </c:if>

    <div class="appointment-layout booking-simple">
        <section class="card booking-card">
            <div class="booking-card-heading">
                <div><span class="panel-eyebrow">ĐẶT LỊCH NHANH</span><h2>Gửi yêu cầu khám</h2><p>Chọn ngày và buổi phù hợp. Nhân viên sẽ xác nhận bác sĩ và giờ cụ thể.</p></div>
                <span class="booking-step-count">3 bước</span>
            </div>
            <form method="post" action="${pageContext.request.contextPath}/PatientAppointments" class="compact-form">
                <div class="booking-step">
                    <div class="booking-step-title"><span>01</span><div><strong>Ngày dự kiến</strong><small>Chọn ngày muốn đến khám</small></div></div>
                    <div class="form-group">
                        <label class="sr-only" for="preferredDate">Ngày khám</label>
                        <input id="preferredDate" class="form-control booking-date-input" type="date" name="preferredDate"
                               min="${minAppointmentDate}" max="${maxAppointmentDate}" required>
                        <small class="form-hint">Từ ngày mai, không nhận Chủ nhật và không quá 90 ngày.</small>
                    </div>
                </div>

                <fieldset class="booking-fieldset">
                    <legend><span class="booking-index">02</span><span>Buổi thuận tiện</span></legend>
                    <div class="time-slot-grid period-choice-grid">
                        <label class="choice-tile time-slot">
                            <input type="radio" name="preferredPeriod" value="MORNING" required>
                            <span><strong>Buổi sáng</strong><small>07:30 – 11:30</small></span>
                        </label>
                        <label class="choice-tile time-slot">
                            <input type="radio" name="preferredPeriod" value="AFTERNOON" required>
                            <span><strong>Buổi chiều</strong><small>13:00 – 17:00</small></span>
                        </label>
                    </div>
                    <p class="booking-helper">Bạn chưa cần chọn giờ cụ thể. Nhân viên sẽ xếp khung 30 phút phù hợp sau khi tiếp nhận.</p>
                </fieldset>

                <fieldset class="booking-fieldset">
                    <legend><span class="booking-index">03</span><span>Lý do khám</span></legend>
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
                            <c:otherwise>
                                <h3><c:if test="${not fn:startsWith(a.doctor_name, 'BS.')}">BS. </c:if><c:out value="${a.doctor_name}"/></h3>
                            </c:otherwise>
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
