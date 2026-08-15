package com.houvven.guise.xposed.hook

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class CameraHookTest {

    @Test
    fun cameraLimitNeverInventsIds() {
        val ids = listOf("0", "1")

        assertArrayEquals(arrayOf("0"), limitVisibleCameraIds(ids, 1))
        assertArrayEquals(arrayOf("0", "1"), limitVisibleCameraIds(ids, 8))
        assertArrayEquals(emptyArray<String>(), limitVisibleCameraIds(ids, 0))
    }
}
