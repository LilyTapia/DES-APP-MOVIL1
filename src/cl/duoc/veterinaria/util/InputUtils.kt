package cl.duoc.veterinaria.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object InputUtils {
    private val FECHA_HORA_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val FECHA_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun leerLinea(prompt: String): String {
        print(prompt)
        return readln().trim()
    }

    fun leerEntero(prompt: String, defecto: Int): Int {
        while (true) {
            print(prompt)
            val input = readln().trim()
            if (input.isEmpty()) return defecto
            input.toIntOrNull()?.let { return it }
            println("Ingrese un número válido.")
        }
    }

    fun leerDouble(prompt: String, defecto: Double): Double {
        while (true) {
            print(prompt)
            val input = readln().trim().replace(",", ".")
            if (input.isEmpty()) return defecto
            input.toDoubleOrNull()?.let { return it }
            println("Ingrese un número válido.")
        }
    }

    fun leerFechaOpcional(prompt: String): LocalDate? {
        while (true) {
            print(prompt)
            val input = readln().trim()
            if (input.isEmpty()) return null
            try {
                return LocalDate.parse(input, FECHA_FORMATTER)
            } catch (ex: Exception) {
                println("Formato inválido. Usa el formato yyyy-MM-dd.")
            }
        }
    }

    fun leerFechaHora(
        prompt: String,
        formatter: DateTimeFormatter = FECHA_HORA_FORMATTER
    ): LocalDateTime {
        while (true) {
            print(prompt)
            val input = readln().trim()
            try {
                return LocalDateTime.parse(input, formatter)
            } catch (ex: Exception) {
                println("Formato inválido. Usa el formato yyyy-MM-dd HH:mm.")
            }
        }
    }

    fun leerFechaHoraOpcional(
        prompt: String,
        defecto: LocalDateTime,
        formatter: DateTimeFormatter = FECHA_HORA_FORMATTER
    ): LocalDateTime {
        while (true) {
            print(prompt)
            val input = readln().trim()
            if (input.isEmpty()) return defecto
            try {
                return LocalDateTime.parse(input, formatter)
            } catch (ex: Exception) {
                println("Formato inválido. Usa el formato yyyy-MM-dd HH:mm.")
            }
        }
    }
}
