package socialuna.servicios;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

import socialuna.datos.ListaDinamica;
import socialuna.datos.ListaUsuarios;
import socialuna.datos.PersistenciaException;
import socialuna.datos.RedAmistades;
import socialuna.modelo.Usuario;

/**
 * Lógica del Módulo 2: búsqueda de usuarios y manejo de amistades.
 *
 * <p>Las ventanas solo recogen los datos y muestran el resultado; todas las reglas están
 * aquí, así se pueden probar sin interfaz gráfica. Todo se consulta en memoria
 * ({@link ListaUsuarios} y {@link RedAmistades}); el archivo solo se escribe al cambiar
 * una amistad.
 *
 * <p>Los métodos reciben el ID del usuario activo como parámetro. Las ventanas lo toman de
 * {@link Sesion#getUsuarioActivo()}.
 */
public class ServicioAmistades {

    private final ContextoAplicacion contexto;

    public ServicioAmistades(ContextoAplicacion contexto) {
        this.contexto = Objects.requireNonNull(contexto, "El contexto no puede ser nulo.");
    }

    // ------------------------------------------------------------------ búsqueda

    /**
     * Busca usuarios cuyo nombre o nombre de usuario contenga el texto, sin distinguir
     * mayúsculas ni tildes ("sofia" encuentra a "Sofía"). Excluye al usuario activo.
     *
     * @param idUsuarioActivo ID de quien busca (no aparece en los resultados)
     * @param texto           texto a buscar; si está vacío se devuelve una lista vacía
     *                        (la ventana debe avisar antes de llamar)
     */
    public ListaDinamica<Usuario> buscarUsuarios(String idUsuarioActivo, String texto) {
        ListaDinamica<Usuario> resultados = new ListaDinamica<>();
        String buscado = normalizar(texto);
        if (buscado.isEmpty()) {
            return resultados;
        }
        for (Usuario usuario : contexto.getUsuarios().obtenerTodos()) {
            if (usuario.getId().equals(idUsuarioActivo)) {
                continue;
            }
            if (normalizar(usuario.getNombre()).contains(buscado)
                    || normalizar(usuario.getUsuario()).contains(buscado)) {
                resultados.agregar(usuario);
            }
        }
        return resultados;
    }

    // ------------------------------------------------------------------ amistades

    /**
     * Agrega una amistad entre el usuario activo y otro usuario, y la guarda en
     * {@code amistades.json} en ese momento.
     *
     * <p>Si el guardado falla, la amistad se quita de memoria para que memoria y archivo
     * sigan iguales.
     */
    public ResultadoAmistad agregarAmigo(String idUsuarioActivo, String idAmigo) {
        ResultadoAmistad error = validar(idUsuarioActivo, idAmigo);
        if (error != null) {
            return error;
        }
        Usuario amigo = contexto.getUsuarios().buscarPorId(idAmigo);
        RedAmistades red = contexto.getAmistades();
        if (red.sonAmigos(idUsuarioActivo, idAmigo)) {
            return ResultadoAmistad.error(ResultadoAmistad.Tipo.YA_SON_AMIGOS,
                    "Ya eres amigo de " + amigo.getNombre() + ".");
        }

        red.agregarAmistad(idUsuarioActivo, idAmigo);
        try {
            contexto.getGestorArchivos().guardarAmistades(red);
        } catch (PersistenciaException e) {
            red.eliminarAmistad(idUsuarioActivo, idAmigo);
            return ResultadoAmistad.error(ResultadoAmistad.Tipo.ERROR_PERSISTENCIA,
                    "No se pudo agregar al amigo porque los datos no se guardaron. " + e.getMessage());
        }
        return ResultadoAmistad.exito(amigo.getNombre() + " ahora es tu amigo.", amigo);
    }

    /**
     * Elimina la amistad entre el usuario activo y otro usuario, y guarda el cambio.
     * Si el guardado falla, la amistad se restaura en memoria.
     */
    public ResultadoAmistad eliminarAmigo(String idUsuarioActivo, String idAmigo) {
        ResultadoAmistad error = validar(idUsuarioActivo, idAmigo);
        if (error != null) {
            return error;
        }
        Usuario amigo = contexto.getUsuarios().buscarPorId(idAmigo);
        RedAmistades red = contexto.getAmistades();
        if (!red.sonAmigos(idUsuarioActivo, idAmigo)) {
            return ResultadoAmistad.error(ResultadoAmistad.Tipo.NO_SON_AMIGOS,
                    amigo.getNombre() + " no está en tu lista de amigos.");
        }

        red.eliminarAmistad(idUsuarioActivo, idAmigo);
        try {
            contexto.getGestorArchivos().guardarAmistades(red);
        } catch (PersistenciaException e) {
            red.agregarAmistad(idUsuarioActivo, idAmigo);
            return ResultadoAmistad.error(ResultadoAmistad.Tipo.ERROR_PERSISTENCIA,
                    "No se pudo eliminar al amigo porque los datos no se guardaron. " + e.getMessage());
        }
        return ResultadoAmistad.exito(amigo.getNombre() + " ya no está en tu lista de amigos.", amigo);
    }

    /** Indica si dos usuarios son amigos. */
    public boolean sonAmigos(String idA, String idB) {
        return contexto.getAmistades().sonAmigos(idA, idB);
    }

    /**
     * Amigos del usuario como objetos {@link Usuario} (no solo IDs). Si algún ID del grafo
     * ya no existe entre los usuarios, se omite.
     */
    public ListaDinamica<Usuario> obtenerAmigos(String idUsuario) {
        ListaDinamica<Usuario> amigos = new ListaDinamica<>();
        ListaUsuarios usuarios = contexto.getUsuarios();
        for (String idAmigo : contexto.getAmistades().obtenerAmigos(idUsuario)) {
            Usuario amigo = usuarios.buscarPorId(idAmigo);
            if (amigo != null) {
                amigos.agregar(amigo);
            }
        }
        return amigos;
    }

    /**
     * IDs cuyas publicaciones puede ver el usuario en el muro general: él mismo y sus
     * amigos. <b>Lo usa el Módulo 3 (Mariano)</b> para filtrar el muro.
     */
    public ListaDinamica<String> obtenerIdsVisibles(String idUsuario) {
        ListaDinamica<String> visibles = new ListaDinamica<>();
        if (idUsuario == null) {
            return visibles;
        }
        visibles.agregar(idUsuario);
        for (String idAmigo : contexto.getAmistades().obtenerAmigos(idUsuario)) {
            visibles.agregar(idAmigo);
        }
        return visibles;
    }

    // ------------------------------------------------------------------ utilidades

    /** Validaciones comunes de agregar y eliminar; devuelve {@code null} si todo está bien. */
    private ResultadoAmistad validar(String idUsuarioActivo, String idAmigo) {
        if (idUsuarioActivo == null || idUsuarioActivo.isBlank()) {
            return ResultadoAmistad.error(ResultadoAmistad.Tipo.SIN_SESION,
                    "Debe iniciar sesión para administrar sus amigos.");
        }
        if (idAmigo == null || idAmigo.isBlank()) {
            return ResultadoAmistad.error(ResultadoAmistad.Tipo.SIN_SELECCION,
                    "Seleccione un usuario de la lista.");
        }
        if (idUsuarioActivo.equals(idAmigo)) {
            return ResultadoAmistad.error(ResultadoAmistad.Tipo.MISMO_USUARIO,
                    "No puedes agregarte a ti mismo como amigo.");
        }
        ListaUsuarios usuarios = contexto.getUsuarios();
        if (!usuarios.existeId(idUsuarioActivo) || !usuarios.existeId(idAmigo)) {
            return ResultadoAmistad.error(ResultadoAmistad.Tipo.USUARIO_NO_EXISTE,
                    "El usuario seleccionado no está registrado.");
        }
        return null;
    }

    /** Minúsculas y sin tildes, para comparar "Sofía" con "sofia". */
    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(texto.strip(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT);
    }
}
