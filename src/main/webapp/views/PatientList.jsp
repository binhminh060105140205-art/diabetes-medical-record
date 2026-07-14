<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<!DOCTYPE html>
<html>
    <head>
        <meta charset="UTF-8">
        <title>Danh Sách Bệnh Nhân</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
    </head>
    <body>
        <jsp:include page="header.jsp"/>
        <jsp:include page="topnav.jsp"/>
        <div class="page-wrapper">
            <h1 class="page-title">👥 Danh Sách Bệnh Nhân</h1>

            <div class="card" style="margin-bottom: 20px;">
                <form action="${pageContext.request.contextPath}/PatientList" method="get" style="display: flex; gap: 10px; align-items: center;">
                    <input type="text" name="keyword" value="${keyword}" placeholder="Tìm kiếm theo tên, SĐT, số BHYT..." style="flex: 1; padding: 8px 12px; border: 1px solid #ddd; border-radius: 4px;">
                    <button type="submit" class="btn btn-primary" style="padding: 8px 20px; background-color: #007bff; color: white; border: none; border-radius: 4px; cursor: pointer;">Tìm kiếm</button>
                    <c:if test="${not empty keyword}">
                        <a href="${pageContext.request.contextPath}/PatientList" class="btn" style="padding: 8px 15px; background-color: #6c757d; color: white; border-radius: 4px; text-decoration: none;">Xóa bộ lọc</a>
                    </c:if>
                </form>
            </div>

            <div class="card">
                <table>
                    <thead>
                        <tr>
                            <th>#</th>
                            <th>HỌ VÀ TÊN</th>
                            <th>NGÀY SINH</th>
                            <th>GIỚI TÍNH</th>
                            <th>SỐ ĐIỆN THOẠI</th>
                            <th>SỐ BHYT</th>
                            <th>THAO TÁC</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:choose>
                            <c:when test="${not empty patients}">
                                <c:forEach var="p" items="${patients}" varStatus="s">
                                    <tr>
                                        <td>${s.count + (currentPage - 1) * 10}</td>
                                        <td><strong>${p.fullName}</strong></td>
                                        <td>${p.dateOfBirth}</td>
                                        <td>${p.gender}</td>
                                        <td>${p.phone}</td>
                                        <td><code>${p.healthInsuranceNo}</code></td>
                                        <td>
                                            <a href="${pageContext.request.contextPath}/PatientHistory?patientId=${p.patientId}" class="btn btn-sm" style="background-color: #28a745; color: white; padding: 5px 10px; border-radius: 4px; text-decoration: none; font-size: 14px;">
                                                📁 Lịch sử
                                            </a>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </c:when>
                            <c:otherwise>
                                <tr>
                                    <td colspan="7" style="text-align: center; padding: 20px; color: #777;">Không tìm thấy bệnh nhân nào.</td>
                                </tr>
                            </c:otherwise>
                        </c:choose>
                    </tbody>
                </table>
            </div>

            <c:if test="${totalPages > 1 && empty keyword}">
                <div class="pagination" style="display: flex; justify-content: center; margin-top: 25px; gap: 8px;">
                    <c:if test="${currentPage > 1}">
                        <a href="${pageContext.request.contextPath}/PatientList?page=${currentPage - 1}" style="padding: 8px 12px; border: 1px solid #ddd; text-decoration: none; border-radius: 4px; color: #333; background-color: white;">
                            &laquo; Trước
                        </a>
                    </c:if>

                    <c:forEach begin="1" end="${totalPages}" var="i">
                        <a href="${pageContext.request.contextPath}/PatientList?page=${i}" style="padding: 8px 14px; border: 1px solid ${currentPage == i ? '#007bff' : '#ddd'}; background-color: ${currentPage == i ? '#007bff' : 'white'}; color: ${currentPage == i ? 'white' : '#333'}; text-decoration: none; border-radius: 4px; font-weight: ${currentPage == i ? 'bold' : 'normal'};">
                            ${i}
                        </a>
                    </c:forEach>

                    <c:if test="${currentPage < totalPages}">
                        <a href="${pageContext.request.contextPath}/PatientList?page=${currentPage + 1}" style="padding: 8px 12px; border: 1px solid #ddd; text-decoration: none; border-radius: 4px; color: #333; background-color: white;">
                            Sau &raquo;
                        </a>
                    </c:if>
                </div>
            </c:if>
        </div>
        <jsp:include page="footer.jsp"/>
        <script src="${pageContext.request.contextPath}/static/js/main.js"></script>
    </body>
</html>