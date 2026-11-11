package socialuna.modelo;

/**
 * Solicitud de amistad <b>pendiente</b> enviada por un usuario (remitente) a otro
 * (destinatario), identificados por su ID.
 *
 * <p>A diferencia de {@link Amistad}, la solicitud es <b>dirigida</b>: (A → B) no es lo
 * mismo que (B → A). Por eso {@link #equals(Object)} sí depende del orden.
 *
 * <p>Solo se guardan las solicitudes pendientes (cada objeto equivale a una entrada de
 * {@code solicitudes.json}). Al aceptarla se convierte en una {@link Amistad} y se
 * elimina; al rechazarla o cancelarla simplemente se elimina.
 */
public class SolicitudAmistad {

    private String idRemitente;
    private String idDestinatario;

    /** Constructor vacío requerido por Gson. */
    private SolicitudAmistad() {
    }

    /**
     * @throws IllegalArgumentException si falta algún ID o si ambos IDs son iguales
     */
    public SolicitudAmistad(String idRemitente, String idDestinatario) {
        this.idRemitente = idRemitente;
        this.idDestinatario = idDestinatario;
        validar();
    }

    public void validar() {
        if (idRemitente == null || idRemitente.isBlank() || idDestinatario == null || idDestinatario.isBlank()) {
            throw new IllegalArgumentException("La solicitud requiere los IDs del remitente y del destinatario.");
        }
        if (idRemitente.equals(idDestinatario)) {
            throw new IllegalArgumentException("Un usuario no puede enviarse una solicitud a sí mismo.");
        }
    }

    public String getIdRemitente() {
        return idRemitente;
    }

    public String getIdDestinatario() {
        return idDestinatario;
    }

    /** Indica si la solicitud es entre estos dos usuarios, en cualquier sentido. */
    public boolean esEntre(String idA, String idB) {
        return (idRemitente.equals(idA) && idDestinatario.equals(idB))
                || (idRemitente.equals(idB) && idDestinatario.equals(idA));
    }

    /** Igualdad dirigida: (A → B) es distinta de (B → A). */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SolicitudAmistad)) {
            return false;
        }
        SolicitudAmistad otra = (SolicitudAmistad) o;
        return idRemitente.equals(otra.idRemitente) && idDestinatario.equals(otra.idDestinatario);
    }

    @Override
    public int hashCode() {
        return 31 * idRemitente.hashCode() + idDestinatario.hashCode();
    }

    @Override
    public String toString() {
        return "SolicitudAmistad{" + idRemitente + " -> " + idDestinatario + "}";
    }
}
