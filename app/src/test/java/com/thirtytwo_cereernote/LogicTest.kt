package com.thirtytwo_cereernote

import com.thirtytwo_cereernote.data.model.*
import com.thirtytwo_cereernote.util.ApplicationStatsCalculator
import com.thirtytwo_cereernote.util.StatFilter
import com.thirtytwo_cereernote.util.StatPeriod
import com.thirtytwo_cereernote.viewmodel.TimeProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class TestTimeProvider(var testDate: Date) : TimeProvider {
    override fun now(): Date = testDate
    override fun today(): LocalDate = Instant.ofEpochMilli(testDate.time).atZone(ZoneId.systemDefault()).toLocalDate()
}

class LogicTest {

    @Test
    fun testApplicationStatusHelpers() {
        assertFalse(ApplicationStatus.INTERESTED.isActualApplied())
        assertFalse(ApplicationStatus.APPLY_PLANNED.isActualApplied())
        assertFalse(ApplicationStatus.CANCELLED.isActualApplied())

        assertTrue(ApplicationStatus.APPLY_COMPLETED.isActualApplied())
        assertTrue(ApplicationStatus.DOCUMENT_PASSED.isActualApplied())
        assertTrue(ApplicationStatus.FINAL_PASSED.isActualApplied())
    }

    @Test
    fun testStatsWithTwoPassedAndEightPending() {
        val apps = mutableListOf<Application>()
        apps.add(Application(id = 1, companyName = "A", jobTitle = "Dev", currentStatus = ApplicationStatus.DOCUMENT_PASSED, submittedDate = Date()))
        apps.add(Application(id = 2, companyName = "B", jobTitle = "Dev", currentStatus = ApplicationStatus.DOCUMENT_PASSED, submittedDate = Date()))
        for (i in 3..10) {
            apps.add(Application(id = i.toLong(), companyName = "Company$i", jobTitle = "Dev", currentStatus = ApplicationStatus.APPLY_COMPLETED, submittedDate = Date()))
        }

        val stats = ApplicationStatsCalculator.calculate(applications = apps, statusHistories = emptyList())

        assertEquals(10, stats.actualAppliedCount)
        assertEquals(2, stats.docPassedCount)
        assertEquals(0, stats.docFailedCount)
        assertEquals(2, stats.docFinishedCount)
        assertEquals(8, stats.docPendingCount)
        assertEquals(100.0f, stats.docPassRate!!, 0.01f)
    }

    @Test
    fun testStatsWhenConfirmedCountIsZero() {
        val apps = (1..5).map {
            Application(id = it.toLong(), companyName = "Company$it", jobTitle = "Dev", currentStatus = ApplicationStatus.APPLY_COMPLETED, submittedDate = Date())
        }

        val stats = ApplicationStatsCalculator.calculate(applications = apps, statusHistories = emptyList())

        assertEquals(5, stats.actualAppliedCount)
        assertEquals(0, stats.docFinishedCount)
        assertEquals(5, stats.docPendingCount)
        assertNull("합격률은 0%로 표시되지 않고 null이어야 함", stats.docPassRate)
        assertTrue(stats.smallSampleNotice!!.contains("확인된 결과가 없습니다"))
    }

    @Test
    fun testNoDoubleCountingWhenInterviewsExist() {
        val app = Application(id = 1, companyName = "A", jobTitle = "Dev", currentStatus = ApplicationStatus.DOCUMENT_PASSED, submittedDate = Date())
        val histories = listOf(
            ApplicationStatusHistory(id = 10, applicationId = 1, status = ApplicationStatus.DOCUMENT_FAILED),
            ApplicationStatusHistory(id = 11, applicationId = 1, status = ApplicationStatus.DOCUMENT_PASSED)
        )
        val interviews = listOf(
            Interview(id = 101, applicationId = 1, stage = "1차 면접", interviewDate = Date())
        )

        val stats = ApplicationStatsCalculator.calculate(applications = listOf(app), statusHistories = histories, interviews = interviews)

        assertEquals(1, stats.docPassedCount)
        assertEquals(0, stats.docFailedCount)
        assertEquals(1, stats.docFinishedCount)
    }

    @Test
    fun testScheduledFutureInterviewNotCountedAsResultPending() {
        val futureDate = Date(System.currentTimeMillis() + 86400000L * 7) // 7 days in future
        val app = Application(id = 1, companyName = "A", jobTitle = "Dev", currentStatus = ApplicationStatus.INTERVIEW_PLANNED, submittedDate = Date())
        val interviews = listOf(
            Interview(id = 101, applicationId = 1, stage = "1차 면접", interviewDate = futureDate, isCompleted = false)
        )

        val stats = ApplicationStatsCalculator.calculate(applications = listOf(app), statusHistories = emptyList(), interviews = interviews)

        assertEquals(0, stats.interviewPendingCount)
        assertEquals(0, stats.interviewFinishedCount)
    }

    @Test
    fun testPreparationMemoNotTrickingReviewCompletion() {
        val interview = Interview(
            id = 1,
            applicationId = 10,
            stage = "1차 면접",
            interviewDate = Date(System.currentTimeMillis() - 86400000L),
            preparations = "1분 자기소개 말하기", // Pre-interview prep memo
            nextPreparations = "",
            strengths = "",
            weaknesses = "",
            review = "",
            isCompleted = false
        )

        assertFalse("면접 전 준비 메모(preparations)만으로는 복기 완료로 판단되어서는 안 됨", interview.isReviewCompleted())
    }

    @Test
    fun testReviewWithWeaknessesOnlyIsCompleted() {
        val interview = Interview(
            id = 1,
            applicationId = 10,
            stage = "1차 면접",
            interviewDate = Date(System.currentTimeMillis() - 86400000L),
            weaknesses = "질문에 당황함",
            isCompleted = true
        )

        assertTrue("아쉬운 점만 적혀있어도 복기 작성 완료로 판단되어야 함", interview.isReviewCompleted())
    }

    @Test
    fun testFilterResultZeroHasTotalRegisteredZero() {
        val apps = listOf(
            Application(id = 1, companyName = "A", jobTitle = "Dev", currentStatus = ApplicationStatus.APPLY_COMPLETED, submittedDate = Date())
        )

        val stats = ApplicationStatsCalculator.calculate(
            applications = apps,
            statusHistories = emptyList(),
            filter = StatFilter(jobFilter = "NON_EXISTENT_JOB")
        )

        assertEquals(0, stats.totalRegistered)
        assertEquals(0, stats.actualAppliedCount)
    }

    @Test
    fun testBothResumeAndCoverLetterInVersionFilter() {
        val app = Application(
            id = 1,
            companyName = "A",
            jobTitle = "Dev",
            currentStatus = ApplicationStatus.APPLY_COMPLETED,
            submittedDate = Date(),
            resumeSnapshot = """{"id":1, "title":"이력서1", "version":"1.0"}""",
            coverLetterSnapshot = """{"id":2, "title":"자소서1", "version":"2.0"}"""
        )

        val stats = ApplicationStatsCalculator.calculate(applications = listOf(app), statusHistories = emptyList())

        assertTrue(stats.availableDocVersions.contains("[이력서#1] 이력서1 v1.0"))
        assertTrue(stats.availableDocVersions.contains("[자소서#2] 자소서1 v2.0"))
    }

    @Test
    fun testPreSubmissionCancelVsPostSubmissionWithdrawal() {
        val apps = listOf(
            Application(id = 1, companyName = "PreCancel", jobTitle = "Dev", currentStatus = ApplicationStatus.CANCELLED, submittedDate = null),
            Application(id = 2, companyName = "PostWithdraw", jobTitle = "Dev", currentStatus = ApplicationStatus.CANCELLED, submittedDate = Date())
        )

        val stats = ApplicationStatsCalculator.calculate(applications = apps, statusHistories = emptyList())

        assertEquals(2, stats.totalRegistered)
        assertEquals(1, stats.actualAppliedCount)
    }
}
