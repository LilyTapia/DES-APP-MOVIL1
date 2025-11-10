package cl.duoc.veterinaria.app

import cl.duoc.veterinaria.model.Consulta
import cl.duoc.veterinaria.model.Dueno
import cl.duoc.veterinaria.model.EstadoConsulta
import cl.duoc.veterinaria.model.EstadoReserva
import cl.duoc.veterinaria.model.Mascota
import cl.duoc.veterinaria.model.TipoServicio
import cl.duoc.veterinaria.model.Usuario
import cl.duoc.veterinaria.model.Veterinario
import cl.duoc.veterinaria.model.descripcionCorta
import cl.duoc.veterinaria.service.AgendaVeterinario
import cl.duoc.veterinaria.service.ConsultaService
import cl.duoc.veterinaria.service.MascotaService
import cl.duoc.veterinaria.util.InputUtils.leerDouble
import cl.duoc.veterinaria.util.InputUtils.leerEmail
import cl.duoc.veterinaria.util.InputUtils.leerEntero
import cl.duoc.veterinaria.util.InputUtils.leerFechaHoraOpcional
import cl.duoc.veterinaria.util.InputUtils.leerFechaOpcional
import cl.duoc.veterinaria.util.InputUtils.leerLinea
import cl.duoc.veterinaria.util.InputUtils.leerNombre
import cl.duoc.veterinaria.util.InputUtils.leerTelefono
import cl.duoc.veterinaria.util.oVacio
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Clock
import java.time.LocalDateTime


class RegistroConsultasControlador(
    private val clock: Clock = Clock.systemDefaultZone(),
    private val veterinarioDataPath: String = "/data/veterinarios.csv"
) {
    private val registrosConsultas = mutableListOf<RegistroConsulta>()

    fun ejecutar(): EjecucionModulo {
        println("=== Módulo Administrativo Veterinaria — Registro de Consultas ===")

        cargarVeterinariosAdicionales()

        val dueno = solicitarDatosDueno()
        val mascotas = registrarMascotas(solicitarCantidadMascotas())
        val tipoServicio = seleccionarTipoServicio()
        val minutos = leerEntero(
            prompt = "Minutos estimados de atención (ej: 30): ",
            defecto = 30,
            validator = { it in 10..180 },
            mensajeValidacion = "El tiempo debe estar entre 10 y 180 minutos."
        )

        val agenda = planificarAgenda()
        val consulta = prepararConsulta(dueno, mascotas, agenda, tipoServicio, minutos)
        val registroActual = registrosConsultas.last()
        val contexto = EjecucionModulo(
            dueno = dueno,
            mascotas = mascotas,
            tipoServicio = tipoServicio,
            consulta = consulta,
            agenda = agenda,
            fechasReservadas = registroActual.fechasReservadas
        )
        mostrarResumen(dueno, mascotas, tipoServicio, minutos, agenda, consulta)
        agendarRecordatorio(dueno)
        actualizarEstadoConsulta(consulta, registroActual.fechasReservadas)
        mostrarPanelConsultas()
        ofrecerMedicamentosPromocionales(contexto)
        return contexto
    }

    private fun cargarVeterinariosAdicionales() {
        val veterinariosAdicionales = cargarVeterinariosDesdeArchivo(veterinarioDataPath)
        if (veterinariosAdicionales.isNotEmpty()) {
            AgendaVeterinario.registrarVeterinarios(veterinariosAdicionales)
            println("Se cargaron ${veterinariosAdicionales.size} veterinarios adicionales.")
        }
    }

    private fun solicitarDatosDueno(): Dueno {
        val nombreDueno = leerNombre("Recepción → Nombre del cliente: ")
        val telefonoDueno = leerTelefono("Recepción → Teléfono de contacto: ")
        val emailIngresado = leerEmail("Recepción → Email (para recordatorios): ")

        val duenoTemporal = Dueno(
            nombre = nombreDueno,
            telefono = telefonoDueno,
            email = emailIngresado.oVacio(Usuario.EMAIL_POR_DEFECTO)
        )

        val emailSeguro = duenoTemporal.emailSeguro()
        if (duenoTemporal.emailValidoOrNull() == null) {
            println("Aviso: email inválido detectado. Se asignó ${Usuario.EMAIL_POR_DEFECTO}.")
        }

        return duenoTemporal.copy(email = emailSeguro)
    }

    private fun solicitarCantidadMascotas(): Int =
        leerEntero(
            prompt = "Recepción → ¿Cuántas mascotas atenderá?: ",
            defecto = 1,
            validator = { it in 1..5 },
            mensajeValidacion = "Puedes registrar entre 1 y 5 mascotas por atención."
        )

    private fun registrarMascotas(cantidad: Int): List<Mascota> {
        val mascotas = mutableListOf<Mascota>()
        repeat(cantidad) { index ->
            println("\nDatos de la mascota #${index + 1}:")
            val nombre = leerNombre("  Nombre: ")
            val especie = leerLinea("  Especie (Perro/Gato/...): ").oVacio("Desconocida")
            val edad = leerEntero(
                prompt = "  Edad (años): ",
                defecto = 0,
                validator = { it in 0..40 },
                mensajeValidacion = "Ingresa una edad entre 0 y 40 años."
            )
            val peso = leerDouble(
                prompt = "  Peso (kg): ",
                defecto = 0.0,
                validator = { it in 0.1..150.0 },
                mensajeValidacion = "El peso debe estar entre 0.1 y 150 kg."
            )
            val ultimaVacuna = leerFechaOpcional("  Última vacuna (yyyy-MM-dd) o Enter si no recuerda: ")
            val mascota = Mascota(nombre, especie, edad, peso, ultimaVacuna)
            mascotas += mascota
            mostrarResumenVacunacion(mascota)
        }
        return mascotas
    }

    private fun mostrarResumenVacunacion(mascota: Mascota) {
        val frecuencia = MascotaService.descripcionFrecuencia(mascota)
        val proxima = MascotaService.calcularProximaVacunacion(mascota)
        val dosis = String.format("%.2f", MascotaService.calcularDosisRecomendada(mascota.pesoKg, mascota.edad))
        println("  -> $frecuencia | Próxima vacuna: $proxima | Dosis estimada: $dosis ml")
    }

    private fun seleccionarTipoServicio(): TipoServicio {
        println("\nServicios disponibles:")
        println("1. Control")
        println("2. Vacuna")
        println("3. Urgencia")
        println("4. Otro")

        while (true) {
            when (leerLinea("Seleccione el número del servicio (1-4): ")) {
                "1" -> return TipoServicio.CONTROL
                "2" -> return TipoServicio.VACUNA
                "3" -> return TipoServicio.URGENCIA
                "4" -> return TipoServicio.OTRO
                else -> println("Opción inválida.")
            }
        }
    }

    private fun planificarAgenda(): AgendaResultado {
        val fechaHoraSugerida = AgendaVeterinario.siguienteSlotHabil(clock, 2L)
        println("\nAgenda → Fecha/hora sugerida automáticamente: ${AgendaVeterinario.fmt(fechaHoraSugerida)}")
        val fechaHoraIngresada = leerFechaHoraOpcional(
            prompt = "Si deseas otra fecha/hora, ingrésala con el formato yyyy-MM-dd HH:mm o presiona Enter para aceptar la sugerida: ",
            defecto = fechaHoraSugerida,
            validator = { !it.isBefore(LocalDateTime.now(clock)) },
            mensajeValidacion = "No es posible agendar fechas en el pasado."
        )
        val fechaHoraReservada = AgendaVeterinario.siguienteSlotHabil(fechaHoraIngresada)
        if (fechaHoraReservada != fechaHoraIngresada) {
            println("La fecha/hora ingresada se ajustó a ${AgendaVeterinario.fmt(fechaHoraReservada)} para cumplir con la agenda hábil.")
        }

        val disponibles = AgendaVeterinario.veterinariosDisponiblesEn(fechaHoraReservada)
        if (disponibles.isEmpty()) {
            println("No hay veterinarios libres en ese horario. Se sugerirá una nueva fecha.")
        } else {
            println("Veterinarios disponibles: ${disponibles.joinToString { it.nombre }}")
        }

        return AgendaResultado(
            fechaSugerida = fechaHoraSugerida,
            fechaIngresada = fechaHoraIngresada,
            fechaReservada = fechaHoraReservada,
            veterinariosDisponibles = disponibles
        )
    }

    private fun prepararConsulta(
        dueno: Dueno,
        mascotas: List<Mascota>,
        agenda: AgendaResultado,
        servicio: TipoServicio,
        minutos: Int
    ): Consulta {
        val idConsulta = leerLinea("ID de consulta (ej: C-001): ").oVacio("C-000")
        val motivoConsulta = leerLinea("Motivo / descripción: ").oVacio("Consulta general")
        val comentariosOpcionales = leerLinea("Comentarios adicionales (opcional): ").takeIf { it.isNotBlank() }

        val costoBase = ConsultaService.calcularCostoBase(servicio, minutos)
        val (montoConDescuento, descuentoAplicado) = ConsultaService.aplicarDescuento(costoBase, mascotas.size)
        val costoFinal = ConsultaService.redondearClp(montoConDescuento)

        val consulta = Consulta(
            idConsulta = idConsulta,
            descripcion = motivoConsulta,
            costoConsulta = costoFinal,
            estado = EstadoConsulta.PENDIENTE,
            comentarios = comentariosOpcionales
        )

        val bloquesNecesarios = mascotas.size.coerceAtLeast(1)
        val veterinarioSeleccionado = seleccionarVeterinario(agenda, bloquesNecesarios)
        val (veterinarioAsignado, slotsReservados) = if (veterinarioSeleccionado != null) {
            AgendaVeterinario.reservarBloqueConVeterinario(veterinarioSeleccionado, agenda.fechaReservada, bloquesNecesarios)
        } else {
            AgendaVeterinario.reservarBloque(agenda.fechaReservada, bloquesNecesarios)
        }
        val fechasConfirmadas = if (slotsReservados.isNotEmpty()) slotsReservados else listOf(agenda.fechaReservada)
        val estadoReserva = if (veterinarioAsignado != null && slotsReservados.size == bloquesNecesarios) {
            EstadoReserva.CONFIRMADA
        } else {
            EstadoReserva.NO_CONFIRMADA
        }
        consulta.veterinarioAsignado = veterinarioAsignado

        registrosConsultas += RegistroConsulta(
            dueno = dueno,
            mascotas = mascotas,
            consulta = consulta,
            fechasReservadas = fechasConfirmadas,
            minutos = minutos,
            tipoServicio = servicio,
            costoBase = costoBase,
            costoFinal = costoFinal,
            descuentoAplicado = descuentoAplicado,
            estadoReserva = estadoReserva
        )

        return consulta
    }

    private fun mostrarResumen(
        dueno: Dueno,
        mascotas: List<Mascota>,
        tipoServicio: TipoServicio,
        minutos: Int,
        agenda: AgendaResultado,
        consulta: Consulta
    ) {
        val registro = registrosConsultas.last()
        println()
        println(seccion("RESUMEN PARA EL CLIENTE"))
        println(consulta.resumenFormateado(dueno, mascotas))
        println(subSeccion("Detalle económico"))
        println("Servicio           : ${tipoServicio.descripcion}")
        println("Duración estimada  : $minutos minutos")
        println("Precio base        : ${formatearMonto(registro.costoBase)}")
        println("Descuento aplicado : ${if (registro.descuentoAplicado) "15%" else "No aplica"}")
        println("Total estimado     : ${formatearMonto(registro.costoFinal)}")

        println(subSeccion("Vacunación y dosis por mascota"))
        println(String.format("  %-18s %-24s %-12s %-12s", "Mascota", "Frecuencia", "Próxima", "Dosis"))
        println("-".repeat(ANCHO_PANTALLA))
        mascotas.forEach { mascota ->
            val frecuencia = MascotaService.descripcionFrecuencia(mascota)
            val proxima = MascotaService.calcularProximaVacunacion(mascota)
            val dosis = String.format("%.2f ml", MascotaService.calcularDosisRecomendada(mascota.pesoKg, mascota.edad))
            val fila = String.format(
                "  %-18s %-24s %-12s %-12s",
                truncar(mascota.nombre, 18),
                truncar(frecuencia, 24),
                proxima.toString(),
                dosis
            )
            println(fila)
        }

        println(subSeccion("Estado de la reserva"))
        println("Reserva            : ${registro.estadoReserva.descripcionCorta()}")
        if (registro.estadoReserva == EstadoReserva.CONFIRMADA) {
            registro.fechasReservadas.forEachIndexed { index, slot ->
                val etiqueta = if (registro.fechasReservadas.size == 1) "Fecha/hora" else "Bloque ${index + 1}"
                println(String.format("%-19s: %s", etiqueta, AgendaVeterinario.fmt(slot)))
            }
            consulta.veterinarioAsignado?.let {
                println("Veterinario        : ${it.nombre} (${it.especialidad})")
            }
        } else {
            val sugerido = AgendaVeterinario.sugerirSiguiente(registro.fechasReservadas.lastOrNull() ?: agenda.fechaReservada)
            println("Fecha sugerida     : ${AgendaVeterinario.fmt(sugerido)}")
            println("Acción             : Contactar a recepción para reagendar")
        }
        println("-".repeat(ANCHO_PANTALLA))
    }

    private fun agendarRecordatorio(dueno: Dueno) {
        dueno.emailValidoOrNull()?.let { correoValido ->
            println("Se programó un recordatorio al correo $correoValido.")
        } ?: println("No se enviará recordatorio por falta de email válido.")
    }

    private fun actualizarEstadoConsulta(consulta: Consulta, fechasReservadas: List<LocalDateTime>) {
        val principal = fechasReservadas.firstOrNull()
        val respuesta = leerLinea("\n¿La atención se realizó? (s/n): ").lowercase()
        when (respuesta) {
            "s" -> {
                val fechaRealizada = LocalDateTime.now(clock)
                consulta.actualizarEstado(EstadoConsulta.REALIZADA, fechaRealizada)
                println("\nConsulta marcada como REALIZADA.")
                println("Fecha de atención efectiva: ${AgendaVeterinario.fmt(fechaRealizada)}")
                println("¡Gracias por su visita!")
            }
            "n" -> {
                consulta.actualizarEstado(EstadoConsulta.PENDIENTE, null)
                println("\nLa atención NO se realizó. La consulta permanece en estado PENDIENTE.")
                principal?.let {
                    println("Fecha/hora aún confirmada: ${AgendaVeterinario.fmt(it)}")
                }
                val sugerido = AgendaVeterinario.sugerirSiguiente(fechasReservadas.lastOrNull() ?: LocalDateTime.now(clock))
                println("Si necesitas reprogramar, sugerencia hábil: ${AgendaVeterinario.fmt(sugerido)}")
            }
            else -> {
                println("\nRespuesta no válida. Se mantiene estado PENDIENTE.")
                principal?.let {
                    println("Fecha/hora confirmada: ${AgendaVeterinario.fmt(it)}")
                }
            }
        }
        println("===========================================================")
    }

    private fun mostrarPanelConsultas() {
        if (registrosConsultas.isEmpty()) return

        println()
        println(seccion("PANEL DE CONSULTAS"))
        println(String.format("%-6s %-20s %-24s %12s %-12s", "ID", "Cliente", "Mascotas", "Monto", "Estado"))
        println("-".repeat(ANCHO_PANTALLA))
        registrosConsultas.forEach { registro ->
            val consulta = registro.consulta
            val mascotasNombres = registro.mascotas.joinToString { it.nombre }
            val fila = String.format(
                "%-6s %-20s %-24s %12s %-12s",
                truncar(consulta.idConsulta, 6),
                truncar(registro.dueno.nombre, 20),
                truncar(mascotasNombres, 24),
                formatearMonto(consulta.costoConsulta),
                truncar(consulta.estado.descripcionCorta(), 12)
            )
            println(fila)
        }
        println("-".repeat(ANCHO_PANTALLA))

        val pendientes = registrosConsultas.filter { it.consulta.estado == EstadoConsulta.PENDIENTE }
        val realizadas = registrosConsultas.filter { it.consulta.estado == EstadoConsulta.REALIZADA }
        println("Resumen estados     : Pendientes ${pendientes.size} | Realizadas ${realizadas.size}")

        if (pendientes.isNotEmpty()) {
            println(subSeccion("Pendientes por veterinario"))
            val pendientesPorVeterinario = pendientes.groupBy { it.consulta.veterinarioAsignado?.nombre ?: "Sin asignar" }
            pendientesPorVeterinario.forEach { (veterinario, lista) ->
                val fechas = lista.flatMap { it.fechasReservadas }
                    .joinToString(", ") { AgendaVeterinario.fmt(it) }
                println(String.format("  %-18s | %s", truncar(veterinario, 18), fechas))
            }
        }

        println(subSeccion("Agenda por veterinario"))
        AgendaVeterinario.agendaPorVeterinario().forEach { (nombre, horas) ->
            val detalle = if (horas.isEmpty()) "Sin reservas" else horas.joinToString(", ") { AgendaVeterinario.fmt(it) }
            println(String.format("  %-18s | %s", truncar(nombre, 18), detalle))
        }
    }

    private fun ofrecerMedicamentosPromocionales(contexto: EjecucionModulo) {
        val respuesta = leerLinea("\nFarmacia → ¿Deseas revisar medicamentos promocionales para ${contexto.dueno.nombre}? (s/n): ").lowercase()
        if (respuesta == "s") {
            ResumenPedidos().generar(contexto)
        } else {
            println("Farmacia → Se omitió la simulación de medicamentos.\n")
        }
    }

    private fun seleccionarVeterinario(agenda: AgendaResultado, bloques: Int): Veterinario? {
        val disponibles = AgendaVeterinario.veterinariosDisponiblesEn(agenda.fechaReservada)
        val continuos = disponibles.filter { AgendaVeterinario.tieneDisponibilidad(it, agenda.fechaReservada, bloques) }
        if (continuos.isEmpty()) {
            println("No hay veterinarios con disponibilidad continua para todos los bloques. Se asignará automáticamente.")
            return null
        }
        println("\nVeterinarios disponibles para toda la atención:")
        continuos.forEachIndexed { index, vet ->
            println("${index + 1}. ${vet.nombre} (${vet.especialidad})")
        }
        val respuesta = leerLinea("Selecciona el número del veterinario o presiona Enter para asignación automática: ")
        val indice = respuesta.toIntOrNull()
        return if (indice != null && indice in 1..continuos.size) continuos[indice - 1] else null
    }
}

data class RegistroConsulta(
    val dueno: Dueno,
    val mascotas: List<Mascota>,
    val consulta: Consulta,
    val fechasReservadas: List<LocalDateTime>,
    val minutos: Int,
    val tipoServicio: TipoServicio,
    val costoBase: Double,
    val costoFinal: Double,
    val descuentoAplicado: Boolean,
    val estadoReserva: EstadoReserva
)

data class AgendaResultado(
    val fechaSugerida: LocalDateTime,
    val fechaIngresada: LocalDateTime,
    val fechaReservada: LocalDateTime,
    val veterinariosDisponibles: List<Veterinario>
)

data class EjecucionModulo(
    val dueno: Dueno,
    val mascotas: List<Mascota>,
    val tipoServicio: TipoServicio,
    val consulta: Consulta,
    val agenda: AgendaResultado,
    val fechasReservadas: List<LocalDateTime>
)

private const val ANCHO_PANTALLA = 78

private fun seccion(titulo: String) = crearBarra(titulo, '=')

private fun subSeccion(titulo: String) = crearBarra(titulo, '-')

private fun crearBarra(titulo: String, caracter: Char): String {
    val texto = " $titulo "
    if (texto.length >= ANCHO_PANTALLA) return texto.trim()
    val padding = ANCHO_PANTALLA - texto.length
    val izquierda = padding / 2
    val derecha = padding - izquierda
    val relleno = caracter.toString()
    return buildString {
        append(relleno.repeat(izquierda))
        append(texto)
        append(relleno.repeat(derecha))
    }
}

private fun truncar(texto: String, max: Int): String {
    if (texto.length <= max) return texto
    if (max <= 3) return texto.take(max)
    return texto.take(max - 3) + "..."
}

private fun formatearMonto(monto: Double): String =
    "$${"%,.0f".format(monto)} CLP"

private fun cargarVeterinariosDesdeArchivo(path: String): List<Veterinario> =
    try {
        val lineas = if (path.startsWith("/")) {
            RegistroConsultasControlador::class.java.getResourceAsStream(path)
                ?.bufferedReader()
                ?.readLines()
                ?: throw IOException("Recurso $path no encontrado.")
        } else {
            Files.readAllLines(Paths.get(path))
        }
        lineas
            .filter { it.isNotBlank() && !it.trim().startsWith("#") }
            .mapNotNull { linea ->
                val partes = linea.split(";")
                if (partes.size >= 4) {
                    Veterinario(
                        nombre = partes[0].trim(),
                        telefono = partes[1].trim(),
                        email = partes[2].trim().oVacio(Usuario.EMAIL_POR_DEFECTO),
                        especialidad = partes[3].trim().oVacio("General")
                    )
                } else {
                    null
                }
            }
    } catch (io: IOException) {
        println("Aviso: no fue posible cargar veterinarios externos (${io.message}). Se usará la lista por defecto.")
        emptyList()
    }
