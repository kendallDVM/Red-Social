package socialuna.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Objects;

import javax.swing.BorderFactory;
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
 * Pestaña "Mis amigos" del Módulo 2.
 *
 * <p>Muestra la lista de amigos del usuario activo y permite eliminar uno. La lista se
 * consulta en memoria y se refresca sin cerrar la ventana: al eliminar un amigo aquí, o
 * cuando la pestaña "Buscar usuarios" llama a {@link #refrescar()} después de agregar uno.
 */
public class PanelMisAmigos extends JPanel {

    private static final String TITULO_ALERTA = "Mis amigos";
    private static final String[] COLUMNAS = {"Nombre", "Usuario", "ID"};
    private static final int COLUMNA_NOMBRE = 0;
    private static final int COLUMNA_ID = 2;

    private final ServicioAmistades servicio;
    private final Runnable alCambiarAmigos;

    private final JButton botonEliminar = Estilos.botonSecundario("Eliminar amigo");
    private final JLabel lblResumen = Estilos.textoSecundario(" ");

    /** Modelo de la tabla: las celdas no se pueden editar. */
    private final DefaultTableModel modelo = new DefaultTableModel(COLUMNAS, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final JTable tabla = new JTable(modelo);

    /**
     * @param servicio        lógica de amistades
     * @param alCambiarAmigos se ejecuta después de eliminar un amigo (por ejemplo, para
     *                        refrescar la pestaña "Buscar usuarios"); puede ser {@code null}
     */
    public PanelMisAmigos(ServicioAmistades servicio, Runnable alCambiarAmigos) {
        this.servicio = Objects.requireNonNull(servicio, "El servicio no puede ser nulo.");
        this.alCambiarAmigos = alCambiarAmigos != null ? alCambiarAmigos : () -> { };
        construirInterfaz();
        refrescar();
    }

    private void construirInterfaz() {
        setLayout(new BorderLayout(0, 12));
        setBackground(Estilos.TARJETA);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // ---- parte superior: título
        JPanel titulos = new JPanel(new BorderLayout(0, 2));
        titulos.setOpaque(false);
        titulos.add(Estilos.titulo("Mis amigos"), BorderLayout.NORTH);
        titulos.add(Estilos.textoSecundario("Estudiantes que agregaste como amigos. Ves sus publicaciones en el muro."),
                BorderLayout.SOUTH);
        add(titulos, BorderLayout.NORTH);

        // ---- centro: tabla de amigos
        tabla.setName("tablaAmigos");
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setRowHeight(28);
        tabla.setFont(Estilos.fuente(Font.PLAIN, 14));
        tabla.getTableHeader().setFont(Estilos.fuente(Font.BOLD, 13));
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.setFillsViewportHeight(true);
        JScrollPane desplazamiento = new JScrollPane(tabla);
        desplazamiento.setBorder(BorderFactory.createLineBorder(Estilos.BORDE));
        add(desplazamiento, BorderLayout.CENTER);

        // ---- parte inferior: resumen y botón eliminar
        JPanel inferior = new JPanel(new BorderLayout());
        inferior.setOpaque(false);
        lblResumen.setName("resumenAmigos");
        inferior.add(lblResumen, BorderLayout.WEST);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        botones.setOpaque(false);
        botones.add(botonEliminar);
        inferior.add(botones, BorderLayout.EAST);
        add(inferior, BorderLayout.SOUTH);

        botonEliminar.addActionListener(e -> eliminarSeleccionado());
    }

    /** Vuelve a cargar la lista de amigos del usuario activo desde memoria. */
    public final void refrescar() {
        Usuario activo = Sesion.getUsuarioActivo();
        modelo.setRowCount(0);
        if (activo == null) {
            lblResumen.setText(" ");
            return;
        }
        ListaDinamica<Usuario> amigos = servicio.obtenerAmigos(activo.getId());
        for (Usuario amigo : amigos) {
            modelo.addRow(new Object[] {amigo.getNombre(), "@" + amigo.getUsuario(), amigo.getId()});
        }
        int cantidad = amigos.tamano();
        if (cantidad == 0) {
            lblResumen.setText("Aún no tienes amigos. Búscalos en la pestaña \"Buscar usuarios\".");
        } else {
            lblResumen.setText(cantidad == 1 ? "Tienes 1 amigo" : "Tienes " + cantidad + " amigos");
        }
    }

    private void eliminarSeleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un amigo de la lista.",
                    TITULO_ALERTA, JOptionPane.WARNING_MESSAGE);
            return;
        }
        String nombre = (String) modelo.getValueAt(fila, COLUMNA_NOMBRE);
        String idAmigo = (String) modelo.getValueAt(fila, COLUMNA_ID);

        int opcion = JOptionPane.showConfirmDialog(this,
                "¿Seguro que desea eliminar a " + nombre + " de sus amigos?",
                "Eliminar amigo", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }

        Usuario activo = Sesion.getUsuarioActivo();
        ResultadoAmistad resultado = servicio.eliminarAmigo(activo == null ? null : activo.getId(), idAmigo);
        if (resultado.esExitoso()) {
            refrescar();
            alCambiarAmigos.run();
            JOptionPane.showMessageDialog(this, resultado.getMensaje(), "Amigo eliminado",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int icono = resultado.getTipo() == ResultadoAmistad.Tipo.ERROR_PERSISTENCIA
                ? JOptionPane.ERROR_MESSAGE : JOptionPane.WARNING_MESSAGE;
        JOptionPane.showMessageDialog(this, resultado.getMensaje(), TITULO_ALERTA, icono);
        refrescar();
    }
}
