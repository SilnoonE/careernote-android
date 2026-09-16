package com.thirtytwo_cereernote.data.database

import androidx.room.*
import com.thirtytwo_cereernote.data.model.Application
import com.thirtytwo_cereernote.data.model.ApplicationStatusHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface ApplicationDao {
    @Query("SELECT * FROM applications ORDER BY appliedDate DESC")
    fun getAllApplications(): Flow<List<Application>>

    @Query("SELECT * FROM applications WHERE id = :id")
    suspend fun getApplicationById(id: Long): Application?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertApplication(application: Application): Long

    @Update
    suspend fun updateApplication(application: Application)

    @Query("UPDATE applications SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean, updatedAt: java.util.Date)

    @Delete
    suspend fun deleteApplication(application: Application)

    @Query("SELECT * FROM application_status_histories WHERE applicationId = :applicationId ORDER BY changedAt DESC")
    fun getStatusHistories(applicationId: Long): Flow<List<ApplicationStatusHistory>>

    @Query("SELECT * FROM application_status_histories")
    fun getAllStatusHistories(): Flow<List<ApplicationStatusHistory>>

    @Insert
    suspend fun insertStatusHistory(history: ApplicationStatusHistory)

    @Transaction
    suspend fun insertApplicationWithHistory(application: Application): Long {
        val id = insertApplication(application)
        if (id > 0) {
            insertStatusHistory(ApplicationStatusHistory(
                applicationId = id,
                status = application.currentStatus,
                memo = "최초 등록",
                changedAt = application.createdAt
            ))
        }
        return id
    }

    @Transaction
    suspend fun updateStatusWithHistory(applicationId: Long, newStatus: com.thirtytwo_cereernote.data.model.ApplicationStatus, memo: String = "") {
        val application = getApplicationById(applicationId) ?: return
        
        // Prevent duplicate history for the same status if it's the current one
        if (application.currentStatus == newStatus) return

        var submittedDate = application.submittedDate
        // Only set submittedDate for meaningful transitions to "applied" or further
        if (submittedDate == null && newStatus.isPassedDocument()) { // Simplification: any "passed doc" implies it was submitted
            // But we should be more specific. If it's APPLY_COMPLETED or later, it's submitted.
            // Let's use a helper from enum if possible or just check here.
        }
        
        // Refined submission logic
        val isSubmissionStatus = newStatus != com.thirtytwo_cereernote.data.model.ApplicationStatus.INTERESTED && 
                                 newStatus != com.thirtytwo_cereernote.data.model.ApplicationStatus.APPLY_PLANNED &&
                                 newStatus != com.thirtytwo_cereernote.data.model.ApplicationStatus.CANCELLED
                                 
        if (submittedDate == null && isSubmissionStatus) {
            submittedDate = java.util.Date()
        }

        updateApplication(application.copy(
            currentStatus = newStatus, 
            submittedDate = submittedDate,
            updatedAt = java.util.Date()
        ))
        insertStatusHistory(ApplicationStatusHistory(applicationId = applicationId, status = newStatus, memo = memo))
    }

    @Query("DELETE FROM applications")
    suspend fun deleteAllApplications()

    @Query("DELETE FROM application_status_histories")
    suspend fun deleteAllStatusHistories()

    // Interview CRUD
    @Query("SELECT * FROM interviews WHERE applicationId = :applicationId ORDER BY interviewDate ASC")
    fun getInterviewsByApplicationId(applicationId: Long): Flow<List<com.thirtytwo_cereernote.data.model.Interview>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInterview(interview: com.thirtytwo_cereernote.data.model.Interview): Long

    @Update
    suspend fun updateInterview(interview: com.thirtytwo_cereernote.data.model.Interview)

    @Delete
    suspend fun deleteInterview(interview: com.thirtytwo_cereernote.data.model.Interview)

    @Transaction
    suspend fun clearAll() {
        deleteAllStatusHistories()
        deleteAllApplications()
    }
}
