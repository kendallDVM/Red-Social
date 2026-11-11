package socialuna.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import socialuna.datos.ListaDinamica;
import socialuna.modelo.Usuario;
import socialuna.servicios.ResultadoAmistad;
import socialuna.servicios.ServicioAmistades;
import socialuna.servicios.Sesion;

/**
 * Pestaña "Solicitudes" del Módulo 2.
 *
 * <p>Muestra las solicitudes de amistad pendientes del usuario activo:
 * <ul>
 *   <li><b>Recibidas:</b> quién se la envió; se puede <b>aceptar</b> (se crea la amistad)
 *       o <b>rechazar</b> (no se crea nada).</li>
 *   <li><b>Enviadas:</b> a quién se la envió y siguen sin respuesta; se puede cancelar.</li>
 * </ul>
 *
 * <p>No hay actualización en tiempo real entre instancias del programa: la lista se
 * relee del disco al abrir la pestaña, al pulsar "Actualizar" y después de cada acción.
 * Toda la lógica está en {@link ServicioAmistades}.
 */
public class PanelSolicitudes extends JPanel {

    private static final String TITULO_ALERTA = "Solicitudes de amistad";
    private static final String[] COLUMNAS = {"Nombre", "Usuario", "ID"};
    private static final int COLUMNA_NOMBRE = 0;
    private static final int COLUMNA_ID = 2;

    private final ServicioAmistades servicio;
    private final Runnable alCambiar;

    private final JButton botonActualizar = Estilos.botonSecundario("Actualizar");
    private final JButton botonAceptar = Estilos.botonPrimario("Aceptar");
    private final JButton botonRechazar = Estilos.botonSecundario("Rechazar");
    private final JButton botonCancelar = Estilos.botonSecundario("Cancelar solicitud");
    private final JLabel lblRecibidas = Estilos.textoSecundario(" ");
    private final JLabel lblEnviadas = Estilos.textoSecundario(" ");

    private final DefaultTableModel modeloRecibidas = modeloNoEditable();
    private final DefaultTableModel modeloEnviadas = modeloNoEditable();
    private final JTable tablaRecibidas = new JTable(modeloRecibidas);
    private final JTable tablaEnviadas = new JTable(modeloEnviadas);

    /**
     * @param servicio  lógica de amistades
     * @param alCambiar se ejecuta después de aceptar, rechazar o cancelar una solicitud, y
     *                  después de actualizar (para refrescar las otras pestañas y el
     *                  contador de la pestaña); puede ser {@code null}
     */
    public PanelSolicitudes(ServicioAmistades servicio, Runnable alCambiar) {
        this.servicio = Objects.requireNonNull(servicio, "El servicio no puede ser nulo.");
        this.alCambiar = alCambiar != null ? alCambiar : () -> { };
        construirInterfaz();
        refrescar();
    }

    private void construirInterfaz() {
        setLayout(new BorderLayout(0, 12));
        setBackground(Estilos.TARJETA);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // ---- parte superior: título y botón actualizar
        JPanel superior = new JPanel(new BorderLayout());
        superior.setOpaque(false);
        JPanel titulos = new JPanel(new BorderLayout(0, 2));
        titulos.setOpaque(false);
        titulos.add(Estilos.titulo("Solicitudes de amistad"), BorderLayout.NORTH);
        titulos.add(Estilos.textoSecundario(
                "Acepta o rechaza las solicitudes que recibiste. Pulsa \"Actualizar\" para ver las nuevas."),
                BorderLayout.SOUTH);
        superior.add(titulos, BorderLayout.CENTER);
        JPanel lateral = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        lateral.setOpaque(false);
        botonActualizar.setName("botonActualizarSolicitudes");
        lateral.add(botonActualizar);
        superior.add(lateral, BorderLayout.EAST);
        add(superior, BorderLayout.NORTH);

        // ---- centro: recibidas arriba, enviadas abajo
        JPanel centro = new JPanel(new GridLayout(2, 1, 0, 16));
        centro.setOpaque(false);

        tablaRecibidas.setName("tablaSolicitudesRecibidas");
        botonAceptar.setName("botonAceptarSolicitud");
        botonRechazar.setName("botonRechazarSolicitud");
        lblRecibidas.setName("resumenRecibidas");
        centro.add(seccion("Recibidas", tablaRecibidas, lblRecibidas, botonRechazar, botonAceptar));

        tablaEnviadas.setName("tablaSolicitudesEnviadas");
        botonCancelar.setName("botonCancelarSolicitud");
        lblEnviadas.setName("resumenEnviadas");
        centro.add(seccion("Enviadas (pendientes de respuesta)", tablaEnviadas, lblEnviadas, botonCancelar));
        add(centro, BorderLayout.CENTER);

        // ---- acciones
        botonActualizar.addActionListener(e -> {
            refrescar();
            alCambiar.run(); // las otras pestañas también reflejan lo que cambió en disco
        });
        botonAceptar.addActionListener(e -> aceptarSeleccionada());
        botonRechazar.addActionListener(e -> rechazarSeleccionada());
        botonCancelar.addActionListener(e -> cancelarSeleccionada());
    }

    /** Arma una sección con subtítulo, tabla, resumen y botones. */
    private static JPanel seccion(String subtitulo, JTable tabla, JLabel resumen, JButton... botones) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);
        JLabel lblSubtitulo = new JLabel(subtitulo);
        lblSubtitulo.setFont(Estilos.fuente(Font.BOLD, 15));
        lblSubtitulo.setForeground(Estilos.TEXTO);
        panel.add(lblSubtitulo, BorderLayout.NORTH);

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setRowHeight(28);
        tabla.setFont(Estilos.fuente(Font.PLAIN, 14));
        tabla.getTableHeader().setFont(Estilos.fuente(Font.BOLD, 13));
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.setFillsViewportHeight(true);
        JScrollPane desplazamiento = new JScrollPane(tabla);
        desplazamiento.setBorder(BorderFactory.createLineBorder(Estilos.BORDE));
        panel.add(desplazamiento, BorderLayout.CENTER);

        JPanel inferior = new JPanel(new BorderLayout());
        inferior.setOpaque(false);
        inferior.add(resumen, BorderLayout.WEST);
        JPanel filaBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        filaBotones.setOpaque(false);
        for (int i = 0; i < botones.length; i++) {
            if (i > 0) {
                filaBotones.add(Box.createHorizontalStrut(8));
            }
            filaBotones.add(botones[i]);
        }
        inferior.add(filaBotones, BorderLayout.EAST);
        panel.add(inferior, BorderLayout.SOUTH);
        return panel;
    }

    // ------------------------------------------------------------------ datos

    /** Relee las solicitudes del disco y vuelve a llenar ambas tablas. */
    public final void refrescar() {
        servicio.actualizarDesdeDisco();
        Usuario activo = Sesion.getUsuarioActivo();
        modeloRecibidas.setRowCount(0);
        modeloEnviadas.setRowCount(0);
        if (activo == null) {
            lblRecibidas.setText(" ");
            lblEnviadas.setText(" ");
            return;
        }
        ListaDinamica<Usuario> recibidas = servicio.obtenerSolicitudesRecibidas(activo.getId());
        ListaDinamica<Usuario> enviadas = servicio.obtenerSolicitudesEnviadas(activo.getId());
        llenar(modeloRecibidas, recibidas);
        llenar(modeloEnviadas, enviadas);
        lblRecibidas.setText(recibidas.estaVacia() ? "No tienes solicitudes recibidas."
                : recibidas.tamano() == 1 ? "1 solicitud recibida" : recibidas.tamano() + " solicitudes recibidas");
        lblEnviadas.setText(enviadas.estaVacia() ? "No tienes solicitudes enviadas pendientes."
                : enviadas.tamano() == 1 ? "1 solicitud enviada" : enviadas.tamano() + " solicitudes enviadas");
        if (!recibidas.estaVacia()) {
            tablaRecibidas.setRowSelectionInterval(0, 0);
        }
        botonAceptar.setEnabled(!recibidas.estaVacia());
        botonRechazar.setEnabled(!recibidas.estaVacia());
        botonCancelar.setEnabled(!enviadas.estaVacia());
    }

    /** Cantidad de solicitudes recibidas mostradas (la ventana la usa en el título de la pestaña). */
    public int getCantidadRecibidas() {
        return modeloRecibidas.getRowCount();
    }

    private static void llenar(DefaultTableModel modelo, ListaDinamica<Usuario> usuarios) {
        for (Usuario u : usuarios) {
            modelo.addRow(new Object[] {u.getNombre(), "@" + u.getUsuario(), u.getId()});
        }
    }

    // ------------------------------------------------------------------ acciones

    private void aceptarSeleccionada() {
        String[] fila = filaSeleccionada(tablaRecibidas, modeloRecibidas, "Seleccione una solicitud recibida.");
        if (fila != null) {
            ejecutar(servicio.aceptarSolicitud(idActivo(), fila[1]), "Solicitud aceptada");
        }
    }

    private void rechazarSeleccionada() {
        String[] fila = filaSeleccionada(tablaRecibidas, modeloRecibidas, "Seleccione una solicitud recibida.");
        if (fila != null && confirmar("¿Desea rechazar la solicitud de " + fila[0] + "?", "Rechazar solicitud")) {
            ejecutar(servicio.rechazarSolicitud(idActivo(), fila[1]), "Solicitud rechazada");
        }
    }

    private void cancelarSeleccionada() {
        String[] fila = filaSeleccionada(tablaEnviadas, modeloEnviadas, "Seleccione una solicitud enviada.");
        if (fila != null && confirmar("¿Desea cancelar la solicitud enviada a " + fila[0] + "?", "Cancelar solicitud")) {
            ejecutar(servicio.cancelarSolicitud(idActivo(), fila[1]), "Solicitud cancelada");
        }
    }

    /** Muestra el resultado, refresca esta pestaña y avisa a las demás. */
    private void ejecutar(ResultadoAmistad resultado, String tituloExito) {
        refrescar();
        alCambiar.run();
        if (resultado.esExitoso()) {
            JOptionPane.showMessageDialog(this, resultado.getMensaje(), tituloExito, JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int icono = resultado.getTipo() == ResultadoAmistad.Tipo.ERROR_PERSISTENCIA
                ? JOptionPane.ERROR_MESSAGE : JOptionPane.WARNING_MESSAGE;
        JOptionPane.showMessageDialog(this, resultado.getMensaje(), TITULO_ALERTA, icono);
    }

    /** @return {nombre, id} de la fila seleccionada, o {@code null} (con aviso) si no hay */
    private String[] filaSeleccionada(JTable tabla, DefaultTableModel modelo, String aviso) {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, aviso, TITULO_ALERTA, JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return new String[] {(String) modelo.getValueAt(fila, COLUMNA_NOMBRE), (String) modelo.getValueAt(fila, COLUMNA_ID)};
    }

    private boolean confirmar(String mensaje, String titulo) {
        return JOptionPane.showConfirmDialog(this, mensaje, titulo, JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    private static String idActivo() {
        Usuario activo = Sesion.getUsuarioActivo();
        return activo == null ? null : activo.getId();
    }

    private static DefaultTableModel modeloNoEditable() {
        return new DefaultTableModel(COLUMNAS, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
    }
}
