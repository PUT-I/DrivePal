package pl.dps.mobile_app.helpers

import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONObject

class ServerClient(private val queue: RequestQueue,
                   private val serverUrl: String) {

    companion object {
        private const val TAG = "ServerClient"
    }

    fun sendDiagnostics(jsonBody: JSONObject) {
        Log.d(TAG, "Sending statistics : $jsonBody")
        val request = JsonObjectRequest(Request.Method.POST, "${serverUrl}/api/diagnostic-data", jsonBody,
                { response ->
                    Log.d(TAG, "Statistics POST succeeded")
                    Log.d(TAG, "Content : $response")
                },
                { error ->
                    Log.d(TAG, "Statistics POST failed")
                    Log.d(TAG, "Status code : ${error.networkResponse?.statusCode}")
                    Log.d(TAG, "Message : ${error.networkResponse?.data}")
                })

        queue.add(request)
    }

    fun sendDetection(jsonBody: JSONObject) {
        Log.d(TAG, "Sending detection : $jsonBody")
        if (jsonBody.getString("image").isBlank()) {
            throw RuntimeException("Tried to send empty image")
        }

        val request = JsonObjectRequest(Request.Method.POST, "${serverUrl}/api/detection", jsonBody,
                { response ->
                    Log.d(TAG, "Detection POST succeeded")
                    Log.d(TAG, "Content : $response")
                },
                { error ->
                    Log.d(TAG, "Detection POST failed")
                    Log.d(TAG, "Status code : ${error.networkResponse?.statusCode}")
                    Log.d(TAG, "Message : ${error.networkResponse?.data}")
                })

        queue.add(request)
    }

}