<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Thùng rác — DiaCare</title><link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css?v=20260722-web-audit2"></head>
<body><jsp:include page="header.jsp"/><jsp:include page="topnav.jsp"/>
<main class="page-wrapper app-workspace">
    <div class="workspace-heading"><div><span class="workspace-kicker">QUẢN TRỊ DỮ LIỆU</span><h1>Thùng rác</h1></div><a class="btn btn-light" href="${pageContext.request.contextPath}/AdminDashboard">Quay lại tài khoản</a></div>
    <c:if test="${not empty adminTrashMessage}"><div class="alert alert-info"><c:out value="${adminTrashMessage}"/></div></c:if>
    <section class="card">
        <div class="section-header"><div><h2>Dữ liệu đã xóa mềm</h2><p>Có thể khôi phục tài khoản; xóa vĩnh viễn chỉ thực hiện được khi không còn dữ liệu liên quan.</p></div><span class="data-count">${trashItems.size()} mục</span></div>
        <div class="table-scroll"><table><thead><tr><th>ID</th><th>Dữ liệu</th><th>Thông tin</th><th>Ngày xóa</th><th>Thao tác</th></tr></thead><tbody>
        <c:forEach var="item" items="${trashItems}"><tr><td>#${item.trash_id}</td><td><strong><c:out value="${item.display_name}"/></strong><small class="table-sub">Tài khoản #${item.entity_id}</small></td><td><c:out value="${item.details}"/></td><td><fmt:formatDate value="${item.deleted_at}" pattern="dd/MM/yyyy HH:mm"/></td><td><div class="table-actions">
            <form method="post" action="${pageContext.request.contextPath}/AdminTrash"><input type="hidden" name="action" value="restore"><input type="hidden" name="trashId" value="${item.trash_id}"><button class="btn btn-success btn-sm" type="submit">Khôi phục</button></form>
            <form method="post" action="${pageContext.request.contextPath}/AdminTrash" onsubmit="return confirm('Xóa vĩnh viễn mục này? Thao tác không thể hoàn tác.')"><input type="hidden" name="action" value="purge"><input type="hidden" name="trashId" value="${item.trash_id}"><button class="btn btn-danger btn-sm" type="submit">Xóa vĩnh viễn</button></form>
        </div></td></tr></c:forEach>
        <c:if test="${empty trashItems}"><tr><td colspan="5" class="empty-state">Thùng rác đang trống.</td></tr></c:if>
        </tbody></table></div>
    </section>
</main><jsp:include page="footer.jsp"/></body></html>
