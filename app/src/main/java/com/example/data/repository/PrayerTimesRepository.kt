package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.DailyPrayerSchedule
import com.example.data.model.DistrictInfo
import com.example.data.model.PrayerCalculationMethod
import com.example.utils.PrayerTimesCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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

    private val _calculationMethod = MutableStateFlow(loadCalculationMethod())
    val calculationMethod: StateFlow<PrayerCalculationMethod> = _calculationMethod.asStateFlow()

    private val _sahriOffset = MutableStateFlow(loadSahriOffset())
    val sahriOffset: StateFlow<Int> = _sahriOffset.asStateFlow()

    private val _iftarOffset = MutableStateFlow(loadIftarOffset())
    val iftarOffset: StateFlow<Int> = _iftarOffset.asStateFlow()

    private val _todaySchedule = MutableStateFlow(calculateCurrentSchedule())
    val todaySchedule: StateFlow<DailyPrayerSchedule> = _todaySchedule.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val settingsRepo = SettingsRepository.getInstance(context)
                launch {
                    settingsRepo.sahriOffsetFlow.collect { offset ->
                        if (_sahriOffset.value != offset) {
                            prefs.edit().putInt("sahri_offset", offset).apply()
                            _sahriOffset.value = offset
                            refreshSchedule()
                        }
                    }
                }
                launch {
                    settingsRepo.iftarOffsetFlow.collect { offset ->
                        if (_iftarOffset.value != offset) {
                            prefs.edit().putInt("iftar_offset", offset).apply()
                            _iftarOffset.value = offset
                            refreshSchedule()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadSelectedDistrict(): DistrictInfo {
        val districtId = prefs.getString("selected_district_id", "dhaka") ?: "dhaka"
        return PrayerTimesCalculator.findDistrictById(districtId)
    }

    private fun loadCalculationMethod(): PrayerCalculationMethod {
        val methodId = prefs.getString("calculation_method_id", PrayerCalculationMethod.KARACHI.id)
        return PrayerCalculationMethod.fromId(methodId)
    }

    private fun loadSahriOffset(): Int {
        return prefs.getInt("sahri_offset", -3)
    }

    private fun loadIftarOffset(): Int {
        return prefs.getInt("iftar_offset", 0)
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

    fun setCalculationMethod(method: PrayerCalculationMethod) {
        prefs.edit().putString("calculation_method_id", method.id).apply()
        _calculationMethod.value = method
        refreshSchedule()
        try {
            com.example.utils.PrayerNotificationHelper.scheduleNextPrayerAlarms(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setSahriOffset(offset: Int) {
        prefs.edit().putInt("sahri_offset", offset).apply()
        _sahriOffset.value = offset
        refreshSchedule()
        try {
            com.example.utils.PrayerNotificationHelper.scheduleNextPrayerAlarms(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setIftarOffset(offset: Int) {
        prefs.edit().putInt("iftar_offset", offset).apply()
        _iftarOffset.value = offset
        refreshSchedule()
        try {
            com.example.utils.PrayerNotificationHelper.scheduleNextPrayerAlarms(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun refreshSchedule(date: LocalDate? = null) {
        val district = _selectedDistrict.value
        val zoneId = try { java.time.ZoneId.of(district.timeZoneId) } catch (e: Exception) { java.time.ZoneId.of("Asia/Dhaka") }
        val targetDate = date ?: LocalDate.now(zoneId)
        _todaySchedule.value = PrayerTimesCalculator.calculatePrayerSchedule(
            date = targetDate,
            district = district,
            isHanafi = _isHanafi.value,
            calculationMethod = _calculationMethod.value,
            sahriOffsetMinutes = _sahriOffset.value,
            iftarOffsetMinutes = _iftarOffset.value
        )
    }

    private fun calculateCurrentSchedule(): DailyPrayerSchedule {
        val district = loadSelectedDistrict()
        val zoneId = try { java.time.ZoneId.of(district.timeZoneId) } catch (e: Exception) { java.time.ZoneId.of("Asia/Dhaka") }
        return PrayerTimesCalculator.calculatePrayerSchedule(
            date = LocalDate.now(zoneId),
            district = district,
            isHanafi = prefs.getBoolean("is_hanafi", true),
            calculationMethod = loadCalculationMethod(),
            sahriOffsetMinutes = loadSahriOffset(),
            iftarOffsetMinutes = loadIftarOffset()
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
