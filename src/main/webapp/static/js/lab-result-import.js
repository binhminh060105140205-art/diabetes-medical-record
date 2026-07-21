document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-lab-import-form]').forEach((form) => {
        const record = form.querySelector('[data-lab-import-record]');
        const file = form.querySelector('[data-lab-import-file]');
        const submit = form.querySelector('[data-lab-import-submit]');
        if (!record || !file || !submit) return;

        const updateState = () => {
            const hasRecord = record.value !== '';
            const hasFile = file.files && file.files.length > 0;
            submit.disabled = !(hasRecord && hasFile);
        };

        record.addEventListener('change', updateState);
        file.addEventListener('change', updateState);
        updateState();
    });
});
