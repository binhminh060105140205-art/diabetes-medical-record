<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Sức khỏe hôm nay — DiaCare</title><link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css?v=20260722-web-audit2"><link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/patient-care-path.css?v=20260721-patient1"></head>
<body><jsp:include page="header.jsp"/><jsp:include page="topnav.jsp"/>
<main class="page-wrapper patient-today-page"><div class="page-heading"><div><div class="eyebrow">THEO DÕI HẰNG NGÀY</div><h1 class="page-title">Sức khỏe hôm nay</h1></div><a class="btn btn-light" href="${pageContext.request.contextPath}/PatientJournal">Nhật ký 30 ngày</a></div>
<c:if test="${not empty msg}"><div class="alert alert-info"><c:out value="${msg}"/></div></c:if>
<c:if test="${not empty diabetesProfile}">
<section class="patient-overview">
    <div>
        <span>HỒ SƠ ĐÁI THÁO ĐƯỜNG DO BÁC SĨ XÁC NHẬN</span>
        <div>${diabetesProfile.diabetesTypeLabel}</div>
        <p>Điều trị hiện tại: ${diabetesProfile.treatmentMethodLabel}<c:if test="${not empty diabetesProfile.diagnosisDate}"> · Phát hiện: ${diabetesProfile.diagnosisDate}</c:if><c:if test="${not empty diabetesProfile.hba1cTarget}"> · Mục tiêu HbA1c: ${diabetesProfile.hba1cTarget}%</c:if></p>
    </div>
</section>
<jsp:include page="diabetesCarePath.jsp"/>
</c:if>
<c:if test="${not empty patient}">
<section class="card ai-advice-card" id="daily-advice">
    <div class="ai-advice-heading">
        <div><div class="eyebrow">TRỢ LÝ SỨC KHỎE</div><h2>Lời khuyên hôm nay</h2></div>
        <span class="ai-advice-icon" aria-hidden="true">✦</span>
    </div>
    <label class="ai-consent"><input type="checkbox" id="aiConsent"> <span>Tôi đồng ý dùng dữ liệu sức khỏe đã ẩn danh để tạo lời khuyên.</span></label>
    <div class="ai-advice-actions"><button type="button" class="btn btn-primary" id="aiAdviceButton" onclick="loadPatientAdvice()">Nhận lời khuyên</button></div>
    <div class="ai-advice-result" id="aiAdviceResult" hidden aria-live="polite">
        <div class="ai-advice-result-head"><strong id="aiAdviceSummary"></strong><span id="aiAdviceSeverity" class="ai-severity"></span></div>
        <div id="aiAdviceItems" class="ai-advice-groups"></div><p id="aiAdviceDoctor" class="ai-doctor-note" hidden></p><small id="aiAdviceSource"></small>
    </div>
</section>

</c:if>
<c:if test="${not empty patient}"><section class="card daily-entry-card daily-entry-full"><div class="card-title">Nhập chỉ số hôm nay <c:if test="${not empty todayLog}"><span class="status-pill status-COMPLETED">Đã nhập</span></c:if></div>
<div class="form-group"><label>Thời điểm đo đường huyết</label><select id="log_meal" class="form-control"><option value="">Chọn thời điểm</option><option value="FASTING" ${todayLog.mealType=='FASTING'?'selected':''}>Lúc đói</option><option value="AFTER_MEAL" ${todayLog.mealType=='AFTER_MEAL'?'selected':''}>Sau ăn</option><option value="BEDTIME" ${todayLog.mealType=='BEDTIME'?'selected':''}>Trước khi ngủ</option><option value="OTHER" ${todayLog.mealType=='OTHER'?'selected':''}>Khác</option></select></div>
<div class="form-group"><label>Đường huyết (mg/dL)</label><input type="number" min="20" max="600" step="0.1" id="log_bg" class="form-control" value="${todayLog.bloodGlucose}" placeholder="Ví dụ: 105"></div>
<div class="two-column"><div class="form-group"><label>Huyết áp tâm thu</label><input type="number" min="60" max="260" id="log_sbp" class="form-control" value="${todayLog.systolicBp}" placeholder="120"></div><div class="form-group"><label>Huyết áp tâm trương</label><input type="number" min="30" max="180" id="log_dbp" class="form-control" value="${todayLog.diastolicBp}" placeholder="80"></div></div>
<div class="two-column"><div class="form-group"><label>Nhịp tim (lần/phút)</label><input type="number" min="30" max="220" id="log_hr" class="form-control" value="${todayLog.heartRate}" placeholder="75"></div><div class="form-group"><label>SpO2 (%)</label><input type="number" min="50" max="100" step="0.1" id="log_spo2" class="form-control" value="${todayLog.spo2}" placeholder="98"></div></div>
<div class="form-group"><label>Cân nặng (kg)</label><input type="number" min="20" max="300" step="0.1" id="log_weight" class="form-control" value="${todayLog.weight}" placeholder="65"></div>
<div class="form-group"><label>Triệu chứng hôm nay</label><div class="checkbox-grid"><label><input type="checkbox" name="symptom" value="Không có triệu chứng"> Không có</label><label><input type="checkbox" name="symptom" value="Mệt mỏi"> Mệt mỏi</label><label><input type="checkbox" name="symptom" value="Khát nhiều"> Khát nhiều</label><label><input type="checkbox" name="symptom" value="Chóng mặt"> Chóng mặt</label><label><input type="checkbox" name="symptom" value="Run tay/vã mồ hôi"> Run tay/vã mồ hôi</label><c:if test="${diabetesProfile.diabetesType=='TYPE_1'}"><label><input type="checkbox" name="symptom" value="Buồn nôn hoặc đau bụng"> Buồn nôn/đau bụng</label><label><input type="checkbox" name="symptom" value="Thở nhanh hoặc thở sâu"> Thở nhanh/thở sâu</label></c:if><c:if test="${diabetesProfile.diabetesType=='TYPE_2'}"><label><input type="checkbox" name="symptom" value="Tê bì bàn chân"> Tê bì bàn chân</label><label><input type="checkbox" name="symptom" value="Phù chân"> Phù chân</label></c:if></div></div>
<div class="form-group"><label>Ghi chú</label><textarea id="log_note" class="form-control" maxlength="1000" placeholder="Thông tin khác muốn bác sĩ biết"><c:out value="${todayLog.note}"/></textarea></div>
<button type="button" onclick="saveLog()" class="btn btn-primary" id="saveLogBtn">Lưu chỉ số hôm nay</button><div id="logResult" class="daily-log-message" role="status" aria-live="polite" hidden></div></section></c:if>
</main><span id="savedSymptoms" hidden><c:out value="${todayLog.symptoms}"/></span><jsp:include page="footer.jsp"/>
<script>
const CTX='${pageContext.request.contextPath}';
const symptomInputs=[...document.querySelectorAll('[name=symptom]')];
const savedSymptoms=new Set(document.getElementById('savedSymptoms').textContent.split(',').map(value=>value.trim()).filter(Boolean));
symptomInputs.forEach(input=>{
    input.checked=savedSymptoms.has(input.value);
    input.addEventListener('change',()=>{
        const none=symptomInputs.find(item=>item.value==='Không có triệu chứng');
        if (input===none && input.checked) symptomInputs.forEach(item=>{if(item!==none)item.checked=false;});
        if (input!==none && input.checked && none) none.checked=false;
    });
});
async function saveLog(){
    const button=document.getElementById('saveLogBtn');
    const meal=document.getElementById('log_meal');
    const glucose=document.getElementById('log_bg');
    const systolic=document.getElementById('log_sbp');
    const diastolic=document.getElementById('log_dbp');
    const heartRate=document.getElementById('log_hr');
    const spo2=document.getElementById('log_spo2');
    const weight=document.getElementById('log_weight');
    const note=document.getElementById('log_note');
    const fields=[meal,glucose,systolic,diastolic,heartRate,spo2,weight];
    showLogResult('');
    const invalid=fields.find(field=>field.value && !field.checkValidity());
    if (invalid) { showLogResult('Giá trị vừa nhập chưa hợp lệ. Vui lòng kiểm tra lại giới hạn của chỉ số.', 'error'); invalid.reportValidity(); return; }
    if (glucose.value && !meal.value) { showLogResult('Vui lòng chọn thời điểm đo đường huyết.', 'error'); meal.focus(); return; }
    if (meal.value && !glucose.value) { showLogResult('Đã chọn thời điểm đo thì cần nhập đường huyết.', 'error'); glucose.focus(); return; }
    if ((systolic.value && !diastolic.value) || (!systolic.value && diastolic.value)) {
        showLogResult('Huyết áp cần nhập đủ cả tâm thu và tâm trương.', 'error');
        (systolic.value ? diastolic : systolic).focus();
        return;
    }
    const symptoms=symptomInputs.filter(input=>input.checked).map(input=>input.value).join(', ');
    if (!glucose.value&&!systolic.value&&!diastolic.value&&!heartRate.value&&!spo2.value&&!weight.value&&!symptoms&&!note.value.trim()) { showLogResult('Cần nhập ít nhất một chỉ số hoặc ghi chú.', 'error'); return; }
    button.disabled=true; showLogResult('Đang lưu chỉ số...', 'loading');
    try{
        const body=new URLSearchParams({action:'saveLog',mealType:meal.value,bloodGlucose:glucose.value,systolicBp:systolic.value,diastolicBp:diastolic.value,heartRate:heartRate.value,spo2:spo2.value,weight:weight.value,symptoms,note:note.value});
        const response=await fetch(CTX+'/PatientHealth',{method:'POST',body});
        const data=await response.json();
        showLogResult(data.success?'Đã lưu chỉ số hôm nay.':(data.error||'Không thể lưu dữ liệu.'), data.success?'success':'error');
    }catch(error){ showLogResult('Không thể kết nối máy chủ.', 'error'); }
    finally{ button.disabled=false; }
}
function showLogResult(message,type){
    const result=document.getElementById('logResult');
    result.textContent=message;
    result.className='daily-log-message'+(type?' is-'+type:'');
    result.hidden=!message;
}
const adviceGroups=[
    {key:'monitoring',title:'Theo dõi hôm nay',marker:'01',prefixes:['THEO_DOI'],keywords:['duong huyet','huyet ap','chi so','hba1c','theo doi','ghi lai ket qua','thoi diem do']},
    {key:'treatment',title:'Thuốc và ăn uống',marker:'02',prefixes:['DIEU_TRI','AN_UONG'],keywords:['insulin','thuoc','dieu tri','an dung bua','bua an','thuc pham','nuoc ngot','tra sua','tinh bot']},
    {key:'care',title:'Vận động và chăm sóc',marker:'03',prefixes:['VAN_DONG','CHAM_SOC'],keywords:['van dong','di bo','nghi ngoi','ban chan','ngu du','nguoi than','da sach']},
    {key:'contact',title:'Khi cần liên hệ bác sĩ',marker:'!',prefixes:['LIEN_HE'],keywords:['lien he','phong kham','tai kham','nhan vien y te','hoi bac si']}
];
function normalizeAdvice(value){
    return value.normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/\u0111/g,'d').replace(/\u0110/g,'D').toLowerCase();
}
function splitAdvice(value){
    const text=String(value||'').trim();
    const prefix=text.match(/^\[(THEO_DOI|DIEU_TRI|AN_UONG|VAN_DONG|CHAM_SOC|LIEN_HE)]\s*/i);
    if(prefix){
        const code=prefix[1].toUpperCase();
        return {text:text.slice(prefix[0].length).replace(/^\s*[:\-]\s*/, '').trim(),group:adviceGroups.find(group=>group.prefixes.includes(code))};
    }
    const normalized=normalizeAdvice(text);
    return {text,group:adviceGroups.find(group=>group.keywords.some(keyword=>normalized.includes(keyword)))};
}
function renderAdviceGroups(values){
    const container=document.getElementById('aiAdviceItems');
    const grouped=new Map(adviceGroups.map(group=>[group.key,[]]));
    (Array.isArray(values)?values:[]).forEach(value=>{
        const parsed=splitAdvice(value);
        if(!parsed.text)return;
        const group=parsed.group||adviceGroups.slice(0,3).reduce((smallest,current)=>grouped.get(current.key).length<grouped.get(smallest.key).length?current:smallest);
        grouped.get(group.key).push(parsed.text);
    });
    const sections=adviceGroups.filter(group=>grouped.get(group.key).length).map(group=>{
        const section=document.createElement('section');section.className='ai-advice-section ai-advice-section-'+group.key;
        const heading=document.createElement('div');heading.className='ai-advice-section-head';
        const marker=document.createElement('span');marker.className='ai-advice-section-marker';marker.textContent=group.marker;
        const title=document.createElement('h3');title.textContent=group.title;heading.append(marker,title);
        const list=document.createElement('ul');list.replaceChildren(...grouped.get(group.key).map(value=>{const item=document.createElement('li');item.textContent=value;return item;}));
        section.append(heading,list);return section;
    });
    container.replaceChildren(...sections);
}
async function loadPatientAdvice(){
    const consent=document.getElementById('aiConsent'),button=document.getElementById('aiAdviceButton'),box=document.getElementById('aiAdviceResult'),summary=document.getElementById('aiAdviceSummary'),items=document.getElementById('aiAdviceItems');
    const severity=document.getElementById('aiAdviceSeverity'),doctor=document.getElementById('aiAdviceDoctor'),source=document.getElementById('aiAdviceSource');
    severity.textContent='';severity.className='ai-severity';doctor.hidden=true;doctor.textContent='';source.textContent='';items.replaceChildren();
    if(!consent.checked){box.hidden=false;summary.textContent='Vui lòng đọc và đánh dấu đồng ý trước khi tiếp tục.';return;}
    button.disabled=true;button.textContent='Đang phân tích...';box.hidden=false;summary.textContent='Đang tạo lời khuyên an toàn cho hôm nay...';
    try{
        const response=await fetch(CTX+'/api/patient/ai-advice',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({consent:true})});
        const data=await response.json();if(!response.ok)throw new Error(data.error||'Không thể tạo lời khuyên.');
        summary.textContent=data.summary;severity.textContent=data.severity==='high'?'Nên liên hệ bác sĩ':data.severity==='medium'?'Cần chú ý':'Ổn định';severity.className='ai-severity ai-severity-'+data.severity;
        renderAdviceGroups(data.advice);
        doctor.hidden=!data.doctorRecommendation;doctor.textContent=data.doctorRecommendation?'Nếu cảm thấy không khỏe hoặc triệu chứng tiếp diễn, hãy liên hệ bác sĩ/phòng khám.':'';
        source.textContent=(data.source==='OPENAI'?'Lời khuyên từ hệ thống phân tích tự động':'Lời khuyên từ bộ quy tắc an toàn nội bộ')+(data.cached?' · đã lưu trong ngày':'');
    }catch(error){summary.textContent=error.message;items.replaceChildren();}
    finally{button.disabled=false;button.textContent='Nhận lời khuyên';}
}
</script></body></html>
