package io.github.evaogbe.diswantin.data

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.util.Locale

@Module
@InstallIn(SingletonComponent::class)
object TimeModule {
    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    fun provideLocale(): Locale = Locale.getDefault()
}
