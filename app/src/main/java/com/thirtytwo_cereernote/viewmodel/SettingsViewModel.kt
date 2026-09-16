package com.thirtytwo_cereernote.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Process
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.thirtytwo_cereernote.data.database.AppDatabase
import com.thirtytwo_cereernote.data.repository.PreferenceRepository
import com.thirtytwo_cereernote.util.StorageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: PreferenceRepository,
    private val database: AppDatabase
) : ViewModel() {

    init {
        checkRestoreMarker()
    }

    private fun checkRestoreMarker() {
        val markerFile = File(context.filesDir, "restore_in_progress")
        if (markerFile.exists()) {
            // Restore was interrupted. Rollback if possible.
            viewModelScope.launch(Dispatchers.IO) {
                val backupStoreDir = File(context.filesDir, "original_backup")
                if (backupStoreDir.exists()) {
                    // Perform rollback logic here
                }
                markerFile.delete()
            }
        }
    }

    val language: StateFlow<String> = repository.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "ko")

    val theme: StateFlow<String> = repository.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    fun setLanguage(langCode: String) {
        viewModelScope.launch {
            repository.setLanguage(langCode)
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            repository.setTheme(theme)
        }
    }

    fun backupData(uri: android.net.Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Force Checkpoint
                try {
                    database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)", emptyArray()).use { cursor ->
                        if (cursor.moveToFirst()) {
                            val isBusy = cursor.getInt(0) != 0
                            if (isBusy) {
                                // Log or handle busy state if needed
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val tempDir = File(context.cacheDir, "backup_work_${System.currentTimeMillis()}")
                tempDir.mkdirs()

                // Copy DB files
                val dbDir = File(tempDir, "databases")
                dbDir.mkdirs()
                val dbFile = context.getDatabasePath("careernote_db")
                if (dbFile.exists()) {
                    dbFile.copyTo(File(dbDir, dbFile.name), true)
                    // Copy WAL/SHM if they still exist (checkpoint 0 doesn't guarantee deletion)
                    File(dbFile.path + "-wal").let { if (it.exists()) it.copyTo(File(dbDir, it.name), true) }
                    File(dbFile.path + "-shm").let { if (it.exists()) it.copyTo(File(dbDir, it.name), true) }
                }

                // Copy relevant files (PDFs and DataStore)
                val filesDir = File(tempDir, "files")
                filesDir.mkdirs()
                
                // Copy only what we need: submitted PDFs and datastore
                context.filesDir.listFiles()?.forEach { file ->
                    if (file.name.startsWith("submitted_pdf_") || file.name == "datastore") {
                        if (file.isDirectory) file.copyRecursively(File(filesDir, file.name), true)
                        else file.copyTo(File(filesDir, file.name), true)
                    }
                }

                // Zip everything
                val zipFile = File(context.cacheDir, "careernote_backup.zip")
                StorageUtils.zipFolder(tempDir, zipFile)

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    zipFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: throw IOException("출력 스트림을 생성할 수 없습니다.")
                
                tempDir.deleteRecursively()
                zipFile.delete()

                withContext(Dispatchers.Main) { onComplete(true) }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onComplete(false) }
            }
        }
    }

    fun restoreData(uri: android.net.Uri, onComplete: (String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val restoreWorkDir = File(context.filesDir, "restore_work")
            val backupStoreDir = File(context.filesDir, "original_backup")
            
            try {
                restoreWorkDir.deleteRecursively()
                restoreWorkDir.mkdirs()

                // 1. Copy ZIP to work dir and unzip
                val tempZip = File(restoreWorkDir, "restore.zip")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempZip.outputStream().use { output -> input.copyTo(output) }
                } ?: throw IOException("파일을 읽을 수 없습니다.")

                val extractDir = File(restoreWorkDir, "extracted")
                extractDir.mkdirs()
                StorageUtils.unzip(tempZip, extractDir)

                val restoredDbFile = File(extractDir, "databases/careernote_db")
                if (!restoredDbFile.exists()) throw IOException("백업 파일에 데이터베이스가 없습니다.")

                // 2. Validate restored data
                android.database.sqlite.SQLiteDatabase.openDatabase(restoredDbFile.absolutePath, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY).use { db ->
                    db.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                        if (!cursor.moveToFirst() || !cursor.getString(0).equals("ok", true)) {
                            throw IOException("데이터베이스 손상됨")
                        }
                    }
                    if (db.version > AppDatabase.VERSION) {
                        throw IOException("백업 파일 버전(${db.version})이 현재 앱 버전(${AppDatabase.VERSION})보다 높음")
                    }
                }

                // Room/Migration Validation
                try {
                    val tempDb = Room.databaseBuilder(context, AppDatabase::class.java, restoredDbFile.absolutePath)
                        .addMigrations(AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
                        .allowMainThreadQueries()
                        .build()
                    tempDb.query("SELECT 1", null).close()
                    tempDb.close()
                } catch (e: Exception) {
                    throw IOException("데이터베이스 호환성 검증 실패: ${e.message}")
                }

                // 3. Prepare for Atomic-ish replacement
                // Create backup of current data in persistent storage
                backupStoreDir.deleteRecursively()
                backupStoreDir.mkdirs()
                
                database.close()
                
                val currentDbFile = context.getDatabasePath("careernote_db")
                val currentDbDir = currentDbFile.parentFile
                if (currentDbDir != null && currentDbDir.exists()) {
                    currentDbDir.copyRecursively(File(backupStoreDir, "databases"), true)
                }
                
                val backupFilesDir = File(backupStoreDir, "files")
                backupFilesDir.mkdirs()
                context.filesDir.listFiles()?.forEach { file ->
                    if (file.name.startsWith("submitted_pdf_") || file.name == "datastore") {
                        if (file.isDirectory) file.copyRecursively(File(backupFilesDir, file.name), true)
                        else file.copyTo(File(backupFilesDir, file.name), true)
                    }
                }

                // Create a marker that we are starting replacement
                val markerFile = File(context.filesDir, "restore_in_progress")
                markerFile.createNewFile()

                // 4. Perform replacement
                try {
                    // Delete current targeted files
                    if (currentDbDir != null) {
                        currentDbDir.listFiles()?.forEach { it.delete() }
                        File(extractDir, "databases").listFiles()?.forEach { it.copyTo(File(currentDbDir, it.name), true) }
                    }

                    // Replace files (PDFs and datastore)
                    val restoredFilesDir = File(extractDir, "files")
                    if (restoredFilesDir.exists()) {
                        restoredFilesDir.listFiles()?.forEach { restoredFile ->
                            val targetFile = File(context.filesDir, restoredFile.name)
                            if (targetFile.exists()) {
                                if (targetFile.isDirectory) targetFile.deleteRecursively() else targetFile.delete()
                            }
                            if (restoredFile.isDirectory) restoredFile.copyRecursively(targetFile, true)
                            else restoredFile.copyTo(targetFile, true)
                        }
                    }
                    
                    // PDF path fix-up in DB
                    android.database.sqlite.SQLiteDatabase.openDatabase(currentDbFile.absolutePath, null, android.database.sqlite.SQLiteDatabase.OPEN_READWRITE).use { db ->
                        db.execSQL(
                            "UPDATE applications SET attachedPdfPath = ? || substr(attachedPdfPath, instr(attachedPdfPath, 'submitted_pdf_')) WHERE attachedPdfPath IS NOT NULL",
                            arrayOf(context.filesDir.absolutePath + File.separator)
                        )
                    }

                } catch (e: Exception) {
                    // Rollback
                    if (currentDbDir != null) {
                        currentDbDir.listFiles()?.forEach { it.delete() }
                        File(backupStoreDir, "databases").listFiles()?.forEach { it.copyTo(File(currentDbDir, it.name), true) }
                    }
                    File(backupStoreDir, "files").listFiles()?.forEach { restoredFile ->
                        val targetFile = File(context.filesDir, restoredFile.name)
                        if (targetFile.exists()) {
                            if (targetFile.isDirectory) targetFile.deleteRecursively() else targetFile.delete()
                        }
                        if (restoredFile.isDirectory) restoredFile.copyRecursively(targetFile, true)
                        else restoredFile.copyTo(targetFile, true)
                    }
                    throw e
                }

                // Success
                markerFile.delete()
                backupStoreDir.deleteRecursively()
                restoreWorkDir.deleteRecursively()

                withContext(Dispatchers.Main) {
                    onComplete(null)
                    restartApp()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onComplete(e.message ?: "복원 중 오류 발생")
                }
            }
        }
    }

    private fun restartApp() {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        Process.killProcess(Process.myPid())
    }

    fun clearAllData(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Cancel all notifications first
                val apps = database.applicationDao().getAllApplications().first()
                val notificationHelper = com.thirtytwo_cereernote.util.NotificationHelper(context)
                apps.forEach { app: com.thirtytwo_cereernote.data.model.Application ->
                    notificationHelper.cancelNotification(app.id, com.thirtytwo_cereernote.util.NotificationHelper.TYPE_APPLICATION)
                    val interviews = database.applicationDao().getInterviewsByApplicationId(app.id).first()
                    interviews.forEach { interview: com.thirtytwo_cereernote.data.model.Interview ->
                        notificationHelper.cancelNotification(interview.id, com.thirtytwo_cereernote.util.NotificationHelper.TYPE_INTERVIEW)
                    }
                }

                database.clearAllTables()
                context.filesDir.listFiles()?.forEach { 
                    if (it.name.startsWith("submitted_pdf_")) it.delete()
                }
                withContext(Dispatchers.Main) {
                    onComplete(true)
                    restartApp()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onComplete(false)
                }
            }
        }
    }
}
