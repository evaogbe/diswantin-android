package io.github.evaogbe.diswantin.app.data

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext appContext: Context) =
        DiswantinDatabase.createDatabase(appContext)

    @Provides
    fun provideTaskDao(db: DiswantinDatabase) = db.taskDao()

    @Provides
    fun provideTagDao(db: DiswantinDatabase) = db.tagDao()
}
