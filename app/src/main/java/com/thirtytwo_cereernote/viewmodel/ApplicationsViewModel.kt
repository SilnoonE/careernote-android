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
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed class DraftState {
    object Loading : DraftState()
    data class Ready(val drafts: Map<String, Draft>) : DraftState()
    data class Error(val throwable: Throwable) : DraftState()
}

@HiltViewModel
class ApplicationsViewModel @Inject constructor(
    private val repository: ApplicationRepository,
    private val careerRepository: CareerRepository,
    private val preferenceRepository: PreferenceRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val notificationHelper = NotificationHelper(context)

    val draftsState: StateFlow<DraftState> = preferenceRepository.draftsJson.map { json ->
        try {
            val map = Json.decodeFromString<Map<String, Draft>>(json)
            DraftState.Ready(map)
        } catch (e: Exception) {
            DraftState.Ready(emptyMap())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DraftState.Loading)

    val drafts = preferenceRepository.draftsJson.map { json ->
        try {
            Json.decodeFromString<Map<String, Draft>>(json)
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

    suspend fun clearApplicationDraft() {
        preferenceRepository.updateDraft("application", 0L) { null }
    }

    val allCoverLetters = careerRepository.getAllCoverLetters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allResumes = careerRepository.getAllResumes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPortfolios = careerRepository.getAllPortfolios()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchQuery = MutableStateFlow("")
    val filterGroup = MutableStateFlow(ApplicationFilterGroup.ALL)
    val sortBy = MutableStateFlow("appliedDate")

    val isInitialEmpty: StateFlow<Boolean> = repository.allApplications
        .map { it.isEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val applications: StateFlow<List<Application>> = combine(
        repository.allApplications,
        searchQuery,
        filterGroup,
        sortBy
    ) { apps, query, group, sort ->
        var result = apps

        if (query.isNotBlank()) {
            result = result.filter {
                it.companyName.contains(query, ignoreCase = true) ||
                it.jobTitle.contains(query, ignoreCase = true) ||
                it.memo.contains(query, ignoreCase = true)
            }
        }

        result = when (group) {
            ApplicationFilterGroup.ALL -> result
            ApplicationFilterGroup.FAVORITE -> result.filter { it.isFavorite }
            ApplicationFilterGroup.ONGOING -> result.filter {
                it.currentStatus in setOf(
                    ApplicationStatus.APPLY_COMPLETED, ApplicationStatus.DOCUMENT_REVIEW,
                    ApplicationStatus.DOCUMENT_PASSED, ApplicationStatus.INTERVIEW_PLANNED,
                    ApplicationStatus.INTERVIEW_ONGOING, ApplicationStatus.INTERVIEW_PASSED
                )
            }
            ApplicationFilterGroup.PASSED -> result.filter { it.currentStatus.isFinalPassed() }
            ApplicationFilterGroup.FAILED -> result.filter { it.currentStatus.isFailedDocument() || it.currentStatus.isFailedInterview() }
            ApplicationFilterGroup.PLANNED -> result.filter {
                it.currentStatus in setOf(ApplicationStatus.INTERESTED, ApplicationStatus.APPLY_PLANNED, ApplicationStatus.CANCELLED)
            }
        }

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

    suspend fun addApplication(application: Application): Long {
        val id = repository.insertApplication(application)
        if (id > 0) {
            clearApplicationDraft()
            application.deadlineDate?.let {
                val triggerTime = it.time - (24 * 60 * 60 * 1000)
                try {
                    notificationHelper.scheduleNotification(
                        id,
                        NotificationHelper.TYPE_APPLICATION,
                        "지원 마감 알림: ${application.companyName}",
                        "${application.jobTitle} 공고 마감이 하루 남았습니다.",
                        triggerTime
                    )
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    e.printStackTrace()
                }
            }
        }
        return id
    }

    suspend fun deleteApplication(application: Application) {
        val filesToDelete = mutableListOf<String?>()
        filesToDelete.add(application.attachedPdfPath)

        val interviewIds = try {
            repository.getInterviewsByApplicationId(application.id).first().map { it.id }
        } catch (_: Exception) {
            emptyList()
        }

        try {
            application.resumeSnapshot?.let { json ->
                val obj = Json.decodeFromString<ResumeSnapshot>(json)
                if (obj.filePath.contains("resume_snapshot_")) filesToDelete.add(obj.filePath)
            }
            application.portfolioSnapshot?.let { json ->
                val obj = Json.decodeFromString<PortfolioSnapshot>(json)
                if (obj.filePath.contains("portfolio_snapshot_")) filesToDelete.add(obj.filePath)
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            e.printStackTrace()
        }

        repository.deleteApplication(application)

        try {
            notificationHelper.cancelNotification(application.id, NotificationHelper.TYPE_APPLICATION)
            interviewIds.forEach { id ->
                notificationHelper.cancelNotification(id, NotificationHelper.TYPE_INTERVIEW)
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            e.printStackTrace()
        }

        filesToDelete.filterNotNull().forEach { path ->
            try {
                val file = java.io.File(path)
                if (file.exists() && file.parentFile?.absolutePath == context.filesDir.absolutePath) {
                    file.delete()
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                e.printStackTrace()
            }
        }
    }

    suspend fun updateStatus(applicationId: Long, newStatus: ApplicationStatus, memo: String = "") {
        repository.updateStatus(applicationId, newStatus, memo)

        if (newStatus == ApplicationStatus.CANCELLED ||
            newStatus == ApplicationStatus.DOCUMENT_FAILED ||
            newStatus == ApplicationStatus.INTERVIEW_FAILED) {
            try {
                notificationHelper.cancelNotification(applicationId, NotificationHelper.TYPE_APPLICATION)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                e.printStackTrace()
            }
        }
    }

    fun getStatusHistories(applicationId: Long): Flow<List<ApplicationStatusHistory>> {
        return repository.getStatusHistories(applicationId)
    }

    fun getInterviews(applicationId: Long): Flow<List<Interview>> {
        return repository.getInterviewsByApplicationId(applicationId)
    }

    suspend fun addInterview(interview: Interview): Long {
        val id = repository.insertInterview(interview)
        if (id > 0) {
            val triggerTime = interview.interviewDate.time - (2 * 60 * 60 * 1000)
            try {
                notificationHelper.scheduleNotification(
                    id,
                    NotificationHelper.TYPE_INTERVIEW,
                    "면접 일정 알림: ${interview.stage}",
                    "오늘 ${interview.stage} 일정이 있습니다. 장소: ${interview.location}",
                    triggerTime,
                    extraId = interview.applicationId
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                e.printStackTrace()
            }
        }
        return id
    }

    suspend fun removeInterview(interview: Interview) {
        repository.deleteInterview(interview)
        try {
            notificationHelper.cancelNotification(interview.id, NotificationHelper.TYPE_INTERVIEW)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            e.printStackTrace()
        }
    }

    suspend fun updateApplicationInfo(application: Application) {
        repository.updateApplication(application)
        try {
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
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            e.printStackTrace()
        }
    }

    suspend fun linkCoverLetter(application: Application, coverLetter: CoverLetter) {
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
        val latestApp = repository.getApplicationById(application.id) ?: application
        repository.updateApplication(latestApp.copy(
            connectedCoverLetterId = coverLetter.id,
            coverLetterSnapshot = snapshotJson,
            updatedAt = Date()
        ))
    }

    suspend fun linkResume(application: Application, resume: Resume) {
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
                if (e is kotlinx.coroutines.CancellationException) throw e
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
        val latestApp = repository.getApplicationById(application.id) ?: application
        repository.updateApplication(latestApp.copy(
            connectedResumeId = resume.id,
            resumeSnapshot = snapshotJson,
            updatedAt = Date()
        ))
    }

    suspend fun linkPortfolio(application: Application, portfolio: Portfolio) {
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
                if (e is kotlinx.coroutines.CancellationException) throw e
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
        val latestApp = repository.getApplicationById(application.id) ?: application
        repository.updateApplication(latestApp.copy(
            connectedPortfolioId = portfolio.id,
            portfolioSnapshot = snapshotJson,
            updatedAt = Date()
        ))
    }

    suspend fun attachPdf(applicationId: Long, uri: android.net.Uri, fileName: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val app = repository.getApplicationById(applicationId)
                    ?: return@withContext Result.failure(java.io.IOException("대상 지원서를 찾을 수 없습니다."))

                val tempFile = java.io.File(context.cacheDir, "temp_pdf_${applicationId}_${System.currentTimeMillis()}.pdf")
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext Result.failure(java.io.IOException("파일을 읽을 수 없습니다."))

                inputStream.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }

                if (!tempFile.exists() || tempFile.length() <= 0) {
                    tempFile.delete()
                    return@withContext Result.failure(java.io.IOException("빈 파일이거나 파일 복사에 실패했습니다."))
                }

                val finalFile = java.io.File(context.filesDir, "submitted_pdf_${applicationId}_${System.currentTimeMillis()}.pdf")
                if (tempFile.renameTo(finalFile) || (tempFile.copyTo(finalFile, true).also { tempFile.delete() }).exists()) {
                    val previousPdfPath = app.attachedPdfPath
                    repository.updateApplication(app.copy(
                        attachedPdfPath = finalFile.absolutePath,
                        attachedPdfName = fileName,
                        updatedAt = Date()
                    ))

                    if (!previousPdfPath.isNull_or_blank() && previousPdfPath != finalFile.absolutePath) {
                        try {
                            val prev = java.io.File(previousPdfPath!!)
                            if (prev.exists() && prev.parentFile?.absolutePath == context.filesDir.absolutePath) {
                                prev.delete()
                            }
                        } catch (_: Exception) {}
                    }
                    Result.success(Unit)
                } else {
                    tempFile.delete()
                    Result.failure(java.io.IOException("최종 저장 위치로 파일을 이동할 수 없습니다."))
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Result.failure(e)
            }
        }
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.isBlank()
