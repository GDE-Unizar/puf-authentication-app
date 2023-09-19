package es.gde.unizar.puf

import android.content.Context
import android.os.Vibrator

/** Makes the device vibrate */
class Vibrator(cntx: Context) {

    private val vibrator = cntx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    fun vibrate() = vibrator.vibrate(always, 0)

    fun stop() = vibrator.cancel()

}

private val always = longArrayOf(0, Integer.MAX_VALUE.toLong())
