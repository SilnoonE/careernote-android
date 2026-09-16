package com.thirtytwo_cereernote.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.thirtytwo_cereernote.data.model.*

@Database(
    entities = [
        Application::class,
        ApplicationStatusHistory::class,
        Interview::class,
        InterviewQuestion::class,
        CoverLetter::class,
        CoverLetterQuestion::class,
        Resume::class,
        Portfolio::class,
        Project::class,
        Certification::class,
        CareerExperience::class,
        Education::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(DateConverters::class, EnumConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun applicationDao(): ApplicationDao
    abstract fun careerDao(): CareerDao

    companion object {
        const val VERSION = 5
        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE applications ADD COLUMN connectedResumeId INTEGER DEFAULT null")
                db.execSQL("ALTER TABLE applications ADD COLUMN connectedCoverLetterId INTEGER DEFAULT null")
                db.execSQL("ALTER TABLE applications ADD COLUMN connectedPortfolioId INTEGER DEFAULT null")
                db.execSQL("ALTER TABLE applications ADD COLUMN resumeSnapshot TEXT DEFAULT null")
                db.execSQL("ALTER TABLE applications ADD COLUMN coverLetterSnapshot TEXT DEFAULT null")
                db.execSQL("ALTER TABLE applications ADD COLUMN portfolioSnapshot TEXT DEFAULT null")
                db.execSQL("ALTER TABLE applications ADD COLUMN submittedDate INTEGER DEFAULT null")
                db.execSQL("ALTER TABLE applications ADD COLUMN attachedPdfPath TEXT DEFAULT null")
                db.execSQL("ALTER TABLE applications ADD COLUMN attachedPdfName TEXT DEFAULT null")
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cover_letter_questions ADD COLUMN limitCount INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
