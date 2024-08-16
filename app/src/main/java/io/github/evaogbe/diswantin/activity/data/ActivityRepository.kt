package io.github.evaogbe.diswantin.activity.data

import kotlinx.coroutines.flow.Flow

interface ActivityRepository {
    val currentActivityStream: Flow<Activity?>

    fun findById(id: Long): Flow<Activity?>

    fun search(query: String): Flow<List<Activity>>

    suspend fun create(name: String): Activity

    suspend fun update(activity: Activity)

    suspend fun remove(activity: Activity)
}
