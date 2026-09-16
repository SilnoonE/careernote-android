package com.thirtytwo_cereernote.data.model

enum class TipCategory {
    MARKET,
    JOB_GUIDE,
    INTERVIEW,
    CAREER_CHANGE
}

data class CareerTip(
    val id: String,
    val category: TipCategory,
    val title: String,
    val subtitle: String? = null,
    val body: String,
    val badge: String? = null,
    val source: String? = null,
    val referenceDate: String? = null,
    val order: Int = 0
)
