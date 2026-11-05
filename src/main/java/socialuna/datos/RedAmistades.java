package socialuna.datos;

import java.util.HashMap;
import java.util.Map;

/**
 * Grafo no dirigido de amistades representado con una <b>lista de adyacencia</b>.
 *
 * <pre>
 *   adyacencia:  "1" → ["2", "3"]
 *                "2" → ["1"]
 *                "3" → ["1"]
 * </pre>
 *
 * <ul>
 *   <li>Cada vértice es el ID de un usuario ({@code String}).</li>
 *   <li>La lista de cada vértice es una {@link ListaDinamica} con los IDs de sus amigos.</li>
 *   <li>El {@code HashMap} permite llegar a la lista de un usuario en O(1) promedio.</li>
 *   <li>Como la amistad es bidireccional, cada arista aparece en las dos listas.</li>
 * </ul>
 *
 * <p><b>Alcance de esta fase:</b> solo se define la estructura y la gestión de vértices.
 * Las operaciones sobre aristas (agregar, eliminar o consultar amistades) y su
 * persistencia las implementa el Módulo 2 (Greivin) sobre esta misma estructura.
 */
public class RedAmistades {

    private final Map<String, ListaDinamica<String>> adyacencia;

    public RedAmistades() {
        this.adyacencia = new HashMap<>();
    }

    /**
     * Registra un usuario como vértice del grafo, sin amigos.
     * Si ya existía, no hace nada.
     */
    public void agregarUsuario(String idUsuario) {
        if (idUsuario == null || idUsuario.isBlank()) {
            throw new IllegalArgumentException("El ID del usuario es obligatorio.");
        }
        adyacencia.putIfAbsent(idUsuario, new ListaDinamica<>());
    }

    public boolean contieneUsuario(String idUsuario) {
        return idUsuario != null && adyacencia.containsKey(idUsuario);
    }

    /**
     * Devuelve una copia de los IDs de los amigos del usuario
     * (lista vacía si el usuario no está en el grafo).
     */
    public ListaDinamica<String> obtenerAmigos(String idUsuario) {
        ListaDinamica<String> amigos = adyacencia.get(idUsuario);
        return amigos == null ? new ListaDinamica<>() : amigos.copia();
    }

    /** Cantidad de vértices (usuarios) en el grafo. */
    public int cantidadUsuarios() {
        return adyacencia.size();
    }

    public void limpiar() {
        adyacencia.clear();
    }

    /**
     * Acceso interno a la lista de adyacencia de un usuario, para las operaciones de
     * aristas que se agregarán en el Módulo 2 dentro de esta misma clase.
     * Devuelve {@code null} si el usuario no existe.
     */
    protected ListaDinamica<String> listaDe(String idUsuario) {
        return adyacencia.get(idUsuario);
    }
}
