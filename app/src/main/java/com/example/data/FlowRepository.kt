package com.example.data

import kotlinx.coroutines.flow.Flow

class FlowRepository(
    private val flowSessionDao: FlowSessionDao,
    private val userReflectionDao: UserReflectionDao
) {
    // Flow Sessions
    val allSessions: Flow<List<FlowSession>> = flowSessionDao.getAllFlowSessions()
    val totalFocusSeconds: Flow<Long> = flowSessionDao.getTotalFocusSeconds()
    val totalSessionsCount: Flow<Int> = flowSessionDao.getTotalSessionsCount()
    val averageFlowScore: Flow<Double> = flowSessionDao.getAverageFlowScore()

    fun getSessionsSince(sinceTimestamp: Long): Flow<List<FlowSession>> {
        return flowSessionDao.getSessionsSince(sinceTimestamp)
    }

    suspend fun insertSession(session: FlowSession): Long {
        return flowSessionDao.insertSession(session)
    }

    suspend fun deleteSession(session: FlowSession) {
        flowSessionDao.deleteSession(session)
    }

    suspend fun clearAllSessions() {
        flowSessionDao.clearAllSessions()
    }

    // User Reflections
    val allReflections: Flow<List<UserReflection>> = userReflectionDao.getAllReflections()
    val reflectionCount: Flow<Int> = userReflectionDao.getReflectionCount()

    fun getReflectionById(id: Long): Flow<UserReflection?> {
        return userReflectionDao.getReflectionById(id)
    }

    fun searchReflections(query: String): Flow<List<UserReflection>> {
        return userReflectionDao.searchReflections(query)
    }

    suspend fun insertReflection(reflection: UserReflection): Long {
        return userReflectionDao.insertReflection(reflection)
    }

    suspend fun updateReflection(reflection: UserReflection) {
        userReflectionDao.updateReflection(reflection)
    }

    suspend fun deleteReflection(reflection: UserReflection) {
        userReflectionDao.deleteReflection(reflection)
    }

    suspend fun clearAllReflections() {
        userReflectionDao.clearAllReflections()
    }

    // Backward compatibility aliases
    suspend fun insert(session: FlowSession) = insertSession(session)
    suspend fun delete(session: FlowSession) = deleteSession(session)
    suspend fun clearAll() {
        clearAllSessions()
        clearAllReflections()
    }
}

