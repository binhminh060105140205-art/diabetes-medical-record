if (!window.__diaCareMainLoaded) {
window.__diaCareMainLoaded = true;

// ================================================

//  main.js — DiabetesMedicalRecord
// ================================================

// Auto-calculate BMI when height/weight change
document.addEventListener('DOMContentLoaded', function () {
    const heightInput = document.querySelector('[name=height]');
    const weightInput = document.querySelector('[name=weight]');
    const bmiDisplay  = document.getElementById('bmiDisplay');

    function calcBMI() {
        if (!heightInput || !weightInput || !bmiDisplay) return;
        const h = parseFloat(heightInput.value);
        const w = parseFloat(weightInput.value);
        if (h > 0 && w > 0) {
            const bmi = w / ((h / 100) * (h / 100));
            bmiDisplay.value = bmi.toFixed(1);
            // Color coding
            if (bmi >= 30)       bmiDisplay.style.color = '#dc3545';
            else if (bmi >= 25)  bmiDisplay.style.color = '#fd7e14';
            else                 bmiDisplay.style.color = '#198754';
        }
    }

    if (heightInput) heightInput.addEventListener('input', calcBMI);
    if (weightInput) weightInput.addEventListener('input', calcBMI);
});

// Replace browser-dependent English validation bubbles with consistent
// Vietnamese messages shown next to the field on every form.
function validationFieldLabel(control) {
    const fieldset = control.closest('fieldset');
    const legend = fieldset ? fieldset.querySelector('legend') : null;
    const formGroup = control.closest('.form-group');
    const explicitLabel = formGroup && control.id
        ? formGroup.querySelector('label[for="' + control.id + '"]') : null;
    const label = explicitLabel || (formGroup ? formGroup.querySelector('label') : null);
    const text = (label ? label.textContent : legend ? legend.textContent : 'thông tin')
        .replace('*', '').replace(/^\s*\d+\.\s*/, '').trim();
    return text.charAt(0).toLowerCase() + text.slice(1);
}

function vietnameseValidationMessage(control) {
    const label = validationFieldLabel(control);
    const validity = control.validity;
    if (validity.customError && control.dataset.validationMessage) {
        return control.dataset.validationMessage;
    }
    if (validity.valueMissing) {
        const action = control.tagName === 'SELECT' || control.type === 'radio'
            || control.type === 'checkbox' ? 'chọn' : 'nhập';
        return 'Vui lòng ' + action + ' ' + label + '.';
    }
    if (validity.typeMismatch) return label.includes('email') || label.includes('gmail')
        ? 'Email không đúng định dạng.' : 'Giá trị không đúng định dạng.';
    if (validity.tooShort) return 'Vui lòng nhập ít nhất ' + control.minLength + ' ký tự.';
    if (validity.tooLong) return 'Nội dung vượt quá ' + control.maxLength + ' ký tự.';
    if (validity.rangeUnderflow) return 'Giá trị nhỏ nhất cho phép là ' + control.min + '.';
    if (validity.rangeOverflow) return 'Giá trị lớn nhất cho phép là ' + control.max + '.';
    if (validity.stepMismatch) return 'Vui lòng chọn giá trị theo đúng khoảng cho phép.';
    if (validity.patternMismatch) return 'Giá trị không đúng định dạng yêu cầu.';
    if (validity.badInput) return 'Vui lòng nhập giá trị hợp lệ.';
    return 'Vui lòng kiểm tra lại ' + label + '.';
}

function showNativeValidationError(control) {
    const container = control.closest('.form-group, fieldset') || control.parentElement;
    if (!container) return;
    control.classList.add('input-error');
    let error = container.querySelector('.err-msg');
    if (!error) {
        error = document.createElement('span');
        error.className = 'err-msg';
        container.appendChild(error);
    }
    error.textContent = '⚠ ' + vietnameseValidationMessage(control);
}

document.addEventListener('invalid', function (event) {
    event.preventDefault();
    showNativeValidationError(event.target);
}, true);

document.addEventListener('input', function (event) {
    if (!event.target.matches('input, select, textarea')) return;
    event.target.classList.remove('input-error');
    const container = event.target.closest('.form-group, fieldset') || event.target.parentElement;
    const error = container ? container.querySelector('.err-msg') : null;
    if (error) error.textContent = '';
});

document.addEventListener('change', function (event) {
    if (!event.target.matches('input, select, textarea')) return;
    event.target.dispatchEvent(new Event('input', { bubbles: true }));
});

function refreshAppointmentTimeOptions(form) {
    const dateControl = form.querySelector('[name=appointmentDate]');
    const timeControl = form.querySelector('[name=appointmentTime]');
    if (!dateControl || !timeControl || !dateControl.value) return;
    const now = new Date();
    const minimum = new Date(now.getTime() + 15 * 60 * 1000);
    const today = now.getFullYear() + '-' + String(now.getMonth() + 1).padStart(2, '0')
        + '-' + String(now.getDate()).padStart(2, '0');
    Array.from(timeControl.options).forEach(function (option) {
        if (!option.value) return;
        const candidate = new Date(dateControl.value + 'T' + option.value + ':00');
        option.disabled = dateControl.value === today && candidate <= minimum;
    });
    if (timeControl.selectedOptions.length && timeControl.selectedOptions[0].disabled) {
        timeControl.value = '';
    }
}

function refreshAppointmentDateTimeValidity(control) {
    control.setCustomValidity('');
    delete control.dataset.validationMessage;
    if (!control.value) return;

    const value = new Date(control.value);
    const minutes = value.getHours() * 60 + value.getMinutes();
    let message = '';
    if (value.getDay() === 0) {
        message = 'Phòng khám nghỉ Chủ nhật.';
    } else if (value.getMinutes() % 30 !== 0) {
        message = 'Giờ khám phải theo khung 30 phút.';
    } else if (!((minutes >= 450 && minutes <= 690)
            || (minutes >= 780 && minutes <= 1020))) {
        message = 'Chỉ nhận lịch từ 07:30–11:30 hoặc 13:00–17:00.';
    }
    if (message) {
        control.dataset.validationMessage = message;
        control.setCustomValidity(message);
    }
}

function refreshRequestedDateValidity(control) {
    if (!control) return;
    control.setCustomValidity('');
    const error = control.closest('.form-group')
        ? control.closest('.form-group').querySelector('[data-date-validation]') : null;
    if (error) {
        error.textContent = '';
        error.hidden = true;
    }
    if (!control.value) return;

    const selected = new Date(control.value + 'T00:00:00');
    if (selected.getDay() !== 0) return;

    const message = 'Phòng khám nghỉ Chủ nhật.';
    control.setCustomValidity(message);
    if (error) {
        error.textContent = message;
        error.hidden = false;
    }
}

function normalizedSearchText(value) {
    return (value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '')
        .toLocaleLowerCase('vi').trim();
}

document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('[data-select-filter]').forEach(function (input) {
        const select = document.getElementById(input.dataset.selectFilter);
        if (!select) return;
        const status = document.querySelector(
            '[data-select-filter-status="' + input.dataset.selectFilter + '"]');
        const refresh = function () {
            const keyword = normalizedSearchText(input.value);
            let visible = 0;
            Array.from(select.options).forEach(function (option) {
                if (!option.value) return;
                const matches = !keyword || normalizedSearchText(option.textContent).includes(keyword);
                option.hidden = !matches;
                option.disabled = !matches;
                if (matches) visible++;
            });
            if (status) status.textContent = keyword
                ? 'Tìm thấy ' + visible + ' bệnh nhân phù hợp.'
                : 'Gõ để thu hẹp danh sách bệnh nhân.';
            if (select.selectedOptions.length && select.selectedOptions[0].disabled) {
                select.value = '';
                select.dispatchEvent(new Event('change', { bubbles: true }));
            }
        };
        input.addEventListener('input', refresh);
        refresh();
    });

    document.querySelectorAll('input[type="datetime-local"][name="appointmentAt"]')
        .forEach(function (control) {
            const refresh = function () { refreshAppointmentDateTimeValidity(control); };
            control.addEventListener('input', refresh);
            control.addEventListener('change', refresh);
            refresh();
        });
});

document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('form[data-appointment-form]').forEach(function (form) {
        refreshAppointmentTimeOptions(form);
        const dateControl = form.querySelector('[name=appointmentDate]');
        if (dateControl) dateControl.addEventListener('change', function () {
            refreshAppointmentTimeOptions(form);
        });
    });
});

// Keep staged workflow actions unavailable until every required prerequisite is valid.
document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('form[data-gated-submit]').forEach(function (form) {
        const submits = Array.from(form.querySelectorAll('button[type=submit],input[type=submit]'));
        const requestedDate = form.querySelector('[name=preferredDate]');
        const refresh = function () {
            refreshRequestedDateValidity(requestedDate);
            const required = Array.from(form.querySelectorAll('[required]'));
            const complete = required.every(function (control) {
                if (control.type === 'radio' || control.type === 'checkbox') {
                    const group = form.elements[control.name];
                    if (group && typeof group.length === 'number') {
                        return Array.from(group).some(function (item) { return item.checked; });
                    }
                    return control.checked;
                }
                return control.value.trim() !== '' && control.validity.valid;
            });
            submits.forEach(function (submit) { submit.disabled = !complete; });
        };
        form.addEventListener('input', refresh);
        form.addEventListener('change', refresh);
        refresh();
    });
});

// Accessible sidebar on tablet/mobile.
document.addEventListener('DOMContentLoaded', function () {
    const toggle = document.querySelector('.sidebar-toggle');
    const backdrop = document.querySelector('.sidebar-backdrop');
    const sidebar = document.getElementById('app-sidebar');
    if (!toggle || !sidebar) return;
    const close = function () {
        document.body.classList.remove('sidebar-open');
        toggle.setAttribute('aria-expanded', 'false');
    };
    toggle.addEventListener('click', function () {
        const open = document.body.classList.toggle('sidebar-open');
        toggle.setAttribute('aria-expanded', String(open));
    });
    if (backdrop) backdrop.addEventListener('click', close);
    sidebar.querySelectorAll('a').forEach(function (link) { link.addEventListener('click', close); });
    document.addEventListener('keydown', function (event) { if (event.key === 'Escape') close(); });

    const activeLink = sidebar.querySelector('a.active');
    if (activeLink) activeLink.scrollIntoView({ block: 'nearest' });
});

// Lightweight client-side filtering for operational tables. Server-side search
// remains the source of truth on paginated screens.
document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('[data-table-filter]').forEach(function (input) {
        const table = document.getElementById(input.dataset.tableFilter);
        if (!table) return;
        const rows = Array.from(table.querySelectorAll('tbody tr[data-search-row]'));
        const empty = document.querySelector('[data-filter-empty="' + input.dataset.tableFilter + '"]');
        const searchText = function (value) {
            return String(value || '').toLocaleLowerCase('vi').normalize('NFD')
                .replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd');
        };
        const filter = function () {
            const query = searchText(input.value.trim());
            let visible = 0;
            rows.forEach(function (row) {
                const match = !query || searchText(row.textContent).includes(query);
                row.hidden = !match;
                if (match) visible += 1;
            });
            if (empty) empty.hidden = visible !== 0;
        };
        input.addEventListener('input', filter);
    });
});

// Make long forms explicit about the action currently being processed without
// disabling named submit buttons that carry status values to the server.
document.addEventListener('submit', function (event) {
    // Chờ các bộ validate khác chạy xong để không khóa form khi submit bị hủy.
    if (event.defaultPrevented || !event.target.checkValidity()) return;
    const form = event.target;
    const submitter = event.submitter;
    window.setTimeout(function () {
        if (event.defaultPrevented || !form.checkValidity()) return;
        form.classList.add('is-submitting');
        if (submitter) {
            submitter.dataset.originalLabel = submitter.textContent;
            submitter.textContent = submitter.dataset.loadingLabel || 'Đang xử lý...';
        }
    }, 0);
});

// Show feedback only when a request is genuinely slow. Normal 150-300 ms
// navigation should feel instant instead of flashing a misleading progress bar.
let navigationFeedbackTimer;
document.addEventListener('click', function (event) {
    const link = event.target.closest('a[href]');
    if (!link || link.target === '_blank' || event.ctrlKey || event.metaKey || event.shiftKey) return;
    const href = link.getAttribute('href');
    if (!href || href.startsWith('#') || href.startsWith('javascript:')) return;
    clearTimeout(navigationFeedbackTimer);
    navigationFeedbackTimer = setTimeout(function () {
        document.body.classList.add('is-navigating');
    }, 400);
});
window.addEventListener('pageshow', function () {
    clearTimeout(navigationFeedbackTimer);
    document.body.classList.remove('is-navigating');
});

// Tab switching (for MedicalRecordForm)
function showTab(n) {
    document.querySelectorAll('.tab-panel').forEach(function(p) {
        p.classList.remove('active');
    });
    document.querySelectorAll('.tab-btn').forEach(function(b) {
        b.classList.remove('active');
    });
    const panel = document.getElementById('tab' + n);
    const btn   = document.querySelectorAll('.tab-btn')[n - 1];
    if (panel) panel.classList.add('active');
    if (btn)   btn.classList.add('active');
    // Persist tab in URL without reload
    const url = new URL(window.location.href);
    url.searchParams.set('tab', n);
    window.history.replaceState({}, '', url);
}

// Confirm before dangerous action
function confirmAction(msg) {
    return confirm(msg || 'Bạn có chắc chắn không?');
}

// Highlight active nav link
document.addEventListener('DOMContentLoaded', function () {
    const path = window.location.pathname;
    document.querySelectorAll('.topnav a').forEach(function (a) {
        if (a.getAttribute('href') && path.includes(a.getAttribute('href').replace(/.*\//, ''))) {
            a.classList.add('active');
        }
    });
});

// Auto-dismiss alerts after 5s
document.addEventListener('DOMContentLoaded', function () {
    const alerts = document.querySelectorAll('.alert');
    alerts.forEach(function (el) {
        setTimeout(function () {
            el.style.transition = 'opacity 0.6s';
            el.style.opacity = '0';
            setTimeout(function () { el.style.display = 'none'; }, 600);
        }, 5000);
    });
});

}
