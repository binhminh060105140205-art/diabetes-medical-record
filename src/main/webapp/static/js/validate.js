/* =====================================================
   validate.js — DiabetesMedicalRecord
   Validate client-side cho tất cả form
   ===================================================== */

// ── HELPER ──────────────────────────────────────────
function showErr(fieldId, msg) {
    const el = document.getElementById(fieldId) || document.querySelector('[name=' + fieldId + ']');
    if (!el) return;
    el.classList.add('input-error');
    let err = el.parentNode.querySelector('.err-msg');
    if (!err) {
        err = document.createElement('span');
        err.className = 'err-msg';
        el.parentNode.appendChild(err);
    }
    err.textContent = '⚠ ' + msg;
}

function clearErr(fieldId) {
    const el = document.getElementById(fieldId) || document.querySelector('[name=' + fieldId + ']');
    if (!el) return;
    el.classList.remove('input-error');
    const err = el.parentNode.querySelector('.err-msg');
    if (err) err.textContent = '';
}

function clearAllErrors() {
    document.querySelectorAll('.input-error').forEach(e => e.classList.remove('input-error'));
    document.querySelectorAll('.err-msg').forEach(e => e.textContent = '');
}

function val(name) {
    const el = document.querySelector('[name=' + name + ']');
    return el ? el.value.trim() : '';
}

function numVal(name) {
    return parseFloat(val(name)) || 0;
}

function inRange(name, min, max) {
    const v = numVal(name);
    return v >= min && v <= max;
}

// ── VALIDATE PATIENT FORM ─────────────────────────────
function validatePatientForm() {
    clearAllErrors();
    let ok = true;

    const fullName = val('fullName');
    if (fullName.length < 2) {
        showErr('fullName', 'Họ tên phải có ít nhất 2 ký tự');
        ok = false;
    }

    const phone = val('phone');
    if (!/^(0|\+84)[0-9]{9}$/.test(phone)) {
        showErr('phone', 'Số điện thoại không hợp lệ (VD: 0912345678)');
        ok = false;
    }

    const dob = val('dateOfBirth');
    if (dob) {
        const year = new Date(dob).getFullYear();
        if (year < 1900 || year > new Date().getFullYear() - 18) {
            showErr('dateOfBirth', 'Ngày sinh không hợp lệ (bệnh nhân phải từ 18 tuổi trở lên)');
            ok = false;
        }
    }

    const bhyt = val('healthInsuranceNo');
    if (bhyt && !/^[A-Z]{2}[0-9]{10}$/.test(bhyt)) {
        showErr('healthInsuranceNo', 'Số BHYT không đúng định dạng (VD: HC4012345678)');
        ok = false;
    }

    return ok;
}

// ── VALIDATE HEALTH INDICATORS ───────────────────────
function validateIndicators() {
    clearAllErrors();
    let ok = true;
    let warnings = [];

    // Chiều cao: 50–250 cm
    const h = numVal('height');
    if (h !== 0 && (h < 50 || h > 250)) {
        showErr('height', 'Chiều cao phải từ 50 – 250 cm');
        ok = false;
    }

    // Cân nặng: 10–300 kg
    const w = numVal('weight');
    if (w !== 0 && (w < 10 || w > 300)) {
        showErr('weight', 'Cân nặng phải từ 10 – 300 kg');
        ok = false;
    }

    // Huyết áp tâm thu: 60–250 mmHg
    const sbp = numVal('systolicBp');
    if (sbp !== 0 && (sbp < 60 || sbp > 250)) {
        showErr('systolicBp', 'Huyết áp tâm thu bất thường (60–250 mmHg)');
        ok = false;
    }

    // Huyết áp tâm trương: 40–150 mmHg
    const dbp = numVal('diastolicBp');
    if (dbp !== 0 && (dbp < 40 || dbp > 150)) {
        showErr('diastolicBp', 'Huyết áp tâm trương bất thường (40–150 mmHg)');
        ok = false;
    }

    // SBP phải > DBP
    if (sbp > 0 && dbp > 0 && sbp <= dbp) {
        showErr('systolicBp', 'Huyết áp tâm thu phải lớn hơn tâm trương');
        ok = false;
    }

    // Nhịp tim: 30–250 lần/phút
    const hr = numVal('heartRate');
    if (hr !== 0 && (hr < 30 || hr > 250)) {
        showErr('heartRate', 'Nhịp tim bất thường (30–250 lần/phút)');
        ok = false;
    }

    // Nhiệt độ: 34–42 °C
    const temp = numVal('temperature');
    if (temp !== 0 && (temp < 34 || temp > 42)) {
        showErr('temperature', 'Nhiệt độ bất thường (34–42 °C)');
        ok = false;
    }

    // Đường huyết: 20–600 mg/dL
    const bg = numVal('bloodGlucose');
    if (bg !== 0 && (bg < 20 || bg > 600)) {
        showErr('bloodGlucose', 'Đường huyết bất thường (20–600 mg/dL)');
        ok = false;
    }

    // HbA1c: 2–20%
    const hba1c = numVal('hba1c');
    if (hba1c !== 0 && (hba1c < 2 || hba1c > 20)) {
        showErr('hba1c', 'HbA1c bất thường (2–20%)');
        ok = false;
    }

    // Cholesterol: 50–700 mg/dL
    const chol = numVal('cholesterol');
    if (chol !== 0 && (chol < 50 || chol > 700)) {
        showErr('cholesterol', 'Cholesterol bất thường (50–700 mg/dL)');
        ok = false;
    }

    // Triglyceride: 20–2000 mg/dL
    const trig = numVal('triglyceride');
    if (trig !== 0 && (trig < 20 || trig > 2000)) {
        showErr('triglyceride', 'Triglyceride bất thường (20–2000 mg/dL)');
        ok = false;
    }

    // HDL-C: 10–150 mg/dL
    const hdl = numVal('hdlC');
    if (hdl !== 0 && (hdl < 10 || hdl > 150)) {
        showErr('hdlC', 'HDL-C bất thường (10–150 mg/dL)');
        ok = false;
    }

    // LDL-C: 10–400 mg/dL
    const ldl = numVal('ldlC');
    if (ldl !== 0 && (ldl < 10 || ldl > 400)) {
        showErr('ldlC', 'LDL-C bất thường (10–400 mg/dL)');
        ok = false;
    }

    // Cảnh báo mềm (warning, không block submit)
    if (bg > 300) warnings.push('⚠ Đường huyết rất cao (' + bg + ' mg/dL) — kiểm tra lại');
    if (hba1c > 12) warnings.push('⚠ HbA1c rất cao (' + hba1c + '%) — kiểm tra lại');
    if (sbp > 180) warnings.push('⚠ Huyết áp tâm thu rất cao (' + sbp + ' mmHg)');
    if (h > 0 && w > 0) {
        const bmi = w / ((h/100)*(h/100));
        if (bmi > 45) warnings.push('⚠ BMI rất cao (' + bmi.toFixed(1) + ') — kiểm tra lại cân nặng/chiều cao');
    }

    if (warnings.length > 0 && ok) {
        const box = document.getElementById('warningBox');
        if (box) {
            box.innerHTML = warnings.map(w => '<p>' + w + '</p>').join('');
            box.style.display = 'block';
        }
        // Không block, chỉ cảnh báo
    }

    return ok;
}

// ── VALIDATE LOGIN FORM ──────────────────────────────
function validateLogin() {
    clearAllErrors();
    let ok = true;
    if (val('username').length < 2) {
        showErr('username', 'Vui lòng nhập tên đăng nhập');
        ok = false;
    }
    if (val('password').length < 4) {
        showErr('password', 'Mật khẩu phải có ít nhất 4 ký tự');
        ok = false;
    }
    return ok;
}

// ── VALIDATE CREATE USER (Admin) ────────────────────
function validateCreateUser() {
    clearAllErrors();
    let ok = true;

    if (val('username').length < 3) {
        showErr('username', 'Tên đăng nhập tối thiểu 3 ký tự');
        ok = false;
    }
    if (!/^[a-zA-Z0-9_]+$/.test(val('username'))) {
        showErr('username', 'Chỉ dùng chữ, số và dấu gạch dưới');
        ok = false;
    }
    if (val('password').length < 6) {
        showErr('password', 'Mật khẩu tối thiểu 6 ký tự');
        ok = false;
    }
    if (val('fullName').length < 2) {
        showErr('fullName', 'Họ tên không được để trống');
        ok = false;
    }
    return ok;
}

// ── VALIDATE BASIC RECORD (Tab 1) ───────────────────
function validateBasicRecord() {
    clearAllErrors();
    let ok = true;
    if (val('doctorId') === '' || val('doctorId') === '0') {
        showErr('doctorId', 'Vui lòng chọn bác sĩ phụ trách');
        ok = false;
    }
    if (val('reasonForVisit').length < 5) {
        showErr('reasonForVisit', 'Vui lòng nhập lý do khám (tối thiểu 5 ký tự)');
        ok = false;
    }
    return ok;
}

// ── VALIDATE CONCLUSION (Tab 4) ─────────────────────
function validateConclusion() {
    clearAllErrors();
    let ok = true;
    if (val('finalDiagnosis').length < 5) {
        showErr('finalDiagnosis', 'Vui lòng nhập chẩn đoán (tối thiểu 5 ký tự)');
        ok = false;
    }
    const fup = val('followUpDate');
    if (fup) {
        const today = new Date(); today.setHours(0,0,0,0);
        const fupDate = new Date(fup);
        if (fupDate <= today) {
            showErr('followUpDate', 'Ngày tái khám phải sau ngày hôm nay');
            ok = false;
        }
    }
    return ok;
}

// ── REAL-TIME VALIDATION (bind on input) ────────────
document.addEventListener('DOMContentLoaded', function () {

    // Real-time error clear khi user sửa
    document.querySelectorAll('.form-control').forEach(function(el) {
        el.addEventListener('input', function() {
            this.classList.remove('input-error');
            const err = this.parentNode.querySelector('.err-msg');
            if (err) err.textContent = '';
        });
        el.addEventListener('change', function() {
            this.classList.remove('input-error');
            const err = this.parentNode.querySelector('.err-msg');
            if (err) err.textContent = '';
        });
    });

    // Bind submit validators tự động theo form action
    const form = document.querySelector('form[data-validate]');
    if (form) {
        const type = form.getAttribute('data-validate');
        form.addEventListener('submit', function(e) {
            let valid = true;
            if (type === 'patient')     valid = validatePatientForm();
            if (type === 'indicators')  valid = validateIndicators();
            if (type === 'login')       valid = validateLogin();
            if (type === 'createUser')  valid = validateCreateUser();
            if (type === 'basicRecord') valid = validateBasicRecord();
            if (type === 'conclusion')  valid = validateConclusion();
            if (!valid) {
                e.preventDefault();
                // Scroll đến lỗi đầu tiên
                const firstErr = document.querySelector('.input-error');
                if (firstErr) firstErr.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }
        });
    }

    // Bind nhiều form trong cùng trang (MedicalRecordForm)
    document.querySelectorAll('form[data-validate]').forEach(function(f) {
        const type = f.getAttribute('data-validate');
        f.addEventListener('submit', function(e) {
            let valid = true;
            if (type === 'patient')     valid = validatePatientForm();
            if (type === 'indicators')  valid = validateIndicators();
            if (type === 'login')       valid = validateLogin();
            if (type === 'createUser')  valid = validateCreateUser();
            if (type === 'basicRecord') valid = validateBasicRecord();
            if (type === 'conclusion')  valid = validateConclusion();
            if (!valid) {
                e.preventDefault();
                const firstErr = document.querySelector('.input-error');
                if (firstErr) firstErr.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }
        });
    });
});
