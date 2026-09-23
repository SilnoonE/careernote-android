package com.thirtytwo_cereernote.di

import android.content.Context
import androidx.room.Room
import com.thirtytwo_cereernote.data.database.AppDatabase
import com.thirtytwo_cereernote.data.database.ApplicationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "careernote_db"
        ).addMigrations(
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideApplicationDao(database: AppDatabase): ApplicationDao {
        return database.applicationDao()
    }

    @Provides
    fun provideCareerDao(database: AppDatabase): com.thirtytwo_cereernote.data.database.CareerDao {
        return database.careerDao()
    }
}
