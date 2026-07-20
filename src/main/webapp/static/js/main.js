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
        const filter = function () {
            const query = input.value.trim().toLocaleLowerCase('vi');
            let visible = 0;
            rows.forEach(function (row) {
                const match = !query || row.textContent.toLocaleLowerCase('vi').includes(query);
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
    if (!event.target.checkValidity()) return;
    event.target.classList.add('is-submitting');
    const submitter = event.submitter;
    if (submitter) {
        submitter.dataset.originalLabel = submitter.textContent;
        submitter.textContent = submitter.dataset.loadingLabel || 'Đang xử lý...';
    }
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
