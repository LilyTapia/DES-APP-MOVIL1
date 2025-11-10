package cl.duoc.veterinaria.util

object ReflectionUtils {
    fun describir(entidad: Any): String {
        val clazz = entidad::class.java
        val propiedades = clazz.declaredFields
            .joinToString { it.name }
        val metodos = clazz.declaredMethods
            .filter { it.parameterCount == 0 }
            .joinToString { it.name }

        return buildString {
            appendLine("Clase: ${clazz.simpleName}")
            appendLine("Propiedades: ${if (propiedades.isBlank()) "N/A" else propiedades}")
            append("Métodos sin parámetros: ${if (metodos.isBlank()) "N/A" else metodos}")
        }
    }
}
