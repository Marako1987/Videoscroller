package com.example.tiktokcontroller

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnEnableAccessibility: Button
    private lateinit var btnIgnoreBattery: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tv_status)
        btnEnableAccessibility = findViewById(R.id.btn_enable_accessibility)
        btnIgnoreBattery = findViewById(R.id.btn_ignore_battery)

        btnEnableAccessibility.setOnClickListener {
            // Führt den Nutzer direkt in die Android Barrierefreiheits-Einstellungen
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        btnIgnoreBattery.setOnClickListener {
            requestIgnoreBatteryOptimization()
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val isEnabled = isAccessibilityServiceEnabled(
            this,
            TikTokHeadsetAccessibilityService::class.java
        )

        if (isEnabled) {
            tvStatus.text = "Status: AKTIV (Dienst läuft und hört auf Kopfhörer-Tasten)"
            tvStatus.setTextColor(0xFF10B981.toInt()) // Grün
            btnEnableAccessibility.text = "In Einstellungen verwalten"
        } else {
            tvStatus.text = "Status: INAKTIV (Barrierefreiheit muss aktiviert werden)"
            tvStatus.setTextColor(0xFFEF4444.toInt()) // Rot
            btnEnableAccessibility.text = "Barrierefreiheit jetzt aktivieren"
        }
    }

    private fun requestIgnoreBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<*>): Boolean {
        val expectedComponentName = "${context.packageName}/${serviceClass.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(expectedComponentName)
    }
}
