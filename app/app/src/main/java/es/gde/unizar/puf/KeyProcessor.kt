package es.gde.unizar.puf

import android.content.Context
import es.gde.unizar.puf.Operation.AVER
import es.gde.unizar.puf.Operation.NOISE
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt


// ---------- CONFIG -------------
private const val N_BITS = 6
private const val START_TIME = 500
private const val END_TIME = 2000
private val GRAVITY = listOf(0.0, 0.0, 9.8)
private fun getSettings(fileNv: Int, fileV: Int, fileG: Int) = sequenceOf(
    Settings(fileNv, NOISE, 10, 3, 3),
    Settings(fileNv, AVER, 10, 3, 2),
    Settings(fileV, NOISE, 30, 1, 2),
    Settings(fileV, AVER, 30, 1, 2),
    Settings(fileG, NOISE, 10, 3, 4),
    Settings(fileG, AVER, 10, 3, 5),
)


/** Processes the keys */
class KeyProcessor(private val cntx: Context) {

    /** Reads the lines of a raw file */
    private fun <T> readRawFile(file: Int, mapper: (Sequence<String>) -> T) = cntx.resources.openRawResource(file).reader().useLines(mapper)

    /** gray values: list of triples(min,max,result) */
    private val grayValues = readRawFile(R.raw.gaussian_gray) { lines ->
        // each line has min, max and the result
        lines.filter { it.isNotBlank() }
            .map { it.split("\t") }
            .map { (min, max, res) -> Triple(min.toFloat(), max.toFloat(), res.padStart(N_BITS, '0')) }
            .toList()
    }

    /** Returns the gray value from this int value*/
    private fun Int.decimalToGray() = let {
        // ensure value in [-100,100] in case precision is not adjusted
        var value = it
        while (value > 100 || value < -100) value /= 10

        // find and return
        grayValues.find { (min, max, _) -> value >= min && value < max }?.third
            ?: "?".repeat(N_BITS)
    }

    /** Processes a configuration for a column */
    private fun process(settings: Settings, column: Int) = run {
        // Load file
        readRawFile(settings.filename) { lines ->
            lines
                // Select time interval
                .take(END_TIME).drop(START_TIME)
                // extract column
                .map { line ->
                    line.split("\t")[column].toDouble()
                }
                // subtract gravity
                .map { it - GRAVITY[column] }

                .chunked(settings.sizeSequence)
                .run {
                    when (settings.value) {

                        // Average of the filtered averaged chunks
                        AVER -> map { it.average() }.toList()
                            .filterBySigma(settings.nSigmas)
                            .average()

                        // difference of the average filtered max & min chunks
                        NOISE -> map { it.max() to it.min() }.unzip().toList()
                            .map { it.filterBySigma(settings.nSigmas) }
                            .map { it.average() }
                            // Noise
                            .let { (max, min) -> abs(max - min) }
                    }
                }
                // get the gray value of it*10^precision
                .timesTenToThePowerOf(settings.precision)
                .decimalToGray()
        }
    }

    /** Main method: extracts the key from the files */
    fun main(fileNv: Int, fileV: Int, fileG: Int) =
        // for each configuration, for each column, process and concatenate all
        getSettings(fileNv, fileV, fileG)
            .flatMap { settings ->
                (0..2).map { column ->
                    process(settings, column)
                }
            }.joinToString("")

}

/** data class to configure the processes */
private data class Settings(val filename: Int, val value: Operation, val sizeSequence: Int, val nSigmas: Int, val precision: Int)

/** type of operations that can be applied */
private enum class Operation { NOISE, AVER }

/** Returns the values under the sigma */
private fun List<Double>.filterBySigma(xSigmas: Int) = run {
    val average = average()
    val standardDeviation = sqrt(map { (it - average).pow(2) }.average())

    filter { abs(it - average) <= xSigmas * standardDeviation }
}

/** this*10^precision as Int */
private fun Double.timesTenToThePowerOf(precision: Int) = (this * 10.0.pow(precision)).toInt()
