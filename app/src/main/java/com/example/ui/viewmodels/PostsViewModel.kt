package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BlogPost
import com.example.data.model.ShortPost
import com.example.data.repository.PostsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PostsViewModel(private val postsRepository: PostsRepository) : ViewModel() {

    val rawBlogPosts = postsRepository.blogPosts
    val rawShortPosts = postsRepository.shortPosts
    val isLoading = postsRepository.isLoading

    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("সকল")
    val pendingPhotoCardPost = MutableStateFlow<ShortPost?>(null)
    val pendingBlogPost = MutableStateFlow<BlogPost?>(null)

    fun setPendingPhotoCardPost(post: ShortPost?) {
        pendingPhotoCardPost.value = post
    }

    /**
     * Quick Create Architecture entry point:
     * Directly launches Photo Card Maker with an Ayah pre-filled and styled.
     */
    fun openPhotoCardFromAyah(ayah: com.example.data.model.CombinedAyah, surahName: String) {
        val payload = com.example.utils.PhotoCardBridge.fromAyah(ayah, surahName)
        pendingPhotoCardPost.value = payload.toShortPost()
    }

    /**
     * Quick Create Architecture entry point:
     * Directly launches Photo Card Maker with a Dua pre-filled and styled.
     */
    fun openPhotoCardFromDua(dua: com.example.data.DuaItem) {
        val payload = com.example.utils.PhotoCardBridge.fromDua(dua)
        pendingPhotoCardPost.value = payload.toShortPost()
    }

    /**
     * Quick Create Architecture entry point:
     * Directly launches Photo Card Maker with a Subjectwise verse pre-filled.
     */
    fun openPhotoCardFromSubjectwise(verse: com.example.data.SubjectwiseVerse, categoryName: String) {
        val payload = com.example.utils.PhotoCardBridge.fromSubjectwiseVerse(verse, categoryName)
        pendingPhotoCardPost.value = payload.toShortPost()
    }

    /**
     * Launch Photo Card from generic QuickCardPayload.
     */
    fun openPhotoCardFromPayload(payload: com.example.utils.PhotoCardBridge.QuickCardPayload) {
        pendingPhotoCardPost.value = payload.toShortPost()
    }

    fun setPendingBlogPost(post: BlogPost?) {
        pendingBlogPost.value = post
    }

    val filteredBlogPosts: StateFlow<List<BlogPost>> = combine(
        rawBlogPosts,
        searchQuery,
        selectedCategory
    ) { posts, query, category ->
        posts.filter { post ->
            val matchesCategory = (category == "সকল" || post.category == category)
            val matchesQuery = query.isEmpty() ||
                    post.title.contains(query, ignoreCase = true) ||
                    post.content.contains(query, ignoreCase = true) ||
                    post.category.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredShortPosts: StateFlow<List<ShortPost>> = combine(
        rawShortPosts,
        searchQuery,
        selectedCategory
    ) { posts, query, category ->
        posts.filter { post ->
            val matchesCategory = (category == "সকল" || post.category == category)
            val matchesQuery = query.isEmpty() ||
                    post.text.contains(query, ignoreCase = true) ||
                    post.reference.contains(query, ignoreCase = true) ||
                    post.category.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addBlogPost(title: String, content: String, category: String, author: String, imageUrl: String = "", onSuccess: () -> Unit, onError: (String) -> Unit) {
        postsRepository.addBlogPost(title, content, category, author, imageUrl, onSuccess, onError)
    }

    fun addShortPost(text: String, reference: String, category: String, author: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        postsRepository.addShortPost(text, reference, category, author, onSuccess, onError)
    }

    fun refresh(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                kotlinx.coroutines.withTimeoutOrNull(3500L) {
                    kotlinx.coroutines.suspendCancellableCoroutine<Unit> { cont ->
                        postsRepository.refresh {
                            if (cont.isActive) cont.resume(Unit) {}
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete?.invoke()
                }
            }
        }
    }
}
