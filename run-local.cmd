@echo off
setlocal EnableExtensions

title DiaCare - Local
cd /d "%~dp0"

echo.
echo ========================================
echo        DiaCare - Chay local
echo ========================================
echo Thu muc: %CD%
echo.

if not exist "mvnw.cmd" (
    echo [LOI] Khong tim thay mvnw.cmd.
    echo Hay dat file nay trong thu muc goc cua du an.
    pause
    exit /b 1
)

where java >nul 2>&1
if errorlevel 1 (
    echo [LOI] Chua tim thay Java trong PATH.
    echo Hay cai JDK 17 va mo lai cua so terminal.
    pause
    exit /b 1
)

for /f "tokens=5" %%P in ('netstat -ano ^| findstr /R /C:":8082 .*LISTENING"') do set "APP_PID=%%P"
if defined APP_PID (
    echo [OK] Server da chay san tren cong 8082, PID %APP_PID%.
    echo Dang mo trang dang nhap...
    start "" "http://localhost:8082/Login"
    pause
    exit /b 0
)

sc query "MSSQL$MINH" | findstr /I "RUNNING" >nul
if errorlevel 1 (
    echo SQL Server MINH dang dung. Dang thu khoi dong...
    net start "MSSQL$MINH" >nul 2>&1
    if errorlevel 1 (
        echo [LOI] Khong the tu khoi dong SQL Server MINH.
        echo Mo SQL Server Configuration Manager va bat service MSSQL$MINH.
        pause
        exit /b 1
    )
)
echo [OK] SQL Server MINH dang chay.

echo [INFO] Dang clean va khoi dong ung dung tren http://localhost:8082 ...
start "" /b powershell.exe -NoProfile -WindowStyle Hidden -Command ^
    "$url='http://localhost:8082'; for($i=0;$i -lt 90;$i++){ try { $response=Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 1; if($response.StatusCode -ge 200){ Start-Process $url; break } } catch {} ; Start-Sleep -Seconds 1 }"

call "%~dp0mvnw.cmd" -DskipTests clean spring-boot:run
set "APP_EXIT=%ERRORLEVEL%"

echo.
if not "%APP_EXIT%"=="0" (
    echo [LOI] Ung dung khoi dong that bai. Xem dong ERROR o phia tren.
) else (
    echo Server da dung.
)
pause
exit /b %APP_EXIT%
