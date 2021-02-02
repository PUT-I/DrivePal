package pl.dps.mobile_app.helpers

import android.location.Location
import android.location.LocationListener

class SpeedWatcher(private val testSpeed: Boolean = false) : LocationListener {

    private var speed: Float = -1.0f

    private var testSpeedValue: Float = 100.0f
    private var testSpeedSlowdown: Boolean = false

    override fun onLocationChanged(location: Location) {
        location.speed *= 3.6f
        speed = location.speed
    }

    fun getSpeed(): Float {
        if (testSpeed) {
            if (testSpeedSlowdown) {
                testSpeedValue -= 10.0f
            } else {
                testSpeedValue += 10.0f
            }

            if (speed <= 0.0f) {
                speed = 0.0f
                testSpeedSlowdown = false
            } else if (speed >= 150.0f) {
                speed = 150.0f
                testSpeedSlowdown = true
            }

            return testSpeedValue
        }

        return speed
    }

}