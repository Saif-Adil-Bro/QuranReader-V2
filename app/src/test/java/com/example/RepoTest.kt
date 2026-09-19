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
        val repo = QuranRepository(
            api = retrofit2.Retrofit.Builder().baseUrl("http://localhost").build().create(com.example.data.api.QuranApi::class.java),
            quranComApi = retrofit2.Retrofit.Builder().baseUrl("http://localhost").build().create(com.example.data.api.QuranComApi::class.java),
            settingsRepository = com.example.data.repository.SettingsRepository(context),
            offlineDao = db.offlineQuranDao(),
            context = context
        )
        val ayahs = repo.getSurahDetailsCombined(2)
        println("Ayahs from repo: " + ayahs.size)
        assertTrue(ayahs.isNotEmpty())
    }
}
