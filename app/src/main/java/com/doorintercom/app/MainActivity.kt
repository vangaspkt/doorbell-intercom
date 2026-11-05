package com.doorintercom.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var etServerIp: EditText
    private lateinit var etServerPort: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnStartService: Button
    private lateinit var btnTestCall: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etServerIp = findViewById(R.id.etServerIp)
        etServerPort = findViewById(R.id.etServerPort)
        etPassword = findViewById(R.id.etPassword)
        btnStartService = findViewById(R.id.btnStartService)
        btnTestCall = findViewById(R.id.btnTestCall)

        val prefs = getSharedPreferences("intercom_prefs", Context.MODE_PRIVATE)
        etServerIp.setText(prefs.getString("server_ip", "192.168.1.100"))
        etServerPort.setText(prefs.getString("server_port", "3000"))
        etPassword.setText(prefs.getString("server_password", "1234"))

        requestRequiredPermissions()

        btnStartService.setOnClickListener {
            val ip = etServerIp.text.toString().trim()
            val port = etServerPort.text.toString().trim()
            val pw = etPassword.text.toString().trim()

            prefs.edit()
                .putString("server_ip", ip)
                .putString("server_port", port)
                .putString("server_password", pw)
                .apply()

            val serviceIntent = Intent(this, IntercomService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }

            Toast.makeText(this, "Đã khởi động dịch vụ chờ chuông ngầm!", Toast.LENGTH_SHORT).show()
        }

        btnTestCall.setOnClickListener {
            Toast.makeText(this, "Khoá màn hình ngay bây giờ để kiểm tra sau 3 giây!", Toast.LENGTH_SHORT).show()
            window.decorView.postDelayed({
                val intent = Intent(this, IncomingCallActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }, 3000L)
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 101)
        }
    }
}

