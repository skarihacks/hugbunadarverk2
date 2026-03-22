package com.hbv501g.forumapp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY created_at DESC")
    suspend fun getAll(): List<PostEntity>

    @Query("SELECT * FROM posts WHERE id = :postId")
    suspend fun getById(postId: String): PostEntity?

    @Query("SELECT * FROM posts WHERE community = :communityName ORDER BY created_at DESC")
    suspend fun getByCommunity(communityName: String): List<PostEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<PostEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: PostEntity)

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deleteById(postId: String)

    @Query("DELETE FROM posts")
    suspend fun deleteAll()

    @Query("DELETE FROM posts WHERE cached_at < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long)
}

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE post_id = :postId ORDER BY created_at DESC")
    suspend fun getByPostId(postId: String): List<CommentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(comments: List<CommentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comment: CommentEntity)

    @Query("DELETE FROM comments WHERE id = :commentId")
    suspend fun deleteById(commentId: String)

    @Query("DELETE FROM comments WHERE post_id = :postId")
    suspend fun deleteByPostId(postId: String)

    @Query("DELETE FROM comments")
    suspend fun deleteAll()
}

@Dao
interface CommunityDao {
    @Query("SELECT * FROM communities ORDER BY name ASC")
    suspend fun getAll(): List<CommunityEntity>

    @Query("SELECT * FROM communities WHERE name = :name")
    suspend fun getByName(name: String): CommunityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(communities: List<CommunityEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(community: CommunityEntity)

    @Query("DELETE FROM communities")
    suspend fun deleteAll()
}
