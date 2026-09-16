package com.thirtytwo_cereernote.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.thirtytwo_cereernote.MainActivity
import com.thirtytwo_cereernote.R

class NotificationHelper(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "커리어 일정 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "지원 마감 및 면접 일정 알림을 제공합니다."
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleNotification(id: Long, type: String, title: String, content: String, time: Long, extraId: Long = -1L) {
        if (time < System.currentTimeMillis()) return

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("id", id)
            putExtra("type", type)
            putExtra("title", title)
            putExtra("content", content)
            putExtra("extraId", extraId)
        }
        
        val requestCode = (type.hashCode() xor id.toInt())
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, time, pendingIntent)
        }
    }

    fun cancelNotification(id: Long, type: String) {
        val intent = Intent(context, NotificationReceiver::class.java)
        val requestCode = (type.hashCode() xor id.toInt())
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
        }
    }

    companion object {
        const val CHANNEL_ID = "career_notifications"
        const val TYPE_APPLICATION = "application"
        const val TYPE_INTERVIEW = "interview"
    }
}

class NotificationReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("id", 0L)
        val type = intent.getStringExtra("type") ?: NotificationHelper.TYPE_APPLICATION
        val title = intent.getStringExtra("title") ?: "커리어 일정 알림"
        val content = intent.getStringExtra("content") ?: "일정을 확인하세요."
        val extraId = intent.getLongExtra("extraId", -1L)

        val notificationIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("id", id)
            putExtra("type", type)
            putExtra("extraId", extraId)
        }
        
        val requestCode = (type.hashCode() xor id.toInt())
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (status != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        notificationManager.notify(requestCode, notification)
    }
}
