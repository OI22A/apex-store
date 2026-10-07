package apexstore.clientes;

import apexstore.IGestionComprasPrx;

public class WebApp extends ClienteBase {
    public WebApp(IGestionComprasPrx compras) {
        super("WebApp", compras);
    }
}