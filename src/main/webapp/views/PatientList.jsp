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
            <div class="workspace-heading"><div><span class="workspace-kicker">HỒ SƠ ĐIỀU TRỊ</span><h1>Danh sách bệnh nhân</h1><p>Tra cứu nhanh hồ sơ, bảo hiểm và lịch sử khám.</p></div></div>

            <div class="card patient-search-card">
                <form action="${pageContext.request.contextPath}/PatientList" method="get" class="compact-search">
                    <input type="search" name="keyword" value="${fn:escapeXml(keyword)}" maxlength="80" autocomplete="off" placeholder="Tên, số điện thoại, BHYT hoặc CCCD">
                    <button type="submit" class="btn btn-primary">Tìm kiếm</button>
                    <c:if test="${not empty keyword}">
                        <a href="${pageContext.request.contextPath}/PatientList" class="btn btn-light">Xóa lọc</a>
                    </c:if>
                </form>
            </div>

            <div class="card table-scroll">
                <table class="modern-table">
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
                                            <a href="${pageContext.request.contextPath}/PatientHistory?patientId=${p.patientId}" class="btn btn-sm history-button">
                                                Xem hồ sơ
                                            </a>
                                            <c:if test="${sessionScope.user.role=='DOCTOR'}"><a href="${pageContext.request.contextPath}/DoctorPatientJournal?patientId=${p.patientId}" class="btn btn-sm btn-outline-dark">Nhật ký sức khỏe</a></c:if>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </c:when>
                            <c:otherwise>
                                <tr>
                                    <td colspan="7" class="empty-state">Không tìm thấy bệnh nhân nào.</td>
                                </tr>
                            </c:otherwise>
                        </c:choose>
                    </tbody>
                </table>
            </div>

            <c:if test="${totalPages > 1 && empty keyword}">
                <div class="pagination">
                    <c:if test="${currentPage > 1}">
                        <a href="${pageContext.request.contextPath}/PatientList?page=${currentPage - 1}">
                            &laquo; Trước
                        </a>
                    </c:if>

                    <c:forEach begin="1" end="${totalPages}" var="i">
                        <a class="${currentPage == i ? 'active' : ''}" href="${pageContext.request.contextPath}/PatientList?page=${i}">
                            ${i}
                        </a>
                    </c:forEach>

                    <c:if test="${currentPage < totalPages}">
                        <a href="${pageContext.request.contextPath}/PatientList?page=${currentPage + 1}">
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
