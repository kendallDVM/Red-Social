package socialuna.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.JTextArea;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultCaret;

import socialuna.datos.ListaDinamica;
import socialuna.modelo.Publicacion;
import socialuna.modelo.Usuario;
import socialuna.servicios.ResultadoPublicacion;
import socialuna.servicios.ServicioPublicaciones;
import socialuna.servicios.Sesion;

/**
 * Pestaña "Muro" del Módulo 3.
 *
 * <p>Arriba se escribe y publica un mensaje; debajo se elige qué muro ver y se muestra la
 * lista de publicaciones, de la más reciente a la más antigua:
 * <ul>
 *   <li><b>Muro general:</b> publicaciones del usuario y de sus amigos.</li>
 *   <li><b>Mi muro:</b> solo las publicaciones del usuario.</li>
 * </ul>
 * La lista se consulta en memoria y se refresca sin cerrar la ventana: al publicar, al
 * cambiar de muro y cuando otra pestaña llama a {@link #refrescar()} (por ejemplo, después
 * de agregar o eliminar un amigo).
 */
public class PanelMuro extends JPanel {

    private static final String TITULO_ALERTA = "Muro";
    private static final DateTimeFormatter FORMATO_VISTA = DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm");
    private static final Color COLOR_PROPIA = new Color(0xFD, 0xF2, 0xF2);

    private final ServicioPublicaciones servicio;

    private final JTextArea areaMensaje = new JTextArea(2, 40);
    private final JLabel lblContador = Estilos.textoSecundario(contador(0));
    private final JButton botonPublicar = Estilos.botonPrimario("Publicar");
    private final JRadioButton radioGeneral = new JRadioButton("Muro general", true);
    private final JRadioButton radioPersonal = new JRadioButton("Mi muro");
    private final JLabel lblResumen = Estilos.textoSecundario(" ");
    private final ListaVertical lista = new ListaVertical();
    private final JScrollPane desplazamiento = new JScrollPane(lista);

    public PanelMuro(ServicioPublicaciones servicio) {
        this.servicio = Objects.requireNonNull(servicio, "El servicio no puede ser nulo.");
        construirInterfaz();
        refrescar();
    }

    private void construirInterfaz() {
        setLayout(new BorderLayout(0, 12));
        setBackground(Estilos.TARJETA);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // ---- parte superior: título, caja para escribir y selector de muro
        JPanel superior = new JPanel();
        superior.setOpaque(false);
        superior.setLayout(new BoxLayout(superior, BoxLayout.Y_AXIS));

        JPanel titulos = new JPanel(new BorderLayout(0, 2));
        titulos.setOpaque(false);
        titulos.add(Estilos.titulo("Muro"), BorderLayout.NORTH);
        titulos.add(Estilos.textoSecundario("Comparte un mensaje con tus amigos y mira lo que publican."),
                BorderLayout.SOUTH);
        superior.add(alinear(titulos));
        superior.add(Box.createVerticalStrut(12));
        superior.add(alinear(construirCajaMensaje()));
        superior.add(Box.createVerticalStrut(12));
        superior.add(alinear(construirSelectorMuro()));
        add(superior, BorderLayout.NORTH);

        // ---- centro: lista de publicaciones con barra de desplazamiento
        desplazamiento.setBorder(BorderFactory.createLineBorder(Estilos.BORDE));
        desplazamiento.getVerticalScrollBar().setUnitIncrement(18);
        desplazamiento.getViewport().setBackground(Estilos.FONDO);
        // El alto de cada mensaje depende del ancho: al cambiar el tamaño de la ventana se recalcula
        desplazamiento.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                lista.revalidate();
            }
        });
        add(desplazamiento, BorderLayout.CENTER);

        // ---- parte inferior: resumen
        lblResumen.setName("resumenMuro");
        add(lblResumen, BorderLayout.SOUTH);
    }

    private JPanel construirCajaMensaje() {
        areaMensaje.setName("areaMensaje");
        areaMensaje.setLineWrap(true);
        areaMensaje.setWrapStyleWord(true);
        areaMensaje.setFont(Estilos.fuente(Font.PLAIN, 14));
        areaMensaje.setForeground(Estilos.TEXTO);
        areaMensaje.setBorder(BorderFactory.createEmptyBorder(7, 9, 7, 9));
        areaMensaje.getAccessibleContext().setAccessibleName("Mensaje a publicar");
        areaMensaje.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                actualizarContador();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                actualizarContador();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                actualizarContador();
            }
        });

        JScrollPane marco = new JScrollPane(areaMensaje);
        marco.setBorder(BorderFactory.createLineBorder(Estilos.BORDE, 1, true));

        lblContador.setName("contadorMensaje");
        botonPublicar.setName("botonPublicar");
        botonPublicar.addActionListener(e -> publicar());

        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        pie.add(lblContador, BorderLayout.WEST);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        botones.setOpaque(false);
        botones.add(botonPublicar);
        pie.add(botones, BorderLayout.EAST);

        JPanel caja = new JPanel(new BorderLayout(0, 8));
        caja.setOpaque(false);
        JLabel etiqueta = Estilos.etiqueta("¿Qué quieres compartir?", areaMensaje, 'Q');
        caja.add(etiqueta, BorderLayout.NORTH);
        caja.add(marco, BorderLayout.CENTER);
        caja.add(pie, BorderLayout.SOUTH);
        return caja;
    }

    private JPanel construirSelectorMuro() {
        radioGeneral.setName("radioMuroGeneral");
        radioPersonal.setName("radioMiMuro");
        for (JRadioButton radio : new JRadioButton[] {radioGeneral, radioPersonal}) {
            radio.setOpaque(false);
            radio.setFont(Estilos.fuente(Font.BOLD, 13));
            radio.setForeground(Estilos.TEXTO);
            radio.setFocusPainted(false);
            radio.addActionListener(e -> refrescar());
        }
        radioGeneral.setToolTipText("Tus publicaciones y las de tus amigos");
        radioPersonal.setToolTipText("Solo tus publicaciones");
        ButtonGroup grupo = new ButtonGroup();
        grupo.add(radioGeneral);
        grupo.add(radioPersonal);

        JPanel selector = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        selector.setOpaque(false);
        selector.add(radioGeneral);
        selector.add(radioPersonal);
        return selector;
    }

    private static JComponent alinear(JComponent componente) {
        componente.setAlignmentX(LEFT_ALIGNMENT);
        return componente;
    }

    // ------------------------------------------------------------------ acciones

    private void publicar() {
        Usuario activo = Sesion.getUsuarioActivo();
        ResultadoPublicacion resultado = servicio.publicar(activo == null ? null : activo.getId(),
                areaMensaje.getText());
        if (resultado.esExitoso()) {
            areaMensaje.setText("");
            radioGeneral.setSelected(true); // la publicación nueva se ve en ambos muros
            refrescar();
            // Mostrar lo más reciente (está arriba)
            SwingUtilities.invokeLater(() -> desplazamiento.getVerticalScrollBar().setValue(0));
            JOptionPane.showMessageDialog(this, resultado.getMensaje(), TITULO_ALERTA,
                    JOptionPane.INFORMATION_MESSAGE);
            areaMensaje.requestFocusInWindow();
            return;
        }
        int icono = resultado.getTipo() == ResultadoPublicacion.Tipo.ERROR_PERSISTENCIA
                ? JOptionPane.ERROR_MESSAGE : JOptionPane.WARNING_MESSAGE;
        JOptionPane.showMessageDialog(this, resultado.getMensaje(), TITULO_ALERTA, icono);
        areaMensaje.requestFocusInWindow();
    }

    /**
     * Vuelve a cargar el muro elegido del usuario activo. Antes relee el disco, para incluir
     * lo que publicaron o aceptaron desde otra instancia del programa.
     */
    public final void refrescar() {
        refrescar(true);
    }

    /** @param alInicio {@code true} para volver arriba (lo más reciente); {@code false} para respetar la posición */
    private void refrescar(boolean alInicio) {
        servicio.actualizarDesdeDisco();
        lista.removeAll();
        Usuario activo = Sesion.getUsuarioActivo();
        if (activo == null) {
            lblResumen.setText(" ");
            lista.revalidate();
            lista.repaint();
            return;
        }

        boolean general = radioGeneral.isSelected();
        ListaDinamica<Publicacion> publicaciones = general
                ? servicio.obtenerMuro(activo.getId())
                : servicio.obtenerMuroPersonal(activo.getId());

        if (publicaciones.estaVacia()) {
            lista.add(crearMensajeVacio(general));
        } else {
            for (Publicacion publicacion : publicaciones) {
                lista.add(crearTarjeta(publicacion, activo));
            }
        }
        int cantidad = publicaciones.tamano();
        lblResumen.setText(cantidad == 1 ? "1 publicación" : cantidad + " publicaciones");
        lista.revalidate();
        lista.repaint();
        if (alInicio) {
            // Lo más reciente está arriba: el muro se muestra desde el inicio
            SwingUtilities.invokeLater(() -> desplazamiento.getVerticalScrollBar().setValue(0));
        }
    }

    // ------------------------------------------------------------------ tarjetas

    private JComponent crearMensajeVacio(boolean general) {
        JLabel vacio = new JLabel(general
                ? "Aún no hay publicaciones. ¡Escribe la primera o agrega amigos!"
                : "Aún no has publicado nada.", SwingConstants.CENTER);
        vacio.setName("muroVacio");
        vacio.setFont(Estilos.fuente(Font.PLAIN, 13));
        vacio.setForeground(Estilos.TEXTO_SECUNDARIO);
        vacio.setBorder(BorderFactory.createEmptyBorder(30, 10, 30, 10));
        return vacio;
    }

    private JComponent crearTarjeta(Publicacion publicacion, Usuario activo) {
        Usuario autor = servicio.obtenerAutor(publicacion);
        boolean propia = publicacion.getIdAutor().equals(activo.getId());

        String nombre = autor == null ? "Usuario desconocido" : autor.getNombre() + (propia ? " (tú)" : "");
        JLabel lblNombre = new JLabel(nombre);
        lblNombre.setFont(Estilos.fuente(Font.BOLD, 14));
        lblNombre.setForeground(Estilos.TEXTO);

        JLabel lblUsuario = Estilos.textoSecundario(autor == null ? "" : "  @" + autor.getUsuario());
        JPanel quien = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        quien.setOpaque(false);
        quien.add(lblNombre);
        quien.add(lblUsuario);

        JLabel lblFecha = Estilos.textoSecundario(publicacion.getFechaHoraComoFecha().format(FORMATO_VISTA));

        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setOpaque(false);
        encabezado.add(quien, BorderLayout.WEST);
        encabezado.add(lblFecha, BorderLayout.EAST);

        JTextArea texto = new JTextArea(publicacion.getTexto());
        texto.setEditable(false);
        texto.setFocusable(false);
        texto.setOpaque(false);
        texto.setLineWrap(true);
        texto.setWrapStyleWord(true);
        texto.setFont(Estilos.fuente(Font.PLAIN, 14));
        texto.setForeground(Estilos.TEXTO);
        texto.setBorder(BorderFactory.createEmptyBorder());
        // Sin esto, el cursor interno del texto hace que el muro se desplace solo hacia abajo
        ((DefaultCaret) texto.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

        JPanel tarjeta = new JPanel(new BorderLayout(0, 6));
        tarjeta.setName("publicacion-" + publicacion.getId());
        tarjeta.setBackground(propia ? COLOR_PROPIA : Estilos.TARJETA);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilos.BORDE),
                BorderFactory.createEmptyBorder(10, 14, 12, 14)));
        tarjeta.add(encabezado, BorderLayout.NORTH);
        tarjeta.add(texto, BorderLayout.CENTER);
        tarjeta.add(crearPie(publicacion, activo, propia), BorderLayout.SOUTH);
        return tarjeta;
    }

    /**
     * Pie de la tarjeta: botón "Me gusta" a la izquierda y, si la publicación es del usuario
     * activo, botón "Eliminar" a la derecha.
     */
    private JComponent crearPie(Publicacion publicacion, Usuario activo, boolean propia) {
        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        pie.add(crearPieMeGusta(publicacion, activo), BorderLayout.WEST);
        if (propia) {
            JButton eliminar = Estilos.botonSecundario("Eliminar");
            eliminar.setName("eliminar-" + publicacion.getId());
            eliminar.setFont(Estilos.fuente(Font.BOLD, 12));
            eliminar.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
            eliminar.setFocusable(false);
            eliminar.setToolTipText("Eliminar esta publicación");
            eliminar.addActionListener(e -> eliminarPublicacion(publicacion));
            JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            derecha.setOpaque(false);
            derecha.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
            derecha.add(eliminar);
            pie.add(derecha, BorderLayout.EAST);
        }
        return pie;
    }

    /** Botón "Me gusta" con el total; cambia de color cuando al usuario activo le gusta la publicación. */
    private JComponent crearPieMeGusta(Publicacion publicacion, Usuario activo) {
        boolean leGusta = publicacion.leGustaA(activo.getId());
        int total = publicacion.cantidadMeGusta();
        String texto = (leGusta ? "Te gusta" : "Me gusta") + (total > 0 ? "  ·  " + total : "");

        JButton boton = leGusta ? Estilos.botonPrimario(texto) : Estilos.botonSecundario(texto);
        boton.setName("meGusta-" + publicacion.getId());
        boton.setFont(Estilos.fuente(Font.BOLD, 12));
        boton.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        boton.setFocusable(false);
        boton.setToolTipText(descripcionMeGusta(publicacion));
        boton.addActionListener(e -> alternarMeGusta(publicacion));

        JPanel pie = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pie.setOpaque(false);
        pie.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        pie.add(boton);
        return pie;
    }

    /** Texto emergente: a quiénes les gusta la publicación. */
    private String descripcionMeGusta(Publicacion publicacion) {
        ListaDinamica<String> nombres = servicio.obtenerNombresMeGusta(publicacion);
        if (nombres.estaVacia()) {
            return "Nadie ha dado me gusta todavía";
        }
        StringBuilder sb = new StringBuilder("Les gusta a: ");
        for (int i = 0; i < nombres.tamano(); i++) {
            sb.append(i > 0 ? ", " : "").append(nombres.obtener(i));
        }
        return sb.toString();
    }

    private void alternarMeGusta(Publicacion publicacion) {
        Usuario activo = Sesion.getUsuarioActivo();
        ResultadoPublicacion resultado = servicio.alternarMeGusta(activo == null ? null : activo.getId(),
                publicacion.getId());
        int posicion = desplazamiento.getVerticalScrollBar().getValue();
        if (!resultado.esExitoso()) {
            int icono = resultado.getTipo() == ResultadoPublicacion.Tipo.ERROR_PERSISTENCIA
                    ? JOptionPane.ERROR_MESSAGE : JOptionPane.WARNING_MESSAGE;
            JOptionPane.showMessageDialog(this, resultado.getMensaje(), TITULO_ALERTA, icono);
        }
        refrescar(false); // se actualiza el botón sin saltar al inicio de la lista
        SwingUtilities.invokeLater(() -> desplazamiento.getVerticalScrollBar().setValue(posicion));
    }

    /** Pide confirmación y elimina una publicación propia; el muro se refresca sin saltar al inicio. */
    private void eliminarPublicacion(Publicacion publicacion) {
        int opcion = JOptionPane.showConfirmDialog(this,
                "¿Seguro que desea eliminar esta publicación?\nNo se podrá recuperar.",
                "Eliminar publicación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }
        Usuario activo = Sesion.getUsuarioActivo();
        ResultadoPublicacion resultado = servicio.eliminarPublicacion(activo == null ? null : activo.getId(),
                publicacion.getId());
        int posicion = desplazamiento.getVerticalScrollBar().getValue();
        refrescar(false);
        SwingUtilities.invokeLater(() -> desplazamiento.getVerticalScrollBar().setValue(posicion));
        if (resultado.esExitoso()) {
            JOptionPane.showMessageDialog(this, resultado.getMensaje(), TITULO_ALERTA,
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            int icono = resultado.getTipo() == ResultadoPublicacion.Tipo.ERROR_PERSISTENCIA
                    ? JOptionPane.ERROR_MESSAGE : JOptionPane.WARNING_MESSAGE;
            JOptionPane.showMessageDialog(this, resultado.getMensaje(), TITULO_ALERTA, icono);
        }
    }

    // ------------------------------------------------------------------ contador

    private void actualizarContador() {
        int largo = areaMensaje.getText().strip().length();
        lblContador.setText(contador(largo));
        lblContador.setForeground(largo > ServicioPublicaciones.MAX_CARACTERES
                ? Estilos.PRIMARIO : Estilos.TEXTO_SECUNDARIO);
    }

    private static String contador(int largo) {
        return largo + " / " + ServicioPublicaciones.MAX_CARACTERES;
    }

    /**
     * Panel vertical que se ajusta al ancho de la ventana, para que los mensajes largos
     * se partan en varias líneas en vez de mostrar una barra horizontal.
     *
     * <p>Usa su propia disposición porque {@code BoxLayout} calcula el alto de un
     * {@code JTextArea} con salto de línea antes de conocer el ancho real y cortaría el texto.
     */
    private static final class ListaVertical extends JPanel implements Scrollable {

        private static final int SEPARACION = 8;
        private static final int ANCHO_MINIMO = 200;

        ListaVertical() {
            setLayout(new DisposicionTarjetas());
            setBackground(Estilos.FONDO);
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        }

        /** El alto depende del ancho: se calcula con el ancho actual del visor. */
        @Override
        public Dimension getPreferredSize() {
            Container padre = getParent();
            int ancho = padre instanceof JViewport ? padre.getWidth() : getWidth();
            return ((DisposicionTarjetas) getLayout()).medir(this, ancho, false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return new Dimension(400, 240);
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 18;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(visible.height - 40, 18);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }

        /** Apila los componentes a todo el ancho, con el alto que necesita cada uno a ese ancho. */
        private static final class DisposicionTarjetas implements LayoutManager {

            @Override
            public void addLayoutComponent(String nombre, Component componente) {
            }

            @Override
            public void removeLayoutComponent(Component componente) {
            }

            @Override
            public Dimension preferredLayoutSize(Container padre) {
                return medir(padre, padre.getWidth(), false);
            }

            @Override
            public Dimension minimumLayoutSize(Container padre) {
                return medir(padre, padre.getWidth(), false);
            }

            @Override
            public void layoutContainer(Container padre) {
                medir(padre, padre.getWidth(), true);
            }

            /**
             * @param anchoTotal ancho del panel; si es menor que el mínimo se usa el mínimo
             * @param colocar    si es {@code true}, además asigna la posición y el tamaño de cada componente
             */
            Dimension medir(Container padre, int anchoTotal, boolean colocar) {
                Insets margen = padre.getInsets();
                int ancho = Math.max(anchoTotal - margen.left - margen.right, ANCHO_MINIMO);
                int y = margen.top;
                boolean primero = true;
                for (Component c : padre.getComponents()) {
                    if (!c.isVisible()) {
                        continue;
                    }
                    if (!primero) {
                        y += SEPARACION;
                    }
                    primero = false;
                    Rectangle anterior = c.getBounds();
                    // Darle el ancho antes de pedir el alto: así el texto ya sabe dónde parte las líneas.
                    // El alto debe ser grande: un JTextArea solo recalcula sus líneas con ancho y alto válidos.
                    c.setSize(ancho, Short.MAX_VALUE);
                    c.doLayout();
                    int alto = c.getPreferredSize().height;
                    c.setBounds(colocar ? new Rectangle(margen.left, y, ancho, alto) : anterior);
                    if (colocar) {
                        c.doLayout();
                    }
                    y += alto;
                }
                return new Dimension(ancho + margen.left + margen.right, y + margen.bottom);
            }
        }
    }
}
