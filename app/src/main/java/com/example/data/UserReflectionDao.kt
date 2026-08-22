package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserReflectionDao {
    @Query("SELECT * FROM user_reflections ORDER BY timestamp DESC")
    fun getAllReflections(): Flow<List<UserReflection>>

    @Query("SELECT * FROM user_reflections WHERE id = :id LIMIT 1")
    fun getReflectionById(id: Long): Flow<UserReflection?>

    @Query("SELECT * FROM user_reflections WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchReflections(query: String): Flow<List<UserReflection>>

    @Query("SELECT COUNT(*) FROM user_reflections")
    fun getReflectionCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReflection(reflection: UserReflection): Long

    @Update
    suspend fun updateReflection(reflection: UserReflection)

    @Delete
    suspend fun deleteReflection(reflection: UserReflection)

    @Query("DELETE FROM user_reflections")
    suspend fun clearAllReflections()
}
