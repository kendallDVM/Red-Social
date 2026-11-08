package socialuna.gui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowListener;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
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
 * <p>La verificación BCrypt (unos 200 ms) se ejecuta en un {@link SwingWorker} para no
 * congelar la interfaz. Mientras tanto, los controles se deshabilitan para impedir
 * envíos repetidos.
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
    private final JButton botonIngresar = Estilos.botonPrimario("Ingresar");
    private final JButton botonCrearCuenta = Estilos.botonSecundario("Crear cuenta");

    /** {@code true} mientras se verifica un inicio de sesión en segundo plano. */
    private boolean procesando;

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

        botonIngresar.addActionListener(e -> ingresar());
        c.insets = new Insets(0, 0, 18, 0);
        formulario.add(botonIngresar, c);

        c.insets = new Insets(0, 0, 6, 0);
        formulario.add(Estilos.textoSecundario("¿Todavía no tiene cuenta?"), c);
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

    /**
     * Inicia sesión en segundo plano. Se ejecuta en el EDT; solo {@code doInBackground}
     * corre en otro hilo y no toca componentes gráficos.
     */
    private void ingresar() {
        if (procesando) {
            return; // evita solicitudes repetidas (doble clic o Enter repetido)
        }
        String usuario = campoUsuario.getText();
        // getPassword() devuelve una copia nueva; el campo se limpia de inmediato.
        char[] contrasena = campoContrasena.getPassword();
        campoContrasena.setText("");
        establecerProcesando(true);

        new SwingWorker<ResultadoAutenticacion, Void>() {
            @Override
            protected ResultadoAutenticacion doInBackground() {
                try {
                    return servicio.iniciarSesion(usuario, contrasena);
                } finally {
                    Arrays.fill(contrasena, '\0'); // se borra cuando BCrypt ya terminó de usarla
                }
            }

            @Override
            protected void done() { // EDT
                establecerProcesando(false);
                try {
                    mostrarResultado(get());
                } catch (ExecutionException e) {
                    JOptionPane.showMessageDialog(VentanaLogin.this,
                            "Ocurrió un error inesperado al iniciar sesión. Intente de nuevo.",
                            "Inicio de sesión", JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    /** Muestra el resultado del inicio de sesión (EDT). */
    private void mostrarResultado(ResultadoAutenticacion resultado) {
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

    /** Deshabilita los controles y muestra el indicador de espera mientras se procesa. */
    private void establecerProcesando(boolean valor) {
        procesando = valor;
        campoUsuario.setEnabled(!valor);
        campoContrasena.setEnabled(!valor);
        botonIngresar.setEnabled(!valor);
        botonCrearCuenta.setEnabled(!valor);
        botonIngresar.setText(valor ? "Verificando..." : "Ingresar");
        setCursor(valor ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
    }

    private void abrirRegistro() {
        if (procesando) {
            return;
        }
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
