package io.github.evaogbe.diswantin.task.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface TaskDataModule {
    @Binds
    fun bindTaskRepository(localTaskRepository: LocalTaskRepository): TaskRepository

    @Binds
    fun bindTaskListRepository(
        localTaskListRepository: LocalTaskListRepository
    ): TaskListRepository
}
