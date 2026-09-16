package com.thirtytwo_cereernote.util

import android.content.Context
import com.thirtytwo_cereernote.data.model.ApplicationStatus
import com.thirtytwo_cereernote.data.repository.ApplicationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    private val repository: ApplicationRepository
) {
    fun rescheduleAll(context: Context) {
        val notificationHelper = NotificationHelper(context)
        notificationHelper.createNotificationChannel()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apps = repository.allApplications.first()
                val now = Date()
                apps.forEach { app ->
                    // 1. Reschedule application deadlines (1 day before)
                    // Only for active statuses
                    if (!app.currentStatus.isFinished()) {
                        app.deadlineDate?.let { deadline ->
                            val triggerTime = deadline.time - (1000 * 60 * 60 * 24)
                            if (triggerTime > now.time) {
                                notificationHelper.scheduleNotification(
                                    app.id,
                                    NotificationHelper.TYPE_APPLICATION,
                                    "마감 임박: ${app.companyName}",
                                    "${app.jobTitle} 지원 마감이 다가옵니다.",
                                    triggerTime
                                )
                            }
                        }
                    }
                    
                    // 2. Reschedule interviews (2 hours before)
                    val interviews = repository.getInterviewsByApplicationId(app.id).first()
                    interviews.forEach { interview ->
                        val triggerTime = interview.interviewDate.time - (2 * 60 * 60 * 1000)
                        if (triggerTime > now.time) {
                            notificationHelper.scheduleNotification(
                                interview.id,
                                NotificationHelper.TYPE_INTERVIEW,
                                "면접 일정 알림: ${interview.stage}",
                                "오늘 ${interview.stage} 일정이 있습니다. 장소: ${interview.location}",
                                triggerTime,
                                extraId = app.id
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
