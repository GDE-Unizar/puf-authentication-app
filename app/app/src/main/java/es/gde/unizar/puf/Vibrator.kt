package es.gde.unizar.puf

import android.content.Context
import android.os.Vibrator

/** Makes the device vibrate */
class Vibrator(cntx: Context) {
    private val vibrator = cntx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    /** turns on the vibration. Will vibrate until stop is called */
    fun vibrate() = vibrator.vibrate(longArrayOf(0, 5000), 0) // vibrate in 5s intervals (without pauses) otherwise the vibration will stop at 20s

    /** turns off the vibration */
    fun stop() = vibrator.cancel()

}

