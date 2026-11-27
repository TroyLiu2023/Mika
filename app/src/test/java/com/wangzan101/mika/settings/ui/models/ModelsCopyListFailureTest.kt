package com.wangzan101.mika.settings.ui.models

import com.wangzan101.mika.core.model.ListFailure
import com.wangzan101.mika.core.model.ModelListResult
import com.wangzan101.mika.i18n.EnglishTexts
import com.wangzan101.mika.i18n.ResFileTexts
import com.wangzan101.mika.i18n.UiText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import java.util.Locale

/** 列模型失败的原因:按失败类型取当前界面语言的资源,不用 [ListFailure] 自带的中文。 */
class ModelsCopyListFailureTest {
    @get:Rule val english = EnglishTexts()

    private val cjk = Regex("[\\u4e00-\\u9fff]")

    private val others = listOf(
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

    @Test fun everyFailureIsWordedInTheInterfaceLanguage() {
        val en = ResFileTexts.english
        assertEquals("The API key is invalid or has no permission", ModelsCopy.listFailure(ModelListResult.Failed(ListFailure.Auth)))
        assertEquals(en.strings.getValue("model_url_scheme"), ModelsCopy.listFailure(ModelListResult.Failed(ListFailure.BadUrl)))
        assertEquals(en.strings.getValue("model_key_illegal_chars"), ModelsCopy.listFailure(ModelListResult.Failed(ListFailure.IllegalKey)))
        assertEquals(en.strings.getValue("net_proxy_auth_failed"), ModelsCopy.listFailure(ModelListResult.Failed(ListFailure.ProxyAuth)))
        for (reason in ListFailure.entries) {
            val text = ModelsCopy.listFailure(ModelListResult.Failed(reason))
            assertFalse(reason.name, cjk.containsMatchIn(text))
        }
    }

    @Test fun detailFollowsTheReason() {
        assertEquals("The API key is invalid or has no permission(401)", ModelsCopy.listFailure(ModelListResult.Failed(ListFailure.Auth, "401")))
        assertEquals("Too many requests, try again later", ModelsCopy.listFailure(ModelListResult.Failed(ListFailure.RateLimited, "")))
    }

    /** 新增的 7 条在简体中文里和 core 自带的文字逐字一致(标点也算),中文界面看不出这次改动。 */
    @Test fun chineseKeepsTheCoreWording() {
        withTexts(ResFileTexts.chinese) {
            for (reason in listOf(ListFailure.Auth, ListFailure.Quota, ListFailure.RateLimited, ListFailure.Network,
                ListFailure.NotSupported, ListFailure.Server, ListFailure.BadResponse)) {
                assertEquals(reason.name, reason.text, ModelsCopy.listFailure(ModelListResult.Failed(reason)))
            }
            assertEquals(ListFailure.Quota.text + "(402)", ModelsCopy.listFailure(ModelListResult.Failed(ListFailure.Quota, "402")))
        }
    }

    /** 日语本来就用汉字,只查它不再是 core 的原文。 */
    @Test fun otherLanguagesDoNotShowTheChineseOriginal() {
        val folders = others + ("values-ja" to Locale.JAPANESE) + ("values-b+zh+Hant" to Locale.TRADITIONAL_CHINESE)
        for ((folder, locale) in folders) {
            val texts = ResFileTexts(folder, locale)
            for (reason in ListFailure.entries) {
                withTexts(texts) {
                    val text = ModelsCopy.listFailure(ModelListResult.Failed(reason))
                    assertEquals("$folder/${reason.name}", false, text == reason.text)
                    if (folder != "values-ja" && folder != "values-b+zh+Hant") {
                        assertFalse("$folder/${reason.name}: $text", cjk.containsMatchIn(text))
                    }
                }
            }
        }
    }

    private fun withTexts(texts: ResFileTexts, block: () -> Unit) {
        val previous = UiText.source
        UiText.source = texts
        try {
            block()
        } finally {
            UiText.source = previous
        }
    }
}
