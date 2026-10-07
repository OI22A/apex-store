package apexstore.clientes;

import apexstore.IGestionComprasPrx;
import apexstore.MedioPago;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Util;

public class ClientesMain {
    public static void main(String[] args) throws InterruptedException {
        try (Communicator communicator = Util.initialize(args, "clientes.cfg")) {
            // Proxy hacia ServicioCheckout (Nodo 2), el único punto de contacto
            IGestionComprasPrx compras = IGestionComprasPrx.checkedCast(
                    communicator.propertyToProxy("Checkout.Proxy"));
            if (compras == null) {
                throw new RuntimeException("Checkout.Proxy no configurado");
            }

            WebApp web = new WebApp(compras);
            MobileApp movil = new MobileApp(compras);

            // Las tres compras en paralelo, una por cada medio de pago
            Thread t1 = new Thread(() -> web.comprar(MedioPago.STRIPE, 120.0, "USD"));
            Thread t2 = new Thread(() -> movil.comprar(MedioPago.PSE, 350000.0, "COP"));
            Thread t3 = new Thread(() -> movil.comprar(MedioPago.CRIPTO, 250000.0, "SAT"));
            t1.start(); t2.start(); t3.start();
            t1.join(); t2.join(); t3.join();
        }
    }
}