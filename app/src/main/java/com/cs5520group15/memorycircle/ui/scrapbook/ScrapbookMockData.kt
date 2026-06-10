package com.cs5520group15.memorycircle.ui.scrapbook

/**
 * What: One group member's contribution to a memory time point — their own photo
 *       and their own one-line description. Because every member sees the day
 *       differently, a single time point holds many of these (one per person who
 *       joined it).
 * Who: Used by ScrapbookEntry, ScrapbookRepository, and the scrapbook screens.
 * When: Created when a member adds their photo + words to a time point.
 */
data class MemberContribution(
    val memberName:  String,   // drives avatar initial + label
    val photoUri:    String,   // content:// (album pick) or remote URL (mock)
    val description: String     // this member's own line about the day
)

/**
 * What: A single memory "time point" in a group's timeline. The first member to
 *       create it sets the `title` and `tags`; everyone else joins by appending a
 *       `MemberContribution`. Each scrapbook covers ONE month, so `date` is the
 *       day within that month.
 * Who: Used by ScrapbookMockData, ScrapbookRepository, and the scrapbook screens.
 * When: Instantiated when loading or creating a timeline entry.
 */
data class ScrapbookEntry(
    val id:            String,
    val date:          String,                 // day within the month, e.g. "June 1"
    val title:         String,                 // set by the first creator
    val tags:          List<String> = emptyList(),
    val contributions: List<MemberContribution> = emptyList(),
    val comments:      List<Comment> = emptyList()
)

/**
 * What: A short comment a group member leaves on a memory, expressing how they
 *       felt about it (Xiaohongshu-style group comments).
 * Who: Used by ScrapbookEntry, ScrapbookRepository, and ScrapbookViewerScreen.
 * When: Created when a member posts a comment.
 */
data class Comment(
    val id:     String,
    val author: String,
    val text:   String
)

/**
 * What: Provides hardcoded scrapbook entries for development and previews.
 *       All entries belong to the same month (June 2025), sorted by day, and
 *       each entry carries one contribution per mock member (photo + their own
 *       description). Firestore will replace this in a later phase.
 * Who: Called by ScrapbookRepository when seeding a group's timeline.
 * When: Used whenever a scrapbook timeline needs sample data.
 */
object ScrapbookMockData {

    // The fixed June 2025 entries, minus the per-member contributions (added per call).
    private data class EntryTemplate(
        val id:    String,
        val date:  String,
        val title: String,
        val tags:  List<String>
    )

    private val templates = listOf(
        EntryTemplate("1", "June 1",  "First Day of Summer", listOf("#summer", "#picnic")),
        EntryTemplate("2", "June 5",  "Garden Walk",         listOf("#flowers")),
        EntryTemplate("3", "June 12", "Late Night Drive",    listOf("#music", "#night")),
        EntryTemplate("4", "June 18", "Beach Day",           listOf("#beach", "#sun")),
        EntryTemplate("5", "June 24", "Rainy Afternoon",     listOf("#cozy")),
        EntryTemplate("6", "June 28", "Month Recap",         listOf("#recap"))
    )

    // Mock members of every group, plus a stock line each leaves on a time point.
    // Different members get different photos, so every contribution looks distinct.
    private val mockMembers = listOf(
        "Sarah" to "Loved every second of this.",
        "Mia"   to "Can't stop smiling at these.",
        "Alex"  to "Wish the day lasted longer."
    )

    // Sample comments per entry id, so the comment feature is visible on load.
    private val seedComments: Map<String, List<Comment>> = mapOf(
        "1" to listOf(
            Comment("1c1", "Mia",  "Best picnic ever 🧺"),
            Comment("1c2", "Alex", "Already miss this day")
        ),
        "3" to listOf(
            Comment("3c1", "Sam", "That playlist was unreal 🎶")
        )
    )

    /**
     * What: Returns the June 2025 sample entries, each carrying one contribution
     *       per mock member. Distinct picsum seeds keep every member's photo
     *       stable but different. Entries are sorted by day ascending.
     * Who: Called by ScrapbookRepository when first seeding a group.
     * When: On first access of a group's timeline in debug builds.
     *
     * @param groupId woven into the seeds so different groups get different images
     */
    fun getMockEntries(groupId: String): List<ScrapbookEntry> {
        return templates
            .map { t ->
                ScrapbookEntry(
                    id            = t.id,
                    date          = t.date,
                    title         = t.title,
                    tags          = t.tags,
                    contributions = mockMembers.map { (name, line) ->
                        MemberContribution(
                            memberName  = name,
                            photoUri    = "https://picsum.photos/seed/${groupId}_${t.id}_$name/400/300",
                            description = line
                        )
                    },
                    comments      = seedComments[t.id] ?: emptyList()
                )
            }
            .sortedBy { it.date.removePrefix("June ").trim().toIntOrNull() ?: 0 }
    }
}
