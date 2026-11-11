package socialuna.servicios;

import java.util.Objects;

import socialuna.datos.GestorArchivos;
import socialuna.datos.ListaPublicaciones;
import socialuna.datos.ListaUsuarios;
import socialuna.datos.PersistenciaException;
import socialuna.datos.RedAmistades;

/**
 * Reúne las estructuras de datos que comparte toda la aplicación y el gestor que las
 * persiste.
 *
 * <p>Se crea <b>una sola vez</b> en {@code socialuna.Main} y se pasa a las ventanas y
 * servicios de los tres módulos. Así todas las pestañas trabajan sobre las mismas
 * listas en memoria y no se crean copias desincronizadas.
 *
 * <p>Toda la persistencia sigue centralizada en {@link GestorArchivos}; esta clase solo
 * le entrega las tres estructuras al cargar o guardar.
 */
public class ContextoAplicacion {

    private final GestorArchivos gestorArchivos;
    private final ListaUsuarios usuarios;
    private final ListaPublicaciones publicaciones;
    private final RedAmistades amistades;

    public ContextoAplicacion(GestorArchivos gestorArchivos) {
        this.gestorArchivos = Objects.requireNonNull(gestorArchivos, "El gestor de archivos no puede ser nulo.");
        this.usuarios = new ListaUsuarios();
        this.publicaciones = new ListaPublicaciones();
        this.amistades = new RedAmistades();
    }

    /** Carga todos los archivos de {@code data/}. Se llama una vez al iniciar. */
    public void cargarTodo() throws PersistenciaException {
        gestorArchivos.cargarTodo(usuarios, publicaciones, amistades);
    }

    /** Guarda todos los datos. Se llama al cerrar sesión y al cerrar la aplicación. */
    public void guardarTodo() throws PersistenciaException {
        gestorArchivos.guardarTodo(usuarios, publicaciones, amistades);
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
}
