package socialuna.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowListener;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

import socialuna.modelo.Usuario;
import socialuna.servicios.ContextoAplicacion;
import socialuna.servicios.ServicioAmistades;
import socialuna.servicios.ServicioPublicaciones;

/**
 * Ventana principal de Social UNA, la que se abre después de iniciar sesión.
 *
 * <p>Reúne los tres módulos en pestañas:
 * <ul>
 *   <li><b>Muro</b> (Módulo 3): publicar y ver el muro general o "Mi muro".</li>
 *   <li><b>Buscar usuarios</b> (Módulo 2): buscar estudiantes y agregarlos como amigos.</li>
 *   <li><b>Mis amigos</b> (Módulo 2): lista de amigos del usuario activo.</li>
 * </ul>
 * El saludo con el nombre del usuario y el botón "Cerrar sesión" están siempre visibles
 * en el encabezado.
 *
 * <p>Comportamiento:
 * <ul>
 *   <li><b>Cerrar sesión:</b> {@code dispose()} de esta ventana y ejecución del
 *       {@code Runnable} recibido de {@link NavegacionPrincipal}, que guarda los datos,
 *       limpia {@code Sesion} y vuelve a mostrar el login.</li>
 *   <li><b>X de la ventana:</b> cierra la <b>aplicación</b> mediante
 *       {@link CierreAplicacion} (guarda los datos, limpia la sesión y termina). No equivale
 *       a cerrar sesión.</li>
 *   <li><b>Cambio de pestaña:</b> la pestaña que se abre siempre muestra datos al día
 *       (por ejemplo, el muro refleja los amigos agregados en la otra pestaña).</li>
 * </ul>
 */
public class VentanaPrincipal extends JFrame {

    private final Runnable cerrarSesion;
    private final JButton botonCerrarSesion = Estilos.botonSecundario("Cerrar sesión");

    private PanelMuro panelMuro;
    private PanelBuscarUsuarios panelBuscar;
    private PanelMisAmigos panelAmigos;

    /**
     * @param contexto     estructuras de datos compartidas de la aplicación
     * @param usuario      usuario que inició sesión
     * @param cerrarSesion acción recibida de {@link NavegacionPrincipal}
     * @param cierre       listener que guarda y termina la aplicación al pulsar la X
     */
    public VentanaPrincipal(ContextoAplicacion contexto, Usuario usuario, Runnable cerrarSesion,
                            WindowListener cierre) {
        super("Social UNA - " + Objects.requireNonNull(usuario, "El usuario no puede ser nulo.").getNombre());
        Objects.requireNonNull(contexto, "El contexto no puede ser nulo.");
        this.cerrarSesion = Objects.requireNonNull(cerrarSesion, "La acción de cerrar sesión no puede ser nula.");
        Objects.requireNonNull(cierre, "El listener de cierre no puede ser nulo.");

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(cierre); // X = salir de la aplicación guardando los datos
        construirInterfaz(contexto, usuario);
        setSize(860, 780);
        setMinimumSize(new Dimension(640, 560));
        setLocationRelativeTo(null);
    }

    private void construirInterfaz(ContextoAplicacion contexto, Usuario usuario) {
        ServicioAmistades servicioAmistades = new ServicioAmistades(contexto);
        ServicioPublicaciones servicioPublicaciones = new ServicioPublicaciones(contexto, servicioAmistades);

        // Las tres pestañas se avisan entre sí cuando cambian los amigos o las publicaciones.
        panelMuro = new PanelMuro(servicioPublicaciones);
        panelAmigos = new PanelMisAmigos(servicioAmistades, this::amigosCambiaron);
        panelBuscar = new PanelBuscarUsuarios(servicioAmistades, this::amigosCambiaron);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Estilos.FONDO);
        raiz.add(construirEncabezado(usuario), BorderLayout.NORTH);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.setName("pestanas");
        pestanas.setFont(Estilos.fuente(Font.BOLD, 13));
        pestanas.addTab("Muro", panelMuro);
        pestanas.addTab("Buscar usuarios", panelBuscar);
        pestanas.addTab("Mis amigos", panelAmigos);
        // Al abrir una pestaña siempre se muestra la información actual
        pestanas.addChangeListener(e -> {
            if (pestanas.getSelectedComponent() == panelMuro) {
                panelMuro.refrescar();
            } else if (pestanas.getSelectedComponent() == panelAmigos) {
                panelAmigos.refrescar();
            } else if (pestanas.getSelectedComponent() == panelBuscar) {
                panelBuscar.refrescar();
            }
        });
        raiz.add(pestanas, BorderLayout.CENTER);

        setContentPane(raiz);
    }

    /** Encabezado con el nombre de la aplicación, el saludo y el botón "Cerrar sesión". */
    private JPanel construirEncabezado(Usuario usuario) {
        JPanel encabezado = Estilos.crearEncabezado("Social UNA",
                "¡Hola, " + usuario.getNombre() + "!  ·  @" + usuario.getUsuario());
        encabezado.setName("encabezado");

        botonCerrarSesion.setName("botonCerrarSesion");
        botonCerrarSesion.addActionListener(e -> cerrarSesion());
        JPanel lateral = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        lateral.setOpaque(false);
        lateral.add(botonCerrarSesion);
        encabezado.add(lateral, BorderLayout.EAST);
        return encabezado;
    }

    /** Se ejecuta cuando una pestaña cambió una amistad: las demás deben reflejarlo. */
    private void amigosCambiaron() {
        panelMuro.refrescar();
        panelAmigos.refrescar();
        panelBuscar.refrescar();
    }

    /** Cierra esta ventana y delega el cierre de sesión (guardar, limpiar, volver al login). */
    private void cerrarSesion() {
        botonCerrarSesion.setEnabled(false); // evita un segundo clic
        dispose();
        cerrarSesion.run();
    }
}
