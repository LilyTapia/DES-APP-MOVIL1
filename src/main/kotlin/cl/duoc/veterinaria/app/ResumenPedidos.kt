package cl.duoc.veterinaria.app

import cl.duoc.veterinaria.model.Cliente
import cl.duoc.veterinaria.model.DetallePedido
import cl.duoc.veterinaria.model.Medicamento
import cl.duoc.veterinaria.model.MedicamentoPromocional
import cl.duoc.veterinaria.model.Pedido
import cl.duoc.veterinaria.util.ReflectionUtils
import cl.duoc.veterinaria.util.formatearTelefonoEstandar
import java.time.LocalDate

class ResumenPedidos {
    fun generar(contexto: EjecucionModulo?) {
        println()
        println("=== SIMULACIÓN DE DESCUENTOS EN MEDICAMENTOS ===")

        val cliente = contexto?.let {
            Cliente(
                nombre = it.dueno.nombre,
                correo = it.dueno.email,
                telefono = it.dueno.telefono.formatearTelefonoEstandar()
            )
        } ?: Cliente(
            nombre = "Cliente demo",
            correo = "demo@veterinaria.cl",
            telefono = "56998765432".formatearTelefonoEstandar()
        )

        val (pedido1, pedido2) = crearPedidosPara(cliente, contexto)
        val pedidoCombinado = pedido1 + pedido2
        val periodo = pedidoCombinado.rangoPromocional()
        val (nombreCliente, correoCliente, telefonoCliente) = pedidoCombinado.cliente

        val etiquetaCliente = if (contexto != null) "registrado en la última consulta" else "demo"
        println("Cliente $etiquetaCliente: ${pedidoCombinado.cliente.nombre}")
        println("Promoción activa del ${periodo.start} al ${periodo.endInclusive}")
        println("Contacto desestructurado: $nombreCliente | $correoCliente | $telefonoCliente")

        val subtotal = pedidoCombinado.totalSinPromocion()
        val total = pedidoCombinado.total
        val ahorro = subtotal - total
        println("Monto sin promoción : $${"%,.0f".format(subtotal)}")
        println("Monto con promoción : $${"%,.0f".format(total)}")
        if (ahorro > 0) println("Ahorro aplicado    : $${"%,.0f".format(ahorro)}")

        val promocionables = pedidoCombinado.detalles
            .filter { it.medicamento.tieneAnotacionPromocionable() != null }
        if (promocionables.isNotEmpty()) {
            println("\nMedicamentos elegibles:")
            println(String.format("%-24s %-10s %-10s", "Producto", "Precio", "Promo %"))
            promocionables.forEach {
                val porcentaje = ((it.medicamento.tieneAnotacionPromocionable()?.descuento ?: 0.0) * 100).toInt()
                val precio = "$${"%,.0f".format(it.medicamento.precio)}"
                println(String.format("%-24s %-10s %2d%%", it.medicamento.nombre, precio, porcentaje))
            }
        }

        println("\nInspección rápida del pedido (reflection):")
        println(ReflectionUtils.describir(pedidoCombinado))
    }

    private fun crearPedidosPara(cliente: Cliente, contexto: EjecucionModulo?): Pair<Pedido, Pedido> {
        val medicamentos = construirCatalogo(contexto)

        val pedidoUno = Pedido(
            cliente = cliente,
            detalles = medicamentos.take(2).map { DetallePedido(it, 2) },
            fecha = LocalDate.now()
        )

        val pedidoDos = Pedido(
            cliente = cliente,
            detalles = medicamentos.drop(1).take(2).map { DetallePedido(it, 3) },
            fecha = LocalDate.now().plusDays(1)
        )
        return pedidoUno to pedidoDos
    }

    private fun construirCatalogo(contexto: EjecucionModulo?): List<Medicamento> {
        val nombreMascota = contexto?.mascotas?.firstOrNull()?.nombre ?: "Mascotas"
        val servicio = contexto?.tipoServicio?.descripcion ?: "Control"
        val analgesico = MedicamentoPromocional("$servicio Premium", 25, 15000.0, descuento = 0.15)
        val antibiotico = Medicamento("Antibiótico $nombreMascota", 50, 12000.0)
        val vitaminas = MedicamentoPromocional("Refuerzo vitamínico", 5, 8000.0)
        return listOf(analgesico, antibiotico, vitaminas)
    }

}
