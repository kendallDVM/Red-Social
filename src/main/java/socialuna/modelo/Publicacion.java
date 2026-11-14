package socialuna.modelo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Mensaje publicado por un usuario.
 *
 * <p>La fecha se guarda como texto con el formato acordado por el equipo
 * ({@value #PATRON_FECHA}). Así el JSON queda legible y no hace falta un
 * adaptador de Gson para {@code LocalDateTime}.
 *
 * <p>Guarda también a quiénes les gusta ("me gusta"): un conjunto de IDs de usuario, sin
 * repetidos y en el orden en que lo dieron. Mientras nadie le dé "me gusta" el campo queda
 * vacío ({@code null}) y no aparece en el JSON, así los archivos anteriores siguen siendo válidos.
 *
 * <p><b>Eliminación:</b> cuando el autor elimina una publicación no se borra del archivo, sino
 * que se <b>marca</b> como eliminada ({@code "eliminada": true}) y deja de mostrarse. Así, si el
 * programa está abierto dos veces, la otra instancia se entera de que se eliminó y no la vuelve
 * a agregar al sincronizar. Mientras no esté eliminada el campo no aparece en el JSON.
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

    /** IDs de los usuarios a quienes les gusta; {@code null} si a nadie (no se escribe en el JSON). */
    private Set<String> meGusta;

    /** {@code true} si el autor la eliminó; {@code null} si sigue visible (no se escribe en el JSON). */
    private Boolean eliminada;

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

    /**
     * Verifica los datos básicos. También sirve para objetos creados por Gson: además
     * descarta de "me gusta" los IDs vacíos que pudiera traer un archivo editado a mano.
     */
    public void validar() {
        if (meGusta != null) {
            meGusta.removeIf(id -> id == null || id.isBlank());
            if (meGusta.isEmpty()) {
                meGusta = null;
            }
        }
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

    // ------------------------------------------------------------------ eliminación

    /** Indica si el autor eliminó la publicación (ya no se muestra en ningún muro). */
    public boolean isEliminada() {
        return Boolean.TRUE.equals(eliminada);
    }

    /**
     * Marca o desmarca la publicación como eliminada. Desmarcar solo se usa para deshacer
     * una eliminación cuyo guardado falló.
     */
    public void setEliminada(boolean eliminada) {
        this.eliminada = eliminada ? Boolean.TRUE : null;
    }

    // ------------------------------------------------------------------ me gusta

    /** Indica si a ese usuario le gusta la publicación. */
    public boolean leGustaA(String idUsuario) {
        return meGusta != null && idUsuario != null && meGusta.contains(idUsuario);
    }

    /** Cantidad de usuarios a quienes les gusta la publicación. */
    public int cantidadMeGusta() {
        return meGusta == null ? 0 : meGusta.size();
    }

    /** Copia de solo lectura de los IDs a quienes les gusta, en el orden en que dieron "me gusta". */
    public Set<String> getMeGusta() {
        return meGusta == null ? Collections.emptySet() : Collections.unmodifiableSet(new LinkedHashSet<>(meGusta));
    }

    /**
     * Registra que a ese usuario le gusta la publicación.
     *
     * @return {@code true} si es nuevo; {@code false} si ya le gustaba
     * @throws IllegalArgumentException si falta el ID
     */
    public boolean agregarMeGusta(String idUsuario) {
        if (idUsuario == null || idUsuario.isBlank()) {
            throw new IllegalArgumentException("El ID del usuario es obligatorio.");
        }
        if (meGusta == null) {
            meGusta = new LinkedHashSet<>();
        }
        return meGusta.add(idUsuario);
    }

    /** @return {@code true} si le gustaba y se quitó; {@code false} si no le gustaba */
    public boolean quitarMeGusta(String idUsuario) {
        if (meGusta == null || !meGusta.remove(idUsuario)) {
            return false;
        }
        if (meGusta.isEmpty()) {
            meGusta = null;
        }
        return true;
    }

    /** Reemplaza todos los "me gusta" por los indicados (se ignoran IDs vacíos y repetidos). */
    public void reemplazarMeGusta(Collection<String> idsUsuarios) {
        meGusta = null;
        for (String id : idsUsuarios) {
            if (id != null && !id.isBlank()) {
                agregarMeGusta(id);
            }
        }
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
