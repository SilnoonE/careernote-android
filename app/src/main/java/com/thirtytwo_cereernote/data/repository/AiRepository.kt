package com.thirtytwo_cereernote.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

interface AiProvider {
    suspend fun generateCoverLetterDraft(question: String, experienceSummary: String): Result<String>
    suspend fun predictInterviewQuestions(companyInfo: String, jobTitle: String): Result<List<String>>
}

@Singleton
class AiRepository @Inject constructor() {
    private var activeProvider: AiProvider? = null

    fun setProvider(provider: AiProvider) {
        activeProvider = provider
    }

    fun isAiConnected(): Boolean = activeProvider != null

    suspend fun generateCoverLetterDraft(question: String, experienceSummary: String): Result<String> {
        val provider = activeProvider
        return if (provider != null) {
            provider.generateCoverLetterDraft(question, experienceSummary)
        } else {
            Result.failure(Exception("AI 서비스 제공자가 연결되지 않았습니다. 외부 API 키 또는 서버 설정을 연동해 주세요."))
        }
    }

    suspend fun predictInterviewQuestions(companyInfo: String, jobTitle: String): Result<List<String>> {
        val provider = activeProvider
        return if (provider != null) {
            provider.predictInterviewQuestions(companyInfo, jobTitle)
        } else {
            Result.failure(Exception("AI 서비스 제공자가 연결되지 않았습니다. 외부 API 키 또는 서버 설정을 연동해 주세요."))
        }
    }
}
