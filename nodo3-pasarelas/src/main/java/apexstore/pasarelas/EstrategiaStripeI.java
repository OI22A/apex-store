package apexstore.pasarelas;

import apexstore.INotificacionPagoPrx;

public class EstrategiaStripeI extends EstrategiaBase {
    public EstrategiaStripeI(INotificacionPagoPrx notificacion) {
        super("Stripe", 800, 0.10, notificacion);
    }
}