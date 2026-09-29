package com.example.appadslib

import com.mobi.libraryads.data.LanguageModel


enum class EnumSelectLanguage(val id: Int, val nameLanguage: Int, val flag: Int, val code: String) {
    HINDI(4, R.string.language_hindi, R.drawable.language_ic_india, "hi"),

    FRENCH(7, R.string.language_french, R.drawable.language_ic_france, "fr"),
    UNITED_STATES(1, R.string.language_english, R.drawable.language_ic_english, "en"),
    FILIPINO(8, R.string.language_filipino, R.drawable.language_ic_philippines, "tl"),

    VIETNAMESE(11, R.string.language_vietnamese, R.drawable.language_ic_vietnam, "vi");

    companion object {
        fun toLanguageModelList(): ArrayList<LanguageModel> {
            val list = arrayListOf<LanguageModel>()
            for (lang in entries) {
                list.add(
                    LanguageModel(
                        txtLanguage = lang.nameLanguage,
                        icFlag = lang.flag,
                        code = lang.code,
                        isSelect = false
                    )
                )
            }
            return list
        }
    }
}
