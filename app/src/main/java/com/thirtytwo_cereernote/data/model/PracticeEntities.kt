package com.thirtytwo_cereernote.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "practice_sessions")
data class PracticeSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val applicationId: Long? = null,
    val startedAt: Date = Date(),
    val completedAt: Date? = null,
    val isCompleted: Boolean = false
)

@Entity(
    tableName = "practice_question_results",
    foreignKeys = [
        ForeignKey(
            entity = PracticeSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class PracticeQuestionResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val questionText: String,
    val sourceType: String = "", // INTERVIEW_QUESTION, COVER_LETTER
    val sourceLabel: String = "",
    val originalAnswer: String = "",
    val practiceAnswer: String = "",
    val selfEvaluation: String = "GOOD", // GOOD, NEEDS_WORK, RETRY
    val updatedAt: Date = Date()
)
