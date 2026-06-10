package com.cs5520group15.memorycircle.ui.scrapbook

/**
 * What: A single memory entry in a monthly scrapbook timeline.
 *       Each scrapbook covers ONE month, so `date` is the day within that month.
 * Who: Used by ScrapbookMockData and ScrapbookViewerScreen.
 * When: Instantiated when loading a scrapbook's timeline.
 */
data class ScrapbookEntry(
    val id:          String,
    val date:        String,  // day within the month, e.g. "June 1"
    val title:       String,
    val description: String,
    val imageUrl:    String,
    val mood:        String    // e.g. "Happy", "Nostalgic", "Excited"
)

/**
 * What: Provides hardcoded scrapbook entries for development and previews.
 *       All entries belong to the same month (June 2025) and are sorted by
 *       day ascending. Firestore will replace this in a later phase.
 * Who: Called by ScrapbookViewerScreen in BuildConfig.DEBUG builds.
 * When: Used whenever a scrapbook timeline needs sample data.
 */
object ScrapbookMockData {

    /**
     * What: Returns 6 sample memory entries for June 2025, sorted by day.
     *       The groupId is woven into each picsum seed so different groups
     *       render different (but stable) images.
     * Who: Called by ScrapbookViewerScreen.
     * When: On screen load in debug builds.
     */
    fun getMockEntries(groupId: String): List<ScrapbookEntry> {
        return listOf(
            ScrapbookEntry(
                id          = "1",
                date        = "June 1",
                title       = "First Day of Summer",
                description = "We kicked off the month with a picnic in the park, blankets spread out and snacks everywhere.",
                imageUrl    = "https://picsum.photos/seed/${groupId}1/400/300",
                mood        = "Happy"
            ),
            ScrapbookEntry(
                id          = "2",
                date        = "June 5",
                title       = "Garden Walk",
                description = "The flowers were blooming everywhere we looked, and the whole place smelled like spring.",
                imageUrl    = "https://picsum.photos/seed/${groupId}2/400/300",
                mood        = "Peaceful"
            ),
            ScrapbookEntry(
                id          = "3",
                date        = "June 12",
                title       = "Late Night Drive",
                description = "Drove around the city with no destination in mind, windows down and music up.",
                imageUrl    = "https://picsum.photos/seed/${groupId}3/400/300",
                mood        = "Nostalgic"
            ),
            ScrapbookEntry(
                id          = "4",
                date        = "June 18",
                title       = "Beach Day",
                description = "Sun, waves, and way too much sunscreen, but we wouldn't have had it any other way.",
                imageUrl    = "https://picsum.photos/seed/${groupId}4/400/300",
                mood        = "Excited"
            ),
            ScrapbookEntry(
                id          = "5",
                date        = "June 24",
                title       = "Rainy Afternoon",
                description = "Stayed in and watched old movies together while the rain tapped on the windows.",
                imageUrl    = "https://picsum.photos/seed/${groupId}5/400/300",
                mood        = "Cozy"
            ),
            ScrapbookEntry(
                id          = "6",
                date        = "June 28",
                title       = "Month Recap",
                description = "Can't believe how much happened this month. Already looking forward to the next one.",
                imageUrl    = "https://picsum.photos/seed/${groupId}6/400/300",
                mood        = "Grateful"
            )
        ).sortedBy { it.date.removePrefix("June ").trim().toIntOrNull() ?: 0 }
    }
}
