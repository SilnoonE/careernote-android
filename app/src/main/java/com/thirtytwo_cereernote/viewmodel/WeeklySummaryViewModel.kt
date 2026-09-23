package com.thirtytwo_cereernote.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thirtytwo_cereernote.data.model.Application
import com.thirtytwo_cereernote.data.model.Interview
import com.thirtytwo_cereernote.data.model.PracticeSession
import com.thirtytwo_cereernote.data.repository.ApplicationRepository
import com.thirtytwo_cereernote.data.repository.CareerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class WeeklySummaryData(
    val weekOffset: Int = 0, // 0 = 이번 주, -1 = 지난 주, +1 = 다음 주
    val weekStartDate: Date = Date(),
    val weekEndDate: Date = Date(),
    val weekLabel: String = "",
    val submittedAppsCount: Int = 0,
    val completedInterviewsCount: Int = 0,
    val completedReviewsCount: Int = 0,
    val completedPracticesCount: Int = 0,
    val upcomingDeadlinesCount: Int = 0,
    val upcomingInterviewsCount: Int = 0,
    val submittedApps: List<Application> = emptyList(),
    val completedInterviews: List<Interview> = emptyList(),
    val completedReviews: List<Interview> = emptyList(),
    val upcomingDeadlines: List<Application> = emptyList(),
    val upcomingInterviews: List<Interview> = emptyList()
)

@HiltViewModel
class WeeklySummaryViewModel @Inject constructor(
    private val applicationRepository: ApplicationRepository,
    private val careerRepository: CareerRepository,
    private val timeProvider: TimeProvider = DefaultTimeProvider()
) : ViewModel() {

    val weekOffset = MutableStateFlow(0)

    fun setWeekOffset(offset: Int) {
        weekOffset.value = offset
    }

    fun navigateWeek(delta: Int) {
        weekOffset.value += delta
    }

    val uiState: StateFlow<WeeklySummaryData> = combine(
        applicationRepository.allApplications,
        applicationRepository.allInterviews,
        careerRepository.getAllPracticeSessions(),
        weekOffset
    ) { apps, interviews, practiceSessions, offset ->
        val now = timeProvider.now()
        val todayLocalDate = Instant.ofEpochMilli(now.time).atZone(ZoneId.systemDefault()).toLocalDate()

        // Calculate Monday 00:00:00 for current week + offset
        val baseMonday = todayLocalDate.with(DayOfWeek.MONDAY).plusWeeks(offset.toLong())
        val nextMonday = baseMonday.plusWeeks(1)

        val zoneId = ZoneId.systemDefault()
        val weekStartInstant = baseMonday.atStartOfDay(zoneId).toInstant()
        val weekEndInstant = nextMonday.atStartOfDay(zoneId).toInstant()

        val weekStartDate = Date.from(weekStartInstant)
        val weekEndDate = Date.from(weekEndInstant)

        val wf = WeekFields.of(Locale.KOREA)
        val weekOfMonth = baseMonday.get(wf.weekOfMonth())
        val weekLabel = "${baseMonday.year}년 ${baseMonday.monthValue}월 ${weekOfMonth}주차"

        // 1. Submitted Applications in this week
        val submittedApps = apps.filter { app ->
            val sub = app.submittedDate ?: return@filter false
            !sub.before(weekStartDate) && sub.before(weekEndDate)
        }

        // 2. Completed Interviews in this week
        val appMap = apps.associateBy { it.id }
        val completedInterviews = interviews.filter { interview ->
            val app = appMap[interview.applicationId]
            if (app?.currentStatus == com.thirtytwo_cereernote.data.model.ApplicationStatus.CANCELLED) return@filter false
            val date = interview.interviewDate
            interview.isCompleted && !date.before(weekStartDate) && date.before(weekEndDate)
        }

        // 3. Completed Reviews in this week
        val completedReviews = interviews.filter { interview ->
            val app = appMap[interview.applicationId]
            if (app?.currentStatus == com.thirtytwo_cereernote.data.model.ApplicationStatus.CANCELLED) return@filter false
            val reviewDate = interview.reviewCompletedAt ?: return@filter false
            !reviewDate.before(weekStartDate) && reviewDate.before(weekEndDate)
        }

        // 4. Completed Practice Sessions in this week
        val completedPractices = practiceSessions.filter { session ->
            val compDate = session.completedAt ?: return@filter false
            session.isCompleted && !compDate.before(weekStartDate) && compDate.before(weekEndDate)
        }

        // 5. Next Week's Deadlines & Interviews
        val nextWeekStartInstant = nextMonday.atStartOfDay(zoneId).toInstant()
        val nextWeekEndInstant = nextMonday.plusWeeks(1).atStartOfDay(zoneId).toInstant()
        val nextWeekStartDate = Date.from(nextWeekStartInstant)
        val nextWeekEndDate = Date.from(nextWeekEndInstant)

        val upcomingDeadlines = apps.filter { app ->
            app.currentStatus != com.thirtytwo_cereernote.data.model.ApplicationStatus.CANCELLED &&
            app.submittedDate == null &&
            app.deadlineDate != null &&
            !app.deadlineDate.before(nextWeekStartDate) &&
            app.deadlineDate.before(nextWeekEndDate)
        }

        val upcomingInterviews = interviews.filter { interview ->
            val app = appMap[interview.applicationId]
            if (app?.currentStatus == com.thirtytwo_cereernote.data.model.ApplicationStatus.CANCELLED) return@filter false
            !interview.isCompleted &&
            !interview.interviewDate.before(nextWeekStartDate) &&
            interview.interviewDate.before(nextWeekEndDate)
        }

        WeeklySummaryData(
            weekOffset = offset,
            weekStartDate = weekStartDate,
            weekEndDate = weekEndDate,
            weekLabel = weekLabel,
            submittedAppsCount = submittedApps.size,
            completedInterviewsCount = completedInterviews.size,
            completedReviewsCount = completedReviews.size,
            completedPracticesCount = completedPractices.size,
            upcomingDeadlinesCount = upcomingDeadlines.size,
            upcomingInterviewsCount = upcomingInterviews.size,
            submittedApps = submittedApps,
            completedInterviews = completedInterviews,
            completedReviews = completedReviews,
            upcomingDeadlines = upcomingDeadlines,
            upcomingInterviews = upcomingInterviews
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeeklySummaryData())
}
