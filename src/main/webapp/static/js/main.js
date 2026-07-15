// ================================================

// Add the session CSRF token to every same-origin form and AJAX mutation.
(function () {
    const meta = document.querySelector('meta[name="csrf-token"]');
    const token = meta && meta.content;
    if (!token) return;
    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('form').forEach(function (form) {
            if ((form.method || 'get').toLowerCase() !== 'post' || form.querySelector('[name="_csrf"]')) return;
            const input = document.createElement('input');
            input.type = 'hidden'; input.name = '_csrf'; input.value = token;
            form.appendChild(input);
        });
    });
    const originalFetch = window.fetch;
    window.fetch = function (input, init) {
        init = init || {};
        const method = (init.method || 'GET').toUpperCase();
        const url = typeof input === 'string' ? input : input.url;
        if (!['GET', 'HEAD', 'OPTIONS'].includes(method) && new URL(url, location.href).origin === location.origin) {
            const headers = new Headers(init.headers || {});
            headers.set('X-CSRF-Token', token);
            init.headers = headers;
        }
        return originalFetch.call(window, input, init);
    };
})();
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

// Immediate feedback while a server-rendered page is loading.
document.addEventListener('click', function (event) {
    const link = event.target.closest('a[href]');
    if (!link || link.target === '_blank' || event.ctrlKey || event.metaKey || event.shiftKey) return;
    const href = link.getAttribute('href');
    if (!href || href.startsWith('#') || href.startsWith('javascript:')) return;
    document.body.classList.add('is-navigating');
});
window.addEventListener('pageshow', function () { document.body.classList.remove('is-navigating'); });

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
