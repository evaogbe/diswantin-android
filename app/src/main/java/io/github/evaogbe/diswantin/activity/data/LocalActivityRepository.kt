package io.github.evaogbe.diswantin.activity.data

import io.github.evaogbe.diswantin.data.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.ZonedDateTime
import javax.inject.Inject

class LocalActivityRepository @Inject constructor(
    private val activityDao: ActivityDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val clock: Clock,
) : ActivityRepository {
    override val currentActivityStream = activityDao.getCurrentActivity(
        scheduledBefore = ZonedDateTime.now(clock).plusHours(1).toInstant()
    ).flowOn(ioDispatcher)

    override suspend fun findById(id: Long) = withContext(ioDispatcher) {
        activityDao.findById(id)
    }

    override fun search(query: String) = activityDao.search(escapeSql(query)).flowOn(ioDispatcher)

    private fun escapeSql(str: String) =
        str.replace("'", "''").replace("\"", "\"\"")

    override suspend fun create(form: ActivityForm): Activity {
        val activity = form.getNewActivity(clock)
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
