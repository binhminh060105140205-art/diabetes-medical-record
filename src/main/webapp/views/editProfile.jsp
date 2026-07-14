<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Sửa Thông Tin Tài Khoản</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
    <style>
        .form-container {
            max-width: 600px;
            margin: 0 auto;
            background: #fff;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.1);
        }
        .form-group {
            margin-bottom: 15px;
        }
        .form-group label {
            display: block;
            margin-bottom: 5px;
            font-weight: 500;
        }
        .form-group input, .form-group select {
            width: 100%;
            padding: 8px;
            border: 1px solid #ccc;
            border-radius: 4px;
        }
        .btn-submit {
            display: inline-block;
            padding: 10px 20px;
            background: #0d6efd;
            color: #fff;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            text-align: center;
        }
        .btn-submit:hover {
            background: #0b5ed7;
        }
        .alert-success { color: #0f5132; background-color: #d1e7dd; padding: 10px; border-radius: 4px; margin-bottom: 15px; }
        .alert-error { color: #842029; background-color: #f8d7da; padding: 10px; border-radius: 4px; margin-bottom: 15px; }
    </style>
</head>
<body>
<jsp:include page="header.jsp"/>
<jsp:include page="topnav.jsp"/>

<div class="page-wrapper">
    <h1 class="page-title">✏️ Sửa Thông Tin Cá Nhân</h1>

    <div class="form-container">
        <c:if test="${not empty successMessage}">
            <div class="alert-success">${successMessage}</div>
        </c:if>
        <c:if test="${not empty errorMessage}">
            <div class="alert-error">${errorMessage}</div>
        </c:if>

        <form action="${pageContext.request.contextPath}/EditProfile" method="POST">
            <div class="form-group">
                <label>Tên đăng nhập</label>
                <input type="text" name="username" value="${profileUser.username}" required>
            </div>
            <div class="form-group">
                <label>Mật khẩu (Để trống nếu không đổi)</label>
                <input type="password" name="password" placeholder="Nhập mật khẩu mới...">
            </div>
            <div class="form-group">
                <label>Họ và tên</label>
                <input type="text" name="fullName" value="${profileUser.fullName}" required>
            </div>
            <div class="form-group">
                <label>Số CCCD / CMND</label>
                <input type="text" name="cccd" value="${profileUser.cccd}">
            </div>
            <div class="form-group">
                <label>Số điện thoại</label>
                <input type="text" name="phone" value="${profileUser.phone}">
            </div>
            <div class="form-group">
                <label>Email</label>
                <input type="email" name="email" value="${profileUser.email}">
            </div>
            <div class="form-group">
                <label>Ngày sinh</label>
                <input type="date" name="dob" value="${profileUser.dob}">
            </div>
            <div class="form-group">
                <label>Giới tính</label>
                <select name="gender">
                    <option value="">-- Chọn giới tính --</option>
                    <option value="Nam" ${profileUser.gender == 'Nam' ? 'selected' : ''}>Nam</option>
                    <option value="Nữ" ${profileUser.gender == 'Nữ' ? 'selected' : ''}>Nữ</option>
                </select>
            </div>
            
            <button type="submit" class="btn-submit">Lưu Thay Đổi</button>
        </form>
    </div>
</div>

</body>
</html>
