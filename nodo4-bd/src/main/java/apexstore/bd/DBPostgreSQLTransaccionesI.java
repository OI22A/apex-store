package apexstore.bd;

import apexstore.*;
import com.zeroc.Ice.Current;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Provee IRepositorioTransacciones. No requiere ninguna interfaz.
public class DBPostgreSQLTransaccionesI implements IRepositorioTransacciones {

    // Simula la tabla de órdenes: idOrden -> estado
    private final Map<String, EstadoOrden> ordenes = new ConcurrentHashMap<>();

    @Override
    public void crearOrden(SolicitudPago solicitud, Current current) {
        EstadoOrden previo = ordenes.putIfAbsent(solicitud.idOrden, EstadoOrden.PENDIENTE);
        System.out.println("[BD] crearOrden " + solicitud.idOrden
                + (previo == null ? " -> PENDIENTE" : " (ya existía, se ignora)"));
    }

    @Override
    public boolean actualizarEstado(String idOrden, EstadoOrden nuevoEstado, Current current)
            throws OrdenNoEncontrada {
        if (!ordenes.containsKey(idOrden)) {
            throw new OrdenNoEncontrada(idOrden);
        }
        // replace(k, esperado, nuevo) es atómico: solo cambia si sigue en PENDIENTE
        boolean aplicado = ordenes.replace(idOrden, EstadoOrden.PENDIENTE, nuevoEstado);
        System.out.println("[BD] actualizarEstado " + idOrden + " -> " + nuevoEstado
                + (aplicado ? "" : " (ignorado: ya no estaba PENDIENTE)"));
        return aplicado;
    }

    @Override
    public EstadoOrden obtenerEstado(String idOrden, Current current) throws OrdenNoEncontrada {
        EstadoOrden estado = ordenes.get(idOrden);
        if (estado == null) {
            throw new OrdenNoEncontrada(idOrden);
        }
        return estado;
    }
}