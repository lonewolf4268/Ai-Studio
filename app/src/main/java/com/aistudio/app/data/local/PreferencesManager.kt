package com.aistudio.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.aistudio.app.data.model.PromptTemplate
import org.json.JSONArray
import org.json.JSONObject

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("ai_studio_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_API_KEY = "gemini_api_key"
        private const val KEY_MODEL_NAME = "gemini_model_name"
        private const val KEY_SYSTEM_INSTRUCTION = "system_instruction"
        private const val KEY_DARK_MODE = "is_dark_mode"
        private const val KEY_AUTO_SCROLL = "auto_scroll"
        private const val KEY_TEMPLATES = "prompt_templates"
        private const val KEY_ACTIVE_SESSION = "active_session_id"
    }

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_API_KEY, value).apply()

    var modelName: String
        get() = prefs.getString(KEY_MODEL_NAME, "gemini-3.5-flash") ?: "gemini-3.5-flash"
        set(value) = prefs.edit().putString(KEY_MODEL_NAME, value).apply()

    var systemInstruction: String
        get() = prefs.getString(KEY_SYSTEM_INSTRUCTION, "You are a helpful, versatile AI assistant.") ?: ""
        set(value) = prefs.edit().putString(KEY_SYSTEM_INSTRUCTION, value).apply()

    var isDarkMode: Boolean?
        get() {
            return if (prefs.contains(KEY_DARK_MODE)) {
                prefs.getBoolean(KEY_DARK_MODE, false)
            } else {
                null
            }
        }
        set(value) {
            if (value == null) {
                prefs.edit().remove(KEY_DARK_MODE).apply()
            } else {
                prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()
            }
        }

    var autoScroll: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SCROLL, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SCROLL, value).apply()

    var activeSessionId: String
        get() = prefs.getString(KEY_ACTIVE_SESSION, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ACTIVE_SESSION, value).apply()

    fun getTemplates(): List<PromptTemplate> {
        val raw = prefs.getString(KEY_TEMPLATES, null)
        if (raw.isNullOrBlank()) {
            return listOf(
                PromptTemplate("t1", "Code Refactoring", "Please refactor the following code for cleanliness, performance, and best practices:\n\n"),
                PromptTemplate("t2", "Bug Fixer", "Analyze this code snippet, identify any bugs or edge case failures, and provide a corrected version:\n\n"),
                PromptTemplate("t3", "Summarizer", "Provide a concise, bulleted summary of the following text:\n\n"),
                PromptTemplate("t4", "Explain Like I'm 5", "Explain the following concept or technical topic in simple terms with an everyday analogy:\n\n")
            )
        }
        val list = mutableListOf<PromptTemplate>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    PromptTemplate(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        prompt = obj.getString("prompt")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun saveTemplates(templates: List<PromptTemplate>) {
        val arr = JSONArray()
        templates.forEach {
            val obj = JSONObject().apply {
                put("id", it.id)
                put("title", it.title)
                put("prompt", it.prompt)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_TEMPLATES, arr.toString()).apply()
    }
}
