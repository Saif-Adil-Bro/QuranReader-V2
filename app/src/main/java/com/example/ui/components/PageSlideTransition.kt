package com.example.ui.components

import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import kotlin.math.abs

/**
 * Custom smooth slide page transition modifier for Quran Reader pages.
 * Provides a natural sheet/page slide effect with subtle depth scale and elevation alpha.
 */
fun Modifier.quranPageSlideTransition(
    pagerState: PagerState,
    page: Int
): Modifier = this.graphicsLayer {
    // Calculate how far this page is from the currently active page position
    val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
    val absOffset = abs(pageOffset).coerceIn(0f, 1f)

    // Smooth scale reduction for inactive pages to create subtle depth
    val scale = lerp(
        start = 1.0f,
        stop = 0.94f,
        fraction = absOffset
    )
    scaleX = scale
    scaleY = scale

    // Smooth alpha fade for adjacent pages
    alpha = lerp(
        start = 1.0f,
        stop = 0.65f,
        fraction = absOffset
    )

    // Smooth parallax slide translation
    translationX = pageOffset * (size.width * 0.08f)
}
