package com.thirtytwo_cereernote.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thirtytwo_cereernote.data.model.*
import com.thirtytwo_cereernote.data.repository.ApplicationRepository
import com.thirtytwo_cereernote.data.repository.CareerRepository
import com.thirtytwo_cereernote.data.repository.PreferenceRepository
import com.thirtytwo_cereernote.util.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.util.Date
import javax.inject.Inject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@HiltViewModel
class ApplicationsViewModel @Inject constructor(
    private val repository: ApplicationRepository,
    private val careerRepository: CareerRepository,
    private val preferenceRepository: PreferenceRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {
    
    private val notificationHelper = NotificationHelper(context)

    val drafts = preferenceRepository.draftsJson.map { 
        try {
            Json.decodeFromString<Map<String, Draft>>(it)
        } catch (e: Exception) {
            emptyMap<String, Draft>()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun saveDraft(companyName: String, jobTitle: String, status: String, deadline: String) {
        viewModelScope.launch(Dispatchers.IO) {
            preferenceRepository.updateDraft("application", 0L) {
                Draft("application", 0L, companyName, jobTitle, status, deadline)
            }
        }
    }

    fun clearApplicationDraft() {
        viewModelScope.launch(Dispatchers.IO) {
            preferenceRepository.updateDraft("application", 0L) { null }
        }
    }

    val allCoverLetters = careerRepository.getAllCoverLetters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allResumes = careerRepository.getAllResumes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val allPortfolios = careerRepository.getAllPortfolios()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchQuery = MutableStateFlow("")
    val statusFilter = MutableStateFlow<ApplicationStatus?>(null)
    val favoriteFilter = MutableStateFlow(false)
    val sortBy = MutableStateFlow("appliedDate")

    val isInitialEmpty: StateFlow<Boolean> = repository.allApplications
        .map { it.isEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val applications: StateFlow<List<Application>> = combine(
        repository.allApplications,
        searchQuery,
        statusFilter,
        favoriteFilter,
        sortBy
    ) { apps, query, status, favOnly, sort ->
        var result = apps
        if (query.isNotBlank()) {
            result = result.filter {
                it.companyName.contains(query, ignoreCase = true) ||
                it.jobTitle.contains(query, ignoreCase = true) ||
                it.memo.contains(query, ignoreCase = true)
            }
        }
        if (status != null) result = result.filter { it.currentStatus == status }
        if (favOnly) result = result.filter { it.isFavorite }

        when (sort) {
            "appliedDate" -> result.sortedByDescending { it.appliedDate }
            "deadlineDate" -> result.sortedBy { it.deadlineDate ?: Date(Long.MAX_VALUE) }
            "updatedAt" -> result.sortedByDescending { it.updatedAt }
            else -> result.sortedByDescending { it.appliedDate }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleFavorite(application: Application) {
        viewModelScope.launch {
            repository.updateFavorite(application.id, !application.isFavorite)
        }
    }

    fun addApplication(application: Application) {
        viewModelScope.launch {
            val id = repository.insertApplication(application)
            if (id > 0) {
                clearApplicationDraft()
                application.deadlineDate?.let {
                    val triggerTime = it.time - (24 * 60 * 60 * 1000)
                    notificationHelper.scheduleNotification(
                        id,
                        NotificationHelper.TYPE_APPLICATION,
                        "지원 마감 알림: ${application.companyName}",
                        "${application.jobTitle} 공고 마감이 하루 남았습니다.",
                        triggerTime
                    )
                }
            }
        }
    }

    fun deleteApplication(application: Application) {
        viewModelScope.launch {
            // Delete attached files and snapshots
            val filesToDelete = mutableListOf<String?>()
            filesToDelete.add(application.attachedPdfPath)
            
            // Try to extract paths from snapshots
            try {
                application.resumeSnapshot?.let { json ->
                    val obj = Json.decodeFromString<ResumeSnapshot>(json)
                    if (obj.filePath.contains("resume_snapshot_")) filesToDelete.add(obj.filePath)
                }
                application.portfolioSnapshot?.let { json ->
                    val obj = Json.decodeFromString<PortfolioSnapshot>(json)
                    if (obj.filePath.contains("portfolio_snapshot_")) filesToDelete.add(obj.filePath)
                }
            } catch (e: Exception) {}

            filesToDelete.filterNotNull().forEach { path ->
                try {
                    val file = java.io.File(path)
                    if (file.exists() && file.parentFile?.absolutePath == context.filesDir.absolutePath) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            repository.deleteApplication(application)
            notificationHelper.cancelNotification(application.id, NotificationHelper.TYPE_APPLICATION)
            val interviews = repository.getInterviewsByApplicationId(application.id).first()
            interviews.forEach { 
                notificationHelper.cancelNotification(it.id, NotificationHelper.TYPE_INTERVIEW)
            }
        }
    }

    fun updateStatus(applicationId: Long, newStatus: ApplicationStatus, memo: String = "") {
        viewModelScope.launch {
            repository.updateStatus(applicationId, newStatus, memo)

            if (newStatus == ApplicationStatus.CANCELLED || 
                newStatus == ApplicationStatus.DOCUMENT_FAILED ||
                newStatus == ApplicationStatus.INTERVIEW_FAILED) {
                notificationHelper.cancelNotification(applicationId, NotificationHelper.TYPE_APPLICATION)
            }
        }
    }

    fun getStatusHistories(applicationId: Long): Flow<List<ApplicationStatusHistory>> {
        return repository.getStatusHistories(applicationId)
    }

    fun getInterviews(applicationId: Long): Flow<List<Interview>> {
        return repository.getInterviewsByApplicationId(applicationId)
    }

    fun addInterview(interview: Interview) {
        viewModelScope.launch {
            val id = repository.insertInterview(interview)
            if (id > 0) {
                val triggerTime = interview.interviewDate.time - (2 * 60 * 60 * 1000)
                notificationHelper.scheduleNotification(
                    id,
                    NotificationHelper.TYPE_INTERVIEW,
                    "면접 일정 알림: ${interview.stage}",
                    "오늘 ${interview.stage} 일정이 있습니다. 장소: ${interview.location}",
                    triggerTime,
                    extraId = interview.applicationId
                )
            }
        }
    }

    fun removeInterview(interview: Interview) {
        viewModelScope.launch {
            repository.deleteInterview(interview)
            notificationHelper.cancelNotification(interview.id, NotificationHelper.TYPE_INTERVIEW)
        }
    }

    fun updateApplicationInfo(application: Application) {
        viewModelScope.launch {
            repository.updateApplication(application)
            notificationHelper.cancelNotification(application.id, NotificationHelper.TYPE_APPLICATION)
            application.deadlineDate?.let {
                val triggerTime = it.time - (24 * 60 * 60 * 1000)
                notificationHelper.scheduleNotification(
                    application.id,
                    NotificationHelper.TYPE_APPLICATION,
                    "지원 마감 알림: ${application.companyName}",
                    "${application.jobTitle} 공고 마감이 하루 남았습니다.",
                    triggerTime
                )
            }
        }
    }

    fun linkCoverLetter(application: Application, coverLetter: CoverLetter) {
        viewModelScope.launch(Dispatchers.IO) {
            val questions = careerRepository.getQuestionsByCoverLetterIdList(coverLetter.id)
            val snapshotObj = CoverLetterSnapshot(
                id = coverLetter.id,
                title = coverLetter.title,
                companyName = coverLetter.companyName,
                jobTitle = coverLetter.jobTitle,
                version = coverLetter.version,
                memo = coverLetter.memo,
                questions = questions.map { 
                    CoverLetterQuestionSnapshot(
                        question = it.question,
                        answer = it.answer,
                        wordCount = it.wordCount,
                        limitCount = it.limitCount
                    )
                }
            )
            val snapshotJson = Json.encodeToString(snapshotObj)
            repository.updateApplication(application.copy(
                connectedCoverLetterId = coverLetter.id,
                coverLetterSnapshot = snapshotJson,
                updatedAt = Date()
            ))
        }
    }

    fun linkResume(application: Application, resume: Resume) {
        viewModelScope.launch(Dispatchers.IO) {
            var snapshotFilePath = resume.filePath
            if (resume.filePath.isNotBlank()) {
                try {
                    val original = java.io.File(resume.filePath)
                    if (original.exists()) {
                        val snapshotFile = java.io.File(context.filesDir, "resume_snapshot_${application.id}_${System.currentTimeMillis()}.pdf")
                        original.copyTo(snapshotFile, true)
                        snapshotFilePath = snapshotFile.absolutePath
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val snapshotObj = ResumeSnapshot(
                id = resume.id,
                title = resume.title,
                version = resume.version,
                filePath = snapshotFilePath,
                memo = resume.memo
            )
            val snapshotJson = Json.encodeToString(snapshotObj)
            repository.updateApplication(application.copy(
                connectedResumeId = resume.id,
                resumeSnapshot = snapshotJson,
                updatedAt = Date()
            ))
        }
    }

    fun linkPortfolio(application: Application, portfolio: Portfolio) {
        viewModelScope.launch(Dispatchers.IO) {
            var snapshotFilePath = portfolio.filePath
            if (portfolio.filePath.isNotBlank()) {
                try {
                    val original = java.io.File(portfolio.filePath)
                    if (original.exists()) {
                        val snapshotFile = java.io.File(context.filesDir, "portfolio_snapshot_${application.id}_${System.currentTimeMillis()}.pdf")
                        original.copyTo(snapshotFile, true)
                        snapshotFilePath = snapshotFile.absolutePath
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val snapshotObj = PortfolioSnapshot(
                id = portfolio.id,
                title = portfolio.title,
                version = portfolio.version,
                url = portfolio.url,
                filePath = snapshotFilePath,
                memo = portfolio.memo
            )
            val snapshotJson = Json.encodeToString(snapshotObj)
            repository.updateApplication(application.copy(
                connectedPortfolioId = portfolio.id,
                portfolioSnapshot = snapshotJson,
                updatedAt = Date()
            ))
        }
    }

    fun attachPdf(applicationId: Long, uri: android.net.Uri, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = java.io.File(context.filesDir, "submitted_pdf_${applicationId}_${System.currentTimeMillis()}.pdf")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                val app = repository.getApplicationById(applicationId)
                if (app != null) {
                    repository.updateApplication(app.copy(
                        attachedPdfPath = file.absolutePath,
                        attachedPdfName = fileName,
                        updatedAt = Date()
                    ))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
