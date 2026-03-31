package com.hbv501g.forumapp.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Centralized vote state manager shared across all screens.
 *
 * Holds optimistic score overrides for posts and comments so that a vote
 * made on the feed is instantly visible when the user opens the post detail,
 * and vice-versa.
 */
class VoteManager {

    private val _postScores = MutableStateFlow<Map<String, Int>>(emptyMap())
    /** Post-id → latest known score (optimistic or server-confirmed). */
    val postScores: StateFlow<Map<String, Int>> = _postScores.asStateFlow()

    private val _commentScores = MutableStateFlow<Map<String, Int>>(emptyMap())
    /** Comment-id → latest known score (optimistic or server-confirmed). */
    val commentScores: StateFlow<Map<String, Int>> = _commentScores.asStateFlow()

    /* ------------------------------------------------------------------ */
    /*  Reads                                                              */
    /* ------------------------------------------------------------------ */

    /** Return the override score for [postId], or null if none recorded. */
    fun postScore(postId: String): Int? = _postScores.value[postId]

    /** Return the override score for [commentId], or null if none recorded. */
    fun commentScore(commentId: String): Int? = _commentScores.value[commentId]

    /* ------------------------------------------------------------------ */
    /*  Writes                                                             */
    /* ------------------------------------------------------------------ */

    /** Optimistically bump the score of a post. */
    fun optimisticPostVote(postId: String, currentScore: Int, direction: Int) {
        _postScores.update { it + (postId to (currentScore + direction)) }
    }

    /** Set the confirmed server score for a post. */
    fun confirmPostScore(postId: String, serverScore: Int) {
        _postScores.update { it + (postId to serverScore) }
    }

    /** Revert a post score to its previous value (on error). */
    fun revertPostScore(postId: String, previousScore: Int) {
        _postScores.update { it + (postId to previousScore) }
    }

    /** Optimistically bump the score of a comment. */
    fun optimisticCommentVote(commentId: String, currentScore: Int, direction: Int) {
        _commentScores.update { it + (commentId to (currentScore + direction)) }
    }

    /** Set the confirmed server score for a comment. */
    fun confirmCommentScore(commentId: String, serverScore: Int) {
        _commentScores.update { it + (commentId to serverScore) }
    }

    /** Revert a comment score to its previous value (on error). */
    fun revertCommentScore(commentId: String, previousScore: Int) {
        _commentScores.update { it + (commentId to previousScore) }
    }

    /** Clear all cached overrides (e.g. on logout). */
    fun clear() {
        _postScores.value = emptyMap()
        _commentScores.value = emptyMap()
    }
}
