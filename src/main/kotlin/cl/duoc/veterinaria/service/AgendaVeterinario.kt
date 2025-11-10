package cl.duoc.veterinaria.service

import cl.duoc.veterinaria.model.Veterinario
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object AgendaVeterinario {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val ocupados = mutableSetOf<LocalDateTime>()
    private val veterinarios = mutableListOf(
        Veterinario("Ana Lopez", "555-2000", "ana@veterinaria.cl", "General"),
        Veterinario("Benjamin Ruiz", "555-2001", "benjamin@veterinaria.cl", "Cirugia"),
        Veterinario("Carla Soto", "555-2002", "carla@veterinaria.cl", "Exoticos")
    )
    private val agenda: MutableMap<Veterinario, MutableSet<LocalDateTime>> =
        veterinarios.associateWith { mutableSetOf<LocalDateTime>() }.toMutableMap()

    private fun agendaPara(veterinario: Veterinario) =
        agenda.getOrPut(veterinario) { mutableSetOf() }

    fun registrarVeterinarios(nuevos: List<Veterinario>) {
        nuevos.forEach { vet ->
            if (veterinarios.none { it.nombre == vet.nombre }) {
                veterinarios.add(vet)
                agendaPara(vet)
            }
        }
    }

    private fun esDiaHabil(dt: LocalDateTime) =
        dt.dayOfWeek in DayOfWeek.MONDAY..DayOfWeek.FRIDAY

    private fun normalizarAHora(dt: LocalDateTime) =
        dt.withMinute(0).withSecond(0).withNano(0)

    fun siguienteSlotHabil(from: LocalDateTime): LocalDateTime {
        var p = normalizarAHora(from)
        if (from.minute != 0) p = p.plusHours(1)

        while (true) {
            if (!esDiaHabil(p)) {
                p = p.plusDays(1).withHour(8).withMinute(0)
                continue
            }
            if (p.hour < 8) {
                p = p.withHour(8)
                continue
            }
            if (p.hour >= 18) {
                p = p.plusDays(1).withHour(8)
                continue
            }
            return p
        }
    }

    fun siguienteSlotHabil(clock: Clock, horasDesde: Long = 0): LocalDateTime =
        siguienteSlotHabil(LocalDateTime.now(clock).plusHours(horasDesde))

    fun estaDisponible(fh: LocalDateTime) = fh !in ocupados

    fun veterinariosDisponiblesEn(fh: LocalDateTime): List<Veterinario> =
        agenda.filter { fh !in it.value }.keys.toList()

    fun reservar(fh: LocalDateTime): Veterinario? {
        val disponible = veterinariosDisponiblesEn(fh).find { true }
        return disponible?.also { bloquearSlot(it, fh) }
    }

    private fun bloquearSlot(veterinario: Veterinario, slot: LocalDateTime) {
        ocupados.add(slot)
        agendaPara(veterinario).add(slot)
    }

    fun tieneDisponibilidad(veterinario: Veterinario, inicio: LocalDateTime, bloques: Int): Boolean =
        (0 until bloques).all { idx ->
            val slot = inicio.plusHours(idx.toLong())
            slot !in agendaPara(veterinario)
        }

    fun reservarBloqueConVeterinario(
        veterinario: Veterinario,
        inicio: LocalDateTime,
        bloques: Int
    ): Pair<Veterinario?, List<LocalDateTime>> {
        val slots = (0 until bloques).map { inicio.plusHours(it.toLong()) }
        if (!tieneDisponibilidad(veterinario, inicio, bloques)) return null to emptyList()
        slots.forEach { bloquearSlot(veterinario, it) }
        return veterinario to slots
    }

    fun reservarBloque(inicio: LocalDateTime, bloques: Int): Pair<Veterinario?, List<LocalDateTime>> {
        if (bloques <= 1) {
            val vet = reservar(inicio)
            return vet?.let { it to listOf(inicio) } ?: (null to emptyList())
        }
        val candidato = veterinarios.firstOrNull { tieneDisponibilidad(it, inicio, bloques) }
        return if (candidato != null) {
            reservarBloqueConVeterinario(candidato, inicio, bloques)
        } else {
            val vet = reservar(inicio)
            vet?.let { it to listOf(inicio) } ?: (null to emptyList())
        }
    }

    fun sugerirSiguiente(desde: LocalDateTime): LocalDateTime {
        var p = siguienteSlotHabil(desde.plusHours(1))
        repeat(100) {
            if (estaDisponible(p)) return p
            p = siguienteSlotHabil(p.plusHours(1))
        }
        return siguienteSlotHabil(desde.plusDays(1))
    }

    fun agendaPorVeterinario(): Map<String, List<LocalDateTime>> =
        agenda.mapKeys { (veterinario, _) -> veterinario.nombre }
            .mapValues { (_, horas) -> horas.sorted() }

    fun fmt(fh: LocalDateTime) = fh.format(formatter)
}
