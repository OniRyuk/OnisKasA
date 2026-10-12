package com.kasa.diagnostic

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.os.Build
import android.os.UserManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.hardware.usb.UsbManager
import android.provider.Settings
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var report: TextView
    private var lastReport = ""
    private val background = Color.rgb(12, 12, 15)
    private val accent = Color.rgb(188, 24, 40)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = background
        window.navigationBarColor = background
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 36, 28, 36)
            setBackgroundColor(background)
        }
        setContentView(ScrollView(this).apply { addView(root) })
        label("KasA", 30, accent)
        label("SETTINGS DIAGNOSTIC • 0.2 KOTLIN", 14, Color.LTGRAY)
        label("USB / OEM inspection", 22, Color.WHITE)
        label("Read-only checks; restricted Samsung settings cannot be forced on.", 14, Color.LTGRAY)
        button("Run USB diagnostics") { diagnose() }
        button("USB preferences") { open("android.settings.USB_SETTINGS") }
        button("Developer options") { open(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS) }
        button("OEM unlocking information") { oem() }
        button("Device administrators") { open(Settings.ACTION_DEVICE_ADMIN_SETTINGS) }
        button("General settings") { open(Settings.ACTION_SETTINGS) }
        button("Copy diagnostic report") {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("KasA diagnostics", lastReport))
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
        }
        report = label("Run diagnostics to inspect available USB signals.", 14, Color.WHITE)
    }

    private fun label(value: String, size: Int, color: Int): TextView =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            setPadding(0, 10, 0, 12)
            root.addView(this)
        }

    private fun button(value: String, action: () -> Unit) {
        val button = Button(this).apply {
            text = value
            isAllCaps = false
            setTextColor(Color.WHITE)
            backgroundTintList = ColorStateList.valueOf(accent)
            setOnClickListener { action() }
        }
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 6, 0, 6) }
        root.addView(button, params)
    }

    private fun open(action: String) {
        try {
            startActivity(Intent(action))
        } catch (_: Exception) {
            AlertDialog.Builder(this)
                .setMessage("This settings screen is unavailable or restricted on your device.")
                .setPositiveButton("OK", null).show()
        }
    }

    private fun oem() {
        val restricted = try {
            val manager = getSystemService(USER_SERVICE) as UserManager
            manager.hasUserRestriction(UserManager.DISALLOW_FACTORY_RESET)
        } catch (_: Exception) { false }
        val message = "Android does not expose a reliable public API for OEM unlocking status. " +
            (if (restricted) "Factory reset is restricted by device policy. " else "") +
            "Check Developer Options. This app cannot bypass manufacturer or carrier restrictions."
        AlertDialog.Builder(this).setTitle("OEM Unlocking")
            .setMessage(message).setNegativeButton("Close", null)
            .setPositiveButton("Developer Options") { _, _ ->
                open(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            }.show()
    }

    private fun diagnose() {
        val output = StringBuilder("KasA Settings Diagnostic v0.2\n")
            .append("Device: ").append(Build.MANUFACTURER).append(" ")
            .append(Build.MODEL).append("\nAndroid: ").append(Build.VERSION.RELEASE).append("\n")
        val usb = getSystemService(USB_SERVICE) as? UsbManager
        if (usb == null) {
            output.append("USB service unavailable\n")
        } else {
            try {
                output.append("Visible USB host devices: ").append(usb.deviceList.size).append("\n")
            } catch (_: Exception) {
                output.append("USB host inspection unavailable\n")
            }
        }
        try {
            val enabled = Settings.Global.getInt(contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
            output.append("ADB setting: ").append(if (enabled) "ON" else "OFF").append("\n")
        } catch (_: Exception) {
            output.append("ADB state unreadable\n")
        }
        output.append("OEM state: unavailable through public API\n")
        output.append("USB host count does not indicate a PC connection.\n")
        output.append("A flickering USB toggle may involve cable, port, negotiation, system UI, or policy.")
        lastReport = output.toString()
        report.text = lastReport
    }
}
