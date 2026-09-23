package com.thirtytwo_cereernote.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
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
        Education::class,
        PracticeSession::class,
        PracticeQuestionResult::class
    ],
    version = 7,
    exportSchema = true
)
@TypeConverters(DateConverters::class, EnumConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun applicationDao(): ApplicationDao
    abstract fun careerDao(): CareerDao

    companion object {
        const val VERSION = 7

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "careernote_db"
                )
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

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

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE applications ADD COLUMN requiredDocTypes TEXT NOT NULL DEFAULT 'RESUME,COVER_LETTER'")
                db.execSQL("ALTER TABLE interviews ADD COLUMN isCompleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE interviews ADD COLUMN nextPreparations TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE interviews ADD COLUMN reviewCompletedAt INTEGER DEFAULT null")

                // Infer reviewCompletedAt for existing records that had non-blank strengths/weaknesses/review
                db.execSQL("""
                    UPDATE interviews
                    SET reviewCompletedAt = interviewDate
                    WHERE (strengths IS NOT NULL AND length(trim(strengths)) > 0)
                       OR (weaknesses IS NOT NULL AND length(trim(weaknesses)) > 0)
                       OR (review IS NOT NULL AND length(trim(review)) > 0)
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `practice_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `applicationId` INTEGER DEFAULT null,
                        `startedAt` INTEGER NOT NULL,
                        `completedAt` INTEGER DEFAULT null,
                        `isCompleted` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `practice_question_results` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sessionId` INTEGER NOT NULL,
                        `questionText` TEXT NOT NULL,
                        `sourceType` TEXT NOT NULL DEFAULT '',
                        `sourceLabel` TEXT NOT NULL DEFAULT '',
                        `originalAnswer` TEXT NOT NULL DEFAULT '',
                        `practiceAnswer` TEXT NOT NULL DEFAULT '',
                        `selfEvaluation` TEXT NOT NULL DEFAULT 'GOOD',
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`sessionId`) REFERENCES `practice_sessions`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_practice_question_results_sessionId` ON `practice_question_results` (`sessionId`)")
            }
        }
    }
}
