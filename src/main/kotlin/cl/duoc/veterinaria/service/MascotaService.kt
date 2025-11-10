package cl.duoc.veterinaria.service

import cl.duoc.veterinaria.model.Mascota
import java.time.LocalDate

enum class FrecuenciaVacuna { ANUAL, SEMESTRAL, NO_REQUIERE }

object MascotaService {
    fun determinarFrecuenciaVacuna(mascota: Mascota): FrecuenciaVacuna =
        when {
            mascota.especie.equals("perro", ignoreCase = true) && mascota.edad < 2 -> FrecuenciaVacuna.SEMESTRAL
            mascota.especie.equals("perro", ignoreCase = true) -> FrecuenciaVacuna.ANUAL
            mascota.especie.equals("gato", ignoreCase = true) && mascota.edad <= 3 -> FrecuenciaVacuna.ANUAL
            mascota.especie.equals("gato", ignoreCase = true) -> FrecuenciaVacuna.SEMESTRAL
            mascota.especie.equals("huron", ignoreCase = true) -> FrecuenciaVacuna.ANUAL
            mascota.edad > 12 -> FrecuenciaVacuna.NO_REQUIERE
            else -> FrecuenciaVacuna.ANUAL
        }

    fun calcularProximaVacunacion(mascota: Mascota): LocalDate {
        val ultima = mascota.ultimaVacunacion ?: LocalDate.now()
        return when (determinarFrecuenciaVacuna(mascota)) {
            FrecuenciaVacuna.ANUAL -> ultima.plusYears(1)
            FrecuenciaVacuna.SEMESTRAL -> ultima.plusMonths(6)
            FrecuenciaVacuna.NO_REQUIERE -> ultima
        }
    }

    fun calcularDosisRecomendada(pesoKg: Double, edad: Int): Double {
        val factorEdad = when {
            edad < 1 -> 0.8
            edad > 10 -> 0.6
            else -> 1.0
        }
        return (pesoKg * 0.1 * factorEdad).coerceAtLeast(0.1)
    }

    fun descripcionFrecuencia(mascota: Mascota): String =
        when (val freq = determinarFrecuenciaVacuna(mascota)) {
            FrecuenciaVacuna.ANUAL -> "Vacuna anual requerida"
            FrecuenciaVacuna.SEMESTRAL -> "Vacuna semestral requerida"
            FrecuenciaVacuna.NO_REQUIERE -> "Vacuna no requerida"
        }
}
