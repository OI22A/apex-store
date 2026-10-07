package apexstore.pasarelas;

import apexstore.*;
import com.zeroc.Ice.Current;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

// Lógica común de las tres pasarelas simuladas.
// Provee IEstrategiaPago; requiere INotificacionPago (campo notificacion).
public abstract class EstrategiaBase implements IEstrategiaPago {

    private final String nombre;
    private final long demoraMs;          // simula el retardo bancario
    private final double probRechazo;     // simula pagos rechazados
    private final INotificacionPagoPrx notificacion; // requerida (socket)

    // Pool propio: el trabajo lento NO ocupa los hilos de ICE
    private final ExecutorService pool = Executors.newFixedThreadPool(8);

    protected EstrategiaBase(String nombre, long demoraMs, double probRechazo,
                             INotificacionPagoPrx notificacion) {
        this.nombre = nombre;
        this.demoraMs = demoraMs;
        this.probRechazo = probRechazo;
        this.notificacion = notificacion;
    }

    @Override
    public boolean procesarPago(SolicitudPago solicitud, Current current) {
        System.out.println("[" + nombre + "] acuse para orden " + solicitud.idOrden);

        // Se procesa en segundo plano y se responde el acuse de inmediato
        pool.submit(() -> {
            try {
                Thread.sleep(demoraMs); // simulación: NO se conecta a ningún servicio real
                boolean aprobado = ThreadLocalRandom.current().nextDouble() >= probRechazo;
                ResultadoPago resultado = aprobado ? ResultadoPago.APROBADO : ResultadoPago.RECHAZADO;
                notificacion.notificarResultado(solicitud.idOrden, resultado,
                        nombre + (aprobado ? ": aprobado" : ": rechazado (simulado)"));
            } catch (Exception e) {
                System.out.println("[" + nombre + "] no se pudo notificar: " + e.getMessage());
            }
        });
        return true;
    }
}