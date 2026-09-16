package com.thirtytwo_cereernote.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thirtytwo_cereernote.R
import com.thirtytwo_cereernote.data.model.Application
import com.thirtytwo_cereernote.data.model.ApplicationStatus
import com.thirtytwo_cereernote.data.model.CareerTip
import com.thirtytwo_cereernote.data.model.TipCategory
import com.thirtytwo_cereernote.data.repository.ApplicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

data class HomeUiState(
    val totalApplications: Int = 0,
    val ongoingApplications: Int = 0,
    val documentPassed: Int = 0,
    val finalPassed: Int = 0,
    val upcomingDeadlines: List<Application> = emptyList(),
    val randomTip: CareerTip? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ApplicationRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _randomTip = MutableStateFlow<CareerTip?>(null)

    init {
        loadRandomTip()
    }

    val uiState: StateFlow<HomeUiState> = combine(
        repository.allApplications,
        repository.getAllStatusHistories(),
        _randomTip
    ) { apps, histories, tip ->
        val today = LocalDate.now()

        val upcoming = apps.filter { 
            if (it.deadlineDate == null) return@filter false
            val deadlineLocalDate = it.deadlineDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            !deadlineLocalDate.isBefore(today) && (
                it.currentStatus == ApplicationStatus.INTERESTED || 
                it.currentStatus == ApplicationStatus.APPLY_PLANNED ||
                it.currentStatus == ApplicationStatus.APPLY_COMPLETED ||
                it.currentStatus == ApplicationStatus.DOCUMENT_REVIEW ||
                it.currentStatus == ApplicationStatus.INTERVIEW_PLANNED
            )
        }.sortedBy { it.deadlineDate }

        val appsWithHistories = apps.map { app ->
            app to histories.filter { it.applicationId == app.id }
        }

        HomeUiState(
            totalApplications = apps.size,
            ongoingApplications = apps.count { !it.currentStatus.isFinished() && it.currentStatus != ApplicationStatus.INTERESTED && it.currentStatus != ApplicationStatus.APPLY_PLANNED },
            documentPassed = appsWithHistories.count { (app, h) -> app.currentStatus.isPassedDocument() || h.any { it.status.isPassedDocument() } },
            finalPassed = appsWithHistories.count { (app, h) -> app.currentStatus.isFinalPassed() || h.any { it.status.isFinalPassed() } },
            upcomingDeadlines = upcoming.take(5),
            randomTip = tip
        )
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

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
                    _randomTip.value = CareerTip(
                        id = obj.getString("id"),
                        category = TipCategory.valueOf(obj.getString("category")),
                        title = obj.getString("title"),
                        subtitle = if (obj.has("subtitle")) obj.getString("subtitle") else null,
                        body = obj.getString("body"),
                        badge = if (obj.has("badge")) obj.getString("badge") else null,
                        source = if (obj.has("source")) obj.getString("source") else null,
                        referenceDate = if (obj.has("referenceDate")) obj.getString("referenceDate") else null,
                        order = if (obj.has("order")) obj.getInt("order") else 0
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
