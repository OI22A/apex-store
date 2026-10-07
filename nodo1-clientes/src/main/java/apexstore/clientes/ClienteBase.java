package apexstore.clientes;

import apexstore.*;

import java.util.HashMap;
import java.util.Map;

// Lógica común de los clientes.
// Requiere IGestionCompras (socket): solo conoce este contrato.
public abstract class ClienteBase {

    private final String nombre;
    private final IGestionComprasPrx compras; // requerida (socket)

    protected ClienteBase(String nombre, IGestionComprasPrx compras) {
        this.nombre = nombre;
        this.compras = compras;
    }

    // Compra completa: envía la solicitud y luego consulta el estado hasta que deje de ser PENDIENTE
    public void comprar(MedioPago medio, double monto, String moneda) {
        Map<String, String> datos = new HashMap<>();
        datos.put("referencia", "demo-" + medio); // dato específico del medio, opaco para el cliente

        // idOrden vacío: lo genera ServicioCheckout y lo devuelve
        SolicitudPago solicitud = new SolicitudPago("", medio, monto, moneda, datos);

        String idOrden = compras.realizarCompra(solicitud);
        System.out.println("[" + nombre + "] compra enviada (" + medio + "), orden " + idOrden);

        esperarResultado(idOrden);
    }

    // No hay push: el cliente consulta el estado por el mismo contrato
    private void esperarResultado(String idOrden) {
        long limite = System.currentTimeMillis() + 15_000;
        try {
            while (System.currentTimeMillis() < limite) {
                EstadoOrden estado = compras.consultarEstadoOrden(idOrden);
                if (estado != EstadoOrden.PENDIENTE) {
                    System.out.println("[" + nombre + "] orden " + idOrden + " -> " + estado);
                    return;
                }
                Thread.sleep(1000);
            }
            System.out.println("[" + nombre + "] orden " + idOrden
                    + " sigue PENDIENTE (pendiente de verificación)");
        } catch (OrdenNoEncontrada e) {
            System.out.println("[" + nombre + "] orden no encontrada: " + e.idOrden);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}