package es.gde.unizar.puf

import android.content.Context
import es.gde.unizar.puf.Operation.AVER
import es.gde.unizar.puf.Operation.NOISE
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt


class KeyProcessor(private val cntx: Context) {
    private fun <T> readRawFile(file: Int, mapper: (Sequence<String>) -> T) = cntx.resources.openRawResource(file).reader().useLines(mapper)


    private val nBits = 6

    private val dataGauss = readRawFile(R.raw.gaussian_gray) { lines ->
        lines.filter { it.isNotBlank() }
            .map { it.split("\t") }
            .map { (min, max, res) -> Triple(min.toFloat(), max.toFloat(), res.padStart(nBits, '0')) }
            .toList()
    }

    private fun Int.decimalToGray() = run {
        // In case precision is not adjusted
        var decimalNumFix = this
        while (decimalNumFix > 100 || decimalNumFix < -100) decimalNumFix /= 10

        // Assign
        dataGauss.find { (min, max, _) -> decimalNumFix >= min && decimalNumFix < max }?.third ?: "?".repeat(nBits)
    }


    private fun process(column: Int, settings: Settings) = run {
        // Load data
        readRawFile(settings.filename) { lines ->
            lines.map { line ->
                line.split("\t")[column].toDouble()
                    .mapIf(column == 2) { it - 9.8 }
            }
                // Select time interval
                .take(2000).drop(500)
                .let { values ->
                    when (settings.value) {
                        AVER -> {
                            // Average
                            values.chunked(settings.sizeSequence)
                                .map { it.average() }.toList()
                                .filterBySigma(settings.nSigmas)
                                .average()
                                .timesTenToThePowerOf(settings.precision)
                                .decimalToGray()
                        }

                        NOISE -> {
                            // Max & min estimation
                            values.chunked(settings.sizeSequence)
                                .map { it.max() to it.min() }.unzip()
                                .map { it.filterBySigma(settings.nSigmas) }
                                .map { it.average() }
                                // Noise
                                .let { (max, min) -> abs(max - min) }
                                .timesTenToThePowerOf(settings.precision)
                                .decimalToGray()
                        }
                    }
                }
        }
    }


    fun main(fileNv: Int, fileV: Int, fileG: Int) =
        sequenceOf(
            Settings(fileNv, NOISE, 10, 3, 3),
            Settings(fileNv, AVER, 10, 3, 2),
            Settings(fileV, NOISE, 30, 1, 2),
            Settings(fileV, AVER, 30, 1, 2),
            Settings(fileG, NOISE, 10, 3, 4),
            Settings(fileG, AVER, 10, 3, 5),
        ).flatMap { settings ->
            (0..2).map { column ->
                process(column, settings)
            }
        }.joinToString("")

}


private data class Settings(val filename: Int, val value: Operation, val sizeSequence: Int, val nSigmas: Int, val precision: Int)

private enum class Operation { NOISE, AVER }

private fun List<Double>.filterBySigma(xSigmas: Int) = run {
    val average = average()
    val standardDeviation = sqrt(map { (it - average).pow(2) }.average())

    filter { abs(it - average) <= xSigmas * standardDeviation }
}

private fun Double.timesTenToThePowerOf(precision: Int) = (this * 10.0.pow(precision)).toInt()

private fun <T> T.mapIf(condition: Boolean, mapper: (T) -> T) = if (condition) mapper(this) else this
private fun <T, R> Pair<T, T>.map(any: (T) -> R) = any(first) to any(second)
