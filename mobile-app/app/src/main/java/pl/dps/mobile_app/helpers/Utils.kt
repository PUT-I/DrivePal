package pl.dps.mobile_app.helpers

import android.os.Build
import java.io.BufferedReader
import java.io.InputStreamReader

object Utils {
    fun getCpuTemperature(): Float? {
        val process = Runtime.getRuntime()
                .exec("cat sys/class/thermal/thermal_zone0/temp")
        process.waitFor()

        var temperature: Float
        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            val line: String = reader.readLine() ?: return null

            temperature = line.toFloat() / 1000.0F
        }

        return temperature
    }

    fun getSocModel(): String {
        return Build.BOARD
    }
}