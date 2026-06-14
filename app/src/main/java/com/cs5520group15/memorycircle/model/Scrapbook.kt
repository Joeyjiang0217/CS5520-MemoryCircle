package com.cs5520group15.memorycircle.model

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
    val description: String    // this member's own line about the day
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
