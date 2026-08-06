package com.houvven.guise.ui.routing.launcher

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DoNotDisturbOnTotalSilence
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.houvven.guise.R
import com.houvven.guise.db.Template
import com.houvven.guise.db.TemplateTransfer
import com.houvven.guise.ui.GlobalSnackbarHost
import com.houvven.guise.ui.components.simplify.SimplifyDropdownMenuItem
import com.houvven.guise.ui.components.simplify.SimplifyIcon
import com.houvven.guise.ui.components.simplify.SimplifyImage
import com.houvven.guise.ui.routing.LauncherState
import com.houvven.guise.ui.routing.LocalNavController
import com.houvven.guise.ui.routing.NavRoutingTypes
import com.houvven.guise.ui.routing.navigateWithTemplate
import com.houvven.guise.ui.routing.template.EnableTemplateDialog
import com.houvven.guise.ui.utils.saveFileToDownloadDir
import com.houvven.guise.xposed.PackageConfig
import com.houvven.guise.xposed.config.ModuleConfig
import java.io.Reader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private object TemplateTypeFilter {
    const val ALL = -1
    const val COMMON = Template.Type.COMMON
    const val EXCLUSIVE = Template.Type.EXCLUSIVE
}

private val typeFilter = mutableIntStateOf(TemplateTypeFilter.ALL)
private val requestEnable = mutableStateOf(false)
private val requestEnableTemplate = mutableStateOf<Template?>(null)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TemplateCard(template: Template, appliedAppCount: Int) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val isExclusive = template.type == Template.Type.EXCLUSIVE
    val exclusiveIcon = remember(isExclusive, template.packageName) {
        if (!isExclusive) {
            null
        } else {
            template.packageName?.let { packageName ->
                runCatching {
                    context.packageManager.getApplicationIcon(packageName)
                        .toBitmap()
                        .asImageBitmap()
                }.getOrNull()
            }
        }
    }
    val installed = !isExclusive || exclusiveIcon != null
    var expanded by remember { mutableStateOf(false) }

    val headIcon = @Composable {
        if (isExclusive) {
            val modifier = Modifier
                .size(25.dp)
                .padding(bottom = 5.dp)
            if (exclusiveIcon == null) {
                SimplifyImage(
                    Icons.Default.DoNotDisturbOnTotalSilence,
                    modifier = modifier,
                )
            } else {
                SimplifyImage(exclusiveIcon, modifier)
            }
        }
    }

    val content = @Composable {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            headIcon()
            if (isExclusive) {
                Spacer(modifier = Modifier.width(10.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = template.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = if (isExclusive) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        contentColor = if (isExclusive) {
                            MaterialTheme.colorScheme.onTertiary
                        } else {
                            MaterialTheme.colorScheme.onPrimary
                        },
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            text = stringResource(
                                if (isExclusive) {
                                    R.string.template_badge_exclusive
                                } else {
                                    R.string.template_badge_common
                                }
                            ),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                        )
                    }
                }
                if (!template.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = template.description!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = pluralStringResource(
                    R.plurals.template_applied_app_count,
                    appliedAppCount,
                    appliedAppCount,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = if (appliedAppCount > 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
            )
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isExclusive && !installed) {
                        GlobalSnackbarHost.showOnErrorByDismissPrevious(
                            resources.getString(R.string.exclusive_template_app_not_installed)
                        )
                    } else if (isExclusive) {
                        requestEnable.value = true
                        requestEnableTemplate.value = template
                    } else {
                        val navHostController = LocalNavController.current
                        navHostController.navigateWithTemplate(
                            NavRoutingTypes.ENABLE_TEMPLATE.name,
                            template,
                        )
                    }
                },
                onLongClick = { expanded = true },
            ),
        shape = RoundedCornerShape(10.dp),
    ) {
        content()
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(10.dp, (-5).dp),
        ) {
            SimplifyDropdownMenuItem(text = stringResource(R.string.edit), onClick = {
                expanded = false
                LocalNavController.current.navigateWithTemplate(
                    NavRoutingTypes.EDIT_TEMPLATE.name,
                    template,
                )
            })
            SimplifyDropdownMenuItem(text = stringResource(R.string.delete), onClick = {
                expanded = false
                LauncherState.deleteTemplate(template)
            })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun TemplateScreen() {
    val context = LocalContext.current
    val resources = LocalResources.current
    val navController = LocalNavController.current
    val templates = LauncherState.templates.value
    val configurationRevision = PackageConfig.configurationRevision.intValue
    val templateSignatures = remember(templates) {
        templates.map { it.id to it.configuration }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumeGeneration by remember { mutableIntStateOf(0) }
    var appliedCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeGeneration++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(templateSignatures, configurationRevision, resumeGeneration) {
        appliedCounts = withContext(Dispatchers.IO) {
            val usageBySignature = ModuleConfig.getAllSaved()
                .asSequence()
                .filter { it.enabled }
                .groupingBy(ModuleConfig::parameterSignature)
                .eachCount()
            templateSignatures.associate { (templateId, configuration) ->
                val signature = ModuleConfig.fromJson(configuration).parameterSignature()
                templateId to (usageBySignature[signature] ?: 0)
            }
        }
    }

    val topBar = @Composable {
        var topBarMenuExpanded by remember { mutableStateOf(false) }
        TopAppBar(
            title = { Text(stringResource(R.string.action_template)) },
            actions = {
                IconButton(onClick = { topBarMenuExpanded = true }) {
                    SimplifyIcon(Icons.Default.MoreVert)
                }
                DropdownMenu(
                    expanded = topBarMenuExpanded,
                    onDismissRequest = { topBarMenuExpanded = false },
                ) {
                    val resultLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent(),
                    ) { result ->
                        topBarMenuExpanded = false
                        if (result == null) return@rememberLauncherForActivityResult
                        runCatching {
                            context.contentResolver.openInputStream(result)?.bufferedReader()?.use {
                                TemplateTransfer.decode(it.readLimitedText(MAX_TEMPLATE_IMPORT_CHARS))
                            } ?: error("Unable to open selected file")
                        }.onSuccess { templates ->
                            LauncherState.addTemplates(templates)
                            GlobalSnackbarHost.showByDismissPrevious(
                                resources.getString(R.string.import_success)
                            )
                        }.onFailure {
                            GlobalSnackbarHost.showOnErrorByDismissPrevious(
                                resources.getString(R.string.import_failed, it.message.orEmpty())
                            )
                        }
                    }

                    SimplifyDropdownMenuItem(
                        text = stringResource(R.string.import_data),
                        onClick = { resultLauncher.launch("application/json") },
                    )
                    SimplifyDropdownMenuItem(
                        text = stringResource(R.string.export_data),
                        onClick = {
                            saveFileToDownloadDir(
                                "Guise-Template-${System.currentTimeMillis()}.json",
                                TemplateTransfer.encode(LauncherState.templates.value),
                            ).onSuccess {
                                GlobalSnackbarHost.showByDismissPrevious(
                                    resources.getString(R.string.export_success, it)
                                )
                            }.onFailure {
                                GlobalSnackbarHost.showOnErrorByDismissPrevious(
                                    resources.getString(R.string.export_failed, it.message.orEmpty())
                                )
                            }
                        },
                    )
                }
            },
        )
    }

    val floatingButton = @Composable {
        FloatingActionButton(
            onClick = { navController.navigate(NavRoutingTypes.ADD_TEMPLATE.name) },
            containerColor = MaterialTheme.colorScheme.primary,
        ) {
            SimplifyIcon(Icons.Default.Add)
        }
    }

    @Composable
    fun TypeFilter() {
        val commonCount = templates.count { it.type == Template.Type.COMMON }
        val exclusiveCount = templates.count { it.type == Template.Type.EXCLUSIVE }

        @Composable
        fun TypeFilterChip(label: String, count: Int, value: Int) {
            FilterChip(
                selected = typeFilter.intValue == value,
                onClick = { typeFilter.intValue = value },
                label = {
                    Text(stringResource(R.string.template_type_with_count, label, count))
                },
            )
        }
        Row(modifier = Modifier.padding(start = 15.dp)) {
            TypeFilterChip(
                label = stringResource(R.string.template_type_all),
                count = templates.size,
                value = TemplateTypeFilter.ALL,
            )
            Spacer(modifier = Modifier.width(5.dp))
            TypeFilterChip(
                label = stringResource(R.string.template_type_common),
                count = commonCount,
                value = TemplateTypeFilter.COMMON,
            )
            Spacer(modifier = Modifier.width(5.dp))
            TypeFilterChip(
                label = stringResource(R.string.template_type_app),
                count = exclusiveCount,
                value = TemplateTypeFilter.EXCLUSIVE,
            )
        }
    }

    Scaffold(
        topBar = topBar,
        floatingActionButton = floatingButton,
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding(),
            )
        ) {
            TypeFilter()
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val visibleTemplates = if (typeFilter.intValue == TemplateTypeFilter.ALL) {
                    templates
                } else {
                    templates.filter { it.type == typeFilter.intValue }
                }
                items(visibleTemplates, key = { it.id }) { template ->
                    TemplateCard(
                        template = template,
                        appliedAppCount = appliedCounts[template.id] ?: 0,
                    )
                }
            }

            requestEnableTemplate.value?.let {
                EnableTemplateDialog(state = requestEnable, template = it)
            }
        }
    }
}

private fun Reader.readLimitedText(maxChars: Int): String {
    val result = StringBuilder(minOf(DEFAULT_BUFFER_SIZE, maxChars))
    val buffer = CharArray(DEFAULT_BUFFER_SIZE)
    var total = 0
    while (true) {
        val count = read(buffer)
        if (count < 0) return result.toString()
        total += count
        require(total <= maxChars) { "Template file is too large" }
        result.append(buffer, 0, count)
    }
}

private const val MAX_TEMPLATE_IMPORT_CHARS = 2_000_000
