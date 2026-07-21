<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Hồ sơ bác sĩ — Quản trị viên</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css?v=20260722-web-audit2">
    <style>
        .form-container {
            max-width: 820px;
            margin: 0 auto;
            background: #fff;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.1);
        }
        .info-row { display:flex; flex-wrap:wrap; gap:20px; margin-bottom:18px; font-size:14px; }
        .info-row div { min-width:180px; }
        .info-row b { display:block; color:#6c757d; font-size:12px; font-weight:600; }
        .doc-image-row {
            display: flex;
            flex-wrap: wrap;
            gap: 20px;
            margin-bottom: 15px;
        }
        .doc-image-box {
            flex: 1 1 220px;
            border: 1px solid #dee2e6;
            border-radius: 8px;
            padding: 12px;
            text-align: center;
        }
        .doc-image-box label {
            display: block;
            font-weight: 600;
            margin-bottom: 8px;
        }
        .doc-image-box .preview {
            width: 100%;
            max-height: 160px;
            object-fit: contain;
            background: #f8f9fa;
            border-radius: 6px;
            margin-bottom: 10px;
        }
        .doc-image-box .no-image {
            width: 100%;
            height: 160px;
            display: flex;
            align-items: center;
            justify-content: center;
            background: #f8f9fa;
            color: #adb5bd;
            border-radius: 6px;
            margin-bottom: 10px;
            font-size: 13px;
        }
        .doc-image-box input[type="file"] { width: 100%; font-size: 12px; }
        .hint { font-size: 12px; color: #6c757d; margin-top: 15px; }
    </style>
</head>
<body>
<jsp:include page="header.jsp"/>
<jsp:include page="topnav.jsp"/>

<div class="page-wrapper">
    <h1 class="page-title">Hồ sơ minh chứng — <c:out value="${targetUser.fullName}"/></h1>

    <c:if test="${not empty toastMessage}">
        <div class="alert alert-${toastType == 'danger' ? 'danger' : 'success'}"><c:out value="${toastMessage}"/></div>
    </c:if>

    <div class="form-container">
        <div class="info-row">
            <div><b>Tên đăng nhập</b><c:out value="${targetUser.username}"/></div>
            <div><b>Số điện thoại</b><c:choose><c:when test="${not empty targetUser.phone}"><c:out value="${targetUser.phone}"/></c:when><c:otherwise>Chưa cập nhật</c:otherwise></c:choose></div>
            <div><b>Email/Gmail</b><c:choose><c:when test="${not empty targetUser.email}"><c:out value="${targetUser.email}"/></c:when><c:otherwise>Chưa cập nhật</c:otherwise></c:choose></div>
            <div><b>Số CCCD</b><c:choose><c:when test="${not empty targetUser.cccd}"><c:out value="${targetUser.cccd}"/></c:when><c:otherwise>Chưa cập nhật</c:otherwise></c:choose></div>
        </div>

        <c:if test="${not empty doctor}">
        <div class="info-row">
            <div><b>Chuyên khoa</b><c:choose><c:when test="${not empty doctor.specialty}"><c:out value="${doctor.specialty}"/></c:when><c:otherwise>Chưa cập nhật</c:otherwise></c:choose></div>
            <div><b>Số chứng chỉ hành nghề</b><c:choose><c:when test="${not empty doctor.licenseNo}"><c:out value="${doctor.licenseNo}"/></c:when><c:otherwise>Chưa cập nhật</c:otherwise></c:choose></div>
            <div><b>Học vị / Bằng cấp</b><c:choose><c:when test="${not empty doctor.degree}"><c:out value="${doctor.degree}"/></c:when><c:otherwise>Chưa cập nhật</c:otherwise></c:choose></div>
            <div><b>Nhóm tiểu đường ưu tiên</b><c:out value="${doctor.diabetesFocusLabel}"/></div>
        </div>
        </c:if>

        <c:choose>
        <c:when test="${empty doctor}">
            <div class="alert alert-danger">Tài khoản này chưa có hồ sơ bác sĩ tương ứng (thiếu dữ liệu chuyên khoa hoặc chứng chỉ), nên chưa thể lưu ảnh minh chứng.</div>
        </c:when>
        <c:otherwise>
            <form action="${pageContext.request.contextPath}/AdminDoctorDetail" method="POST" enctype="multipart/form-data">
                <input type="hidden" name="userId" value="${targetUser.userId}">
                <div class="form-group">
                    <label>Nhóm tiểu đường ưu tiên</label>
                    <select name="diabetesFocus" class="form-control">
                        <option value="BOTH" ${doctor.diabetesFocus=='BOTH'?'selected':''}>Theo dõi cả típ 1 và típ 2</option>
                        <option value="TYPE_1" ${doctor.diabetesFocus=='TYPE_1'?'selected':''}>Ưu tiên đái tháo đường típ 1</option>
                        <option value="TYPE_2" ${doctor.diabetesFocus=='TYPE_2'?'selected':''}>Ưu tiên đái tháo đường típ 2</option>
                    </select>
                    <small>Mặc định bác sĩ theo dõi cả hai típ; chỉ chọn riêng khi có ưu tiên chuyên môn rõ ràng.</small>
                </div>
                <div class="doc-image-row">
                    <div class="doc-image-box">
                        <label>CCCD mặt trước</label>
                        <c:choose>
                            <c:when test="${not empty doctor.cccdImagePath}">
                                <img class="preview" src="${pageContext.request.contextPath}/DoctorFile?doctorId=${doctor.doctorId}&type=cccd">
                            </c:when>
                            <c:otherwise><div class="no-image">Chưa có ảnh</div></c:otherwise>
                        </c:choose>
                        <input type="file" name="cccdFrontImage" accept="image/png,image/jpeg,image/webp">
                    </div>

                    <div class="doc-image-box">
                        <label>CCCD mặt sau</label>
                        <c:choose>
                            <c:when test="${not empty doctor.cccdBackImagePath}">
                                <img class="preview" src="${pageContext.request.contextPath}/DoctorFile?doctorId=${doctor.doctorId}&type=cccd-back">
                            </c:when>
                            <c:otherwise><div class="no-image">Chưa có ảnh</div></c:otherwise>
                        </c:choose>
                        <input type="file" name="cccdBackImage" accept="image/png,image/jpeg,image/webp">
                    </div>

                    <div class="doc-image-box">
                        <label>Ảnh chứng chỉ hành nghề</label>
                        <c:choose>
                            <c:when test="${not empty doctor.licenseImagePath}">
                                <img class="preview" src="${pageContext.request.contextPath}/DoctorFile?doctorId=${doctor.doctorId}&type=license">
                            </c:when>
                            <c:otherwise><div class="no-image">Chưa có ảnh</div></c:otherwise>
                        </c:choose>
                        <input type="file" name="licenseImage" accept="image/png,image/jpeg,image/webp">
                    </div>
                </div>

                <button type="submit" class="btn btn-primary">Lưu hồ sơ</button>
                <a href="${pageContext.request.contextPath}/AdminDashboard?filterRole=DOCTOR" class="btn btn-light">Quay lại danh sách</a>
                <p class="hint">Quản trị viên có thể xem và thay thế ảnh hộ bác sĩ nếu cần. Chỉ chấp nhận ảnh JPG, PNG hoặc WEBP, tối đa 5MB mỗi ảnh.</p>
            </form>
        </c:otherwise>
        </c:choose>
    </div>
</div>

<jsp:include page="footer.jsp"/>
</body>
</html>
