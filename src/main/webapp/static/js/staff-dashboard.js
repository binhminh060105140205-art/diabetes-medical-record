(function () {
    const workspace = document.querySelector('.staff-patient-workspace');
    const intake = document.getElementById('new-patient');
    if (!workspace || !intake) return;

    function openIntake(smooth) {
        intake.hidden = false;
        intake.scrollIntoView({ behavior: smooth ? 'smooth' : 'auto', block: 'start' });
        const firstField = intake.querySelector('input,select,textarea');
        if (firstField) firstField.focus({ preventScroll: true });
    }

    // Nút "Tiếp nhận bệnh nhân mới" chỉ mở form và đưa con trỏ vào ô đầu tiên.
    // Khi nhân viên bấm nút tạo, form mới gửi sang /PatientForm để tạo tài khoản và hồ sơ.
    document.querySelectorAll('[data-open-intake]').forEach(function (button) {
        button.addEventListener('click', function (event) {
            event.preventDefault();
            openIntake(true);
        });
    });

    document.querySelectorAll('[data-close-intake]').forEach(function (button) {
        button.addEventListener('click', function () {
            intake.hidden = true;
        });
    });

    if (workspace.dataset.openIntake === 'true') intake.hidden = false;
    if (workspace.dataset.scrollIntake === 'true') {
        window.addEventListener('load', function () { openIntake(false); });
    }
}());
