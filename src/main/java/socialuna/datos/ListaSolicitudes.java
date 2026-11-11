package socialuna.datos;

import socialuna.modelo.SolicitudAmistad;

/**
 * Solicitudes de amistad <b>pendientes</b>, guardadas en una {@link ListaDinamica} en el
 * orden en que se enviaron (la más antigua primero).
 *
 * <p>Reglas que mantiene la estructura:
 * <ul>
 *   <li>No hay duplicados: (A → B) solo puede estar una vez.</li>
 *   <li>Entre dos usuarios hay como máximo <b>una</b> solicitud pendiente: si existe
 *       (B → A), no se acepta (A → B). Así nunca quedan dos solicitudes cruzadas.</li>
 * </ul>
 *
 * <p>Como la lista de amistades, solo conoce IDs: no valida que los usuarios existan ni
 * que ya sean amigos. Esas reglas se aplican en {@code ServicioAmistades} y al cargar
 * {@code solicitudes.json}.
 *
 * <p>Complejidad: todas las operaciones recorren la lista, O(n) con n = solicitudes
 * pendientes en todo el sistema (es un número pequeño).
 */
public class ListaSolicitudes {

    private final ListaDinamica<SolicitudAmistad> solicitudes;

    public ListaSolicitudes() {
        this.solicitudes = new ListaDinamica<>();
    }

    /**
     * Registra la solicitud (remitente → destinatario).
     *
     * @return {@code true} si se agregó; {@code false} si ya había una solicitud pendiente
     *         entre ambos usuarios (en cualquier sentido)
     * @throws IllegalArgumentException si falta algún ID o ambos son iguales
     */
    public boolean agregar(String idRemitente, String idDestinatario) {
        SolicitudAmistad nueva = new SolicitudAmistad(idRemitente, idDestinatario);
        if (existeEntre(idRemitente, idDestinatario)) {
            return false;
        }
        solicitudes.agregar(nueva);
        return true;
    }

    /** Indica si existe la solicitud pendiente exactamente en el sentido (remitente → destinatario). */
    public boolean existe(String idRemitente, String idDestinatario) {
        return buscar(idRemitente, idDestinatario) >= 0;
    }

    /** Indica si hay una solicitud pendiente entre ambos usuarios, en cualquier sentido. */
    public boolean existeEntre(String idA, String idB) {
        return existe(idA, idB) || existe(idB, idA);
    }

    /**
     * Elimina la solicitud (remitente → destinatario).
     *
     * @return {@code true} si existía y se eliminó
     */
    public boolean eliminar(String idRemitente, String idDestinatario) {
        int indice = buscar(idRemitente, idDestinatario);
        if (indice < 0) {
            return false;
        }
        solicitudes.eliminar(indice);
        return true;
    }

    /** Elimina cualquier solicitud pendiente entre ambos usuarios (en los dos sentidos). */
    public boolean eliminarEntre(String idA, String idB) {
        boolean ab = eliminar(idA, idB);
        boolean ba = eliminar(idB, idA);
        return ab || ba;
    }

    /** IDs de quienes enviaron una solicitud al usuario, de la más antigua a la más reciente. */
    public ListaDinamica<String> obtenerRemitentesPara(String idDestinatario) {
        ListaDinamica<String> resultado = new ListaDinamica<>();
        for (SolicitudAmistad s : solicitudes) {
            if (s.getIdDestinatario().equals(idDestinatario)) {
                resultado.agregar(s.getIdRemitente());
            }
        }
        return resultado;
    }

    /** IDs de los usuarios a quienes el usuario les envió una solicitud que sigue pendiente. */
    public ListaDinamica<String> obtenerDestinatariosDe(String idRemitente) {
        ListaDinamica<String> resultado = new ListaDinamica<>();
        for (SolicitudAmistad s : solicitudes) {
            if (s.getIdRemitente().equals(idRemitente)) {
                resultado.agregar(s.getIdDestinatario());
            }
        }
        return resultado;
    }

    /** Copia de todas las solicitudes (es el formato que se guarda en {@code solicitudes.json}). */
    public ListaDinamica<SolicitudAmistad> obtenerTodas() {
        return solicitudes.copia();
    }

    public int tamano() {
        return solicitudes.tamano();
    }

    public boolean estaVacia() {
        return solicitudes.estaVacia();
    }

    public void limpiar() {
        solicitudes.limpiar();
    }

    /**
     * Reemplaza todo el contenido por el de otra lista. Lo usa {@code GestorArchivos}
     * para cargar primero en una lista temporal y solo después pasar los datos.
     */
    void reemplazarCon(ListaSolicitudes otra) {
        if (otra == this) {
            return;
        }
        solicitudes.limpiar();
        for (SolicitudAmistad s : otra.solicitudes) {
            solicitudes.agregar(s);
        }
    }

    private int buscar(String idRemitente, String idDestinatario) {
        if (idRemitente == null || idDestinatario == null) {
            return -1;
        }
        for (int i = 0; i < solicitudes.tamano(); i++) {
            SolicitudAmistad s = solicitudes.obtener(i);
            if (s.getIdRemitente().equals(idRemitente) && s.getIdDestinatario().equals(idDestinatario)) {
                return i;
            }
        }
        return -1;
    }
}
