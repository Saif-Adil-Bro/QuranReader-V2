package com.example.data.model

import androidx.annotation.Keep

@Keep
data class BlogPost(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val author: String = "ইসলামিক এডমিন",
    val category: String = "সাধারণ",
    val imageUrl: String = "",
    val readTime: String = "৩ মিনিট",
    val timestamp: Long = System.currentTimeMillis()
)

@Keep
data class ShortPost(
    val id: String = "",
    val text: String = "",
    val reference: String = "",
    val category: String = "দৈনিক নসীহত",
    val author: String = "ইসলামিক স্কলার",
    val timestamp: Long = System.currentTimeMillis()
)
