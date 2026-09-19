package com.example
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.offline.OfflineQuranDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue

@RunWith(RobolectricTestRunner::class)
class DaoTest {
    @Test
    fun testDao() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = OfflineQuranDatabase.getDatabase(context)
        val list = db.offlineQuranDao().getAyahsByPage(3)
        println("Found ayahs: " + list.size)
        assertTrue(list.isNotEmpty())
    }
}
