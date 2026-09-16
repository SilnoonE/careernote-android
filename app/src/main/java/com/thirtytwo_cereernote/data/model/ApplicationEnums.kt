package com.thirtytwo_cereernote.data.model

enum class ApplicationStatus(val displayName: String) {
    INTERESTED("관심"),
    APPLY_PLANNED("지원 예정"),
    APPLY_COMPLETED("지원 완료"),
    DOCUMENT_REVIEW("서류 검토 중"),
    DOCUMENT_PASSED("서류 합격"),
    DOCUMENT_FAILED("서류 불합격"),
    INTERVIEW_PLANNED("면접 예정"),
    INTERVIEW_ONGOING("면접 진행"),
    INTERVIEW_PASSED("면접 합격"),
    INTERVIEW_FAILED("면접 불합격"),
    FINAL_PASSED("최종 합격"),
    ONBOARDING_PLANNED("입사 예정"),
    ONBOARDING_COMPLETED("입사 완료"),
    CANCELLED("지원 취소");

    fun isPassedDocument(): Boolean = this in setOf(
        DOCUMENT_PASSED, INTERVIEW_PLANNED, INTERVIEW_ONGOING, INTERVIEW_PASSED,
        INTERVIEW_FAILED, FINAL_PASSED, ONBOARDING_PLANNED, ONBOARDING_COMPLETED
    )

    fun isPassedInterview(): Boolean = this in setOf(
        INTERVIEW_PASSED, FINAL_PASSED, ONBOARDING_PLANNED, ONBOARDING_COMPLETED
    )

    fun isFinalPassed(): Boolean = this in setOf(
        FINAL_PASSED, ONBOARDING_PLANNED, ONBOARDING_COMPLETED
    )
    
    fun isFailedDocument(): Boolean = this == DOCUMENT_FAILED
    
    fun isFailedInterview(): Boolean = this == INTERVIEW_FAILED
    
    fun isFinished(): Boolean = this in setOf(
        DOCUMENT_FAILED, INTERVIEW_FAILED, FINAL_PASSED, ONBOARDING_PLANNED, ONBOARDING_COMPLETED, CANCELLED
    )

    fun isActualApplied(): Boolean = this !in setOf(INTERESTED, APPLY_PLANNED, CANCELLED)
}

enum class EmploymentType(val displayName: String) {
    FULL_TIME("정규직"),
    CONTRACT("계약직"),
    INTERN("인턴"),
    PART_TIME("아르바이트"),
    FREELANCER("프리랜서"),
    OTHER("기타")
}

enum class WorkMode(val displayName: String) {
    OFFICE("출근"),
    REMOTE("재택"),
    HYBRID("하이브리드")
}

enum class CompanySize(val displayName: String) {
    STARTUP("스타트업"),
    SME("중소기업"),
    MIDDLE("중견기업"),
    LARGE("대기업"),
    PUBLIC("공공기관"),
    FOREIGN("외국계"),
    OTHER("기타")
}
