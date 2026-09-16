package com.thirtytwo_cereernote.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thirtytwo_cereernote.data.model.ApplicationStatus
import com.thirtytwo_cereernote.data.model.CompanySize
import com.thirtytwo_cereernote.data.repository.ApplicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ReportUiState(
    val totalApps: Int = 0,
    val totalCompanies: Int = 0,
    val actualAppliedCount: Int = 0,
    val docPassRate: Float? = null,
    val docPassedCount: Int = 0,
    val docFinishedCount: Int = 0,
    val interviewPassRate: Float? = null,
    val interviewPassedCount: Int = 0,
    val interviewFinishedCount: Int = 0,
    val finalPassRate: Float? = null,
    val finalPassedCount: Int = 0,
    val insights: List<String> = emptyList(),
    val statusBreakdown: Map<ApplicationStatus, Int> = emptyMap(),
    val favoriteCount: Int = 0
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val repository: ApplicationRepository
) : ViewModel() {

    val uiState: StateFlow<ReportUiState> = combine(
        repository.allApplications,
        repository.getAllStatusHistories()
    ) { apps, histories ->
        if (apps.isEmpty()) return@combine ReportUiState()

        val uniqueCompanies = apps.distinctBy { it.companyName.trim().lowercase() }.size
        
        // Map apps to their histories
        val appsWithHistories = apps.map { app ->
            app to histories.filter { it.applicationId == app.id }
        }

        val actualAppliedApps = apps.filter { it.currentStatus.isActualApplied() || it.submittedDate != null }
        val actualAppliedCount = actualAppliedApps.size

        // History-based Pass Detection
        val docPassedCount = appsWithHistories.count { (app, h) ->
            app.currentStatus.isPassedDocument() || h.any { it.status.isPassedDocument() }
        }
        val docFinishedCount = appsWithHistories.count { (app, h) ->
            app.currentStatus.isPassedDocument() || app.currentStatus.isFailedDocument() ||
            h.any { it.status.isPassedDocument() || it.status.isFailedDocument() }
        }

        val interviewPassedCount = appsWithHistories.count { (app, h) ->
            app.currentStatus.isPassedInterview() || h.any { it.status.isPassedInterview() }
        }
        val interviewFinishedCount = appsWithHistories.count { (app, h) ->
            app.currentStatus.isPassedInterview() || app.currentStatus.isFailedInterview() ||
            h.any { it.status.isPassedInterview() || it.status.isFailedInterview() }
        }

        val finalPassedCount = appsWithHistories.count { (app, h) ->
            app.currentStatus.isFinalPassed() || h.any { it.status.isFinalPassed() }
        }

        val docPassRate = if (docFinishedCount > 0) docPassedCount.toFloat() / docFinishedCount * 100 else null
        val interviewPassRate = if (interviewFinishedCount > 0) interviewPassedCount.toFloat() / interviewFinishedCount * 100 else null
        val finalPassRate = if (actualAppliedCount > 0) finalPassedCount.toFloat() / actualAppliedCount * 100 else null
        
        val insights = mutableListOf<String>()
        insights.add("현재까지 등록된 지원 기록은 총 ${apps.size}건이며, 지원한 고유 기업 수는 ${uniqueCompanies}개입니다.")
        
        val topChannel = apps.filter { it.channel.isNotBlank() }
            .groupBy { it.channel }
            .maxByOrNull { it.value.size }?.key
        if (topChannel != null) insights.add("가장 활발하게 이용 중인 지원 경로는 '${topChannel}'입니다.")

        if (actualAppliedCount >= 3) {
            docPassRate?.let {
                if (it > 65f) insights.add("서류 합격률이 ${"%.1f%%".format(it)}로 매우 높습니다!")
                else if (it < 20f && docFinishedCount >= 5) insights.add("서류 합격률을 높이기 위해 직무 키워드 점검을 권장합니다.")
            }
        }

        ReportUiState(
            totalApps = apps.size,
            totalCompanies = uniqueCompanies,
            actualAppliedCount = actualAppliedCount,
            docPassRate = docPassRate,
            docPassedCount = docPassedCount,
            docFinishedCount = docFinishedCount,
            interviewPassRate = interviewPassRate,
            interviewPassedCount = interviewPassedCount,
            interviewFinishedCount = interviewFinishedCount,
            finalPassRate = finalPassRate,
            finalPassedCount = finalPassedCount,
            insights = insights,
            statusBreakdown = apps.groupBy { it.currentStatus }.mapValues { it.value.size },
            favoriteCount = apps.count { it.isFavorite }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportUiState())
}
