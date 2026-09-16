package com.thirtytwo_cereernote.util

import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object StorageUtils {
    fun zipFolder(sourceFolder: File, zipFile: File) {
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            sourceFolder.listFiles()?.forEach { file ->
                zipFileOrDirectory("", file, zos)
            }
        }
    }

    private fun zipFileOrDirectory(path: String, file: File, zos: ZipOutputStream) {
        val entryName = if (path.isEmpty()) file.name else "$path/${file.name}"
        if (file.isDirectory) {
            val files = file.listFiles() ?: return
            if (files.isEmpty()) {
                zos.putNextEntry(ZipEntry("$entryName/"))
                zos.closeEntry()
            } else {
                for (f in files) {
                    zipFileOrDirectory(entryName, f, zos)
                }
            }
        } else {
            val buf = ByteArray(4096)
            FileInputStream(file).use { fis ->
                zos.putNextEntry(ZipEntry(entryName))
                var len: Int
                while (fis.read(buf).also { len = it } > 0) {
                    zos.write(buf, 0, len)
                }
                zos.closeEntry()
            }
        }
    }

    fun unzip(zipFile: File, targetDirectory: File) {
        val maxFiles = 1000
        val maxTotalSize = 100 * 1024 * 1024 // 100MB
        var fileCount = 0
        var totalSize = 0L

        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                fileCount++
                if (fileCount > maxFiles) throw IOException("압축 파일 내 파일 개수가 너무 많습니다.")

                val newFile = File(targetDirectory, entry.name)
                
                // Path Traversal check (Zip Slip)
                val canonicalPath = newFile.canonicalPath
                if (!canonicalPath.startsWith(targetDirectory.canonicalPath + File.separator)) {
                    throw IOException("정상적이지 않은 압축 항목 경로: ${entry.name}")
                }

                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { fos ->
                        val buffer = ByteArray(4096)
                        var len: Int
                        while (zis.read(buffer).also { len = it } > 0) {
                            totalSize += len
                            if (totalSize > maxTotalSize) throw IOException("압축 해제 용량이 제한을 초과했습니다.")
                            fos.write(buffer, 0, len)
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}
