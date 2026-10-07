package apexstore.pasarelas;

import apexstore.INotificacionPagoPrx;

public class EstrategiaCriptoI extends EstrategiaBase {
    public EstrategiaCriptoI(INotificacionPagoPrx notificacion) {
        super("Cripto", 5000, 0.05, notificacion);
    }
}