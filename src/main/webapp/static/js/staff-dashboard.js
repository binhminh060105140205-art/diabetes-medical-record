(function () {
    const workspace = document.querySelector('.staff-patient-workspace');
    const intake = document.getElementById('new-patient');
    if (!workspace || !intake) return;
    const intakeForm = intake.querySelector('.patient-intake-form');

    function resetIntakeState() {
        if (!intakeForm) return;
        intakeForm.classList.remove('is-submitting');
        intakeForm.style.pointerEvents = 'auto';
        intakeForm.querySelectorAll('input, select, textarea').forEach(function (field) {
            field.style.pointerEvents = 'auto';
        });
    }

    function openIntake(smooth) {
        intake.hidden = false;
        resetIntakeState();
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

    resetIntakeState();
    window.addEventListener('pageshow', resetIntakeState);
    intake.addEventListener('focusin', resetIntakeState);

    if (workspace.dataset.openIntake === 'true') intake.hidden = false;
    if (workspace.dataset.scrollIntake === 'true') {
        window.addEventListener('load', function () { openIntake(false); });
    }
}());
