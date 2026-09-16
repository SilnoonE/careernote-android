package com.thirtytwo_cereernote.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object CommonUtils {
    fun openUrl(context: Context, url: String) {
        if (url.isBlank()) return
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "URL을 열 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    fun openFile(context: Context, path: String) {
        val file = File(path)
        if (!file.exists()) {
            Toast.makeText(context, "파일이 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "PDF 뷰어가 설치되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}
