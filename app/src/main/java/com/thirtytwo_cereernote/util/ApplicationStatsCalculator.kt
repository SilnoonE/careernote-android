package com.thirtytwo_cereernote.util

import com.thirtytwo_cereernote.data.model.Application
import com.thirtytwo_cereernote.data.model.ApplicationStatus
import com.thirtytwo_cereernote.data.model.ApplicationStatusHistory
import com.thirtytwo_cereernote.data.model.Interview
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

enum class StatPeriod(val displayName: String) {
    ALL("전체"),
    LAST_30_DAYS("최근 30일"),
    LAST_90_DAYS("최근 90일"),
    CUSTOM("직접 지정")
}

data class StatFilter(
    val period: StatPeriod = StatPeriod.ALL,
    val customStartDate: Date? = null,
    val customEndDate: Date? = null,
    val jobFilter: String? = null, // null = 전체, "미분류" = 직무 미기재, 또는 특정 직무명/카테고리
    val docVersionFilter: String? = null // null = 전체, 또는 특정 서류 라벨
)

data class ApplicationStats(
    val totalRegistered: Int = 0,
    val actualAppliedCount: Int = 0,

    // Doc Stage
    val docPassedCount: Int = 0,
    val docFailedCount: Int = 0,
    val docPendingCount: Int = 0,
    val docFinishedCount: Int = 0,
    val docPassRate: Float? = null,

    // Interview Stage
    val interviewPassedCount: Int = 0,
    val interviewFailedCount: Int = 0,
    val interviewPendingCount: Int = 0,
    val interviewFinishedCount: Int = 0,
    val interviewPassRate: Float? = null,

    // Final Stage
    val finalPassedCount: Int = 0,
    val finalFailedCount: Int = 0,
    val finalPendingCount: Int = 0,
    val finalFinishedCount: Int = 0,
    val finalPassRateVsSubmitted: Float? = null,
    val finalPassRateVsConfirmed: Float? = null,

    val smallSampleNotice: String? = null,
    val availableJobs: List<String> = emptyList(),
    val availableDocVersions: List<String> = emptyList(),
    val insights: List<String> = emptyList()
)

object ApplicationStatsCalculator {

    fun calculate(
        applications: List<Application>,
        statusHistories: List<ApplicationStatusHistory>,
        interviews: List<Interview> = emptyList(),
        filter: StatFilter = StatFilter()
    ): ApplicationStats {
        if (applications.isEmpty()) return ApplicationStats()

        val historiesByApp = statusHistories.groupBy { it.applicationId }
        val interviewsByApp = interviews.groupBy { it.applicationId }

        // Extract available jobs for dropdown
        val jobs = applications.map { app ->
            when {
                app.jobCategory.isNotBlank() -> app.jobCategory.trim()
                app.jobTitle.isNotBlank() -> app.jobTitle.trim()
                else -> "미분류"
            }
        }.distinct().sorted()

        // Extract available doc versions (ALL connected snapshot types)
        val docVersions = mutableSetOf<String>()
        applications.forEach { app ->
            app.resumeSnapshot?.let { extractVersionLabels(it, "이력서") }?.let { docVersions.addAll(it) }
            app.coverLetterSnapshot?.let { extractVersionLabels(it, "자소서") }?.let { docVersions.addAll(it) }
            app.portfolioSnapshot?.let { extractVersionLabels(it, "포트폴리오") }?.let { docVersions.addAll(it) }
        }
        val sortedDocVersions = docVersions.sorted()

        // Filter applications
        val today = LocalDate.now()
        val now = Date()

        val filteredApps = applications.filter { app ->
            // 1. Period Filter (using strict submission date if period is 30d/90d/custom)
            val passPeriod = when (filter.period) {
                StatPeriod.ALL -> true
                StatPeriod.LAST_30_DAYS, StatPeriod.LAST_90_DAYS, StatPeriod.CUSTOM -> {
                    val subDate = app.submittedDate ?: return@filter false
                    val subLocalDate = Instant.ofEpochMilli(subDate.time)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()

                    if (subLocalDate.isAfter(today)) return@filter false

                    when (filter.period) {
                        StatPeriod.LAST_30_DAYS -> !subLocalDate.isBefore(today.minusDays(30))
                        StatPeriod.LAST_90_DAYS -> !subLocalDate.isBefore(today.minusDays(90))
                        StatPeriod.CUSTOM -> {
                            val start = filter.customStartDate?.let { Instant.ofEpochMilli(it.time).atZone(ZoneId.systemDefault()).toLocalDate() }
                            val end = filter.customEndDate?.let { Instant.ofEpochMilli(it.time).atZone(ZoneId.systemDefault()).toLocalDate() }
                            (start == null || !subLocalDate.isBefore(start)) && (end == null || !subLocalDate.isAfter(end))
                        }
                        else -> true
                    }
                }
            }
            if (!passPeriod) return@filter false

            // 2. Job Filter
            if (filter.jobFilter != null) {
                val appJob = when {
                    app.jobCategory.isNotBlank() -> app.jobCategory.trim()
                    app.jobTitle.isNotBlank() -> app.jobTitle.trim()
                    else -> "미분류"
                }
                if (filter.jobFilter == "미분류" && appJob != "미분류") return@filter false
                if (filter.jobFilter != "미분류" && !appJob.equals(filter.jobFilter, ignoreCase = true)) return@filter false
            }

            // 3. Document Version Filter
            if (filter.docVersionFilter != null) {
                val appVersions = mutableSetOf<String>()
                app.resumeSnapshot?.let { extractVersionLabels(it, "이력서") }?.let { appVersions.addAll(it) }
                app.coverLetterSnapshot?.let { extractVersionLabels(it, "자소서") }?.let { appVersions.addAll(it) }
                app.portfolioSnapshot?.let { extractVersionLabels(it, "포트폴리오") }?.let { appVersions.addAll(it) }

                if (!appVersions.contains(filter.docVersionFilter)) return@filter false
            }

            true
        }

        if (filteredApps.isEmpty()) {
            return ApplicationStats(
                totalRegistered = 0,
                availableJobs = jobs,
                availableDocVersions = sortedDocVersions
            )
        }

        val appsWithData = filteredApps.map { app ->
            val h = historiesByApp[app.id] ?: emptyList()
            val i = interviewsByApp[app.id] ?: emptyList()
            Triple(app, h, i)
        }

        // 1. Actual Applied Count
        val actualApplied = appsWithData.filter { (app, h, _) ->
            if (app.submittedDate != null) return@filter true
            if (app.currentStatus.isActualApplied()) return@filter true
            if (app.currentStatus == ApplicationStatus.CANCELLED) {
                return@filter h.any { it.status.isActualApplied() }
            }
            false
        }
        val actualAppliedCount = actualApplied.size

        // 2. Document Stage Stats
        val docPassedIds = actualApplied.filter { (app, h, _) ->
            app.currentStatus.isPassedDocument() || h.any { it.status.isPassedDocument() } || app.currentStatus.isFinalPassed()
        }.map { it.first.id }.toSet()

        val docFailedIds = actualApplied.filter { (app, h, _) ->
            (app.currentStatus == ApplicationStatus.DOCUMENT_FAILED || h.any { it.status == ApplicationStatus.DOCUMENT_FAILED }) &&
            !docPassedIds.contains(app.id)
        }.map { it.first.id }.toSet()

        val docFinishedIds = docPassedIds + docFailedIds
        val docPassedCount = docPassedIds.size
        val docFailedCount = docFailedIds.size
        val docFinishedCount = docFinishedIds.size

        val docPendingCount = actualApplied.count { (app, _, _) ->
            !docFinishedIds.contains(app.id) &&
            app.currentStatus != ApplicationStatus.CANCELLED &&
            (app.currentStatus == ApplicationStatus.APPLY_COMPLETED || app.currentStatus == ApplicationStatus.DOCUMENT_REVIEW)
        }
        val docPassRate = if (docFinishedCount > 0) (docPassedCount.toFloat() / docFinishedCount) * 100f else null

        // 3. Interview Stage Stats
        val interviewPassedIds = actualApplied.filter { (app, h, _) ->
            app.currentStatus.isPassedInterview() || h.any { it.status.isPassedInterview() } || app.currentStatus.isFinalPassed()
        }.map { it.first.id }.toSet()

        val interviewFailedIds = actualApplied.filter { (app, h, _) ->
            (app.currentStatus == ApplicationStatus.INTERVIEW_FAILED || h.any { it.status == ApplicationStatus.INTERVIEW_FAILED }) &&
            !interviewPassedIds.contains(app.id)
        }.map { it.first.id }.toSet()

        val interviewFinishedIds = interviewPassedIds + interviewFailedIds
        val interviewPassedCount = interviewPassedIds.size
        val interviewFailedCount = interviewFailedIds.size
        val interviewFinishedCount = interviewFinishedIds.size

        // ONLY count interviews as result pending IF the interview was completed or its date has passed, and no result confirmed yet
        val interviewPendingCount = actualApplied.count { (app, _, i) ->
            if (interviewFinishedIds.contains(app.id)) return@count false
            if (app.currentStatus == ApplicationStatus.CANCELLED) return@count false

            i.any { interview ->
                interview.isCompleted || interview.reviewCompletedAt != null || interview.interviewDate.before(now)
            }
        }
        val interviewPassRate = if (interviewFinishedCount > 0) (interviewPassedCount.toFloat() / interviewFinishedCount) * 100f else null

        // 4. Final Pass Stats
        val finalPassedIds = actualApplied.filter { (app, h, _) ->
            app.currentStatus.isFinalPassed() || h.any { it.status.isFinalPassed() }
        }.map { it.first.id }.toSet()

        val finalFailedIds = actualApplied.filter { (app, h, _) ->
            (app.currentStatus == ApplicationStatus.DOCUMENT_FAILED || app.currentStatus == ApplicationStatus.INTERVIEW_FAILED) &&
            !finalPassedIds.contains(app.id)
        }.map { it.first.id }.toSet()

        val finalFinishedIds = finalPassedIds + finalFailedIds
        val finalPassedCount = finalPassedIds.size
        val finalFailedCount = finalFailedIds.size
        val finalFinishedCount = finalFinishedIds.size

        val finalPendingCount = actualApplied.count { (app, _, _) ->
            !finalFinishedIds.contains(app.id) && app.currentStatus != ApplicationStatus.CANCELLED
        }

        val finalPassRateVsSubmitted = if (actualAppliedCount > 0) (finalPassedCount.toFloat() / actualAppliedCount) * 100f else null
        val finalPassRateVsConfirmed = if (finalFinishedCount > 0) (finalPassedCount.toFloat() / finalFinishedCount) * 100f else null

        // 5. Small Sample Notice
        val minConfirmed = listOfNotNull(docFinishedCount, interviewFinishedCount).filter { it > 0 }.minOrNull() ?: 0
        val smallSampleNotice = when {
            docFinishedCount == 0 && interviewFinishedCount == 0 && actualAppliedCount > 0 -> "아직 확인된 결과가 없습니다. 결과 대기 중인 지원 건수를 확인해보세요."
            actualAppliedCount in 1..4 || (minConfirmed in 1..4) -> "결과 확인 건수가 적어 해석에 주의가 필요합니다."
            else -> null
        }

        // 6. Data-driven Insights
        val insightsList = mutableListOf<String>()
        val periodText = filter.period.displayName
        insightsList.add("${periodText} 기준 총 등록된 기록은 ${filteredApps.size}건이며, 실제 제출 완료 및 진행 중인 지원은 ${actualAppliedCount}건입니다.")

        if (docFinishedCount > 0) {
            insightsList.add("서류 결과가 확인된 ${docFinishedCount}건 중 ${docPassedCount}건 합격하였습니다.")
        } else if (actualAppliedCount > 0) {
            insightsList.add("현재 제출 완료된 ${actualAppliedCount}건이 서류 결과를 기다리는 중입니다.")
        }

        if (interviewFinishedCount > 0) {
            insightsList.add("면접 결과가 확인된 ${interviewFinishedCount}건 중 ${interviewPassedCount}건 합격하였습니다.")
        }

        return ApplicationStats(
            totalRegistered = filteredApps.size,
            actualAppliedCount = actualAppliedCount,
            docPassedCount = docPassedCount,
            docFailedCount = docFailedCount,
            docPendingCount = docPendingCount,
            docFinishedCount = docFinishedCount,
            docPassRate = docPassRate,
            interviewPassedCount = interviewPassedCount,
            interviewFailedCount = interviewFailedCount,
            interviewPendingCount = interviewPendingCount,
            interviewFinishedCount = interviewFinishedCount,
            interviewPassRate = interviewPassRate,
            finalPassedCount = finalPassedCount,
            finalFailedCount = finalFailedCount,
            finalPendingCount = finalPendingCount,
            finalFinishedCount = finalFinishedCount,
            finalPassRateVsSubmitted = finalPassRateVsSubmitted,
            finalPassRateVsConfirmed = finalPassRateVsConfirmed,
            smallSampleNotice = smallSampleNotice,
            availableJobs = jobs,
            availableDocVersions = sortedDocVersions,
            insights = insightsList
        )
    }

    private fun extractVersionLabels(json: String, typeName: String): List<String> {
        return try {
            val jsonElement = kotlinx.serialization.json.Json.parseToJsonElement(json)
            val obj = jsonElement as? kotlinx.serialization.json.JsonObject ?: return emptyList()
            val title = (obj["title"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: typeName
            val version = (obj["version"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "1.0"
            val id = (obj["id"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: ""
            listOf("[$typeName#$id] $title v$version")
        } catch (_: Exception) {
            emptyList()
        }
    }
}
