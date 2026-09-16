package com.thirtytwo_cereernote

import com.thirtytwo_cereernote.data.model.ApplicationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicTest {

    @Test
    fun testApplicationStatusLogic() {
        // Interest/Planned should not be counted as actual applied
        assertFalse(ApplicationStatus.INTERESTED.isActualApplied())
        assertFalse(ApplicationStatus.APPLY_PLANNED.isActualApplied())
        assertFalse(ApplicationStatus.CANCELLED.isActualApplied())
        
        // Completed and further should be actual applied
        assertTrue(ApplicationStatus.APPLY_COMPLETED.isActualApplied())
        assertTrue(ApplicationStatus.DOCUMENT_PASSED.isActualApplied())
        assertTrue(ApplicationStatus.FINAL_PASSED.isActualApplied())
    }

    @Test
    fun testPassLogic() {
        assertTrue(ApplicationStatus.DOCUMENT_PASSED.isPassedDocument())
        assertTrue(ApplicationStatus.INTERVIEW_PLANNED.isPassedDocument())
        assertTrue(ApplicationStatus.FINAL_PASSED.isPassedDocument())
        assertFalse(ApplicationStatus.DOCUMENT_FAILED.isPassedDocument())

        assertTrue(ApplicationStatus.INTERVIEW_PASSED.isPassedInterview())
        assertTrue(ApplicationStatus.FINAL_PASSED.isPassedInterview())
        assertFalse(ApplicationStatus.INTERVIEW_FAILED.isPassedInterview())
    }

    @Test
    fun testFinishedLogic() {
        assertTrue(ApplicationStatus.DOCUMENT_FAILED.isFinished())
        assertTrue(ApplicationStatus.INTERVIEW_FAILED.isFinished())
        assertTrue(ApplicationStatus.FINAL_PASSED.isFinished())
        assertTrue(ApplicationStatus.CANCELLED.isFinished())
        assertFalse(ApplicationStatus.APPLY_COMPLETED.isFinished())
    }
}
