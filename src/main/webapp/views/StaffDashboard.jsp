<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Tiếp nhận bệnh nhân — DiaCare</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css?v=20260722-web-audit2">
</head>
<body>
<jsp:include page="header.jsp"/>
<jsp:include page="topnav.jsp"/>

<main class="page-wrapper app-workspace staff-patient-workspace">
    <div class="workspace-heading">
        <div>
            <span class="workspace-kicker">NHÂN VIÊN TIẾP NHẬN</span>
            <h1>Tiếp nhận và quản lý bệnh nhân</h1>
            <p>Tìm hồ sơ trước khi tạo mới; mọi thao tác bệnh nhân được gom tại một màn hình.</p>
        </div>
        <div class="heading-actions">
            <a class="btn ${pendingAppointmentRequests > 0 ? 'btn-warning' : 'btn-light'}"
               href="${pageContext.request.contextPath}/ClinicWorkflow?view=appointments">
                <c:choose>
                    <c:when test="${pendingAppointmentRequests > 0}">${pendingAppointmentRequests} lịch chờ xác nhận</c:when>
                    <c:otherwise>Xem lịch hẹn</c:otherwise>
                </c:choose>
            </a>
            <a class="btn btn-primary" href="#new-patient" data-open-intake>+ Tiếp nhận bệnh nhân mới</a>
        </div>
    </div>

    <c:if test="${not empty sessionScope.flashSuccess}">
        <div class="alert alert-success"><c:out value="${sessionScope.flashSuccess}"/></div>
        <% session.removeAttribute("flashSuccess"); %>
    </c:if>

    <section class="card intake-disclosure" id="new-patient" hidden>
        <div class="intake-panel-heading">
            <div><strong>Tiếp nhận bệnh nhân mới</strong><small>Tạo hồ sơ và tài khoản đăng nhập.</small></div>
            <button class="btn btn-light btn-sm" type="button" data-close-intake>Đóng form</button>
        </div>
        <div class="intake-disclosure-content">
            <c:if test="${not empty intakeError}">
                <div class="alert alert-danger"><c:out value="${intakeError}"/></div>
            </c:if>
            <div class="intake-guide">
                <strong>Kiểm tra trước khi tạo</strong>
                <span>Tìm bằng số điện thoại, BHYT hoặc CCCD ở danh sách bên dưới để tránh hồ sơ trùng.</span>
            </div>
            <form action="${pageContext.request.contextPath}/PatientForm" method="post"
                  data-validate="patient" class="patient-intake-form" novalidate>
                <div class="patient-intake-grid">
                    <div class="form-group">
                        <label class="required" for="intakeFullName">Họ và tên</label>
                        <input id="intakeFullName" class="form-control" name="fullName"
                               value="${fn:escapeXml(param.fullName)}" maxlength="100"
                               autocomplete="name" placeholder="Nguyễn Văn A" required>
                        <span class="err-msg" id="err_fullName"></span>
                    </div>
                    <div class="form-group">
                        <label class="required" for="intakeDob">Ngày sinh</label>
                        <input id="intakeDob" class="form-control" type="date" name="dateOfBirth"
                               value="${fn:escapeXml(param.dateOfBirth)}" min="1900-01-01" max="${maxDOB}" required>
                        <span class="err-msg" id="err_dob"></span>
                    </div>
                    <div class="form-group">
                        <label for="intakeGender">Giới tính</label>
                        <select id="intakeGender" class="form-control" name="gender">
                            <option value="Nam" ${empty param.gender || param.gender=='Nam'?'selected':''}>Nam</option>
                            <option value="Nữ" ${param.gender=='Nữ'?'selected':''}>Nữ</option>
                            <option value="Khác" ${param.gender=='Khác'?'selected':''}>Khác</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label class="required" for="intakePhone">Số điện thoại</label>
                        <input id="intakePhone" class="form-control" name="phone"
                               value="${fn:escapeXml(param.phone)}" inputmode="tel" maxlength="15"
                               autocomplete="tel" placeholder="0912345678" required>
                        <span class="err-msg" id="err_phone"></span>
                    </div>
                    <div class="form-group">
                        <label for="intakeInsurance">Số BHYT</label>
                        <input id="intakeInsurance" class="form-control" name="healthInsuranceNo"
                               value="${fn:escapeXml(param.healthInsuranceNo)}" maxlength="20"
                               placeholder="HC4012345678">
                        <span class="err-msg" id="err_bhyt"></span>
                    </div>
                    <div class="form-group patient-intake-wide">
                        <label class="required" for="intakeAddress">Địa chỉ</label>
                        <textarea id="intakeAddress" class="form-control" name="address" maxlength="255"
                                  autocomplete="street-address"
                                  placeholder="Số nhà, đường, phường/xã, quận/huyện, tỉnh/thành phố" required><c:out value="${param.address}"/></textarea>
                    </div>
                </div>

                <section class="account-section intake-account-section">
                    <div class="card-title">Tài khoản đăng nhập của bệnh nhân</div>
                    <p class="text-muted">Có Email/Gmail: hệ thống gửi thông tin đăng nhập tự động. Không có email: nhân viên cấp trực tiếp mật khẩu tạm thời.</p>
                    <div class="patient-intake-grid account-grid">
                        <div class="form-group">
                            <label class="required" for="intakeUsername">Tên đăng nhập</label>
                            <input id="intakeUsername" class="form-control" name="username"
                                   minlength="4" maxlength="30" pattern="[A-Za-z0-9_]+"
                                   value="${fn:escapeXml(param.username)}" autocomplete="off"
                                   placeholder="Ví dụ: nguyenvana" required>
                            <small>Chỉ dùng chữ không dấu, số và dấu gạch dưới.</small>
                        </div>
                        <div class="form-group">
                            <label class="required" for="intakePassword">Mật khẩu tạm thời</label>
                            <input id="intakePassword" class="form-control" type="password" name="password"
                                   minlength="8" maxlength="72" autocomplete="new-password" required>
                            <small>Tối thiểu 8 ký tự; bệnh nhân nên đổi sau lần đăng nhập đầu.</small>
                        </div>
                        <div class="form-group">
                            <label for="intakeEmail">Email/Gmail nhận tài khoản</label>
                            <input id="intakeEmail" class="form-control" type="email" name="email"
                                   maxlength="100" value="${fn:escapeXml(param.email)}"
                                   autocomplete="email" placeholder="benhnhan@gmail.com">
                            <small>Chấp nhận Gmail hoặc địa chỉ email hợp lệ khác.</small>
                        </div>
                    </div>
                </section>

                <div class="appointment-form-footer intake-form-footer">
                    <p>Sau khi tạo thành công, bệnh nhân xuất hiện ngay trong danh sách bên dưới.</p>
                    <button type="submit" class="btn btn-primary">Tạo hồ sơ và tài khoản</button>
                </div>
            </form>
        </div>
    </section>

    <section class="card patient-management-card" id="patients">
        <div class="section-header">
            <div>
                <span class="panel-eyebrow">HỒ SƠ BỆNH NHÂN</span>
                <h2>${not empty keyword ? 'Kết quả tìm kiếm' : 'Danh sách bệnh nhân'}</h2>
                <p>Tìm theo tên, số điện thoại, BHYT hoặc CCCD rồi mở đúng hồ sơ cần xử lý.</p>
            </div>
            <span class="data-count">${totalPatients} ${not empty keyword ? 'kết quả' : 'bệnh nhân'}</span>
        </div>

        <form action="${pageContext.request.contextPath}/StaffDashboard#patients" method="get"
              class="patient-search-bar" role="search">
            <label class="sr-only" for="staffPatientSearch">Tìm bệnh nhân</label>
            <input id="staffPatientSearch" type="search" name="keyword"
                   value="${fn:escapeXml(keyword)}" maxlength="80" autocomplete="off"
                   placeholder="Nhập tên, số điện thoại, BHYT hoặc CCCD">
            <button type="submit" class="btn btn-primary">Tìm bệnh nhân</button>
            <c:if test="${not empty keyword}">
                <a class="btn btn-light" href="${pageContext.request.contextPath}/StaffDashboard#patients">Xóa bộ lọc</a>
            </c:if>
        </form>

        <div class="table-scroll">
            <table class="modern-table patient-management-table">
                <thead>
                <tr>
                    <th>Bệnh nhân</th>
                    <th>Ngày sinh</th>
                    <th>Liên hệ</th>
                    <th>BHYT</th>
                    <th>Thao tác</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="p" items="${patients}">
                    <tr>
                        <td>
                            <div class="person-cell">
                                <span><c:out value="${fn:substring(p.fullName,0,1)}"/></span>
                                <div><strong><c:out value="${p.fullName}"/></strong><small><c:out value="${p.genderLabel}"/></small></div>
                            </div>
                        </td>
                        <td><c:out value="${p.dateOfBirth}" default="—"/></td>
                        <td><c:out value="${p.phone}"/><small class="table-sub"><c:out value="${p.address}"/></small></td>
                        <td><c:out value="${p.healthInsuranceNo}" default="—"/></td>
                        <td>
                            <div class="row-actions">
                                <a class="primary" href="${pageContext.request.contextPath}/PatientHistory?patientId=${p.patientId}">Xem hồ sơ</a>
                                <a href="${pageContext.request.contextPath}/PatientForm?id=${p.patientId}">Cập nhật</a>
                            </div>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty patients}">
                    <tr><td colspan="5" class="empty-state">Không tìm thấy bệnh nhân phù hợp.</td></tr>
                </c:if>
                </tbody>
            </table>
        </div>

        <c:if test="${totalPages > 1}">
            <nav class="pagination" aria-label="Phân trang bệnh nhân">
                <c:if test="${currentPage > 1}"><c:url var="staffPreviousPageUrl" value="/StaffDashboard"><c:param name="page" value="${currentPage-1}"/><c:param name="keyword" value="${keyword}"/></c:url><a href="${staffPreviousPageUrl}#patients">&laquo; Trước</a></c:if>
                <c:forEach begin="1" end="${totalPages}" var="pageNumber">
                    <c:if test="${pageNumber>=currentPage-2&&pageNumber<=currentPage+2}"><c:url var="staffPageUrl" value="/StaffDashboard"><c:param name="page" value="${pageNumber}"/><c:param name="keyword" value="${keyword}"/></c:url><a class="${currentPage==pageNumber?'active':''}" href="${staffPageUrl}#patients">${pageNumber}</a></c:if>
                </c:forEach>
                <c:if test="${currentPage < totalPages}"><c:url var="staffNextPageUrl" value="/StaffDashboard"><c:param name="page" value="${currentPage+1}"/><c:param name="keyword" value="${keyword}"/></c:url><a href="${staffNextPageUrl}#patients">Sau &raquo;</a></c:if>
            </nav>
        </c:if>
    </section>
</main>

<script>
document.querySelectorAll('[data-open-intake]').forEach(function (button) {
    button.addEventListener('click', function (event) {
        event.preventDefault();
        var intake = document.getElementById('new-patient');
        if (intake) {
            intake.hidden = false;
            intake.scrollIntoView({ behavior: 'smooth', block: 'start' });
            var firstField = intake.querySelector('input,select,textarea');
            if (firstField) firstField.focus({ preventScroll: true });
        }
    });
});
document.querySelectorAll('[data-close-intake]').forEach(function (button) {
    button.addEventListener('click', function () {
        var intake = document.getElementById('new-patient');
        if (intake) intake.hidden = true;
    });
});
var intake = document.getElementById('new-patient');
var shouldOpenIntake = ${showIntakeForm == true || param.newPatient == '1'};
if (intake && shouldOpenIntake) intake.hidden = false;
if (${showIntakeForm == true}) {
    window.addEventListener('load', function () {
        document.getElementById('new-patient').scrollIntoView({ block: 'start' });
    });
}
</script>
<script src="${pageContext.request.contextPath}/static/js/validate.js?v=20260722-validation1"></script>
<jsp:include page="footer.jsp"/>
</body>
</html>
