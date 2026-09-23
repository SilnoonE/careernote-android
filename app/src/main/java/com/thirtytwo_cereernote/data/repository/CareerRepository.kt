package com.thirtytwo_cereernote.data.repository

import com.thirtytwo_cereernote.data.database.CareerDao
import com.thirtytwo_cereernote.data.model.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CareerRepository @Inject constructor(
    private val careerDao: CareerDao
) {
    fun getAllCoverLetters() = careerDao.getAllCoverLetters()
    suspend fun getCoverLetterById(id: Long) = careerDao.getCoverLetterById(id)
    suspend fun insertCoverLetter(item: CoverLetter) = careerDao.insertCoverLetter(item)
    suspend fun updateCoverLetter(item: CoverLetter) = careerDao.updateCoverLetter(item)
    suspend fun deleteCoverLetter(item: CoverLetter) = careerDao.deleteCoverLetter(item)

    // CoverLetterQuestion
    suspend fun insertCoverLetterQuestion(item: CoverLetterQuestion) = careerDao.insertCoverLetterQuestion(item)
    suspend fun updateCoverLetterQuestion(item: CoverLetterQuestion) = careerDao.updateCoverLetterQuestion(item)
    suspend fun deleteCoverLetterQuestion(item: CoverLetterQuestion) = careerDao.deleteCoverLetterQuestion(item)
    fun getQuestionsByCoverLetterId(coverLetterId: Long) = careerDao.getQuestionsByCoverLetterId(coverLetterId)
    suspend fun getQuestionsByCoverLetterIdList(coverLetterId: Long) = careerDao.getQuestionsByCoverLetterIdList(coverLetterId)
    suspend fun deleteQuestionsByCoverLetterId(coverLetterId: Long) = careerDao.deleteQuestionsByCoverLetterId(coverLetterId)

    fun getAllResumes() = careerDao.getAllResumes()
    suspend fun getResumeById(id: Long) = careerDao.getResumeById(id)
    suspend fun insertResume(item: Resume) = careerDao.insertResume(item)
    suspend fun updateResume(item: Resume) = careerDao.updateResume(item)
    suspend fun deleteResume(item: Resume) = careerDao.deleteResume(item)

    fun getAllPortfolios() = careerDao.getAllPortfolios()
    suspend fun getPortfolioById(id: Long) = careerDao.getPortfolioById(id)
    suspend fun insertPortfolio(item: Portfolio) = careerDao.insertPortfolio(item)
    suspend fun updatePortfolio(item: Portfolio) = careerDao.updatePortfolio(item)
    suspend fun deletePortfolio(item: Portfolio) = careerDao.deletePortfolio(item)

    fun getAllProjects() = careerDao.getAllProjects()
    suspend fun getProjectById(id: Long) = careerDao.getProjectById(id)
    suspend fun insertProject(item: Project) = careerDao.insertProject(item)
    suspend fun updateProject(item: Project) = careerDao.updateProject(item)
    suspend fun deleteProject(item: Project) = careerDao.deleteProject(item)

    fun getAllCertifications() = careerDao.getAllCertifications()
    suspend fun getCertificationById(id: Long) = careerDao.getCertificationById(id)
    suspend fun insertCertification(item: Certification) = careerDao.insertCertification(item)
    suspend fun updateCertification(item: Certification) = careerDao.updateCertification(item)
    suspend fun deleteCertification(item: Certification) = careerDao.deleteCertification(item)

    fun getAllInterviewQuestions() = careerDao.getAllInterviewQuestions()
    suspend fun getInterviewQuestionById(id: Long) = careerDao.getInterviewQuestionById(id)
    suspend fun insertInterviewQuestion(item: InterviewQuestion) = careerDao.insertInterviewQuestion(item)
    suspend fun updateInterviewQuestion(item: InterviewQuestion) = careerDao.updateInterviewQuestion(item)
    suspend fun deleteInterviewQuestion(item: InterviewQuestion) = careerDao.deleteInterviewQuestion(item)

    fun getAllExperiences() = careerDao.getAllExperiences()
    suspend fun getExperienceById(id: Long) = careerDao.getExperienceById(id)
    suspend fun insertExperience(item: CareerExperience) = careerDao.insertExperience(item)
    suspend fun updateExperience(item: CareerExperience) = careerDao.updateExperience(item)
    suspend fun deleteExperience(item: CareerExperience) = careerDao.deleteExperience(item)

    fun getAllEducations() = careerDao.getAllEducations()
    suspend fun getEducationById(id: Long) = careerDao.getEducationById(id)
    suspend fun insertEducation(item: Education) = careerDao.insertEducation(item)
    suspend fun updateEducation(item: Education) = careerDao.updateEducation(item)
    suspend fun deleteEducation(item: Education) = careerDao.deleteEducation(item)

    // Practice Sessions
    suspend fun insertPracticeSession(session: PracticeSession) = careerDao.insertPracticeSession(session)
    suspend fun updatePracticeSession(session: PracticeSession) = careerDao.updatePracticeSession(session)
    suspend fun getPracticeSessionById(id: Long) = careerDao.getPracticeSessionById(id)
    suspend fun getUnfinishedPracticeSession() = careerDao.getUnfinishedPracticeSession()
    fun getAllPracticeSessions() = careerDao.getAllPracticeSessions()
    suspend fun deletePracticeSession(session: PracticeSession) = careerDao.deletePracticeSession(session)

    // Practice Question Results
    suspend fun insertPracticeQuestionResult(result: PracticeQuestionResult) = careerDao.insertPracticeQuestionResult(result)
    suspend fun updatePracticeQuestionResult(result: PracticeQuestionResult) = careerDao.updatePracticeQuestionResult(result)
    fun getPracticeQuestionResultsBySession(sessionId: Long) = careerDao.getPracticeQuestionResultsBySession(sessionId)
    suspend fun getPracticeQuestionResultsListBySession(sessionId: Long) = careerDao.getPracticeQuestionResultsListBySession(sessionId)
}
