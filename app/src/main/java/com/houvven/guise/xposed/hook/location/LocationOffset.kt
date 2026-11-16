package com.houvven.guise.xposed.hook.location

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

internal data class OffsetCoordinates(
    val latitude: Double?,
    val longitude: Double?,
)

/** Generates one uniformly distributed point inside a radius on the Earth's surface. */
internal object LocationOffset {

    fun randomWithinRadius(
        latitude: Double?,
        longitude: Double?,
        radiusMeters: Double,
        random: Random = Random.Default,
    ): OffsetCoordinates {
        if (radiusMeters <= 0.0 || !radiusMeters.isFinite() ||
            (latitude == null && longitude == null)
        ) {
            return OffsetCoordinates(latitude, longitude)
        }

        // sqrt keeps points uniform by area instead of clustering them near the centre.
        val distanceMeters = sqrt(random.nextDouble()) * radiusMeters
        val bearingRadians = random.nextDouble() * 2.0 * PI
        val destination = destinationPoint(
            latitude = latitude ?: 0.0,
            longitude = longitude ?: 0.0,
            distanceMeters = distanceMeters,
            bearingRadians = bearingRadians,
        )
        return OffsetCoordinates(
            latitude = latitude?.let { destination.first },
            longitude = longitude?.let { destination.second },
        )
    }

    private fun destinationPoint(
        latitude: Double,
        longitude: Double,
        distanceMeters: Double,
        bearingRadians: Double,
    ): Pair<Double, Double> {
        val latitudeRadians = Math.toRadians(latitude.coerceIn(-90.0, 90.0))
        val longitudeRadians = Math.toRadians(longitude)
        val angularDistance = distanceMeters / EARTH_RADIUS_METERS
        val destinationLatitude = asin(
            sin(latitudeRadians) * cos(angularDistance) +
                cos(latitudeRadians) * sin(angularDistance) * cos(bearingRadians)
        )
        val destinationLongitude = longitudeRadians + atan2(
            sin(bearingRadians) * sin(angularDistance) * cos(latitudeRadians),
            cos(angularDistance) - sin(latitudeRadians) * sin(destinationLatitude),
        )
        val normalizedLongitude =
            ((Math.toDegrees(destinationLongitude) + 540.0) % 360.0) - 180.0
        return Math.toDegrees(destinationLatitude) to normalizedLongitude
    }

    private const val EARTH_RADIUS_METERS = 6_371_008.8
}
