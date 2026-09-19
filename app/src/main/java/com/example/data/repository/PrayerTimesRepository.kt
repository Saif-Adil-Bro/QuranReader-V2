package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.DailyPrayerSchedule
import com.example.data.model.DistrictInfo
import com.example.utils.PrayerTimesCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

class PrayerTimesRepository(val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("prayer_times_prefs", Context.MODE_PRIVATE)

    private val _selectedDistrict = MutableStateFlow(loadSelectedDistrict())
    val selectedDistrict: StateFlow<DistrictInfo> = _selectedDistrict.asStateFlow()

    private val _isHanafi = MutableStateFlow(prefs.getBoolean("is_hanafi", true))
    val isHanafi: StateFlow<Boolean> = _isHanafi.asStateFlow()

    private val _todaySchedule = MutableStateFlow(calculateCurrentSchedule())
    val todaySchedule: StateFlow<DailyPrayerSchedule> = _todaySchedule.asStateFlow()

    private fun loadSelectedDistrict(): DistrictInfo {
        val districtId = prefs.getString("selected_district_id", "dhaka") ?: "dhaka"
        return PrayerTimesCalculator.findDistrictById(districtId)
    }

    fun setDistrict(district: DistrictInfo) {
        prefs.edit().putString("selected_district_id", district.id).apply()
        _selectedDistrict.value = district
        refreshSchedule()
        try {
            com.example.utils.PrayerNotificationHelper.scheduleNextPrayerAlarms(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setHanafi(hanafi: Boolean) {
        prefs.edit().putBoolean("is_hanafi", hanafi).apply()
        _isHanafi.value = hanafi
        refreshSchedule()
        try {
            com.example.utils.PrayerNotificationHelper.scheduleNextPrayerAlarms(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun refreshSchedule(date: LocalDate = LocalDate.now()) {
        _todaySchedule.value = PrayerTimesCalculator.calculatePrayerSchedule(
            date = date,
            district = _selectedDistrict.value,
            isHanafi = _isHanafi.value
        )
    }

    private fun calculateCurrentSchedule(): DailyPrayerSchedule {
        return PrayerTimesCalculator.calculatePrayerSchedule(
            date = LocalDate.now(),
            district = loadSelectedDistrict(),
            isHanafi = prefs.getBoolean("is_hanafi", true)
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: PrayerTimesRepository? = null

        fun getInstance(context: Context): PrayerTimesRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PrayerTimesRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
