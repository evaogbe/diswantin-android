package io.github.evaogbe.diswantin.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface PreferencesModule {
    @Binds
    fun clockMonitor(broadcastClockMonitor: BroadcastClockMonitor): ClockMonitor
}
