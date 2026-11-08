package socialuna.datos;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import socialuna.modelo.Amistad;

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
 *   <li>El mapa permite llegar a la lista de un usuario en O(1) promedio. Es un
 *       {@code LinkedHashMap} para que el recorrido conserve el orden de inserción y el
 *       archivo {@code amistades.json} salga siempre en el mismo orden.</li>
 *   <li>Como la amistad es <b>bidireccional</b>, cada arista aparece en las dos listas:
 *       al agregar A–B se guarda B en la lista de A y A en la lista de B.</li>
 * </ul>
 *
 * <p>Complejidad (n = cantidad de amigos del usuario):
 * <table>
 *   <caption>Costo de las operaciones</caption>
 *   <tr><td>agregarAmistad / eliminarAmistad / sonAmigos</td><td>O(n)</td></tr>
 *   <tr><td>obtenerAmigos / cantidadAmigos</td><td>O(n) / O(1)</td></tr>
 *   <tr><td>obtenerAmistades</td><td>O(V + E)</td></tr>
 * </table>
 *
 * <p>El grafo solo conoce IDs: <b>no</b> valida que los usuarios existan. Esa regla de
 * negocio se aplica en la capa de servicios (Módulo 2) y al cargar {@code amistades.json}.
 */
public class RedAmistades {

    private final Map<String, ListaDinamica<String>> adyacencia;

    public RedAmistades() {
        this.adyacencia = new LinkedHashMap<>();
    }

    // ------------------------------------------------------------------ vértices

    /**
     * Registra un usuario como vértice del grafo, sin amigos.
     * Si ya existía, no hace nada.
     */
    public void agregarUsuario(String idUsuario) {
        validarId(idUsuario);
        adyacencia.putIfAbsent(idUsuario, new ListaDinamica<>());
    }

    public boolean contieneUsuario(String idUsuario) {
        return idUsuario != null && adyacencia.containsKey(idUsuario);
    }

    /** Cantidad de vértices (usuarios) en el grafo. */
    public int cantidadUsuarios() {
        return adyacencia.size();
    }

    // ------------------------------------------------------------------ aristas (amistades)

    /**
     * Agrega la amistad A–B en <b>ambos sentidos</b>. Si alguno de los usuarios aún no es
     * vértice del grafo, se registra automáticamente.
     *
     * @return {@code true} si la amistad es nueva; {@code false} si ya eran amigos
     *         (no se duplica)
     * @throws IllegalArgumentException si falta algún ID o si ambos IDs son iguales
     */
    public boolean agregarAmistad(String idA, String idB) {
        validarPar(idA, idB);
        if (sonAmigos(idA, idB)) {
            return false;
        }
        agregarUsuario(idA);
        agregarUsuario(idB);
        adyacencia.get(idA).agregar(idB);
        adyacencia.get(idB).agregar(idA);
        return true;
    }

    /**
     * Elimina la amistad A–B de ambos sentidos.
     *
     * @return {@code true} si existía y se eliminó; {@code false} si no eran amigos
     */
    public boolean eliminarAmistad(String idA, String idB) {
        if (!sonAmigos(idA, idB)) {
            return false;
        }
        adyacencia.get(idA).eliminarElemento(idB);
        adyacencia.get(idB).eliminarElemento(idA);
        return true;
    }

    /**
     * Indica si A y B son amigos. Devuelve {@code false} (sin lanzar error) si algún ID
     * es nulo o no está en el grafo.
     */
    public boolean sonAmigos(String idA, String idB) {
        if (idA == null || idB == null) {
            return false;
        }
        ListaDinamica<String> amigosDeA = adyacencia.get(idA);
        return amigosDeA != null && amigosDeA.contiene(idB);
    }

    /**
     * Devuelve una copia de los IDs de los amigos del usuario
     * (lista vacía si el usuario no está en el grafo).
     * Modificar la copia no altera el grafo.
     */
    public ListaDinamica<String> obtenerAmigos(String idUsuario) {
        ListaDinamica<String> amigos = adyacencia.get(idUsuario);
        return amigos == null ? new ListaDinamica<>() : amigos.copia();
    }

    /** Cantidad de amigos del usuario (0 si no está en el grafo). */
    public int cantidadAmigos(String idUsuario) {
        ListaDinamica<String> amigos = adyacencia.get(idUsuario);
        return amigos == null ? 0 : amigos.tamano();
    }

    /**
     * Devuelve todas las amistades del grafo, <b>cada una una sola vez</b>
     * (A–B y B–A son la misma). Es el formato que se guarda en {@code amistades.json}.
     *
     * <p>Recorre los vértices en orden de inserción y, para cada uno, solo agrega las
     * aristas hacia vértices que todavía no se han recorrido; así ninguna se repite.
     */
    public ListaDinamica<Amistad> obtenerAmistades() {
        ListaDinamica<Amistad> resultado = new ListaDinamica<>();
        Set<String> recorridos = new HashSet<>();
        for (Map.Entry<String, ListaDinamica<String>> vertice : adyacencia.entrySet()) {
            String id = vertice.getKey();
            for (String amigo : vertice.getValue()) {
                if (!recorridos.contains(amigo)) {
                    resultado.agregar(new Amistad(id, amigo));
                }
            }
            recorridos.add(id);
        }
        return resultado;
    }

    /** Cantidad total de amistades (aristas) del grafo. */
    public int cantidadAmistades() {
        int sumaDeGrados = 0;
        for (ListaDinamica<String> amigos : adyacencia.values()) {
            sumaDeGrados += amigos.tamano();
        }
        return sumaDeGrados / 2; // cada arista se cuenta en las dos listas
    }

    public void limpiar() {
        adyacencia.clear();
    }

    /**
     * Acceso interno a la lista de adyacencia de un usuario.
     * Devuelve {@code null} si el usuario no existe.
     */
    protected ListaDinamica<String> listaDe(String idUsuario) {
        return adyacencia.get(idUsuario);
    }

    // ------------------------------------------------------------------ validaciones

    private static void validarId(String idUsuario) {
        if (idUsuario == null || idUsuario.isBlank()) {
            throw new IllegalArgumentException("El ID del usuario es obligatorio.");
        }
    }

    private static void validarPar(String idA, String idB) {
        validarId(idA);
        validarId(idB);
        if (idA.equals(idB)) {
            throw new IllegalArgumentException("Un usuario no puede ser amigo de sí mismo.");
        }
    }
}
