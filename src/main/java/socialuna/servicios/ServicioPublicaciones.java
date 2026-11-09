package socialuna.servicios;

import java.time.LocalDateTime;
import java.util.Objects;

import socialuna.datos.ListaDinamica;
import socialuna.datos.ListaPublicaciones;
import socialuna.datos.PersistenciaException;
import socialuna.modelo.Publicacion;
import socialuna.modelo.Usuario;

/**
 * Lógica del Módulo 3: publicar mensajes y armar el muro.
 *
 * <p>Las ventanas solo recogen el texto y muestran el resultado; todas las reglas están
 * aquí, así se pueden probar sin interfaz gráfica. Todo se consulta en memoria; el
 * archivo {@code publicaciones.json} solo se escribe al publicar.
 *
 * <p>Usa el Módulo 2 ({@link ServicioAmistades#obtenerIdsVisibles}) para saber de quién
 * se ven las publicaciones, y el Módulo 1 (la lista de usuarios) para mostrar el nombre
 * del autor.
 */
public class ServicioPublicaciones {

    /** Longitud máxima de un mensaje. */
    public static final int MAX_CARACTERES = 500;

    private final ContextoAplicacion contexto;
    private final ServicioAmistades servicioAmistades;

    public ServicioPublicaciones(ContextoAplicacion contexto, ServicioAmistades servicioAmistades) {
        this.contexto = Objects.requireNonNull(contexto, "El contexto no puede ser nulo.");
        this.servicioAmistades = Objects.requireNonNull(servicioAmistades,
                "El servicio de amistades no puede ser nulo.");
    }

    // ------------------------------------------------------------------ publicar

    /**
     * Publica un mensaje del usuario indicado con la fecha y hora actuales y lo guarda en
     * {@code publicaciones.json} en ese momento.
     *
     * <p>Si el guardado falla, la publicación se quita de memoria para que memoria y
     * archivo sigan iguales.
     *
     * @param idAutor ID de quien publica (el usuario activo)
     * @param texto   mensaje; no puede estar vacío ni superar {@value #MAX_CARACTERES} caracteres
     */
    public ResultadoPublicacion publicar(String idAutor, String texto) {
        return publicar(idAutor, texto, LocalDateTime.now());
    }

    /** Igual que {@link #publicar(String, String)} pero con fecha indicada (útil para probar el orden). */
    ResultadoPublicacion publicar(String idAutor, String texto, LocalDateTime fechaHora) {
        if (idAutor == null || idAutor.isBlank() || !contexto.getUsuarios().existeId(idAutor)) {
            return ResultadoPublicacion.error(ResultadoPublicacion.Tipo.SIN_SESION,
                    "Debe iniciar sesión para publicar.");
        }
        String mensaje = texto == null ? "" : texto.strip();
        if (mensaje.isEmpty()) {
            return ResultadoPublicacion.error(ResultadoPublicacion.Tipo.TEXTO_VACIO,
                    "Escriba un mensaje antes de publicar.");
        }
        if (mensaje.length() > MAX_CARACTERES) {
            return ResultadoPublicacion.error(ResultadoPublicacion.Tipo.TEXTO_MUY_LARGO,
                    "El mensaje es demasiado largo (" + mensaje.length() + " caracteres). "
                            + "El máximo es " + MAX_CARACTERES + ".");
        }

        ListaPublicaciones lista = contexto.getPublicaciones();
        Publicacion nueva = new Publicacion(lista.generarId(), idAutor, mensaje, fechaHora);
        lista.agregar(nueva);
        try {
            contexto.getGestorArchivos().guardarPublicaciones(lista);
        } catch (PersistenciaException e) {
            lista.eliminarPorId(nueva.getId());
            return ResultadoPublicacion.error(ResultadoPublicacion.Tipo.ERROR_PERSISTENCIA,
                    "No se pudo publicar porque los datos no se guardaron. " + e.getMessage());
        }
        return ResultadoPublicacion.exito("Tu mensaje se publicó.", nueva);
    }

    // ------------------------------------------------------------------ muro

    /**
     * <b>Muro general</b>: publicaciones del usuario y de sus amigos, de la más reciente
     * a la más antigua. Un usuario que no es amigo no aparece, aunque exista.
     *
     * <p>La amistad es en ambos sentidos: si A y B son amigos, A ve las publicaciones de B
     * y B ve las de A.
     */
    public ListaDinamica<Publicacion> obtenerMuro(String idUsuario) {
        return contexto.getPublicaciones().obtenerRecientesDe(servicioAmistades.obtenerIdsVisibles(idUsuario));
    }

    /** <b>Mi muro</b>: solo las publicaciones del usuario, de la más reciente a la más antigua. */
    public ListaDinamica<Publicacion> obtenerMuroPersonal(String idUsuario) {
        ListaDinamica<String> propio = new ListaDinamica<>();
        if (idUsuario != null) {
            propio.agregar(idUsuario);
        }
        return contexto.getPublicaciones().obtenerRecientesDe(propio);
    }

    // ------------------------------------------------------------------ autor

    /**
     * Resuelve el autor de una publicación con la lista de usuarios (Módulo 1).
     *
     * @return el usuario, o {@code null} si ya no está registrado
     */
    public Usuario obtenerAutor(Publicacion publicacion) {
        return publicacion == null ? null : contexto.getUsuarios().buscarPorId(publicacion.getIdAutor());
    }
}
