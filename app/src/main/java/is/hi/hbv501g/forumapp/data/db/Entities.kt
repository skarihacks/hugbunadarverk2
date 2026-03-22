package com.hbv501g.forumapp.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey
    val id: String,
    val community: String,
    val author: String,
    val title: String,
    val type: String,
    val body: String?,
    val url: String?,
    @ColumnInfo(name = "media_url")
    val mediaUrl: String?,
    @ColumnInfo(name = "media_base64")
    val mediaBase64: String?,
    val score: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "comments",
    foreignKeys = [ForeignKey(
        entity = PostEntity::class,
        parentColumns = ["id"],
        childColumns = ["post_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("post_id")]
)
data class CommentEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "post_id")
    val postId: String,
    val author: String,
    val body: String,
    val score: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "communities")
data class CommunityEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis()
)
