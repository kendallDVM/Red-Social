/**
 * Capa de interfaz gráfica (Java Swing).
 *
 * <p>Contenido:
 * <ul>
 *   <li>Módulo 1 (Kendall): {@link socialuna.gui.VentanaLogin}, {@link socialuna.gui.VentanaRegistro},
 *       {@link socialuna.gui.CierreAplicacion} (guardar al salir), {@link socialuna.gui.Estilos}
 *       (apariencia común) y {@link socialuna.gui.NavegacionPrincipal} (conexión con la ventana
 *       principal).</li>
 *   <li>Módulo 2 (Greivin): {@link socialuna.gui.PanelBuscarUsuarios} y
 *       {@link socialuna.gui.PanelMisAmigos}.</li>
 *   <li>Módulo 3 (Mariano): {@link socialuna.gui.PanelMuro} y
 *       {@link socialuna.gui.VentanaPrincipal}, que reúne las pestañas Muro, Buscar usuarios
 *       y Mis amigos.</li>
 * </ul>
 *
 * <p>Regla de la arquitectura: las ventanas no leen ni escriben archivos directamente,
 * solo usan las clases de {@code socialuna.servicios} y {@code socialuna.datos}.
 */
package socialuna.gui;
