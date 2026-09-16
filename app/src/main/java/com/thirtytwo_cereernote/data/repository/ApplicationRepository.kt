package com.thirtytwo_cereernote.data.repository

import com.thirtytwo_cereernote.data.database.ApplicationDao
import com.thirtytwo_cereernote.data.model.Application
import com.thirtytwo_cereernote.data.model.ApplicationStatus
import com.thirtytwo_cereernote.data.model.ApplicationStatusHistory
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApplicationRepository @Inject constructor(
    private val applicationDao: ApplicationDao
) {
    val allApplications: Flow<List<Application>> = applicationDao.getAllApplications()

    suspend fun getApplicationById(id: Long): Application? = applicationDao.getApplicationById(id)

    suspend fun insertApplication(application: Application): Long = applicationDao.insertApplicationWithHistory(application)

    suspend fun updateApplication(application: Application) = applicationDao.updateApplication(application)

    suspend fun deleteApplication(application: Application) = applicationDao.deleteApplication(application)

    fun getStatusHistories(applicationId: Long): Flow<List<ApplicationStatusHistory>> = 
        applicationDao.getStatusHistories(applicationId)

    fun getAllStatusHistories(): Flow<List<ApplicationStatusHistory>> = 
        applicationDao.getAllStatusHistories()

    suspend fun updateStatus(applicationId: Long, newStatus: ApplicationStatus, memo: String = "") = 
        applicationDao.updateStatusWithHistory(applicationId, newStatus, memo)

    suspend fun insertStatusHistory(history: ApplicationStatusHistory) =
        applicationDao.insertStatusHistory(history)

    suspend fun updateFavorite(id: Long, isFavorite: Boolean) = 
        applicationDao.updateFavorite(id, isFavorite, java.util.Date())

    fun getInterviewsByApplicationId(applicationId: Long) = applicationDao.getInterviewsByApplicationId(applicationId)
    suspend fun insertInterview(interview: com.thirtytwo_cereernote.data.model.Interview) = applicationDao.insertInterview(interview)
    suspend fun updateInterview(interview: com.thirtytwo_cereernote.data.model.Interview) = applicationDao.updateInterview(interview)
    suspend fun deleteInterview(interview: com.thirtytwo_cereernote.data.model.Interview) = applicationDao.deleteInterview(interview)
}
