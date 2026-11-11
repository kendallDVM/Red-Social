package socialuna.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import socialuna.datos.ListaDinamica;
import socialuna.modelo.Usuario;
import socialuna.servicios.EstadoAmistad;
import socialuna.servicios.ResultadoAmistad;
import socialuna.servicios.ServicioAmistades;
import socialuna.servicios.Sesion;
import socialuna.servicios.UsuarioEncontrado;

/**
 * Pestaña "Buscar usuarios" del Módulo 2.
 *
 * <p>Permite buscar estudiantes registrados por nombre o nombre de usuario. Cada
 * resultado muestra su relación con el usuario activo (columna "Estado"). Justo debajo
 * del buscador hay una <b>barra de acción</b> con el usuario seleccionado, su estado y el
 * botón que corresponde. Está arriba, y no al pie del panel, para que siga visible aunque
 * la ventana sea más baja que la pantalla: si falta espacio, lo que se reduce es la tabla.
 *
 * <table>
 *   <caption>Acción según el estado</caption>
 *   <tr><td>No son amigos</td><td>Enviar solicitud</td></tr>
 *   <tr><td>Solicitud enviada (pendiente)</td><td>Cancelar solicitud</td></tr>
 *   <tr><td>Te envió una solicitud</td><td>Aceptar / Rechazar</td></tr>
 *   <tr><td>Ya son amigos / Es tu cuenta</td><td>ninguna (botón deshabilitado)</td></tr>
 * </table>
 *
 * <p>Toda la lógica está en {@link ServicioAmistades}; este panel solo recoge los datos y
 * muestra los resultados y las alertas.
 */
public class PanelBuscarUsuarios extends JPanel {

    private static final String TITULO_ALERTA = "Buscar usuarios";
    private static final String[] COLUMNAS = {"Nombre", "Usuario", "ID", "Estado"};
    private static final int COLUMNA_ID = 2;

    private final ServicioAmistades servicio;
    private final Runnable alCambiarAmigos;
    /** Abre el perfil de un usuario (por ID); {@code null} si esta ventana no ofrece perfiles. */
    private final Consumer<String> abrirPerfil;

    private final JTextField campoBusqueda = Estilos.campoTexto(24);
    private final JButton botonBuscar = Estilos.botonPrimario("Buscar");
    /** Acción principal; su texto cambia según el estado del usuario seleccionado. */
    private final JButton botonAccion = Estilos.botonPrimario("Enviar solicitud");
    /** Abre el perfil del usuario seleccionado; no cambia la relación de amistad. */
    private final JButton botonVerPerfil = Estilos.botonSecundario("Ver perfil");
    /** Solo se muestra cuando el usuario seleccionado le envió una solicitud al usuario activo. */
    private final JButton botonRechazar = Estilos.botonSecundario("Rechazar");
    private final JLabel lblResumen = Estilos.textoSecundario(" ");
    /** Nombre del usuario seleccionado, en la barra de acción. */
    private final JLabel lblSeleccionado = new JLabel(" ");
    /** Explicación del estado del usuario seleccionado, en la barra de acción. */
    private final JLabel lblEstadoSeleccionado = Estilos.textoSecundario(" ");

    /** Modelo de la tabla: las celdas no se pueden editar. */
    private final DefaultTableModel modelo = new DefaultTableModel(COLUMNAS, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final JTable tabla = new JTable(modelo);

    /** Resultados mostrados, en el mismo orden que las filas de la tabla. */
    private ListaDinamica<UsuarioEncontrado> resultados = new ListaDinamica<>();

    /** Último texto buscado, para refrescar la tabla después de un cambio. */
    private String ultimaBusqueda = "";

    /**
     * @param servicio        lógica de amistades
     * @param alCambiarAmigos se ejecuta después de cambiar una solicitud o amistad (por
     *                        ejemplo, para refrescar las otras pestañas); puede ser {@code null}
     */
    public PanelBuscarUsuarios(ServicioAmistades servicio, Runnable alCambiarAmigos) {
        this(servicio, alCambiarAmigos, null);
    }

    /**
     * @param abrirPerfil recibe el ID del usuario seleccionado al pulsar «Ver perfil»;
     *                    si es {@code null}, el botón no se muestra
     */
    public PanelBuscarUsuarios(ServicioAmistades servicio, Runnable alCambiarAmigos, Consumer<String> abrirPerfil) {
        this.servicio = Objects.requireNonNull(servicio, "El servicio no puede ser nulo.");
        this.alCambiarAmigos = alCambiarAmigos != null ? alCambiarAmigos : () -> { };
        this.abrirPerfil = abrirPerfil;
        construirInterfaz();
        actualizarBotones();
    }

    private void construirInterfaz() {
        setLayout(new BorderLayout(0, 12));
        setBackground(Estilos.TARJETA);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // ---- parte superior: título y buscador
        JPanel superior = new JPanel(new BorderLayout(0, 10));
        superior.setOpaque(false);
        JPanel titulos = new JPanel(new BorderLayout(0, 2));
        titulos.setOpaque(false);
        titulos.add(Estilos.titulo("Buscar usuarios"), BorderLayout.NORTH);
        titulos.add(Estilos.textoSecundario(
                "Escribe parte del nombre o del usuario. Selecciona un resultado para enviarle una solicitud."),
                BorderLayout.SOUTH);
        superior.add(titulos, BorderLayout.NORTH);

        JPanel buscador = new JPanel(new BorderLayout(8, 0));
        buscador.setOpaque(false);
        campoBusqueda.setName("campoBusqueda");
        buscador.add(campoBusqueda, BorderLayout.CENTER);
        buscador.add(botonBuscar, BorderLayout.EAST);
        superior.add(buscador, BorderLayout.CENTER);
        superior.add(construirBarraAccion(), BorderLayout.SOUTH);
        add(superior, BorderLayout.NORTH);

        // ---- centro: tabla de resultados
        tabla.setName("tablaResultados");
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setRowHeight(28);
        tabla.setFont(Estilos.fuente(Font.PLAIN, 14));
        tabla.getTableHeader().setFont(Estilos.fuente(Font.BOLD, 13));
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.setFillsViewportHeight(true);
        JScrollPane desplazamiento = new JScrollPane(tabla);
        desplazamiento.setBorder(BorderFactory.createLineBorder(Estilos.BORDE));
        add(desplazamiento, BorderLayout.CENTER);

        // ---- parte inferior: cantidad de resultados
        lblResumen.setName("resumenBusqueda");
        add(lblResumen, BorderLayout.SOUTH);

        // ---- acciones
        botonBuscar.addActionListener(e -> buscar());
        campoBusqueda.addActionListener(e -> buscar()); // Enter en el cuadro de texto
        botonAccion.addActionListener(e -> ejecutarAccionPrincipal());
        botonRechazar.addActionListener(e -> rechazarSeleccionado());
        botonVerPerfil.addActionListener(e -> {
            UsuarioEncontrado sel = seleccionado();
            if (sel != null && abrirPerfil != null) {
                abrirPerfil.accept(sel.getUsuario().getId()); // solo consulta: no cambia la relación
            }
        });
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                actualizarBotones();
            }
        });
    }

    /**
     * Barra con el usuario seleccionado, su estado y los botones de acción
     * ("Enviar solicitud", "Cancelar solicitud", "Aceptar solicitud" + "Rechazar").
     */
    private JPanel construirBarraAccion() {
        JPanel barra = new JPanel(new BorderLayout(12, 0));
        barra.setName("barraAccion");
        barra.setBackground(Estilos.FONDO);
        barra.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilos.BORDE, 1, true),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));

        JPanel textos = new JPanel(new GridLayout(2, 1, 0, 2));
        textos.setOpaque(false);
        lblSeleccionado.setName("lblSeleccionado");
        lblSeleccionado.setFont(Estilos.fuente(Font.BOLD, 14));
        lblSeleccionado.setForeground(Estilos.TEXTO);
        lblEstadoSeleccionado.setName("lblEstadoSeleccionado");
        textos.add(lblSeleccionado);
        textos.add(lblEstadoSeleccionado);
        barra.add(textos, BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        botones.setOpaque(false);
        botonRechazar.setName("botonRechazar");
        botonAccion.setName("botonAccion");
        botonVerPerfil.setName("botonVerPerfil");
        botonVerPerfil.setVisible(abrirPerfil != null);
        botones.add(botonVerPerfil);
        botones.add(Box.createHorizontalStrut(8));
        botones.add(botonRechazar);
        botones.add(Box.createHorizontalStrut(8));
        botones.add(botonAccion);
        barra.add(botones, BorderLayout.EAST);
        return barra;
    }

    // ------------------------------------------------------------------ acciones

    private void buscar() {
        String texto = campoBusqueda.getText();
        if (texto == null || texto.isBlank()) {
            JOptionPane.showMessageDialog(this, "Escriba un nombre o usuario para buscar.",
                    TITULO_ALERTA, JOptionPane.WARNING_MESSAGE);
            campoBusqueda.requestFocusInWindow();
            return;
        }
        ultimaBusqueda = texto;
        int encontrados = mostrarResultados(texto, null);
        if (encontrados == 0) {
            JOptionPane.showMessageDialog(this, "No se encontraron usuarios que coincidan con \""
                    + texto.strip() + "\".", TITULO_ALERTA, JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /** Ejecuta la acción que corresponde al estado del usuario seleccionado. */
    private void ejecutarAccionPrincipal() {
        UsuarioEncontrado seleccionado = seleccionado();
        if (seleccionado == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un usuario de la lista.",
                    TITULO_ALERTA, JOptionPane.WARNING_MESSAGE);
            return;
        }
        String idActivo = idActivo();
        String idOtro = seleccionado.getUsuario().getId();
        switch (seleccionado.getEstado()) {
            case SIN_RELACION:
                mostrarResultado(servicio.enviarSolicitud(idActivo, idOtro), "Solicitud enviada", idOtro);
                break;
            case SOLICITUD_ENVIADA:
                if (confirmar("¿Desea cancelar la solicitud enviada a " + seleccionado.getUsuario().getNombre() + "?",
                        "Cancelar solicitud")) {
                    mostrarResultado(servicio.cancelarSolicitud(idActivo, idOtro), "Solicitud cancelada", idOtro);
                }
                break;
            case SOLICITUD_RECIBIDA:
                mostrarResultado(servicio.aceptarSolicitud(idActivo, idOtro), "Solicitud aceptada", idOtro);
                break;
            default:
                // AMIGOS, PROPIA_CUENTA o USUARIO_NO_EXISTE: el botón está deshabilitado.
                break;
        }
    }

    private void rechazarSeleccionado() {
        UsuarioEncontrado seleccionado = seleccionado();
        if (seleccionado == null || seleccionado.getEstado() != EstadoAmistad.SOLICITUD_RECIBIDA) {
            return;
        }
        if (confirmar("¿Desea rechazar la solicitud de " + seleccionado.getUsuario().getNombre() + "?",
                "Rechazar solicitud")) {
            String idOtro = seleccionado.getUsuario().getId();
            mostrarResultado(servicio.rechazarSolicitud(idActivo(), idOtro), "Solicitud rechazada", idOtro);
        }
    }

    /**
     * Muestra el mensaje del resultado y vuelve a buscar, para que la columna "Estado"
     * refleje los datos actuales (aunque la operación haya fallado porque otra instancia
     * del programa cambió algo).
     */
    private void mostrarResultado(ResultadoAmistad resultado, String tituloExito, String idSeleccionar) {
        mostrarResultados(ultimaBusqueda, idSeleccionar);
        if (resultado.esExitoso()) {
            alCambiarAmigos.run();
            JOptionPane.showMessageDialog(this, resultado.getMensaje(), tituloExito,
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int icono = resultado.getTipo() == ResultadoAmistad.Tipo.ERROR_PERSISTENCIA
                ? JOptionPane.ERROR_MESSAGE : JOptionPane.WARNING_MESSAGE;
        JOptionPane.showMessageDialog(this, resultado.getMensaje(), TITULO_ALERTA, icono);
    }

    /**
     * Vuelve a ejecutar la última búsqueda (si hubo una) para actualizar la columna
     * "Estado", conservando la fila seleccionada. Lo llaman las otras pestañas y la
     * ventana principal al cambiar de pestaña.
     */
    public void refrescar() {
        if (!ultimaBusqueda.isBlank()) {
            UsuarioEncontrado actual = seleccionado();
            mostrarResultados(ultimaBusqueda, actual == null ? null : actual.getUsuario().getId());
        }
    }

    /**
     * Llena la tabla con los resultados de la búsqueda y devuelve cuántos hay.
     *
     * @param idSeleccionar usuario que debe quedar seleccionado; si es {@code null} o ya no
     *                      aparece, se selecciona la primera fila
     */
    private int mostrarResultados(String texto, String idSeleccionar) {
        resultados = servicio.buscarConEstado(idActivo(), texto);
        modelo.setRowCount(0);
        int filaASeleccionar = 0;
        for (int i = 0; i < resultados.tamano(); i++) {
            UsuarioEncontrado r = resultados.obtener(i);
            Usuario usuario = r.getUsuario();
            modelo.addRow(new Object[] {usuario.getNombre(), "@" + usuario.getUsuario(), usuario.getId(),
                    r.getEstado().getDescripcion()});
            if (usuario.getId().equals(idSeleccionar)) {
                filaASeleccionar = i;
            }
        }
        int cantidad = resultados.tamano();
        if (cantidad > 0) {
            tabla.setRowSelectionInterval(filaASeleccionar, filaASeleccionar);
        }
        lblResumen.setText(cantidad == 0 ? "No se encontraron usuarios"
                : cantidad == 1 ? "1 usuario encontrado" : cantidad + " usuarios encontrados");
        actualizarBotones();
        return cantidad;
    }

    /** Ajusta texto, habilitación y visibilidad de los botones según la fila seleccionada. */
    private void actualizarBotones() {
        UsuarioEncontrado sel = seleccionado();
        botonRechazar.setVisible(false);
        botonVerPerfil.setEnabled(sel != null);
        if (sel == null) {
            lblSeleccionado.setText(resultados.estaVacia()
                    ? "Busca un usuario para ver las acciones disponibles."
                    : "Selecciona un usuario de la lista.");
            lblEstadoSeleccionado.setText(" ");
            configurarAccion("Enviar solicitud", false, "Busque y seleccione un usuario.");
            return;
        }
        Usuario usuario = sel.getUsuario();
        lblSeleccionado.setText(usuario.getNombre() + "  (@" + usuario.getUsuario() + ")");
        switch (sel.getEstado()) {
            case SIN_RELACION:
                lblEstadoSeleccionado.setText("No son amigos. Envíale una solicitud de amistad.");
                configurarAccion("Enviar solicitud", true, "Enviar una solicitud de amistad.");
                break;
            case SOLICITUD_ENVIADA:
                lblEstadoSeleccionado.setText("Solicitud enviada. Esperando a que la acepte o la rechace.");
                configurarAccion("Cancelar solicitud", true, "Su solicitud sigue pendiente.");
                break;
            case SOLICITUD_RECIBIDA:
                lblEstadoSeleccionado.setText("Te envió una solicitud de amistad.");
                configurarAccion("Aceptar", true, "Aceptar la solicitud: serán amigos.");
                botonRechazar.setVisible(true);
                break;
            case AMIGOS:
                lblEstadoSeleccionado.setText("Ya son amigos.");
                configurarAccion("Ya son amigos", false, "Ya son amigos.");
                break;
            case PROPIA_CUENTA:
                lblEstadoSeleccionado.setText("Es tu cuenta. No puedes enviarte una solicitud.");
                configurarAccion("Es tu cuenta", false, "No puede enviarse una solicitud a sí mismo.");
                break;
            default:
                lblEstadoSeleccionado.setText("El usuario ya no está registrado.");
                configurarAccion("Enviar solicitud", false, "El usuario ya no está registrado.");
                break;
        }
    }

    /**
     * Ajusta el botón principal. Deshabilitado se pinta en gris (estilo secundario) para
     * que no parezca una acción disponible; habilitado, con el color principal.
     */
    private void configurarAccion(String texto, boolean habilitado, String ayuda) {
        botonAccion.setText(texto);
        botonAccion.setEnabled(habilitado);
        botonAccion.setToolTipText(ayuda);
        botonAccion.setBackground(habilitado ? Estilos.PRIMARIO : Estilos.SECUNDARIO);
        botonAccion.setForeground(habilitado ? java.awt.Color.WHITE : Estilos.TEXTO_SECUNDARIO);
    }

    /** Resultado de la fila seleccionada, o {@code null} si no hay selección. */
    private UsuarioEncontrado seleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= resultados.tamano()) {
            return null;
        }
        UsuarioEncontrado r = resultados.obtener(fila);
        // Comprobación defensiva: la fila y la lista deben corresponder al mismo usuario.
        return r.getUsuario().getId().equals(modelo.getValueAt(fila, COLUMNA_ID)) ? r : null;
    }

    private boolean confirmar(String mensaje, String titulo) {
        return JOptionPane.showConfirmDialog(this, mensaje, titulo, JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    private static String idActivo() {
        Usuario activo = Sesion.getUsuarioActivo();
        return activo == null ? null : activo.getId();
    }
}
