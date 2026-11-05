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

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
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
 */
public class VentanaRegistro extends JFrame {

    private final ServicioAutenticacion servicio;
    private final VentanaLogin login;

    private final JTextField campoId = Estilos.campoTexto(24);
    private final JTextField campoNombre = Estilos.campoTexto(24);
    private final JTextField campoUsuario = Estilos.campoTexto(24);
    private final JPasswordField campoContrasena = Estilos.campoContrasena(24);
    private final JPasswordField campoConfirmacion = Estilos.campoContrasena(24);

    public VentanaRegistro(ServicioAutenticacion servicio, VentanaLogin login) {
        super("Social UNA - Crear cuenta");
        this.servicio = Objects.requireNonNull(servicio);
        this.login = Objects.requireNonNull(login);

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                volver(null);
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

        JButton botonRegistrar = Estilos.botonPrimario("Registrar");
        botonRegistrar.addActionListener(e -> registrar());
        JButton botonVolver = Estilos.botonSecundario("Volver");
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

    private void registrar() {
        char[] contrasena = campoContrasena.getPassword();
        char[] confirmacion = campoConfirmacion.getPassword();
        ResultadoAutenticacion resultado;
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            resultado = servicio.registrar(campoId.getText(), campoNombre.getText(), campoUsuario.getText(),
                    contrasena, confirmacion);
        } finally {
            Arrays.fill(contrasena, '\0'); // no dejar las contraseñas en memoria
            Arrays.fill(confirmacion, '\0');
            setCursor(Cursor.getDefaultCursor());
        }

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
