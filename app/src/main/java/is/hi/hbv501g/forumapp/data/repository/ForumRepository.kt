package com.hbv501g.forumapp.data.repository

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.hbv501g.forumapp.data.model.Comment
import com.hbv501g.forumapp.data.model.FeedSort
import com.hbv501g.forumapp.data.model.Page
import com.hbv501g.forumapp.data.model.Post
import com.hbv501g.forumapp.data.model.UserSession
import com.hbv501g.forumapp.data.network.ApiService
import com.hbv501g.forumapp.data.network.CommentResponse
import com.hbv501g.forumapp.data.network.CreateCommentRequest
import com.hbv501g.forumapp.data.network.CreateCommunityRequest
import com.hbv501g.forumapp.data.network.CreatePostRequest
import com.hbv501g.forumapp.data.network.LoginRequest
import com.hbv501g.forumapp.data.network.PageResponse
import com.hbv501g.forumapp.data.network.PostResponse
import com.hbv501g.forumapp.data.network.RegisterRequest
import com.hbv501g.forumapp.data.session.SessionStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject
import retrofit2.HttpException

class ForumRepository(
    private val apiService: ApiService,
    private val sessionStore: SessionStore
) {
    private val gson = Gson()
    private val postListType = object : TypeToken<List<PostResponse>>() {}.type
    private val postPageType = object : TypeToken<PageResponse<PostResponse>>() {}.type
    private val _joinedCommunities = MutableStateFlow<Set<String>>(emptySet())

    val sessionFlow: Flow<UserSession?> = sessionStore.sessionFlow
    val joinedCommunitiesFlow: StateFlow<Set<String>> = _joinedCommunities.asStateFlow()

    suspend fun register(username: String, email: String, password: String) {
        try {
            apiService.register(
                RegisterRequest(
                    username = username.trim(),
                    email = email.trim(),
                    password = password
                )
            )
        } catch (throwable: Throwable) {
            throw RepositoryException(throwable.toUserMessage())
        }
    }

    suspend fun registerAndLogin(username: String, email: String, password: String) {
        register(username = username, email = email, password = password)
        login(identifier = username, password = password)
    }

    suspend fun login(identifier: String, password: String) {
        try {
            val response = apiService.login(
                LoginRequest(
                    identifier = identifier.trim(),
                    usernameOrEmail = identifier.trim(),
                    password = password
                )
            )
            sessionStore.saveSession(
                UserSession(
                    sessionId = response.sessionId,
                    userId = response.user.id,
                    username = response.user.username,
                    email = response.user.email
                )
            )
        } catch (throwable: Throwable) {
            throw RepositoryException(throwable.toUserMessage())
        }
    }

    suspend fun logout() {
        val sessionId = sessionStore.currentSessionId()
        if (sessionId != null) {
            runCatching { apiService.logout(sessionId) }
        }
        sessionStore.clearSession()
        _joinedCommunities.value = emptySet()
    }

    suspend fun getFeed(sort: FeedSort, page: Int = 0, size: Int = 25): Page<Post> {
        return try {
            val raw = apiService.listFeed(sort = sort.name, page = page, size = size)
            raw.toPostPage()
        } catch (throwable: Throwable) {
            throw RepositoryException(throwable.toUserMessage())
        }
    }

    suspend fun createTextPost(communityName: String, title: String, body: String): Post {
        val sessionId = requireSessionId()
        return try {
            apiService.createPost(
                sessionId = sessionId,
                request = CreatePostRequest(
                    communityName = communityName.trim(),
                    title = title.trim(),
                    type = "TEXT",
                    body = body.trim()
                )
            ).toDomain(fallbackCommunity = communityName.trim())
        } catch (throwable: Throwable) {
            throw RepositoryException(throwable.toUserMessage())
        }
    }

    suspend fun createCommunity(name: String, description: String?): String {
        val sessionId = requireSessionId()
        return try {
            val response = apiService.createCommunity(
                sessionId = sessionId,
                request = CreateCommunityRequest(
                    name = name.trim(),
                    description = description?.trim().takeUnless { it.isNullOrBlank() }
                )
            )
            response.name
        } catch (throwable: Throwable) {
            throw RepositoryException(throwable.toUserMessage())
        }
    }

    suspend fun getPost(postId: String): Post {
        return try {
            apiService.getPost(postId).toDomain()
        } catch (throwable: Throwable) {
            throw RepositoryException(throwable.toUserMessage())
        }
    }

    suspend fun getComments(postId: String): List<Comment> {
        return try {
            apiService.listComments(postId).map { it.toDomain() }
        } catch (throwable: Throwable) {
            throw RepositoryException(throwable.toUserMessage())
        }
    }

    suspend fun createComment(postId: String, body: String): Comment {
        val sessionId = requireSessionId()
        return try {
            apiService.createComment(
                sessionId = sessionId,
                request = CreateCommentRequest(postId = postId, body = body.trim())
            ).toDomain()
        } catch (throwable: Throwable) {
            throw RepositoryException(throwable.toUserMessage())
        }
    }

    suspend fun listCommunities(sort: FeedSort = FeedSort.HOT, size: Int = 100): List<String> {
        val page = getFeed(sort = sort, page = 0, size = size)
        return page.items
            .map { it.community.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
    }

    fun setCommunityJoined(communityName: String, joined: Boolean) {
        val normalized = communityName.trim()
        if (normalized.isBlank()) {
            return
        }

        _joinedCommunities.update { current ->
            val existing = current.firstOrNull { it.equals(normalized, ignoreCase = true) }
            if (joined) {
                if (existing == null) current + normalized else current
            } else {
                if (existing == null) current else current - existing
            }
        }
    }

    fun toggleCommunityMembership(communityName: String) {
        val normalized = communityName.trim()
        if (normalized.isBlank()) {
            return
        }
        val alreadyJoined = _joinedCommunities.value.any { it.equals(normalized, ignoreCase = true) }
        setCommunityJoined(communityName = normalized, joined = !alreadyJoined)
    }

    private suspend fun requireSessionId(): String {
        return sessionStore.currentSessionId() ?: throw RepositoryException("Session expired. Please log in again.")
    }

    private fun isMessageUsable(message: String?): Boolean {
        return !message.isNullOrBlank() && !message.equals("null", ignoreCase = true)
    }

    private fun Throwable.toUserMessage(): String {
        return when (this) {
            is RepositoryException -> message ?: "Unexpected error"
            is HttpException -> {
                val errorBody = response()?.errorBody()?.string()
                val rawBody = errorBody?.trim().orEmpty()
                val apiMessage = runCatching {
                    JSONObject(errorBody ?: "{}").optString("message")
                }.getOrDefault("")
                val apiError = runCatching {
                    JSONObject(errorBody ?: "{}").optString("error")
                }.getOrDefault("")
                when {
                    isMessageUsable(apiMessage) -> apiMessage
                    code() == 409 -> "Username or email already in use. Try logging in or choose different values."
                    code() == 400 && isMessageUsable(rawBody) && !rawBody.startsWith("<") ->
                        "Bad request: ${rawBody.take(220)}"
                    isMessageUsable(apiError) -> apiError
                    isMessageUsable(message()) -> message()
                    else -> "Request failed with HTTP ${code()}"
                }
            }
            is IOException -> "Could not reach server. Check the backend URL/network."
            else -> message ?: "Unexpected error"
        }
    }

    private fun JsonElement.toPostPage(): Page<Post> {
        return when {
            isJsonArray -> {
                val posts: List<PostResponse> = gson.fromJson(this, postListType) ?: emptyList()
                Page(
                    items = posts.map { it.toDomain() },
                    page = 0,
                    size = posts.size,
                    totalElements = posts.size.toLong(),
                    totalPages = 1
                )
            }
            isJsonObject -> parsePostPageObject(asJsonObject)
            else -> throw RepositoryException("Unexpected feed response format")
        }
    }

    private fun parsePostPageObject(obj: JsonObject): Page<Post> {
        if (obj.has("items")) {
            val page: PageResponse<PostResponse> = gson.fromJson(obj, postPageType)
            return Page(
                items = page.items.map { it.toDomain() },
                page = page.page,
                size = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages
            )
        }

        if (obj.has("content")) {
            val posts: List<PostResponse> = gson.fromJson(obj.get("content"), postListType) ?: emptyList()
            return Page(
                items = posts.map { it.toDomain() },
                page = obj.intOrDefault("number", 0),
                size = obj.intOrDefault("size", posts.size),
                totalElements = obj.longOrDefault("totalElements", posts.size.toLong()),
                totalPages = obj.intOrDefault("totalPages", 1)
            )
        }

        if (obj.has("posts") && obj.get("posts").isJsonArray) {
            val posts: List<PostResponse> = gson.fromJson(obj.get("posts"), postListType) ?: emptyList()
            return Page(
                items = posts.map { it.toDomain() },
                page = 0,
                size = posts.size,
                totalElements = posts.size.toLong(),
                totalPages = 1
            )
        }

        throw RepositoryException("Feed response missing posts/items/content")
    }

    private fun JsonObject.intOrDefault(name: String, default: Int): Int {
        return runCatching { get(name)?.asInt }.getOrNull() ?: default
    }

    private fun JsonObject.longOrDefault(name: String, default: Long): Long {
        return runCatching { get(name)?.asLong }.getOrNull() ?: default
    }

    private fun PostResponse.toDomain(): Post {
        return toDomain(fallbackCommunity = null)
    }

    private fun PostResponse.toDomain(fallbackCommunity: String?): Post {
        val postId = id?.takeIf { it.isNotBlank() } ?: throw RepositoryException("Post response missing id")
        val communityName = firstNonBlank(community, communityId, fallbackCommunity) ?: "unknown"
        val authorName = firstNonBlank(author, authorId) ?: "unknown"
        val postTitle = title?.takeIf { it.isNotBlank() } ?: "(untitled)"

        return Post(
            id = postId,
            community = communityName,
            author = authorName,
            title = postTitle,
            type = type ?: "TEXT",
            body = body,
            url = url,
            mediaUrl = mediaUrl,
            score = score ?: 0,
            createdAt = createdAt ?: ""
        )
    }

    private fun firstNonBlank(vararg values: String?): String? {
        return values.firstOrNull { !it.isNullOrBlank() }?.trim()
    }

    private fun CommentResponse.toDomain(): Comment {
        return Comment(
            id = id,
            postId = postId,
            author = author,
            body = body,
            score = score,
            createdAt = createdAt
        )
    }
}

class RepositoryException(message: String) : RuntimeException(message)
