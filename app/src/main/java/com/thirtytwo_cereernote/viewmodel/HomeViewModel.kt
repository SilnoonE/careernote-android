package com.thirtytwo_cereernote.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thirtytwo_cereernote.R
import com.thirtytwo_cereernote.data.model.*
import com.thirtytwo_cereernote.data.repository.ApplicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import javax.inject.Inject

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

interface TimeProvider {
    fun now(): Date
    fun today(): LocalDate = Instant.ofEpochMilli(now().time).atZone(ZoneId.systemDefault()).toLocalDate()
}

class DefaultTimeProvider @Inject constructor() : TimeProvider {
    override fun now(): Date = Date()
}

@Module
@InstallIn(ViewModelComponent::class)
abstract class TimeModule {
    @Binds
    abstract fun bindTimeProvider(impl: DefaultTimeProvider): TimeProvider
}

enum class DeadlineCategory {
    TODAY, TOMORROW, UPCOMING, OVERDUE
}

enum class DocState {
    NOT_REQUIRED, NOT_CONNECTED, CONNECTED, SUBMITTED
}

data class TodayInterviewItem(
    val interview: Interview,
    val application: Application,
    val hasPrepMemo: Boolean,
    val prepMemo: String
)

data class UnsubmittedDeadlineItem(
    val application: Application,
    val category: DeadlineCategory,
    val formattedDeadline: String,
    val dDayText: String
)

data class PendingReviewItem(
    val interview: Interview,
    val application: Application,
    val needsCompletionConfirm: Boolean,
    val needsReview: Boolean
)

data class PreSubmissionDocItem(
    val application: Application,
    val requiredTypes: List<String>,
    val resumeState: DocState,
    val coverLetterState: DocState,
    val portfolioState: DocState
)

data class HomeUiState(
    val todayInterviews: List<TodayInterviewItem> = emptyList(),
    val upcomingInterviews: List<TodayInterviewItem> = emptyList(),
    val unsubmittedDeadlines: List<UnsubmittedDeadlineItem> = emptyList(),
    val pendingReviews: List<PendingReviewItem> = emptyList(),
    val docCheckItems: List<PreSubmissionDocItem> = emptyList(),
    val totalApplications: Int = 0,
    val ongoingApplications: Int = 0,
    val documentPassed: Int = 0,
    val finalPassed: Int = 0,
    val randomTip: CareerTip? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ApplicationRepository,
    @ApplicationContext private val context: Context,
    private val timeProvider: TimeProvider = DefaultTimeProvider()
) : ViewModel() {

    private val _randomTip = MutableStateFlow<CareerTip?>(null)
    private val _manualRefreshTrigger = MutableStateFlow(System.currentTimeMillis())

    private val timeTickerFlow = flow {
        while (true) {
            emit(timeProvider.now())
            delay(30_000L)
        }
    }

    init {
        loadRandomTip()
    }

    fun refresh() {
        _manualRefreshTrigger.value = System.currentTimeMillis()
    }

    private val dataFlow = combine(
        repository.allApplications,
        repository.allInterviews,
        repository.getAllStatusHistories()
    ) { apps, interviews, histories ->
        Triple(apps, interviews, histories)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        dataFlow,
        _randomTip,
        timeTickerFlow,
        _manualRefreshTrigger
    ) { (apps, interviews, histories), tip, currentTime, _ ->
        val today = Instant.ofEpochMilli(currentTime.time).atZone(ZoneId.systemDefault()).toLocalDate()
        val appMap = apps.associateBy { it.id }

        // Filter active interviews (application not cancelled)
        val activeInterviews = interviews.mapNotNull { interview ->
            val app = appMap[interview.applicationId] ?: return@mapNotNull null
            if (app.currentStatus == ApplicationStatus.CANCELLED) return@mapNotNull null
            interview to app
        }.sortedBy { it.first.interviewDate }

        // 1. Today's Interviews (Scheduled for TODAY & Uncompleted)
        val todayInterviews = activeInterviews.filter { (interview, _) ->
            val interviewLocalDate = Instant.ofEpochMilli(interview.interviewDate.time).atZone(ZoneId.systemDefault()).toLocalDate()
            interviewLocalDate == today && !interview.isCompleted
        }.map { (interview, app) ->
            TodayInterviewItem(
                interview = interview,
                application = app,
                hasPrepMemo = interview.preparations.isNotBlank() || interview.memo.isNotBlank(),
                prepMemo = interview.preparations.ifBlank { interview.memo }
            )
        }

        // 2. Upcoming Interviews (Scheduled for FUTURE & Uncompleted)
        val upcomingInterviews = activeInterviews.filter { (interview, _) ->
            val interviewLocalDate = Instant.ofEpochMilli(interview.interviewDate.time).atZone(ZoneId.systemDefault()).toLocalDate()
            interviewLocalDate.isAfter(today) && !interview.isCompleted
        }.map { (interview, app) ->
            TodayInterviewItem(
                interview = interview,
                application = app,
                hasPrepMemo = interview.preparations.isNotBlank() || interview.memo.isNotBlank(),
                prepMemo = interview.preparations.ifBlank { interview.memo }
            )
        }

        // 3. Pending Reviews (Past interview date or marked completed but missing review content)
        val reviewItems = activeInterviews.filter { (interview, _) ->
            interview.interviewDate.before(currentTime) || interview.isCompleted
        }.mapNotNull { (interview, app) ->
            val needsConfirm = !interview.isCompleted
            // A review is completed if ANY of strengths, weaknesses, review, or preparations has content
            val isReviewFilled = interview.strengths.isNotBlank() || interview.weaknesses.isNotBlank() || interview.review.isNotBlank() || interview.preparations.isNotBlank()
            val needsReview = interview.isCompleted && !isReviewFilled

            if (!needsConfirm && !needsReview) return@mapNotNull null
            PendingReviewItem(
                interview = interview,
                application = app,
                needsCompletionConfirm = needsConfirm,
                needsReview = needsReview
            )
        }

        // 4. Unsubmitted Deadlines
        val unsubmitted = apps.filter { app ->
            app.currentStatus != ApplicationStatus.CANCELLED &&
            app.submittedDate == null &&
            (app.currentStatus == ApplicationStatus.INTERESTED || app.currentStatus == ApplicationStatus.APPLY_PLANNED) &&
            app.deadlineDate != null
        }.map { app ->
            val deadlineDate = app.deadlineDate!!
            val deadlineLocalDate = Instant.ofEpochMilli(deadlineDate.time)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            val diffDays = ChronoUnit.DAYS.between(today, deadlineLocalDate)
            val category = when {
                diffDays < 0 -> DeadlineCategory.OVERDUE
                diffDays == 0L -> DeadlineCategory.TODAY
                diffDays == 1L -> DeadlineCategory.TOMORROW
                else -> DeadlineCategory.UPCOMING
            }

            val dDayText = when (category) {
                DeadlineCategory.OVERDUE -> "마감 경과 (미제출)"
                DeadlineCategory.TODAY -> "오늘 마감"
                DeadlineCategory.TOMORROW -> "내일 마감"
                DeadlineCategory.UPCOMING -> "D-$diffDays"
            }

            val cal = java.util.Calendar.getInstance().apply { time = deadlineDate }
            val hasTime = cal.get(java.util.Calendar.HOUR_OF_DAY) != 0 || cal.get(java.util.Calendar.MINUTE) != 0
            val formatted = if (hasTime) {
                java.text.SimpleDateFormat("yyyy.MM.dd HH:mm", java.util.Locale.getDefault()).format(deadlineDate)
            } else {
                java.text.SimpleDateFormat("yyyy.MM.dd", java.util.Locale.getDefault()).format(deadlineDate)
            }

            UnsubmittedDeadlineItem(app, category, formatted, dDayText)
        }.sortedWith(compareBy({ it.category.ordinal }, { it.application.deadlineDate }))

        // 5. Pre-submission Document Check Items
        val docCheckList = apps.filter { app ->
            app.currentStatus != ApplicationStatus.CANCELLED &&
            app.submittedDate == null &&
            (app.currentStatus == ApplicationStatus.INTERESTED || app.currentStatus == ApplicationStatus.APPLY_PLANNED)
        }.take(5).map { app ->
            val requiredList = app.requiredDocTypes.split(",").map { it.trim() }.filter { it.isNotBlank() }

            fun getDocState(type: String, connectedId: Long?, snapshot: String?): DocState {
                if (!requiredList.contains(type)) return DocState.NOT_REQUIRED
                return when {
                    snapshot != null -> DocState.CONNECTED
                    connectedId != null -> DocState.CONNECTED
                    else -> DocState.NOT_CONNECTED
                }
            }

            PreSubmissionDocItem(
                application = app,
                requiredTypes = requiredList,
                resumeState = getDocState("RESUME", app.connectedResumeId, app.resumeSnapshot),
                coverLetterState = getDocState("COVER_LETTER", app.connectedCoverLetterId, app.coverLetterSnapshot),
                portfolioState = getDocState("PORTFOLIO", app.connectedPortfolioId, app.portfolioSnapshot)
            )
        }

        // Overall stats
        val appsWithHistories = apps.map { app -> app to histories.filter { it.applicationId == app.id } }

        HomeUiState(
            todayInterviews = todayInterviews,
            upcomingInterviews = upcomingInterviews,
            unsubmittedDeadlines = unsubmitted,
            pendingReviews = reviewItems,
            docCheckItems = docCheckList,
            totalApplications = apps.size,
            ongoingApplications = apps.count { !it.currentStatus.isFinished() && it.currentStatus != ApplicationStatus.INTERESTED && it.currentStatus != ApplicationStatus.APPLY_PLANNED },
            documentPassed = appsWithHistories.count { (app, h) -> app.currentStatus.isPassedDocument() || h.any { it.status.isPassedDocument() } },
            finalPassed = appsWithHistories.count { (app, h) -> app.currentStatus.isFinalPassed() || h.any { it.status.isFinalPassed() } },
            randomTip = tip
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    suspend fun updateRequiredDocTypes(applicationId: Long, types: String): Boolean {
        val app = repository.getApplicationById(applicationId) ?: return false
        repository.updateApplication(app.copy(requiredDocTypes = types, updatedAt = Date()))
        return true
    }

    suspend fun confirmInterviewCompleted(interview: Interview): Boolean {
        repository.updateInterview(interview.copy(isCompleted = true))
        return true
    }

    suspend fun saveInterviewReview(interview: Interview, strengths: String, weaknesses: String, betterAnswer: String, preparations: String): Boolean {
        repository.updateInterview(
            interview.copy(
                strengths = strengths,
                weaknesses = weaknesses,
                review = betterAnswer,
                preparations = preparations,
                isCompleted = true
            )
        )
        return true
    }

    suspend fun savePreparationMemo(interview: Interview, memo: String): Boolean {
        repository.updateInterview(
            interview.copy(preparations = memo)
        )
        return true
    }

    private fun loadRandomTip() {
        viewModelScope.launch {
            try {
                val appLocales = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()
                val locale = if (appLocales.isEmpty) {
                    context.resources.configuration.locales[0].language
                } else {
                    appLocales.get(0)?.language ?: "ko"
                }

                val resId = when (locale) {
                    "ko" -> R.raw.career_tips_ko
                    "en" -> R.raw.career_tips_en
                    "ja" -> R.raw.career_tips_ja
                    else -> R.raw.career_tips_ko
                }

                val jsonString = context.resources.openRawResource(resId).bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(jsonString)
                if (jsonArray.length() > 0) {
                    val randomIndex = (0 until jsonArray.length()).random()
                    val obj = jsonArray.getJSONObject(randomIndex)
                    val categoryStr = obj.optString("category")
                    val category = try {
                        TipCategory.valueOf(categoryStr)
                    } catch (e: Exception) {
                        TipCategory.MARKET
                    }

                    _randomTip.value = CareerTip(
                        id = obj.optString("id", "0"),
                        category = category,
                        title = obj.optString("title", ""),
                        subtitle = if (obj.has("subtitle")) obj.optString("subtitle") else null,
                        body = obj.optString("body", ""),
                        badge = if (obj.has("badge")) obj.optString("badge") else null,
                        source = if (obj.has("source")) obj.optString("source") else null,
                        referenceDate = if (obj.has("referenceDate")) obj.optString("referenceDate") else null,
                        order = obj.optInt("order", 0)
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
