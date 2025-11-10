package cl.duoc.veterinaria.app

fun main() {
    runCatching {
        RegistroConsultasControlador().ejecutar()
    }.onFailure {
        println("Aviso: no fue posible completar el módulo interactivo (${it.message}).")
    }
}
