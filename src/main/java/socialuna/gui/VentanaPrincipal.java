package socialuna.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.WindowListener;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

import socialuna.modelo.Usuario;
import socialuna.servicios.ContextoAplicacion;
import socialuna.servicios.ServicioAmistades;
import socialuna.servicios.ServicioPerfiles;
import socialuna.servicios.ServicioPublicaciones;
import socialuna.servicios.Sesion;

/**
 * Ventana principal de Social UNA, la que se abre después de iniciar sesión.
 *
 * <p>Reúne los tres módulos en pestañas:
 * <ul>
 *   <li><b>Muro</b> (Módulo 3): publicar y ver el muro general o "Mi muro".</li>
 *   <li><b>Buscar usuarios</b> (Módulo 2): buscar estudiantes y enviarles una solicitud de amistad.</li>
 *   <li><b>Solicitudes</b> (Módulo 2): aceptar o rechazar las solicitudes recibidas y ver
 *       las enviadas. El título de la pestaña muestra cuántas hay recibidas.</li>
 *   <li><b>Mis amigos</b> (Módulo 2): lista de amigos del usuario activo.</li>
 * </ul>
 * El saludo con el nombre del usuario y el botón "Cerrar sesión" están siempre visibles
 * en el encabezado. A la izquierda del encabezado está el menú <b>☰</b>, con la opción
 * «Mi perfil» (abre {@link DialogoPerfil} con el perfil del usuario activo). Desde
 * "Buscar usuarios" y "Mis amigos" se puede abrir el perfil de otra persona con «Ver perfil».
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
    private final JButton botonMenu = new JButton(new IconoMenu());
    private final JPopupMenu menu = new JPopupMenu();
    private final JMenuItem itemMiPerfil = new OpcionMenu("Mi perfil", "Consulta y edita tu información");
    private ServicioPerfiles servicioPerfiles;

    private PanelMuro panelMuro;
    private PanelBuscarUsuarios panelBuscar;
    private PanelMisAmigos panelAmigos;
    private PanelSolicitudes panelSolicitudes;
    private JTabbedPane pestanas;

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
        // Tamaño preferido 860x780, pero nunca más grande que el área útil de la pantalla
        // (sin la barra de tareas). En pantallas de 720 px lógicos (p. ej. 1920x1080 al 150 %)
        // la ventana de 780 px quedaba cortada y no se veía su parte inferior.
        Rectangle util = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setMinimumSize(new Dimension(Math.min(640, util.width), Math.min(560, util.height)));
        setSize(Math.min(860, util.width), Math.min(780, util.height));
        setLocationRelativeTo(null);
    }

    private void construirInterfaz(ContextoAplicacion contexto, Usuario usuario) {
        ServicioAmistades servicioAmistades = new ServicioAmistades(contexto);
        ServicioPublicaciones servicioPublicaciones = new ServicioPublicaciones(contexto, servicioAmistades);
        servicioPerfiles = new ServicioPerfiles(contexto);

        // Las pestañas se avisan entre sí cuando cambian los amigos, las solicitudes o las publicaciones.
        panelMuro = new PanelMuro(servicioPublicaciones);
        panelAmigos = new PanelMisAmigos(servicioAmistades, this::amigosCambiaron, this::abrirPerfil);
        panelBuscar = new PanelBuscarUsuarios(servicioAmistades, this::amigosCambiaron, this::abrirPerfil);
        panelSolicitudes = new PanelSolicitudes(servicioAmistades, this::amigosCambiaron);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Estilos.FONDO);
        raiz.add(construirEncabezado(usuario), BorderLayout.NORTH);

        pestanas = new JTabbedPane();
        pestanas.setName("pestanas");
        pestanas.setFont(Estilos.fuente(Font.BOLD, 13));
        pestanas.addTab("Muro", panelMuro);
        pestanas.addTab("Buscar usuarios", panelBuscar);
        pestanas.addTab("Solicitudes", panelSolicitudes);
        pestanas.addTab("Mis amigos", panelAmigos);
        actualizarTituloSolicitudes(); // al iniciar sesión ya se ve si hay solicitudes pendientes
        // Al abrir una pestaña siempre se muestra la información actual
        pestanas.addChangeListener(e -> {
            if (pestanas.getSelectedComponent() == panelMuro) {
                panelMuro.refrescar();
            } else if (pestanas.getSelectedComponent() == panelAmigos) {
                panelAmigos.refrescar();
            } else if (pestanas.getSelectedComponent() == panelBuscar) {
                panelBuscar.refrescar();
            } else if (pestanas.getSelectedComponent() == panelSolicitudes) {
                panelSolicitudes.refrescar();
            }
            actualizarTituloSolicitudes();
        });
        raiz.add(pestanas, BorderLayout.CENTER);

        setContentPane(raiz);
    }

    /** Encabezado con el menú ☰, el nombre de la aplicación, el saludo y el botón "Cerrar sesión". */
    private JPanel construirEncabezado(Usuario usuario) {
        JPanel encabezado = Estilos.crearEncabezado("Social UNA",
                "¡Hola, " + usuario.getNombre() + "!  ·  @" + usuario.getUsuario());
        encabezado.setName("encabezado");
        // El menú ☰ va a la izquierda del título, en la misma fila: se toma el título que
        // creó Estilos.crearEncabezado (parte NORTH) y se coloca junto al botón del menú.
        Component titulo = ((BorderLayout) encabezado.getLayout()).getLayoutComponent(BorderLayout.NORTH);
        encabezado.remove(titulo);
        JPanel filaTitulo = new JPanel(new BorderLayout(12, 0));
        filaTitulo.setOpaque(false);
        filaTitulo.add(construirMenu(), BorderLayout.WEST);
        filaTitulo.add(titulo, BorderLayout.CENTER);
        encabezado.add(filaTitulo, BorderLayout.NORTH);

        botonCerrarSesion.setName("botonCerrarSesion");
        botonCerrarSesion.addActionListener(e -> cerrarSesion());
        JPanel lateral = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        lateral.setOpaque(false);
        lateral.add(botonCerrarSesion);
        encabezado.add(lateral, BorderLayout.EAST);
        return encabezado;
    }

    /**
     * Botón ☰ (arriba a la izquierda) con su menú desplegable. Por ahora tiene una opción,
     * «Mi perfil», que solo está habilitada si hay una sesión activa.
     */
    private JPanel construirMenu() {
        botonMenu.setName("botonMenu");
        botonMenu.setToolTipText("Menú");
        botonMenu.getAccessibleContext().setAccessibleName("Menú");
        botonMenu.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        botonMenu.setOpaque(true);
        botonMenu.setBackground(Estilos.PRIMARIO);
        botonMenu.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6));
        botonMenu.setFocusPainted(false);
        botonMenu.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        botonMenu.getModel().addChangeListener(e -> botonMenu.setBackground(
                botonMenu.getModel().isRollover() || menu.isVisible() ? Estilos.PRIMARIO_HOVER : Estilos.PRIMARIO));

        itemMiPerfil.setName("itemMiPerfil");
        itemMiPerfil.addActionListener(e -> abrirMiPerfil());
        menu.setName("menuPrincipal");
        menu.setBackground(Estilos.TARJETA);
        menu.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(Estilos.BORDE, 1, true),
                javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6)));
        menu.add(itemMiPerfil);

        botonMenu.addActionListener(e -> {
            itemMiPerfil.setEnabled(Sesion.haySesionActiva());
            menu.show(botonMenu, 0, botonMenu.getHeight() + 6); // un pequeño espacio bajo el botón
        });

        JPanel contenedor = new JPanel(new GridBagLayout()); // centra el botón verticalmente
        contenedor.setOpaque(false);
        contenedor.add(botonMenu);
        return contenedor;
    }

    /** «Mi perfil»: abre el perfil del usuario con la sesión activa. */
    private void abrirMiPerfil() {
        Usuario activo = Sesion.getUsuarioActivo();
        if (activo == null) {
            javax.swing.JOptionPane.showMessageDialog(this, "Debe iniciar sesión para ver su perfil.",
                    "Mi perfil", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }
        abrirPerfil(activo.getId());
    }

    /** Abre el perfil de cualquier usuario (propio o ajeno) sin cerrar esta ventana. */
    private void abrirPerfil(String idUsuario) {
        DialogoPerfil.mostrar(this, servicioPerfiles, idUsuario);
    }

    /** Icono de tres rayas (☰) dibujado, para no depender de que la fuente tenga el carácter. */
    private static final class IconoMenu implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            for (int i = 0; i < 3; i++) {
                g2.fillRoundRect(x, y + 2 + i * 8, getIconWidth(), 3, 3, 3);
            }
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 24;
        }

        @Override
        public int getIconHeight() {
            return 22;
        }
    }

    /** Se ejecuta cuando una pestaña cambió una amistad o solicitud: las demás deben reflejarlo. */
    private void amigosCambiaron() {
        panelSolicitudes.refrescar(); // primero: relee el disco para todas las pestañas
        panelAmigos.refrescar();
        panelBuscar.refrescar();
        panelMuro.refrescar();
        actualizarTituloSolicitudes();
    }

    /** Muestra en la pestaña "Solicitudes" cuántas solicitudes recibidas hay pendientes. */
    private void actualizarTituloSolicitudes() {
        int indice = pestanas.indexOfComponent(panelSolicitudes);
        int recibidas = panelSolicitudes.getCantidadRecibidas();
        pestanas.setTitleAt(indice, recibidas > 0 ? "Solicitudes (" + recibidas + ")" : "Solicitudes");
    }

    /** Cierra esta ventana y delega el cierre de sesión (guardar, limpiar, volver al login). */
    private void cerrarSesion() {
        botonCerrarSesion.setEnabled(false); // evita un segundo clic
        dispose();
        cerrarSesion.run();
    }
}
