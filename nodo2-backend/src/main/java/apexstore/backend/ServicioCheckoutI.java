package apexstore.backend;

import apexstore.*;
import com.zeroc.Ice.Current;

import java.util.UUID;

// Provee IGestionCompras e IEstadoOrden.
// Requiere IServicioPagos (contexto) e IRepositorioTransacciones (BD).
public class ServicioCheckoutI implements IGestionCompras {

    private final IServicioPagosPrx pagos;               // requerida (socket)
    private final IRepositorioTransaccionesPrx repositorio; // requerida (socket)

    public ServicioCheckoutI(IServicioPagosPrx pagos, IRepositorioTransaccionesPrx repositorio) {
        this.pagos = pagos;
        this.repositorio = repositorio;
    }

    @Override
    public String realizarCompra(SolicitudPago solicitud, Current current) {
        // El id lo genera el backend: identificador opaco de correlación
        String idOrden = UUID.randomUUID().toString();
        SolicitudPago conId = new SolicitudPago(idOrden, solicitud.medio, solicitud.monto,
                solicitud.moneda, solicitud.datos);

        // 1. La orden queda registrada en PENDIENTE antes de despachar
        repositorio.crearOrden(conId);

        // 2. Se despacha y solo se recibe el acuse, no el resultado
        boolean acuse = pagos.iniciarPago(conId);
        if (!acuse) {
            System.out.println("[Checkout] sin acuse para " + idOrden + ": queda PENDIENTE de verificación");
        }
        return idOrden;
    }

    @Override
    public EstadoOrden consultarEstadoOrden(String idOrden, Current current)
            throws OrdenNoEncontrada {
        return repositorio.obtenerEstado(idOrden);
    }

    // Segundo servant del mismo componente: la interfaz IEstadoOrden
    public IEstadoOrden estadoOrden() {
        return new EstadoOrdenI();
    }

    private static class EstadoOrdenI implements IEstadoOrden {
        @Override
        public void ordenActualizada(String idOrden, EstadoOrden estado, Current current) {
            System.out.println("[Checkout] orden " + idOrden + " actualizada -> " + estado);
        }
    }
}