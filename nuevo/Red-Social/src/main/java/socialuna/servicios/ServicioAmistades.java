package socialuna.servicios;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

import socialuna.datos.ListaDinamica;
import socialuna.datos.ListaSolicitudes;
import socialuna.datos.ListaUsuarios;
import socialuna.datos.PersistenciaException;
import socialuna.datos.RedAmistades;
import socialuna.modelo.Usuario;

/**
 * Lógica del Módulo 2: búsqueda de usuarios, solicitudes de amistad y amistades.
 *
 * <p>Las ventanas solo recogen los datos y muestran el resultado; todas las reglas están
 * aquí, así se pueden probar sin interfaz gráfica.
 *
 * <p><b>Flujo de amistad:</b> A envía una solicitud a B ({@link #enviarSolicitud}) →
 * queda pendiente (no son amigos) → B la acepta ({@link #aceptarSolicitud}, se crea la
 * amistad) o la rechaza ({@link #rechazarSolicitud}, no se crea nada). A puede cancelarla
 * mientras siga pendiente ({@link #cancelarSolicitud}).
 *
 * <p><b>Datos compartidos entre instancias:</b> antes de cada cambio se releen
 * usuarios, amistades y solicitudes del disco ({@link ContextoAplicacion#sincronizarRelaciones()}),
 * se valida contra ese estado actualizado y se guarda el archivo en ese momento. Así,
 * si el programa está abierto dos veces (un usuario en cada una), cada instancia ve lo
 * que hizo la otra al buscar, al actualizar una pestaña o al intentar un cambio.
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

    /**
     * Busca usuarios igual que {@link #buscarUsuarios} y además indica la relación de cada
     * uno con el usuario activo. Antes de buscar relee los datos del disco, para incluir
     * usuarios registrados y solicitudes enviadas desde otra instancia del programa.
     *
     * <p>A diferencia de {@link #buscarUsuarios}, <b>sí</b> incluye la propia cuenta del
     * usuario activo si coincide, con el estado {@link EstadoAmistad#PROPIA_CUENTA}; así la
     * interfaz puede explicar por qué no se puede agregar en lugar de decir "sin resultados".
     *
     * @return lista vacía si el texto está vacío o no hay coincidencias
     */
    public ListaDinamica<UsuarioEncontrado> buscarConEstado(String idUsuarioActivo, String texto) {
        ListaDinamica<UsuarioEncontrado> resultados = new ListaDinamica<>();
        String buscado = normalizar(texto);
        if (buscado.isEmpty()) {
            return resultados;
        }
        actualizarDesdeDisco();
        for (Usuario usuario : contexto.getUsuarios().obtenerTodos()) {
            if (normalizar(usuario.getNombre()).contains(buscado)
                    || normalizar(usuario.getUsuario()).contains(buscado)) {
                resultados.agregar(new UsuarioEncontrado(usuario, obtenerEstado(idUsuarioActivo, usuario.getId())));
            }
        }
        return resultados;
    }

    /**
     * Relación entre el usuario activo y otro usuario, según los datos en memoria (no relee
     * el disco; para eso está {@link #actualizarDesdeDisco()}).
     */
    public EstadoAmistad obtenerEstado(String idUsuarioActivo, String idOtro) {
        ListaUsuarios usuarios = contexto.getUsuarios();
        if (idOtro == null || !usuarios.existeId(idOtro)) {
            return EstadoAmistad.USUARIO_NO_EXISTE;
        }
        if (idOtro.equals(idUsuarioActivo)) {
            return EstadoAmistad.PROPIA_CUENTA;
        }
        if (contexto.getAmistades().sonAmigos(idUsuarioActivo, idOtro)) {
            return EstadoAmistad.AMIGOS;
        }
        ListaSolicitudes solicitudes = contexto.getSolicitudes();
        if (solicitudes.existe(idUsuarioActivo, idOtro)) {
            return EstadoAmistad.SOLICITUD_ENVIADA;
        }
        if (solicitudes.existe(idOtro, idUsuarioActivo)) {
            return EstadoAmistad.SOLICITUD_RECIBIDA;
        }
        return EstadoAmistad.SIN_RELACION;
    }

    /**
     * Relee usuarios, amistades y solicitudes del disco para ver los cambios hechos por
     * otra instancia del programa. Las pestañas lo llaman al refrescarse.
     *
     * @return {@code true} si se pudo leer; {@code false} si algún archivo no se pudo leer
     *         (en ese caso se sigue trabajando con lo que hay en memoria)
     */
    public boolean actualizarDesdeDisco() {
        try {
            contexto.sincronizarRelaciones();
            return true;
        } catch (PersistenciaException e) {
            return false;
        }
    }

    // ------------------------------------------------------------------ solicitudes de amistad

    /**
     * El usuario activo envía una solicitud de amistad. Queda <b>pendiente</b>: enviarla
     * no crea la amistad. Se guarda en {@code solicitudes.json} en ese momento.
     *
     * <p>No se permite si ya son amigos, si ya hay una solicitud pendiente en el mismo
     * sentido (duplicada) o si el otro usuario ya le había enviado una al usuario activo
     * (en ese caso debe aceptarla o rechazarla; así nunca quedan dos solicitudes cruzadas).
     */
    public ResultadoAmistad enviarSolicitud(String idUsuarioActivo, String idDestinatario) {
        ResultadoAmistad error = validarConDatosActuales(idUsuarioActivo, idDestinatario,
                "No puedes enviarte una solicitud de amistad a ti mismo.");
        if (error != null) {
            return error;
        }
        Usuario destinatario = contexto.getUsuarios().buscarPorId(idDestinatario);
        ListaSolicitudes solicitudes = contexto.getSolicitudes();
        if (contexto.getAmistades().sonAmigos(idUsuarioActivo, idDestinatario)) {
            return ResultadoAmistad.error(ResultadoAmistad.Tipo.YA_SON_AMIGOS,
                    "Ya eres amigo de " + destinatario.getNombre() + ".");
        }
        if (solicitudes.existe(idUsuarioActivo, idDestinatario)) {
            return ResultadoAmistad.error(ResultadoAmistad.Tipo.SOLICITUD_YA_ENVIADA,
                    "Ya le enviaste una solicitud a " + destinatario.getNombre() + ". Sigue pendiente.");
        }
        if (solicitudes.existe(idDestinatario, idUsuarioActivo)) {
            return ResultadoAmistad.error(ResultadoAmistad.Tipo.SOLICITUD_RECIBIDA_PENDIENTE,
                    destinatario.getNombre() + " ya te envió una solicitud de amistad. "
                            + "Puedes aceptarla o rechazarla.");
        }

        solicitudes.agregar(idUsuarioActivo, idDestinatario);
        try {
            contexto.getGestorArchivos().guardarSolicitudes(solicitudes);
        } catch (PersistenciaException e) {
            solicitudes.eliminar(idUsuarioActivo, idDestinatario);
            return ResultadoAmistad.error(ResultadoAmistad.Tipo.ERROR_PERSISTENCIA,
                    "No se pudo enviar la solicitud porque los datos no se guardaron. " + e.getMessage());
        }
        return ResultadoAmistad.exito("Solicitud enviada a " + destinatario.getNombre()
                + ". Serán amigos cuando la acepte.", destinatario);
    }

    /**
     * El usuario activo acepta la solicitud que le envió {@code idRemitente}: se crea la
     * amistad (en ambos sentidos) y la solicitud deja de estar pendiente.
     *
     * <p>Primero se guarda la amistad; si eso falla, nada cambia. Si después falla el
     * guardado de las solicitudes, la amistad ya quedó guardada y la solicitud sobrante se
     * descarta al volver a leer el archivo (no se aceptan solicitudes entre amigos).
     */
    public ResultadoAmistad aceptarSolicitud(String idUsuarioActivo, String idRemitente) {
        ResultadoAmistad error = validarConDatosActuales(idUsuarioActivo, idRemitente,
                "No puedes aceptar una solicitud tuya.");
        if (error != null) {
            return error;
        }
        Usuario remitente = contexto.getUsuarios().buscarPorId(idRemitente);
        RedAmistades red = contexto.getAmistades();
        ListaSolicitudes solicitudes = contexto.getSolicitudes();
        if (!solicitudes.existe(idRemitente, idUsuarioActivo)) {
            if (red.sonAmigos(idUsuarioActivo, idRemitente)) {
                return ResultadoAmistad.error(ResultadoAmistad.Tipo.YA_SON_AMIGOS,
                        "Ya eres amigo de " + remitente.getNombre() + ".");
            }
            return ResultadoAmistad.error(ResultadoAmistad.Tipo.SOLICITUD_NO_EXISTE,
                    remitente.getNombre() + " no tiene una solicitud pendiente contigo (pudo haberla cancelado).");
        }

        red.agregarAmistad(idRemitente, idUsuarioActivo);
        try {
            contexto.getGestorArchivos().guardarAmistades(red);
        } catch (PersistenciaException e) {
            red.eliminarAmistad(idRemitente, idUsuarioActivo);
            return ResultadoAmistad.error(ResultadoAmistad.Tipo.ERROR_PERSISTENCIA,
                    "No se pudo aceptar la solicitud porque los datos no se guardaron. " + e.getMessage());
        }
        solicitudes.eliminarEntre(idRemitente, idUsuarioActivo);
        guardarSolicitudesSiEsPosible();
        return ResultadoAmistad.exito("Aceptaste la solicitud. " + remitente.getNombre()
                + " ahora es tu amigo.", remitente);
    }

    /**
     * El usuario activo rechaza la solicitud que le envió {@code idRemitente}: la solicitud
     * se elimina y <b>no</b> se crea ninguna amistad. El remitente podrá enviar otra más adelante.
     */
    public ResultadoAmistad rechazarSolicitud(String idUsuarioActivo, String idRemitente) {
        ResultadoAmistad error = validarConDatosActuales(idUsuarioActivo, idRemitente,
                "No puedes rechazar una solicitud tuya.");
        if (error != null) {
            return error;
        }
        Usuario remitente = contexto.getUsuarios().buscarPorId(idRemitente);
        return quitarSolicitud(idRemitente, idUsuarioActivo, remitente,
                "Rechazaste la solicitud de " + remitente.getNombre() + ". No se agregó como amigo.",
                remitente.getNombre() + " no tiene una solicitud pendiente contigo (pudo haberla cancelado).");
    }

    /**
     * El usuario activo cancela la solicitud que le envió a {@code idDestinatario} mientras
     * sigue pendiente.
     */
    public ResultadoAmistad cancelarSolicitud(String idUsuarioActivo, String idDestinatario) {
        ResultadoAmistad error = validarConDatosActuales(idUsuarioActivo, idDestinatario,
                "No tienes solicitudes contigo mismo.");
        if (error != null) {
            return error;
        }
        Usuario destinatario = contexto.getUsuarios().buscarPorId(idDestinatario);
        return quitarSolicitud(idUsuarioActivo, idDestinatario, destinatario,
                "Cancelaste la solicitud enviada a " + destinatario.getNombre() + ".",
                "Ya no tienes una solicitud pendiente con " + destinatario.getNombre()
                        + " (pudo haberla aceptado o rechazado).");
    }

    /** Usuarios que le enviaron al usuario una solicitud pendiente (la más antigua primero). */
    public ListaDinamica<Usuario> obtenerSolicitudesRecibidas(String idUsuario) {
        return aUsuarios(contexto.getSolicitudes().obtenerRemitentesPara(idUsuario));
    }

    /** Usuarios a quienes el usuario les envió una solicitud que sigue pendiente. */
    public ListaDinamica<Usuario> obtenerSolicitudesEnviadas(String idUsuario) {
        return aUsuarios(contexto.getSolicitudes().obtenerDestinatariosDe(idUsuario));
    }

    // ------------------------------------------------------------------ amistades

    /**
     * Agrega una amistad <b>directamente</b> (sin solicitud) y la guarda en
     * {@code amistades.json} en ese momento.
     *
     * <p>La interfaz gráfica <b>no</b> lo usa: allí la amistad solo se crea al aceptar una
     * solicitud ({@link #aceptarSolicitud}). Se conserva por compatibilidad y para las
     * pruebas. Si había solicitudes pendientes entre ambos, se eliminan.
     *
     * <p>Si el guardado falla, la amistad se quita de memoria para que memoria y archivo
     * sigan iguales.
     */
    public ResultadoAmistad agregarAmigo(String idUsuarioActivo, String idAmigo) {
        ResultadoAmistad error = validarConDatosActuales(idUsuarioActivo, idAmigo,
                "No puedes agregarte a ti mismo como amigo.");
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
        if (contexto.getSolicitudes().eliminarEntre(idUsuarioActivo, idAmigo)) {
            guardarSolicitudesSiEsPosible();
        }
        return ResultadoAmistad.exito(amigo.getNombre() + " ahora es tu amigo.", amigo);
    }

    /**
     * Elimina la amistad entre el usuario activo y otro usuario, y guarda el cambio.
     * Si el guardado falla, la amistad se restaura en memoria.
     */
    public ResultadoAmistad eliminarAmigo(String idUsuarioActivo, String idAmigo) {
        ResultadoAmistad error = validarConDatosActuales(idUsuarioActivo, idAmigo,
                "No puedes eliminarte a ti mismo de tus amigos.");
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

    /**
     * Validaciones comunes de todas las operaciones que cambian amistades o solicitudes.
     * Entre las validaciones básicas y la de existencia se releen los datos del disco,
     * para validar contra lo último que guardó cualquier instancia del programa.
     *
     * @return {@code null} si todo está bien; si no, el error a mostrar
     */
    private ResultadoAmistad validarConDatosActuales(String idUsuarioActivo, String idOtro,
                                                     String mensajeMismoUsuario) {
        if (idUsuarioActivo == null || idUsuarioActivo.isBlank()) {
            return ResultadoAmistad.error(ResultadoAmistad.Tipo.SIN_SESION,
                    "Debe iniciar sesión para administrar sus amigos.");
        }
        if (idOtro == null || idOtro.isBlank()) {
            return ResultadoAmistad.error(ResultadoAmistad.Tipo.SIN_SELECCION,
                    "Seleccione un usuario de la lista.");
        }
        if (idUsuarioActivo.equals(idOtro)) {
            return ResultadoAmistad.error(ResultadoAmistad.Tipo.MISMO_USUARIO, mensajeMismoUsuario);
        }
        try {
            contexto.sincronizarRelaciones();
        } catch (PersistenciaException e) {
            return ResultadoAmistad.error(ResultadoAmistad.Tipo.ERROR_PERSISTENCIA,
                    "No se pudieron leer los datos actualizados; no se hizo ningún cambio. " + e.getMessage());
        }
        ListaUsuarios usuarios = contexto.getUsuarios();
        if (!usuarios.existeId(idUsuarioActivo) || !usuarios.existeId(idOtro)) {
            return ResultadoAmistad.error(ResultadoAmistad.Tipo.USUARIO_NO_EXISTE,
                    "El usuario seleccionado no está registrado.");
        }
        return null;
    }

    /**
     * Elimina la solicitud (remitente → destinatario) y guarda. Si el guardado falla, la
     * solicitud se restaura en memoria.
     */
    private ResultadoAmistad quitarSolicitud(String idRemitente, String idDestinatario, Usuario otro,
                                             String mensajeExito, String mensajeNoExiste) {
        ListaSolicitudes solicitudes = contexto.getSolicitudes();
        if (!solicitudes.eliminar(idRemitente, idDestinatario)) {
            return ResultadoAmistad.error(ResultadoAmistad.Tipo.SOLICITUD_NO_EXISTE, mensajeNoExiste);
        }
        try {
            contexto.getGestorArchivos().guardarSolicitudes(solicitudes);
        } catch (PersistenciaException e) {
            solicitudes.agregar(idRemitente, idDestinatario);
            return ResultadoAmistad.error(ResultadoAmistad.Tipo.ERROR_PERSISTENCIA,
                    "No se pudo actualizar la solicitud porque los datos no se guardaron. " + e.getMessage());
        }
        return ResultadoAmistad.exito(mensajeExito, otro);
    }

    /**
     * Guarda las solicitudes después de que la amistad ya quedó guardada. Si falla no se
     * deshace nada: la solicitud sobrante es entre amigos y se descarta al volver a leer.
     */
    private void guardarSolicitudesSiEsPosible() {
        try {
            contexto.getGestorArchivos().guardarSolicitudes(contexto.getSolicitudes());
        } catch (PersistenciaException e) {
            // Ver la descripción del método: no deja un estado contradictorio.
        }
    }

    /** Convierte IDs en objetos {@link Usuario}, omitiendo los que ya no existen. */
    private ListaDinamica<Usuario> aUsuarios(ListaDinamica<String> ids) {
        ListaDinamica<Usuario> resultado = new ListaDinamica<>();
        ListaUsuarios usuarios = contexto.getUsuarios();
        for (String id : ids) {
            Usuario usuario = usuarios.buscarPorId(id);
            if (usuario != null) {
                resultado.agregar(usuario);
            }
        }
        return resultado;
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
