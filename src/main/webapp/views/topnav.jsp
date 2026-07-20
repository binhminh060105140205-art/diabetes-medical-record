<%@taglib prefix="c" uri="jakarta.tags.core"%><%@taglib prefix="fn" uri="jakarta.tags.functions"%><%@page contentType="text/html" pageEncoding="UTF-8"%>
<c:set var="uri" value="${pageContext.request.requestURI}"/>
<c:choose>
    <c:when test="${sessionScope.user.role=='ADMIN'}"><c:set var="roleLabel" value="Quản trị viên"/></c:when>
    <c:when test="${sessionScope.user.role=='STAFF'}"><c:set var="roleLabel" value="Nhân viên tiếp nhận"/></c:when>
    <c:when test="${sessionScope.user.role=='DOCTOR'}"><c:set var="roleLabel" value="Bác sĩ điều trị"/></c:when>
    <c:otherwise><c:set var="roleLabel" value="Bệnh nhân"/></c:otherwise>
</c:choose>
<aside class="app-sidebar" id="app-sidebar">
    <div class="sidebar-context">
        <span class="sidebar-avatar"><c:out value="${fn:substring(sessionScope.user.fullName,0,1)}" default="D"/></span>
        <div><strong><c:out value="${sessionScope.user.fullName}"/></strong><small>${roleLabel}</small></div>
    </div>
    <div class="sidebar-focus">
        <span>Ưu tiên hôm nay</span>
        <c:choose>
            <c:when test="${sessionScope.user.role=='ADMIN'}"><strong>Kiểm soát vận hành và tài khoản</strong></c:when>
            <c:when test="${sessionScope.user.role=='STAFF'}"><strong>Tiếp nhận đúng thứ tự quy trình</strong></c:when>
            <c:when test="${sessionScope.user.role=='DOCTOR'}"><strong>Xử lý lượt khám đang chờ</strong></c:when>
            <c:otherwise><strong>Ghi chỉ số và theo dõi lịch khám</strong></c:otherwise>
        </c:choose>
    </div>
    <nav class="sidebar-nav"><c:choose>
        <c:when test="${sessionScope.user.role=='ADMIN'}">
            <div class="sidebar-label">TỔNG QUAN</div>
            <a class="${uri.contains('AdminDashboard')?'active':''}" href="${pageContext.request.contextPath}/AdminDashboard"><span>⌂</span><b>Tổng quan</b></a>
            <div class="sidebar-label">VẬN HÀNH PHÒNG KHÁM</div>
            <a class="${uri.contains('ClinicWorkflow') && param.view=='appointments'?'active':''}" href="${pageContext.request.contextPath}/ClinicWorkflow?view=appointments"><span>▦</span><b>Lịch hẹn</b></a>
            <a class="${uri.contains('ClinicWorkflow') && (empty param.view || param.view=='encounters')?'active':''}" href="${pageContext.request.contextPath}/ClinicWorkflow?view=encounters"><span>≡</span><b>Lượt khám & hàng đợi</b></a>
            <a class="${uri.contains('ClinicWorkflow') && param.view=='labs'?'active':''}" href="${pageContext.request.contextPath}/ClinicWorkflow?view=labs"><span>◇</span><b>Kết quả xét nghiệm</b></a>
            <div class="sidebar-label">DỮ LIỆU HỆ THỐNG</div>
            <a class="${uri.contains('PatientList')?'active':''}" href="${pageContext.request.contextPath}/PatientList"><span>♙</span><b>Quản lý bệnh nhân</b></a>
            <a class="${uri.contains('AdminCreateUser')?'active':''}" href="${pageContext.request.contextPath}/AdminCreateUser"><span>＋</span><b>Tạo tài khoản</b></a>
        </c:when>
        <c:when test="${sessionScope.user.role=='STAFF'}">
            <div class="sidebar-label">CÔNG VIỆC HÔM NAY</div>
            <a class="${uri.contains('StaffDashboard')?'active':''}" href="${pageContext.request.contextPath}/StaffDashboard"><span>⌂</span><b>Tổng quan hôm nay</b></a>
            <a class="${uri.contains('ClinicWorkflow') && param.view=='appointments'?'active':''}" href="${pageContext.request.contextPath}/ClinicWorkflow?view=appointments"><span>▦</span><b>Lịch hẹn</b></a>
            <a class="${uri.contains('ClinicWorkflow') && (empty param.view || param.view=='encounters')?'active':''}" href="${pageContext.request.contextPath}/ClinicWorkflow?view=encounters"><span>≡</span><b>Tiếp nhận đến khám</b></a>
            <a class="${uri.contains('ClinicWorkflow') && param.view=='labs'?'active':''}" href="${pageContext.request.contextPath}/ClinicWorkflow?view=labs"><span>◇</span><b>Trả kết quả xét nghiệm</b></a>
            <div class="sidebar-label">BỆNH NHÂN</div>
            <a class="${uri.contains('PatientList')?'active':''}" href="${pageContext.request.contextPath}/PatientList"><span>♙</span><b>Danh sách bệnh nhân</b></a>
            <a class="${uri.contains('PatientForm')?'active':''}" href="${pageContext.request.contextPath}/PatientForm"><span>＋</span><b>Tiếp nhận bệnh nhân</b></a>
        </c:when>
        <c:when test="${sessionScope.user.role=='DOCTOR'}">
            <div class="sidebar-label">CÔNG VIỆC HÔM NAY</div>
            <a class="${uri.contains('DoctorDashboard')?'active':''}" href="${pageContext.request.contextPath}/DoctorDashboard"><span>⌂</span><b>Tổng quan hôm nay</b></a>
            <a class="${uri.contains('ClinicWorkflow') && (empty param.view || param.view=='encounters')?'active':''}" href="${pageContext.request.contextPath}/ClinicWorkflow?view=encounters"><span>▤</span><b>Lượt khám của tôi</b></a>
            <a class="${uri.contains('ClinicWorkflow') && param.view=='labs'?'active':''}" href="${pageContext.request.contextPath}/ClinicWorkflow?view=labs"><span>◇</span><b>Chỉ định xét nghiệm</b></a>
            <div class="sidebar-label">HỒ SƠ LÂM SÀNG</div>
            <a class="${uri.contains('PatientList')?'active':''}" href="${pageContext.request.contextPath}/PatientList"><span>♙</span><b>Hồ sơ bệnh nhân</b></a>
            <a class="${uri.contains('ClinicWorkflow') && param.view=='clinical'?'active':''}" href="${pageContext.request.contextPath}/ClinicWorkflow?view=clinical"><span>◎</span><b>Dị ứng & tiền sử</b></a>
            <a class="${uri.contains('DoctorProfile')?'active':''}" href="${pageContext.request.contextPath}/DoctorProfile"><span>♧</span><b>Hồ sơ hành nghề</b></a>
        </c:when>
        <c:when test="${sessionScope.user.role=='PATIENT'}">
            <div class="sidebar-label">HÔM NAY</div>
            <a class="${uri.contains('PatientDashboard')?'active':''}" href="${pageContext.request.contextPath}/PatientDashboard"><span>⌂</span><b>Sức khỏe hôm nay</b></a>
            <a class="${uri.contains('PatientAppointments')?'active':''}" href="${pageContext.request.contextPath}/PatientAppointments"><span>▦</span><b>Lịch khám</b></a>
            <div class="sidebar-label">THEO DÕI DÀI HẠN</div>
            <a class="${uri.contains('PatientJournal')?'active':''}" href="${pageContext.request.contextPath}/PatientJournal"><span>▤</span><b>Nhật ký sức khỏe</b></a>
            <a class="${uri.contains('DeviceData')?'active':''}" href="${pageContext.request.contextPath}/DeviceData"><span>◉</span><b>Thiết bị & cảnh báo</b></a>
            <a class="${uri.contains('PatientHistory')?'active':''}" href="${pageContext.request.contextPath}/PatientHistory"><span>♙</span><b>Hồ sơ khám</b></a>
        </c:when>
    </c:choose></nav>
    <div class="sidebar-bottom">
        <a class="${uri.contains('EditProfile')?'active':''}" href="${pageContext.request.contextPath}/EditProfile"><span>⚙</span><b>Hồ sơ & cài đặt</b></a>
        <a class="sidebar-main-link" href="${pageContext.request.contextPath}/"><span>⌂</span><b>Trang giới thiệu</b></a>
        <a href="${pageContext.request.contextPath}/Logout"><span>⇥</span><b>Đăng xuất</b></a>
    </div>
</aside>
<button class="sidebar-toggle" type="button" aria-label="Mở menu" aria-controls="app-sidebar" aria-expanded="false">☰</button>
<button class="sidebar-backdrop" type="button" aria-label="Đóng menu"></button>
<script src="${pageContext.request.contextPath}/static/js/main.js?v=20260720-ux1" defer></script>
