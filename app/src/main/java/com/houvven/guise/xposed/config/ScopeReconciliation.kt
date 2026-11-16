package com.houvven.guise.xposed.config

internal data class ScopeReconciliation(
    val removedPackages: Set<String>,
    val addedPackages: Set<String>,
)

/**
 * Calculates scope changes without ever requesting an uninstalled package.
 *
 * Saved configurations deliberately survive uninstall/reinstall, but LSPosed scope requests must
 * only contain packages that PackageManager can currently see for this user. Otherwise a stale
 * enabled configuration causes a new approval prompt every time the module UI binds to LSPosed.
 */
internal fun planScopeReconciliation(
    savedConfigs: Iterable<ModuleConfig>,
    installedPackages: Set<String>,
    currentScope: Set<String>,
): ScopeReconciliation {
    val desiredScope = savedConfigs.asSequence()
        .filter { it.enabled && it.packageName in installedPackages }
        .mapTo(mutableSetOf()) { it.packageName }
    return ScopeReconciliation(
        removedPackages = currentScope - desiredScope,
        addedPackages = desiredScope - currentScope,
    )
}
