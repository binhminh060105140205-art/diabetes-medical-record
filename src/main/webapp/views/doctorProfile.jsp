<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Hồ Sơ Minh Chứng Bác Sĩ</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
    <style>
        .form-container {
            max-width: 760px;
            margin: 0 auto;
            background: #fff;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.1);
        }
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
        .doc-image-box input[type="file"] {
            width: 100%;
            font-size: 12px;
        }
        .btn-submit {
            display: inline-block;
            padding: 10px 20px;
            background: #0d6efd;
            color: #fff;
            border: none;
            border-radius: 4px;
            cursor: pointer;
        }
        .btn-submit:hover { background: #0b5ed7; }
        .hint { font-size: 12px; color: #6c757d; margin-top: 15px; }
    </style>
</head>
<body>
<jsp:include page="header.jsp"/>
<jsp:include page="topnav.jsp"/>

<div class="page-wrapper">
    <h1 class="page-title">🪪 Hồ Sơ Minh Chứng Bác Sĩ</h1>

    <c:if test="${not empty toastMessage}">
        <div class="alert alert-${toastType == 'danger' ? 'danger' : 'success'}">${toastMessage}</div>
    </c:if>

    <c:if test="${empty doctor}">
        <div class="alert alert-danger">Không tìm thấy hồ sơ bác sĩ tương ứng với tài khoản này.</div>
    </c:if>

    <c:if test="${not empty doctor}">
        <div class="form-container">
            <form action="${pageContext.request.contextPath}/DoctorProfile" method="POST" enctype="multipart/form-data">
                <div class="doc-image-row">
                    <div class="doc-image-box">
                        <label>Ảnh khuôn mặt</label>
                        <c:choose>
                            <c:when test="${not empty doctor.faceImagePath}">
                                <img class="preview" src="${pageContext.request.contextPath}/DoctorFile?doctorId=${doctor.doctorId}&type=face">
                            </c:when>
                            <c:otherwise><div class="no-image">Chưa có ảnh</div></c:otherwise>
                        </c:choose>
                        <input type="file" name="faceImage" accept="image/png,image/jpeg,image/webp">
                    </div>

                    <div class="doc-image-box">
                        <label>Ảnh CCCD</label>
                        <c:choose>
                            <c:when test="${not empty doctor.cccdImagePath}">
                                <img class="preview" src="${pageContext.request.contextPath}/DoctorFile?doctorId=${doctor.doctorId}&type=cccd">
                            </c:when>
                            <c:otherwise><div class="no-image">Chưa có ảnh</div></c:otherwise>
                        </c:choose>
                        <input type="file" name="cccdImage" accept="image/png,image/jpeg,image/webp">
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

                <button type="submit" class="btn-submit">💾 Lưu ảnh</button>
                <p class="hint">Chỉ chấp nhận ảnh JPG/PNG/WEBP, tối đa 5MB mỗi ảnh. Có thể chỉ chọn ảnh muốn cập nhật, các ảnh còn lại sẽ được giữ nguyên.</p>
            </form>
        </div>
    </c:if>
</div>

<jsp:include page="footer.jsp"/>
</body>
</html>
