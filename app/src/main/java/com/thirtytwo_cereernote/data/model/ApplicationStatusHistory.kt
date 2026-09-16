package com.thirtytwo_cereernote.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "application_status_histories",
    foreignKeys = [
        ForeignKey(
            entity = Application::class,
            parentColumns = ["id"],
            childColumns = ["applicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["applicationId"])]
)
data class ApplicationStatusHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val applicationId: Long,
    val status: ApplicationStatus,
    val changedAt: Date = Date(),
    val memo: String = ""
)
