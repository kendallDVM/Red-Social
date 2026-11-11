package socialuna.servicios;

import socialuna.modelo.Usuario;

/**
 * Resultado de una operación de autenticación (registro, inicio o cierre de sesión).
 *
 * <p>La interfaz gráfica no necesita conocer las reglas de negocio: le basta con
 * consultar {@link #esExitoso()}, mostrar {@link #getMensaje()} en una alerta y, si
 * quiere, usar {@link #getTipo()} para poner el foco en el campo con el problema.
 *
 * <p>Los mensajes están redactados para mostrarse al estudiante y nunca incluyen contraseñas.
 */
public final class ResultadoAutenticacion {

    /** Motivo del resultado. */
    public enum Tipo {
        EXITO,
        /** Algún campo obligatorio está vacío. */
        CAMPOS_VACIOS,
        /** El nombre de usuario contiene espacios. */
        USUARIO_INVALIDO,
        /** La contraseña y su confirmación no coinciden. */
        CONTRASENAS_NO_COINCIDEN,
        /** La contraseña no se puede procesar (supera el límite de BCrypt). */
        CONTRASENA_INVALIDA,
        /** El ID ya pertenece a otra cuenta. */
        ID_DUPLICADO,
        /** El nombre de usuario ya pertenece a otra cuenta (sin distinguir mayúsculas). */
        USUARIO_DUPLICADO,
        /** Usuario inexistente o contraseña incorrecta: a propósito no se distingue cuál. */
        CREDENCIALES_INVALIDAS,
        /** No se pudieron guardar los datos en disco. */
        ERROR_PERSISTENCIA
    }

    private final Tipo tipo;
    private final String mensaje;
    private final Usuario usuario;

    private ResultadoAutenticacion(Tipo tipo, String mensaje, Usuario usuario) {
        this.tipo = tipo;
        this.mensaje = mensaje;
        this.usuario = usuario;
    }

    static ResultadoAutenticacion exito(String mensaje, Usuario usuario) {
        return new ResultadoAutenticacion(Tipo.EXITO, mensaje, usuario);
    }

    static ResultadoAutenticacion error(Tipo tipo, String mensaje) {
        if (tipo == Tipo.EXITO) {
            throw new IllegalArgumentException("Un error no puede tener tipo EXITO.");
        }
        return new ResultadoAutenticacion(tipo, mensaje, null);
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

    /** Usuario registrado o que inició sesión; {@code null} si la operación falló. */
    public Usuario getUsuario() {
        return usuario;
    }

    @Override
    public String toString() {
        return "ResultadoAutenticacion{" + tipo + ", '" + mensaje + "'}";
    }
}
