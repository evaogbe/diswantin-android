package io.github.evaogbe.diswantin.activity.data

import io.github.evaogbe.diswantin.data.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

class LocalActivityRepository @Inject constructor(
    private val activityDao: ActivityDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ActivityRepository {
    override val currentActivityStream = activityDao.getCurrentActivity()

    override fun findById(id: Long) = activityDao.findById(id)

    override fun search(query: String) = activityDao.search(escapeSql(query))

    private fun escapeSql(str: String) =
        str.replace("'", "''").replace("\"", "\"\"")

    override suspend fun create(name: String): Activity {
        val activity = Activity(createdAt = Instant.now(), name = name.trim())
        return withContext(ioDispatcher) {
            activity.copy(id = activityDao.insert(activity))
        }
    }

    override suspend fun update(activity: Activity) {
        withContext(ioDispatcher) {
            activityDao.update(activity)
        }
    }

    override suspend fun remove(activity: Activity) {
        withContext(ioDispatcher) {
            activityDao.delete(activity)
        }
    }
}
