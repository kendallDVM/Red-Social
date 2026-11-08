package socialuna.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.text.JTextComponent;

/**
 * Colores, fuentes y componentes con el estilo visual común de Social UNA.
 *
 * <p>Es pública para que las ventanas de los demás módulos (por ejemplo,
 * {@code VentanaPrincipal}) puedan usar la misma apariencia.
 */
public final class Estilos {

    public static final Color PRIMARIO = new Color(0xB0, 0x1E, 0x23);
    public static final Color PRIMARIO_HOVER = new Color(0x8C, 0x15, 0x1A);
    public static final Color FONDO = new Color(0xF3, 0xF4, 0xF6);
    public static final Color TARJETA = Color.WHITE;
    public static final Color TEXTO = new Color(0x1F, 0x29, 0x37);
    public static final Color TEXTO_SECUNDARIO = new Color(0x6B, 0x72, 0x80);
    public static final Color BORDE = new Color(0xD1, 0xD5, 0xDB);
    public static final Color SECUNDARIO = new Color(0xE5, 0xE7, 0xEB);
    public static final Color SECUNDARIO_HOVER = new Color(0xD1, 0xD5, 0xDB);

    private static final String FAMILIA = elegirFamilia();

    private Estilos() {
    }

    /** Usa la apariencia nativa del sistema operativo (si no está disponible, la de Java). */
    public static void aplicarLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Se mantiene la apariencia por defecto de Swing; no impide usar la aplicación.
        }
    }

    public static Font fuente(int estilo, float tamano) {
        return new Font(FAMILIA, estilo, Math.round(tamano));
    }

    /** Franja superior con el nombre de la aplicación y un subtítulo. */
    public static JPanel crearEncabezado(String titulo, String subtitulo) {
        JPanel encabezado = new JPanel(new BorderLayout(0, 4));
        encabezado.setBackground(PRIMARIO);
        encabezado.setBorder(BorderFactory.createEmptyBorder(22, 28, 20, 28));

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(fuente(Font.BOLD, 26));
        lblTitulo.setForeground(Color.WHITE);

        JLabel lblSubtitulo = new JLabel(subtitulo);
        lblSubtitulo.setFont(fuente(Font.PLAIN, 13));
        lblSubtitulo.setForeground(new Color(0xFF, 0xE4, 0xE4));

        encabezado.add(lblTitulo, BorderLayout.NORTH);
        encabezado.add(lblSubtitulo, BorderLayout.SOUTH);
        return encabezado;
    }

    public static JLabel titulo(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(fuente(Font.BOLD, 18));
        etiqueta.setForeground(TEXTO);
        return etiqueta;
    }

    public static JLabel textoSecundario(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(fuente(Font.PLAIN, 12));
        etiqueta.setForeground(TEXTO_SECUNDARIO);
        return etiqueta;
    }

    /** Etiqueta de un campo, asociada a él para accesibilidad y con tecla de acceso (Alt + letra). */
    public static JLabel etiqueta(String texto, JComponent campo, char teclaAcceso) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(fuente(Font.BOLD, 13));
        etiqueta.setForeground(TEXTO);
        etiqueta.setLabelFor(campo);
        etiqueta.setDisplayedMnemonic(teclaAcceso);
        return etiqueta;
    }

    public static JTextField campoTexto(int columnas) {
        JTextField campo = new JTextField(columnas);
        darEstiloCampo(campo);
        return campo;
    }

    public static JPasswordField campoContrasena(int columnas) {
        JPasswordField campo = new JPasswordField(columnas);
        darEstiloCampo(campo);
        return campo;
    }

    /** Botón de la acción principal (fondo de color). */
    public static JButton botonPrimario(String texto) {
        return boton(texto, PRIMARIO, PRIMARIO_HOVER, Color.WHITE);
    }

    /** Botón de una acción secundaria (fondo gris claro). */
    public static JButton botonSecundario(String texto) {
        return boton(texto, SECUNDARIO, SECUNDARIO_HOVER, TEXTO);
    }

    private static JButton boton(String texto, Color fondo, Color fondoHover, Color colorTexto) {
        JButton boton = new JButton(texto);
        // BasicButtonUI respeta el color de fondo con cualquier apariencia del sistema.
        boton.setUI(new BasicButtonUI());
        boton.setFont(fuente(Font.BOLD, 14));
        boton.setBackground(fondo);
        boton.setForeground(colorTexto);
        boton.setOpaque(true);
        boton.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        boton.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        boton.getModel().addChangeListener(e ->
                boton.setBackground(boton.getModel().isRollover() ? fondoHover : fondo));
        return boton;
    }

    private static void darEstiloCampo(JTextComponent campo) {
        campo.setFont(fuente(Font.PLAIN, 14));
        campo.setForeground(TEXTO);
        campo.setBorder(bordeCampo(BORDE));
        // Resalta el campo que tiene el foco
        campo.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                campo.setBorder(bordeCampo(PRIMARIO));
            }

            @Override
            public void focusLost(FocusEvent e) {
                campo.setBorder(bordeCampo(BORDE));
            }
        });
    }

    private static Border bordeCampo(Color color) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 1, true),
                BorderFactory.createEmptyBorder(7, 9, 7, 9));
    }

    private static String elegirFamilia() {
        String[] disponibles = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String preferida : new String[] {"Segoe UI", "Helvetica Neue", "Arial"}) {
            if (Arrays.asList(disponibles).contains(preferida)) {
                return preferida;
            }
        }
        return Font.SANS_SERIF;
    }
}
