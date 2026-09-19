package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.model.ShortPost
import com.example.utils.PostShareUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PostNotificationShareReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val id = intent.getStringExtra("post_id") ?: ""
        val text = intent.getStringExtra("post_text") ?: ""
        val ref = intent.getStringExtra("post_ref") ?: ""
        val category = intent.getStringExtra("post_category") ?: ""
        val author = intent.getStringExtra("post_author") ?: ""

        if (text.isBlank()) return

        val post = ShortPost(
            id = id,
            text = text,
            reference = ref,
            category = category,
            author = author
        )

        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            PostShareUtil.shareAsImage(appContext, post)
        }
    }
}
