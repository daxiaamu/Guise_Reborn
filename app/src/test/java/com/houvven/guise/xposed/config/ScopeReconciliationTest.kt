package com.houvven.guise.xposed.config

import org.junit.Assert.assertEquals
import org.junit.Test

class ScopeReconciliationTest {

    @Test
    fun staleEnabledConfigurationIsNeverRequested() {
        val changes = planScopeReconciliation(
            savedConfigs = listOf(
                ModuleConfig(packageName = "cn.ieway.evplayer", enabled = true),
                ModuleConfig(packageName = "com.example.installed", enabled = true),
            ),
            installedPackages = setOf("com.example.installed"),
            currentScope = emptySet(),
        )

        assertEquals(emptySet<String>(), changes.removedPackages)
        assertEquals(setOf("com.example.installed"), changes.addedPackages)
    }

    @Test
    fun stalePackageAlreadyInScopeIsRemoved() {
        val changes = planScopeReconciliation(
            savedConfigs = listOf(
                ModuleConfig(packageName = "cn.ieway.evplayer", enabled = true),
            ),
            installedPackages = emptySet(),
            currentScope = setOf("cn.ieway.evplayer"),
        )

        assertEquals(setOf("cn.ieway.evplayer"), changes.removedPackages)
        assertEquals(emptySet<String>(), changes.addedPackages)
    }

    @Test
    fun installedEnabledScopeIsKeptWhileDisabledScopeIsRemoved() {
        val changes = planScopeReconciliation(
            savedConfigs = listOf(
                ModuleConfig(packageName = "com.example.enabled", enabled = true),
                ModuleConfig(packageName = "com.example.disabled", enabled = false),
            ),
            installedPackages = setOf("com.example.enabled", "com.example.disabled"),
            currentScope = setOf("com.example.enabled", "com.example.disabled"),
        )

        assertEquals(setOf("com.example.disabled"), changes.removedPackages)
        assertEquals(emptySet<String>(), changes.addedPackages)
    }
}
