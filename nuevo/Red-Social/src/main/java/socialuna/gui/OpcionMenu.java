package socialuna.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;

import javax.swing.JMenuItem;
import javax.swing.plaf.basic.BasicMenuItemUI;

/**
 * Opción del menú ☰ con el estilo de Social UNA: icono de usuario en un círculo, título en
 * negrita y un texto secundario, alineados a la izquierda y con espacio interno.
 *
 * <p>Es solo <b>presentación</b>: sigue siendo un {@link JMenuItem}, así que los
 * {@code ActionListener}, el cierre del menú al elegirla y {@code setEnabled} funcionan igual
 * que en un {@code JMenuItem} normal. Se dibuja a mano porque la apariencia de Windows reserva
 * a la izquierda una columna vacía (para marcas e iconos) que desplazaba el texto.
 */
class OpcionMenu extends JMenuItem {

    private static final int ANCHO = 280;
    private static final int ALTO = 60;
    private static final int MARGEN = 14;
    private static final int DIAMETRO_ICONO = 36;

    /** Fondo al pasar el cursor: un rosado muy suave derivado del color principal. */
    private static final Color FONDO_HOVER = new Color(0xFD, 0xEC, 0xEC);
    /** Fondo del círculo del icono en reposo. */
    private static final Color FONDO_ICONO = new Color(0xFB, 0xE2, 0xE3);

    private final String subtitulo;

    OpcionMenu(String titulo, String subtitulo) {
        super(titulo);
        this.subtitulo = subtitulo;
        setUI(new BasicMenuItemUI()); // sin la columna de iconos de la apariencia del sistema
        setOpaque(false);
        setFont(Estilos.fuente(Font.BOLD, 14));
        setRolloverEnabled(true);
        getAccessibleContext().setAccessibleDescription(subtitulo);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(ANCHO, ALTO);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        boolean activo = isEnabled();
        boolean resaltado = activo && (getModel().isArmed() || getModel().isRollover() || getModel().isSelected());

        // fondo y barra lateral de color al pasar el cursor
        g2.setColor(resaltado ? FONDO_HOVER : Estilos.TARJETA);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
        if (resaltado) {
            g2.setColor(Estilos.PRIMARIO);
            g2.fillRoundRect(0, 10, 4, getHeight() - 20, 4, 4);
        }

        // icono: silueta de persona dentro de un círculo
        int xIcono = MARGEN;
        int yIcono = (getHeight() - DIAMETRO_ICONO) / 2;
        Color colorIcono = activo ? Estilos.PRIMARIO : Estilos.TEXTO_SECUNDARIO;
        g2.setColor(resaltado ? Estilos.PRIMARIO : (activo ? FONDO_ICONO : Estilos.SECUNDARIO));
        g2.fill(new Ellipse2D.Double(xIcono, yIcono, DIAMETRO_ICONO, DIAMETRO_ICONO));
        dibujarPersona(g2, xIcono, yIcono, resaltado ? Color.WHITE : colorIcono);

        // textos
        int xTexto = xIcono + DIAMETRO_ICONO + 12;
        Font fTitulo = Estilos.fuente(Font.BOLD, 14);
        Font fSub = Estilos.fuente(Font.PLAIN, 12);
        FontMetrics mTitulo = g2.getFontMetrics(fTitulo);
        FontMetrics mSub = g2.getFontMetrics(fSub);
        int altoBloque = mTitulo.getHeight() + mSub.getHeight();
        int yTitulo = (getHeight() - altoBloque) / 2 + mTitulo.getAscent();
        g2.setFont(fTitulo);
        g2.setColor(activo ? Estilos.TEXTO : Estilos.TEXTO_SECUNDARIO);
        g2.drawString(getText(), xTexto, yTitulo);
        g2.setFont(fSub);
        g2.setColor(Estilos.TEXTO_SECUNDARIO);
        g2.drawString(activo ? subtitulo : "Inicia sesión para ver tu perfil", xTexto,
                yTitulo + mTitulo.getDescent() + mSub.getAscent());

        // indicador ">" a la derecha cuando está resaltado
        if (resaltado) {
            int xf = getWidth() - MARGEN - 6;
            int yc = getHeight() / 2;
            g2.setColor(Estilos.PRIMARIO);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(xf - 4, yc - 5, xf + 1, yc);
            g2.drawLine(xf + 1, yc, xf - 4, yc + 5);
        }
        g2.dispose();
    }

    /** Cabeza y hombros dentro del círculo del icono. */
    private static void dibujarPersona(Graphics2D g2, int x, int y, Color color) {
        g2.setColor(color);
        double d = DIAMETRO_ICONO;
        double cabeza = d * 0.30;
        g2.fill(new Ellipse2D.Double(x + (d - cabeza) / 2, y + d * 0.20, cabeza, cabeza));
        double anchoHombros = d * 0.56;
        g2.fill(new java.awt.geom.Arc2D.Double(x + (d - anchoHombros) / 2, y + d * 0.55,
                anchoHombros, d * 0.40, 0, 180, java.awt.geom.Arc2D.CHORD));
    }
}
