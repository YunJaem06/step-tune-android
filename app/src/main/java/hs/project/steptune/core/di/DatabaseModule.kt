package hs.project.steptune.core.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import hs.project.steptune.data.local.database.DayRecordDao
import hs.project.steptune.data.local.database.StepTuneDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideStepTuneDatabase(
        @ApplicationContext context: Context
    ): StepTuneDatabase {
        return Room.databaseBuilder(
            context,
            StepTuneDatabase::class.java,
            StepTuneDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    fun provideDayRecordDao(database: StepTuneDatabase): DayRecordDao = database.dayRecordDao()
}

