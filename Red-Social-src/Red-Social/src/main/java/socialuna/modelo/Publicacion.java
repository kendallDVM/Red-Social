package socialuna.modelo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/**
 * Mensaje publicado por un usuario.
 *
 * <p>La fecha se guarda como texto con el formato acordado por el equipo
 * ({@value #PATRON_FECHA}). Así el JSON queda legible y no hace falta un
 * adaptador de Gson para {@code LocalDateTime}.
 *
 * <p>Clase base compartida: la lógica de publicar y del muro corresponde al Módulo 3.
 */
public class Publicacion {

    /** Formato de fecha acordado por el equipo. */
    public static final String PATRON_FECHA = "yyyy-MM-dd HH:mm:ss";
    public static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern(PATRON_FECHA);

    private String id;
    private String idAutor;
    private String texto;
    private String fechaHora;

    /** Constructor vacío requerido por Gson. */
    private Publicacion() {
    }

    /**
     * @param fechaHora fecha en formato {@value #PATRON_FECHA}
     * @throws IllegalArgumentException si algún dato es inválido
     */
    public Publicacion(String id, String idAutor, String texto, String fechaHora) {
        this.id = id;
        this.idAutor = idAutor;
        this.texto = texto;
        this.fechaHora = fechaHora;
        validar();
    }

    /** Crea la publicación usando un {@link LocalDateTime}, que se formatea automáticamente. */
    public Publicacion(String id, String idAutor, String texto, LocalDateTime fechaHora) {
        this(id, idAutor, texto, fechaHora == null ? null : fechaHora.format(FORMATO_FECHA));
    }

    /** Verifica los datos básicos. También sirve para objetos creados por Gson. */
    public void validar() {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El ID de la publicación es obligatorio.");
        }
        if (idAutor == null || idAutor.isBlank()) {
            throw new IllegalArgumentException("El autor de la publicación es obligatorio.");
        }
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("El texto de la publicación es obligatorio.");
        }
        if (!esFechaValida(fechaHora)) {
            throw new IllegalArgumentException("La fecha debe tener el formato " + PATRON_FECHA + ".");
        }
    }

    public static boolean esFechaValida(String fecha) {
        if (fecha == null) {
            return false;
        }
        try {
            LocalDateTime.parse(fecha, FORMATO_FECHA);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public String getId() {
        return id;
    }

    public String getIdAutor() {
        return idAutor;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("El texto de la publicación es obligatorio.");
        }
        this.texto = texto;
    }

    public String getFechaHora() {
        return fechaHora;
    }

    /** Fecha convertida a {@link LocalDateTime}, útil para ordenar el muro. */
    public LocalDateTime getFechaHoraComoFecha() {
        return LocalDateTime.parse(fechaHora, FORMATO_FECHA);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Publicacion)) {
            return false;
        }
        return Objects.equals(id, ((Publicacion) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Publicacion{id='" + id + "', idAutor='" + idAutor + "', fechaHora='" + fechaHora
                + "', texto='" + texto + "'}";
    }
}
