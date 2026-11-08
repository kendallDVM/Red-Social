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
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import socialuna.datos.ListaDinamica;
import socialuna.modelo.Usuario;
import socialuna.servicios.ResultadoAmistad;
import socialuna.servicios.ServicioAmistades;
import socialuna.servicios.Sesion;

/**
 * Pestaña "Buscar usuarios" del Módulo 2.
 *
 * <p>Permite buscar estudiantes registrados por nombre o nombre de usuario y agregar al
 * seleccionado como amigo. Toda la lógica está en {@link ServicioAmistades}; este panel
 * solo recoge los datos y muestra los resultados y las alertas.
 *
 * <p>Es un {@code JPanel} para poder colocarlo como pestaña en la ventana principal.
 */
public class PanelBuscarUsuarios extends JPanel {

    private static final String TITULO_ALERTA = "Buscar usuarios";
    private static final String[] COLUMNAS = {"Nombre", "Usuario", "ID", "Estado"};
    private static final int COLUMNA_ID = 2;

    private final ServicioAmistades servicio;
    private final Runnable alCambiarAmigos;

    private final JTextField campoBusqueda = Estilos.campoTexto(24);
    private final JButton botonBuscar = Estilos.botonPrimario("Buscar");
    private final JButton botonAgregar = Estilos.botonPrimario("Agregar como amigo");
    private final JLabel lblResumen = Estilos.textoSecundario(" ");

    /** Modelo de la tabla: las celdas no se pueden editar. */
    private final DefaultTableModel modelo = new DefaultTableModel(COLUMNAS, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final JTable tabla = new JTable(modelo);

    /** Último texto buscado, para refrescar la tabla después de agregar un amigo. */
    private String ultimaBusqueda = "";

    /**
     * @param servicio        lógica de amistades
     * @param alCambiarAmigos se ejecuta después de agregar un amigo (por ejemplo, para
     *                        refrescar la pestaña "Mis amigos"); puede ser {@code null}
     */
    public PanelBuscarUsuarios(ServicioAmistades servicio, Runnable alCambiarAmigos) {
        this.servicio = Objects.requireNonNull(servicio, "El servicio no puede ser nulo.");
        this.alCambiarAmigos = alCambiarAmigos != null ? alCambiarAmigos : () -> { };
        construirInterfaz();
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
        titulos.add(Estilos.textoSecundario("Escribe parte del nombre o del usuario del estudiante."),
                BorderLayout.SOUTH);
        superior.add(titulos, BorderLayout.NORTH);

        JPanel buscador = new JPanel(new BorderLayout(8, 0));
        buscador.setOpaque(false);
        campoBusqueda.setName("campoBusqueda");
        buscador.add(campoBusqueda, BorderLayout.CENTER);
        buscador.add(botonBuscar, BorderLayout.EAST);
        superior.add(buscador, BorderLayout.SOUTH);
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

        // ---- parte inferior: resumen y botón agregar
        JPanel inferior = new JPanel(new BorderLayout());
        inferior.setOpaque(false);
        inferior.add(lblResumen, BorderLayout.WEST);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        botones.setOpaque(false);
        botones.add(botonAgregar);
        inferior.add(botones, BorderLayout.EAST);
        add(inferior, BorderLayout.SOUTH);

        // ---- acciones
        botonBuscar.addActionListener(e -> buscar());
        campoBusqueda.addActionListener(e -> buscar()); // Enter en el cuadro de texto
        botonAgregar.addActionListener(e -> agregarSeleccionado());
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
        int encontrados = mostrarResultados(texto);
        if (encontrados == 0) {
            JOptionPane.showMessageDialog(this, "No se encontraron usuarios que coincidan con \""
                    + texto.strip() + "\".", TITULO_ALERTA, JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void agregarSeleccionado() {
        Usuario activo = Sesion.getUsuarioActivo();
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un usuario de la lista.",
                    TITULO_ALERTA, JOptionPane.WARNING_MESSAGE);
            return;
        }
        String idSeleccionado = (String) modelo.getValueAt(fila, COLUMNA_ID);
        ResultadoAmistad resultado = servicio.agregarAmigo(activo == null ? null : activo.getId(), idSeleccionado);

        if (resultado.esExitoso()) {
            JOptionPane.showMessageDialog(this, resultado.getMensaje(), "Amigo agregado",
                    JOptionPane.INFORMATION_MESSAGE);
            mostrarResultados(ultimaBusqueda); // actualiza la columna "Estado"
            alCambiarAmigos.run();
            return;
        }
        int icono = resultado.getTipo() == ResultadoAmistad.Tipo.ERROR_PERSISTENCIA
                ? JOptionPane.ERROR_MESSAGE : JOptionPane.WARNING_MESSAGE;
        JOptionPane.showMessageDialog(this, resultado.getMensaje(), TITULO_ALERTA, icono);
    }

    /**
     * Vuelve a ejecutar la última búsqueda (si hubo una) para actualizar la columna
     * "Estado". Lo puede llamar la pestaña "Mis amigos" al eliminar un amigo.
     */
    public void refrescar() {
        if (!ultimaBusqueda.isBlank()) {
            mostrarResultados(ultimaBusqueda);
        }
    }

    /** Llena la tabla con los resultados de la búsqueda y devuelve cuántos hay. */
    private int mostrarResultados(String texto) {
        Usuario activo = Sesion.getUsuarioActivo();
        String idActivo = activo == null ? null : activo.getId();

        modelo.setRowCount(0);
        ListaDinamica<Usuario> resultados = servicio.buscarUsuarios(idActivo, texto);
        for (Usuario usuario : resultados) {
            String estado = servicio.sonAmigos(idActivo, usuario.getId()) ? "Ya es tu amigo" : "";
            modelo.addRow(new Object[] {usuario.getNombre(), "@" + usuario.getUsuario(), usuario.getId(), estado});
        }
        int cantidad = resultados.tamano();
        lblResumen.setText(cantidad == 1 ? "1 usuario encontrado" : cantidad + " usuarios encontrados");
        return cantidad;
    }
}
