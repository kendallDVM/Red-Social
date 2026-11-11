package socialuna.datos;

import socialuna.modelo.Perfil;

/**
 * Perfiles de los usuarios, guardados en una {@link ListaDinamica}. Hay como máximo un
 * perfil por usuario y solo se guardan los que tienen al menos un campo completado: un
 * usuario sin registro aquí tiene, simplemente, el perfil vacío.
 *
 * <p>Complejidad: búsqueda secuencial por ID de usuario, O(n).
 */
public class ListaPerfiles {

    private final ListaDinamica<Perfil> perfiles;

    public ListaPerfiles() {
        this.perfiles = new ListaDinamica<>();
    }

    /** Perfil del usuario, o {@code null} si no tiene información guardada. */
    public Perfil buscar(String idUsuario) {
        int i = indiceDe(idUsuario);
        return i < 0 ? null : perfiles.obtener(i);
    }

    /**
     * Guarda el perfil del usuario, reemplazando el anterior. Si el perfil está vacío, se
     * elimina el registro (no tiene sentido guardar un perfil sin datos).
     */
    public void guardar(Perfil perfil) {
        if (perfil == null) {
            throw new IllegalArgumentException("El perfil no puede ser nulo.");
        }
        int i = indiceDe(perfil.getIdUsuario());
        if (perfil.estaVacio()) {
            if (i >= 0) {
                perfiles.eliminar(i);
            }
        } else if (i >= 0) {
            perfiles.reemplazar(i, perfil);
        } else {
            perfiles.agregar(perfil);
        }
    }

    /** Elimina el perfil del usuario; devuelve {@code true} si existía. */
    public boolean eliminar(String idUsuario) {
        int i = indiceDe(idUsuario);
        if (i < 0) {
            return false;
        }
        perfiles.eliminar(i);
        return true;
    }

    /** Copia de la lista (es el formato que se guarda en {@code perfiles.json}). */
    public ListaDinamica<Perfil> obtenerTodos() {
        return perfiles.copia();
    }

    public int tamano() {
        return perfiles.tamano();
    }

    public void limpiar() {
        perfiles.limpiar();
    }

    /** Reemplaza todo el contenido (lo usa {@code GestorArchivos} tras una carga correcta). */
    void reemplazarCon(ListaPerfiles otra) {
        if (otra == this) {
            return;
        }
        perfiles.limpiar();
        for (Perfil p : otra.perfiles) {
            perfiles.agregar(p);
        }
    }

    private int indiceDe(String idUsuario) {
        if (idUsuario == null) {
            return -1;
        }
        for (int i = 0; i < perfiles.tamano(); i++) {
            if (idUsuario.equals(perfiles.obtener(i).getIdUsuario())) {
                return i;
            }
        }
        return -1;
    }
}
