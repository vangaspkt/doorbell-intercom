# Hướng Dẫn Build File APK Cài Lên Điện Thoại

Dự án Android hoàn chỉnh đã được tạo sẵn tại thư mục:
`C:\Users\Admin\Desktop\SMART_KEY\DOOR_BELL\android_app`

---

## Cách 1: Tự động Build bằng GitHub Actions (Khuyên dùng - Nhanh nhất & Miễn phí)

Không cần cài đặt bộ công cụ Android SDK nặng hơn 10GB trên máy tính, GitHub sẽ tự động build trên máy chủ đám mây và xuất file APK cho bạn:

1. Tạo một tài khoản GitHub và tạo 1 repository mới (ví dụ đặt tên là `door-intercom-app`, có thể để Private).
2. Tải toàn bộ thư mục `android_app` lên repo đó.
3. Vào tab **Actions** trên GitHub: bạn sẽ thấy luồng **"Tự động Build APK Chuông Cửa"** đang tự động chạy.
4. Sau khoảng 2–3 phút khi hoàn tất (có dấu tích xanh ✅), bấm vào lượt chạy đó.
5. Kéo xuống mục **Artifacts** ở dưới cùng và tải file **`ChuongCua-ESP32-APK`** về.
6. Giải nén file zip và cài đặt file **`app-debug.apk`** trực tiếp lên điện thoại Android!

---

## Cách 2: Mở và Build bằng Android Studio

Nếu máy bạn đã hoặc muốn cài **Android Studio**:

1. Tải và cài đặt Android Studio: https://developer.android.com/studio
2. Mở Android Studio, chọn **File -> Open...** -> trỏ đến thư mục:
   `C:\Users\Admin\Desktop\SMART_KEY\DOOR_BELL\android_app`
3. Chờ Android Studio đồng bộ xong (Sync finished).
4. Vào thanh menu trên cùng: **Build -> Build Bundle(s) / APK(s) -> Build APK(s)**.
5. Sau khi build xong, góc dưới bên phải màn hình sẽ hiện thông báo có nút **locate**, bấm vào sẽ mở thư mục chứa file:
   `app/build/outputs/apk/debug/app-debug.apk`
6. Chép file `app-debug.apk` này sang điện thoại để cài đặt.

---

## Các tính năng đã được lập trình sẵn trong App:
- **Tự động bật sáng màn hình khi đang khóa:** Sử dụng cơ chế `setShowWhenLocked(true)`, `setTurnScreenOn(true)` và `USE_FULL_SCREEN_INTENT`.
- **Hiển thị hình ảnh trực tiếp từ ESP32-CAM:** Nhận từng khung hình JPEG qua WebSocket và hiển thị mượt mà.
- **Đàm thoại 2 chiều:**
  - Nghe âm thanh từ micro INMP441 của ESP32 qua loa ngoài điện thoại.
  - Nhấn giữ nút **"Giữ để nói"** để thu âm micro điện thoại và phát ra loa MAX98357A của ESP32.
- **Điều khiển phụ:** Có sẵn nút bật/tắt đèn và còi báo động ngay trên màn hình chuông gọi.
- **Tự động tắt:** Sau khi cúp máy hoặc quá thời gian chờ, app tự đóng để màn hình tắt tiết kiệm pin.

