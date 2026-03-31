package com.hbv501g.forumapp.data.repository

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.hbv501g.forumapp.data.db.CommentDao
import com.hbv501g.forumapp.data.db.CommentEntity
import com.hbv501g.forumapp.data.db.CommunityDao
import com.hbv501g.forumapp.data.db.CommunityEntity
import com.hbv501g.forumapp.data.db.PostDao
import com.hbv501g.forumapp.data.db.PostEntity
import com.hbv501g.forumapp.data.model.Comment
import com.hbv501g.forumapp.data.model.Community
import com.hbv501g.forumapp.data.model.FeedSort
import com.hbv501g.forumapp.data.model.Page
import com.hbv501g.forumapp.data.model.Post
import com.hbv501g.forumapp.data.model.SearchResults
import com.hbv501g.forumapp.data.model.SearchUser
import com.hbv501g.forumapp.data.model.UserProfile
import com.hbv501g.forumapp.data.model.UserSession
import com.hbv501g.forumapp.data.network.ApiService
import com.hbv501g.forumapp.data.network.CommentResponse
import com.hbv501g.forumapp.data.network.CommunityResponse
import com.hbv501g.forumapp.data.network.CreateCommentRequest
import com.hbv501g.forumapp.data.network.CreateCommunityRequest
import com.hbv501g.forumapp.data.network.CreatePostRequest
import com.hbv501g.forumapp.data.network.LoginRequest
import com.hbv501g.forumapp.data.network.MembershipRequest
import com.hbv501g.forumapp.data.network.PageResponse
import com.hbv501g.forumapp.data.network.PostResponse
import com.hbv501g.forumapp.data.network.RegisterRequest
import com.hbv501g.forumapp.data.network.SearchResultsResponse
import com.hbv501g.forumapp.data.network.VoteRequest
import com.hbv501g.forumapp.data.session.SessionStore
import java.io.IOException
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.HttpException

class ForumRepository(
    private val apiService: ApiService,
    private val sessionStore: SessionStore,
    private val postDao: PostDao,
    private val commentDao: CommentDao,
    private val communityDao: CommunityDao
) {
    private val gson = Gson()
    private val postListType = object : TypeToken<List<PostResponse>>() {}.type
    private val postPageType = object : TypeToken<PageResponse<PostResponse>>() {}.type
    private val _joinedCommunities = MutableStateFlow<Set<String>>(emptySet())
    private val _ownedCommunities = MutableStateFlow<Set<String>>(emptySet())
    private val _removedPostIds = MutableStateFlow<Set<String>>(emptySet())

    /** Shared vote state – all screens observe the same overrides. */
    val voteManager = VoteManager()

    init {
        runBlocking {
            val userId = sessionStore.sessionFlow.first()?.userId
            if (userId != null) {
                _joinedCommunities.value = sessionStore.loadJoinedCommunities(userId)
                _ownedCommunities.value = sessionStore.loadOwnedCommunities(userId)
            }
        }
    }

    val sessionFlow: Flow<UserSession?> = sessionStore.sessionFlow
    val joinedCommunitiesFlow: StateFlow<Set<String>> = _joinedCommunities.asStateFlow()
    val ownedCommunitiesFlow: StateFlow<Set<String>> = _ownedCommunities.asStateFlow()

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
            val userId = response.user.id
            sessionStore.saveSession(
                UserSession(
                    sessionId = response.sessionId,
                    userId = userId,
                    username = response.user.username,
                    email = response.user.email
                )
            )
            _joinedCommunities.value = sessionStore.loadJoinedCommunities(userId)
            _ownedCommunities.value = sessionStore.loadOwnedCommunities(userId)
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
        _ownedCommunities.value = emptySet()
        _removedPostIds.value = emptySet()
        voteManager.clear()
        postDao.deleteAll()
        commentDao.deleteAll()
        communityDao.deleteAll()
    }

    suspend fun getFeed(sort: FeedSort, page: Int = 0, size: Int = 25): Page<Post> {
        return try {
            val cachedById = postDao.getAll().associateBy { it.id }
            val raw = apiService.listFeed(sort = sort.name, page = page, size = size)
            val feedPage = raw.toPostPage()
                .mergeCachedPosts(cachedById)
                .filterRemovedPosts()
            postDao.insertAll(feedPage.items.map { it.toEntity() })
            feedPage
        } catch (throwable: Throwable) {
            val cached = postDao.getAll()
            if (cached.isNotEmpty()) {
                Page(
                    items = cached.map { it.toDomain() },
                    page = 0,
                    size = cached.size,
                    totalElements = cached.size.toLong(),
                    totalPages = 1
                ).filterRemovedPosts()
            } else {
                throw RepositoryException(throwable.toUserMessage())
            }
        }
    }

    suspend fun createTextPost(communityName: String, title: String, body: String): Post {
        val sessionId = requireSessionId()
        val username = currentUsername()
        return try {
            apiService.createPost(
                sessionId = sessionId,
                request = CreatePostRequest(
                    communityName = communityName.trim(),
                    title = title.trim(),
                    type = "TEXT",
                    body = body.trim()
                )
            ).toDomain(fallbackCommunity = communityName.trim(), fallbackAuthor = username)
        } catch (throwable: Throwable) {
            throw RepositoryException(throwable.toUserMessage())
        }
    }

    suspend fun createLinkPost(communityName: String, title: String, url: String, body: String?): Post {
        val sessionId = requireSessionId()
        val username = currentUsername()
        return try {
            try {
                apiService.createPost(
                    sessionId = sessionId,
                    request = CreatePostRequest(
                        communityName = communityName.trim(),
                        title = title.trim(),
                        type = "LINK",
                        body = body?.trim().takeUnless { it.isNullOrBlank() },
                        url = url.trim()
                    )
                ).toDomain(fallbackCommunity = communityName.trim(), fallbackAuthor = username)
            } catch (http: HttpException) {
                if (http.code() !in listOf(400, 404, 405, 422)) {
                    throw http
                }

                val fallbackBody = buildString {
                    append(url.trim())
                    val optionalBody = body?.trim().orEmpty()
                    if (optionalBody.isNotBlank()) {
                        append("\n\n")
                        append(optionalBody)
                    }
                }

                apiService.createPost(
                    sessionId = sessionId,
                    request = CreatePostRequest(
                        communityName = communityName.trim(),
                        title = title.trim(),
                        type = "TEXT",
                        body = fallbackBody
                    )
                ).toDomain(fallbackCommunity = communityName.trim(), fallbackAuthor = username)
            }
        } catch (throwable: Throwable) {
            throw RepositoryException(throwable.toUserMessage())
        }
    }

    suspend fun createMediaPost(
        communityName: String,
        title: String,
        body: String?,
        mediaBytes: ByteArray,
        mediaFileName: String,
        mediaMimeType: String
    ): Post {
        val sessionId = requireSessionId()
        val username = currentUsername()
        return try {
            val payload = gson.toJson(
                CreatePostRequest(
                    communityName = communityName.trim(),
                    title = title.trim(),
                    type = "MEDIA",
                    body = body?.trim().takeUnless { it.isNullOrBlank() }
                )
            ).toRequestBody("application/json".toMediaType())

            val tempFile = File.createTempFile("upload_", "_${mediaFileName}")
            tempFile.writeBytes(mediaBytes)

            val mediaPart = MultipartBody.Part.createFormData(
                "media",
                mediaFileName,
                tempFile.asRequestBody(mediaMimeType.toMediaType())
            )

            try {
                try {
                    apiService.createMediaPost(
                        sessionId = sessionId,
                        payload = payload,
                        media = mediaPart
                    ).toDomain(fallbackCommunity = communityName.trim(), fallbackAuthor = username)
                } catch (http: HttpException) {
                    if (http.code() !in listOf(404, 405, 415)) {
                        throw http
                    }

                    val mediaBase64 = Base64.encodeToString(mediaBytes, Base64.NO_WRAP)
                    apiService.createPost(
                        sessionId = sessionId,
                        request = CreatePostRequest(
                            communityName = communityName.trim(),
                            title = title.trim(),
                            type = "MEDIA",
                            body = body?.trim().takeUnless { it.isNullOrBlank() },
                            mediaBase64 = mediaBase64
                        )
                    ).toDomain(fallbackCommunity = communityName.trim(), fallbackAuthor = username)
                }
            } finally {
                tempFile.delete()
            }
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
            setCommunityJoined(response.name, joined = true)
            setCommunityOwned(response.name, owned = true)
            val userId = requireUserId()
            sessionStore.saveJoinedCommunities(userId, _joinedCommunities.value)
            sessionStore.saveOwnedCommunities(userId, _ownedCommunities.value)
            response.name
        } catch (throwable: Throwable) {
            throw RepositoryException(throwable.toUserMessage())
        }
    }

    suspend fun getPost(postId: String): Post {
        return try {
            val cachedPost = postDao.getById(postId)?.toDomain()
            val post = apiService.getPost(postId).toDomain(
                fallbackCommunity = cachedPost?.community,
                fallbackAuthor = cachedPost?.author
            )
            if (post.id in _removedPostIds.value) {
                throw RepositoryException("Post not found")
            }
            postDao.insert(post.toEntity())
            post
        } catch (throwable: Throwable) {
            postDao.getById(postId)?.toDomain()?.takeUnless { it.id in _removedPostIds.value }
                ?: throw RepositoryException(throwable.toUserMessage())
        }
    }

    suspend fun getComments(postId: String): List<Comment> {
        return try {
            val comments = apiService.listComments(postId).map { it.toDomain() }
            commentDao.deleteByPostId(postId)
            commentDao.insertAll(comments.map { it.toEntity() })
            comments
        } catch (throwable: Throwable) {
            val cached = commentDao.getByPostId(postId)
            if (cached.isNotEmpty()) {
                cached.map { it.toDomain() }
            } else {
                throw RepositoryException(throwable.toUserMessage())
            }
        }
    }

    suspend fun createComment(postId: String, body: String): Comment {
        val sessionId = requireSessionId()
        return try {
            apiService.createComment(
                sessionId = sessionId,
                postId = postId,
                request = CreateCommentRequest(body = body.trim())
            ).toDomain()
        } catch (throwable: Throwable) {
            throw RepositoryException(throwable.toUserMessage())
        }
    }

    suspend fun listCommunities(sort: FeedSort = FeedSort.HOT, size: Int = 100): List<String> {
        return try {
            val communities = apiService.listCommunities()
            communityDao.insertAll(communities.map { it.toDomain().toEntity() })
            communities
                .map { it.name.trim() }
                .filter { it.isNotBlank() }
                .distinctBy { it.lowercase() }
                .sortedBy { it.lowercase() }
        } catch (_: Throwable) {
            val cached = communityDao.getAll()
            if (cached.isNotEmpty()) {
                cached.map { it.name }
                    .filter { it.isNotBlank() }
                    .distinctBy { it.lowercase() }
                    .sortedBy { it.lowercase() }
            } else {
                val page = getFeed(sort = sort, page = 0, size = size)
                page.items
                    .map { it.community.trim() }
                    .filter { it.isNotBlank() }
                    .distinctBy { it.lowercase() }
                    .sortedBy { it.lowercase() }
            }
        }
    }

    private fun setCommunityJoined(communityName: String, joined: Boolean) {
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

    private fun setCommunityOwned(communityName: String, owned: Boolean) {
        val normalized = communityName.trim()
        if (normalized.isBlank()) {
            return
        }

        _ownedCommunities.update { current ->
            val existing = current.firstOrNull { it.equals(normalized, ignoreCase = true) }
            if (owned) {
                if (existing == null) current + normalized else current
            } else {
                if (existing == null) current else current - existing
            }
        }
    }

    suspend fun joinCommunity(communityName: String) {
        val sessionId = requireSessionId()
        try {
            apiService.joinCommunity(
                sessionId = sessionId,
                request = MembershipRequest(communityName = communityName.trim())
            )
            setCommunityJoined(communityName, joined = true)
            sessionStore.saveJoinedCommunities(requireUserId(), _joinedCommunities.value)
        } catch (throwable: Throwable) {
            throw RepositoryException(throwable.toUserMessage())
        }
    }

    suspend fun leaveCommunity(communityName: String) {
        val sessionId = requireSessionId()
        try {
            apiService.leaveCommunity(
                sessionId = sessionId,
                request = MembershipRequest(communityName = communityName.trim())
            )
            setCommunityJoined(communityName, joined = false)
            sessionStore.saveJoinedCommunities(requireUserId(), _joinedCommunities.value)
        } catch (throwable: Throwable) {
            throw RepositoryException(throwable.toUserMessage())
        }
    }

    suspend fun toggleCommunityMembership(communityName: String) {
        val normalized = communityName.trim()
        if (normalized.isBlank()) {
            return
        }
        val alreadyJoined = _joinedCommunities.value.any { it.equals(normalized, ignoreCase = true) }
        if (alreadyJoined) leaveCommunity(normalized) else joinCommunity(normalized)
    }

    suspend fun vote(targetId: String, targetType: String, direction: Int) {
        val sessionId = requireSessionId()
        try {
            apiService.vote(
                sessionId = sessionId,
                request = VoteRequest(
                    targetId = targetId,
                    targetType = targetType,
                    direction = direction
                )
            )
        } catch (throwable: Throwable) {
            throw RepositoryException(throwable.toUserMessage())
        }
    }

    suspend fun search(query: String): SearchResults {
        return try {
            val results = apiService.search(query.trim()).toDomain().filterRemovedPosts()
            postDao.insertAll(results.posts.map { it.toEntity() })
            results
        } catch (throwable: Throwable) {
            throw RepositoryException(throwable.toUserMessage())
        }
    }

    suspend fun getUserProfile(username: String): UserProfile {
        return try {
            val profile = apiService.getUserProfile(username.trim()).toUserProfile()
            postDao.insertAll(profile.posts.items.filterNotRemoved().map { it.toEntity() })
            profile
        } catch (throwable: Throwable) {
            throw RepositoryException(throwable.toUserMessage())
        }
    }

    suspend fun removePost(postId: String) {
        val sessionId = requireSessionId()
        try {
            apiService.removePost(sessionId = sessionId, postId = postId)
            postDao.deleteById(postId)
            _removedPostIds.update { it + postId }
        } catch (throwable: Throwable) {
            throw RepositoryException(throwable.toUserMessage())
        }
    }

    suspend fun removeComment(commentId: String) {
        val sessionId = requireSessionId()
        try {
            apiService.removeComment(sessionId = sessionId, commentId = commentId)
            commentDao.deleteById(commentId)
        } catch (throwable: Throwable) {
            throw RepositoryException(throwable.toUserMessage())
        }
    }

    private suspend fun requireSessionId(): String {
        return sessionStore.currentSessionId() ?: throw RepositoryException("Session expired. Please log in again.")
    }

    private suspend fun requireUserId(): String {
        return sessionStore.sessionFlow.first()?.userId ?: throw RepositoryException("Session expired. Please log in again.")
    }

    private suspend fun currentUsername(): String? {
        return sessionStore.sessionFlow.first()?.username
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

    private fun parsePostPageObject(
        obj: JsonObject,
        fallbackAuthor: String? = null
    ): Page<Post> {
        if (obj.has("items")) {
            val page: PageResponse<PostResponse> = gson.fromJson(obj, postPageType)
            return Page(
                items = page.items.map { it.toDomain(fallbackAuthor = fallbackAuthor) },
                page = page.page,
                size = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages
            )
        }

        if (obj.has("content")) {
            val posts: List<PostResponse> = gson.fromJson(obj.get("content"), postListType) ?: emptyList()
            return Page(
                items = posts.map { it.toDomain(fallbackAuthor = fallbackAuthor) },
                page = obj.intOrDefault("number", 0),
                size = obj.intOrDefault("size", posts.size),
                totalElements = obj.longOrDefault("totalElements", posts.size.toLong()),
                totalPages = obj.intOrDefault("totalPages", 1)
            )
        }

        if (obj.has("posts") && obj.get("posts").isJsonArray) {
            val posts: List<PostResponse> = gson.fromJson(obj.get("posts"), postListType) ?: emptyList()
            return Page(
                items = posts.map { it.toDomain(fallbackAuthor = fallbackAuthor) },
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
        return toDomain(fallbackCommunity = null, fallbackAuthor = null)
    }

    private fun PostResponse.toDomain(
        fallbackCommunity: String? = null,
        fallbackAuthor: String? = null
    ): Post {
        val postId = id?.takeIf { it.isNotBlank() } ?: throw RepositoryException("Post response missing id")
        val communityName = firstNonBlank(
            extractCommunityName(community),
            communityName,
            communityId,
            fallbackCommunity
        ) ?: "unknown"
        val authorName = firstNonBlank(
            extractAuthorName(author),
            authorUsername,
            this.authorName,
            authorId,
            fallbackAuthor
        ) ?: "unknown"
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
            mediaBase64 = mediaBase64,
            score = score ?: 0,
            createdAt = createdAt ?: ""
        )
    }

    private fun firstNonBlank(vararg values: String?): String? {
        return values.firstOrNull { !it.isNullOrBlank() }?.trim()
    }

    private fun CommentResponse.toDomain(): Comment {
        val commentId = id?.takeIf { it.isNotBlank() } ?: throw RepositoryException("Comment response missing id")
        val commentPostId = postId?.takeIf { it.isNotBlank() }
            ?: throw RepositoryException("Comment response missing postId")
        return Comment(
            id = commentId,
            postId = commentPostId,
            author = firstNonBlank(
                extractAuthorName(author),
                authorUsername,
                this.authorName,
                authorId
            ) ?: "unknown",
            body = body?.takeIf { it.isNotBlank() } ?: "",
            score = score ?: 0,
            createdAt = createdAt ?: ""
        )
    }

    private fun extractAuthorName(element: JsonElement?): String? {
        return extractStringValue(
            element = element,
            preferredKeys = listOf("username", "authorUsername", "name", "displayName", "id")
        )
    }

    private fun extractCommunityName(element: JsonElement?): String? {
        return extractStringValue(
            element = element,
            preferredKeys = listOf("name", "communityName", "title", "id")
        )
    }

    private fun extractStringValue(element: JsonElement?, preferredKeys: List<String>): String? {
        if (element == null || element.isJsonNull) {
            return null
        }

        if (element.isJsonPrimitive) {
            return element.asString?.trim()?.takeIf { it.isNotBlank() }
        }

        if (!element.isJsonObject) {
            return null
        }

        return extractStringValueFromObject(element.asJsonObject, preferredKeys, maxDepth = 3)
    }

    private fun extractStringValueFromObject(
        obj: JsonObject,
        preferredKeys: List<String>,
        maxDepth: Int
    ): String? {
        preferredKeys.forEach { key ->
            val value = obj.get(key)
            when {
                value == null || value.isJsonNull -> Unit
                value.isJsonPrimitive -> {
                    val text = value.asString?.trim()
                    if (!text.isNullOrBlank()) {
                        return text
                    }
                }
                maxDepth > 0 && value.isJsonObject -> {
                    extractStringValueFromObject(value.asJsonObject, preferredKeys, maxDepth - 1)?.let {
                        return it
                    }
                }
            }
        }

        obj.entrySet().forEach { (_, value) ->
            when {
                value == null || value.isJsonNull -> Unit
                value.isJsonPrimitive -> {
                    val text = value.asString?.trim()
                    if (!text.isNullOrBlank()) {
                        return text
                    }
                }
                maxDepth > 0 && value.isJsonObject -> {
                    extractStringValueFromObject(value.asJsonObject, preferredKeys, maxDepth - 1)?.let {
                        return it
                    }
                }
            }
        }

        return null
    }

    private fun CommunityResponse.toDomain(): Community {
        return Community(
            id = id,
            name = name,
            description = description,
            createdAt = createdAt
        )
    }

    private fun SearchResultsResponse.toDomain(): SearchResults {
        return SearchResults(
            posts = posts.map { it.toDomain() },
            communities = communities.map {
                Community(
                    id = it.id,
                    name = it.name,
                    description = it.description
                )
            },
            users = users.map {
                SearchUser(
                    id = it.id,
                    username = it.username,
                    status = it.status?.takeIf { value -> value.isNotBlank() } ?: "No status"
                )
            }
        )
    }

    private fun SearchResults.filterRemovedPosts(): SearchResults {
        return copy(posts = posts.filterNotRemoved())
    }

    private fun Page<Post>.mergeCachedPosts(cachedById: Map<String, PostEntity>): Page<Post> {
        return copy(items = items.map { post ->
            cachedById[post.id]?.toDomain()?.let { cached -> post.mergeWith(cached) } ?: post
        })
    }

    private fun Page<Post>.filterRemovedPosts(): Page<Post> {
        val filteredItems = items.filterNotRemoved()
        return copy(items = filteredItems, size = filteredItems.size, totalElements = filteredItems.size.toLong())
    }

    private fun List<Post>.filterNotRemoved(): List<Post> {
        val removedIds = _removedPostIds.value
        return filterNot { it.id in removedIds }
    }

    private fun Post.mergeWith(cached: Post): Post {
        return copy(
            community = preferKnownValue(primary = community, fallback = cached.community),
            author = preferKnownValue(primary = author, fallback = cached.author),
            title = preferKnownValue(primary = title, fallback = cached.title),
            body = body ?: cached.body,
            url = url ?: cached.url,
            mediaUrl = mediaUrl ?: cached.mediaUrl,
            mediaBase64 = mediaBase64 ?: cached.mediaBase64,
            createdAt = preferKnownValue(primary = createdAt, fallback = cached.createdAt)
        )
    }

    private fun preferKnownValue(primary: String, fallback: String): String {
        return if (primary.isBlank() || primary.equals("unknown", ignoreCase = true)) fallback else primary
    }

    private fun JsonElement.toUserProfile(): UserProfile {
        if (!isJsonObject) {
            throw RepositoryException("Unexpected user profile response format")
        }

        val obj = asJsonObject
        val userObj = obj.getAsJsonObject("user")
            ?: obj
        val postsElement = obj.get("posts")
        val profileUsername = userObj.stringOrEmpty("username").takeIf { it.isNotBlank() }

        val profilePosts = when {
            postsElement == null || postsElement.isJsonNull -> Page(
                items = emptyList(),
                page = 0,
                size = 0,
                totalElements = 0,
                totalPages = 0
            )
            postsElement.isJsonArray -> {
                val posts: List<PostResponse> = gson.fromJson(postsElement, postListType) ?: emptyList()
                Page(
                    items = posts.map { it.toDomain(fallbackAuthor = profileUsername) },
                    page = 0,
                    size = posts.size,
                    totalElements = posts.size.toLong(),
                    totalPages = 1
                )
            }
            postsElement.isJsonObject -> parsePostPageObject(postsElement.asJsonObject, fallbackAuthor = profileUsername)
            else -> throw RepositoryException("Unexpected posts format in user profile")
        }

        return UserProfile(
            id = userObj.stringOrEmpty("id"),
            username = userObj.stringOrEmpty("username"),
            email = userObj.stringOrEmpty("email"),
            status = userObj.get("status")?.takeIf { !it.isJsonNull }?.asString?.takeIf { it.isNotBlank() }
                ?: "No status",
            karma = runCatching { userObj.get("karma")?.asInt }.getOrNull() ?: 0,
            posts = profilePosts
        )
    }

    private fun JsonObject.stringOrEmpty(name: String): String {
        return runCatching { get(name)?.takeIf { !it.isJsonNull }?.asString }.getOrNull().orEmpty()
    }

    private fun Post.toEntity(): PostEntity = PostEntity(
        id = id,
        community = community,
        author = author,
        title = title,
        type = type,
        body = body,
        url = url,
        mediaUrl = mediaUrl,
        mediaBase64 = mediaBase64,
        score = score,
        createdAt = createdAt
    )

    private fun PostEntity.toDomain(): Post = Post(
        id = id,
        community = community,
        author = author,
        title = title,
        type = type,
        body = body,
        url = url,
        mediaUrl = mediaUrl,
        mediaBase64 = mediaBase64,
        score = score,
        createdAt = createdAt
    )

    private fun Comment.toEntity(): CommentEntity = CommentEntity(
        id = id,
        postId = postId,
        author = author,
        body = body,
        score = score,
        createdAt = createdAt
    )

    private fun CommentEntity.toDomain(): Comment = Comment(
        id = id,
        postId = postId,
        author = author,
        body = body,
        score = score,
        createdAt = createdAt
    )

    private fun Community.toEntity(): CommunityEntity = CommunityEntity(
        id = id,
        name = name,
        description = description,
        createdAt = createdAt
    )
}

class RepositoryException(message: String) : RuntimeException(message)
