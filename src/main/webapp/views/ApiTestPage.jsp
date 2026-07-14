<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Kiểm tra kết nối máy xét nghiệm</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
    <style>
        .api-demo { background:#1e293b; border-radius:12px; padding:20px 24px; margin-bottom:20px; }
        .api-demo pre { color:#86efac; font-size:13px; white-space:pre-wrap; word-break:break-all; margin:0; }
        .api-url { background:#0f172a; color:#60a5fa; padding:10px 14px; border-radius:8px; font-family:monospace; font-size:14px; margin-bottom:12px; }
        .api-result { background:#0f172a; border-radius:8px; padding:14px; margin-top:14px; min-height:60px; }
        .api-result pre { color:#4ade80; font-size:13px; }
        .api-result.err pre { color:#f87171; }
        .flow-step { display:flex; align-items:flex-start; gap:16px; padding:16px; background:#f8fafc; border-radius:10px; margin-bottom:12px; border-left:4px solid #0d6efd; }
        .flow-num { width:32px; height:32px; border-radius:50%; background:#0d6efd; color:white; display:flex; align-items:center; justify-content:center; font-weight:800; flex-shrink:0; }
    </style>
</head>
<body>
<jsp:include page="header.jsp"/>
<jsp:include page="topnav.jsp"/>
<div class="page-wrapper">
    <h1 class="page-title">🔬 Kết nối máy xét nghiệm — REST API</h1>

    <!-- LUỒNG HOẠT ĐỘNG -->
    <div class="card">
        <div class="card-title">📋 Luồng hoạt động khi có máy xét nghiệm</div>
        <div class="flow-step">
            <div class="flow-num">1</div>
            <div>
                <strong>Staff tạo bệnh án</strong> — nhập thông tin khám (Tab 1), lấy được <code>recordId</code>
            </div>
        </div>
        <div class="flow-step" style="border-left-color:#198754;">
            <div class="flow-num" style="background:#198754;">2</div>
            <div>
                <strong>Máy xét nghiệm gửi kết quả</strong> — Lab/máy đo gọi API POST với JSON chứa các chỉ số và <code>recordId</code>
            </div>
        </div>
        <div class="flow-step" style="border-left-color:#9333ea;">
            <div class="flow-num" style="background:#9333ea;">3</div>
            <div>
                <strong>Server nhận → AI phân tích ngay</strong> — Hệ thống lưu chỉ số và tự động chạy AI, trả về mức nguy cơ
            </div>
        </div>
        <div class="flow-step" style="border-left-color:#f97316;">
            <div class="flow-num" style="background:#f97316;">4</div>
            <div>
                <strong>Bác sĩ nhận thông báo</strong> — Dashboard bác sĩ hiển thị cảnh báo AI ngay lập tức
            </div>
        </div>
    </div>

    <!-- API SPEC -->
    <div class="card">
        <div class="card-title">📡 Thông tin API Endpoint</div>

        <table>
            <tr><th style="width:140px">Method</th><td><strong style="color:#198754;">POST</strong></td></tr>
            <tr><th>Endpoint</th><td><code>${pageContext.request.contextPath}/api/indicators</code></td></tr>
            <tr><th>Auth Header</th><td><code>X-API-Key: diabetes-lab-2026</code></td></tr>
            <tr><th>Content-Type</th><td><code>application/json</code></td></tr>
        </table>

        <br>
        <div class="card-title" style="font-size:14px;">📥 Request Body (JSON)</div>
        <div class="api-demo">
<pre>{
  "recordId"    : 1,          // BẮT BUỘC — mã bệnh án
  "height"      : 165,        // cm
  "weight"      : 70,         // kg  → BMI tự tính
  "systolicBp"  : 135,        // mmHg huyết áp tâm thu
  "diastolicBp" : 85,         // mmHg huyết áp tâm trương
  "heartRate"   : 78,         // lần/phút
  "temperature" : 36.8,       // °C
  "bloodGlucose": 180.5,      // mg/dL — đường huyết
  "hba1c"       : 7.2,        // %     — đường huyết 3 tháng
  "cholesterol" : 220.0,      // mg/dL
  "triglyceride": 145.0,      // mg/dL
  "hdlC"        : 42.0,       // mg/dL
  "ldlC"        : 138.0       // mg/dL
}</pre>
        </div>

        <div class="card-title" style="font-size:14px;">📤 Response thành công (200)</div>
        <div class="api-demo">
<pre>{
  "success"    : true,
  "message"    : "Đã lưu chỉ số và phân tích AI thành công",
  "recordId"   : 1,
  "bmi"        : 25.7,
  "aiRiskLevel": "HIGH",
  "aiScore"    : 72.0,
  "aiWarning"  : "• Đường huyết ≥ 126 mg/dL...",
  "aiSuggestion": "• Cần điều trị thuốc hạ đường huyết..."
}</pre>
        </div>
    </div>

    <!-- TEST TOOL -->
    <div class="card">
        <div class="card-title">🧪 Công cụ test gửi dữ liệu mô phỏng máy xét nghiệm</div>
        <p class="text-muted" style="margin-bottom:16px;">Nhập mã bệnh án và chỉ số để test API ngay trên trình duyệt</p>

        <div class="form-row">
            <div class="form-group">
                <label class="required">Mã bệnh án (recordId)</label>
                <input type="number" id="t_recordId" class="form-control" placeholder="1" value="1">
            </div>
            <div class="form-group">
                <label>Đường huyết (mg/dL)</label>
                <input type="number" step="0.1" id="t_bloodGlucose" class="form-control" placeholder="180">
            </div>
            <div class="form-group">
                <label>HbA1c (%)</label>
                <input type="number" step="0.1" id="t_hba1c" class="form-control" placeholder="7.2">
            </div>
            <div class="form-group">
                <label>Chiều cao (cm)</label>
                <input type="number" step="0.1" id="t_height" class="form-control" placeholder="165">
            </div>
            <div class="form-group">
                <label>Cân nặng (kg)</label>
                <input type="number" step="0.1" id="t_weight" class="form-control" placeholder="70">
            </div>
            <div class="form-group">
                <label>HA tâm thu</label>
                <input type="number" id="t_systolicBp" class="form-control" placeholder="135">
            </div>
            <div class="form-group">
                <label>HA tâm trương</label>
                <input type="number" id="t_diastolicBp" class="form-control" placeholder="85">
            </div>
            <div class="form-group">
                <label>Cholesterol</label>
                <input type="number" step="0.1" id="t_cholesterol" class="form-control" placeholder="220">
            </div>
            <div class="form-group">
                <label>Triglyceride</label>
                <input type="number" step="0.1" id="t_triglyceride" class="form-control" placeholder="145">
            </div>
        </div>

        <button onclick="sendTest()" class="btn btn-primary">
            🚀 Gửi dữ liệu lên API (mô phỏng máy xét nghiệm)
        </button>
        &nbsp;
        <button onclick="fillSample('high')" class="btn btn-danger btn-sm">Mẫu nguy cơ CAO</button>
        <button onclick="fillSample('medium')" class="btn btn-warning btn-sm">Mẫu nguy cơ TRUNG BÌNH</button>
        <button onclick="fillSample('low')" class="btn btn-success btn-sm">Mẫu nguy cơ THẤP</button>

        <div id="apiResult" class="api-result" style="display:none;">
            <pre id="apiResultText"></pre>
        </div>
    </div>

    <!-- HƯỚNG DẪN TÍCH HỢP -->
    <div class="card">
        <div class="card-title">🔧 Hướng dẫn tích hợp với máy xét nghiệm thực tế</div>
        <p style="margin-bottom:14px;color:#555;">
            Phần mềm điều khiển máy xét nghiệm (thường là Windows app) cần gọi HTTP như sau:
        </p>
        <div class="api-demo">
<pre style="color:#fbbf24;">// Ví dụ code Python (phía máy xét nghiệm)
import requests

url = "http://YOUR_SERVER_IP:9999/DiabetesMedicalRecord/api/indicators"
headers = {
    "Content-Type": "application/json",
    "X-API-Key": "diabetes-lab-2026"
}
data = {
    "recordId"    : 5,
    "bloodGlucose": 180.5,
    "hba1c"       : 7.2,
    "cholesterol" : 220.0,
    "triglyceride": 145.0,
    "hdlC"        : 42.0,
    "ldlC"        : 138.0
}
response = requests.post(url, json=data, headers=headers)
result = response.json()
print(f"Risk Level: {result['aiRiskLevel']} | Score: {result['aiScore']}")</pre>
        </div>

        <div class="api-demo" style="margin-top:14px;">
<pre style="color:#93c5fd;">// Ví dụ code C# (nếu máy dùng .NET)
var client = new HttpClient();
client.DefaultRequestHeaders.Add("X-API-Key", "diabetes-lab-2026");
var json = JsonSerializer.Serialize(new {
    recordId = 5,
    bloodGlucose = 180.5,
    hba1c = 7.2
});
var content = new StringContent(json, Encoding.UTF8, "application/json");
var response = await client.PostAsync(
    "http://YOUR_SERVER:9999/DiabetesMedicalRecord/api/indicators",
    content
);</pre>
        </div>
    </div>
</div>

<script src="${pageContext.request.contextPath}/static/js/main.js"></script>
<script>
const API_URL = '${pageContext.request.contextPath}/api/indicators';
const API_KEY = 'diabetes-lab-2026';

function getNum(id) {
    const v = document.getElementById(id).value;
    return v ? parseFloat(v) : undefined;
}

async function sendTest() {
    const recordId = parseInt(document.getElementById('t_recordId').value);
    if (!recordId) { alert('Vui lòng nhập mã bệnh án'); return; }

    const body = { recordId };
    const fields = ['bloodGlucose','hba1c','height','weight','systolicBp',
                    'diastolicBp','cholesterol','triglyceride'];
    fields.forEach(f => {
        const v = getNum('t_' + f);
        if (v !== undefined) body[f] = v;
    });

    const resultDiv = document.getElementById('apiResult');
    const resultText = document.getElementById('apiResultText');
    resultDiv.style.display = 'block';
    resultDiv.className = 'api-result';
    resultText.textContent = '⏳ Đang gửi...';

    try {
        const res = await fetch(API_URL, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'X-API-Key': API_KEY },
            body: JSON.stringify(body)
        });
        const data = await res.json();
        resultText.textContent = JSON.stringify(data, null, 2);

        if (data.success) {
            resultDiv.className = 'api-result';
            resultText.textContent = '✅ THÀNH CÔNG!\n\n' + JSON.stringify(data, null, 2);
        } else {
            resultDiv.className = 'api-result err';
            resultText.textContent = '❌ LỖI!\n\n' + JSON.stringify(data, null, 2);
        }
    } catch (e) {
        resultDiv.className = 'api-result err';
        resultText.textContent = '❌ Không thể kết nối: ' + e.message;
    }
}

function fillSample(level) {
    const samples = {
        high: { bloodGlucose:185, hba1c:7.5, height:162, weight:78, systolicBp:148, diastolicBp:92, cholesterol:245, triglyceride:210 },
        medium: { bloodGlucose:115, hba1c:6.1, height:165, weight:68, systolicBp:130, diastolicBp:82, cholesterol:205, triglyceride:160 },
        low: { bloodGlucose:88, hba1c:5.2, height:168, weight:62, systolicBp:118, diastolicBp:75, cholesterol:175, triglyceride:120 }
    };
    const s = samples[level];
    Object.entries(s).forEach(([k,v]) => {
        const el = document.getElementById('t_' + k);
        if (el) el.value = v;
    });
}
</script>
<jsp:include page="footer.jsp"/>
</body>
</html>
