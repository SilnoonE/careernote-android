package com.thirtytwo_cereernote.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thirtytwo_cereernote.data.model.*
import com.thirtytwo_cereernote.data.repository.ApplicationRepository
import com.thirtytwo_cereernote.data.repository.CareerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import java.util.Date
import javax.inject.Inject

data class PracticeQuestionCandidate(
    val questionText: String,
    val sourceType: String, // INTERVIEW_QUESTION, COVER_LETTER
    val sourceLabel: String,
    val originalAnswer: String
)

data class PracticeUiState(
    val isSessionActive: Boolean = false,
    val currentSession: PracticeSession? = null,
    val currentQuestionIndex: Int = 0,
    val totalQuestionsCount: Int = 0,
    val currentQuestionResult: PracticeQuestionResult? = null,
    val isOriginalAnswerVisible: Boolean = false,
    val practiceAnswerText: String = "",
    val availableCandidatesCount: Int = 0,
    val sessionResults: List<PracticeQuestionResult> = emptyList(),
    val isCompleted: Boolean = false,
    val needsWorkCount: Int = 0,
    val errorMessage: String? = null,
    val pastSessions: List<PracticeSession> = emptyList()
)

@HiltViewModel
class PracticeViewModel @Inject constructor(
    private val applicationRepository: ApplicationRepository,
    private val careerRepository: CareerRepository
) : ViewModel() {

    private val _selectedApplicationId = MutableStateFlow<Long?>(null)
    private val _currentSessionId = MutableStateFlow<Long?>(null)
    private val _currentQuestionIndex = MutableStateFlow(0)
    private val _isOriginalAnswerVisible = MutableStateFlow(false)
    private val _practiceAnswerText = MutableStateFlow("")
    private val _errorMessage = MutableStateFlow<String?>(null)

    fun selectApplication(appId: Long?) {
        _selectedApplicationId.value = appId
    }

    val pastSessions: StateFlow<List<PracticeSession>> = careerRepository.getAllPracticeSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val candidatesFlow: StateFlow<List<PracticeQuestionCandidate>> = combine(
        applicationRepository.allApplications,
        careerRepository.getAllInterviewQuestions(),
        _selectedApplicationId
    ) { apps, interviewQuestions, selectedAppId ->
        val candidates = mutableListOf<PracticeQuestionCandidate>()

        val filteredIQs = if (selectedAppId != null) {
            interviewQuestions.filter { it.interviewId != null }
        } else {
            interviewQuestions
        }

        filteredIQs.forEach { iq ->
            if (iq.question.isNotBlank()) {
                candidates.add(
                    PracticeQuestionCandidate(
                        questionText = iq.question.trim(),
                        sourceType = "INTERVIEW_QUESTION",
                        sourceLabel = if (iq.category.isNotBlank()) "면접 질문 (${iq.category})" else "면접 질문",
                        originalAnswer = iq.myAnswer.ifBlank { iq.betterAnswer }
                    )
                )
            }
        }

        val filteredApps = if (selectedAppId != null) {
            apps.filter { it.id == selectedAppId }
        } else {
            apps
        }

        filteredApps.forEach { app ->
            app.coverLetterSnapshot?.let { json ->
                try {
                    val snap = Json.decodeFromString<CoverLetterSnapshot>(json)
                    snap.questions.forEach { q ->
                        if (q.question.isNotBlank()) {
                            candidates.add(
                                PracticeQuestionCandidate(
                                    questionText = q.question.trim(),
                                    sourceType = "COVER_LETTER",
                                    sourceLabel = "자기소개서 기반 (${app.companyName})",
                                    originalAnswer = q.answer
                                )
                            )
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        candidates.distinctBy { it.questionText }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private data class SessionFormState(
        val sessionId: Long?,
        val index: Int,
        val showAnswer: Boolean,
        val text: String,
        val error: String?
    )

    private val sessionFormFlow = combine(
        _currentSessionId,
        _currentQuestionIndex,
        _isOriginalAnswerVisible,
        _practiceAnswerText,
        _errorMessage
    ) { sessionId, index, showAnswer, text, err ->
        SessionFormState(sessionId, index, showAnswer, text, err)
    }

    val uiState: StateFlow<PracticeUiState> = combine(
        sessionFormFlow,
        candidatesFlow,
        pastSessions
    ) { formState, candidates, sessions ->
        val sessionId = formState.sessionId
        if (sessionId == null) {
            return@combine PracticeUiState(
                isSessionActive = false,
                availableCandidatesCount = candidates.size,
                errorMessage = formState.error,
                pastSessions = sessions
            )
        }

        val session = careerRepository.getPracticeSessionById(sessionId)
        val results = careerRepository.getPracticeQuestionResultsListBySession(sessionId)

        val total = results.size
        val index = formState.index
        val currentRes = if (index in results.indices) results[index] else null
        val isDone = session?.isCompleted == true || (total > 0 && index >= total)

        val needsWork = results.count { it.selfEvaluation == "NEEDS_WORK" || it.selfEvaluation == "RETRY" }

        PracticeUiState(
            isSessionActive = !isDone,
            currentSession = session,
            currentQuestionIndex = index,
            totalQuestionsCount = total,
            currentQuestionResult = currentRes,
            isOriginalAnswerVisible = formState.showAnswer,
            practiceAnswerText = formState.text,
            availableCandidatesCount = candidates.size,
            sessionResults = results,
            isCompleted = isDone,
            needsWorkCount = needsWork,
            errorMessage = formState.error,
            pastSessions = sessions
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PracticeUiState())

    fun startNewSession(applicationId: Long? = null, customCandidates: List<PracticeQuestionCandidate>? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val candidateList = customCandidates ?: candidatesFlow.value
            if (candidateList.isEmpty()) {
                _errorMessage.value = "연습 가능한 면접 질문이나 자기소개서 문항이 없습니다."
                return@launch
            }

            _errorMessage.value = null
            val selected = candidateList.shuffled().take(3)

            val session = PracticeSession(
                applicationId = applicationId,
                startedAt = Date(),
                isCompleted = false
            )
            val sessionId = careerRepository.insertPracticeSession(session)

            if (sessionId > 0) {
                selected.forEach { candidate ->
                    careerRepository.insertPracticeQuestionResult(
                        PracticeQuestionResult(
                            sessionId = sessionId,
                            questionText = candidate.questionText,
                            sourceType = candidate.sourceType,
                            sourceLabel = candidate.sourceLabel,
                            originalAnswer = candidate.originalAnswer,
                            practiceAnswer = "",
                            selfEvaluation = "GOOD",
                            updatedAt = Date()
                        )
                    )
                }
                _currentSessionId.value = sessionId
                _currentQuestionIndex.value = 0
                _isOriginalAnswerVisible.value = false
                _practiceAnswerText.value = ""
            } else {
                _errorMessage.value = "연습 세션을 생성하지 못했습니다."
            }
        }
    }

    fun toggleOriginalAnswer() {
        _isOriginalAnswerVisible.value = !_isOriginalAnswerVisible.value
    }

    fun updatePracticeAnswerText(text: String) {
        _practiceAnswerText.value = text
    }

    fun submitQuestionEvaluation(evaluation: String) {
        val sessionId = _currentSessionId.value ?: return
        val index = _currentQuestionIndex.value

        viewModelScope.launch(Dispatchers.IO) {
            val results = careerRepository.getPracticeQuestionResultsListBySession(sessionId)
            if (index in results.indices) {
                val current = results[index]
                careerRepository.updatePracticeQuestionResult(
                    current.copy(
                        practiceAnswer = _practiceAnswerText.value.trim(),
                        selfEvaluation = evaluation,
                        updatedAt = Date()
                    )
                )
            }

            val nextIndex = index + 1
            if (nextIndex >= results.size) {
                val session = careerRepository.getPracticeSessionById(sessionId)
                if (session != null) {
                    careerRepository.updatePracticeSession(
                        session.copy(
                            completedAt = Date(),
                            isCompleted = true
                        )
                    )
                }
            }

            _currentQuestionIndex.value = nextIndex
            _isOriginalAnswerVisible.value = false
            _practiceAnswerText.value = ""
        }
    }

    fun retryNeedsWork() {
        val sessionId = _currentSessionId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val results = careerRepository.getPracticeQuestionResultsListBySession(sessionId)
            val needsWorkList = results.filter { it.selfEvaluation == "NEEDS_WORK" || it.selfEvaluation == "RETRY" }.map { res ->
                PracticeQuestionCandidate(
                    questionText = res.questionText,
                    sourceType = res.sourceType,
                    sourceLabel = res.sourceLabel,
                    originalAnswer = res.originalAnswer
                )
            }
            if (needsWorkList.isNotEmpty()) {
                startNewSession(customCandidates = needsWorkList)
            }
        }
    }

    fun finishSession() {
        _currentSessionId.value = null
        _currentQuestionIndex.value = 0
        _isOriginalAnswerVisible.value = false
        _practiceAnswerText.value = ""
    }

    fun deleteSession(session: PracticeSession) {
        viewModelScope.launch(Dispatchers.IO) {
            careerRepository.deletePracticeSession(session)
            if (_currentSessionId.value == session.id) {
                finishSession()
            }
        }
    }
}
