package io.github.evaogbe.diswantin.testing

import io.github.evaogbe.diswantin.activity.data.Activity
import io.github.evaogbe.diswantin.activity.data.ActivityPath
import io.github.evaogbe.diswantin.activity.data.ActivityRepository
import io.github.evaogbe.diswantin.activity.data.EditActivityForm
import io.github.evaogbe.diswantin.activity.data.NewActivityForm
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import java.time.ZonedDateTime
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty0

class FakeActivityRepository(initialActivities: Collection<Activity>) : ActivityRepository {
    constructor(vararg initialActivities: Activity) : this(initialActivities.toSet())

    private val throwingMethods = MutableStateFlow(setOf<KFunction<*>>())

    private var activityIdGen = initialActivities.maxOfOrNull { it.id } ?: 0L

    private var activityPathIdGen = 0L

    private val activitiesTable = MutableStateFlow(initialActivities.associateBy { it.id })

    private val activityPaths = MutableStateFlow(initialActivities.map { activity ->
        ActivityPath(
            id = ++activityPathIdGen,
            ancestor = activity.id,
            descendant = activity.id,
            depth = 0,
        )
    }.toSet())

    val activities
        get() = activitiesTable.value.values

    override val currentActivityStream: Flow<Activity?> =
        combine(
            throwingMethods,
            activitiesTable,
            activityPaths,
        ) { throwingMethods, activities, activityPaths ->
            if (::currentActivityStream::get in throwingMethods) {
                throw RuntimeException("Test")
            }

            val scheduledBefore = ZonedDateTime.now().plusHours(1).toInstant()
            activities.values.sortedWith(
                compareBy(nullsLast(), Activity::scheduledAt)
                    .thenComparing(Activity::dueAt, nullsLast())
                    .thenComparing(Activity::createdAt)
                    .thenComparing(Activity::id)
            ).mapNotNull { activity ->
                val path = activityPaths.filter { it.descendant == activity.id }.maxBy { it.depth }
                activities[path.ancestor]
            }.firstOrNull { activity ->
                activity.scheduledAt?.let { it <= scheduledBefore } != false
            }
        }

    override fun getById(id: Long): Flow<Activity?> =
        combine(throwingMethods, activitiesTable) { throwingMethods, activities ->
            if (::getById in throwingMethods) {
                throw RuntimeException("Test")
            }

            activities[id]
        }

    override fun search(
        query: String,
        tailsOnly: Boolean,
        excludeChainFor: Long?
    ): Flow<List<Activity>> =
        combine(
            throwingMethods,
            activitiesTable,
            activityPaths,
        ) { throwingMethods, activities, activityPaths ->
            if (::search in throwingMethods) {
                throw RuntimeException("Test")
            }

            val excludeIds = if (tailsOnly) {
                activityPaths.filter { it.depth > 0 }
                    .map { it.ancestor }
                    .toSet() + activityPaths.filter { it.ancestor == excludeChainFor }
                    .map { it.descendant }
            } else {
                emptySet()
            }
            activities.values.filter {
                it.name.contains(query, ignoreCase = true) && it.id !in excludeIds
            }
        }

    override fun getChain(id: Long): Flow<List<Activity>> =
        combine(
            throwingMethods,
            activitiesTable,
            activityPaths,
        ) { throwingMethods, activities, activityPaths ->
            if (::getChain in throwingMethods) {
                throw RuntimeException("Test")
            }

            val head = activityPaths.filter { it.descendant == id }.maxByOrNull { it.depth }
            activityPaths.filter { it.ancestor == head?.ancestor }
                .sortedByDescending { it.depth }
                .mapNotNull { activities[it.descendant] }
        }

    override fun getParent(id: Long): Flow<Activity?> =
        combine(
            throwingMethods,
            activitiesTable,
            activityPaths,
        ) { throwingMethods, activities, activityPaths ->
            if (::getParent in throwingMethods) {
                throw RuntimeException("Test")
            }

            activityPaths.firstOrNull { it.descendant == id && it.depth == 1 }?.let {
                activities[it.ancestor]
            }
        }

    override fun hasActivities(excludeChainFor: Long?): Flow<Boolean> =
        combine(
            throwingMethods,
            activitiesTable,
            activityPaths,
        ) { throwingMethods, activities, activityPaths ->
            if (::hasActivities in throwingMethods) {
                throw RuntimeException("Test")
            }

            if (excludeChainFor == null) {
                activities.isNotEmpty()
            } else {
                val chainIds = activityPaths.filter {
                    it.descendant == excludeChainFor
                }.map {
                    it.ancestor
                }.toSet() + activityPaths.filter {
                    it.ancestor == excludeChainFor
                }.map {
                    it.descendant
                }
                activities.values.any { it.id !in chainIds }
            }
        }

    override suspend fun create(form: NewActivityForm): Activity {
        if (::create in throwingMethods.value) {
            throw RuntimeException("Test")
        }

        val activity = form.newActivity.copy(id = ++activityIdGen)
        val leafPath = ActivityPath(
            id = ++activityPathIdGen,
            ancestor = activity.id,
            descendant = activity.id,
            depth = 0,
        )
        activitiesTable.update { it + (activity.id to activity) }
        activityPaths.update { paths ->
            paths + leafPath + paths.filter {
                it.descendant == form.prevActivityId
            }.map {
                ActivityPath(
                    id = ++activityPathIdGen,
                    ancestor = it.id,
                    descendant = activity.id,
                    depth = it.depth + 1
                )
            }
        }
        return activity
    }

    override suspend fun update(form: EditActivityForm) {
        if (::update in throwingMethods.value) {
            throw RuntimeException("Test")
        }

        val activity = form.updatedActivity
        activitiesTable.update { it + (activity.id to activity) }

        if (form.parentId != form.oldParentId) {
            activityPaths.update { paths ->
                val pathsToRemove = form.oldParentId?.let { oldParentId ->
                    val ancestors =
                        paths.filter { it.descendant == oldParentId }.map { it.ancestor }.toSet()
                    val descendants =
                        paths.filter { it.ancestor == activity.id }.map { it.descendant }.toSet()
                    paths.filter { it.ancestor in ancestors && it.descendant in descendants }
                        .toSet()
                } ?: emptySet()
                val pathsToAdd = form.parentId?.let { parentId ->
                    paths.filter {
                        it.descendant == parentId
                    }.map {
                        ActivityPath(
                            id = ++activityPathIdGen,
                            ancestor = it.id,
                            descendant = activity.id,
                            depth = it.depth + 1
                        )
                    } + paths.filter {
                        it.descendant == parentId
                    }.flatMap { p1 ->
                        paths.filter {
                            it.ancestor == activity.id && it.depth > 0
                        }.map { p2 ->
                            ActivityPath(
                                id = ++activityPathIdGen,
                                ancestor = p1.ancestor,
                                descendant = p2.descendant,
                                depth = p1.depth + p2.depth + 1,
                            )
                        }
                    }
                } ?: emptySet()
                paths - pathsToRemove + pathsToAdd
            }
        }
    }

    override suspend fun remove(id: Long) {
        if (::remove in throwingMethods.value) {
            throw RuntimeException("Test")
        }

        activityPaths.update { paths ->
            val parentId = paths.firstOrNull { it.descendant == id && it.depth == 1 }?.let {
                activitiesTable.value[it.ancestor]
            }?.id
            val childId = paths.firstOrNull { it.ancestor == id && it.depth == 1 }?.let {
                activitiesTable.value[it.descendant]
            }?.id
            val chainPaths = if (parentId != null && childId != null) {
                val ancestors =
                    paths.filter { it.descendant == parentId }.map { it.ancestor }.toSet()
                val descendants =
                    paths.filter { it.ancestor == childId }.map { it.descendant }.toSet()
                paths.filter { it.ancestor in ancestors && it.descendant in descendants }.toSet()
            } else {
                emptySet()
            }
            paths - paths.filter { it.ancestor == id || it.descendant == id }.toSet() -
                    chainPaths +
                    chainPaths.map { it.copy(depth = it.depth - 1) }
        }
        activitiesTable.update { it - id }
    }

    fun setThrows(property: KProperty0<*>, showThrow: Boolean) {
        setThrows(property::get, showThrow)
    }

    fun setThrows(method: KFunction<*>, shouldThrow: Boolean) {
        if (shouldThrow) {
            throwingMethods.update { it + method }
        } else {
            throwingMethods.update { it - method }
        }
    }
}
