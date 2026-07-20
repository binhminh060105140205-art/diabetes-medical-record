<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<c:choose>
    <c:when test="${profileUser.role=='ADMIN'}">
        <c:set var="roleLabel" value="Quản trị viên"/>
        <c:set var="roleDescription" value="Quản lý vận hành, tài khoản và dữ liệu toàn hệ thống."/>
        <c:set var="roleHome" value="/AdminDashboard"/>
        <c:set var="roleClass" value="admin"/>
    </c:when>
    <c:when test="${profileUser.role=='STAFF'}">
        <c:set var="roleLabel" value="Nhân viên tiếp nhận"/>
        <c:set var="roleDescription" value="Tiếp nhận bệnh nhân, điều phối lịch và cập nhật kết quả xét nghiệm."/>
        <c:set var="roleHome" value="/StaffDashboard"/>
        <c:set var="roleClass" value="staff"/>
    </c:when>
    <c:when test="${profileUser.role=='DOCTOR'}">
        <c:set var="roleLabel" value="Bác sĩ điều trị"/>
        <c:set var="roleDescription" value="Khám, chỉ định xét nghiệm và hoàn tất hồ sơ điều trị tiểu đường."/>
        <c:set var="roleHome" value="/DoctorDashboard"/>
        <c:set var="roleClass" value="doctor"/>
    </c:when>
    <c:otherwise>
        <c:set var="roleLabel" value="Bệnh nhân"/>
        <c:set var="roleDescription" value="Theo dõi sức khỏe, lịch khám và hồ sơ điều trị cá nhân."/>
        <c:set var="roleHome" value="/PatientDashboard"/>
        <c:set var="roleClass" value="patient"/>
    </c:otherwise>
</c:choose>
<c:set var="profilePost" value="${pageContext.request.method=='POST' && activeSetting=='personal' && not empty errorMessage}"/>
<c:set var="selectedGender" value="${profilePost ? param.gender : profileUser.genderLabel}"/>
<c:set var="selectedSetting" value="${empty activeSetting ? 'menu' : activeSetting}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Cài đặt tài khoản — DiaCare</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css?v=20260720-ui8">
</head>
<body>
<jsp:include page="header.jsp"/>
<jsp:include page="topnav.jsp"/>

<main class="page-wrapper settings-page">
    <div class="page-heading settings-heading">
        <div>
            <div class="eyebrow">TÀI KHOẢN VÀ BẢO MẬT</div>
            <h1 class="page-title">Cài đặt tài khoản</h1>
            <p class="text-muted">Quản lý thông tin cá nhân, mật khẩu và phiên đăng nhập tại một nơi.</p>
        </div>
        <a class="btn btn-light" href="${pageContext.request.contextPath}${roleHome}">Quay lại trang làm việc</a>
    </div>

    <c:if test="${not empty successMessage}"><div class="alert alert-success"><c:out value="${successMessage}"/></div></c:if>
    <c:if test="${not empty errorMessage}"><div class="alert alert-danger"><c:out value="${errorMessage}"/></div></c:if>

    <div class="settings-layout">
        <aside class="card settings-sidebar-card">
            <div class="settings-account-summary">
                <span class="settings-avatar"><c:out value="${fn:substring(profileUser.fullName,0,1)}" default="D"/></span>
                <div>
                    <strong><c:out value="${profileUser.fullName}"/></strong>
                    <small>@<c:out value="${profileUser.username}"/></small>
                </div>
            </div>
            <div class="settings-role role-${roleClass}">
                <span>${roleLabel}</span>
                <p>${roleDescription}</p>
            </div>
            <div class="settings-menu-hint">Chọn một mục để mở đúng phần cần thao tác.</div>
            <nav class="settings-action-list" aria-label="Các mục cài đặt" role="tablist">
                <a class="settings-action ${selectedSetting=='personal'?'active':''}" href="${pageContext.request.contextPath}/Settings?section=profile" data-setting-trigger="personal" role="tab" aria-controls="settings-personal" aria-selected="${selectedSetting=='personal'}">
                    <span>01</span><div><strong>Hồ sơ tài khoản</strong><small>Thông tin cá nhân và liên hệ</small></div><b aria-hidden="true">›</b>
                </a>
                <a class="settings-action ${selectedSetting=='security'?'active':''}" href="${pageContext.request.contextPath}/Settings?section=password" data-setting-trigger="security" role="tab" aria-controls="settings-security" aria-selected="${selectedSetting=='security'}">
                    <span>02</span><div><strong>Đổi mật khẩu</strong><small>Bảo mật tài khoản</small></div><b aria-hidden="true">›</b>
                </a>
                <a class="settings-action ${selectedSetting=='session'?'active':''}" href="${pageContext.request.contextPath}/Settings?section=logout" data-setting-trigger="session" role="tab" aria-controls="settings-session" aria-selected="${selectedSetting=='session'}">
                    <span>03</span><div><strong>Đăng xuất</strong><small>Kết thúc phiên an toàn</small></div><b aria-hidden="true">›</b>
                </a>
            </nav>
        </aside>

        <div class="settings-content">
            <div class="settings-empty-state ${selectedSetting=='menu'?'is-active':''}" data-setting-empty>
                <span class="settings-empty-icon">⚙</span>
                <h2>Chọn mục cài đặt</h2>
                <p>Chọn một nút bên trái để mở đúng form cần dùng.</p>
            </div>

            <section class="card settings-section settings-panel ${selectedSetting=='personal'?'is-active':''}" id="settings-personal" data-setting-panel="personal">
                <div class="section-header">
                    <div><span class="panel-eyebrow">THÔNG TIN CÁ NHÂN</span><h2>Hồ sơ tài khoản</h2><p>Cập nhật thông tin dùng chung trên toàn hệ thống.</p></div>
                    <span class="status-pill status-ACTIVE">Đang hoạt động</span>
                </div>
                <form action="${pageContext.request.contextPath}/Settings?section=profile" method="post">
                    <input type="hidden" name="action" value="profile">
                    <div class="settings-form-grid">
                        <div class="form-group">
                            <label class="required" for="settingUsername">Tên đăng nhập</label>
                            <input id="settingUsername" class="form-control" name="username"
                                   value="${fn:escapeXml(profilePost ? param.username : profileUser.username)}"
                                   minlength="4" maxlength="50" pattern="[A-Za-z0-9._-]+"
                                   autocomplete="username" required>
                            <small>Dùng để đăng nhập; không chứa khoảng trắng.</small>
                        </div>
                        <div class="form-group">
                            <label class="required" for="settingFullName">Họ và tên</label>
                            <input id="settingFullName" class="form-control" name="fullName"
                                   value="${fn:escapeXml(profilePost ? param.fullName : profileUser.fullName)}"
                                   minlength="2" maxlength="100" autocomplete="name" required>
                        </div>
                        <div class="form-group">
                            <label for="settingEmail">Email/Gmail</label>
                            <input id="settingEmail" class="form-control" type="email" name="email"
                                   value="${fn:escapeXml(profilePost ? param.email : profileUser.email)}"
                                   maxlength="100" autocomplete="email" placeholder="ten@gmail.com">
                        </div>
                        <div class="form-group">
                            <label for="settingPhone">Số điện thoại</label>
                            <input id="settingPhone" class="form-control" type="tel" name="phone"
                                   value="${fn:escapeXml(profilePost ? param.phone : profileUser.phone)}"
                                   pattern="(0|\+84)[0-9]{9}" maxlength="12" autocomplete="tel"
                                   placeholder="0912345678">
                        </div>
                        <div class="form-group">
                            <label for="settingIdentity">CCCD/CMND</label>
                            <input id="settingIdentity" class="form-control" name="cccd"
                                   value="${fn:escapeXml(profilePost ? param.cccd : profileUser.cccd)}"
                                   inputmode="numeric" pattern="([0-9]{9}|[0-9]{12})" maxlength="12">
                        </div>
                        <div class="form-group">
                            <label for="settingDob">Ngày sinh</label>
                            <input id="settingDob" class="form-control" type="date" name="dob"
                                   value="${fn:escapeXml(profilePost ? param.dob : profileUser.dob)}" max="${maxDOB}">
                        </div>
                        <div class="form-group">
                            <label for="settingGender">Giới tính</label>
                            <select id="settingGender" class="form-control" name="gender">
                                <option value="">Chưa cập nhật</option>
                                <option value="Nam" ${selectedGender=='Nam'?'selected':''}>Nam</option>
                                <option value="Nữ" ${selectedGender=='Nữ'?'selected':''}>Nữ</option>
                                <option value="Khác" ${selectedGender=='Khác'?'selected':''}>Khác</option>
                            </select>
                        </div>
                        <div class="form-group settings-wide-field">
                            <label for="settingAddress">Địa chỉ</label>
                            <textarea id="settingAddress" class="form-control" name="address" maxlength="255"
                                      autocomplete="street-address" placeholder="Địa chỉ liên hệ hiện tại"><c:out value="${profilePost ? param.address : profileUser.address}"/></textarea>
                        </div>
                    </div>
                    <div class="settings-form-footer">
                        <p>Thông tin của bệnh nhân sẽ đồng bộ với hồ sơ điều trị cá nhân.</p>
                        <button type="submit" class="btn btn-primary">Lưu thông tin</button>
                    </div>
                </form>
            </section>

            <c:if test="${profileUser.role=='DOCTOR'}">
                <section class="card settings-section settings-panel ${selectedSetting=='personal'?'is-active':''}" id="professional" data-setting-panel="personal">
                    <div class="section-header">
                        <div><span class="panel-eyebrow">HỒ SƠ HÀNH NGHỀ</span><h2>Minh chứng bác sĩ</h2><p>Ảnh do Admin cập nhật khi tạo hoặc quản lý tài khoản bác sĩ.</p></div>
                        <span class="status-pill professional-readonly">Chỉ xem</span>
                    </div>
                    <c:if test="${not empty professionalError}"><div class="alert alert-danger"><c:out value="${professionalError}"/></div></c:if>
                    <c:choose>
                        <c:when test="${empty doctor}">
                            <div class="professional-empty">Chưa có hồ sơ bác sĩ liên kết với tài khoản này.</div>
                        </c:when>
                        <c:otherwise>
                            <dl class="professional-summary">
                                <div><dt>Chuyên khoa</dt><dd><c:out value="${doctor.specialty}" default="Nội tiết - Đái tháo đường"/></dd></div>
                                <div><dt>Số chứng chỉ</dt><dd><c:out value="${doctor.licenseNo}" default="Chưa cập nhật"/></dd></div>
                                <div><dt>Phạm vi theo dõi</dt><dd><c:out value="${doctor.diabetesFocusLabel}"/></dd></div>
                            </dl>
                            <div class="professional-document-grid">
                                <article class="professional-document">
                                    <strong>Ảnh bác sĩ</strong>
                                    <c:choose><c:when test="${not empty doctor.faceImagePath}">
                                        <a href="${pageContext.request.contextPath}/DoctorFile?doctorId=${doctor.doctorId}&type=face" target="_blank" rel="noopener">
                                            <img src="${pageContext.request.contextPath}/DoctorFile?doctorId=${doctor.doctorId}&type=face" alt="Ảnh bác sĩ" loading="lazy" decoding="async">
                                        </a>
                                    </c:when><c:otherwise><div class="professional-document-empty">Chưa được Admin cập nhật</div></c:otherwise></c:choose>
                                </article>
                                <article class="professional-document">
                                    <strong>Ảnh CCCD</strong>
                                    <c:choose><c:when test="${not empty doctor.cccdImagePath}">
                                        <a href="${pageContext.request.contextPath}/DoctorFile?doctorId=${doctor.doctorId}&type=cccd" target="_blank" rel="noopener">
                                            <img src="${pageContext.request.contextPath}/DoctorFile?doctorId=${doctor.doctorId}&type=cccd" alt="Ảnh CCCD của bác sĩ" loading="lazy" decoding="async">
                                        </a>
                                    </c:when><c:otherwise><div class="professional-document-empty">Chưa được Admin cập nhật</div></c:otherwise></c:choose>
                                </article>
                                <article class="professional-document">
                                    <strong>Chứng chỉ hành nghề</strong>
                                    <c:choose><c:when test="${not empty doctor.licenseImagePath}">
                                        <a href="${pageContext.request.contextPath}/DoctorFile?doctorId=${doctor.doctorId}&type=license" target="_blank" rel="noopener">
                                            <img src="${pageContext.request.contextPath}/DoctorFile?doctorId=${doctor.doctorId}&type=license" alt="Chứng chỉ hành nghề của bác sĩ" loading="lazy" decoding="async">
                                        </a>
                                    </c:when><c:otherwise><div class="professional-document-empty">Chưa được Admin cập nhật</div></c:otherwise></c:choose>
                                </article>
                            </div>
                            <p class="professional-admin-note">Nếu tài liệu chưa đúng, bác sĩ liên hệ Admin để được kiểm tra và cập nhật.</p>
                        </c:otherwise>
                    </c:choose>
                </section>
            </c:if>

            <section class="card settings-section settings-panel ${selectedSetting=='security'?'is-active':''}" id="settings-security" data-setting-panel="security">
                <div class="section-header">
                    <div><span class="panel-eyebrow">BẢO MẬT</span><h2>Đổi mật khẩu</h2><p>Xác nhận mật khẩu hiện tại trước khi đặt mật khẩu mới.</p></div>
                </div>
                <form action="${pageContext.request.contextPath}/Settings?section=password" method="post" class="password-settings-form">
                    <input type="hidden" name="action" value="password">
                    <div class="settings-security-note"><strong>Mật khẩu an toàn</strong><span>Dùng tối thiểu 8 ký tự và không sử dụng lại mật khẩu hiện tại.</span></div>
                    <div class="settings-password-grid">
                        <div class="form-group"><label class="required" for="currentPassword">Mật khẩu hiện tại</label><input id="currentPassword" class="form-control" type="password" name="currentPassword" maxlength="72" autocomplete="current-password" required></div>
                        <div class="form-group"><label class="required" for="newPassword">Mật khẩu mới</label><input id="newPassword" class="form-control" type="password" name="newPassword" minlength="8" maxlength="72" autocomplete="new-password" required></div>
                        <div class="form-group"><label class="required" for="confirmPassword">Xác nhận mật khẩu mới</label><input id="confirmPassword" class="form-control" type="password" name="confirmPassword" minlength="8" maxlength="72" autocomplete="new-password" required></div>
                    </div>
                    <div class="settings-form-footer"><p>Sau khi đổi, sử dụng mật khẩu mới cho lần đăng nhập tiếp theo.</p><button type="submit" class="btn btn-primary">Đổi mật khẩu</button></div>
                </form>
            </section>

            <section class="card settings-section session-settings settings-panel ${selectedSetting=='session'?'is-active':''}" id="settings-session" data-setting-panel="session">
                <div class="session-settings-copy">
                    <span class="session-settings-icon">⇥</span>
                    <div><span class="panel-eyebrow">PHIÊN ĐĂNG NHẬP</span><h2>Đăng xuất khỏi DiaCare</h2><p>Kết thúc phiên làm việc hiện tại trên thiết bị này. Dữ liệu đã lưu không bị ảnh hưởng.</p></div>
                </div>
                <form action="${pageContext.request.contextPath}/Logout" method="post"
                      onsubmit="return confirm('Bạn muốn đăng xuất khỏi DiaCare?')">
                    <button type="submit" class="btn btn-danger">Đăng xuất</button>
                </form>
            </section>
        </div>
    </div>
</main>

<jsp:include page="footer.jsp"/>
<script>
(function () {
    const triggers = document.querySelectorAll('[data-setting-trigger]');
    const panels = document.querySelectorAll('[data-setting-panel]');
    const emptyState = document.querySelector('[data-setting-empty]');

    function activate(setting) {
        triggers.forEach(function (trigger) {
            const active = trigger.dataset.settingTrigger === setting;
            trigger.classList.toggle('active', active);
            trigger.setAttribute('aria-selected', String(active));
        });
        panels.forEach(function (panel) {
            const active = panel.dataset.settingPanel === setting;
            panel.classList.toggle('is-active', active);
            panel.setAttribute('aria-hidden', String(!active));
        });
        if (emptyState) emptyState.classList.toggle('is-active', !setting);
    }

    triggers.forEach(function (trigger) {
        trigger.addEventListener('click', function (event) {
            event.preventDefault();
            activate(trigger.dataset.settingTrigger);
            history.replaceState(null, '', trigger.getAttribute('href'));
        });
    });

    const initial = document.querySelector('[data-setting-trigger].active');
    activate(initial ? initial.dataset.settingTrigger : '');
}());
</script>
</body>
</html>
