package socialuna.servicios;

import socialuna.modelo.Publicacion;

/**
 * Resultado de publicar un mensaje o de dar o quitar un "me gusta".
 *
 * <p>Sigue el mismo estilo que {@link ResultadoAmistad}: la interfaz gráfica solo
 * consulta {@link #esExitoso()} y muestra {@link #getMensaje()} en una alerta.
 */
public final class ResultadoPublicacion {

    /** Motivo del resultado. */
    public enum Tipo {
        EXITO,
        /** El mensaje está vacío o solo tiene espacios. */
        TEXTO_VACIO,
        /** El mensaje supera {@link ServicioPublicaciones#MAX_CARACTERES}. */
        TEXTO_MUY_LARGO,
        /** No hay un usuario con sesión activa o ya no está registrado. */
        SIN_SESION,
        /** La publicación ya no existe (por ejemplo, el ID no está en el archivo). */
        PUBLICACION_NO_EXISTE,
        /**
         * La acción no está permitida: dar "me gusta" a publicaciones que no son propias ni de
         * amigos, o eliminar una publicación de otra persona.
         */
        NO_PERMITIDO,
        /** No se pudo guardar publicaciones.json; el cambio se deshizo. */
        ERROR_PERSISTENCIA
    }

    private final Tipo tipo;
    private final String mensaje;
    private final Publicacion publicacion;

    private ResultadoPublicacion(Tipo tipo, String mensaje, Publicacion publicacion) {
        this.tipo = tipo;
        this.mensaje = mensaje;
        this.publicacion = publicacion;
    }

    static ResultadoPublicacion exito(String mensaje, Publicacion publicacion) {
        return new ResultadoPublicacion(Tipo.EXITO, mensaje, publicacion);
    }

    static ResultadoPublicacion error(Tipo tipo, String mensaje) {
        if (tipo == Tipo.EXITO) {
            throw new IllegalArgumentException("Un error no puede tener tipo EXITO.");
        }
        return new ResultadoPublicacion(tipo, mensaje, null);
    }

    public boolean esExitoso() {
        return tipo == Tipo.EXITO;
    }

    public Tipo getTipo() {
        return tipo;
    }

    /** Mensaje listo para mostrar en una alerta. */
    public String getMensaje() {
        return mensaje;
    }

    /** La publicación creada; {@code null} si la operación falló. */
    public Publicacion getPublicacion() {
        return publicacion;
    }

    @Override
    public String toString() {
        return "ResultadoPublicacion{" + tipo + ", '" + mensaje + "'}";
    }
}
