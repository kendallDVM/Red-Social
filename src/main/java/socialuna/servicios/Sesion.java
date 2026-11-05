package socialuna.servicios;

import socialuna.modelo.Usuario;

/**
 * Guarda el usuario que inició sesión en la aplicación.
 *
 * <p>Se implementa con miembros {@code static} porque en la aplicación solo hay una
 * sesión a la vez y la consultan ventanas de los tres módulos (Muro, Buscar, Amigos).
 * Así no hace falta pasar el usuario activo de una ventana a otra.
 *
 * <p>Esta clase solo administra el estado. El guardado de datos al cerrar sesión lo
 * coordina la capa de servicios (Fase 3), que llama a
 * {@code GestorArchivos.guardarTodo(...)} y luego a {@link #cerrar()}.
 */
public final class Sesion {

    private static Usuario usuarioActivo;

    private Sesion() {
        // Clase de utilidad: no se instancia.
    }

    /**
     * Establece el usuario activo. Si ya había una sesión, se reemplaza.
     *
     * @throws IllegalArgumentException si el usuario es nulo
     */
    public static void iniciar(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("No se puede iniciar sesión con un usuario nulo.");
        }
        usuarioActivo = usuario;
    }

    /** @return el usuario activo, o {@code null} si no hay sesión */
    public static Usuario getUsuarioActivo() {
        return usuarioActivo;
    }

    public static boolean haySesionActiva() {
        return usuarioActivo != null;
    }

    /** Cierra la sesión y limpia el usuario activo. */
    public static void cerrar() {
        usuarioActivo = null;
    }
}
