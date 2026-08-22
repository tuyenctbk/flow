package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FlowSessionDao {
    @Query("SELECT * FROM flow_sessions ORDER BY timestamp DESC")
    fun getAllFlowSessions(): Flow<List<FlowSession>>

    @Query("SELECT * FROM flow_sessions WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    fun getSessionsSince(sinceTimestamp: Long): Flow<List<FlowSession>>

    @Query("SELECT COUNT(*) FROM flow_sessions")
    fun getTotalSessionsCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM flow_sessions")
    fun getTotalFocusSeconds(): Flow<Long>

    @Query("SELECT COALESCE(AVG(flowScore), 0.0) FROM flow_sessions")
    fun getAverageFlowScore(): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FlowSession): Long

    @Delete
    suspend fun deleteSession(session: FlowSession)

    @Query("DELETE FROM flow_sessions")
    suspend fun clearAllSessions()
}
