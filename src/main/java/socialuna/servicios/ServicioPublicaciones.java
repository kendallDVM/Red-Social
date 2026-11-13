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
 * archivo {@code publicaciones.json} se escribe al publicar, al dar "me gusta" y al eliminar.
 *
 * <p><b>Datos compartidos entre instancias:</b> antes de publicar se releen las publicaciones
 * del disco ({@link ContextoAplicacion#sincronizarPublicaciones()}). Así, si el programa está
 * abierto dos veces, cada instancia ve lo que publicó la otra, no repite IDs y no borra
 * publicaciones ajenas al guardar.
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

        try {
            contexto.sincronizarPublicaciones(); // primero lo que publicó otra instancia
        } catch (PersistenciaException e) {
            return ResultadoPublicacion.error(ResultadoPublicacion.Tipo.ERROR_PERSISTENCIA,
                    "No se pudieron leer los datos actualizados; no se publicó el mensaje. " + e.getMessage());
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
     * Relee del disco las publicaciones, los usuarios y las amistades para ver los cambios
     * hechos por otra instancia del programa. El muro lo llama al refrescarse.
     *
     * @return {@code true} si se pudo leer todo; {@code false} si algún archivo no se pudo
     *         leer (en ese caso se sigue con lo que hay en memoria)
     */
    public boolean actualizarDesdeDisco() {
        boolean todoBien = servicioAmistades.actualizarDesdeDisco();
        try {
            contexto.sincronizarPublicaciones();
        } catch (PersistenciaException e) {
            todoBien = false;
        }
        return todoBien;
    }

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

    // ------------------------------------------------------------------ me gusta

    /**
     * Da "me gusta" a una publicación, o lo quita si ya se lo había dado, y lo guarda en
     * {@code publicaciones.json} en ese momento.
     *
     * <p>Solo se puede dar "me gusta" a las publicaciones que el usuario ve en su muro
     * general: las propias y las de sus amigos. Antes de cambiar se releen del disco las
     * publicaciones y las amistades, así se valida contra lo último que guardó cualquier
     * instancia del programa. Si el guardado falla, el cambio se deshace en memoria.
     *
     * @param idUsuario     quien da el "me gusta" (el usuario activo)
     * @param idPublicacion publicación elegida
     * @return éxito (la publicación queda en {@link ResultadoPublicacion#getPublicacion()}) o el motivo del error
     */
    public ResultadoPublicacion alternarMeGusta(String idUsuario, String idPublicacion) {
        if (idUsuario == null || idUsuario.isBlank() || !contexto.getUsuarios().existeId(idUsuario)) {
            return ResultadoPublicacion.error(ResultadoPublicacion.Tipo.SIN_SESION,
                    "Debe iniciar sesión para dar me gusta.");
        }
        servicioAmistades.actualizarDesdeDisco(); // si falla se sigue con lo que hay en memoria
        try {
            contexto.sincronizarPublicaciones();
        } catch (PersistenciaException e) {
            return ResultadoPublicacion.error(ResultadoPublicacion.Tipo.ERROR_PERSISTENCIA,
                    "No se pudieron leer los datos actualizados; no se cambió nada. " + e.getMessage());
        }

        ListaPublicaciones lista = contexto.getPublicaciones();
        Publicacion publicacion = lista.buscarPorId(idPublicacion);
        if (publicacion == null || publicacion.isEliminada()) {
            return ResultadoPublicacion.error(ResultadoPublicacion.Tipo.PUBLICACION_NO_EXISTE,
                    "La publicación ya no existe.");
        }
        if (!servicioAmistades.obtenerIdsVisibles(idUsuario).contiene(publicacion.getIdAutor())) {
            return ResultadoPublicacion.error(ResultadoPublicacion.Tipo.NO_PERMITIDO,
                    "Solo puedes dar me gusta a tus publicaciones y a las de tus amigos.");
        }

        boolean daMeGusta = !publicacion.leGustaA(idUsuario);
        cambiarMeGusta(publicacion, idUsuario, daMeGusta);
        try {
            contexto.getGestorArchivos().guardarPublicaciones(lista);
        } catch (PersistenciaException e) {
            cambiarMeGusta(publicacion, idUsuario, !daMeGusta);
            return ResultadoPublicacion.error(ResultadoPublicacion.Tipo.ERROR_PERSISTENCIA,
                    "No se pudo guardar el me gusta porque los datos no se guardaron. " + e.getMessage());
        }
        return ResultadoPublicacion.exito(daMeGusta ? "Te gusta esta publicación." : "Quitaste tu me gusta.",
                publicacion);
    }

    // ------------------------------------------------------------------ eliminar

    /**
     * Elimina una publicación del usuario y lo guarda en {@code publicaciones.json} en ese
     * momento. Deja de verse en todos los muros (el del autor y el de sus amigos).
     *
     * <p>Solo el autor puede eliminar sus publicaciones. La publicación se <b>marca</b> como
     * eliminada en vez de borrarse del archivo, para que otra instancia del programa abierta al
     * mismo tiempo se entere y no la vuelva a agregar al sincronizar. Antes de cambiar se
     * releen las publicaciones del disco. Si el guardado falla, el cambio se deshace en memoria.
     *
     * @param idUsuario     quien elimina (el usuario activo)
     * @param idPublicacion publicación elegida
     */
    public ResultadoPublicacion eliminarPublicacion(String idUsuario, String idPublicacion) {
        if (idUsuario == null || idUsuario.isBlank() || !contexto.getUsuarios().existeId(idUsuario)) {
            return ResultadoPublicacion.error(ResultadoPublicacion.Tipo.SIN_SESION,
                    "Debe iniciar sesión para eliminar publicaciones.");
        }
        try {
            contexto.sincronizarPublicaciones();
        } catch (PersistenciaException e) {
            return ResultadoPublicacion.error(ResultadoPublicacion.Tipo.ERROR_PERSISTENCIA,
                    "No se pudieron leer los datos actualizados; no se eliminó nada. " + e.getMessage());
        }

        ListaPublicaciones lista = contexto.getPublicaciones();
        Publicacion publicacion = lista.buscarPorId(idPublicacion);
        if (publicacion == null || publicacion.isEliminada()) {
            return ResultadoPublicacion.error(ResultadoPublicacion.Tipo.PUBLICACION_NO_EXISTE,
                    "La publicación ya no existe.");
        }
        if (!publicacion.getIdAutor().equals(idUsuario)) {
            return ResultadoPublicacion.error(ResultadoPublicacion.Tipo.NO_PERMITIDO,
                    "Solo puedes eliminar tus propias publicaciones.");
        }

        publicacion.setEliminada(true);
        try {
            contexto.getGestorArchivos().guardarPublicaciones(lista);
        } catch (PersistenciaException e) {
            publicacion.setEliminada(false);
            return ResultadoPublicacion.error(ResultadoPublicacion.Tipo.ERROR_PERSISTENCIA,
                    "No se pudo eliminar la publicación porque los datos no se guardaron. " + e.getMessage());
        }
        return ResultadoPublicacion.exito("Tu publicación se eliminó.", publicacion);
    }

    private static void cambiarMeGusta(Publicacion publicacion, String idUsuario, boolean darMeGusta) {
        if (darMeGusta) {
            publicacion.agregarMeGusta(idUsuario);
        } else {
            publicacion.quitarMeGusta(idUsuario);
        }
    }

    /**
     * Nombres de los usuarios a quienes les gusta la publicación, en el orden en que dieron
     * "me gusta". Se omiten los usuarios que ya no existen.
     */
    public ListaDinamica<String> obtenerNombresMeGusta(Publicacion publicacion) {
        ListaDinamica<String> nombres = new ListaDinamica<>();
        if (publicacion == null) {
            return nombres;
        }
        for (String id : publicacion.getMeGusta()) {
            Usuario usuario = contexto.getUsuarios().buscarPorId(id);
            if (usuario != null) {
                nombres.agregar(usuario.getNombre());
            }
        }
        return nombres;
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
