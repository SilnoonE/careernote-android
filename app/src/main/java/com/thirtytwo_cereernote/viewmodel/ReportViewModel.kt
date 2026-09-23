package com.thirtytwo_cereernote.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thirtytwo_cereernote.data.model.ApplicationStatus
import com.thirtytwo_cereernote.data.repository.ApplicationRepository
import com.thirtytwo_cereernote.util.ApplicationStats
import com.thirtytwo_cereernote.util.ApplicationStatsCalculator
import com.thirtytwo_cereernote.util.StatFilter
import com.thirtytwo_cereernote.util.StatPeriod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Date
import javax.inject.Inject

data class ReportUiState(
    val stats: ApplicationStats = ApplicationStats(),
    val totalCompanies: Int = 0,
    val favoriteCount: Int = 0,
    val statusBreakdown: Map<ApplicationStatus, Int> = emptyMap(),
    val currentFilter: StatFilter = StatFilter()
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val repository: ApplicationRepository
) : ViewModel() {

    val periodFilter = MutableStateFlow(StatPeriod.ALL)
    val customStartDate = MutableStateFlow<Date?>(null)
    val customEndDate = MutableStateFlow<Date?>(null)
    val jobFilter = MutableStateFlow<String?>(null)
    val docVersionFilter = MutableStateFlow<String?>(null)

    fun setPeriod(period: StatPeriod) {
        periodFilter.value = period
    }

    fun setCustomRange(start: Date?, end: Date?) {
        customStartDate.value = start
        customEndDate.value = end
        periodFilter.value = StatPeriod.CUSTOM
    }

    fun setJobFilter(job: String?) {
        jobFilter.value = job
    }

    fun setDocVersionFilter(version: String?) {
        docVersionFilter.value = version
    }

    private val dataFlow = combine(
        repository.allApplications,
        repository.getAllStatusHistories(),
        repository.allInterviews
    ) { apps, histories, interviews ->
        Triple(apps, histories, interviews)
    }

    private val filterFlow = combine(
        periodFilter,
        customStartDate,
        customEndDate,
        jobFilter,
        docVersionFilter
    ) { period, start, end, job, version ->
        StatFilter(
            period = period,
            customStartDate = start,
            customEndDate = end,
            jobFilter = job,
            docVersionFilter = version
        )
    }

    val uiState: StateFlow<ReportUiState> = combine(
        dataFlow,
        filterFlow
    ) { (apps, histories, interviews), filter ->
        if (apps.isEmpty()) return@combine ReportUiState()

        val stats = ApplicationStatsCalculator.calculate(
            applications = apps,
            statusHistories = histories,
            interviews = interviews,
            filter = filter
        )

        val uniqueCompanies = apps.distinctBy { it.companyName.trim().lowercase() }.size

        ReportUiState(
            stats = stats,
            totalCompanies = uniqueCompanies,
            favoriteCount = apps.count { it.isFavorite },
            statusBreakdown = apps.groupBy { it.currentStatus }.mapValues { it.value.size },
            currentFilter = filter
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportUiState())
}
