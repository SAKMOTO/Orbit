package com.roubao.autopilot.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * 语言切换辅助类
 */
object LocaleHelper {

    /**
     * 应用语言设置到 Context
     */
    fun onAttach(context: Context, language: String): Context {
        val locale = getLocale(language)
        return updateResources(context, locale)
    }

    /**
     * 获取语言对应的 Locale
     */
    private fun getLocale(language: String): Locale {
        return when (language) {
            "zh" -> Locale.SIMPLIFIED_CHINESE
            "en" -> Locale.ENGLISH
            else -> Locale.getDefault()
        }
    }

    /**
     * 更新 Context 的资源配置
     */
    private fun updateResources(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)
        val res = context.resources
        val config = Configuration(res.configuration)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            val localeList = LocaleList(locale)
            config.setLocales(localeList)
            return context.createConfigurationContext(config)
        } else {
            config.setLocale(locale)
            res.updateConfiguration(config, res.displayMetrics)
            return context
        }
    }
}
