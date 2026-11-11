package socialuna.gui;

import socialuna.modelo.Usuario;

/**
 * Punto de conexión entre el Módulo 1 (login) y la ventana principal (Módulo 3, Mariano).
 *
 * <p>{@link VentanaLogin} llama a {@link #abrir(Usuario, Runnable)} cuando el inicio de
 * sesión es exitoso. En ese momento:
 * <ul>
 *   <li>{@code Sesion.getUsuarioActivo()} ya devuelve el usuario.</li>
 *   <li>La ventana de login ya está oculta.</li>
 * </ul>
 *
 * <p>Ejemplo de uso para {@code VentanaPrincipal} (en {@code socialuna.Main}):
 * <pre>{@code
 * NavegacionPrincipal navegacion = (usuario, cerrarSesion) ->
 *         new VentanaPrincipal(contexto, usuario, cerrarSesion, cierre).setVisible(true);
 *
 * // Dentro de VentanaPrincipal, en el botón "Cerrar sesión":
 * dispose();
 * cerrarSesion.run();   // guarda todo, limpia Sesion y vuelve a mostrar el login
 * }</pre>
 */
@FunctionalInterface
public interface NavegacionPrincipal {

    /**
     * @param usuarioActivo usuario que acaba de iniciar sesión
     * @param cerrarSesion  acción que guarda los datos, cierra la sesión, muestra una alerta
     *                      si el guardado falló y vuelve al login. La ventana principal
     *                      debe cerrarse ({@code dispose()}) antes de ejecutarla.
     */
    void abrir(Usuario usuarioActivo, Runnable cerrarSesion);
}
