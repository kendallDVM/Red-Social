package socialuna;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import socialuna.datos.GestorArchivos;
import socialuna.datos.PersistenciaException;
import socialuna.gui.CierreAplicacion;
import socialuna.gui.Estilos;
import socialuna.gui.NavegacionPrincipal;
import socialuna.gui.VentanaAutenticada;
import socialuna.gui.VentanaLogin;
import socialuna.servicios.ContextoAplicacion;
import socialuna.servicios.ServicioAutenticacion;

/**
 * Punto de entrada de Social UNA.
 *
 * <p>Flujo: cargar {@code data/} → mostrar el login → (inicio de sesión) → pantalla
 * autenticada → (cerrar sesión) → login.
 *
 * <p>Si los datos no se pueden cargar (por ejemplo, {@code usuarios.json} está dañado),
 * se muestra el error y la aplicación <b>no continúa</b>: así no se crea ninguna sesión
 * ni se guarda una lista vacía encima del archivo original. Un archivo inexistente o
 * vacío no es un error.
 *
 * <p><b>Integración pendiente (Módulo 3, Mariano):</b> {@link VentanaAutenticada} es un
 * destino temporal para comprobar la autenticación. Cuando exista {@code VentanaPrincipal},
 * se reemplaza la línea marcada con {@code TODO}, que usa el mismo contrato
 * {@link NavegacionPrincipal}.
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
                    e.getMessage()
                            + "\n\nPara proteger sus datos, la aplicación no se iniciará y no modificó el archivo."
                            + "\nRevise o restaure el archivo (puede usar la copia de respaldo indicada)"
                            + " y vuelva a abrir Social UNA.",
                    "Social UNA - No se pudieron cargar los datos",
                    JOptionPane.ERROR_MESSAGE);
            // Se termina sin pasar por CierreAplicacion: no se guarda nada.
            System.exit(1);
            return;
        }

        // El servicio usa un hash ficticio precalculado: crearlo no bloquea el EDT.
        ServicioAutenticacion servicio = new ServicioAutenticacion(contexto);
        CierreAplicacion cierre = new CierreAplicacion(contexto);

        // TODO Módulo 3 (Mariano): reemplazar VentanaAutenticada por
        //   (usuario, cerrarSesion) -> new VentanaPrincipal(contexto, usuario, cerrarSesion, cierre).setVisible(true);
        NavegacionPrincipal navegacion = (usuario, cerrarSesion) ->
                new VentanaAutenticada(usuario, cerrarSesion, cierre).setVisible(true);

        new VentanaLogin(servicio, navegacion, cierre).mostrar(null);
    }
}
