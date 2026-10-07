# ApexStore: Tarea 1, Ingeniería de Software IV (Universidad Icesi, 2026-2)

 Jhon Edwin Escudero Arias: A00408255,  Anderson Olave Ibargüen: A00408142   

Implementación en Java con ZeroC ICE 3.7.10 del diagrama arquitectónico corregido (Punto 3).
Las pasarelas de pago están **completamente simuladas**: no se conecta a ningún servicio financiero real.

## Arquitectura

Cuatro nodos, cada uno un proceso Java independiente:

| Nodo | Módulo | Componentes | Puerto |
|---|---|---|---|
| 1. Dispositivos Clientes | `nodo1-clientes` | `WebApp`, `MobileApp` | (solo cliente) |
| 2. Backend Core | `nodo2-backend` | `ServicioCheckout`, `ProcesadorPagosContexto`, `GestorConfirmacionesPago` | 10002 |
| 3. Pasarelas y Estrategias | `nodo3-pasarelas` | `EstrategiaStripe`, `EstrategiaPSE`, `EstrategiaCripto` | 10003 |
| 4. Base de Datos Transaccional | `nodo4-bd` | `DBPostgreSQLTransacciones` (simulada) | 10004 |

El módulo `contratos` contiene `apexstore.ice`, compartido por todos los nodos. De ahí
`slice2java` genera las interfaces y proxies Java.

### Interfaces (diagrama → Slice)

| Interfaz | Provee | Requiere |
|---|---|---|
| `IGestionCompras` | `ServicioCheckout` | `WebApp`, `MobileApp` |
| `IServicioPagos` | `ProcesadorPagosContexto` | `ServicioCheckout` |
| `IEstrategiaPago` | las tres estrategias | `ProcesadorPagosContexto` |
| `INotificacionPago` | `GestorConfirmacionesPago` | las tres estrategias |
| `IEstadoOrden` | `ServicioCheckout` | `GestorConfirmacionesPago` |
| `IRepositorioTransacciones` | `DBPostgreSQLTransacciones` | `ServicioCheckout`, `GestorConfirmacionesPago` |

### Flujo de una compra

1. El cliente llama `realizarCompra` en `ServicioCheckout`.
2. `ServicioCheckout` genera el `idOrden` y registra la orden en **PENDIENTE** (`crearOrden`).
3. `ProcesadorPagosContexto` elige la estrategia y despacha. Solo recibe el **acuse**, no el resultado.
4. La estrategia simula la demora bancaria en un pool propio y notifica al Gestor (`notificarResultado`).
5. `GestorConfirmacionesPago` actualiza el estado en la BD (solo si sigue en PENDIENTE) y avisa a `ServicioCheckout`.
6. El cliente consulta `consultarEstadoOrden` hasta que deje de ser PENDIENTE.

## Requisitos

- Java 21
- ZeroC ICE 3.7.10 (`slice2java` debe estar en el `PATH`)
- Gradle: usar siempre el wrapper `./gradlew` (Gradle 8.12)

## Compilar

```bash
./gradlew clean build installDist
```

## Ejecutar

Orden de arranque: Nodo 4, Nodo 2, Nodo 3 y, por último, Nodo 1. Cada uno en su propia terminal:

```bash
./gradlew :nodo4-bd:run
./gradlew :nodo2-backend:run
./gradlew :nodo3-pasarelas:run
./gradlew :nodo1-clientes:run
```

El Nodo 1 lanza tres compras en paralelo (Stripe, PSE y Cripto). Salida esperada:

```
[WebApp] orden <id> -> PAGADA
[MobileApp] orden <id> -> PAGADA
[MobileApp] orden <id> -> PAGADA
```

Algunas pueden quedar `FALLIDA`: la simulación rechaza entre 5 % y 15 % de los pagos al azar.

## Simplificaciones de la simulación

- **HTTPS / Internet** se simula con TCP local, sin cifrado. En producción corresponde a endpoints `ssl`
  de IceSSL en un adapter público separado del adapter interno.
- **PostgreSQL** se simula con un mapa en memoria: los datos se pierden al reiniciar el Nodo 4.
- **Pasarelas** simuladas con una demora fija y un rechazo aleatorio.
- Todos los nodos corren en `localhost`, en puertos distintos.
- El cliente no recibe notificaciones push: consulta el estado de la orden.
- Si el Gestor no está disponible cuando una pasarela notifica, el resultado se pierde y la orden
  queda en PENDIENTE (pendiente de verificación).

## Estructura

```
apex-store/
├── contratos/          # apexstore.ice (Slice) + generación de código
├── nodo1-clientes/
├── nodo2-backend/
├── nodo3-pasarelas/
├── nodo4-bd/
└── docs/               # diagrama del Punto 3 y bitácora de uso de IAG
```

## Autores

- Anderson Olave Ibargüen (A00408142)
- Jhon Edwin Escudero Arias (A00408255)