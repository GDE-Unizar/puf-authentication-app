package es.gde.unizar.puf

import android.content.Context
import android.util.Log
import es.gde.unizar.puf.Operation.AVER
import es.gde.unizar.puf.Operation.NOISE
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt


/** data class to configure the steps */
data class Step(
    val data: List<List<Double>>, // TODO: List<Event> where Event(x,y,z)
    val value: Operation,
    val sizeSequence: Int,
    val nSigmas: Int,
    val precision: Int,
    val startTime: Int,
    val endTime: Int,
    val gravity: List<Double>,
)

/** type of operations that can be applied */
enum class Operation { NOISE, AVER }

/** Processes the keys */
class KeyProcessor(
    private val cntx: Context,
    private val nBits: Int = 6,
) {

    /** Main method: extracts the key from the files */
    fun main(settings: List<Step>) =
        // for each step, for each column, process and concatenate all
        settings.flatMap { setting ->
            (0..2).map { column ->
                process(setting, column)
            }
        }.joinToString("")

    /** Processes a configuration for a column */
    private fun process(step: Step, column: Int) =
        step.data.asSequence()
            // Select time interval
            .take(step.endTime).drop(step.startTime)
            // get column
            .map { it[column] }
            // subtract gravity
            .map { it - step.gravity[column] }

            .chunked(step.sizeSequence)
            .run {
                when (step.value) {

                    // Average of the filtered averaged chunks
                    AVER -> map { it.average() }.toList()
                        .filterBySigma(step.nSigmas)
                        .average()

                    // difference of the average filtered max & min chunks
                    NOISE -> map { it.max() to it.min() }.unzip().toList()
                        .map { it.filterBySigma(step.nSigmas) }
                        .map { it.average() }
                        // Noise
                        .let { (max, min) -> abs(max - min) }
                }
            }
            // get the gray value of it*10^precision
            .timesTenToThePowerOf(step.precision)
            .decimalToGray()

    /** Reads the lines of a raw file */
    private fun <T> readRawFile(file: Int, mapper: (Sequence<String>) -> T) = cntx.resources.openRawResource(file).reader().useLines(mapper)

    /** gray values: list of triples(min,max,result) */
    private val grayValues = readRawFile(R.raw.gaussian_gray) { lines ->
        // each line has min, max and the result
        lines.filter { it.isNotBlank() }
            .map { it.split("\t") }
            .map { (min, max, res) -> Triple(min.toFloat(), max.toFloat(), res.padStart(nBits, '0')) }
            .toList()
    }

    /** Returns the gray value from this int value*/
    private fun Int.decimalToGray() = let {
        // ensure value in [-100,100] in case precision is not adjusted
        var value = it
        while (value > 100 || value < -100) value /= 10

        // find and return
        grayValues.find { (min, max, _) -> value >= min && value < max }?.third
            ?: "?".repeat(nBits)
    }

    // ----- testing -----

    fun test() = run {
        val (fileNv, fileV, fileG) = listOf(R.raw.example, R.raw.example2, R.raw.example3)
            .map { file ->
                readRawFile(file) { lines ->
                    lines.map { line ->
                        line.split("\t").map { it.toDouble() }
                    }.toList()
                }
            }
        val gravity = listOf(0.0, 0.0, 9.8)

        val obtained = main(
            listOf(
                Step(fileNv, NOISE, 10, 3, 3, 500, 2000, gravity),
                Step(fileNv, AVER, 10, 3, 2, 500, 2000, gravity),
                Step(fileV, NOISE, 30, 1, 2, 500, 2000, gravity),
                Step(fileV, AVER, 30, 1, 2, 500, 2000, gravity),
                Step(fileG, NOISE, 10, 3, 4, 500, 2000, gravity),
                Step(fileG, AVER, 10, 3, 5, 500, 2000, gravity),
            )
        )
        val expected = "110101110101111110101000000010011000101000111010011100111000000010011000110000110101110001001011000000000000"

        Log.d("OBTAINED", obtained)
        Log.d("EXPECTED", expected)

        obtained == expected
    }

}

/** Returns the values under the sigma */
private fun List<Double>.filterBySigma(xSigmas: Int) = run {
    val average = average()
    val standardDeviation = sqrt(map { (it - average).pow(2) }.average())

    filter { abs(it - average) <= xSigmas * standardDeviation }
}

/** this*10^precision as Int */
private fun Double.timesTenToThePowerOf(precision: Int) = (this * 10.0.pow(precision)).toInt()
