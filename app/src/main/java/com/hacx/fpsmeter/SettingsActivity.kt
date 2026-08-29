package com.hacx.fpsmeter

import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

class SettingsActivity : Activity() {

    companion object {
        const val PREFS_NAME = "fps_meter_settings"
        const val KEY_SHOW_BG = "show_background"
        const val KEY_SHOW_FRAMETIME = "show_frame_time"
        const val KEY_KEEP_SCREEN = "keep_screen_on"
        const val KEY_COLOR = "overlay_color"
        const val KEY_TEXT_SIZE = "text_size"
        const val KEY_SHOW_NOTIFICATION = "show_notification"
        const val KEY_SHOW_REFRESH_RATE = "show_refresh_rate"

        const val COLOR_GREEN = "#3FB950"
        const val COLOR_BLUE = "#58A6FF"
        const val COLOR_RED = "#F85149"
        const val COLOR_YELLOW = "#D29922"
    }

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        val swBg = findViewById<Switch>(R.id.swBackground)
        val swFt = findViewById<Switch>(R.id.swFrameTime)
        val swScreen = findViewById<Switch>(R.id.swKeepScreen)
        val swNotif = findViewById<Switch>(R.id.swNotification)
        val swRefresh = findViewById<Switch>(R.id.swRefreshRate)
        val seekSize = findViewById<SeekBar>(R.id.seekTextSize)
        val tvSizeValue = findViewById<TextView>(R.id.tvTextSizeValue)

        val colorGreen = findViewById<TextView>(R.id.colorGreen)
        val colorBlue = findViewById<TextView>(R.id.colorBlue)
        val colorRed = findViewById<TextView>(R.id.colorRed)
        val colorYellow = findViewById<TextView>(R.id.colorYellow)

        swBg.isChecked = prefs.getBoolean(KEY_SHOW_BG, true)
        swFt.isChecked = prefs.getBoolean(KEY_SHOW_FRAMETIME, true)
        swScreen.isChecked = prefs.getBoolean(KEY_KEEP_SCREEN, false)
        swNotif.isChecked = prefs.getBoolean(KEY_SHOW_NOTIFICATION, true)
        swRefresh.isChecked = prefs.getBoolean(KEY_SHOW_REFRESH_RATE, false)

        val savedSize = prefs.getInt(KEY_TEXT_SIZE, 20)
        seekSize.progress = savedSize - 12
        tvSizeValue.text = "${savedSize}sp"

        val savedColor = prefs.getString(KEY_COLOR, COLOR_GREEN) ?: COLOR_GREEN
        updateColorSelection(colorGreen, colorBlue, colorRed, colorYellow, savedColor)

        swBg.setOnCheckedChangeListener { _, v -> prefs.edit().putBoolean(KEY_SHOW_BG, v).apply() }
        swFt.setOnCheckedChangeListener { _, v -> prefs.edit().putBoolean(KEY_SHOW_FRAMETIME, v).apply() }
        swScreen.setOnCheckedChangeListener { _, v -> prefs.edit().putBoolean(KEY_KEEP_SCREEN, v).apply() }
        swNotif.setOnCheckedChangeListener { _, v -> prefs.edit().putBoolean(KEY_SHOW_NOTIFICATION, v).apply() }
        swRefresh.setOnCheckedChangeListener { _, v -> prefs.edit().putBoolean(KEY_SHOW_REFRESH_RATE, v).apply() }

        seekSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                val size = progress + 12
                tvSizeValue.text = "${size}sp"
                prefs.edit().putInt(KEY_TEXT_SIZE, size).apply()
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        val colorClick = { view: TextView, color: String ->
            view.setOnClickListener {
                prefs.edit().putString(KEY_COLOR, color).apply()
                updateColorSelection(colorGreen, colorBlue, colorRed, colorYellow, color)
            }
        }
        colorClick(colorGreen, COLOR_GREEN)
        colorClick(colorBlue, COLOR_BLUE)
        colorClick(colorRed, COLOR_RED)
        colorClick(colorYellow, COLOR_YELLOW)
    }

    private fun updateColorSelection(green: TextView, blue: TextView, red: TextView, yellow: TextView, selected: String) {
        green.alpha = if (selected == COLOR_GREEN) 1.0f else 0.3f
        blue.alpha = if (selected == COLOR_BLUE) 1.0f else 0.3f
        red.alpha = if (selected == COLOR_RED) 1.0f else 0.3f
        yellow.alpha = if (selected == COLOR_YELLOW) 1.0f else 0.3f
    }
}
