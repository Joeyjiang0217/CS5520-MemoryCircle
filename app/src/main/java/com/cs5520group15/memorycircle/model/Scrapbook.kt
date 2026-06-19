package com.cs5520group15.memorycircle.model

/**
 * What: A single photo within a scrapbook post. Multiple photos per post are
 *       supported — the post's original author lays down the first one, and
 *       teammates can later "join" the post to append theirs.
 * Who: Used by ScrapbookEntry, ScrapbookRepository, and the scrapbook screens.
 * When: Created when a member uploads a photo to a new or existing post.
 *
 * Firestore note: only `uploaderId` is persisted — uploaderName and
 * uploaderAvatarUrl are derived at read time by ScrapbookRepository so
 * renaming a user or uploading a new avatar doesn't leave stale strings
 * inside historical post arrays.
 */
data class Photo(
    val photoId:           String,
    val url:               String,    // Firebase Storage download URL
    val storagePath:       String,    // for future deletion
    val description:       String,
    val uploaderId:        String,
    val uploaderName:      String = "",  // derived: filled by repo on read
    val uploaderAvatarUrl: String = ""   // derived: filled by repo on read
)

/**
 * What: A single memory "time point" in a group's monthly scrapbook. One author
 *       creates it (sets title + tags + first photo + day); teammates can join
 *       it later by appending their own photos. Each scrapbook covers ONE
 *       month, so `date` is the day within that month, e.g. "June 1".
 *
 *       Storage layout: posts/{postId} stores only `authorId` — the
 *       authorName / authorAvatarUrl fields below are populated by
 *       ScrapbookRepository at read time from a cached user lookup. Same for
 *       photo uploaders and comment authors.
 * Who: Used by ScrapbookRepository and all scrapbook screens.
 * When: Instantiated when loading or rendering a timeline entry.
 */
data class ScrapbookEntry(
    val id:               String,
    val authorId:         String,
    val authorName:       String,                       // derived: filled by repo on read
    val authorAvatarUrl:  String = "",                  // derived: filled by repo on read
    val date:             String,                       // day within the month, e.g. "June 1"
    val title:            String,
    val tags:             List<String> = emptyList(),
    val photos:           List<Photo> = emptyList(),
    val comments:         List<Comment> = emptyList(),
    val commentCount:     Int = 0
)

/**
 * What: A short comment a group member leaves on a memory. Like posts, only
 *       `authorId` is persisted to Firestore — authorName / authorAvatarUrl
 *       are filled in at read time from the user lookup cache.
 * Who: Used by ScrapbookEntry, ScrapbookRepository, and ScrapbookViewerScreen.
 * When: Created when a member posts a comment.
 */
data class Comment(
    val id:              String,
    val authorId:        String,
    val authorName:      String,    // derived: filled by repo on read
    val authorAvatarUrl: String = "", // derived: filled by repo on read
    val text:            String
)
