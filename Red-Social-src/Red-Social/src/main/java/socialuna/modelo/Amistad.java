package socialuna.modelo;

/**
 * Relación de amistad entre dos usuarios, identificados por su ID.
 *
 * <p>La amistad es <b>bidireccional</b>: {@code Amistad(A, B)} y {@code Amistad(B, A)}
 * representan la misma relación. Por eso {@link #equals(Object)} y {@link #hashCode()}
 * no dependen del orden.
 *
 * <p>Se usa sobre todo como formato de persistencia (cada objeto equivale a una entrada
 * de {@code amistades.json}). En memoria, las amistades se representan con la lista de
 * adyacencia de {@code RedAmistades}. La lógica de amistades corresponde al Módulo 2.
 */
public class Amistad {

    private String idUsuarioA;
    private String idUsuarioB;

    /** Constructor vacío requerido por Gson. */
    private Amistad() {
    }

    /**
     * @throws IllegalArgumentException si falta algún ID o si ambos IDs son iguales
     */
    public Amistad(String idUsuarioA, String idUsuarioB) {
        this.idUsuarioA = idUsuarioA;
        this.idUsuarioB = idUsuarioB;
        validar();
    }

    public void validar() {
        if (idUsuarioA == null || idUsuarioA.isBlank() || idUsuarioB == null || idUsuarioB.isBlank()) {
            throw new IllegalArgumentException("La amistad requiere los IDs de ambos usuarios.");
        }
        if (idUsuarioA.equals(idUsuarioB)) {
            throw new IllegalArgumentException("Un usuario no puede ser amigo de sí mismo.");
        }
    }

    public String getIdUsuarioA() {
        return idUsuarioA;
    }

    public String getIdUsuarioB() {
        return idUsuarioB;
    }

    /** Indica si el usuario participa en esta amistad. */
    public boolean involucra(String idUsuario) {
        return idUsuarioA.equals(idUsuario) || idUsuarioB.equals(idUsuario);
    }

    /**
     * Devuelve el ID del otro participante.
     *
     * @throws IllegalArgumentException si {@code idUsuario} no participa en la amistad
     */
    public String obtenerOtro(String idUsuario) {
        if (idUsuarioA.equals(idUsuario)) {
            return idUsuarioB;
        }
        if (idUsuarioB.equals(idUsuario)) {
            return idUsuarioA;
        }
        throw new IllegalArgumentException("El usuario " + idUsuario + " no participa en esta amistad.");
    }

    /** Igualdad sin importar el orden: (A,B) es igual a (B,A). */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Amistad)) {
            return false;
        }
        Amistad otra = (Amistad) o;
        return (idUsuarioA.equals(otra.idUsuarioA) && idUsuarioB.equals(otra.idUsuarioB))
                || (idUsuarioA.equals(otra.idUsuarioB) && idUsuarioB.equals(otra.idUsuarioA));
    }

    @Override
    public int hashCode() {
        // Suma de hashes: conmutativa, así (A,B) y (B,A) producen el mismo valor.
        return idUsuarioA.hashCode() + idUsuarioB.hashCode();
    }

    @Override
    public String toString() {
        return "Amistad{" + idUsuarioA + " <-> " + idUsuarioB + "}";
    }
}
