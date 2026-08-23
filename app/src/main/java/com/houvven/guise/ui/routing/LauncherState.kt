package com.houvven.guise.ui.routing

import android.annotation.SuppressLint
import androidx.compose.runtime.*
import com.houvven.guise.db.Template
import com.houvven.guise.db.TemplateDBHelper
import com.houvven.guise.db.defaults.BundledTemplateManager
import com.houvven.guise.ContextAmbient
import com.houvven.guise.module.apps.AppInfo
import com.houvven.guise.module.apps.AppInfoProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@SuppressLint("MutableCollectionMutableState")
object LauncherState {

    private val templateDao by lazy { TemplateDBHelper.templateDao }

    val apps = mutableStateOf<List<AppInfo>>(emptyList())

    val templates by lazy {
        mutableStateOf(BundledTemplateManager.synchronize(ContextAmbient.current))
    }

    fun refreshApps() {
        apps.value = AppInfoProvider.getList()
    }

    fun setAppEnabled(packageName: String, enabled: Boolean) {
        apps.value = apps.value.map { app ->
            if (app.packageName == packageName) app.copy(isEnable = enabled) else app
        }
    }

    fun setAppsEnabled(enabledPackages: Set<String>, disabledPackages: Set<String>) {
        apps.value = apps.value.map { app ->
            when (app.packageName) {
                in enabledPackages -> app.copy(isEnable = true)
                in disabledPackages -> app.copy(isEnable = false)
                else -> app
            }
        }
    }

    private fun refreshTemplates() {
        templates.value = templateDao.getAll()
    }

    fun addTemplate(template: Template) {
        templateDao.insert(template)
        refreshTemplates()
    }

    suspend fun addTemplates(imported: List<Template>) {
        val refreshed = withContext(Dispatchers.IO) {
            templateDao.insertMany(imported)
            templateDao.getAll()
        }
        templates.value = refreshed
    }

    fun deleteTemplate(template: Template) {
        BundledTemplateManager.delete(template)
        refreshTemplates()
    }

    fun updateTemplate(template: Template) {
        templateDao.update(template)
        refreshTemplates()
    }


}
