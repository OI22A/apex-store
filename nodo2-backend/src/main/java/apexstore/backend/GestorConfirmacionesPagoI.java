package apexstore.backend;

import apexstore.*;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.LocalException;

// Provee INotificacionPago. Requiere IRepositorioTransacciones e IEstadoOrden.
public class GestorConfirmacionesPagoI implements INotificacionPago {

    private final IRepositorioTransaccionesPrx repo;   // requerida (socket)
    private final IEstadoOrdenPrx orden;               // requerida (socket)

    public GestorConfirmacionesPagoI(IRepositorioTransaccionesPrx repo, IEstadoOrdenPrx orden) {
        this.repo = repo;
        this.orden = orden;
    }

    @Override
    public void notificarResultado(String idOrden, ResultadoPago resultado,
                                   String detalle, Current current) {
        // 1. Traducir el resultado de la pasarela al estado de la orden
        EstadoOrden nuevoEstado = (resultado == ResultadoPago.APROBADO)
                ? EstadoOrden.PAGADA
                : EstadoOrden.FALLIDA;

        try {
            // 2. Persistir. La BD solo aplica el cambio si la orden sigue en PENDIENTE
            boolean aplicado = repo.actualizarEstado(idOrden, nuevoEstado);

            if (!aplicado) {
                // Callback repetido: ya estaba resuelta, no se vuelve a aplicar ni a notificar
                System.out.println("[Gestor] callback repetido para " + idOrden + ", se ignora");
                return;
            }

            // 3. Solo si se aplicó, avisar a ServicioCheckout
            System.out.println("[Gestor] " + idOrden + " -> " + nuevoEstado + " (" + detalle + ")");
            orden.ordenActualizada(idOrden, nuevoEstado);

        } catch (OrdenNoEncontrada e) {
            // Resultado de una orden que la BD no conoce: se registra y se descarta
            System.out.println("[Gestor] orden desconocida: " + e.idOrden);
        } catch (LocalException e) {
            // BD o Checkout inalcanzables: la orden queda como está (PENDIENTE de verificación)
            System.out.println("[Gestor] no se pudo procesar " + idOrden + ": "
                    + e.getClass().getSimpleName());
        }
    }
}