package socialuna.gui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowListener;
import java.util.Arrays;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import socialuna.servicios.ResultadoAutenticacion;
import socialuna.servicios.ServicioAutenticacion;

/**
 * Ventana de inicio de sesión (Módulo 1).
 *
 * <p>Campos: usuario y contraseña oculta. Botones: <b>Ingresar</b> (también con Enter)
 * y <b>Crear cuenta</b>. Toda la validación la hace {@link ServicioAutenticacion}; esta
 * ventana solo muestra el resultado.
 *
 * <p>Al iniciar sesión, la ventana se oculta y llama a {@link NavegacionPrincipal}.
 * Cerrar esta ventana con la X termina la aplicación guardando los datos
 * ({@link CierreAplicacion}).
 */
public class VentanaLogin extends JFrame {

    private final ServicioAutenticacion servicio;
    private final NavegacionPrincipal navegacion;
    private final WindowListener cierre;

    private final JTextField campoUsuario = Estilos.campoTexto(22);
    private final JPasswordField campoContrasena = Estilos.campoContrasena(22);

    public VentanaLogin(ServicioAutenticacion servicio, NavegacionPrincipal navegacion, WindowListener cierre) {
        super("Social UNA - Iniciar sesión");
        this.servicio = Objects.requireNonNull(servicio);
        this.navegacion = Objects.requireNonNull(navegacion);
        this.cierre = Objects.requireNonNull(cierre);

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(cierre);
        construirInterfaz();
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private void construirInterfaz() {
        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Estilos.FONDO);
        raiz.add(Estilos.crearEncabezado("Social UNA", "Red social estudiantil de la Universidad Nacional"),
                BorderLayout.NORTH);

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBackground(Estilos.TARJETA);
        formulario.setBorder(BorderFactory.createEmptyBorder(26, 32, 28, 32));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        c.insets = new Insets(0, 0, 4, 0);
        formulario.add(Estilos.titulo("Iniciar sesión"), c);
        c.insets = new Insets(0, 0, 20, 0);
        formulario.add(Estilos.textoSecundario("Ingrese con su nombre de usuario y contraseña."), c);

        c.insets = new Insets(0, 0, 6, 0);
        formulario.add(Estilos.etiqueta("Usuario", campoUsuario, 'U'), c);
        c.insets = new Insets(0, 0, 14, 0);
        campoUsuario.setToolTipText("No distingue mayúsculas de minúsculas");
        formulario.add(campoUsuario, c);

        c.insets = new Insets(0, 0, 6, 0);
        formulario.add(Estilos.etiqueta("Contraseña", campoContrasena, 'C'), c);
        c.insets = new Insets(0, 0, 22, 0);
        formulario.add(campoContrasena, c);

        JButton botonIngresar = Estilos.botonPrimario("Ingresar");
        botonIngresar.addActionListener(e -> ingresar());
        c.insets = new Insets(0, 0, 18, 0);
        formulario.add(botonIngresar, c);

        c.insets = new Insets(0, 0, 6, 0);
        formulario.add(Estilos.textoSecundario("¿Todavía no tiene cuenta?"), c);
        JButton botonCrearCuenta = Estilos.botonSecundario("Crear cuenta");
        botonCrearCuenta.addActionListener(e -> abrirRegistro());
        c.insets = new Insets(0, 0, 0, 0);
        formulario.add(botonCrearCuenta, c);

        raiz.add(formulario, BorderLayout.CENTER);
        setContentPane(raiz);
        getRootPane().setDefaultButton(botonIngresar); // Enter = Ingresar
    }

    /**
     * Muestra (o vuelve a mostrar) la ventana con la contraseña vacía.
     *
     * @param usuarioSugerido nombre de usuario para precargar (por ejemplo, tras registrarse), o {@code null}
     */
    public void mostrar(String usuarioSugerido) {
        campoContrasena.setText("");
        campoUsuario.setText(usuarioSugerido == null ? "" : usuarioSugerido);
        setVisible(true);
        toFront();
        if (usuarioSugerido == null || usuarioSugerido.isEmpty()) {
            campoUsuario.requestFocusInWindow();
        } else {
            campoContrasena.requestFocusInWindow();
        }
    }

    private void ingresar() {
        char[] contrasena = campoContrasena.getPassword();
        ResultadoAutenticacion resultado;
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            resultado = servicio.iniciarSesion(campoUsuario.getText(), contrasena);
        } finally {
            Arrays.fill(contrasena, '\0'); // no dejar la contraseña en memoria
            setCursor(Cursor.getDefaultCursor());
        }
        campoContrasena.setText("");

        if (!resultado.esExitoso()) {
            boolean faltanDatos = resultado.getTipo() == ResultadoAutenticacion.Tipo.CAMPOS_VACIOS;
            JOptionPane.showMessageDialog(this, resultado.getMensaje(), "Inicio de sesión",
                    faltanDatos ? JOptionPane.WARNING_MESSAGE : JOptionPane.ERROR_MESSAGE);
            if (campoUsuario.getText().isBlank()) {
                campoUsuario.requestFocusInWindow();
            } else {
                campoContrasena.requestFocusInWindow();
            }
            return;
        }

        setVisible(false);
        navegacion.abrir(resultado.getUsuario(), this::cerrarSesionYVolver);
    }

    private void abrirRegistro() {
        campoContrasena.setText("");
        setVisible(false);
        new VentanaRegistro(servicio, this).setVisible(true);
    }

    /**
     * Guarda los datos, cierra la sesión y vuelve a mostrar el login.
     * Es la acción que recibe la ventana principal como {@code cerrarSesion}.
     */
    public void cerrarSesionYVolver() {
        ResultadoAutenticacion resultado = servicio.cerrarSesion();
        if (!resultado.esExitoso()) {
            JOptionPane.showMessageDialog(this, resultado.getMensaje(), "Cerrar sesión",
                    JOptionPane.WARNING_MESSAGE);
        }
        mostrar(null);
    }

    /** Listener de cierre de la aplicación, para que otras ventanas lo reutilicen. */
    public WindowListener getCierre() {
        return cierre;
    }
}
