package com.houvven.guise.xposed.hook.network

import org.junit.Assert.assertEquals
import org.junit.Test

class SimHookTest {

    @Test
    fun visibleSubscriptionLimitNeverFabricatesEntries() {
        val subscriptions = listOf("primary", "secondary")

        assertEquals(listOf("primary"), limitVisibleSubscriptions(subscriptions, 1))
        assertEquals(subscriptions, limitVisibleSubscriptions(subscriptions, 4))
        assertEquals(emptyList<String>(), limitVisibleSubscriptions(subscriptions, 0))
    }
}
