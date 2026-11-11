package socialuna.servicios;

/**
 * Relación entre el usuario activo y otro usuario, vista desde el usuario activo.
 * La calcula {@link ServicioAmistades#obtenerEstado(String, String)} y la interfaz la usa
 * para mostrar el estado y decidir qué acción ofrecer.
 */
public enum EstadoAmistad {

    /** El usuario encontrado es el propio usuario activo: no hay acción posible. */
    PROPIA_CUENTA("Es tu cuenta"),
    /** Ya existe la amistad (aceptada). */
    AMIGOS("Ya son amigos"),
    /** El usuario activo envió una solicitud que sigue pendiente. */
    SOLICITUD_ENVIADA("Solicitud enviada (pendiente)"),
    /** El otro usuario le envió una solicitud al usuario activo y está pendiente. */
    SOLICITUD_RECIBIDA("Te envió una solicitud"),
    /** No hay amistad ni solicitudes: se puede enviar una solicitud. */
    SIN_RELACION("No son amigos"),
    /** El ID no corresponde a ningún usuario registrado. */
    USUARIO_NO_EXISTE("Usuario no encontrado");

    private final String descripcion;

    EstadoAmistad(String descripcion) {
        this.descripcion = descripcion;
    }

    /** Texto listo para mostrar en la columna "Estado". */
    public String getDescripcion() {
        return descripcion;
    }
}
