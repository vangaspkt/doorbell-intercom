@echo off
chcp 65001 >nul
title Đẩy Doorbell Intercom lên GitHub
cd /d "%~dp0"

echo ========================================================
echo       ĐẨY MÃ NGUỒN LÊN GITHUB ĐỂ TỰ ĐỘNG BUILD APK
echo ========================================================
echo.

REM Kiểm tra commit
"C:\Users\Admin\mingit\cmd\git.exe" add .
"C:\Users\Admin\mingit\cmd\git.exe" commit -m "Auto commit files" >nul 2>&1
"C:\Users\Admin\mingit\cmd\git.exe" branch -M main

echo [1] Đẩy lên: https://github.com/vangaspkt/doorbell-intercom.git
echo [2] Nhập URL khác
echo.
set /p CHOICE="Chọn [1, 2, hoặc 3] (Mặc định là 1): "

if "%CHOICE%"=="2" (
    set TARGET_URL=https://github.com/vanngaspkt-dotcom/doorbell-intercom.git
) else if "%CHOICE%"=="3" (
    set /p TARGET_URL="Nhập link repository: "
) else (
    set TARGET_URL=https://github.com/vangaspkt/doorbell-intercom.git
)

echo.
echo Đang đặt remote: %TARGET_URL%
"C:\Users\Admin\mingit\cmd\git.exe" remote remove origin 2>nul
"C:\Users\Admin\mingit\cmd\git.exe" remote add origin %TARGET_URL%

echo.
echo Đang đẩy dữ liệu lên GitHub...
"C:\Users\Admin\mingit\cmd\git.exe" push -u origin main --force

echo.
if %ERRORLEVEL% equ 0 (
    echo ========================================================
    echo   [THÀNH CÔNG!] Mã nguồn đã được tải lên GitHub.
    echo.
    echo   Máy chủ GitHub đang bắt đầu build file APK cho bạn...
    echo   Vào mục 'Actions' trên GitHub để xem tiến độ và tải APK:
    echo   %TARGET_URL:~0,-4%/actions
    echo ========================================================
) else (
    echo ========================================================
    echo   [THẤT BẠI] Không thể đẩy lên GitHub.
    echo   Lý do thường gặp:
    echo   1. Bạn chưa tạo repository này trên GitHub.
    echo   2. Bạn chưa đăng nhập tài khoản sở hữu repository này.
    echo ========================================================
)
echo.
pause
