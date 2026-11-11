package socialuna.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
import socialuna.servicios.ServicioAmistades;

/**
 * Pantalla <b>temporal</b> del Módulo 1 que se muestra después de iniciar sesión.
 *
 * <p>Se usa mientras no exista la ventana principal de la aplicación (responsabilidad de
 * la integración del Módulo 3). Tiene tres pestañas: "Inicio" (quién inició sesión y
 * cerrar sesión) y las del Módulo 2, "Buscar usuarios" y "Mis amigos". <b>No</b> es la
 * ventana definitiva: Mariano agregará el muro en {@code VentanaPrincipal}.
 *
 * <p>Comportamiento:
 * <ul>
 *   <li><b>Cerrar sesión:</b> {@code dispose()} de esta ventana y ejecución del
 *       {@code Runnable} recibido de {@link NavegacionPrincipal}, que guarda los datos,
 *       limpia {@code Sesion} y vuelve a mostrar la misma instancia del login.</li>
 *   <li><b>X de la ventana:</b> cierra la <b>aplicación</b> mediante
 *       {@link CierreAplicacion} (guarda los datos, limpia la sesión y termina). No equivale
 *       a cerrar sesión.</li>
 * </ul>
 *
 * <p>Para sustituirla, basta con cambiar la implementación de {@link NavegacionPrincipal}
 * en {@code socialuna.Main}.
 */
public class VentanaAutenticada extends JFrame {

    private final Runnable cerrarSesion;
    private final ServicioAmistades servicioAmistades;
    private final JButton botonCerrarSesion = Estilos.botonPrimario("Cerrar sesión");

    /**
     * @param usuario           usuario que inició sesión
     * @param cerrarSesion      acción recibida de {@link NavegacionPrincipal}
     * @param cierre            listener que guarda y termina la aplicación al pulsar la X
     * @param servicioAmistades lógica del Módulo 2 para las pestañas "Buscar usuarios" y
     *                          "Mis amigos"
     */
    public VentanaAutenticada(Usuario usuario, Runnable cerrarSesion, WindowListener cierre,
                              ServicioAmistades servicioAmistades) {
        super("Social UNA - " + Objects.requireNonNull(usuario, "El usuario no puede ser nulo.").getNombre());
        this.cerrarSesion = Objects.requireNonNull(cerrarSesion, "La acción de cerrar sesión no puede ser nula.");
        this.servicioAmistades = Objects.requireNonNull(servicioAmistades,
                "El servicio de amistades no puede ser nulo.");
        Objects.requireNonNull(cierre, "El listener de cierre no puede ser nulo.");

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(cierre); // X = salir de la aplicación guardando los datos
        construirInterfaz(usuario);
        setSize(760, 600);
        setMinimumSize(new java.awt.Dimension(620, 480));
        setLocationRelativeTo(null);
    }

    private void construirInterfaz(Usuario usuario) {
        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Estilos.FONDO);
        raiz.add(Estilos.crearEncabezado("Social UNA", "Red social estudiantil de la Universidad Nacional"),
                BorderLayout.NORTH);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.setName("pestanas");
        pestanas.setFont(Estilos.fuente(Font.BOLD, 13));
        pestanas.addTab("Inicio", construirInicio(usuario));
        agregarPestanasAmistades(pestanas);
        raiz.add(pestanas, BorderLayout.CENTER);

        setContentPane(raiz);
        getRootPane().setDefaultButton(botonCerrarSesion);
    }

    /**
     * Módulo 2 (Greivin): pestañas "Buscar usuarios" y "Mis amigos", conectadas para que
     * cada una se refresque cuando la otra cambia una amistad.
     *
     * <p>Mariano: en {@code VentanaPrincipal} se pueden crear igual que aquí.
     */
    private void agregarPestanasAmistades(JTabbedPane pestanas) {
        PanelMisAmigos[] misAmigos = new PanelMisAmigos[1];
        PanelBuscarUsuarios buscar = new PanelBuscarUsuarios(servicioAmistades, () -> misAmigos[0].refrescar());
        misAmigos[0] = new PanelMisAmigos(servicioAmistades, buscar::refrescar);

        pestanas.addTab("Buscar usuarios", buscar);
        pestanas.addTab("Mis amigos", misAmigos[0]);
        // Al abrir "Mis amigos" siempre se muestra la lista actual
        pestanas.addChangeListener(e -> {
            if (pestanas.getSelectedComponent() == misAmigos[0]) {
                misAmigos[0].refrescar();
            }
        });
    }

    /** Pestaña "Inicio": saludo y botón de cerrar sesión (contenido original del Módulo 1). */
    private JPanel construirInicio(Usuario usuario) {
        JPanel contenido = new JPanel(new GridBagLayout());
        contenido.setBackground(Estilos.TARJETA);
        contenido.setBorder(BorderFactory.createEmptyBorder(26, 32, 28, 32));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        JLabel saludo = Estilos.titulo("¡Hola, " + usuario.getNombre() + "!");
        saludo.setName("saludo");
        c.insets = new Insets(0, 0, 6, 0);
        contenido.add(saludo, c);

        c.insets = new Insets(0, 0, 18, 0);
        contenido.add(Estilos.textoSecundario("@" + usuario.getUsuario() + "  ·  ID " + usuario.getId()), c);

        JLabel estado = new JLabel("●  Sesión iniciada correctamente");
        estado.setFont(Estilos.fuente(Font.BOLD, 13));
        estado.setForeground(new Color(0x15, 0x80, 0x3D));
        c.insets = new Insets(0, 0, 6, 0);
        contenido.add(estado, c);

        c.insets = new Insets(0, 0, 22, 0);
        contenido.add(Estilos.textoSecundario(
                "Usa las pestañas de arriba para buscar estudiantes y ver tus amigos."), c);

        botonCerrarSesion.addActionListener(e -> cerrarSesion());
        c.insets = new Insets(0, 0, 0, 0);
        contenido.add(botonCerrarSesion, c);

        return contenido;
    }

    /** Cierra esta ventana y delega el cierre de sesión (guardar, limpiar, volver al login). */
    private void cerrarSesion() {
        botonCerrarSesion.setEnabled(false); // evita un segundo clic
        dispose();
        cerrarSesion.run();
    }
}
