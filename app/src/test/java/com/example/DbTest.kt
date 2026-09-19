package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.offline.OfflineQuranDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DbTest {
    @Test
    fun checkDb() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = OfflineQuranDatabase.getDatabase(context)
        val surahs = db.offlineQuranDao().getAllSurahs()
        println("SURAHS_COUNT: ${surahs.size}")
        assertEquals(114, surahs.size)
    }
}
