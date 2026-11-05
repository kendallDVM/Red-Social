package socialuna;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import socialuna.datos.GestorArchivos;
import socialuna.datos.PersistenciaException;
import socialuna.gui.CierreAplicacion;
import socialuna.gui.Estilos;
import socialuna.gui.NavegacionPrincipal;
import socialuna.gui.VentanaLogin;
import socialuna.modelo.Usuario;
import socialuna.servicios.ContextoAplicacion;
import socialuna.servicios.ServicioAutenticacion;

/**
 * Punto de entrada de Social UNA.
 *
 * <p>Flujo: cargar {@code data/} → mostrar el login → (inicio de sesión) → ventana principal.
 *
 * <p><b>Integración pendiente (Módulo 3, Mariano):</b> {@link #destinoTemporal} es solo
 * un aviso provisional para poder probar el inicio y el cierre de sesión. Cuando exista
 * {@code VentanaPrincipal}, se reemplaza la línea marcada con {@code TODO}.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        // Swing debe crearse y usarse en el hilo de eventos (EDT)
        SwingUtilities.invokeLater(Main::iniciar);
    }

    private static void iniciar() {
        Estilos.aplicarLookAndFeel();

        ContextoAplicacion contexto = new ContextoAplicacion(new GestorArchivos());
        try {
            contexto.cargarTodo();
        } catch (PersistenciaException e) {
            JOptionPane.showMessageDialog(null,
                    e.getMessage() + "\n\nLa aplicación continuará sin los datos que no se pudieron cargar.",
                    "Social UNA - Problema al cargar los datos",
                    JOptionPane.WARNING_MESSAGE);
        }

        ServicioAutenticacion servicio = new ServicioAutenticacion(contexto);
        CierreAplicacion cierre = new CierreAplicacion(contexto);

        // TODO Módulo 3 (Mariano): reemplazar por
        //   (usuario, cerrarSesion) -> new VentanaPrincipal(contexto, usuario, cerrarSesion, cierre).setVisible(true);
        NavegacionPrincipal navegacion = (usuario, cerrarSesion) -> destinoTemporal(usuario, cerrarSesion, cierre);

        new VentanaLogin(servicio, navegacion, cierre).mostrar(null);
    }

    /**
     * Aviso provisional que ocupa el lugar de la ventana principal mientras el Módulo 3
     * no esté integrado. Permite probar "Cerrar sesión" y "Salir".
     */
    private static void destinoTemporal(Usuario usuario, Runnable cerrarSesion, CierreAplicacion cierre) {
        String[] opciones = {"Cerrar sesión", "Salir de la aplicación"};
        int opcion = JOptionPane.showOptionDialog(null,
                "Sesión iniciada como " + usuario.getNombre() + " (@" + usuario.getUsuario() + ").\n\n"
                        + "La ventana principal (Muro, Buscar, Amigos) se integrará en el Módulo 3.",
                "Social UNA - Sesión iniciada",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null, opciones, opciones[0]);

        if (opcion == 0) {
            cerrarSesion.run();
        } else {
            cierre.salir(null);
            // Si el usuario decidió no salir (falló el guardado), se vuelve a mostrar el aviso.
            destinoTemporal(usuario, cerrarSesion, cierre);
        }
    }
}
