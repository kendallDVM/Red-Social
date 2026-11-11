package socialuna.datos;

import java.util.HashMap;
import java.util.Map;

import socialuna.modelo.Usuario;

/**
 * Administra los usuarios registrados en memoria.
 *
 * <p>Usa dos estructuras que siempre se mantienen sincronizadas:
 * <ol>
 *   <li>{@link ListaDinamica}{@code <Usuario>}: almacena los usuarios en orden de registro.
 *       Es la estructura "dueña" de los datos y la que se recorre y se guarda en JSON.</li>
 *   <li>{@code HashMap<String, Usuario>} (diccionario usuario → Usuario, acuerdo del
 *       equipo): índice para buscar por nombre de usuario en O(1) promedio, en lugar de
 *       recorrer toda la lista (O(n)). Durante el login se busca por nombre de usuario,
 *       por eso esa búsqueda es la que se optimiza.</li>
 * </ol>
 *
 * <p><b>Sin distinguir mayúsculas:</b> las claves del índice se guardan normalizadas con
 * {@link Usuario#normalizarNombreUsuario(String)} ({@code "Kendall"} → {@code "kendall"}).
 * Por eso {@code buscarPorUsuario("KENDALL")} encuentra la cuenta {@code kendall}, y no
 * se puede registrar {@code Kendall} si ya existe {@code kendall}. El objeto
 * {@code Usuario} conserva el nombre tal como se escribió, para mostrarlo.
 *
 * <p>Sincronización: la única forma de agregar es {@link #agregar(Usuario)}, que inserta
 * en ambas estructuras. {@code Usuario} no permite cambiar su {@code usuario} ni su
 * {@code id}, así que las claves del índice no pueden quedar obsoletas.
 * {@link #obtenerTodos()} devuelve una copia, así que modificar esa copia no afecta
 * el índice.
 */
public class ListaUsuarios {

    private final ListaDinamica<Usuario> usuarios;
    private final Map<String, Usuario> indicePorUsuario;

    public ListaUsuarios() {
        this.usuarios = new ListaDinamica<>();
        this.indicePorUsuario = new HashMap<>();
    }

    /**
     * Agrega un usuario después de validarlo.
     *
     * @throws IllegalArgumentException si el usuario es nulo, tiene datos inválidos,
     *                                  o si su ID o nombre de usuario ya existen
     */
    public void agregar(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("El usuario no puede ser nulo.");
        }
        usuario.validar();
        if (existeUsuario(usuario.getUsuario())) {
            throw new IllegalArgumentException("El nombre de usuario '" + usuario.getUsuario() + "' ya existe.");
        }
        if (existeId(usuario.getId())) {
            throw new IllegalArgumentException("El ID '" + usuario.getId() + "' ya está registrado.");
        }
        usuarios.agregar(usuario);
        indicePorUsuario.put(clave(usuario.getUsuario()), usuario);
    }

    /** Búsqueda por nombre de usuario (sin distinguir mayúsculas) usando el diccionario: O(1) promedio. */
    public Usuario buscarPorUsuario(String nombreUsuario) {
        if (nombreUsuario == null) {
            return null;
        }
        return indicePorUsuario.get(clave(nombreUsuario));
    }

    /** Búsqueda secuencial por ID sobre la lista dinámica: O(n). */
    public Usuario buscarPorId(String id) {
        if (id == null) {
            return null;
        }
        for (Usuario u : usuarios) {
            if (id.equals(u.getId())) {
                return u;
            }
        }
        return null;
    }

    public boolean existeUsuario(String nombreUsuario) {
        return nombreUsuario != null && indicePorUsuario.containsKey(clave(nombreUsuario));
    }

    public boolean existeId(String id) {
        return buscarPorId(id) != null;
    }

    /** Devuelve una copia de la lista; modificarla no altera los datos internos. */
    public ListaDinamica<Usuario> obtenerTodos() {
        return usuarios.copia();
    }

    public int tamano() {
        return usuarios.tamano();
    }

    public boolean estaVacia() {
        return usuarios.estaVacia();
    }

    /** Vacía la lista y el índice (se usa antes de recargar desde archivo). */
    public void limpiar() {
        usuarios.limpiar();
        indicePorUsuario.clear();
    }

    /**
     * Reemplaza todo el contenido por el de otra lista. Lo usa {@code GestorArchivos}
     * para cargar primero en una lista temporal y, solo si todo salió bien, pasar los
     * datos a la lista real.
     */
    void reemplazarCon(ListaUsuarios otra) {
        if (otra == this) {
            return;
        }
        limpiar();
        for (Usuario u : otra.usuarios) {
            usuarios.agregar(u);
            indicePorUsuario.put(clave(u.getUsuario()), u);
        }
    }

    /**
     * Crea una copia independiente con los mismos usuarios (los objetos {@code Usuario}
     * son compartidos). El servicio de autenticación la usa para guardar primero en
     * disco y modificar la lista real solo si el guardado tuvo éxito.
     */
    public ListaUsuarios copia() {
        ListaUsuarios copia = new ListaUsuarios();
        copia.reemplazarCon(this);
        return copia;
    }

    /** Clave del índice: nombre de usuario normalizado a minúsculas. */
    private static String clave(String nombreUsuario) {
        return Usuario.normalizarNombreUsuario(nombreUsuario);
    }
}
