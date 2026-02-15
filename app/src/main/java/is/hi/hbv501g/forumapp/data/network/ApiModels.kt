package com.hbv501g.forumapp.data.network

data class LoginRequest(
    val identifier: String,
    val usernameOrEmail: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class UserResponse(
    val id: String,
    val username: String,
    val email: String,
    val status: String
)

data class AuthSessionResponse(
    val sessionId: String,
    val user: UserResponse
)

data class PageResponse<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class PostResponse(
    val id: String?,
    val community: String?,
    val communityId: String?,
    val author: String?,
    val authorId: String?,
    val title: String?,
    val type: String?,
    val body: String?,
    val url: String?,
    val mediaUrl: String?,
    val mediaBase64: String?,
    val score: Int?,
    val createdAt: String?
)

data class CreatePostRequest(
    val communityName: String,
    val title: String,
    val type: String,
    val body: String? = null,
    val url: String? = null
)

data class CreateCommunityRequest(
    val name: String,
    val description: String? = null
)

data class CommunityResponse(
    val id: String,
    val name: String,
    val description: String?,
    val createdAt: String
)

data class CommentResponse(
    val id: String,
    val postId: String,
    val author: String,
    val body: String,
    val score: Int,
    val createdAt: String
)

data class CreateCommentRequest(
    val postId: String,
    val body: String
)
