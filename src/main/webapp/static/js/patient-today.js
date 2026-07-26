(function () {
    const script = document.currentScript;
    const contextPath = script ? new URL(script.src).pathname.split('/static/js/')[0] : '';
    const byId = function (id) { return document.getElementById(id); };
    const symptomInputs = Array.from(document.querySelectorAll('[name=symptom]'));
    const adviceGroups = [
        { key: 'monitoring', title: 'Theo dõi chỉ số', marker: String.fromCodePoint(129658), prefixes: ['THEO_DOI'],
            keywords: ['duong huyet', 'huyet ap', 'chi so', 'hba1c', 'theo doi', 'ghi lai ket qua', 'thoi diem do'] },
        { key: 'treatment', title: 'Thuốc và insulin', marker: String.fromCodePoint(128138), prefixes: ['DIEU_TRI'],
            keywords: ['insulin', 'thuoc', 'dieu tri', 'lieu dung', 'mui tiem'] },
        { key: 'nutrition', title: 'Ăn uống và dinh dưỡng', marker: String.fromCodePoint(129337), prefixes: ['AN_UONG'],
            keywords: ['an dung bua', 'bua an', 'thuc pham', 'rau', 'ca', 'thit nac', 'dau phu', 'nuoc ngot', 'tra sua', 'tinh bot'] },
        { key: 'care', title: 'Vận động và chăm sóc', marker: String.fromCodePoint(128616), prefixes: ['VAN_DONG', 'CHAM_SOC'],
            keywords: ['van dong', 'di bo', 'nghi ngoi', 'ban chan', 'ngu du', 'nguoi than', 'da sach'] },
        { key: 'contact', title: 'Khi cần liên hệ bác sĩ', marker: String.fromCodePoint(128680), prefixes: ['LIEN_HE'],
            keywords: ['lien he', 'phong kham', 'tai kham', 'nhan vien y te', 'hoi bac si'] }
    ];

    restoreSymptoms();
    bindSymptomRules();
    if (byId('saveLogBtn')) byId('saveLogBtn').addEventListener('click', saveLog);
    if (byId('aiAdviceButton')) byId('aiAdviceButton').addEventListener('click', loadPatientAdvice);
    window.saveLog = saveLog;
    window.loadPatientAdvice = loadPatientAdvice;

    function restoreSymptoms() {
        const saved = byId('savedSymptoms');
        const values = new Set((saved ? saved.textContent : '').split(',')
                .map(function (value) { return value.trim(); }).filter(Boolean));
        symptomInputs.forEach(function (input) { input.checked = values.has(input.value); });
    }

    function bindSymptomRules() {
        const none = symptomInputs.find(function (input) {
            return input.value === 'Không có triệu chứng';
        });
        symptomInputs.forEach(function (input) {
            input.addEventListener('change', function () {
                if (input === none && input.checked) {
                    symptomInputs.forEach(function (item) {
                        if (item !== none) item.checked = false;
                    });
                } else if (input.checked && none) {
                    none.checked = false;
                }
            });
        });
    }

    // Nút "Lưu chỉ số hôm nay": kiểm tra dữ liệu trên trang rồi POST /PatientHealth.
    // Controller lưu nhật ký và trả JSON để hiện kết quả ngay dưới nút, không tải lại trang.
    async function saveLog() {
        const button = byId('saveLogBtn');
        const fields = healthFields();
        const symptoms = symptomInputs.filter(function (input) { return input.checked; })
                .map(function (input) { return input.value; }).join(', ');
        showLogResult('');
        if (!validateHealthFields(fields, symptoms)) return;

        button.disabled = true;
        showLogResult('Đang lưu chỉ số...', 'loading');
        try {
            const body = new URLSearchParams({
                action: 'saveLog', mealType: fields.meal.value,
                bloodGlucose: fields.glucose.value, systolicBp: fields.systolic.value,
                diastolicBp: fields.diastolic.value, heartRate: fields.heartRate.value,
                weight: fields.weight.value,
                symptoms: symptoms, note: fields.note.value
            });
            const response = await fetch(contextPath + '/PatientHealth', { method: 'POST', body: body });
            const data = await response.json();
            showLogResult(data.success ? 'Đã lưu chỉ số hôm nay.'
                    : (data.error || 'Không thể lưu dữ liệu.'), data.success ? 'success' : 'error');
        } catch (error) {
            showLogResult('Không thể kết nối máy chủ.', 'error');
        } finally {
            button.disabled = false;
        }
    }

    function healthFields() {
        return {
            meal: byId('log_meal'), glucose: byId('log_bg'), systolic: byId('log_sbp'),
            diastolic: byId('log_dbp'), heartRate: byId('log_hr'),
            weight: byId('log_weight'), note: byId('log_note')
        };
    }

    function validateHealthFields(fields, symptoms) {
        const measured = [fields.glucose, fields.systolic, fields.diastolic,
            fields.heartRate, fields.weight];
        const invalid = [fields.meal].concat(measured)
                .find(function (field) { return field.value && !field.checkValidity(); });
        if (invalid) return rejectLog('Giá trị vừa nhập chưa hợp lệ. Vui lòng kiểm tra lại giới hạn của chỉ số.', invalid);
        if (fields.glucose.value && !fields.meal.value) return rejectLog('Vui lòng chọn thời điểm đo đường huyết.', fields.meal);
        if (fields.meal.value && !fields.glucose.value) return rejectLog('Đã chọn thời điểm đo thì cần nhập đường huyết.', fields.glucose);
        if (Boolean(fields.systolic.value) !== Boolean(fields.diastolic.value)) {
            return rejectLog('Huyết áp cần nhập đủ cả tâm thu và tâm trương.',
                    fields.systolic.value ? fields.diastolic : fields.systolic);
        }
        if (!measured.some(function (field) { return field.value; }) && !symptoms && !fields.note.value.trim()) {
            return rejectLog('Cần nhập ít nhất một chỉ số hoặc ghi chú.');
        }
        return true;
    }

    function rejectLog(message, field) {
        showLogResult(message, 'error');
        if (field) {
            field.focus();
            if (!field.checkValidity()) field.reportValidity();
        }
        return false;
    }

    function showLogResult(message, type) {
        const result = byId('logResult');
        result.textContent = message;
        result.className = 'daily-log-message' + (type ? ' is-' + type : '');
        result.hidden = !message;
    }

    // Nút "Tạo lời khuyên chi tiết": gửi xác nhận đồng ý tới controller AI.
    // Service dùng cache trong ngày, thử OpenAI khi có cấu hình và luôn ghép luật an toàn trước khi trả về.
    async function loadPatientAdvice() {
        const consent = byId('aiConsent');
        const button = byId('aiAdviceButton');
        const view = adviceView();
        resetAdvice(view);
        if (!consent.checked) {
            view.box.hidden = false;
            view.summary.textContent = 'Vui lòng đọc và đánh dấu đồng ý trước khi tiếp tục.';
            return;
        }

        button.disabled = true;
        button.textContent = 'Đang phân tích...';
        view.box.hidden = false;
        view.summary.textContent = 'Đang phân tích chỉ số và xây dựng hướng dẫn phù hợp cho hôm nay...';
        try {
            const response = await fetch(contextPath + '/api/patient/ai-advice', {
                method: 'POST', headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ consent: true })
            });
            const data = await response.json();
            if (!response.ok) throw new Error(data.error || 'Không thể tạo lời khuyên.');
            renderAdvice(data, view);
        } catch (error) {
            view.summary.textContent = error.message;
            view.items.replaceChildren();
        } finally {
            button.disabled = false;
            button.textContent = '\u2728 Tạo lại lời khuyên chi tiết';
        }
    }

    function adviceView() {
        return {
            box: byId('aiAdviceResult'), summary: byId('aiAdviceSummary'),
            items: byId('aiAdviceItems'), severity: byId('aiAdviceSeverity'),
            doctor: byId('aiAdviceDoctor'), source: byId('aiAdviceSource')
        };
    }

    function resetAdvice(view) {
        view.severity.textContent = '';
        view.severity.className = 'ai-severity';
        view.doctor.hidden = true;
        view.doctor.textContent = '';
        view.source.textContent = '';
        view.items.replaceChildren();
    }

    function renderAdvice(data, view) {
        const severityLabels = { high: 'Nên liên hệ bác sĩ', medium: 'Cần chú ý', low: 'Ổn định' };
        view.summary.textContent = data.summary;
        view.severity.textContent = severityLabels[data.severity] || 'Ổn định';
        view.severity.className = 'ai-severity ai-severity-' + data.severity;
        renderAdviceGroups(data.advice, view.items);
        view.doctor.hidden = !data.doctorRecommendation;
        view.doctor.textContent = data.doctorRecommendation
                ? 'Nếu cảm thấy không khỏe hoặc triệu chứng tiếp diễn, hãy liên hệ bác sĩ/phòng khám.' : '';
        view.source.textContent = (data.source === 'OPENAI'
                ? 'Lời khuyên từ hệ thống phân tích tự động'
                : 'Lời khuyên từ bộ quy tắc an toàn nội bộ') + (data.cached ? ' · đã lưu trong ngày' : '');
    }

    function renderAdviceGroups(values, container) {
        const grouped = new Map(adviceGroups.map(function (group) { return [group.key, []]; }));
        (Array.isArray(values) ? values : []).forEach(function (value) {
            const parsed = splitAdvice(value);
            if (!parsed.text) return;
            const group = parsed.group || adviceGroups.slice(0, 4).reduce(function (smallest, current) {
                return grouped.get(current.key).length < grouped.get(smallest.key).length ? current : smallest;
            });
            grouped.get(group.key).push(parsed.text);
        });
        const sections = adviceGroups.filter(function (group) { return grouped.get(group.key).length; })
                .map(function (group) { return createAdviceSection(group, grouped.get(group.key)); });
        container.replaceChildren.apply(container, sections);
    }

    function splitAdvice(value) {
        const text = String(value || '').trim();
        const prefix = text.match(/^\[(THEO_DOI|DIEU_TRI|AN_UONG|VAN_DONG|CHAM_SOC|LIEN_HE)]\s*/i);
        if (prefix) {
            const code = prefix[1].toUpperCase();
            return { text: text.slice(prefix[0].length).replace(/^\s*[:\-]\s*/, '').trim(),
                group: adviceGroups.find(function (group) { return group.prefixes.includes(code); }) };
        }
        const normalized = normalizeAdvice(text);
        return { text: text, group: adviceGroups.find(function (group) {
            return group.keywords.some(function (keyword) { return normalized.includes(keyword); });
        }) };
    }

    function createAdviceSection(group, values) {
        const section = document.createElement('section');
        const heading = document.createElement('div');
        const marker = document.createElement('span');
        const title = document.createElement('h3');
        const list = document.createElement('ul');
        section.className = 'ai-advice-section ai-advice-section-' + group.key;
        heading.className = 'ai-advice-section-head';
        marker.className = 'ai-advice-section-marker';
        marker.textContent = group.marker;
        title.textContent = group.title;
        heading.append(marker, title);
        list.replaceChildren.apply(list, values.map(function (value) {
            const item = document.createElement('li');
            item.textContent = value;
            return item;
        }));
        section.append(heading, list);
        return section;
    }

    function normalizeAdvice(value) {
        return value.normalize('NFD').replace(/[\u0300-\u036f]/g, '')
                .replace(/đ/g, 'd').replace(/Đ/g, 'D').toLowerCase();
    }
}());
