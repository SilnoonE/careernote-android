package com.thirtytwo_cereernote.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "cover_letters")
data class CoverLetter(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val jobTitle: String = "",
    val companyName: String = "",
    val version: String = "1.0",
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val memo: String = ""
)

@Entity(tableName = "cover_letter_questions")
data class CoverLetterQuestion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val coverLetterId: Long,
    val question: String,
    val answer: String,
    val wordCount: Int = 0,
    val limitCount: Int = 0
)

@Entity(tableName = "resumes")
data class Resume(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val version: String = "1.0",
    val filePath: String = "",
    val memo: String = "",
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

@Entity(tableName = "portfolios")
data class Portfolio(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val version: String = "1.0",
    val url: String = "",
    val filePath: String = "",
    val memo: String = "",
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val period: String = "",
    val role: String = "",
    val teamSize: Int = 1,
    val description: String = "",
    val problem: String = "",
    val solution: String = "",
    val techStack: String = "", // Comma separated or separate table
    val outcome: String = "",
    val url: String = "",
    val githubUrl: String = "",
    val status: String = "COMPLETED", // PLANNED, ONGOING, COMPLETED, DROPPED
    val memo: String = "",
    val updatedAt: Date = Date()
)

@Entity(tableName = "certifications")
data class Certification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val issuer: String = "",
    val status: String = "ACQUIRED", // PLANNED, STUDYING, SCHEDULED, WAITING, ACQUIRED, FAILED
    val acquisitionDate: Date? = null,
    val scheduledDate: Date? = null,
    val score: String = "",
    val grade: String = "",
    val expiryDate: Date? = null,
    val memo: String = "",
    val updatedAt: Date = Date()
)

@Entity(tableName = "career_experiences")
data class CareerExperience(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companyName: String,
    val jobTitle: String,
    val startDate: Date,
    val endDate: Date? = null,
    val isCurrent: Boolean = false,
    val employmentType: EmploymentType = EmploymentType.FULL_TIME,
    val description: String = "",
    val outcome: String = "",
    val techStack: String = "",
    val memo: String = "",
    val updatedAt: Date = Date()
)

@Entity(tableName = "educations")
data class Education(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val institution: String,
    val startDate: Date,
    val endDate: Date? = null,
    val status: String = "GRADUATED", // ONGOING, GRADUATED, DROPPED
    val description: String = "",
    val memo: String = "",
    val updatedAt: Date = Date()
)
