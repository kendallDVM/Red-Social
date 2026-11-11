package socialuna.gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;

import socialuna.modelo.CampoPerfil;
import socialuna.modelo.Perfil;
import socialuna.modelo.Usuario;
import socialuna.servicios.ResultadoPerfil;
import socialuna.servicios.ServicioPerfiles;
import socialuna.servicios.Sesion;
import socialuna.servicios.VistaPerfil;

/**
 * Ventana de perfil (diálogo modal sobre la ventana principal, que no se cierra).
 *
 * <p>Tiene dos modos claramente separados:
 * <ul>
 *   <li><b>Consulta:</b> encabezado con iniciales, nombre y usuario; secciones "Sobre mí",
 *       "Información académica" y "Redes sociales" con los campos completados (los vacíos no
 *       se muestran). En el perfil propio se agrega "Datos de la cuenta", visible solo para
 *       el dueño, y el botón «Editar perfil».</li>
 *   <li><b>Edición</b> (solo el perfil propio): formulario con los campos opcionales,
 *       «Guardar cambios» y «Cancelar».</li>
 * </ul>
 * Toda la lógica y la privacidad están en {@link ServicioPerfiles}.
 */
public class DialogoPerfil extends JDialog {

    private static final String MODO_CONSULTA = "consulta";
    private static final String MODO_EDICION = "edicion";

    private final ServicioPerfiles servicio;
    private final String idUsuario;

    private final CardLayout tarjetas = new CardLayout();
    private final JPanel contenido = new JPanel(tarjetas);
    private final JPanel consulta = new JPanel(new BorderLayout());
    private final Map<CampoPerfil, JTextComponent> campos = new EnumMap<>(CampoPerfil.class);
    private Perfil perfilAlEditar;
    /** Formulario de edición abierto (se reemplaza cada vez que se pulsa «Editar perfil»). */
    private JPanel edicion;

    /**
     * Abre el perfil de {@code idUsuario} sobre {@code padre}. Si el usuario no existe, avisa.
     */
    public static void mostrar(Component padre, ServicioPerfiles servicio, String idUsuario) {
        Window ventana = padre == null ? null : SwingUtilities.getWindowAncestor(padre);
        if (padre instanceof Window) {
            ventana = (Window) padre;
        }
        VistaPerfil vista = servicio.verPerfil(idActivo(), idUsuario);
        if (vista == null) {
            JOptionPane.showMessageDialog(padre, "No se encontró el usuario seleccionado.", "Perfil",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        new DialogoPerfil(ventana, servicio, idUsuario, vista).setVisible(true);
    }

    private DialogoPerfil(Window padre, ServicioPerfiles servicio, String idUsuario, VistaPerfil vista) {
        super(padre, vista.esPropio() ? "Mi perfil" : "Perfil de " + vista.getNombre(),
                ModalityType.APPLICATION_MODAL);
        this.servicio = Objects.requireNonNull(servicio);
        this.idUsuario = Objects.requireNonNull(idUsuario);
        setName("dialogoPerfil");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        contenido.add(consulta, MODO_CONSULTA);
        setContentPane(contenido);
        mostrarConsulta(vista);

        // Nunca más grande que el área útil de la pantalla (sin la barra de tareas).
        Rectangle util = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(620, util.width - 40), Math.min(700, util.height - 40));
        setMinimumSize(new Dimension(Math.min(460, util.width), Math.min(420, util.height)));
        setLocationRelativeTo(padre);
    }

    // ------------------------------------------------------------------ modo consulta

    private void mostrarConsulta(VistaPerfil vista) {
        consulta.removeAll();
        consulta.setBackground(Estilos.TARJETA);
        consulta.add(construirCabecera(vista), BorderLayout.NORTH);

        JPanel cuerpo = new PanelDesplazable();
        cuerpo.setLayout(new GridBagLayout());
        cuerpo.setBackground(Estilos.TARJETA);
        cuerpo.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        GridBagConstraints c = filaCompleta();

        if (!vista.tieneInformacion()) {
            String mensaje = vista.esPropio()
                    ? "Todavía no agregaste información a tu perfil. Pulsa «Editar perfil» para contar algo sobre ti."
                    : vista.getNombre() + " todavía no ha agregado información a su perfil.";
            JLabel vacio = new JLabel("<html>" + escapar(mensaje) + "</html>");
            vacio.setName("mensajePerfilVacio");
            vacio.setFont(Estilos.fuente(Font.ITALIC, 13));
            vacio.setForeground(Estilos.TEXTO_SECUNDARIO);
            c.insets = new Insets(4, 0, 12, 0);
            cuerpo.add(vacio, c);
        }

        for (CampoPerfil.Seccion seccion : CampoPerfil.Seccion.values()) {
            var datos = vista.getDatos(seccion);
            if (datos.estaVacia()) {
                continue; // las secciones sin datos no se muestran
            }
            c.insets = new Insets(6, 0, 6, 0);
            cuerpo.add(tituloSeccion(seccion.getTitulo()), c);
            for (VistaPerfil.Dato dato : datos) {
                c.insets = new Insets(0, 0, 10, 0);
                cuerpo.add(filaDato(dato), c);
            }
        }

        if (vista.esPropio()) {
            c.insets = new Insets(10, 0, 6, 0);
            cuerpo.add(tituloSeccion("Datos de la cuenta (solo tú los ves)"), c);
            c.insets = new Insets(0, 0, 4, 0);
            cuerpo.add(filaTexto("ID", vista.getIdCuenta(), false), c);
            c.insets = new Insets(0, 0, 10, 0);
            cuerpo.add(Estilos.textoSecundario(
                    "Tu ID y tu contraseña no se muestran a otros usuarios ni se editan aquí."), c);
        }

        // relleno para que el contenido quede arriba
        c.weighty = 1;
        cuerpo.add(Box.createVerticalGlue(), c);

        JScrollPane desplazamiento = new JScrollPane(cuerpo);
        desplazamiento.setBorder(BorderFactory.createEmptyBorder());
        desplazamiento.getVerticalScrollBar().setUnitIncrement(16);
        consulta.add(desplazamiento, BorderLayout.CENTER);

        JPanel botones = barraBotones();
        JButton principal;
        if (vista.esPropio()) {
            JButton editar = Estilos.botonPrimario("Editar perfil");
            editar.setName("botonEditarPerfil");
            editar.addActionListener(e -> mostrarEdicion());
            botones.add(editar);
            botones.add(Box.createHorizontalStrut(8));
            principal = editar;
        } else {
            principal = null;
        }
        JButton cerrar = Estilos.botonSecundario("Cerrar");
        cerrar.setName("botonCerrarPerfil");
        cerrar.addActionListener(e -> dispose());
        botones.add(cerrar);
        consulta.add(botones, BorderLayout.SOUTH);

        consulta.revalidate();
        consulta.repaint();
        tarjetas.show(contenido, MODO_CONSULTA);
        // El foco va al botón principal y el perfil se muestra desde arriba.
        JButton conFoco = principal != null ? principal : cerrar;
        SwingUtilities.invokeLater(() -> {
            conFoco.requestFocusInWindow();
            desplazamiento.getVerticalScrollBar().setValue(0);
        });
        setTitle(vista.esPropio() ? "Mi perfil" : "Perfil de " + vista.getNombre());
    }

    /** Franja superior: avatar con iniciales, nombre y usuario. */
    private JPanel construirCabecera(VistaPerfil vista) {
        JPanel cabecera = new JPanel(new BorderLayout(16, 0));
        cabecera.setBackground(Estilos.PRIMARIO);
        cabecera.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        cabecera.add(new Avatar(vista.getNombre()), BorderLayout.WEST);

        JPanel textos = new JPanel(new BorderLayout(0, 2));
        textos.setOpaque(false);
        JLabel nombre = new JLabel(vista.getNombre());
        nombre.setName("perfilNombre");
        nombre.setFont(Estilos.fuente(Font.BOLD, 22));
        nombre.setForeground(Color.WHITE);
        JLabel usuario = new JLabel("@" + vista.getUsuario() + (vista.esPropio() ? "   ·   Este es tu perfil" : ""));
        usuario.setName("perfilUsuario");
        usuario.setFont(Estilos.fuente(Font.PLAIN, 13));
        usuario.setForeground(new Color(0xFF, 0xE4, 0xE4));
        textos.add(nombre, BorderLayout.CENTER);
        textos.add(usuario, BorderLayout.SOUTH);
        cabecera.add(textos, BorderLayout.CENTER);
        return cabecera;
    }

    private static JComponent filaDato(VistaPerfil.Dato dato) {
        CampoPerfil campo = dato.getCampo();
        boolean sinEtiqueta = campo == CampoPerfil.BIOGRAFIA; // la biografía se lee como párrafo
        return filaTexto(sinEtiqueta ? null : campo.getEtiqueta(), dato.getValor(), true);
    }

    /** Etiqueta pequeña en gris y valor seleccionable (se puede copiar un enlace). */
    private static JComponent filaTexto(String etiqueta, String valor, boolean ajustar) {
        JPanel fila = new JPanel(new BorderLayout(0, 2));
        fila.setOpaque(false);
        if (etiqueta != null) {
            JLabel lbl = new JLabel(etiqueta);
            lbl.setFont(Estilos.fuente(Font.BOLD, 12));
            lbl.setForeground(Estilos.TEXTO_SECUNDARIO);
            fila.add(lbl, BorderLayout.NORTH);
        }
        JTextArea texto = new JTextArea(valor);
        texto.setEditable(false);
        texto.setLineWrap(ajustar);
        texto.setWrapStyleWord(true);
        texto.setOpaque(false);
        texto.setBorder(BorderFactory.createEmptyBorder());
        texto.setFont(Estilos.fuente(Font.PLAIN, 14));
        texto.setForeground(Estilos.TEXTO);
        // Solo lectura: se puede seleccionar y copiar (por ejemplo, un enlace), pero sin cursor.
        texto.setCaret(new DefaultCaret() {
            @Override
            public void setVisible(boolean visible) {
                super.setVisible(false);
            }
        });
        fila.add(texto, BorderLayout.CENTER);
        return fila;
    }

    // ------------------------------------------------------------------ modo edición

    private void mostrarEdicion() {
        perfilAlEditar = servicio.obtenerPerfilEditable(idActivo());
        if (perfilAlEditar == null || !idUsuario.equals(perfilAlEditar.getIdUsuario())) {
            JOptionPane.showMessageDialog(this, "Solo puede editar su propio perfil.", "Editar perfil",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (edicion != null) {
            contenido.remove(edicion);
        }
        edicion = new JPanel(new BorderLayout());
        edicion.setName("panelEdicionPerfil");
        edicion.setBackground(Estilos.TARJETA);

        JPanel cabecera = new JPanel(new BorderLayout(0, 2));
        cabecera.setBackground(Estilos.TARJETA);
        cabecera.setBorder(BorderFactory.createEmptyBorder(18, 24, 8, 24));
        cabecera.add(Estilos.titulo("Editar perfil"), BorderLayout.NORTH);
        cabecera.add(Estilos.textoSecundario("<html>Todos los campos son opcionales. Lo que completes será visible"
                + " para los demás usuarios. Tu nombre, usuario, ID y contraseña no se cambian aquí.</html>"),
                BorderLayout.SOUTH);
        edicion.add(cabecera, BorderLayout.NORTH);

        JPanel formulario = new PanelDesplazable();
        formulario.setLayout(new GridBagLayout());
        formulario.setBackground(Estilos.TARJETA);
        formulario.setBorder(BorderFactory.createEmptyBorder(4, 24, 12, 24));
        GridBagConstraints c = filaCompleta();
        campos.clear();

        CampoPerfil.Seccion seccionActual = null;
        for (CampoPerfil campo : CampoPerfil.values()) {
            if (campo.getSeccion() != seccionActual) {
                seccionActual = campo.getSeccion();
                c.insets = new Insets(12, 0, 6, 0);
                formulario.add(tituloSeccion(seccionActual.getTitulo()), c);
            }
            String valor = perfilAlEditar.obtener(campo);
            c.insets = new Insets(0, 0, 10, 0);
            formulario.add(campoFormulario(campo, valor == null ? "" : valor), c);
        }
        c.weighty = 1;
        formulario.add(Box.createVerticalGlue(), c);

        JScrollPane desplazamiento = new JScrollPane(formulario);
        desplazamiento.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Estilos.BORDE));
        desplazamiento.getVerticalScrollBar().setUnitIncrement(16);
        edicion.add(desplazamiento, BorderLayout.CENTER);

        JPanel botones = barraBotones();
        JButton guardar = Estilos.botonPrimario("Guardar cambios");
        guardar.setName("botonGuardarPerfil");
        guardar.addActionListener(e -> guardar());
        JButton cancelar = Estilos.botonSecundario("Cancelar");
        cancelar.setName("botonCancelarEdicion");
        cancelar.addActionListener(e -> cancelarEdicion());
        botones.add(guardar);
        botones.add(Box.createHorizontalStrut(8));
        botones.add(cancelar);
        edicion.add(botones, BorderLayout.SOUTH);

        contenido.add(edicion, MODO_EDICION);
        tarjetas.show(contenido, MODO_EDICION);
        setTitle("Editar mi perfil");
        campos.get(CampoPerfil.BIOGRAFIA).requestFocusInWindow();
    }

    /** Etiqueta, ayuda y control de un campo del formulario. */
    private JComponent campoFormulario(CampoPerfil campo, String valor) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setOpaque(false);
        JLabel etiqueta = new JLabel(campo.getEtiqueta());
        etiqueta.setFont(Estilos.fuente(Font.BOLD, 13));
        etiqueta.setForeground(Estilos.TEXTO);
        encabezado.add(etiqueta, BorderLayout.WEST);
        JLabel contador = Estilos.textoSecundario("");
        encabezado.add(contador, BorderLayout.EAST);
        panel.add(encabezado, BorderLayout.NORTH);

        JTextComponent control;
        if (campo.getTipo() == CampoPerfil.Tipo.TEXTO_LARGO) {
            JTextArea area = new JTextArea(valor, 4, 30);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setFont(Estilos.fuente(Font.PLAIN, 14));
            area.setForeground(Estilos.TEXTO);
            area.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
            JScrollPane sp = new JScrollPane(area);
            sp.setBorder(BorderFactory.createLineBorder(Estilos.BORDE, 1, true));
            panel.add(sp, BorderLayout.CENTER);
            control = area;
        } else {
            JTextField texto = Estilos.campoTexto(30);
            texto.setText(valor);
            panel.add(texto, BorderLayout.CENTER);
            control = texto;
        }
        control.setName("campo_" + campo.name());
        etiqueta.setLabelFor(control);
        // Al recibir el foco (con Tab o al señalar un error) el campo completo se desplaza a la
        // vista. JTextField.scrollRectToVisible solo desplaza su propio texto, por eso se pide
        // al panel contenedor, que sí lo propaga al área de desplazamiento del formulario.
        control.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                panel.scrollRectToVisible(new Rectangle(0, 0, panel.getWidth(), panel.getHeight()));
            }
        });
        if (campo.getTipo() == CampoPerfil.Tipo.ENLACE) {
            panel.add(Estilos.textoSecundario("Enlace https://… o tu usuario, por ejemplo @ana.mora"),
                    BorderLayout.SOUTH);
        }
        // contador de caracteres
        Runnable actualizar = () -> {
            int largo = control.getText().strip().length();
            contador.setText(largo + "/" + campo.getLargoMaximo());
            contador.setForeground(largo > campo.getLargoMaximo() ? Estilos.PRIMARIO : Estilos.TEXTO_SECUNDARIO);
        };
        control.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { actualizar.run(); }
            @Override public void removeUpdate(DocumentEvent e) { actualizar.run(); }
            @Override public void changedUpdate(DocumentEvent e) { actualizar.run(); }
        });
        actualizar.run();
        campos.put(campo, control);
        return panel;
    }

    /** Perfil con lo que hay escrito en el formulario. */
    private Perfil leerFormulario() {
        Perfil perfil = new Perfil(idUsuario);
        for (Map.Entry<CampoPerfil, JTextComponent> e : campos.entrySet()) {
            perfil.establecer(e.getKey(), e.getValue().getText());
        }
        return perfil;
    }

    private void guardar() {
        ResultadoPerfil resultado = servicio.guardarPerfil(idActivo(), leerFormulario());
        if (!resultado.esExitoso()) {
            int icono = resultado.getTipo() == ResultadoPerfil.Tipo.ERROR_PERSISTENCIA
                    ? JOptionPane.ERROR_MESSAGE : JOptionPane.WARNING_MESSAGE;
            JOptionPane.showMessageDialog(this, resultado.getMensaje(), "Editar perfil", icono);
            if (resultado.getCampo() != null) {
                campos.get(resultado.getCampo()).requestFocusInWindow();
            }
            return;
        }
        VistaPerfil vista = servicio.verPerfil(idActivo(), idUsuario);
        if (vista != null) {
            mostrarConsulta(vista);
        }
        JOptionPane.showMessageDialog(this, resultado.getMensaje(), "Perfil guardado",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void cancelarEdicion() {
        boolean hayCambios = false;
        Perfil escrito = leerFormulario();
        for (CampoPerfil campo : CampoPerfil.values()) {
            if (!Objects.equals(escrito.obtener(campo), perfilAlEditar.obtener(campo))) {
                hayCambios = true;
                break;
            }
        }
        if (hayCambios && JOptionPane.showConfirmDialog(this, "¿Desea descartar los cambios sin guardar?",
                "Cancelar edición", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }
        tarjetas.show(contenido, MODO_CONSULTA);
        setTitle("Mi perfil");
        Component editar = PanelBuscador.buscar(consulta, "botonEditarPerfil");
        if (editar != null) {
            editar.requestFocusInWindow();
        }
    }

    /** Búsqueda de un componente por nombre dentro de un contenedor. */
    private static final class PanelBuscador {
        static Component buscar(java.awt.Container raiz, String nombre) {
            for (Component c : raiz.getComponents()) {
                if (nombre.equals(c.getName())) {
                    return c;
                }
                if (c instanceof java.awt.Container) {
                    Component r = buscar((java.awt.Container) c, nombre);
                    if (r != null) {
                        return r;
                    }
                }
            }
            return null;
        }
    }

    // ------------------------------------------------------------------ utilidades

    private static JLabel tituloSeccion(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(Estilos.fuente(Font.BOLD, 15));
        lbl.setForeground(Estilos.PRIMARIO);
        lbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Estilos.BORDE),
                BorderFactory.createEmptyBorder(0, 0, 4, 0)));
        return lbl;
    }

    private static JPanel barraBotones() {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        barra.setBackground(Estilos.FONDO);
        barra.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Estilos.BORDE),
                BorderFactory.createEmptyBorder(12, 24, 12, 24)));
        return barra;
    }

    private static GridBagConstraints filaCompleta() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        return c;
    }

    private static String idActivo() {
        Usuario activo = Sesion.getUsuarioActivo();
        return activo == null ? null : activo.getId();
    }

    private static String escapar(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Panel que se ajusta al ancho del área de desplazamiento (así el texto se ajusta en líneas). */
    private static final class PanelDesplazable extends JPanel implements Scrollable {
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() {
            return getParent() != null && getParent().getHeight() > getPreferredSize().height;
        }
    }

    /** Círculo con las iniciales del nombre (no se usan fotos en esta versión). */
    private static final class Avatar extends JPanel {
        private static final int TAMANO = 64;
        private final String iniciales;

        Avatar(String nombre) {
            this.iniciales = iniciales(nombre);
            setOpaque(false);
            setPreferredSize(new Dimension(TAMANO, TAMANO));
            getAccessibleContext().setAccessibleName("Iniciales " + iniciales);
        }

        private static String iniciales(String nombre) {
            StringBuilder sb = new StringBuilder();
            for (String parte : nombre.strip().split("\\s+")) {
                if (!parte.isEmpty() && sb.length() < 2) {
                    sb.appendCodePoint(Character.toUpperCase(parte.codePointAt(0)));
                }
            }
            return sb.length() == 0 ? "?" : sb.toString();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillOval(0, 0, TAMANO - 1, TAMANO - 1);
            g2.setColor(new Color(0xFF, 0xE4, 0xE4));
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(1, 1, TAMANO - 3, TAMANO - 3);
            g2.setColor(Estilos.PRIMARIO);
            g2.setFont(Estilos.fuente(Font.BOLD, 24));
            FontMetrics fm = g2.getFontMetrics();
            int x = (TAMANO - fm.stringWidth(iniciales)) / 2;
            int y = (TAMANO - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(iniciales, x, y);
            g2.dispose();
        }
    }
}
