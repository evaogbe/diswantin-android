package io.github.evaogbe.diswantin.activity.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface ActivityDataModule {
    @Binds
    fun bindActivityRepository(localActivityRepository: LocalActivityRepository): ActivityRepository
}
