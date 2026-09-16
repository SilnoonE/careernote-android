package com.thirtytwo_cereernote

import android.app.Application

import com.thirtytwo_cereernote.util.NotificationScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CareerNoteApp : Application() {
    
    @Inject
    lateinit var scheduler: NotificationScheduler

    override fun onCreate() {
        super.onCreate()
        // Production advertising integration is excluded from this public snapshot.
        scheduler.rescheduleAll(this)
    }
}
