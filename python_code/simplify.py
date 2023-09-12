import numpy as np


def filtrar(valores, x_sigmas):
    promedio = np.mean(valores)
    desviacion_estandar = np.std(valores)
    return [x for x in valores if abs(x - promedio) <= x_sigmas * desviacion_estandar]


# Read data
with open("gaussian_gray.txt") as f:
    data_gauss = [line.strip().split('\t') for line in f.readlines() if line != ""]


def decimal_to_gray(decimal_num):
    decimal_num = int(decimal_num)

    # In case precision is not adjusted
    while decimal_num < -100 or 100 < decimal_num:
        decimal_num = decimal_num / 10

    # To ensure 6-bit responses
    nbits = 6

    return [res.zfill(nbits) for min, max, res in data_gauss if float(min) <= decimal_num < float(max)][0]


def procesa(filename, col, var, tam_tramo, nsigmas, precision):
    # Load data
    with open(filename) as f:
        valores = [[float(x) for x in line.strip().split('\t')][col] for line in f.readlines() if line != ""]
    if col == 2:
        valores = [elemento - 9.8 for elemento in valores]

    # Select time interval
    valores = valores[500:2000]

    # Average estimation
    if var == "aver":
        # Average
        datos_aver = [np.mean(valores[i:i + tam_tramo]) for i in range(0, len(valores), tam_tramo)]
        datos_aver_filtered = filtrar(datos_aver, nsigmas)
        aver_filtered = np.mean(datos_aver_filtered)
        aver_filtered = int(aver_filtered * (10 ** precision))

        # Local key
        return decimal_to_gray(aver_filtered)

    # Average estimation
    if var == "noise":
        # Max estimation
        datos_max = [max(valores[i:i + tam_tramo]) for i in range(0, len(valores), tam_tramo)]
        datos_max_filtered = filtrar(datos_max, nsigmas)
        promedio_max_filtered = np.mean(datos_max_filtered)

        # Min estimation
        datos_min = [min(valores[i:i + tam_tramo]) for i in range(0, len(valores), tam_tramo)]
        datos_min_filtered = filtrar(datos_min, nsigmas)
        promedio_min_filtered = np.mean(datos_min_filtered)

        # Noise
        noise = abs(promedio_max_filtered - promedio_min_filtered)
        noise = int(noise * (10 ** precision))

        # Local key
        return decimal_to_gray(noise)


def main(file_nv, file_v, file_g):
    return ''.join(procesa(file, c, var, tam_tramo, nsigmas, precision) for file, var, tam_tramo, nsigmas, precision in [
        (file_nv, "noise", 10, 3, 3),
        (file_nv, "aver", 10, 3, 2),
        (file_v, "noise", 30, 1, 2),
        (file_v, "aver", 30, 1, 2),
        (file_g, "noise", 10, 3, 4),
        (file_g, "aver", 10, 3, 5),
    ] for c in range(3))


import unittest


class TestStringMethods(unittest.TestCase):
    def test_refactor(self):
        self.assertEqual(
            main("example.txt", "example2.txt", "example3.txt"),
            "110101110101111110101000000010011000101000111010011100111000000010011000110000110101110001001011000000000000"
        )


if __name__ == '__main__':
    unittest.main()
