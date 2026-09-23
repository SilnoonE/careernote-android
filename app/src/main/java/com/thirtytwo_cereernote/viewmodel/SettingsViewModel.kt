package com.thirtytwo_cereernote.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.thirtytwo_cereernote.data.database.AppDatabase
import com.thirtytwo_cereernote.data.model.PortfolioSnapshot
import com.thirtytwo_cereernote.data.model.ResumeSnapshot
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: PreferenceRepository,
    private val database: AppDatabase
) : ViewModel() {

    private var isBackupInProgress = false
    private var isRestoreInProgress = false

    init {
        checkRestoreMarker()
    }

    private fun checkRestoreMarker() {
        val markerFile = File(context.filesDir, "restore_in_progress")
        if (markerFile.exists()) {
            viewModelScope.launch(Dispatchers.IO) {
                val backupStoreDir = File(context.filesDir, "original_backup")
                if (backupStoreDir.exists()) {
                    try {
                        val currentDbFile = context.getDatabasePath("careernote_db")
                        val currentDbDir = currentDbFile.parentFile
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
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                markerFile.delete()
            }
        }
    }

    val language: StateFlow<String> = repository.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "ko")

    val theme: StateFlow<String> = repository.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "light")

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
        if (isBackupInProgress || isRestoreInProgress) {
            onComplete(false)
            return
        }
        isBackupInProgress = true

        viewModelScope.launch(Dispatchers.IO) {
            var tempDir: File? = null
            var zipFile: File? = null
            var isSuccess = false

            try {
                // 1. Force WAL Checkpoint safely
                try {
                    database.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                tempDir = File(context.cacheDir, "backup_work_${System.currentTimeMillis()}")
                tempDir.mkdirs()

                // Copy DB files
                val dbDir = File(tempDir, "databases")
                dbDir.mkdirs()
                val dbFile = context.getDatabasePath("careernote_db")
                if (dbFile.exists()) {
                    dbFile.copyTo(File(dbDir, dbFile.name), true)
                    File(dbFile.path + "-wal").let { if (it.exists()) it.copyTo(File(dbDir, it.name), true) }
                    File(dbFile.path + "-shm").let { if (it.exists()) it.copyTo(File(dbDir, it.name), true) }
                }

                // Copy PDF & snapshot files
                val filesDir = File(tempDir, "files")
                filesDir.mkdirs()

                val apps = try {
                    database.applicationDao().getAllApplicationsList()
                } catch (_: Exception) {
                    emptyList()
                }

                val snapshotFileNames = mutableSetOf<String>()
                apps.forEach { app ->
                    app.attachedPdfPath?.let { File(it).name.takeIf { name -> name.isNotBlank() }?.let { name -> snapshotFileNames.add(name) } }
                    app.resumeSnapshot?.let { json ->
                        try {
                            val snap = Json.decodeFromString<ResumeSnapshot>(json)
                            if (snap.filePath.isNotBlank()) snapshotFileNames.add(File(snap.filePath).name)
                        } catch (_: Exception) {}
                    }
                    app.portfolioSnapshot?.let { json ->
                        try {
                            val snap = Json.decodeFromString<PortfolioSnapshot>(json)
                            if (snap.filePath.isNotBlank()) snapshotFileNames.add(File(snap.filePath).name)
                        } catch (_: Exception) {}
                    }
                }

                context.filesDir.listFiles()?.forEach { file ->
                    val name = file.name
                    if (file.isFile && !file.isDirectory) {
                        val isSubmittedPdf = name.startsWith("submitted_pdf_")
                        val isResumeSnap = name.startsWith("resume_snapshot_")
                        val isPortfolioSnap = name.startsWith("portfolio_snapshot_")
                        val isReferencedInDb = snapshotFileNames.contains(name)

                        if (isSubmittedPdf || isResumeSnap || isPortfolioSnap || isReferencedInDb) {
                            try {
                                file.copyTo(File(filesDir, file.name), true)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }

                // Create Manifest
                val manifestFile = File(tempDir, "manifest.json")
                val fileEntries = mutableListOf<String>()
                tempDir.walkTopDown().forEach { f ->
                    if (f.isFile && f.name != "manifest.json") {
                        val relPath = f.relativeTo(tempDir).path.replace('\\', '/')
                        fileEntries.add("""{"path":"$relPath","size":${f.length()}}""")
                    }
                }
                manifestFile.writeText("""{"version":2,"timestamp":${System.currentTimeMillis()},"files":[${fileEntries.joinToString(",")}]}""")

                // Zip to temp cache file
                zipFile = File(context.cacheDir, "careernote_backup_${System.currentTimeMillis()}.zip")
                StorageUtils.zipFolder(tempDir, zipFile)

                // Write to SAF OutputStream
                context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                    zipFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                        outputStream.flush()
                    }
                } ?: throw IOException("출력 스트림을 생성할 수 없습니다.")

                isSuccess = true
            } catch (e: Exception) {
                e.printStackTrace()
                isSuccess = false
            } finally {
                try {
                    tempDir?.deleteRecursively()
                    zipFile?.delete()
                } catch (_: Exception) {}

                isBackupInProgress = false
                withContext(Dispatchers.Main) { onComplete(isSuccess) }
            }
        }
    }

    fun restoreData(uri: android.net.Uri, onComplete: (String?) -> Unit) {
        if (isBackupInProgress || isRestoreInProgress) {
            onComplete("백업 또는 복원이 진행 중입니다.")
            return
        }
        isRestoreInProgress = true

        viewModelScope.launch(Dispatchers.IO) {
            val restoreWorkDir = File(context.filesDir, "restore_work")
            val backupStoreDir = File(context.filesDir, "original_backup")

            try {
                restoreWorkDir.deleteRecursively()
                restoreWorkDir.mkdirs()

                // 1. Copy ZIP and unzip
                val tempZip = File(restoreWorkDir, "restore.zip")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempZip.outputStream().use { output -> input.copyTo(output) }
                } ?: throw IOException("파일을 읽을 수 없습니다.")

                val extractDir = File(restoreWorkDir, "extracted")
                extractDir.mkdirs()
                StorageUtils.unzip(tempZip, extractDir)

                val restoredDbFile = File(extractDir, "databases/careernote_db")
                if (!restoredDbFile.exists()) throw IOException("백업 파일에 데이터베이스가 없습니다.")

                // 2. Integrity & Migration Validation
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
                        .addMigrations(
                            AppDatabase.MIGRATION_3_4,
                            AppDatabase.MIGRATION_4_5,
                            AppDatabase.MIGRATION_5_6,
                            AppDatabase.MIGRATION_6_7
                        )
                        .allowMainThreadQueries()
                        .build()
                    tempDb.query("SELECT 1", null).close()
                    tempDb.close()
                } catch (e: Exception) {
                    throw IOException("데이터베이스 호환성 검증 실패: ${e.message}")
                }

                // Rewrite file paths in restored DB to current filesDir
                android.database.sqlite.SQLiteDatabase.openDatabase(restoredDbFile.absolutePath, null, android.database.sqlite.SQLiteDatabase.OPEN_READWRITE).use { db ->
                    val targetDirStr = context.filesDir.absolutePath + File.separator

                    // Update attachedPdfPath
                    db.execSQL(
                        "UPDATE applications SET attachedPdfPath = ? || substr(attachedPdfPath, instr(attachedPdfPath, 'submitted_pdf_')) WHERE attachedPdfPath IS NOT NULL AND instr(attachedPdfPath, 'submitted_pdf_') > 0",
                        arrayOf(targetDirStr)
                    )

                    // Update resumeSnapshot JSON
                    db.rawQuery("SELECT id, resumeSnapshot FROM applications WHERE resumeSnapshot IS NOT NULL", null).use { cursor ->
                        while (cursor.moveToNext()) {
                            val appId = cursor.getLong(0)
                            val json = cursor.getString(1)
                            try {
                                val snap = Json.decodeFromString<ResumeSnapshot>(json)
                                if (snap.filePath.isNotBlank()) {
                                    val fileName = File(snap.filePath).name
                                    val newSnap = snap.copy(filePath = File(context.filesDir, fileName).absolutePath)
                                    val newJson = Json.encodeToString(newSnap)
                                    db.execSQL("UPDATE applications SET resumeSnapshot = ? WHERE id = ?", arrayOf<Any>(newJson, appId))
                                }
                            } catch (_: Exception) {}
                        }
                    }

                    // Update portfolioSnapshot JSON
                    db.rawQuery("SELECT id, portfolioSnapshot FROM applications WHERE portfolioSnapshot IS NOT NULL", null).use { cursor ->
                        while (cursor.moveToNext()) {
                            val appId = cursor.getLong(0)
                            val json = cursor.getString(1)
                            try {
                                val snap = Json.decodeFromString<PortfolioSnapshot>(json)
                                if (snap.filePath.isNotBlank()) {
                                    val fileName = File(snap.filePath).name
                                    val newSnap = snap.copy(filePath = File(context.filesDir, fileName).absolutePath)
                                    val newJson = Json.encodeToString(newSnap)
                                    db.execSQL("UPDATE applications SET portfolioSnapshot = ? WHERE id = ?", arrayOf<Any>(newJson, appId))
                                }
                            } catch (_: Exception) {}
                        }
                    }
                }

                // 3. Backup current state for Rollback
                backupStoreDir.deleteRecursively()
                backupStoreDir.mkdirs()

                val currentDbFile = context.getDatabasePath("careernote_db")
                val currentDbDir = currentDbFile.parentFile
                if (currentDbDir != null && currentDbDir.exists()) {
                    currentDbDir.copyRecursively(File(backupStoreDir, "databases"), true)
                }

                val backupFilesDir = File(backupStoreDir, "files")
                backupFilesDir.mkdirs()
                context.filesDir.listFiles()?.forEach { file ->
                    if (file.name.startsWith("submitted_pdf_") || file.name.startsWith("resume_snapshot_") || file.name.startsWith("portfolio_snapshot_")) {
                        if (file.isDirectory) file.copyRecursively(File(backupFilesDir, file.name), true)
                        else file.copyTo(File(backupFilesDir, file.name), true)
                    }
                }

                val markerFile = File(context.filesDir, "restore_in_progress")
                markerFile.createNewFile()

                // 4. Perform Replacement
                try {
                    if (currentDbDir != null) {
                        currentDbDir.listFiles()?.forEach { it.delete() }
                        File(extractDir, "databases").listFiles()?.forEach { it.copyTo(File(currentDbDir, it.name), true) }
                    }

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

                markerFile.delete()
                backupStoreDir.deleteRecursively()
                restoreWorkDir.deleteRecursively()

                withContext(Dispatchers.Main) {
                    onComplete(null)
                    restartApp(context)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onComplete(e.message ?: "복원 중 오류 발생")
                }
            } finally {
                isRestoreInProgress = false
            }
        }
    }

    private fun restartApp(context: Context) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            if (intent != null) {
                val restartIntent = Intent.makeRestartActivityTask(intent.component)
                context.startActivity(restartIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearAllData(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val apps = try { database.applicationDao().getAllApplicationsList() } catch (_: Exception) { emptyList() }
                val notificationHelper = com.thirtytwo_cereernote.util.NotificationHelper(context)
                apps.forEach { app: com.thirtytwo_cereernote.data.model.Application ->
                    try {
                        notificationHelper.cancelNotification(app.id, com.thirtytwo_cereernote.util.NotificationHelper.TYPE_APPLICATION)
                        val interviews = database.applicationDao().getInterviewsByApplicationId(app.id).first()
                        interviews.forEach { interview: com.thirtytwo_cereernote.data.model.Interview ->
                            notificationHelper.cancelNotification(interview.id, com.thirtytwo_cereernote.util.NotificationHelper.TYPE_INTERVIEW)
                        }
                    } catch (_: Exception) {}
                }

                database.applicationDao().clearAll()
                database.careerDao().clearAll()
                database.clearAllTables()

                repository.clearAllPreferences()

                context.filesDir.listFiles()?.forEach { file ->
                    if (file.name != "datastore") {
                        if (file.isDirectory) file.deleteRecursively() else file.delete()
                    }
                }
                context.cacheDir.listFiles()?.forEach { file ->
                    if (file.isDirectory) file.deleteRecursively() else file.delete()
                }

                withContext(Dispatchers.Main) {
                    onComplete(true)
                    restartApp(context)
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
