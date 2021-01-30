package pl.dps.mobile_app.helpers

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.getSystemService
import com.example.mobile_app.R

class Alert(context: Context, message: String) {

    private val vibrator: Vibrator = getSystemService(context, Vibrator::class.java)!!
    private val notificationSound: Ringtone
    private val alertDialog: AlertDialog

    init {
        val notificationSoundUri: Uri =
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        notificationSound = RingtoneManager.getRingtone(context, notificationSoundUri)

        alertDialog = AlertDialog.Builder(context)
                .setTitle("Warning")
                .setMessage(message)
                .setIcon(R.drawable.warning)
                .create()
        alertDialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
    }

    fun show() {
        vibrator.vibrate(VibrationEffect.createOneShot(1000, 10))
        notificationSound.play()
        alertDialog.show()
    }

    fun hide() {
        if (isShowing()) {
            alertDialog.dismiss()
        }
    }

    private fun isShowing(): Boolean {
        return alertDialog.isShowing
    }

}