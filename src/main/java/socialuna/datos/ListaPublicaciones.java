package socialuna.datos;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import socialuna.modelo.Publicacion;

/**
 * Administra las publicaciones en memoria usando una {@link ListaDinamica}.
 *
 * <p>Además de las operaciones generales (agregar, consultar, eliminar), ofrece las
 * consultas del muro: filtrar por autores y ordenar de la más reciente a la más antigua.
 * La lista interna conserva el <b>orden de publicación</b>; el orden del muro se calcula
 * en cada consulta y no altera ese historial.
 */
public class ListaPublicaciones {

    /** Prefijo de los IDs generados automáticamente ({@code P1}, {@code P2}, ...). */
    public static final String PREFIJO_ID = "P";

    private final ListaDinamica<Publicacion> publicaciones;

    public ListaPublicaciones() {
        this.publicaciones = new ListaDinamica<>();
    }

    /**
     * @throws IllegalArgumentException si la publicación es nula, inválida o su ID ya existe
     */
    public void agregar(Publicacion publicacion) {
        if (publicacion == null) {
            throw new IllegalArgumentException("La publicación no puede ser nula.");
        }
        publicacion.validar();
        if (existeId(publicacion.getId())) {
            throw new IllegalArgumentException("Ya existe una publicación con el ID '" + publicacion.getId() + "'.");
        }
        publicaciones.agregar(publicacion);
    }

    /** Devuelve la publicación de la posición indicada (en orden de inserción). */
    public Publicacion obtener(int indice) {
        return publicaciones.obtener(indice);
    }

    /** Búsqueda secuencial por ID: O(n). */
    public Publicacion buscarPorId(String id) {
        if (id == null) {
            return null;
        }
        for (Publicacion p : publicaciones) {
            if (id.equals(p.getId())) {
                return p;
            }
        }
        return null;
    }

    public boolean existeId(String id) {
        return buscarPorId(id) != null;
    }

    /**
     * Incorpora lo que otra instancia del programa guardó en el archivo ({@code otra}):
     * <ul>
     *   <li>Agrega las publicaciones que todavía no están (se comparan por ID). Nunca se
     *       eliminan publicaciones ni se cambia su texto.</li>
     *   <li>En las que ya existen, los "me gusta" se <b>reemplazan</b> por los de {@code otra}:
     *       cada cambio se guarda en el momento, así que el archivo es la fuente de verdad
     *       (unirlos haría reaparecer un "me gusta" que otra instancia ya quitó).</li>
     * </ul>
     *
     * @return cantidad de publicaciones agregadas
     */
    public int combinarCon(ListaPublicaciones otra) {
        int agregadas = 0;
        for (Publicacion p : otra.publicaciones) {
            Publicacion existente = buscarPorId(p.getId());
            if (existente == null) {
                publicaciones.agregar(p);
                agregadas++;
            } else if (existente != p) {
                existente.reemplazarMeGusta(p.getMeGusta());
            }
        }
        return agregadas;
    }

    /** @return {@code true} si se encontró y eliminó la publicación */
    public boolean eliminarPorId(String id) {
        Publicacion p = buscarPorId(id);
        return p != null && publicaciones.eliminarElemento(p);
    }

    /**
     * Genera un ID que no existe todavía: {@code P} seguido del mayor número usado + 1.
     * Los IDs que no siguen ese formato (por ejemplo, de un archivo editado a mano) se
     * ignoran al calcular el número, pero nunca se repiten.
     */
    public String generarId() {
        long mayor = 0;
        for (Publicacion p : publicaciones) {
            String id = p.getId();
            if (id.startsWith(PREFIJO_ID)) {
                try {
                    mayor = Math.max(mayor, Long.parseLong(id.substring(PREFIJO_ID.length())));
                } catch (NumberFormatException e) {
                    // ID con otro formato: no cuenta para la numeración
                }
            }
        }
        String id = PREFIJO_ID + (mayor + 1);
        while (existeId(id)) { // seguridad extra ante IDs desbordados o raros
            mayor++;
            id = PREFIJO_ID + (mayor + 1);
        }
        return id;
    }

    /**
     * Publicaciones cuyo autor está en {@code idsAutores}, ordenadas de la <b>más reciente a
     * la más antigua</b>. Si dos tienen la misma fecha y hora, la publicada después aparece primero.
     *
     * <p>Costo: O(n) para filtrar (los IDs se buscan en un conjunto hash) más O(k log k)
     * para ordenar las k publicaciones que quedaron.
     *
     * @param idsAutores IDs de los autores a incluir; si es {@code null} o está vacía, el resultado es vacío
     */
    public ListaDinamica<Publicacion> obtenerRecientesDe(ListaDinamica<String> idsAutores) {
        ListaDinamica<Publicacion> resultado = new ListaDinamica<>();
        if (idsAutores == null || idsAutores.estaVacia()) {
            return resultado;
        }
        Set<String> autores = new HashSet<>();
        for (String id : idsAutores) {
            autores.add(id);
        }

        // Se arma en orden inverso de publicación y luego se ordena con un algoritmo
        // estable: los empates de fecha quedan con la publicación más nueva primero.
        Publicacion[] filtradas = new Publicacion[publicaciones.tamano()];
        int cantidad = 0;
        for (int i = publicaciones.tamano() - 1; i >= 0; i--) {
            Publicacion p = publicaciones.obtener(i);
            if (autores.contains(p.getIdAutor())) {
                filtradas[cantidad++] = p;
            }
        }
        Publicacion[] ordenadas = Arrays.copyOf(filtradas, cantidad);
        // El formato yyyy-MM-dd HH:mm:ss se ordena igual como texto que como fecha
        Arrays.sort(ordenadas, Comparator.comparing(Publicacion::getFechaHora).reversed());
        for (Publicacion p : ordenadas) {
            resultado.agregar(p);
        }
        return resultado;
    }

    /** Devuelve una copia de la lista; modificarla no altera los datos internos. */
    public ListaDinamica<Publicacion> obtenerTodas() {
        return publicaciones.copia();
    }

    public int tamano() {
        return publicaciones.tamano();
    }

    public boolean estaVacia() {
        return publicaciones.estaVacia();
    }

    public void limpiar() {
        publicaciones.limpiar();
    }
}
