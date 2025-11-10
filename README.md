# DES-APP-MOVIL1 · Veterinaria — Semana 3 · Sumativa N.º 1

Proyecto desarrollado en Kotlin para la asignatura **Desarrollo de Aplicaciones Móviles I (DUOC UC)**.  
Su objetivo es simular el módulo administrativo de una veterinaria: capturar datos de dueños y mascotas, planificar agendas, generar resúmenes ejecutivos y, desde la semana 3, integrar funcionalidades avanzadas (regex, rangos, anotaciones, reflection, sobrecarga de operadores y desestructuración).

## Ejecutar

```bash
# Desde la raíz del proyecto
# Opción 1: usar Gradle directamente (puedes responder desde la misma terminal)
./gradlew run --console=plain

# Opción 2: generar el script distribuible e invocarlo (recomendado para interacción)
./gradlew installDist
./build/install/Veterinaria/bin/Veterinaria
```

> Si prefieres compilar manualmente: `kotlinc src -include-runtime -d veterinaria.jar && java -jar veterinaria.jar`.  
> El punto de entrada es `cl.duoc.veterinaria.app.MainKt`.

## Funcionalidades destacadas de la semana

| Indicador | Implementación |
| --- | --- |
| Validaciones con Regex y Ranges | `InputUtils.leerEmail` exige formato `nombre@dominio.com`; `leerTelefono` formatea a `(XXX)XXXX-XXXX`. Las cantidades (1-100) se validan en `DetallePedido`, y `Pedido` aplica el descuento del 10 % cuando la fecha cae en `periodoPromocional`. |
| Anotaciones y Reflection | La anotación `@Promocionable` marca medicamentos con descuentos especiales. `ReflectionUtils.describir` se usa en la simulación de farmacia para listar propiedades y métodos del pedido combinado. |
| Sobrecarga de operadores | `Pedido` implementa `operator fun plus` para combinar pedidos. `Medicamento` redefine `equals/hashCode` para comparar por nombre/dosificación. |
| Desestructuración | `Cliente` (data class) y `Pedido` (`component1/2/3`) se desestructuran para obtener nombre/correo/teléfono y cliente/productos/total respectivamente. |
| Evaluación de igualdad | `Cliente` y `Medicamento` sobrescriben `equals/hashCode` para evitar duplicados al combinar pedidos o registrar nuevas entradas. |
| Resumen integrado | Después del flujo principal se imprime un resumen completo que muestra: datos del cliente capturado, planificación por bloques, panel de consultas, simulación de pedidos combinados, productos promocionales, reflection, etc. |

## Flujo resumido

1. **Captura interactiva:** se ingresan datos del dueño (con validaciones), mascotas, tipo de servicio y comentarios.
2. **Agenda inteligente:** se propone un horario y se permite elegir veterinario con disponibilidad continua para todas las mascotas.
3. **Resumen y panel:** se imprime un informe completo para el cliente y se actualiza el panel administrativo.
4. **Simulación de farmacia:** se combinan pedidos con `+`, se listan promociones `@Promocionable`, se muestra el cliente desestructurado y se inspecciona el pedido con reflection.

## Estructura principal

```
src/
└── main
    ├── kotlin/cl/duoc/veterinaria
    │   ├── app (Main, controladores, resumen)
    │   ├── model (Mascota, Pedido, anotaciones, etc.)
    │   ├── service (Agenda, cálculos de consultas/mascotas)
    │   └── util (InputUtils, ReflectionUtils, extensiones)
    └── resources
        └── data/veterinarios.csv
```

## Próximos pasos sugeridos

- Integrar persistencia (archivos o base de datos) para guardar consultas y pedidos reales.
- Agregar pruebas automatizadas para `InputUtils`, `Pedido` y `AgendaVeterinario`.
- Explorar una interfaz gráfica o API REST reutilizando la lógica ya implementada.

---

**Autoría:** Liliana Tapia Urra · Estudiante de Desarrollo de Aplicaciones Móviles – DUOC UC 2025
