package com.cs5520group15.memorycircle.ui.scrapbook

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.cs5520group15.memorycircle.ui.common.MemoryCircleTopBar
import com.cs5520group15.memorycircle.ui.theme.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape

/**
 * What: Displays the generated scrapbook with two views:
 *       1. HorizontalPager for template-based photo pages (swipe left/right)
 *       2. Timeline LazyColumn for scrolling through photos with captions
 * Who: Called by MemoryCircleNavigation after the user generates a scrapbook.
 * When: Navigated to when ScrapbookScreen's Generate button is tapped.
 */
@Composable
fun ScrapbookViewerScreen(
    groupId:   String,
    onBack:    () -> Unit,
    viewModel: ScrapbookViewModel = viewModel()
) {
    val pages by viewModel.pages.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Cream,
        topBar = {
            MemoryCircleTopBar(
                title    = "Memory Scrapbook",
                showBack = true,
                onBack   = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- Section 1: HorizontalPager (template pages) ---
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Text(
                        text     = "SCRAPBOOK PAGES",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = InkSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (pages.isEmpty()) {
                        // Empty state
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(GraySoft)
                        ) {
                            Text(
                                "No pages generated yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkTertiary
                            )
                        }
                    } else {
                        val pagerState = rememberPagerState(pageCount = { pages.size })

                        HorizontalPager(
                            state    = pagerState,
                            modifier = Modifier.fillMaxWidth()
                        ) { pageIndex ->
                            ScrapbookPageView(page = pages[pageIndex])
                        }

                        // Page indicator dots
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(pages.size) { index ->
                                Box(
                                    modifier = Modifier
                                        .size(if (pagerState.currentPage == index) 8.dp else 6.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (pagerState.currentPage == index) Brown
                                            else Beige
                                        )
                                )
                                if (index < pages.size - 1) Spacer(modifier = Modifier.width(6.dp))
                            }
                        }
                    }
                }
            }

            // --- Section 2: Timeline ---
            item {
                Text(
                    text     = "TIMELINE",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = InkSecondary,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            // Flatten all photos from all pages into a timeline
            val allPhotos = pages.flatMap { it.photos }
            itemsIndexed(allPhotos, key = { _, photo -> photo.id }) { index, photo ->
                TimelinePhotoItem(
                    photo  = photo,
                    isLast = index == allPhotos.lastIndex
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

/**
 * What: Renders a single scrapbook page as a photo grid based on the template type.
 * Who: Called by ScrapbookViewerScreen inside the HorizontalPager.
 * When: Rendered for each page in the pages list.
 */
@Composable
fun ScrapbookPageView(page: ScrapbookViewModel.ScrapbookPage) {
    val cols = when (page.template) {
        "grid6" -> 3
        else    -> 2
    }

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(GraySoft)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Date label
        Text(
            text     = page.date,
            style    = MaterialTheme.typography.bodyMedium,
            color    = InkSecondary,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Photo grid
        page.photos.chunked(cols).forEach { rowPhotos ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                rowPhotos.forEach { photo ->
                    AsyncImage(
                        model               = photo.url,
                        contentDescription  = photo.caption,
                        contentScale        = ContentScale.Crop,
                        modifier            = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
                // Fill empty slots if last row is incomplete
                repeat(cols - rowPhotos.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * What: Renders a single photo in a vertical timeline layout.
 *       Shows a vertical line on the left with a dot at each date,
 *       and the photo + caption on the right.
 * Who: Called by ScrapbookViewerScreen's LazyColumn for each photo.
 * When: Rendered for each photo across all scrapbook pages.
 */
@Composable
fun TimelinePhotoItem(
    photo: ScrapbookViewModel.PhotoItem,
    isLast: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        // --- Left side: vertical line + dot ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            // Dot on the timeline
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Brown)
            )
            // Vertical line below the dot
            // Hidden for the last item
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(220.dp)   // 200dp +
                        .background(Beige)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // --- Right side: date + photo + caption ---
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Date label
            Text(
                text = photo.date,
                style = MaterialTheme.typography.labelSmall,
                color = InkSecondary
            )
            // Photo
            AsyncImage(
                model = photo.url,
                contentDescription = photo.caption,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            // Caption (only shown if not empty)
            if (photo.caption.isNotBlank()) {
                Text(
                    text  = photo.caption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScrapbookViewerScreenPreview() {
    MemoryCircleTheme {
        ScrapbookViewerScreen(groupId = "1", onBack = {})
    }
}