package cl.duoc.veterinaria.model

import java.time.LocalDate

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Promocionable(val descuento: Double = 0.1)

data class Cliente(
    val nombre: String,
    val correo: String,
    val telefono: String
) {
    init {
        require(nombre.isNotBlank()) { "El nombre del cliente no puede estar vacío." }
        require(correo.isNotBlank()) { "El correo del cliente es obligatorio." }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Cliente) return false
        return nombre.equals(other.nombre, ignoreCase = true) &&
            correo.equals(other.correo, ignoreCase = true)
    }

    override fun hashCode(): Int =
        31 * nombre.lowercase().hashCode() + correo.lowercase().hashCode()
}

open class Medicamento(
    val nombre: String,
    val dosisMg: Int,
    val precio: Double
) {
    open fun precioConAjustes(): Double = precio

    fun tieneAnotacionPromocionable(): Promocionable? =
        this::class.java.getAnnotation(Promocionable::class.java)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Medicamento) return false
        return nombre.equals(other.nombre, ignoreCase = true) &&
            dosisMg == other.dosisMg
    }

    override fun hashCode(): Int =
        37 * nombre.lowercase().hashCode() + dosisMg

    override fun toString(): String =
        "$nombre ${dosisMg}mg - $${"%,.0f".format(precio)}"
}

@Promocionable(descuento = 0.15)
class MedicamentoPromocional(
    nombre: String,
    dosisMg: Int,
    precio: Double,
    private val descuento: Double = 0.15
) : Medicamento(nombre, dosisMg, precio) {

    override fun precioConAjustes(): Double =
        precio * (1 - descuento)

    fun porcentajeDescuento(): Double = descuento
}

data class DetallePedido(val medicamento: Medicamento, val cantidad: Int) {
    init {
        require(cantidad in 1..100) { "La cantidad debe estar entre 1 y 100 unidades." }
    }

    val subtotal: Double get() = medicamento.precioConAjustes() * cantidad
}

class Pedido(
    val cliente: Cliente,
    detalles: List<DetallePedido>,
    val fecha: LocalDate = LocalDate.now()
) {
    private val detallesInternos = detalles.toMutableList()

    val detalles: List<DetallePedido> get() = detallesInternos.toList()

    private val periodoPromocional: ClosedRange<LocalDate> =
        LocalDate.now().minusDays(2)..LocalDate.now().plusDays(5)

    private val montoBase: Double
        get() = detallesInternos.sumOf { it.subtotal }

    val total: Double
        get() {
            val suma = montoBase
            return if (fecha in periodoPromocional) suma * 0.9 else suma
        }

    fun totalSinPromocion(): Double = montoBase

    fun agregarDetalle(detalle: DetallePedido) {
        val existente = detallesInternos.indexOfFirst { it.medicamento == detalle.medicamento }
        detallesInternos += if (existente == -1) detalle else {
            val anterior = detallesInternos.removeAt(existente)
            DetallePedido(anterior.medicamento, anterior.cantidad + detalle.cantidad)
        }
    }

    fun rangoPromocional(): ClosedRange<LocalDate> = periodoPromocional

    operator fun plus(other: Pedido): Pedido {
        require(cliente == other.cliente) {
            "Solo es posible combinar pedidos pertenecientes al mismo cliente."
        }
        val combinados = (detalles + other.detalles)
            .groupBy { it.medicamento }
            .values
            .map { lista ->
                val cantidadTotal = lista.sumOf { it.cantidad }
                DetallePedido(lista.first().medicamento, cantidadTotal)
            }
        val fechaMasAntigua = if (fecha <= other.fecha) fecha else other.fecha
        return Pedido(cliente, combinados, fechaMasAntigua)
    }

    operator fun component1() = cliente
    operator fun component2() = detalles
    operator fun component3() = total
}
