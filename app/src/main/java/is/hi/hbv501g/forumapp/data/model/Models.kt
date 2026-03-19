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
    val mediaBase64: String?,
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

data class Community(
    val id: String,
    val name: String,
    val description: String?,
    val createdAt: String = ""
)

data class SearchUser(
    val id: String,
    val username: String,
    val status: String
)

data class SearchResults(
    val posts: List<Post>,
    val communities: List<Community>,
    val users: List<SearchUser>
)

data class UserProfile(
    val id: String,
    val username: String,
    val email: String,
    val status: String,
    val karma: Int,
    val posts: Page<Post>
)

data class Page<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)
