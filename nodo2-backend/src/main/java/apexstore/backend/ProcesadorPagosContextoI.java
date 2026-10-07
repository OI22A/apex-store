package apexstore.backend;

import apexstore.*;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.LocalException;

import java.util.HashMap;
import java.util.Map;

// Provee IServicioPagos. Requiere IEstrategiaPago (una por medio de pago).
// Solo elige la estrategia y despacha: no persiste ni espera resultados.
public class ProcesadorPagosContextoI implements IServicioPagos {

    private static final int UMBRAL_FALLOS = 3;
    private static final long ABIERTO_MS = 10_000;
    private static final int TIMEOUT_MS = 2_000;

    // Una entrada por medio: agregar un medio nuevo es agregar una entrada (RAS-04)
    private final Map<MedioPago, Destino> destinos = new HashMap<>();

    public ProcesadorPagosContextoI(Map<MedioPago, IEstrategiaPagoPrx> estrategias) {
        estrategias.forEach((medio, prx) -> destinos.put(medio,
                // Tiempo máximo de espera por despacho: ningún hilo queda retenido
                new Destino(IEstrategiaPagoPrx.uncheckedCast(prx.ice_invocationTimeout(TIMEOUT_MS)))));
    }

    @Override
    public boolean iniciarPago(SolicitudPago solicitud, Current current) {
        Destino d = destinos.get(solicitud.medio);
        if (d == null) {
            return false;
        }
        if (d.abierto()) {
            // Aislamiento de fallos: no se le envía nada a una estrategia caída
            System.out.println("[Contexto] " + solicitud.medio + " no disponible, se rechaza el despacho");
            return false;
        }
        try {
            boolean acuse = d.prx.procesarPago(solicitud);
            d.exito();
            return acuse;
        } catch (LocalException e) {
            d.fallo();
            System.out.println("[Contexto] fallo al despachar a " + solicitud.medio + ": " + e.getClass().getSimpleName());
            return false;
        }
    }

    // Estado de salud de cada estrategia (corta el despacho tras fallos consecutivos)
    private static class Destino {
        final IEstrategiaPagoPrx prx;
        private int fallos;
        private long abiertoHasta;

        Destino(IEstrategiaPagoPrx prx) { this.prx = prx; }

        synchronized boolean abierto() { return System.currentTimeMillis() < abiertoHasta; }
        synchronized void exito() { fallos = 0; }
        synchronized void fallo() {
            if (++fallos >= UMBRAL_FALLOS) {
                abiertoHasta = System.currentTimeMillis() + ABIERTO_MS;
                fallos = 0;
            }
        }
    }
}