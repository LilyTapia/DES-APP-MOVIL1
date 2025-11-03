package cl.duoc.veterinaria.service

import cl.duoc.veterinaria.model.TipoServicio
import kotlin.math.round

object ConsultaService {
    fun calcularCostoBase(tipo: TipoServicio, minutos: Int): Double {
        val base = when (tipo) {
            TipoServicio.CONTROL -> 15000.0
            TipoServicio.VACUNA -> 12000.0
            TipoServicio.URGENCIA -> 25000.0
            TipoServicio.OTRO -> 18000.0
        }
        val recargo = (minutos / 30) * 2000.0
        return base + recargo
    }

    fun aplicarDescuento(costo: Double, cantidad: Int): Pair<Double, Boolean> =
        if (cantidad > 1) costo * 0.85 to true else costo to false

    fun redondearClp(monto: Double) = round(monto)
}
