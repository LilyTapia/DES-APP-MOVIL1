package cl.duoc.veterinaria.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class Mascota(
    val nombre: String,
    val especie: String,
    val edad: Int,
    val pesoKg: Double,
    val ultimaVacunacion: LocalDate?
) {
    fun mostrarInformacion(): String {
        val ultimaVacunaTexto = ultimaVacunacion?.toString() ?: "Sin registro"
        val pesoTexto = String.format(Locale.US, "%.1f", pesoKg)
        return "$nombre ($especie) | Edad: $edad años | Peso: $pesoTexto kg | Última vacuna: $ultimaVacunaTexto"
    }
}

open class Usuario(
    open val nombre: String,
    open val telefono: String,
    open val email: String
) {
    fun emailValidoOrNull(): String? =
        email.takeIf { EMAIL_REGEX.matches(it) && it != EMAIL_POR_DEFECTO }

    fun emailSeguro(): String = emailValidoOrNull() ?: EMAIL_POR_DEFECTO

    companion object {
        const val EMAIL_POR_DEFECTO = "correo@invalido.com"
        private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}

data class Dueno(
    override val nombre: String,
    override val telefono: String,
    override val email: String
) : Usuario(nombre, telefono, email)

data class Veterinario(
    override val nombre: String,
    override val telefono: String,
    override val email: String,
    val especialidad: String
) : Usuario(nombre, telefono, email)

enum class TipoServicio(val descripcion: String) {
    CONTROL("Control"),
    VACUNA("Vacuna"),
    URGENCIA("Urgencia"),
    OTRO("Otro");

    val clave get() = descripcion.lowercase()
}

enum class EstadoReserva { CONFIRMADA, NO_CONFIRMADA }

enum class EstadoConsulta { PENDIENTE, REALIZADA }

data class Consulta(
    val idConsulta: String,
    val descripcion: String,
    var costoConsulta: Double,
    var estado: EstadoConsulta,
    var fechaAtencion: LocalDateTime? = null,
    var comentarios: String? = null,
    var veterinarioAsignado: Veterinario? = null
) {
    fun calcularCostoFinalConDescuento(porcentaje: Double): Double {
        val factor = 1 - porcentaje.coerceIn(0.0, 1.0)
        return costoConsulta * factor
    }

    fun actualizarEstado(nuevoEstado: EstadoConsulta, fecha: LocalDateTime? = null) {
        estado = nuevoEstado
        fechaAtencion = fecha
    }

    fun resumenFormateado(dueno: Dueno, mascotas: List<Mascota>): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val fechaTexto = fechaAtencion?.format(formatter) ?: "Pendiente"
        val comentariosTexto = comentarios?.takeIf { it.isNotBlank() } ?: "Sin comentarios"
        val veterinarioTexto = veterinarioAsignado?.nombre ?: "No asignado"
        val detallesMascotas = mascotas.joinToString(separator = "\n") { "  - ${it.mostrarInformacion()}" }

        return buildString {
            appendLine("Cliente    : ${dueno.nombre}")
            appendLine("Teléfono   : ${dueno.telefono}")
            appendLine("Email      : ${dueno.email}")
            appendLine("Veterinario: $veterinarioTexto")
            appendLine("Estado     : ${estado.descripcionCorta()}")
            appendLine("Fecha aten.: $fechaTexto")
            appendLine("Mascotas   :")
            appendLine(detallesMascotas)
            append("Comentarios: $comentariosTexto")
        }
    }
}

fun EstadoConsulta.descripcionCorta(): String = formatearNombreEnum(name)

fun EstadoReserva.descripcionCorta(): String = formatearNombreEnum(name)

private fun formatearNombreEnum(valor: String): String =
    valor.lowercase()
        .replace('_', ' ')
        .split(' ')
        .joinToString(" ") { palabra ->
            if (palabra.isEmpty()) palabra else palabra[0].uppercaseChar() + palabra.substring(1)
        }
