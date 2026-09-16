package com.thirtytwo_cereernote.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CoverLetterSnapshot(
    val id: Long,
    val title: String,
    val companyName: String,
    val jobTitle: String,
    val version: String,
    val memo: String,
    val questions: List<CoverLetterQuestionSnapshot> = emptyList()
)

@Serializable
data class CoverLetterQuestionSnapshot(
    val question: String,
    val answer: String,
    val wordCount: Int,
    val limitCount: Int = 0
)

@Serializable
data class ResumeSnapshot(
    val id: Long,
    val title: String,
    val version: String,
    val filePath: String,
    val memo: String
)

@Serializable
data class PortfolioSnapshot(
    val id: Long,
    val title: String,
    val version: String,
    val url: String,
    val filePath: String,
    val memo: String
)

@Serializable
data class Draft(
    val type: String,
    val itemId: Long,
    val field1: String = "",
    val field2: String = "",
    val field3: String = "",
    val field4: String = "",
    val field5: String = "",
    val field6: String = "",
    val field7: String = "",
    val field8: String = "",
    val field9: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

