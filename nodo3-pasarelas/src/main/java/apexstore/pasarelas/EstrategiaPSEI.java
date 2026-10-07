package apexstore.pasarelas;

import apexstore.INotificacionPagoPrx;

public class EstrategiaPSEI extends EstrategiaBase {
    public EstrategiaPSEI(INotificacionPagoPrx notificacion) {
        super("PSE", 3000, 0.15, notificacion);
    }
}