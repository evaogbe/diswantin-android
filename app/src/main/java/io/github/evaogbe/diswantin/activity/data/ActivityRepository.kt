package io.github.evaogbe.diswantin.activity.data

import kotlinx.coroutines.flow.Flow

interface ActivityRepository {
    val currentActivityStream: Flow<Activity?>

    suspend fun findById(id: Long): Activity

    fun search(query: String): Flow<List<Activity>>

    suspend fun create(form: ActivityForm): Activity

    suspend fun update(activity: Activity)

    suspend fun remove(activity: Activity)
}
