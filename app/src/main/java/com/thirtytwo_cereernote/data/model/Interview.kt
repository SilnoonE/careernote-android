package com.thirtytwo_cereernote.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "interviews",
    foreignKeys = [
        ForeignKey(
            entity = Application::class,
            parentColumns = ["id"],
            childColumns = ["applicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["applicationId"])]
)
data class Interview(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val applicationId: Long,
    val stage: String, // 예: 1차 면접, 2차 면접, 최종 면접
    val interviewDate: Date,
    val location: String = "",
    val isOnline: Boolean = false,
    val interviewers: String = "",
    val method: String = "", // 예: 다대다, 일대일
    val result: String = "",
    val memo: String = "",
    val review: String = "", // 복기
    val strengths: String = "", // 잘한 점
    val weaknesses: String = "", // 아쉬운 점
    val preparations: String = "" // 다음 준비사항
)

@Entity(
    tableName = "interview_questions",
    foreignKeys = [
        ForeignKey(
            entity = Interview::class,
            parentColumns = ["id"],
            childColumns = ["interviewId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["interviewId"])]
)
data class InterviewQuestion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val interviewId: Long? = null, // Nullable for standalone career questions
    val question: String,
    val myAnswer: String = "",
    val betterAnswer: String = "",
    val difficulty: Int = 3, // 1~5
    val category: String = "", // 자기소개, 지원동기, 직무, 기술 등
    val memo: String = "",
    val updatedAt: Date = Date()
)
