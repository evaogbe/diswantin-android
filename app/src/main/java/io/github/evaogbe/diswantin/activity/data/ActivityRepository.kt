package io.github.evaogbe.diswantin.activity.data

import kotlinx.coroutines.flow.Flow

interface ActivityRepository {
    val currentActivityStream: Flow<Activity?>

    fun getById(id: Long): Flow<Activity?>

    fun search(
        query: String,
        tailsOnly: Boolean = false,
        excludeChainFor: Long? = null
    ): Flow<List<Activity>>

    fun getChain(id: Long): Flow<List<Activity>>

    fun getParent(id: Long): Flow<Activity?>

    fun hasActivities(excludeChainFor: Long?): Flow<Boolean>

    suspend fun create(form: NewActivityForm): Activity

    suspend fun update(form: EditActivityForm)

    suspend fun remove(id: Long)
}
