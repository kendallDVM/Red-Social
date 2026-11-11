package socialuna.servicios;

import java.util.Objects;

import socialuna.datos.GestorArchivos;
import socialuna.datos.ListaPerfiles;
import socialuna.datos.ListaPublicaciones;
import socialuna.datos.ListaSolicitudes;
import socialuna.datos.ListaUsuarios;
import socialuna.datos.PersistenciaException;
import socialuna.datos.RedAmistades;
import socialuna.modelo.Usuario;

/**
 * Reúne las estructuras de datos que comparte toda la aplicación y el gestor que las
 * persiste.
 *
 * <p>Se crea <b>una sola vez</b> en {@code socialuna.Main} y se pasa a las ventanas y
 * servicios de los tres módulos. Así todas las pestañas trabajan sobre las mismas
 * listas en memoria y no se crean copias desincronizadas.
 *
 * <p>Toda la persistencia sigue centralizada en {@link GestorArchivos}; esta clase solo
 * le entrega las estructuras al cargar o guardar.
 *
 * <p><b>Varias instancias del programa:</b> cada proceso tiene su propia memoria, pero
 * todas comparten la carpeta {@code data/}. Para que una instancia vea lo que hizo otra
 * (por ejemplo, una solicitud de amistad recibida) y no la borre al guardar, el servicio
 * de amistades llama a {@link #sincronizarRelaciones()} antes de consultar o modificar
 * amistades y solicitudes, y {@link #guardarTodo()} no sobrescribe esos archivos con
 * datos viejos de memoria.
 */
public class ContextoAplicacion {

    private final GestorArchivos gestorArchivos;
    private final ListaUsuarios usuarios;
    private final ListaPublicaciones publicaciones;
    private final RedAmistades amistades;
    private final ListaSolicitudes solicitudes;
    private final ListaPerfiles perfiles;

    public ContextoAplicacion(GestorArchivos gestorArchivos) {
        this.gestorArchivos = Objects.requireNonNull(gestorArchivos, "El gestor de archivos no puede ser nulo.");
        this.usuarios = new ListaUsuarios();
        this.publicaciones = new ListaPublicaciones();
        this.amistades = new RedAmistades();
        this.solicitudes = new ListaSolicitudes();
        this.perfiles = new ListaPerfiles();
    }

    /** Carga todos los archivos de {@code data/}. Se llama una vez al iniciar. */
    public void cargarTodo() throws PersistenciaException {
        gestorArchivos.cargarTodo(usuarios, publicaciones, amistades);
        gestorArchivos.cargarSolicitudes(solicitudes, usuarios, amistades); // valida contra usuarios y amistades
        gestorArchivos.cargarPerfiles(perfiles, usuarios);
    }

    /**
     * Guarda los datos al cerrar sesión y al cerrar la aplicación.
     *
     * <ul>
     *   <li><b>Usuarios:</b> antes de escribir se agregan los usuarios que otra instancia
     *       registró en el archivo mientras esta estaba abierta; así no se borran cuentas.</li>
     *   <li><b>Publicaciones:</b> se guardan como antes.</li>
     *   <li><b>Amistades, solicitudes y perfiles:</b> <b>no</b> se reescriben aquí. El servicio de
     *       amistades ya las guarda en el momento de cada cambio (y deshace el cambio en
     *       memoria si falla), así que el archivo siempre está al día. Reescribirlas desde
     *       memoria borraría los cambios hechos por otra instancia del programa.</li>
     * </ul>
     */
    public void guardarTodo() throws PersistenciaException {
        sincronizarUsuarios();
        gestorArchivos.guardarUsuarios(usuarios);
        gestorArchivos.guardarPublicaciones(publicaciones);
    }

    /**
     * Actualiza la memoria con lo que hay en disco para usuarios, amistades y solicitudes.
     *
     * <ul>
     *   <li>Usuarios: se <b>agregan</b> los que estén en el archivo y falten en memoria
     *       (la aplicación nunca elimina usuarios).</li>
     *   <li>Amistades y solicitudes: se <b>recargan</b> del archivo, que es la fuente de
     *       verdad porque cada cambio se guarda en el momento.</li>
     * </ul>
     * Cada carga es transaccional: si un archivo está dañado, su estructura en memoria no
     * cambia y se bloquea su escritura (reglas de {@link GestorArchivos}).
     */
    public void sincronizarRelaciones() throws PersistenciaException {
        sincronizarUsuarios();
        gestorArchivos.cargarAmistades(amistades, usuarios);
        gestorArchivos.cargarSolicitudes(solicitudes, usuarios, amistades);
    }

    /**
     * Actualiza la memoria con los usuarios y los perfiles que hay en disco, para ver los
     * cambios hechos por otra instancia del programa. Los perfiles se <b>recargan</b> del
     * archivo, que es la fuente de verdad porque cada edición se guarda en el momento.
     */
    public void sincronizarPerfiles() throws PersistenciaException {
        sincronizarUsuarios();
        gestorArchivos.cargarPerfiles(perfiles, usuarios);
    }

    /** Agrega a memoria los usuarios del archivo que todavía no están (por ejemplo, registrados en otra instancia). */
    private void sincronizarUsuarios() throws PersistenciaException {
        ListaUsuarios enDisco = new ListaUsuarios();
        gestorArchivos.cargarUsuarios(enDisco);
        for (Usuario usuario : enDisco.obtenerTodos()) {
            if (!usuarios.existeId(usuario.getId()) && !usuarios.existeUsuario(usuario.getUsuario())) {
                usuarios.agregar(usuario);
            }
        }
    }

    public GestorArchivos getGestorArchivos() {
        return gestorArchivos;
    }

    public ListaUsuarios getUsuarios() {
        return usuarios;
    }

    public ListaPublicaciones getPublicaciones() {
        return publicaciones;
    }

    public RedAmistades getAmistades() {
        return amistades;
    }

    /** Solicitudes de amistad pendientes. */
    public ListaSolicitudes getSolicitudes() {
        return solicitudes;
    }

    /** Información opcional de los perfiles. */
    public ListaPerfiles getPerfiles() {
        return perfiles;
    }
}
