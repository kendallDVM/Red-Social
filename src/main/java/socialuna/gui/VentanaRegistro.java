package socialuna.gui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.text.JTextComponent;

import socialuna.servicios.ResultadoAutenticacion;
import socialuna.servicios.ServicioAutenticacion;

/**
 * Ventana de registro de cuentas (Módulo 1).
 *
 * <p>Campos: ID, Nombre, Usuario, Contraseña y Confirmar contraseña.
 * Botones: <b>Registrar</b> (también con Enter) y <b>Volver</b>.
 *
 * <p>Las validaciones las hace {@link ServicioAutenticacion#registrar}. Esta ventana
 * muestra el mensaje en una alerta y pone el foco en el campo con el problema.
 * Si el registro tiene éxito, vuelve al login con el usuario ya escrito.
 *
 * <p>Cerrar esta ventana con la X equivale a <b>Volver</b>: no termina la aplicación.
 *
 * <p>El registro (hash BCrypt y guardado en {@code usuarios.json}) se ejecuta en un
 * {@link SwingWorker}. Mientras tanto, los controles se deshabilitan para impedir
 * registros duplicados por doble clic, y la X no cierra la ventana para no interrumpir
 * la escritura.
 */
public class VentanaRegistro extends JFrame {

    private final ServicioAutenticacion servicio;
    private final VentanaLogin login;

    private final JTextField campoId = Estilos.campoTexto(24);
    private final JTextField campoNombre = Estilos.campoTexto(24);
    private final JTextField campoUsuario = Estilos.campoTexto(24);
    private final JPasswordField campoContrasena = Estilos.campoContrasena(24);
    private final JPasswordField campoConfirmacion = Estilos.campoContrasena(24);
    private final JButton botonRegistrar = Estilos.botonPrimario("Registrar");
    private final JButton botonVolver = Estilos.botonSecundario("Volver");

    /** {@code true} mientras se registra una cuenta en segundo plano. */
    private boolean procesando;

    public VentanaRegistro(ServicioAutenticacion servicio, VentanaLogin login) {
        super("Social UNA - Crear cuenta");
        this.servicio = Objects.requireNonNull(servicio);
        this.login = Objects.requireNonNull(login);

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!procesando) { // no interrumpir un registro que se está guardando
                    volver(null);
                }
            }
        });
        construirInterfaz();
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private void construirInterfaz() {
        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Estilos.FONDO);
        raiz.add(Estilos.crearEncabezado("Social UNA", "Cree su cuenta de estudiante"), BorderLayout.NORTH);

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBackground(Estilos.TARJETA);
        formulario.setBorder(BorderFactory.createEmptyBorder(24, 32, 26, 32));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        c.insets = new Insets(0, 0, 4, 0);
        formulario.add(Estilos.titulo("Crear cuenta"), c);
        c.insets = new Insets(0, 0, 18, 0);
        formulario.add(Estilos.textoSecundario("Todos los campos son obligatorios."), c);

        campoId.setToolTipText("Su identificador de estudiante (por ejemplo, cédula o carné). Debe ser único.");
        campoUsuario.setToolTipText("Sin espacios. No distingue mayúsculas: \"Ana\" y \"ana\" son el mismo usuario.");

        agregarCampo(formulario, c, "ID", 'I', campoId, "Cédula o carné; no puede repetirse.");
        agregarCampo(formulario, c, "Nombre completo", 'N', campoNombre, null);
        agregarCampo(formulario, c, "Usuario", 'U', campoUsuario, "Sin espacios; no distingue mayúsculas.");
        agregarCampo(formulario, c, "Contraseña", 'C', campoContrasena, null);
        agregarCampo(formulario, c, "Confirmar contraseña", 'f', campoConfirmacion, null);

        botonRegistrar.addActionListener(e -> registrar());
        botonVolver.addActionListener(e -> volver(null));

        JPanel botones = new JPanel(new GridLayout(1, 2, 12, 0));
        botones.setOpaque(false);
        botones.add(botonVolver);
        botones.add(botonRegistrar);
        c.insets = new Insets(10, 0, 0, 0);
        formulario.add(botones, c);

        raiz.add(formulario, BorderLayout.CENTER);
        setContentPane(raiz);
        getRootPane().setDefaultButton(botonRegistrar); // Enter = Registrar
    }

    private void agregarCampo(JPanel formulario, GridBagConstraints c, String texto, char tecla,
                              JComponent campo, String ayuda) {
        c.insets = new Insets(0, 0, 6, 0);
        formulario.add(Estilos.etiqueta(texto, campo, tecla), c);
        c.insets = new Insets(0, 0, ayuda == null ? 14 : 4, 0);
        formulario.add(campo, c);
        if (ayuda != null) {
            c.insets = new Insets(0, 0, 14, 0);
            formulario.add(Estilos.textoSecundario(ayuda), c);
        }
    }

    /**
     * Registra la cuenta en segundo plano. Se ejecuta en el EDT; solo {@code doInBackground}
     * corre en otro hilo y no toca componentes gráficos.
     */
    private void registrar() {
        if (procesando) {
            return; // evita registros duplicados por doble clic
        }
        String id = campoId.getText();
        String nombre = campoNombre.getText();
        String usuario = campoUsuario.getText();
        // getPassword() devuelve copias nuevas; se borran cuando el servicio termina.
        char[] contrasena = campoContrasena.getPassword();
        char[] confirmacion = campoConfirmacion.getPassword();
        establecerProcesando(true);

        new SwingWorker<ResultadoAutenticacion, Void>() {
            @Override
            protected ResultadoAutenticacion doInBackground() {
                try {
                    return servicio.registrar(id, nombre, usuario, contrasena, confirmacion);
                } finally {
                    Arrays.fill(contrasena, '\0'); // BCrypt ya terminó de usarlas
                    Arrays.fill(confirmacion, '\0');
                }
            }

            @Override
            protected void done() { // EDT
                establecerProcesando(false);
                try {
                    mostrarResultado(get());
                } catch (ExecutionException e) {
                    JOptionPane.showMessageDialog(VentanaRegistro.this,
                            "Ocurrió un error inesperado al registrar la cuenta. Intente de nuevo.",
                            "No se pudo registrar", JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    /** Deshabilita los controles y muestra el indicador de espera mientras se procesa. */
    private void establecerProcesando(boolean valor) {
        procesando = valor;
        for (JComponent campo : new JComponent[] {
                campoId, campoNombre, campoUsuario, campoContrasena, campoConfirmacion, botonRegistrar, botonVolver}) {
            campo.setEnabled(!valor);
        }
        botonRegistrar.setText(valor ? "Registrando..." : "Registrar");
        setCursor(valor ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
    }

    /** Muestra el resultado del registro (EDT). */
    private void mostrarResultado(ResultadoAutenticacion resultado) {
        if (resultado.esExitoso()) {
            JOptionPane.showMessageDialog(this, resultado.getMensaje(), "Registro exitoso",
                    JOptionPane.INFORMATION_MESSAGE);
            volver(resultado.getUsuario().getUsuario());
            return;
        }

        JOptionPane.showMessageDialog(this, resultado.getMensaje(), "No se pudo registrar",
                resultado.getTipo() == ResultadoAutenticacion.Tipo.ERROR_PERSISTENCIA
                        ? JOptionPane.ERROR_MESSAGE : JOptionPane.WARNING_MESSAGE);
        enfocarCampoConError(resultado.getTipo());
    }

    /** Pone el foco en el campo relacionado con el error, para corregirlo rápido. */
    private void enfocarCampoConError(ResultadoAutenticacion.Tipo tipo) {
        switch (tipo) {
            case CAMPOS_VACIOS:
                for (JTextComponent campo : new JTextComponent[] {
                        campoId, campoNombre, campoUsuario, campoContrasena, campoConfirmacion}) {
                    if (estaVacio(campo)) {
                        campo.requestFocusInWindow();
                        break;
                    }
                }
                break;
            case ID_DUPLICADO:
                seleccionar(campoId);
                break;
            case USUARIO_INVALIDO:
            case USUARIO_DUPLICADO:
                seleccionar(campoUsuario);
                break;
            case CONTRASENAS_NO_COINCIDEN:
            case CONTRASENA_INVALIDA:
                campoContrasena.setText("");
                campoConfirmacion.setText("");
                campoContrasena.requestFocusInWindow();
                break;
            default:
                break;
        }
    }

    private static boolean estaVacio(JTextComponent campo) {
        if (campo instanceof JPasswordField) {
            char[] valor = ((JPasswordField) campo).getPassword();
            boolean vacio = true;
            for (char ch : valor) {
                if (!Character.isWhitespace(ch)) {
                    vacio = false;
                    break;
                }
            }
            Arrays.fill(valor, '\0');
            return vacio;
        }
        return campo.getText().isBlank();
    }

    private static void seleccionar(JTextField campo) {
        campo.requestFocusInWindow();
        campo.selectAll();
    }

    /** Cierra el registro y vuelve al login (opcionalmente con un usuario ya escrito). */
    private void volver(String usuarioSugerido) {
        campoContrasena.setText("");
        campoConfirmacion.setText("");
        dispose();
        login.mostrar(usuarioSugerido);
    }
}
