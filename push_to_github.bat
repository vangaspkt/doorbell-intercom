@echo off
chcp 65001 >nul
echo ========================================================
echo   ĐẨY DỰ ÁN LÊN GITHUB: vangaspkt/doorbell-intercom
echo ========================================================
echo.
cd /d "%~dp0"

echo Đang kết nối tới GitHub...
"C:\Users\Admin\mingit\cmd\git.exe" push -u origin main

echo.
if %ERRORLEVEL% equ 0 (
    echo ========================================================
    echo   THÀNH CÔNG! Mã nguồn đã được đưa lên GitHub.
    echo.
    echo   Luồng build APK tự động đang chạy tại:
    echo   https://github.com/vangaspkt/doorbell-intercom/actions
    echo.
    echo   Vui lòng đợi 2-3 phút rồi vào link trên tải file APK về!
    echo ========================================================
) else (
    echo [CHÚ Ý] Nếu có cửa sổ đăng nhập GitHub hiện lên, vui lòng chọn "Sign in with your browser" để hoàn tất.
)
echo.
pause
