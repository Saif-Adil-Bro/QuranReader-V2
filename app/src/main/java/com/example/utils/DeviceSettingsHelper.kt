package com.example.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast

object DeviceSettingsHelper {

    /**
     * Check if battery optimization is disabled (ignoring battery optimizations)
     */
    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            return powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
        }
        return true
    }

    /**
     * Request battery optimization ignore or open battery optimization settings
     */
    @SuppressLint("BatteryLife")
    fun openBatteryOptimizationSettings(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!isBatteryOptimizationIgnored(context)) {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    return
                }
            }
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return
            } catch (ex: Exception) {
                openAppDetailsSettings(context)
            }
        }
    }

    /**
     * Open App Settings / App Info page (where user can allow AutoStart, Lock screen notifications, Pop-ups)
     */
    fun openAppDetailsSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "সেটিংস খোলা যায়নি", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Check if special exact alarm permission is granted on Android 12+ (API 31+)
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
            return alarmManager?.canScheduleExactAlarms() == true
        }
        return true
    }

    /**
     * Open exact alarm settings for Android 12+
     */
    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                openAppDetailsSettings(context)
            }
        }
    }

    /**
     * Check manufacturer and give brand-specific tips
     */
    fun getDeviceBrandTip(): String {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> {
                "Xiaomi/Redmi/Poco: সেটিংস > Apps > Manage Apps > QawmiManager এ গিয়ে 'Autostart' চালু করুন এবং 'Other permissions' থেকে 'Show on Lock screen' ইনেবল করুন।"
            }
            manufacturer.contains("samsung") -> {
                "Samsung: সেটিংস > Apps > QawmiManager > Battery অপশনে গিয়ে 'Unrestricted' সিলেক্ট করুন।"
            }
            manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> {
                "Vivo/iQOO: Settings > Battery > High background power consumption এ QawmiManager চালু করুন।"
            }
            manufacturer.contains("oppo") || manufacturer.contains("realme") || manufacturer.contains("oneplus") -> {
                "Oppo/Realme/OnePlus: App Info > Battery usage > 'Allow background activity' এবং 'Allow auto-launch' চালু করুন।"
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                "Huawei/Honor: Settings > Battery > App launch এ গিয়ে Manual এ 'Auto-launch', 'Secondary launch' এবং 'Run in background' চালু করুন।"
            }
            else -> {
                "লক স্ক্রিনে নিশ্চিতভাবে অ্যালার্ম পেতে অ্যাপ ইনফো থেকে ব্যাটারি অপটিমাইজেশন 'No restrictions' এবং লক স্ক্রিন পারমিশন দিয়ে রাখুন।"
            }
        }
    }
}
