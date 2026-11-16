package com.houvven.guise.xposed.hook.location

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationOffsetTest {

    @Test
    fun generatedCoordinatesStayWithinConfiguredRadius() {
        val latitude = 39.9042
        val longitude = 116.4074
        val radiusMeters = 250.0
        val random = Random(25)

        repeat(500) {
            val result = LocationOffset.randomWithinRadius(
                latitude = latitude,
                longitude = longitude,
                radiusMeters = radiusMeters,
                random = random,
            )
            val distance = distanceMeters(
                latitude,
                longitude,
                result.latitude!!,
                result.longitude!!,
            )
            assertTrue("distance=$distance", distance <= radiusMeters + 0.01)
        }
    }

    @Test
    fun zeroRadiusLeavesCoordinatesUnchanged() {
        val result = LocationOffset.randomWithinRadius(10.0, 20.0, 0.0, Random(1))

        assertEquals(10.0, result.latitude!!, 0.0)
        assertEquals(20.0, result.longitude!!, 0.0)
    }

    @Test
    fun anUnsetCoordinateRemainsUnset() {
        val result = LocationOffset.randomWithinRadius(null, 116.4074, 100.0, Random(1))

        assertNull(result.latitude)
        assertTrue(result.longitude!! in -180.0..180.0)
    }

    private fun distanceMeters(
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double,
    ): Double {
        val latitudeDelta = Math.toRadians(endLatitude - startLatitude)
        val longitudeDelta = Math.toRadians(endLongitude - startLongitude)
        val startLatitudeRadians = Math.toRadians(startLatitude)
        val endLatitudeRadians = Math.toRadians(endLatitude)
        val haversine = sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
            cos(startLatitudeRadians) * cos(endLatitudeRadians) *
            sin(longitudeDelta / 2) * sin(longitudeDelta / 2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(haversine))
    }

    private companion object {
        const val EARTH_RADIUS_METERS = 6_371_008.8
    }
}
