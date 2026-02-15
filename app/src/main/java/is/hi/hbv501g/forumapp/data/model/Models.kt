package com.hbv501g.forumapp.data.model

data class UserSession(
    val sessionId: String,
    val userId: String,
    val username: String,
    val email: String
)

enum class FeedSort {
    HOT,
    NEW,
    TOP
}

data class Post(
    val id: String,
    val community: String,
    val author: String,
    val title: String,
    val type: String,
    val body: String?,
    val url: String?,
    val mediaUrl: String?,
    val score: Int,
    val createdAt: String
)

data class Comment(
    val id: String,
    val postId: String,
    val author: String,
    val body: String,
    val score: Int,
    val createdAt: String
)

data class Page<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)
