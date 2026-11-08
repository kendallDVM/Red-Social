package socialuna.datos;

import socialuna.modelo.Publicacion;

/**
 * Administra las publicaciones en memoria usando una {@link ListaDinamica}.
 *
 * <p>Solo incluye las operaciones generales (agregar, consultar, eliminar). La lógica
 * del muro (filtrar por autor o por amigos, ordenar por fecha) corresponde al Módulo 3.
 */
public class ListaPublicaciones {

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

    /** @return {@code true} si se encontró y eliminó la publicación */
    public boolean eliminarPorId(String id) {
        Publicacion p = buscarPorId(id);
        return p != null && publicaciones.eliminarElemento(p);
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
