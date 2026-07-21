<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<section class="card diabetes-care-path care-${diabetesProfile.carePathCode}">
    <div class="care-path-heading">
        <div>
            <span class="panel-eyebrow">LUỒNG ĐIỀU TRỊ THEO LOẠI BỆNH</span>
            <h2>${diabetesProfile.carePathTitle}</h2>
            <p>${diabetesProfile.carePathSummary}</p>
        </div>
        <span class="diabetes-type-badge">${diabetesProfile.diabetesTypeLabel}</span>
    </div>
    <ol class="care-path-steps">
        <c:forEach var="step" items="${diabetesProfile.carePathSteps}" varStatus="status">
            <li><span>${status.count}</span><p><c:out value="${step}"/></p></li>
        </c:forEach>
    </ol>
    <div class="care-path-difference">
        <c:choose>
            <c:when test="${diabetesProfile.diabetesType=='TYPE_1'}">
                <strong>Điểm khác biệt bắt buộc:</strong> phương pháp điều trị phải có insulin; hệ thống ưu tiên theo dõi hạ đường huyết và dấu hiệu liên quan ketone.
            </c:when>
            <c:when test="${diabetesProfile.diabetesType=='TYPE_2'}">
                <strong>Điểm khác biệt:</strong> có thể bắt đầu bằng lối sống hoặc thuốc, sau đó tăng bậc theo đánh giá; hệ thống theo dõi thêm cân nặng, huyết áp, thận và mỡ máu.
            </c:when>
            <c:otherwise>
                <strong>Chưa thể chọn luồng:</strong> bác sĩ cần xác nhận loại đái tháo đường trước khi áp dụng kế hoạch điều trị riêng.
            </c:otherwise>
        </c:choose>
    </div>
</section>