/**
 * Capa de interfaz gráfica (Java Swing).
 *
 * <p>Contenido:
 * <ul>
 *   <li>Módulo 1 (Kendall): {@link socialuna.gui.VentanaLogin}, {@link socialuna.gui.VentanaRegistro},
 *       {@link socialuna.gui.CierreAplicacion} (guardar al salir), {@link socialuna.gui.Estilos}
 *       (apariencia común), {@link socialuna.gui.NavegacionPrincipal} (conexión con la ventana principal)
 *       y {@link socialuna.gui.VentanaAutenticada} (destino temporal tras el login, hasta que exista
 *       la ventana principal).</li>
 *   <li>Pendiente, Mariano: {@code VentanaPrincipal} con las pestañas Muro, Buscar y Amigos.</li>
 *   <li>Pendiente, Módulo 2 (Greivin): paneles de búsqueda y amigos.</li>
 * </ul>
 *
 * <p>Regla de la arquitectura: las ventanas no leen ni escriben archivos directamente,
 * solo usan las clases de {@code socialuna.servicios} y {@code socialuna.datos}.
 */
package socialuna.gui;
