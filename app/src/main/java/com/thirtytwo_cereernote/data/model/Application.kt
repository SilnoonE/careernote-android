package com.thirtytwo_cereernote.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "applications")
data class Application(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companyName: String,
    val jobTitle: String,
    val employmentType: EmploymentType = EmploymentType.FULL_TIME,
    val workMode: WorkMode = WorkMode.OFFICE,
    val location: String = "",
    val companySize: CompanySize = CompanySize.OTHER,
    val channel: String = "", // 지원 경로
    val noticeUrl: String = "",
    val appliedDate: Date = Date(),
    val deadlineDate: Date? = null,
    val targetSalary: Long = 0,
    val offeredSalary: Long = 0,
    val jobCategory: String = "",
    val currentStatus: ApplicationStatus = ApplicationStatus.APPLY_PLANNED,
    val memo: String = "",
    val isFavorite: Boolean = false,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val connectedResumeId: Long? = null,
    val connectedCoverLetterId: Long? = null,
    val connectedPortfolioId: Long? = null,
    val resumeSnapshot: String? = null,
    val coverLetterSnapshot: String? = null,
    val portfolioSnapshot: String? = null,
    val submittedDate: Date? = null,
    val attachedPdfPath: String? = null,
    val attachedPdfName: String? = null
)
