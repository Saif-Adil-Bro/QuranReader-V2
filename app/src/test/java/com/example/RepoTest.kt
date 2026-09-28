package com.example
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.offline.OfflineQuranDatabase
import com.example.data.repository.QuranRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import android.content.Context
import org.robolectric.shadows.ShadowNetworkInfo
import android.net.ConnectivityManager
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class RepoTest {
    @Test
    fun testRepo() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = OfflineQuranDatabase.getDatabase(context)
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        shadowOf(cm).setActiveNetworkInfo(null)
        val wbwDb = com.example.data.local.offline.QuranWbwDatabase.getDatabase(context)
        val repo = QuranRepository(
            api = retrofit2.Retrofit.Builder().baseUrl("http://localhost").build().create(com.example.data.api.QuranApi::class.java),
            quranComApi = retrofit2.Retrofit.Builder().baseUrl("http://localhost").build().create(com.example.data.api.QuranComApi::class.java),
            settingsRepository = com.example.data.repository.SettingsRepository(context),
            offlineDao = db.offlineQuranDao(),
            quranWbwDao = wbwDb.quranWbwDao(),
            context = context
        )
        try {
            val dbAyahs = db.offlineQuranDao().getAyahsBySurah(2)
            println("Direct DB Ayahs count: " + dbAyahs.size)
        } catch (e: Throwable) {
            println("DIRECT DB ERROR: " + e)
            e.printStackTrace()
        }
        val ayahs = repo.getSurahDetailsCombined(2)
        println("Ayahs from repo: " + ayahs.size)
        assertTrue(ayahs.isNotEmpty())
    }
}
