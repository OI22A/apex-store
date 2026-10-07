package apexstore.bd;

import com.zeroc.Ice.*;

public class BaseDatosMain {
    public static void main(String[] args) {
        try (Communicator communicator = Util.initialize(args, "bd.cfg")) {
            ObjectAdapter adapter = communicator.createObjectAdapter("BD");
            adapter.add(new DBPostgreSQLTransaccionesI(), Util.stringToIdentity("Repositorio"));
            adapter.activate();
            System.out.println("[BD] Nodo 4 listo");
            communicator.waitForShutdown();
        }
    }
}