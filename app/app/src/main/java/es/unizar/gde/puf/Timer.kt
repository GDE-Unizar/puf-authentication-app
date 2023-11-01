package es.unizar.gde.puf

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

/** Calls [callback] each [delayMillis] until a total of [timerMillis] has passed */
suspend fun Timer(timerMillis: Int, delayMillis: Int, callback: (Int) -> Unit) = coroutineScope {
    var timer = 0
    while (timer < timerMillis) {
        delay(delayMillis.toLong())
        callback(timer)
        timer += delayMillis
    }
}
