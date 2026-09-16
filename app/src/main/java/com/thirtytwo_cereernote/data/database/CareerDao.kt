package com.thirtytwo_cereernote.data.database

import androidx.room.*
import com.thirtytwo_cereernote.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CareerDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCoverLetter(item: CoverLetter): Long

    @Query("SELECT * FROM cover_letters ORDER BY updatedAt DESC")
    fun getAllCoverLetters(): Flow<List<CoverLetter>>

    @Query("SELECT * FROM cover_letters WHERE id = :id")
    suspend fun getCoverLetterById(id: Long): CoverLetter?

    @Update
    suspend fun updateCoverLetter(item: CoverLetter)

    @Delete
    suspend fun deleteCoverLetter(item: CoverLetter)

    // CoverLetterQuestion CRUD
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCoverLetterQuestion(item: CoverLetterQuestion): Long

    @Update
    suspend fun updateCoverLetterQuestion(item: CoverLetterQuestion)

    @Delete
    suspend fun deleteCoverLetterQuestion(item: CoverLetterQuestion)

    @Query("SELECT * FROM cover_letter_questions WHERE coverLetterId = :coverLetterId")
    fun getQuestionsByCoverLetterId(coverLetterId: Long): Flow<List<CoverLetterQuestion>>

    @Query("SELECT * FROM cover_letter_questions WHERE coverLetterId = :coverLetterId")
    suspend fun getQuestionsByCoverLetterIdList(coverLetterId: Long): List<CoverLetterQuestion>

    @Query("DELETE FROM cover_letter_questions WHERE coverLetterId = :coverLetterId")
    suspend fun deleteQuestionsByCoverLetterId(coverLetterId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertResume(item: Resume): Long

    @Query("SELECT * FROM resumes ORDER BY updatedAt DESC")
    fun getAllResumes(): Flow<List<Resume>>

    @Query("SELECT * FROM resumes WHERE id = :id")
    suspend fun getResumeById(id: Long): Resume?

    @Update
    suspend fun updateResume(item: Resume)

    @Delete
    suspend fun deleteResume(item: Resume)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPortfolio(item: Portfolio): Long

    @Query("SELECT * FROM portfolios ORDER BY updatedAt DESC")
    fun getAllPortfolios(): Flow<List<Portfolio>>

    @Query("SELECT * FROM portfolios WHERE id = :id")
    suspend fun getPortfolioById(id: Long): Portfolio?

    @Update
    suspend fun updatePortfolio(item: Portfolio)

    @Delete
    suspend fun deletePortfolio(item: Portfolio)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProject(item: Project): Long

    @Query("SELECT * FROM projects")
    fun getAllProjects(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Long): Project?

    @Update
    suspend fun updateProject(item: Project)

    @Delete
    suspend fun deleteProject(item: Project)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCertification(item: Certification): Long

    @Query("SELECT * FROM certifications")
    fun getAllCertifications(): Flow<List<Certification>>

    @Query("SELECT * FROM certifications WHERE id = :id")
    suspend fun getCertificationById(id: Long): Certification?

    @Update
    suspend fun updateCertification(item: Certification)

    @Delete
    suspend fun deleteCertification(item: Certification)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInterviewQuestion(item: InterviewQuestion): Long

    @Query("SELECT * FROM interview_questions")
    fun getAllInterviewQuestions(): Flow<List<InterviewQuestion>>

    @Query("SELECT * FROM interview_questions WHERE id = :id")
    suspend fun getInterviewQuestionById(id: Long): InterviewQuestion?

    @Update
    suspend fun updateInterviewQuestion(item: InterviewQuestion)

    @Delete
    suspend fun deleteInterviewQuestion(item: InterviewQuestion)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExperience(item: CareerExperience): Long

    @Query("SELECT * FROM career_experiences ORDER BY startDate DESC")
    fun getAllExperiences(): Flow<List<CareerExperience>>

    @Query("SELECT * FROM career_experiences WHERE id = :id")
    suspend fun getExperienceById(id: Long): CareerExperience?

    @Update
    suspend fun updateExperience(item: CareerExperience)

    @Delete
    suspend fun deleteExperience(item: CareerExperience)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEducation(item: Education): Long

    @Query("SELECT * FROM educations ORDER BY startDate DESC")
    fun getAllEducations(): Flow<List<Education>>

    @Query("SELECT * FROM educations WHERE id = :id")
    suspend fun getEducationById(id: Long): Education?

    @Update
    suspend fun updateEducation(item: Education)

    @Delete
    suspend fun deleteEducation(item: Education)

    @Query("DELETE FROM cover_letters") suspend fun deleteAllCoverLetters()
    @Query("DELETE FROM resumes") suspend fun deleteAllResumes()
    @Query("DELETE FROM portfolios") suspend fun deleteAllPortfolios()
    @Query("DELETE FROM projects") suspend fun deleteAllProjects()
    @Query("DELETE FROM certifications") suspend fun deleteAllCertifications()
    @Query("DELETE FROM interview_questions") suspend fun deleteAllInterviewQuestions()
    @Query("DELETE FROM career_experiences") suspend fun deleteAllExperiences()
    @Query("DELETE FROM educations") suspend fun deleteAllEducations()

    @Transaction
    suspend fun clearAll() {
        deleteAllCoverLetters()
        deleteAllResumes()
        deleteAllPortfolios()
        deleteAllProjects()
        deleteAllCertifications()
        deleteAllInterviewQuestions()
        deleteAllExperiences()
        deleteAllEducations()
    }
}
