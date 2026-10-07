module apexstore
{
    // ===== Tipos base =====
    enum EstadoOrden { PENDIENTE, PAGADA, FALLIDA };
    enum MedioPago { STRIPE, PSE, CRIPTO };
    enum ResultadoPago { APROBADO, RECHAZADO };

    // Datos específicos de cada medio de pago (token, banco, wallet...), opacos para el contexto
    dictionary<string, string> DatosPago;

    struct SolicitudPago
    {
        string idOrden;      // identificador opaco de correlación
        MedioPago medio;
        double monto;
        string moneda;
        DatosPago datos;
    };

    exception OrdenNoEncontrada
    {
        string idOrden;
    };

    // ===== Provista por ServicioCheckout, requerida por los clientes =====
    interface IGestionCompras
    {
        // Registra la orden en PENDIENTE, despacha el cobro y devuelve el idOrden
        string realizarCompra(SolicitudPago solicitud);
        EstadoOrden consultarEstadoOrden(string idOrden) throws OrdenNoEncontrada;
    };

    // ===== Provista por ProcesadorPagosContexto, requerida por ServicioCheckout =====
    interface IServicioPagos
    {
        // Devuelve el acuse de recepción, no el resultado del cobro
        bool iniciarPago(SolicitudPago solicitud);
    };

    // ===== Provista por cada Estrategia (Stripe, PSE, Cripto), requerida por el contexto =====
    interface IEstrategiaPago
    {
        // Devuelve el acuse de recepción; el resultado llega después por INotificacionPago
        bool procesarPago(SolicitudPago solicitud);
    };

    // ===== Provista por GestorConfirmacionesPago, requerida por las tres estrategias =====
    interface INotificacionPago
    {
        void notificarResultado(string idOrden, ResultadoPago resultado, string detalle);
    };

    // ===== Provista por ServicioCheckout, requerida por GestorConfirmacionesPago =====
    interface IEstadoOrden
    {
        void ordenActualizada(string idOrden, EstadoOrden estado);
    };

    // ===== Provista por DB PostgreSQL, requerida por ServicioCheckout y el Gestor =====
    interface IRepositorioTransacciones
    {
        // Solo la invoca ServicioCheckout
        void crearOrden(SolicitudPago solicitud);
        // Solo la invoca el Gestor; aplica únicamente si la orden sigue en PENDIENTE
        bool actualizarEstado(string idOrden, EstadoOrden nuevoEstado) throws OrdenNoEncontrada;
        EstadoOrden obtenerEstado(string idOrden) throws OrdenNoEncontrada;
    };
};