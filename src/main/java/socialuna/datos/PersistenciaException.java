package socialuna.datos;

/**
 * Error al leer o escribir los archivos de datos.
 *
 * <p>Es una excepción verificada (checked) para obligar a quien llama a manejarla.
 * La interfaz gráfica la captura y muestra {@link #getMessage()} en una alerta, así
 * la aplicación no se cierra de forma inesperada. Los mensajes nunca incluyen
 * contraseñas.
 */
public class PersistenciaException extends Exception {

    public PersistenciaException(String mensaje) {
        super(mensaje);
    }

    public PersistenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
