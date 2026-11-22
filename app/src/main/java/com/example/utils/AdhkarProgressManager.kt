package com.example.utils

import android.content.Context
import com.example.data.DuaItem
import com.example.data.MorningEveningSection
import java.time.LocalDate

/**
 * Lightweight SharedPreferences-backed tracker for daily Morning & Evening Adhkar completion.
 * - Automatically resets all counts whenever the calendar date changes.
 * - Scopes keys by section ("morning" vs "evening") so shared duas are tracked independently.
 */
object AdhkarProgressManager {
    private const val PREFS_NAME = "morning_evening_adhkar_progress"
    private const val KEY_LAST_DATE = "last_date"

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun ensureCurrentDate(context: Context) {
        val prefs = getPrefs(context)
        val today = LocalDate.now().toString()
        val lastDate = prefs.getString(KEY_LAST_DATE, null)
        if (lastDate != today) {
            prefs.edit().clear().putString(KEY_LAST_DATE, today).apply()
        }
    }

    private fun keyFor(section: MorningEveningSection, duaId: Int): String =
        "${section.key}_$duaId"

    fun getCount(context: Context, section: MorningEveningSection, duaId: Int): Int {
        ensureCurrentDate(context)
        return getPrefs(context).getInt(keyFor(section, duaId), 0)
    }

    fun getCountsForSection(
        context: Context,
        section: MorningEveningSection,
        duas: List<DuaItem>
    ): Map<Int, Int> {
        ensureCurrentDate(context)
        val prefs = getPrefs(context)
        return duas.associate { dua ->
            dua.id to prefs.getInt(keyFor(section, dua.id), 0)
        }
    }

    fun incrementCount(
        context: Context,
        section: MorningEveningSection,
        duaId: Int,
        targetCount: Int
    ): Int {
        ensureCurrentDate(context)
        val safeTarget = targetCount.coerceAtLeast(1)
        val current = getCount(context, section, duaId)
        val next = (current + 1).coerceAtMost(safeTarget)
        getPrefs(context).edit().putInt(keyFor(section, duaId), next).apply()
        return next
    }

    fun toggleComplete(
        context: Context,
        section: MorningEveningSection,
        duaId: Int,
        targetCount: Int
    ): Int {
        ensureCurrentDate(context)
        val safeTarget = targetCount.coerceAtLeast(1)
        val current = getCount(context, section, duaId)
        val next = if (current >= safeTarget) 0 else safeTarget
        getPrefs(context).edit().putInt(keyFor(section, duaId), next).apply()
        return next
    }

    fun resetDua(context: Context, section: MorningEveningSection, duaId: Int) {
        ensureCurrentDate(context)
        getPrefs(context).edit().remove(keyFor(section, duaId)).apply()
    }

    fun resetSection(context: Context, section: MorningEveningSection, duas: List<DuaItem>) {
        ensureCurrentDate(context)
        val editor = getPrefs(context).edit()
        duas.forEach { editor.remove(keyFor(section, it.id)) }
        editor.apply()
    }

    fun getCompletedCount(
        context: Context,
        section: MorningEveningSection,
        duas: List<DuaItem>
    ): Int {
        ensureCurrentDate(context)
        val prefs = getPrefs(context)
        return duas.count { dua ->
            prefs.getInt(keyFor(section, dua.id), 0) >= dua.recommendedCount.coerceAtLeast(1)
        }
    }
}
