package com.thirtytwo_cereernote

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.*
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.thirtytwo_cereernote.ui.screen.MainContainer
import com.thirtytwo_cereernote.ui.screen.theme.CareerNoteTheme
import com.thirtytwo_cereernote.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val id = intent.getLongExtra("id", -1L)
        val type = intent.getStringExtra("type")
        val extraId = intent.getLongExtra("extraId", -1L)

        // Clear extras to prevent re-handling on config change
        intent.removeExtra("id")
        intent.removeExtra("type")
        intent.removeExtra("extraId")

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val language by settingsViewModel.language.collectAsState()
            val theme by settingsViewModel.theme.collectAsState()

            LaunchedEffect(language) {
                if (language.isNotEmpty()) {
                    val appLocales = LocaleListCompat.forLanguageTags(language)
                    if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != appLocales.toLanguageTags()) {
                        AppCompatDelegate.setApplicationLocales(appLocales)
                    }
                }
            }

            val darkTheme = theme == "dark"

            key(language, theme) {
                CareerNoteTheme(darkTheme = darkTheme) {
                    MainContainer(initialId = id, initialType = type, extraId = extraId)
                }
            }
        }
    }
}
