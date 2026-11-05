package com.doorintercom.app

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okio.ByteString.Companion.toByteString
import org.json.JSONObject

class IncomingCallActivity : AppCompatActivity() {

    private lateinit var imgCamera: ImageView
    private lateinit var tvStatus: TextView
    private lateinit var btnTalk: Button
    private lateinit var btnHangup: Button
    private lateinit var btnLight: Button
    private lateinit var btnBuzzer: Button

    private var audioManager: AudioStreamManager? = null
    private var isLightOn = false
    private val autoCloseHandler = Handler(Looper.getMainLooper())
    private val autoCloseRunnable = Runnable { finish() }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. CẤU HÌNH BẬT SÁNG MÀN HÌNH & HIỂN THỊ TRÊN MÀN HÌNH KHÓA
        configureWakeScreen()

        setContentView(R.layout.activity_incoming_call)

        imgCamera = findViewById(R.id.imgCamera)
        tvStatus = findViewById(R.id.tvStatus)
        btnTalk = findViewById(R.id.btnTalk)
        btnHangup = findViewById(R.id.btnHangup)
        btnLight = findViewById(R.id.btnLight)
        btnBuzzer = findViewById(R.id.btnBuzzer)

        // Tự động tắt màn hình sau 45 giây nếu không có ai trả lời
        resetAutoCloseTimer()

        // Khởi tạo Audio Manager
        audioManager = AudioStreamManager { pcmBytes ->
            // Gửi âm thanh từ micro điện thoại về Server -> ESP32
            IntercomService.activeWebSocket?.send(pcmBytes.toByteString())
        }

        // Lắng nghe hình ảnh từ camera ESP32
        IntercomService.frameListener = { jpegBytes ->
            val bmp = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
            if (bmp != null) {
                runOnUiThread {
                    imgCamera.setImageBitmap(bmp)
                    tvStatus.text = "Trực tiếp"
                }
            }
        }

        // Lắng nghe tiếng từ micro ESP32 phát ra loa ngoài điện thoại
        IntercomService.audioListener = { pcmBytes ->
            audioManager?.playAudio(pcmBytes, 0, pcmBytes.size)
        }

        // 2. NÚT GIỮ ĐỂ NÓI (Push To Talk)
        btnTalk.setOnTouchListener { _, event ->
            resetAutoCloseTimer()
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    btnTalk.text = "Đang nói... 🔴"
                    btnTalk.setBackgroundColor(0xFFE53935.toInt())
                    audioManager?.startRecording()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    btnTalk.text = "Giữ Để Nói 🎙️"
                    btnTalk.setBackgroundColor(0xFF4CAF50.toInt())
                    audioManager?.stopRecording()
                    true
                }
                else -> false
            }
        }

        // 3. NÚT CÚP MÁY (Đóng app, tắt màn hình)
        btnHangup.setOnClickListener {
            finish()
        }

        // 4. NÚT BẬT/TẮT ĐÈN
        btnLight.setOnClickListener {
            resetAutoCloseTimer()
            isLightOn = !isLightOn
            btnLight.text = if (isLightOn) "Tắt Đèn" else "Bật Đèn"
            val msg = JSONObject().apply {
                put("t", "light")
                put("on", isLightOn)
            }
            IntercomService.activeWebSocket?.send(msg.toString())
        }

        // 5. NÚT BẤM CÒI
        btnBuzzer.setOnClickListener {
            resetAutoCloseTimer()
            val msg = JSONObject().apply {
                put("t", "buzzer")
                put("ms", 1000)
            }
            IntercomService.activeWebSocket?.send(msg.toString())
        }
    }

    private fun configureWakeScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            km.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun resetAutoCloseTimer() {
        autoCloseHandler.removeCallbacks(autoCloseRunnable)
        autoCloseHandler.postDelayed(autoCloseRunnable, 45000L) // 45 giây
    }

    override fun onDestroy() {
        super.onDestroy()
        autoCloseHandler.removeCallbacks(autoCloseRunnable)
        IntercomService.frameListener = null
        IntercomService.audioListener = null
        audioManager?.release()
        audioManager = null
    }
}

