package socialuna.gui;

import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;

import javax.swing.JOptionPane;

import socialuna.datos.PersistenciaException;
import socialuna.servicios.ContextoAplicacion;
import socialuna.servicios.Sesion;

/**
 * Cierra la aplicación <b>guardando todos los datos</b> antes de salir.
 *
 * <p>Se registra como {@code WindowListener} en las ventanas que, al cerrarse con la X,
 * deben terminar la aplicación (login y, más adelante, la ventana principal). Esas
 * ventanas deben usar {@code setDefaultCloseOperation(DO_NOTHING_ON_CLOSE)} para que
 * esta clase decida si se sale o no.
 *
 * <p>Si el guardado falla, se pregunta al usuario si quiere salir de todos modos
 * (perdería los cambios que no se guardaron) o quedarse en la aplicación.
 */
public class CierreAplicacion extends WindowAdapter {

    private final ContextoAplicacion contexto;

    public CierreAplicacion(ContextoAplicacion contexto) {
        this.contexto = Objects.requireNonNull(contexto, "El contexto no puede ser nulo.");
    }

    @Override
    public void windowClosing(WindowEvent e) {
        salir(e.getWindow());
    }

    /**
     * Guarda todo y termina el programa. Si no se puede guardar, pregunta antes de salir.
     *
     * @param padre componente sobre el que se centran las alertas (puede ser {@code null})
     */
    public void salir(Component padre) {
        if (prepararSalida(padre)) {
            System.exit(0);
        }
    }

    /**
     * Guarda todos los datos y limpia la sesión, sin terminar el programa (separado de
     * {@link #salir} para poder probarlo).
     *
     * @return {@code true} si se puede terminar: el guardado funcionó, o falló y el usuario
     *         decidió salir de todos modos. {@code false} si el usuario prefiere quedarse
     *         (en ese caso la sesión <b>no</b> se cierra).
     */
    boolean prepararSalida(Component padre) {
        try {
            contexto.guardarTodo();
        } catch (PersistenciaException ex) {
            int opcion = JOptionPane.showConfirmDialog(padre,
                    ex.getMessage() + "\n\n¿Desea salir de todos modos? Se perderán los cambios no guardados.",
                    "No se pudieron guardar los datos",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (opcion != JOptionPane.YES_OPTION) {
                return false; // el usuario prefiere quedarse
            }
        }
        Sesion.cerrar();
        return true;
    }
}
