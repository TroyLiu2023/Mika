package com.wangzan101.mika.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * 「本对话主模型」面板(`chat_model_*`)、「模型组」页面(`models_group*`)和备援模型(`main_fallback_*`)的文案,
 * 在每种非英语语言里都要有译文:条目数和占位符都对得上、内容却是英文原文的时候,[StringResourcesTest] 查不出来,
 * 土耳其语和波斯语目录曾整块留着英文。本来就与英文拼写相同的条目列在 [sameSpelling] 里。
 */
class ModelPickerTranslationTest {
    private val prefixes = listOf("chat_model_", "models_group", "main_fallback_")

    private val others = listOf(
        "values-zh" to Locale.SIMPLIFIED_CHINESE,
        "values-b+zh+Hant" to Locale.TRADITIONAL_CHINESE,
        "values-ja" to Locale.JAPANESE,
        "values-ko" to Locale.KOREAN,
        "values-es" to Locale.forLanguageTag("es"),
        "values-pt" to Locale.forLanguageTag("pt"),
        "values-ru" to Locale.forLanguageTag("ru"),
        "values-tr" to Locale.forLanguageTag("tr"),
        "values-ar" to Locale.forLanguageTag("ar"),
        "values-fa" to Locale.forLanguageTag("fa"),
        // 印尼语的资源文件夹沿用安卓的旧语言代码 in
        "values-in" to Locale.forLanguageTag("id"),
        "values-vi" to Locale.forLanguageTag("vi"),
    )

    /** 语言文件夹/条目名:这种语言里的拼写本来就和英文相同。 */
    private val sameSpelling = setOf(
        "values-tr/chat_model_chip_model",
        "values-in/chat_model_chip_model",
        "values-in/models_group_edit",
    )

    private val en = ResFileTexts.english

    @Test
    fun modelPickerTextsAreTranslatedEverywhere() {
        val names = en.strings.keys.filter { name -> prefixes.any { name.startsWith(it) } }
        assertTrue(names.size > 40)
        val leftInEnglish = others.flatMap { (folder, locale) ->
            val texts = ResFileTexts(folder, locale)
            names.filter { name -> "$folder/$name" !in sameSpelling && texts.strings[name] == en.strings[name] }
                .map { name -> "$folder/$name" }
        }
        assertEquals(emptyList<String>(), leftInEnglish)
    }
}
