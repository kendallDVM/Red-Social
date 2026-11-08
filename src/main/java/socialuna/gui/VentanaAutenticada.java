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
import javax.swing.WindowConstants;

import socialuna.modelo.Usuario;

/**
 * Pantalla <b>temporal</b> del Módulo 1 que se muestra después de iniciar sesión.
 *
 * <p>Solo sirve para comprobar la autenticación mientras no exista la ventana principal
 * de la aplicación (responsabilidad de la integración del Módulo 3): muestra quién inició
 * sesión y permite cerrar la sesión. <b>No</b> es la ventana social definitiva y no
 * contiene funciones de otros módulos.
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
    private final JButton botonCerrarSesion = Estilos.botonPrimario("Cerrar sesión");

    /**
     * @param usuario      usuario que inició sesión
     * @param cerrarSesion acción recibida de {@link NavegacionPrincipal}
     * @param cierre       listener que guarda y termina la aplicación al pulsar la X
     */
    public VentanaAutenticada(Usuario usuario, Runnable cerrarSesion, WindowListener cierre) {
        super("Social UNA - Sesión iniciada");
        Objects.requireNonNull(usuario, "El usuario no puede ser nulo.");
        this.cerrarSesion = Objects.requireNonNull(cerrarSesion, "La acción de cerrar sesión no puede ser nula.");
        Objects.requireNonNull(cierre, "El listener de cierre no puede ser nulo.");

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(cierre); // X = salir de la aplicación guardando los datos
        construirInterfaz(usuario);
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private void construirInterfaz(Usuario usuario) {
        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Estilos.FONDO);
        raiz.add(Estilos.crearEncabezado("Social UNA", "Red social estudiantil de la Universidad Nacional"),
                BorderLayout.NORTH);

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
                "Pantalla temporal de autenticación: la ventana principal se integrará aquí."), c);

        botonCerrarSesion.addActionListener(e -> cerrarSesion());
        c.insets = new Insets(0, 0, 0, 0);
        contenido.add(botonCerrarSesion, c);

        raiz.add(contenido, BorderLayout.CENTER);
        setContentPane(raiz);
        getRootPane().setDefaultButton(botonCerrarSesion);
    }

    /** Cierra esta ventana y delega el cierre de sesión (guardar, limpiar, volver al login). */
    private void cerrarSesion() {
        botonCerrarSesion.setEnabled(false); // evita un segundo clic
        dispose();
        cerrarSesion.run();
    }
}
