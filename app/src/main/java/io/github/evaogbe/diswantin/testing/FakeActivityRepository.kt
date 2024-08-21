package io.github.evaogbe.diswantin.testing

import io.github.evaogbe.diswantin.activity.data.Activity
import io.github.evaogbe.diswantin.activity.data.ActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import java.time.Instant
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty0

class FakeActivityRepository(initialActivities: List<Activity>) : ActivityRepository {
    constructor(vararg initialActivities: Activity) : this(initialActivities.toList())

    private val activitiesState = MutableStateFlow(initialActivities)

    val activities
        get() = activitiesState.value

    private val throwingMethods = MutableStateFlow(setOf<KFunction<*>>())

    private var idGen = 0L

    override val currentActivityStream: Flow<Activity?> =
        combine(activitiesState, throwingMethods) { activities, throwingMethods ->
            if (::currentActivityStream::get in throwingMethods) {
                throw RuntimeException("Test")
            }

            val plannedActivityComparator =
                compareBy(Activity::skippedAt).thenComparing(Activity::createdAt)
            activities.sortedWith(plannedActivityComparator).firstOrNull()
        }

    override suspend fun findById(id: Long): Activity {
        if (::findById in throwingMethods.value) {
            throw RuntimeException("Test")
        }

        return activities.first { it.id == id }
    }

    override fun search(query: String): Flow<List<Activity>> {
        return combine(activitiesState, throwingMethods) { activities, throwingMethods ->
            if (::search in throwingMethods) {
                throw RuntimeException("Test")
            }

            activities.filter { it.name.contains(query, ignoreCase = true) }
        }
    }

    override suspend fun create(name: String): Activity {
        if (::create in throwingMethods.value) {
            throw RuntimeException("Test")
        }

        val activity = Activity(id = ++idGen, createdAt = Instant.now(), name = name.trim())
        activitiesState.update { it + activity }
        return activity
    }

    override suspend fun update(activity: Activity) {
        if (::update in throwingMethods.value) {
            throw RuntimeException("Test")
        }

        activitiesState.update { activities ->
            activities.map { if (it.id == activity.id) activity else it }
        }
    }

    override suspend fun remove(activity: Activity) {
        if (::remove in throwingMethods.value) {
            throw RuntimeException("Test")
        }

        activitiesState.update { activities ->
            activities.filterNot { it.id == activity.id }
        }
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
