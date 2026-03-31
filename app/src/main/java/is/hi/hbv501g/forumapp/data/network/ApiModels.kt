package com.hbv501g.forumapp.data.network

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

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
    val status: String?
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
    val community: JsonElement?,
    @SerializedName("communityName")
    val communityName: String?,
    val communityId: String?,
    val author: JsonElement?,
    @SerializedName("authorUsername")
    val authorUsername: String?,
    val authorName: String?,
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
    val url: String? = null,
    val mediaBase64: String? = null
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

data class MembershipRequest(
    val communityName: String
)

data class CommentResponse(
    val id: String?,
    val postId: String?,
    val author: JsonElement?,
    @SerializedName("authorUsername")
    val authorUsername: String?,
    val authorName: String?,
    val authorId: String?,
    val body: String?,
    val score: Int?,
    val createdAt: String?
)

data class CreateCommentRequest(
    val body: String
)

data class VoteRequest(
    val targetId: String,
    val targetType: String,
    val direction: Int
)

data class VoteResponse(
    val id: String,
    val targetType: String,
    val targetId: String,
    val value: String
)

data class SearchResultsResponse(
    val posts: List<PostResponse>,
    val communities: List<SearchCommunityResponse>,
    val users: List<SearchUserResponse>
)

data class SearchCommunityResponse(
    val id: String,
    val name: String,
    val description: String?
)

data class SearchUserResponse(
    val id: String,
    val username: String,
    val status: String?
)

data class UserProfileResponse(
    val user: UserSummaryResponse,
    val posts: PageResponse<PostResponse>
)

data class UserSummaryResponse(
    val id: String,
    val username: String,
    val email: String,
    val status: String?,
    val karma: Int
)
