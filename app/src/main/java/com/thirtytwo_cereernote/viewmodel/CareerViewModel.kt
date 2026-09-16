package com.thirtytwo_cereernote.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thirtytwo_cereernote.data.model.*
import com.thirtytwo_cereernote.data.repository.CareerRepository
import com.thirtytwo_cereernote.data.repository.PreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import com.thirtytwo_cereernote.R
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.util.Date
import javax.inject.Inject
import kotlinx.serialization.json.Json

data class CareerSummary(
    val counts: Map<String, Int> = emptyMap()
)

data class RecentActivity(
    val id: Long,
    val type: String,
    val typeName: String,
    val title: String,
    val date: Date,
    val categoryId: String,
    val titleResId: Int
)

@HiltViewModel
class CareerViewModel @Inject constructor(
    private val repository: CareerRepository,
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {

    val drafts = preferenceRepository.draftsJson.map { 
        try {
            Json.decodeFromString<Map<String, Draft>>(it)
        } catch (e: Exception) {
            emptyMap<String, Draft>()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun saveDraft(type: String, itemId: Long, f1: String, f2: String, f3: String, f4: String = "", f5: String = "", f6: String = "", f7: String = "", f8: String = "", f9: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            preferenceRepository.updateDraft(type, itemId) {
                Draft(type, itemId, f1, f2, f3, f4, f5, f6, f7, f8, f9, System.currentTimeMillis())
            }
        }
    }

    suspend fun removeDraft(type: String, itemId: Long) = withContext(Dispatchers.IO) {
        preferenceRepository.updateDraft(type, itemId) { null }
    }

    // Cover Letter
    suspend fun saveCoverLetter(title: String, company: String, content: String): Long = withContext(Dispatchers.IO) {
        val id = repository.insertCoverLetter(CoverLetter(title = title, companyName = company, memo = content))
        if (id > 0) removeDraft("cover_letter", 0L)
        id
    }

    suspend fun updateCoverLetter(item: CoverLetter): Boolean = withContext(Dispatchers.IO) {
        repository.updateCoverLetter(item.copy(updatedAt = Date()))
        removeDraft("cover_letter", item.id)
        true
    }

    suspend fun deleteCoverLetter(item: CoverLetter) = withContext(Dispatchers.IO) {
        repository.deleteCoverLetter(item)
    }

    suspend fun duplicateCoverLetter(item: CoverLetter): Long = withContext(Dispatchers.IO) {
        val newId = repository.insertCoverLetter(item.copy(id = 0, title = "${item.title} (복사본)", updatedAt = Date(), createdAt = Date()))
        if (newId > 0) {
            val questions = repository.getQuestionsByCoverLetterIdList(item.id)
            questions.forEach {
                repository.insertCoverLetterQuestion(it.copy(id = 0, coverLetterId = newId))
            }
        }
        newId
    }

    suspend fun getCoverLetterById(id: Long) = repository.getCoverLetterById(id)
    suspend fun getResumeById(id: Long) = repository.getResumeById(id)
    suspend fun getPortfolioById(id: Long) = repository.getPortfolioById(id)
    suspend fun getProjectById(id: Long) = repository.getProjectById(id)
    suspend fun getCertificationById(id: Long) = repository.getCertificationById(id)
    suspend fun getInterviewQuestionById(id: Long) = repository.getInterviewQuestionById(id)
    suspend fun getExperienceById(id: Long) = repository.getExperienceById(id)
    suspend fun getEducationById(id: Long) = repository.getEducationById(id)

    // Resume
    suspend fun saveResume(title: String, version: String, memo: String): Long = withContext(Dispatchers.IO) {
        val id = repository.insertResume(Resume(title = title, version = version, memo = memo))
        if (id > 0) removeDraft("resume", 0L)
        id
    }

    suspend fun updateResume(item: Resume): Boolean = withContext(Dispatchers.IO) {
        repository.updateResume(item.copy(updatedAt = Date()))
        removeDraft("resume", item.id)
        true
    }

    suspend fun deleteResume(item: Resume) = withContext(Dispatchers.IO) {
        repository.deleteResume(item)
    }

    suspend fun duplicateResume(item: Resume): Long = withContext(Dispatchers.IO) {
        repository.insertResume(item.copy(id = 0, title = "${item.title} (복사본)", updatedAt = Date(), createdAt = Date()))
    }

    // Portfolio
    suspend fun savePortfolio(title: String, url: String, memo: String): Long = withContext(Dispatchers.IO) {
        val id = repository.insertPortfolio(Portfolio(title = title, url = url, memo = memo))
        if (id > 0) removeDraft("portfolio", 0L)
        id
    }

    suspend fun updatePortfolio(item: Portfolio): Boolean = withContext(Dispatchers.IO) {
        repository.updatePortfolio(item.copy(updatedAt = Date()))
        removeDraft("portfolio", item.id)
        true
    }

    suspend fun deletePortfolio(item: Portfolio) = withContext(Dispatchers.IO) {
        repository.deletePortfolio(item)
    }

    suspend fun duplicatePortfolio(item: Portfolio): Long = withContext(Dispatchers.IO) {
        repository.insertPortfolio(item.copy(id = 0, title = "${item.title} (복사본)", updatedAt = Date(), createdAt = Date()))
    }

    // Project
    suspend fun saveProject(name: String, role: String, tech: String, desc: String, prob: String = "", outcome: String = ""): Long = withContext(Dispatchers.IO) {
        val id = repository.insertProject(Project(
            name = name, 
            role = role, 
            techStack = tech, 
            description = desc, 
            problem = prob, 
            outcome = outcome,
            updatedAt = Date()
        ))
        if (id > 0) removeDraft("project", 0L)
        id
    }

    suspend fun updateProject(item: Project): Boolean = withContext(Dispatchers.IO) {
        repository.updateProject(item.copy(updatedAt = Date()))
        removeDraft("project", item.id)
        true
    }

    suspend fun deleteProject(item: Project) = withContext(Dispatchers.IO) {
        repository.deleteProject(item)
    }

    suspend fun duplicateProject(item: Project): Long = withContext(Dispatchers.IO) {
        repository.insertProject(item.copy(id = 0, name = "${item.name} (복사본)", updatedAt = Date()))
    }

    // Certification
    suspend fun saveCertification(name: String, issuer: String, score: String, grade: String = "", acquired: Date? = null): Long = withContext(Dispatchers.IO) {
        val id = repository.insertCertification(Certification(name = name, issuer = issuer, score = score, grade = grade, acquisitionDate = acquired, updatedAt = Date()))
        if (id > 0) removeDraft("certification", 0L)
        id
    }

    suspend fun updateCertification(item: Certification): Boolean = withContext(Dispatchers.IO) {
        repository.updateCertification(item.copy(updatedAt = Date()))
        removeDraft("certification", item.id)
        true
    }

    suspend fun deleteCertification(item: Certification) = withContext(Dispatchers.IO) {
        repository.deleteCertification(item)
    }

    suspend fun duplicateCertification(item: Certification): Long = withContext(Dispatchers.IO) {
        repository.insertCertification(item.copy(id = 0, name = "${item.name} (복사본)", updatedAt = Date()))
    }

    // Interview Question
    suspend fun saveInterviewQuestion(category: String, question: String, answer: String, better: String = ""): Long = withContext(Dispatchers.IO) {
        val id = repository.insertInterviewQuestion(InterviewQuestion(category = category, question = question, myAnswer = answer, betterAnswer = better, interviewId = null, updatedAt = Date()))
        if (id > 0) removeDraft("interview_question", 0L)
        id
    }

    suspend fun updateInterviewQuestion(item: InterviewQuestion): Boolean = withContext(Dispatchers.IO) {
        repository.updateInterviewQuestion(item.copy(updatedAt = Date()))
        removeDraft("interview_question", item.id)
        true
    }

    suspend fun deleteInterviewQuestion(item: InterviewQuestion) = withContext(Dispatchers.IO) {
        repository.deleteInterviewQuestion(item)
    }

    suspend fun duplicateInterviewQuestion(item: InterviewQuestion): Long = withContext(Dispatchers.IO) {
        repository.insertInterviewQuestion(item.copy(id = 0, question = "${item.question} (복사본)", updatedAt = Date()))
    }

    // Experience
    suspend fun saveExperience(company: String, job: String, desc: String, outcome: String = "", start: Date = Date(), end: Date? = null, current: Boolean = false): Long = withContext(Dispatchers.IO) {
        val id = repository.insertExperience(CareerExperience(companyName = company, jobTitle = job, description = desc, outcome = outcome, startDate = start, endDate = end, isCurrent = current, updatedAt = Date()))
        if (id > 0) removeDraft("experience", 0L)
        id
    }

    suspend fun updateExperience(item: CareerExperience): Boolean = withContext(Dispatchers.IO) {
        repository.updateExperience(item.copy(updatedAt = Date()))
        removeDraft("experience", item.id)
        true
    }

    suspend fun deleteExperience(item: CareerExperience) = withContext(Dispatchers.IO) {
        repository.deleteExperience(item)
    }

    suspend fun duplicateExperience(item: CareerExperience): Long = withContext(Dispatchers.IO) {
        repository.insertExperience(item.copy(id = 0, companyName = "${item.companyName} (복사본)", updatedAt = Date()))
    }

    // Education
    suspend fun saveEducation(name: String, institution: String, status: String, desc: String = "", start: Date = Date(), end: Date? = null): Long = withContext(Dispatchers.IO) {
        val id = repository.insertEducation(Education(name = name, institution = institution, status = status, description = desc, startDate = start, endDate = end, updatedAt = Date()))
        if (id > 0) removeDraft("education", 0L)
        id
    }

    suspend fun updateEducation(item: Education): Boolean = withContext(Dispatchers.IO) {
        repository.updateEducation(item.copy(updatedAt = Date()))
        removeDraft("education", item.id)
        true
    }

    suspend fun deleteEducation(item: Education) = withContext(Dispatchers.IO) {
        repository.deleteEducation(item)
    }

    suspend fun duplicateEducation(item: Education): Long = withContext(Dispatchers.IO) {
        repository.insertEducation(item.copy(id = 0, name = "${item.name} (복사본)", updatedAt = Date()))
    }

    // Flows
    val coverLetters: StateFlow<List<CoverLetter>> = repository.getAllCoverLetters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val resumes: StateFlow<List<Resume>> = repository.getAllResumes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val portfolios: StateFlow<List<Portfolio>> = repository.getAllPortfolios()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projects: StateFlow<List<Project>> = repository.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val certifications: StateFlow<List<Certification>> = repository.getAllCertifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val interviewQuestions: StateFlow<List<InterviewQuestion>> = repository.getAllInterviewQuestions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val experiences: StateFlow<List<CareerExperience>> = repository.getAllExperiences()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val educations: StateFlow<List<Education>> = repository.getAllEducations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val careerSummary: StateFlow<CareerSummary> = combine(
        repository.getAllCoverLetters(),
        repository.getAllResumes(),
        repository.getAllPortfolios(),
        repository.getAllProjects(),
        repository.getAllCertifications(),
        repository.getAllInterviewQuestions(),
        repository.getAllExperiences(),
        repository.getAllEducations()
    ) { flows ->
        CareerSummary(
            counts = mapOf(
                "cover_letter" to (flows[0] as List<*>).size,
                "resume" to (flows[1] as List<*>).size,
                "portfolio" to (flows[2] as List<*>).size,
                "project" to (flows[3] as List<*>).size,
                "certification" to (flows[4] as List<*>).size,
                "interview_question" to (flows[5] as List<*>).size,
                "experience" to (flows[6] as List<*>).size,
                "education" to (flows[7] as List<*>).size
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CareerSummary())

    val recentActivities: StateFlow<List<RecentActivity>> = combine(
        repository.getAllCoverLetters(),
        repository.getAllResumes(),
        repository.getAllPortfolios(),
        repository.getAllProjects(),
        repository.getAllCertifications(),
        repository.getAllInterviewQuestions(),
        repository.getAllExperiences(),
        repository.getAllEducations()
    ) { flows ->
        val allItems = mutableListOf<RecentActivity>()
        
        (flows[0] as List<CoverLetter>).forEach { allItems.add(RecentActivity(it.id, "cover_letter", "자기소개서", it.title, it.updatedAt, "cover_letter", R.string.career_cover_letter)) }
        (flows[1] as List<Resume>).forEach { allItems.add(RecentActivity(it.id, "resume", "이력서", it.title, it.updatedAt, "resume", R.string.career_resume)) }
        (flows[2] as List<Portfolio>).forEach { allItems.add(RecentActivity(it.id, "portfolio", "포트폴리오", it.title, it.updatedAt, "portfolio", R.string.career_portfolio)) }
        (flows[3] as List<Project>).forEach { allItems.add(RecentActivity(it.id, "project", "프로젝트", it.name, it.updatedAt, "project", R.string.career_project)) }
        (flows[4] as List<Certification>).forEach { allItems.add(RecentActivity(it.id, "certification", "자격증", it.name, it.updatedAt, "certification", R.string.career_certification)) }
        (flows[5] as List<InterviewQuestion>).forEach { allItems.add(RecentActivity(it.id, "interview_question", "면접 질문", it.question, it.updatedAt, "interview_question", R.string.career_interview_question)) }
        (flows[6] as List<CareerExperience>).forEach { allItems.add(RecentActivity(it.id, "experience", "경력", it.companyName, it.updatedAt, "experience", R.string.career_experience)) }
        (flows[7] as List<Education>).forEach { allItems.add(RecentActivity(it.id, "education", "교육", it.institution, it.updatedAt, "education", R.string.career_education)) }
        
        allItems.sortedByDescending { it.date }.take(10)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
