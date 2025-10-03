package io.github.evaogbe.diswantin.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BroadcastClockMonitor @Inject constructor(
    @ApplicationContext context: Context,
    @ApplicationScope appScope: CoroutineScope,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : ClockMonitor {
    private val timeZone = callbackFlow {
        trySend(ZoneId.systemDefault())

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != Intent.ACTION_TIMEZONE_CHANGED) return

                val zoneId = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    null
                } else {
                    intent.getStringExtra(Intent.EXTRA_TIMEZONE)?.let { zoneId ->
                        ZoneId.of(zoneId, ZoneId.SHORT_IDS)
                    }
                }

                trySend(zoneId ?: ZoneId.systemDefault())
            }
        }
        val filter = IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)

        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED,
        )

        trySend(ZoneId.systemDefault())

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }.distinctUntilChanged().conflate().flowOn(ioDispatcher)
        .shareIn(scope = appScope, started = SharingStarted.WhileSubscribed(), replay = 1)

    private val currentTimeMillis = callbackFlow {
        trySend(System.currentTimeMillis())

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action in dateTimeActions) {
                    trySend(System.currentTimeMillis())
                }
            }
        }
        val filter = IntentFilter().apply {
            dateTimeActions.forEach { addAction(it) }
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED,
        )

        trySend(System.currentTimeMillis())

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }.distinctUntilChanged().conflate().flowOn(ioDispatcher)
        .shareIn(scope = appScope, started = SharingStarted.WhileSubscribed(), replay = 1)

    override val clock = combine(timeZone, currentTimeMillis) { zone, _ ->
        Clock.system(zone)
    }

    companion object {
        private val dateTimeActions = setOf(Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIME_CHANGED)
    }
}
