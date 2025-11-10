package cl.duoc.veterinaria.util

import cl.duoc.veterinaria.model.Usuario
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.NoSuchElementException

class EntradaNoDisponibleException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

object InputUtils {
    private val FECHA_HORA_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val FECHA_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    private val NOMBRE_REGEX = Regex("^[A-Za-zÁÉÍÓÚáéíóúÑñÜü\\s'-]+$")

    fun leerLinea(prompt: String): String =
        try {
            leerDesdeConsola(prompt)
        } catch (ex: EntradaNoDisponibleException) {
            println("Aviso: ${ex.message}. Se asumirá una entrada vacía.")
            ""
        }

    fun leerEmail(prompt: String): String {
        while (true) {
            val input = leerLinea(prompt)
            if (input.isBlank()) {
                println("Debes ingresar un correo válido.")
                continue
            }
            if (EMAIL_REGEX.matches(input.trim())) return input.trim()
            println("Correo inválido. Usa el formato nombre@dominio.com.")
        }
    }

    fun leerTelefono(prompt: String, defecto: String = "+56 (000) 000-0000"): String {
        while (true) {
            val input = leerLinea(prompt)
            if (input.isEmpty()) return defecto
            val digitos = input.filter(Char::isDigit)
            if (digitos.length < 10) {
                println("Teléfono inválido. Ingresa al menos 10 dígitos.")
                continue
            }
            return digitos.formatearTelefonoEstandar()
        }
    }

    fun leerNombre(prompt: String, defecto: String = "SinNombre"): String {
        while (true) {
            val input = leerLinea(prompt)
            if (input.isEmpty()) return defecto
            if (NOMBRE_REGEX.matches(input)) return input.trim()
            println("Nombre inválido. Usa solo letras, espacios o guiones.")
        }
    }

    fun leerEntero(
        prompt: String,
        defecto: Int,
        validator: (Int) -> Boolean = { true },
        mensajeValidacion: String = "El valor no cumple con el rango esperado."
    ): Int {
        while (true) {
            val input = try {
                leerDesdeConsola(prompt)
            } catch (ex: EntradaNoDisponibleException) {
                println("Aviso: ${ex.message}. Se usará el valor por defecto ($defecto).")
                return defecto
            }
            if (input.isEmpty()) return defecto
            val numero = input.toIntOrNull()
            if (numero == null) {
                println("Ingrese un número válido.")
                continue
            }
            if (!validator(numero)) {
                println(mensajeValidacion)
                continue
            }
            return numero
        }
    }

    fun leerDouble(
        prompt: String,
        defecto: Double,
        validator: (Double) -> Boolean = { true },
        mensajeValidacion: String = "El valor no cumple con el rango esperado."
    ): Double {
        while (true) {
            val input = try {
                leerDesdeConsola(prompt)
            } catch (ex: EntradaNoDisponibleException) {
                println("Aviso: ${ex.message}. Se usará el valor por defecto ($defecto).")
                return defecto
            }
            val normalizado = input.replace(",", ".")
            if (input.isEmpty()) return defecto
            val numero = normalizado.toDoubleOrNull()
            if (numero == null) {
                println("Ingrese un número válido.")
                continue
            }
            if (!validator(numero)) {
                println(mensajeValidacion)
                continue
            }
            return numero
        }
    }

    fun leerFechaOpcional(prompt: String): LocalDate? {
        while (true) {
            val input = try {
                leerDesdeConsola(prompt)
            } catch (ex: EntradaNoDisponibleException) {
                println("Aviso: ${ex.message}. Se omitirá el ingreso de fecha.")
                return null
            }
            if (input.isEmpty()) return null
            try {
                return LocalDate.parse(input, FECHA_FORMATTER)
            } catch (ex: DateTimeParseException) {
                println("Formato inválido. Usa el formato yyyy-MM-dd.")
            }
        }
    }

    fun leerFechaHora(
        prompt: String,
        formatter: DateTimeFormatter = FECHA_HORA_FORMATTER,
        validator: (LocalDateTime) -> Boolean = { true },
        mensajeValidacion: String = "La fecha/hora ingresada no es válida para la agenda."
    ): LocalDateTime {
        while (true) {
            val input = try {
                leerDesdeConsola(prompt)
            } catch (ex: EntradaNoDisponibleException) {
                println("Error al leer la fecha/hora: ${ex.message}.")
                continue
            }
            if (input.isBlank()) {
                println("Debes ingresar una fecha y hora.")
                continue
            }
            try {
                val fechaHora = LocalDateTime.parse(input, formatter)
                if (!validator(fechaHora)) {
                    println(mensajeValidacion)
                    continue
                }
                return fechaHora
            } catch (ex: DateTimeParseException) {
                println("Formato inválido. Usa el formato yyyy-MM-dd HH:mm.")
            }
        }
    }

    fun leerFechaHoraOpcional(
        prompt: String,
        defecto: LocalDateTime,
        formatter: DateTimeFormatter = FECHA_HORA_FORMATTER,
        validator: (LocalDateTime) -> Boolean = { true },
        mensajeValidacion: String = "La fecha/hora ingresada no es válida para la agenda."
    ): LocalDateTime {
        while (true) {
            val input = try {
                leerDesdeConsola(prompt)
            } catch (ex: EntradaNoDisponibleException) {
                println("Aviso: ${ex.message}. Se mantendrá la fecha sugerida.")
                return defecto
            }
            if (input.isEmpty()) return defecto
            try {
                val fechaHora = LocalDateTime.parse(input, formatter)
                if (!validator(fechaHora)) {
                    println(mensajeValidacion)
                    continue
                }
                return fechaHora
            } catch (ex: DateTimeParseException) {
                println("Formato inválido. Usa el formato yyyy-MM-dd HH:mm.")
            }
        }
    }

    private fun leerDesdeConsola(prompt: String): String {
        print(prompt)
        return try {
            readln().trim()
        } catch (ex: NoSuchElementException) {
            throw EntradaNoDisponibleException("No se encontró la entrada del usuario.", ex)
        } catch (ex: IllegalStateException) {
            throw EntradaNoDisponibleException("La entrada estándar no está disponible.", ex)
        }
    }
}
