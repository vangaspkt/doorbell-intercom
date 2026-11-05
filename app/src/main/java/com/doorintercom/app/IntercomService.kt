package com.doorintercom.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.Vibrator
import android.os.VibrationEffect
import androidx.core.app.NotificationCompat
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class IntercomService : Service() {

    companion object {
        const val CHANNEL_SERVICE = "intercom_service_channel"
        const val CHANNEL_CALL = "intercom_call_high_priority_channel"
        const val NOTIFICATION_ID_SERVICE = 101
        const val NOTIFICATION_ID_CALL = 102

        var activeWebSocket: WebSocket? = null
        var frameListener: ((ByteArray) -> Unit)? = null
        var audioListener: ((ByteArray) -> Unit)? = null
    }

    private var client: OkHttpClient? = null
    private var webSocket: WebSocket? = null
    private var isRunning = false
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        startForeground(NOTIFICATION_ID_SERVICE, createServiceNotification("Đang kết nối tới chuông cửa..."))

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DoorIntercom:ServiceWakeLock")
        wakeLock?.acquire(10 * 60 * 1000L) // Giữ wake lock cơ bản
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = getSharedPreferences("intercom_prefs", Context.MODE_PRIVATE)
        val ip = prefs.getString("server_ip", "192.168.1.100") ?: "192.168.1.100"
        val port = prefs.getString("server_port", "3000") ?: "3000"
        val password = prefs.getString("server_password", "1234") ?: "1234"

        connectWebSocket(ip, port, password)
        return START_STICKY
    }

    private fun connectWebSocket(ip: String, port: String, pw: String) {
        if (isRunning) return
        isRunning = true

        client = OkHttpClient.Builder()
            .pingInterval(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        val url = "ws://$ip:$port/web?pw=$pw"
        val request = Request.Builder().url(url).build()

        webSocket = client?.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                activeWebSocket = ws
                updateForegroundNotification("Đã kết nối chuông cửa ($ip:$port)")
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val type = json.optString("t")
                    if (type == "call") {
                        // KHI CÓ NGƯỜI BẤM CHUÔNG
                        onCallReceived()
                    }
                } catch (_: Exception) {}
            }

            override fun onMessage(ws: WebSocket, bytes: ByteString) {
                val data = bytes.toByteArray()
                if (data.isEmpty()) return

                if (data[0].toInt() == 1) {
                    // Frame hình ảnh camera JPEG (từ byte số 1 trở đi)
                    val jpeg = ByteArray(data.size - 1)
                    System.arraycopy(data, 1, jpeg, 0, jpeg.size)
                    frameListener?.invoke(jpeg)
                } else {
                    // Âm thanh PCM từ mic ESP32
                    audioListener?.invoke(data)
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                activeWebSocket = null
                updateForegroundNotification("Mất kết nối. Đang thử lại...")
                // Tự động kết nối lại sau 4 giây
                Thread.sleep(4000)
                if (isRunning) {
                    isRunning = false
                    connectWebSocket(ip, port, pw)
                }
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                activeWebSocket = null
            }
        })
    }

    // ĐÁNH THỨC ĐIỆN THOẠI & BẬT SÁNG MÀN HÌNH
    private fun onCallReceived() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        @Suppress("DEPRECATION")
        val screenLock = pm.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "DoorIntercom:CallWakeScreenLock"
        )
        screenLock.acquire(15000L) // Bật sáng màn hình trong 15s

        // Rung điện thoại
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500, 200, 500), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 500, 200, 500, 200, 500), -1)
        }

        // Tạo Intent mở màn hình IncomingCallActivity đè lên màn hình khóa
        val callIntent = Intent(this, IncomingCallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            callIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Bắn FullScreen Notification chuẩn Incoming Call
        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val notification = NotificationCompat.Builder(this, CHANNEL_CALL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("CÓ NGƯỜI BẤM CHUÔNG!")
            .setContentText("Nhấn để xem hình ảnh và trả lời")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pendingIntent, true) // Tự mở App ngay lập tức
            .setSound(ringtoneUri)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_CALL, notification)

        // Đồng thời khởi chạy Activity trực tiếp
        try {
            startActivity(callIntent)
        } catch (_: Exception) {}
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Kênh chạy ngầm
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                "Dịch vụ chuông cửa",
                NotificationManager.IMPORTANCE_LOW
            )
            nm.createNotificationChannel(serviceChannel)

            // Kênh cuộc gọi đến khẩn cấp (bật sáng màn hình và đổ chuông)
            val ringUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val callChannel = NotificationChannel(
                CHANNEL_CALL,
                "Cuộc gọi chuông cửa",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Bật màn hình và thông báo khi có người bấm chuông"
                setSound(ringUri, AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                )
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(callChannel)
        }
    }

    private fun createServiceNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_SERVICE)
            .setContentTitle("Chuông cửa ESP32")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateForegroundNotification(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID_SERVICE, createServiceNotification(text))
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        webSocket?.close(1000, "Service destroyed")
        wakeLock?.let { if (it.isHeld) it.release() }
    }
}

