package com.hbv501g.forumapp.data.network

import com.google.gson.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
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

    @POST("api/communities")
    suspend fun createCommunity(
        @Header("X-Session-Id") sessionId: String,
        @Body request: CreateCommunityRequest
    ): CommunityResponse

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
}
