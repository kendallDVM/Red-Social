package socialuna.servicios;

import socialuna.modelo.Usuario;

/**
 * Resultado de una operación de amistad (agregar o eliminar amigo).
 *
 * <p>Sigue el mismo estilo que {@link ResultadoAutenticacion}: la interfaz gráfica solo
 * consulta {@link #esExitoso()} y muestra {@link #getMensaje()} en una alerta.
 */
public final class ResultadoAmistad {

    /** Motivo del resultado. */
    public enum Tipo {
        EXITO,
        /** No se indicó a qué usuario (no se seleccionó a nadie). */
        SIN_SELECCION,
        /** No hay un usuario con sesión activa. */
        SIN_SESION,
        /** Se intentó agregar o eliminar a uno mismo. */
        MISMO_USUARIO,
        /** El usuario indicado no está registrado. */
        USUARIO_NO_EXISTE,
        /** Ya eran amigos (al agregar). */
        YA_SON_AMIGOS,
        /** No eran amigos (al eliminar). */
        NO_SON_AMIGOS,
        /** No se pudo guardar amistades.json; el cambio se deshizo. */
        ERROR_PERSISTENCIA
    }

    private final Tipo tipo;
    private final String mensaje;
    private final Usuario amigo;

    private ResultadoAmistad(Tipo tipo, String mensaje, Usuario amigo) {
        this.tipo = tipo;
        this.mensaje = mensaje;
        this.amigo = amigo;
    }

    static ResultadoAmistad exito(String mensaje, Usuario amigo) {
        return new ResultadoAmistad(Tipo.EXITO, mensaje, amigo);
    }

    static ResultadoAmistad error(Tipo tipo, String mensaje) {
        if (tipo == Tipo.EXITO) {
            throw new IllegalArgumentException("Un error no puede tener tipo EXITO.");
        }
        return new ResultadoAmistad(tipo, mensaje, null);
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

    /** El otro usuario de la amistad; {@code null} si la operación falló. */
    public Usuario getAmigo() {
        return amigo;
    }

    @Override
    public String toString() {
        return "ResultadoAmistad{" + tipo + ", '" + mensaje + "'}";
    }
}
