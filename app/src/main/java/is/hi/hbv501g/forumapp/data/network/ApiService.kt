package com.hbv501g.forumapp.data.network

import com.google.gson.JsonElement
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Part
import retrofit2.http.Query

interface ApiService {

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): UserResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthSessionResponse

    @POST("api/auth/logout")
    suspend fun logout(@Header("X-Session-Id") sessionId: String)

    @GET("api/feed")
    suspend fun listFeed(
        @Query("scope") scope: String = "GLOBAL",
        @Query("sort") sort: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 25
    ): JsonElement

    @POST("api/posts")
    suspend fun createPost(
        @Header("X-Session-Id") sessionId: String,
        @Body request: CreatePostRequest
    ): PostResponse

    @Multipart
    @POST("api/posts/media")
    suspend fun createMediaPost(
        @Header("X-Session-Id") sessionId: String,
        @Part("payload") payload: RequestBody,
        @Part media: MultipartBody.Part
    ): PostResponse

    @POST("api/communities")
    suspend fun createCommunity(
        @Header("X-Session-Id") sessionId: String,
        @Body request: CreateCommunityRequest
    ): CommunityResponse

    @GET("api/communities")
    suspend fun listCommunities(
        @Query("q") query: String? = null
    ): List<CommunityResponse>

    @GET("api/communities/{name}")
    suspend fun getCommunity(
        @Path("name") name: String
    ): CommunityResponse

    @POST("api/communities/join")
    suspend fun joinCommunity(
        @Header("X-Session-Id") sessionId: String,
        @Body request: MembershipRequest
    )

    @POST("api/communities/leave")
    suspend fun leaveCommunity(
        @Header("X-Session-Id") sessionId: String,
        @Body request: MembershipRequest
    )

    @GET("api/posts/{id}")
    suspend fun getPost(@Path("id") postId: String): PostResponse

    @GET("api/posts/{postId}/comments")
    suspend fun listComments(@Path("postId") postId: String): List<CommentResponse>

    @POST("api/posts/{postId}/comments")
    suspend fun createComment(
        @Header("X-Session-Id") sessionId: String,
        @Path("postId") postId: String,
        @Body request: CreateCommentRequest
    ): CommentResponse

    @POST("api/votes")
    suspend fun vote(
        @Header("X-Session-Id") sessionId: String,
        @Body request: VoteRequest
    ): VoteResponse

    @GET("api/search")
    suspend fun search(
        @Query("q") query: String
    ): SearchResultsResponse

    @GET("api/users/{username}")
    suspend fun getUserProfile(
        @Path("username") username: String,
        @Query("sort") sort: String = "NEW",
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): JsonElement

    @POST("api/moderation/posts/{postId}/remove")
    suspend fun removePost(
        @Header("X-Session-Id") sessionId: String,
        @Path("postId") postId: String
    )

    @POST("api/moderation/comments/{commentId}/remove")
    suspend fun removeComment(
        @Header("X-Session-Id") sessionId: String,
        @Path("commentId") commentId: String
    )
}
