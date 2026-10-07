package apexstore.pasarelas;

import apexstore.INotificacionPagoPrx;
import com.zeroc.Ice.*;

public class PasarelasMain {
    public static void main(String[] args) {
        try (Communicator communicator = Util.initialize(args, "pasarelas.cfg")) {
            // El socket de las tres estrategias: proxy al Gestor (Nodo 2)
            INotificacionPagoPrx gestor = INotificacionPagoPrx.checkedCast(
                    communicator.propertyToProxy("Gestor.Proxy"));
            if (gestor == null) {
                throw new RuntimeException("Gestor.Proxy no configurado");
            }

            ObjectAdapter adapter = communicator.createObjectAdapter("Pasarelas");
            adapter.add(new EstrategiaStripeI(gestor), Util.stringToIdentity("EstrategiaStripe"));
            adapter.add(new EstrategiaPSEI(gestor), Util.stringToIdentity("EstrategiaPSE"));
            adapter.add(new EstrategiaCriptoI(gestor), Util.stringToIdentity("EstrategiaCripto"));
            adapter.activate();
            System.out.println("[Pasarelas] Nodo 3 listo");
            communicator.waitForShutdown();
        }
    }
}