document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-lab-import-form]').forEach((form) => {
        const record = form.querySelector('[data-lab-import-record]');
        const submit = form.querySelector('[data-lab-import-submit]');
        if (!record || !submit) return;

        const updateState = () => {
            submit.disabled = record.value === '';
        };

        record.addEventListener('change', updateState);
        updateState();
    });
});
