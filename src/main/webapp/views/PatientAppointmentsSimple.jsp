<%@page contentType="text/html" pageEncoding="UTF-8"%><%@taglib prefix="c" uri="jakarta.tags.core"%><%@taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Lịch khám — DiaCare</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css?v=20260719-appt1">
</head>
<body>
<jsp:include page="header.jsp"/>
<jsp:include page="topnav.jsp"/>
<main class="page-wrapper">
    <div class="page-heading">
        <div>
            <div class="eyebrow">DÀNH CHO BỆNH NHÂN</div>
            <h1 class="page-title">Lịch khám</h1>
            <p class="text-muted">Chọn ngày, buổi và lý do. Nhân viên sẽ xác nhận giờ khám cụ thể cho bạn.</p>
        </div>
    </div>

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
                    <input id="preferredDate" class="form-control" type="date" name="preferredDate" required>
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

        <section class="card">
            <div class="card-title">Lịch của tôi</div>
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
                                    <c:otherwise><c:out value="${a.status}"/></c:otherwise>
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
                                <button class="btn btn-outline btn-sm" type="submit">Tôi không thể đến</button>
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
<script src="${pageContext.request.contextPath}/static/js/main.js?v=20260719-ai1"></script>
<script>
(function () {
    const input = document.getElementById('preferredDate');
    if (!input) return;
    const format = date => {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return year + '-' + month + '-' + day;
    };
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const latest = new Date();
    latest.setDate(latest.getDate() + 90);
    input.min = format(tomorrow);
    input.max = format(latest);
})();
</script>
</body>
</html>
