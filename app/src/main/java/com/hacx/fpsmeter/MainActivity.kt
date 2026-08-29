package com.hacx.fpsmeter

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.OnRequestPermissionResultListener
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.Executors

class MainActivity : Activity(), OnRequestPermissionResultListener {

    private lateinit var tvFps: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvFrameTime: TextView
    private lateinit var btnStart: TextView
    private lateinit var btnStop: TextView
    private lateinit var progressBar: ProgressBar

    private var shizukuPermissionGranted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
        setupShizuku()
        updateButtonState()
    }

    override fun onResume() {
        super.onResume()
        updateButtonState()
    }

    private fun initViews() {
        tvFps = findViewById(R.id.tvFps)
        tvStatus = findViewById(R.id.tvStatus)
        tvFrameTime = findViewById(R.id.tvFrameTime)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        progressBar = findViewById(R.id.progressBar)

        btnStart.setOnClickListener {
            if (!shizukuPermissionGranted) {
                tvStatus.text = "Requesting Shizuku permission..."
                Shizuku.requestPermission(1001)
                return@setOnClickListener
            }
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:$packageName"))
                startActivity(intent)
                tvStatus.text = "Grant overlay permission"
                return@setOnClickListener
            }
            startOverlayService()
        }

        btnStop.setOnClickListener { stopOverlayService() }

        findViewById<TextView>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun updateButtonState() {
        val running = FpsOverlayService.isRunning
        btnStart.isEnabled = !running
        btnStop.isEnabled = running
        btnStart.alpha = if (running) 0.4f else 1.0f
        btnStop.alpha = if (running) 1.0f else 0.4f
        if (running) {
            tvStatus.text = "● Overlay active"
            tvStatus.setTextColor(0xFF3FB950.toInt())
            progressBar.visibility = ProgressBar.VISIBLE
        } else {
            progressBar.visibility = ProgressBar.GONE
        }
    }

    private fun setupShizuku() {
        try {
            Shizuku.addRequestPermissionResultListener(this)
            if (Shizuku.pingBinder()) {
                val result = Shizuku.checkSelfPermission()
                if (result == PackageManager.PERMISSION_GRANTED) {
                    shizukuPermissionGranted = true
                    tvStatus.text = "● Shizuku: Ready"
                    tvStatus.setTextColor(0xFF3FB950.toInt())
                } else {
                    tvStatus.text = "○ Tap START to grant permission"
                    tvStatus.setTextColor(0xFFF0883E.toInt())
                }
            } else {
                tvStatus.text = "Shizuku not running"
            }
        } catch (e: Exception) {
            tvStatus.text = "Shizuku error: ${e.message}"
        }
    }

    private fun startOverlayService() {
        val intent = Intent(this, FpsOverlayService::class.java)
        startForegroundService(intent)
        btnStart.isEnabled = false
        btnStop.isEnabled = true
        tvStatus.text = "Overlay active"
        progressBar.visibility = ProgressBar.VISIBLE
    }

    private fun stopOverlayService() {
        val intent = Intent(this, FpsOverlayService::class.java)
        stopService(intent)
        btnStart.isEnabled = true
        btnStop.isEnabled = false
        tvStatus.text = "Stopped"
        progressBar.visibility = ProgressBar.GONE
    }

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        if (requestCode == 1001) {
            shizukuPermissionGranted = (grantResult == PackageManager.PERMISSION_GRANTED)
            tvStatus.text = if (shizukuPermissionGranted) "Shizuku: Ready" else "Shizuku: Denied"
            updateButtonState()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            Shizuku.removeRequestPermissionResultListener(this)
        } catch (_: Exception) {}
    }
}
