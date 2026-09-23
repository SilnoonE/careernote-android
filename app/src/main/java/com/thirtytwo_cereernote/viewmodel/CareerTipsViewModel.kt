package com.thirtytwo_cereernote.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thirtytwo_cereernote.R
import com.thirtytwo_cereernote.data.model.CareerTip
import com.thirtytwo_cereernote.data.model.TipCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CareerTipsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _tips = MutableStateFlow<List<CareerTip>>(emptyList())
    val tips: StateFlow<List<CareerTip>> = _tips.asStateFlow()

    init {
        loadTips()
    }

    private fun loadTips() {
        viewModelScope.launch {
            try {
                // AppCompatDelegate를 통해 현재 설정된 언어 확인
                val appLocales = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()
                val locale = if (appLocales.isEmpty) {
                    context.resources.configuration.locales[0].language
                } else {
                    appLocales.get(0)?.language ?: "ko"
                }

                val resId = when (locale) {
                    "ko" -> R.raw.career_tips_ko
                    "en" -> R.raw.career_tips_en
                    "ja" -> R.raw.career_tips_ja
                    else -> R.raw.career_tips_ko
                }

                val jsonString = context.resources.openRawResource(resId).bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(jsonString)
                val list = mutableListOf<CareerTip>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val categoryStr = obj.optString("category")
                    val category = try {
                        TipCategory.valueOf(categoryStr)
                    } catch (e: Exception) {
                        TipCategory.MARKET
                    }

                    list.add(
                        CareerTip(
                            id = obj.optString("id", i.toString()),
                            category = category,
                            title = obj.optString("title", ""),
                            subtitle = if (obj.has("subtitle")) obj.optString("subtitle") else null,
                            body = obj.optString("body", ""),
                            badge = if (obj.has("badge")) obj.optString("badge") else null,
                            source = if (obj.has("source")) obj.optString("source") else null,
                            referenceDate = if (obj.has("referenceDate")) obj.optString("referenceDate") else null,
                            order = obj.optInt("order", 0)
                        )
                    )
                }
                _tips.value = list.sortedBy { it.order }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
