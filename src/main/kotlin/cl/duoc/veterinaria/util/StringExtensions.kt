package cl.duoc.veterinaria.util

fun String?.oVacio(defecto: String): String =
    if (this.isNullOrBlank()) defecto else this

fun String.formatearTelefonoEstandar(): String {
    val digitos = filter(Char::isDigit)
    if (digitos.length < 10) return this
    val cuerpo = digitos.takeLast(10)
    val area = cuerpo.take(3)
    val resto = cuerpo.drop(3)
    return "(${area})${resto.take(4)}-${resto.takeLast(4)}"
}
