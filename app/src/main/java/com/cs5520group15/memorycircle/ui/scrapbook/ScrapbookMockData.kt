package com.cs5520group15.memorycircle.ui.scrapbook

/**
 * What: A single memory entry in a monthly scrapbook timeline.
 *       Each scrapbook covers ONE month, so `date` is the day within that month.
 *       A group memory holds one photo per member, so `memberPhotos` has one
 *       image URL for each member of the group.
 * Who: Used by ScrapbookMockData and ScrapbookViewerScreen.
 * When: Instantiated when loading a scrapbook's timeline.
 */
data class ScrapbookEntry(
    val id:           String,
    val date:         String,       // day within the month, e.g. "June 1"
    val title:        String,
    val description:  String,
    val memberPhotos: List<String>, // one photo per group member
    val mood:         String         // e.g. "Happy", "Nostalgic", "Excited"
)

/**
 * What: Provides hardcoded scrapbook entries for development and previews.
 *       All entries belong to the same month (June 2025), sorted by day, and
 *       each entry contains one photo per group member to reflect a shared
 *       group memory. Firestore will replace this in a later phase.
 * Who: Called by ScrapbookViewerScreen in BuildConfig.DEBUG builds.
 * When: Used whenever a scrapbook timeline needs sample data.
 */
object ScrapbookMockData {

    // The fixed June 2025 entries, minus the per-member photos (added per call).
    private data class EntryTemplate(
        val id:          String,
        val date:        String,
        val title:       String,
        val description: String,
        val mood:        String
    )

    private val templates = listOf(
        EntryTemplate("1", "June 1", "First Day of Summer",
            "We kicked off the month with a picnic in the park, blankets spread out and snacks everywhere.",
            "Happy"),
        EntryTemplate("2", "June 5", "Garden Walk",
            "The flowers were blooming everywhere we looked, and the whole place smelled like spring.",
            "Peaceful"),
        EntryTemplate("3", "June 12", "Late Night Drive",
            "Drove around the city with no destination in mind, windows down and music up.",
            "Nostalgic"),
        EntryTemplate("4", "June 18", "Beach Day",
            "Sun, waves, and way too much sunscreen, but we wouldn't have had it any other way.",
            "Excited"),
        EntryTemplate("5", "June 24", "Rainy Afternoon",
            "Stayed in and watched old movies together while the rain tapped on the windows.",
            "Cozy"),
        EntryTemplate("6", "June 28", "Month Recap",
            "Can't believe how much happened this month. Already looking forward to the next one.",
            "Grateful")
    )

    /**
     * What: Returns the June 2025 sample entries, each carrying `memberCount`
     *       photos (one per member). Distinct picsum seeds keep every member's
     *       photo stable but different. Entries are sorted by day ascending.
     * Who: Called by ScrapbookViewerScreen.
     * When: On screen load in debug builds.
     *
     * @param groupId woven into the seeds so different groups get different images
     * @param memberCount how many member photos each entry should contain (1..6)
     */
    fun getMockEntries(groupId: String, memberCount: Int): List<ScrapbookEntry> {
        val safeCount = memberCount.coerceIn(1, 6)
        return templates
            .map { t ->
                ScrapbookEntry(
                    id           = t.id,
                    date         = t.date,
                    title        = t.title,
                    description  = t.description,
                    mood         = t.mood,
                    memberPhotos = (1..safeCount).map { member ->
                        "https://picsum.photos/seed/${groupId}_${t.id}_m$member/400/300"
                    }
                )
            }
            .sortedBy { it.date.removePrefix("June ").trim().toIntOrNull() ?: 0 }
    }
}
