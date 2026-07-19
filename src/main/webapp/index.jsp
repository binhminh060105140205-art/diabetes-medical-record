<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>DiaCare — Chăm sóc tiểu đường toàn diện</title>
    <meta name="description" content="Nền tảng quản lý khám ngoại trú và theo dõi bệnh tiểu đường dành cho phòng khám, bác sĩ và người bệnh.">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/home.css?v=20260717-perf1">
    <link rel="prefetch" href="${pageContext.request.contextPath}/Login">
    <link rel="prefetch" href="${pageContext.request.contextPath}/static/css/style.css?v=20260719-ai1" as="style">
</head>
<body class="home-body">
<div class="hosp-topbar">
    <div class="topbar-container">
        <div class="topbar-left"><span>Thứ 2–Thứ 7 · 07:30–17:30</span><span>Hà Nội</span></div>
        <div class="topbar-right"><span class="topbar-hotline">Hỗ trợ đặt lịch <strong>1800 1234</strong></span></div>
    </div>
</div>

<header class="hosp-nav">
    <div class="nav-container">
        <a class="hosp-brand" href="${pageContext.request.contextPath}/" aria-label="DiaCare - Trang chủ">
            <span class="brand-logo">+</span>
            <span class="brand-text"><span class="brand-name">DiaCare</span><span class="brand-sub">Chăm sóc tiểu đường toàn diện</span></span>
        </a>
        <nav class="nav-links" id="home-nav" aria-label="Điều hướng chính">
            <a class="active" href="#home">Trang chủ</a>
            <a href="#services">Dịch vụ</a>
            <a href="#workflow">Quy trình khám</a>
            <a href="#about">Về DiaCare</a>
        </nav>
        <div class="nav-actions">
            <c:choose>
                <c:when test="${not empty sessionScope.user}"><a class="home-btn home-btn-primary" href="${pageContext.request.contextPath}/Login">Vào hệ thống <span>→</span></a></c:when>
                <c:otherwise><a class="home-btn home-btn-primary" href="${pageContext.request.contextPath}/Login">Đăng nhập <span>→</span></a></c:otherwise>
            </c:choose>
            <button class="mobile-toggle" type="button" aria-label="Mở menu" aria-controls="home-nav" onclick="document.getElementById('home-nav').classList.toggle('active')">☰</button>
        </div>
    </div>
</header>

<main>
    <section class="hosp-hero" id="home">
        <div class="hero-orb hero-orb-one"></div><div class="hero-orb hero-orb-two"></div>
        <div class="hero-container">
            <div class="hero-copy">
                <span class="hero-badge"><i></i> Phòng khám chuyên khoa nội tiết</span>
                <h1>Chăm sóc chủ động.<br><span>Sống khỏe mỗi ngày.</span></h1>
                <p class="hero-desc">Kết nối lịch hẹn, lượt khám, xét nghiệm và hồ sơ sức khỏe trong một hành trình rõ ràng — để người bệnh an tâm và đội ngũ y tế phối hợp tốt hơn.</p>
                <div class="hero-buttons">
                    <a class="home-btn home-btn-primary home-btn-large" href="tel:18001234">Gọi hotline đặt lịch: 1800 1234 <span>→</span></a>
                    <a class="home-btn home-btn-ghost home-btn-large" href="${pageContext.request.contextPath}/Login">Đăng nhập hệ thống</a>
                </div>
                <div class="hero-trust">
                    <span><b>✓</b> Lịch hẹn rõ ràng</span><span><b>✓</b> Hồ sơ tập trung</span><span><b>✓</b> Theo dõi liên tục</span>
                </div>
            </div>

            <div class="hero-visual" aria-label="Minh họa quy trình chăm sóc DiaCare">
                <div class="visual-glow"></div>
                <div class="care-preview">
                    <div class="preview-head"><div><small>HÀNH TRÌNH HÔM NAY</small><strong>Xin chào, Minh Anh</strong></div><span class="preview-avatar">M</span></div>
                    <div class="preview-progress"><span class="done"></span><span class="active"></span><span></span><span></span></div>
                    <div class="preview-appointment">
                        <div class="preview-date"><strong>18</strong><small>THÁNG 07</small></div>
                        <div><small>LỊCH KHÁM SẮP TỚI</small><strong>09:00 · BS. Nguyễn Văn An</strong><p>Nội tiết · Phòng khám 02</p></div>
                    </div>
                    <div class="preview-metrics"><div><span>Đường huyết</span><strong>105 <small>mg/dL</small></strong><i class="metric-good">Ổn định</i></div><div><span>Huyết áp</span><strong>120/80</strong><i class="metric-neutral">Bình thường</i></div></div>
                </div>
                <div class="floating-chip chip-top"><span>✓</span><div><strong>Đã xác nhận</strong><small>Lịch khám của bạn</small></div></div>
                <div class="floating-chip chip-bottom"><span>↗</span><div><strong>Chỉ số ổn định</strong><small>Cập nhật hôm nay</small></div></div>
            </div>
        </div>
        <div class="hero-wave" aria-hidden="true"></div>
    </section>

    <section class="home-quick" aria-label="Truy cập nhanh">
        <a class="quick-pill" href="tel:18001234"><span class="quick-icon">01</span><div><strong>Gọi hotline</strong><small>Đặt lịch qua điện thoại hoặc đến trực tiếp</small></div><b>→</b></a>
        <a class="quick-pill quick-featured" href="${pageContext.request.contextPath}/Login"><span class="quick-icon">02</span><div><strong>Đăng nhập hệ thống</strong><small>Dành cho bệnh nhân đã có tài khoản</small></div><b>→</b></a>
        <a class="quick-pill" href="#workflow"><span class="quick-icon">03</span><div><strong>Xem quy trình</strong><small>6 bước khám liên thông</small></div><b>↓</b></a>
    </section>

    <section class="hosp-stats">
        <div class="stats-container">
            <div class="stats-intro"><span class="section-kicker">DIAcare TRONG MỘT NỀN TẢNG</span><h2>Dễ dùng cho cả phòng khám và người bệnh</h2></div>
            <div class="stat-box"><strong>4</strong><span>Vai trò phối hợp</span></div>
            <div class="stat-box"><strong>6</strong><span>Bước khám liên thông</span></div>
            <div class="stat-box"><strong>24/7</strong><span>Truy cập hồ sơ</span></div>
        </div>
    </section>

    <section class="hosp-section services-section" id="services">
        <div class="section-header"><span class="section-kicker">DỊCH VỤ CHUYÊN MÔN</span><h2 class="section-title">Một hệ thống, trọn hành trình chăm sóc</h2><p class="section-desc">Thông tin được kết nối xuyên suốt từ lúc đặt lịch đến theo dõi sau khám.</p></div>
        <div class="services-grid">
            <article class="service-card"><div class="srv-icon">⌚</div><span>01</span><h3>Lịch hẹn & hàng đợi</h3><p>Chủ động chọn giờ khám, check-in và theo dõi lượt chờ rõ ràng.</p></article>
            <article class="service-card service-accent"><div class="srv-icon">♡</div><span>02</span><h3>Khám nội tiết</h3><p>Sinh hiệu, triệu chứng, tiền sử và dị ứng được ghi nhận đầy đủ.</p></article>
            <article class="service-card"><div class="srv-icon">⌁</div><span>03</span><h3>Xét nghiệm</h3><p>Chỉ định và kết quả được liên kết trực tiếp với lượt khám.</p></article>
            <article class="service-card"><div class="srv-icon">✓</div><span>04</span><h3>Kế hoạch điều trị</h3><p>Chẩn đoán, đơn thuốc cơ bản và lịch tái khám được trình bày dễ hiểu.</p></article>
            <article class="service-card service-accent"><div class="srv-icon">▦</div><span>05</span><h3>Hồ sơ điện tử</h3><p>Lịch sử khám tập trung, đúng người và đúng vai trò sử dụng.</p></article>
            <article class="service-card"><div class="srv-icon">↗</div><span>06</span><h3>Theo dõi tại nhà</h3><p>Người bệnh cập nhật chỉ số và xem lại diễn tiến sức khỏe mỗi ngày.</p></article>
        </div>
    </section>

    <section class="hosp-section workflow-section" id="workflow">
        <div class="workflow-wrap">
            <div class="section-header section-header-left"><span class="section-kicker">QUY TRÌNH NGOẠI TRÚ</span><h2 class="section-title">Mỗi bước đều rõ ràng</h2><p class="section-desc">Luồng khám được tối giản để người bệnh biết mình đang ở đâu và cần làm gì tiếp theo.</p><a class="home-btn home-btn-primary" href="${pageContext.request.contextPath}/Login">Bắt đầu với DiaCare <span>→</span></a></div>
            <div class="workflow-steps">
                <div class="workflow-step"><span>01</span><div><h3>Đặt lịch</h3><p>Chọn bác sĩ và thời gian phù hợp.</p></div></div>
                <div class="workflow-step"><span>02</span><div><h3>Tiếp nhận</h3><p>Xác nhận lịch và cấp số hàng đợi.</p></div></div>
                <div class="workflow-step"><span>03</span><div><h3>Sàng lọc</h3><p>Đo sinh hiệu, ghi nhận tiền sử.</p></div></div>
                <div class="workflow-step"><span>04</span><div><h3>Bác sĩ khám</h3><p>Đánh giá và chỉ định cần thiết.</p></div></div>
                <div class="workflow-step"><span>05</span><div><h3>Xét nghiệm</h3><p>Cập nhật kết quả vào đúng hồ sơ.</p></div></div>
                <div class="workflow-step"><span>06</span><div><h3>Hoàn tất</h3><p>Kết luận, điều trị và hẹn tái khám.</p></div></div>
            </div>
        </div>
    </section>

    <section class="hosp-section about-section" id="about">
        <div class="about-panel">
            <div class="about-copy"><span class="section-kicker">VỀ DIAcare</span><h2>Dữ liệu rõ ràng.<br><em>Chăm sóc gần gũi.</em></h2><p>DiaCare giúp đội ngũ y tế dành ít thời gian hơn cho việc tìm kiếm thông tin và nhiều thời gian hơn để lắng nghe người bệnh.</p><div class="about-points"><span>Hồ sơ nhất quán</span><span>Quy trình dễ hiểu</span><span>Trải nghiệm thân thiện</span></div></div>
            <div class="about-contact"><small>CẦN HỖ TRỢ?</small><strong>1800 1234</strong><span>diabetesclinic@gmail.com</span><a class="home-btn home-btn-light" href="${pageContext.request.contextPath}/Login">Đăng nhập hệ thống <b>→</b></a></div>
        </div>
    </section>
</main>

<footer class="hosp-footer">
    <div class="footer-container">
        <div class="footer-brand"><div class="brand-logo">+</div><div><h3>DiaCare</h3><p>Hệ thống quản lý hồ sơ và điều hành khám bệnh tiểu đường dành cho phòng khám ngoại trú.</p></div></div>
        <div><h3>Truy cập nhanh</h3><ul><li><a href="#services">Dịch vụ</a></li><li><a href="#workflow">Quy trình khám</a></li><li><a href="${pageContext.request.contextPath}/Login">Đăng nhập</a></li></ul></div>
        <div><h3>Liên hệ</h3><ul><li>Hà Nội, Việt Nam</li><li>1800 1234</li><li>diabetesclinic@gmail.com</li></ul></div>
    </div>
    <div class="footer-bottom">© 2026 DiaCare · Chăm sóc tiểu đường toàn diện</div>
</footer>
</body>
</html>
