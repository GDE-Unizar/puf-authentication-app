package es.gde.unizar.puf

import android.content.Context
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class KeyProcessor(val cntx: Context) {


    fun readRawFile(file: Int) = cntx.resources.openRawResource(file).reader().readText()


    fun promedio_tramos(valores: List<Double>, tam_tramo: Int) = valores.chunked(tam_tramo).map { it.average() }

    fun maximo_tramos(valores: List<Double>, tam_tramo: Int) = valores.chunked(tam_tramo).map { it.max() }

    fun minimo_tramos(valores: List<Double>, tam_tramo: Int) = valores.chunked(tam_tramo).map { it.min() }

    fun filtrar(valores: List<Double>, x_sigmas: Int) = run {
        val promedio = valores.average()
        val desviacion_estandar = sqrt(valores.map { (it - promedio).pow(2) }.average())

        valores.filter { abs(it - promedio) <= x_sigmas * desviacion_estandar }
    }

    fun decimal_to_gray(decimal_num: Int, nbits: Int = 6) = run {
        // Read data
        val data_gauss = readRawFile(R.raw.gaussian_gray).lines()
            .filter { it.isNotBlank() }
            .map { it.split("\t") }
            .map { (min, max, res) -> Triple(min.toFloat(), max.toFloat(), res.padStart(nbits, '0')) }

        // In case precision is not adjusted
        var decimal_num_fix = decimal_num
        while (decimal_num_fix > 100 || decimal_num_fix < -100) decimal_num_fix /= 10

        // Assign
        data_gauss.find { (min_num, max_num, _) -> decimal_num_fix >= min_num && decimal_num_fix < max_num }?.third ?: "0".repeat(nbits)
    }

    fun procesa(filename: Int, col: Int, varr: String, tam_tramo: Int, nsigmas: Int, precision: Int) = run {
        // Load data
        readRawFile(filename).lines().map {
            it.split("\t")[col].toDouble().let {
                if (col == 2) it - 9.8 else it
            }
        }
            // Select time interval
            .run { subList(500, min(2000, size)) }
            .let { valores ->
                when (varr) {
                    // Average estimation
                    "aver" -> {
                        // Average
                        val datos_aver = promedio_tramos(valores, tam_tramo)
                        val datos_aver_filtered = filtrar(datos_aver, nsigmas)
                        val aver_filtered = (datos_aver_filtered.average() * (10.0.pow(precision))).toInt()


                        // Local key
                        val key_local = decimal_to_gray(aver_filtered)
                        key_local
                    }
                    // Average estimation
                    "noise" -> {
                        // Max estimation
                        val datos_max = maximo_tramos(valores, tam_tramo)
                        val datos_max_filtered = filtrar(datos_max, nsigmas)
                        val promedio_max_filtered = datos_max_filtered.average()

                        // Min estimation
                        val datos_min = minimo_tramos(valores, tam_tramo)
                        val datos_min_filtered = filtrar(datos_min, nsigmas)
                        val promedio_min_filtered = datos_min_filtered.average()

                        // Noise
                        val noise = (abs(promedio_max_filtered - promedio_min_filtered) * (10.0.pow(precision))).toInt()

                        // Local key
                        val key_local = decimal_to_gray(noise)
                        return key_local
                    }

                    else -> "????????"
                }
            }
    }

    fun main(file_nv: Int, file_v: Int, file_g: Int) = run {
        val key_nv_noise = procesa(file_nv, 0, "noise", 10, 3, 3) + procesa(file_nv, 1, "noise", 10, 3, 3) + procesa(file_nv, 2, "noise", 10, 3, 3)
        val key_nv_aver = procesa(file_nv, 0, "aver", 10, 3, 2) + procesa(file_nv, 1, "aver", 10, 3, 2) + procesa(file_nv, 2, "aver", 10, 3, 2)
        val key_v_noise = procesa(file_v, 0, "noise", 30, 1, 2) + procesa(file_v, 1, "noise", 30, 1, 2) + procesa(file_v, 2, "noise", 30, 1, 2)
        val key_v_aver = procesa(file_v, 0, "aver", 30, 1, 2) + procesa(file_v, 1, "aver", 30, 1, 2) + procesa(file_v, 2, "aver", 30, 1, 2)
        val key_g_noise = procesa(file_g, 0, "noise", 10, 3, 4) + procesa(file_g, 1, "noise", 10, 3, 4) + procesa(file_g, 2, "noise", 10, 3, 4)
        val key_g_aver = procesa(file_g, 0, "aver", 10, 3, 5) + procesa(file_g, 1, "aver", 10, 3, 5) + procesa(file_g, 2, "aver", 10, 3, 5)

        val key_main = key_nv_noise + key_nv_aver + key_v_noise + key_v_aver + key_g_noise + key_g_aver

        key_main
    }

}