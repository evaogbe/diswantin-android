package io.github.evaogbe.diswantin.activity.data

import io.github.evaogbe.diswantin.data.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.ZonedDateTime
import javax.inject.Inject

class LocalActivityRepository @Inject constructor(
    private val activityDao: ActivityDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    clock: Clock,
) : ActivityRepository {
    override val currentActivityStream = activityDao.getCurrentActivity(
        scheduledBefore = ZonedDateTime.now(clock).plusHours(1).toInstant()
    ).flowOn(ioDispatcher)

    override fun getById(id: Long) = activityDao.getById(id).flowOn(ioDispatcher)

    override fun search(
        query: String,
        tailsOnly: Boolean,
        excludeChainFor: Long?
    ): Flow<List<Activity>> = when {
        !tailsOnly -> activityDao.search(escapeSql(query))
        excludeChainFor == null -> activityDao.searchTails(escapeSql(query))
        else -> activityDao.searchAvailableParents(escapeSql(query), excludeChainFor)
    }.flowOn(ioDispatcher)

    private fun escapeSql(str: String) =
        str.replace("'", "''").replace("\"", "\"\"")

    override fun getChain(id: Long) = activityDao.getChain(id).flowOn(ioDispatcher)

    override fun getParent(id: Long) = activityDao.getParent(id).flowOn(ioDispatcher)

    override fun hasActivities(excludeChainFor: Long?) =
        (excludeChainFor?.let { activityDao.hasActivitiesOutsideChain(it) }
            ?: activityDao.hasActivities())
            .flowOn(ioDispatcher)

    override suspend fun create(form: NewActivityForm): Activity {
        val activity = form.newActivity
        return withContext(ioDispatcher) {
            activity.copy(id = activityDao.insertWithParent(activity, form.prevActivityId))
        }
    }

    override suspend fun update(form: EditActivityForm) {
        withContext(ioDispatcher) {
            if (form.parentId == form.oldParentId) {
                activityDao.update(form.updatedActivity)
            } else {
                activityDao.updateAndReplaceParent(
                    activity = form.updatedActivity,
                    parentId = form.parentId,
                    oldParentId = form.oldParentId,
                )
            }
        }
    }

    override suspend fun remove(id: Long) {
        withContext(ioDispatcher) {
            activityDao.deleteWithChain(id)
        }
    }
}
