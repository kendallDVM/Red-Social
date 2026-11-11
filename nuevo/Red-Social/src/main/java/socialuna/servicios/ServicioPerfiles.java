package socialuna.servicios;

import java.util.Objects;

import socialuna.datos.ListaDinamica;
import socialuna.datos.ListaPerfiles;
import socialuna.datos.PersistenciaException;
import socialuna.modelo.CampoPerfil;
import socialuna.modelo.Perfil;
import socialuna.modelo.Usuario;

/**
 * Lógica de los perfiles: consultar el propio o el de otra persona y editar el propio.
 *
 * <p>Las ventanas solo muestran y recogen datos; las reglas están aquí, así se pueden
 * probar sin interfaz gráfica.
 *
 * <ul>
 *   <li><b>Consultar</b> ({@link #verPerfil}) no cambia nada: no envía solicitudes ni toca
 *       amistades. Funciona igual sean amigos o no.</li>
 *   <li><b>Editar</b> ({@link #guardarPerfil}): cada usuario solo puede guardar su propio
 *       perfil y solo los campos opcionales de {@link CampoPerfil}. El nombre, el usuario,
 *       el ID y la contraseña no se modifican desde aquí.</li>
 *   <li><b>Varias instancias:</b> antes de consultar o guardar se relee
 *       {@code perfiles.json}; al guardar solo se escribe ese archivo.</li>
 * </ul>
 */
public class ServicioPerfiles {

    private final ContextoAplicacion contexto;

    public ServicioPerfiles(ContextoAplicacion contexto) {
        this.contexto = Objects.requireNonNull(contexto, "El contexto no puede ser nulo.");
    }

    /**
     * Arma la vista del perfil de {@code idUsuario} tal como la debe ver {@code idSolicitante}.
     * Si quien mira es otra persona, solo se incluyen los campos públicos completados y
     * nunca el ID de la cuenta. La contraseña y su hash no se incluyen nunca.
     *
     * @return la vista, o {@code null} si el usuario no existe
     */
    public VistaPerfil verPerfil(String idSolicitante, String idUsuario) {
        actualizarDesdeDisco();
        Usuario usuario = idUsuario == null ? null : contexto.getUsuarios().buscarPorId(idUsuario);
        if (usuario == null) {
            return null;
        }
        boolean propio = idUsuario.equals(idSolicitante);
        Perfil perfil = contexto.getPerfiles().buscar(idUsuario);

        ListaDinamica<VistaPerfil.Dato> datos = new ListaDinamica<>();
        if (perfil != null) {
            for (CampoPerfil campo : CampoPerfil.values()) {
                String valor = perfil.obtener(campo);
                if (valor != null && (propio || campo.esPublico())) {
                    datos.agregar(new VistaPerfil.Dato(campo, valor));
                }
            }
        }
        return new VistaPerfil(usuario.getNombre(), usuario.getUsuario(), propio ? usuario.getId() : null,
                propio, datos);
    }

    /**
     * Copia del perfil del usuario activo para llenar el formulario de edición (vacía si
     * todavía no completó nada). Modificar la copia no cambia nada hasta llamar a
     * {@link #guardarPerfil}.
     *
     * @return {@code null} si no hay sesión o el usuario no existe
     */
    public Perfil obtenerPerfilEditable(String idUsuarioActivo) {
        actualizarDesdeDisco();
        if (idUsuarioActivo == null || !contexto.getUsuarios().existeId(idUsuarioActivo)) {
            return null;
        }
        Perfil perfil = contexto.getPerfiles().buscar(idUsuarioActivo);
        return perfil == null ? new Perfil(idUsuarioActivo) : perfil.copia();
    }

    /**
     * Guarda el perfil del usuario activo en {@code perfiles.json} en ese momento.
     * Si el guardado falla, la memoria vuelve a como estaba.
     *
     * @param idUsuarioActivo quien tiene la sesión (de {@link Sesion#getUsuarioActivo()})
     * @param datos           perfil editado; debe pertenecer al usuario activo
     */
    public ResultadoPerfil guardarPerfil(String idUsuarioActivo, Perfil datos) {
        if (idUsuarioActivo == null || idUsuarioActivo.isBlank()) {
            return ResultadoPerfil.error(ResultadoPerfil.Tipo.SIN_SESION, "Debe iniciar sesión para editar su perfil.");
        }
        if (datos == null || !idUsuarioActivo.equals(datos.getIdUsuario())) {
            return ResultadoPerfil.error(ResultadoPerfil.Tipo.NO_AUTORIZADO, "Solo puede editar su propio perfil.");
        }
        Perfil nuevo = datos.copia(); // normaliza todos los campos
        for (CampoPerfil campo : CampoPerfil.values()) {
            String error = campo.validar(nuevo.obtener(campo));
            if (error != null) {
                return ResultadoPerfil.error(ResultadoPerfil.Tipo.DATO_INVALIDO, error, campo);
            }
        }
        try {
            contexto.sincronizarPerfiles();
        } catch (PersistenciaException e) {
            return ResultadoPerfil.error(ResultadoPerfil.Tipo.ERROR_PERSISTENCIA,
                    "No se pudieron leer los datos actualizados; no se guardó ningún cambio. " + e.getMessage());
        }
        if (!contexto.getUsuarios().existeId(idUsuarioActivo)) {
            return ResultadoPerfil.error(ResultadoPerfil.Tipo.SIN_SESION, "El usuario de la sesión no está registrado.");
        }

        ListaPerfiles perfiles = contexto.getPerfiles();
        Perfil anterior = perfiles.buscar(idUsuarioActivo);
        perfiles.guardar(nuevo);
        try {
            contexto.getGestorArchivos().guardarPerfiles(perfiles);
        } catch (PersistenciaException e) {
            if (anterior != null) {
                perfiles.guardar(anterior);
            } else {
                perfiles.eliminar(idUsuarioActivo);
            }
            return ResultadoPerfil.error(ResultadoPerfil.Tipo.ERROR_PERSISTENCIA,
                    "No se pudo guardar el perfil porque los datos no se guardaron. " + e.getMessage());
        }
        return ResultadoPerfil.exito("Tu perfil se guardó correctamente.");
    }

    /** Relee usuarios y perfiles del disco; si falla, se sigue con lo que hay en memoria. */
    private void actualizarDesdeDisco() {
        try {
            contexto.sincronizarPerfiles();
        } catch (PersistenciaException e) {
            // Se muestran los datos en memoria; guardar avisará del problema.
        }
    }
}
