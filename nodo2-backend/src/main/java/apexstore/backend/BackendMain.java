package apexstore.backend;

import apexstore.*;
import com.zeroc.Ice.*;

import java.util.EnumMap;
import java.util.Map;

public class BackendMain {
    public static void main(String[] args) {
        try (Communicator communicator = Util.initialize(args, "backend.cfg")) {

            // uncheckedCast: no contacta al otro nodo al arrancar, así el orden de arranque no rompe
            IRepositorioTransaccionesPrx repo = IRepositorioTransaccionesPrx.uncheckedCast(
                    communicator.propertyToProxy("Repositorio.Proxy"));
            IServicioPagosPrx pagos = IServicioPagosPrx.uncheckedCast(
                    communicator.propertyToProxy("Contexto.Proxy"));
            IEstadoOrdenPrx estadoOrden = IEstadoOrdenPrx.uncheckedCast(
                    communicator.propertyToProxy("EstadoOrden.Proxy"));

            Map<MedioPago, IEstrategiaPagoPrx> estrategias = new EnumMap<>(MedioPago.class);
            estrategias.put(MedioPago.STRIPE, IEstrategiaPagoPrx.uncheckedCast(
                    communicator.propertyToProxy("Stripe.Proxy")));
            estrategias.put(MedioPago.PSE, IEstrategiaPagoPrx.uncheckedCast(
                    communicator.propertyToProxy("PSE.Proxy")));
            estrategias.put(MedioPago.CRIPTO, IEstrategiaPagoPrx.uncheckedCast(
                    communicator.propertyToProxy("Cripto.Proxy")));

            ServicioCheckoutI checkout = new ServicioCheckoutI(pagos, repo);

            ObjectAdapter adapter = communicator.createObjectAdapter("Backend");
            adapter.add(checkout, Util.stringToIdentity("ServicioCheckout"));
            adapter.add(checkout.estadoOrden(), Util.stringToIdentity("EstadoOrden"));
            adapter.add(new ProcesadorPagosContextoI(estrategias), Util.stringToIdentity("ProcesadorPagos"));
            adapter.add(new GestorConfirmacionesPagoI(repo, estadoOrden), Util.stringToIdentity("Gestor"));
            adapter.activate();
            System.out.println("[Backend] Nodo 2 listo");
            communicator.waitForShutdown();
        }
    }
}