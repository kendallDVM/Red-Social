package socialuna.servicios;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Objects;

import at.favre.lib.crypto.bcrypt.BCrypt;
import socialuna.datos.ListaUsuarios;
import socialuna.datos.PersistenciaException;
import socialuna.modelo.Usuario;

/**
 * Lógica del Módulo 1: registro, inicio de sesión y cierre de sesión.
 *
 * <p>Las ventanas solo recogen los datos y muestran el {@link ResultadoAutenticacion};
 * todas las reglas están aquí, así se pueden probar sin interfaz gráfica.
 *
 * <p><b>Contraseñas:</b>
 * <ul>
 *   <li>Se reciben como {@code char[]} (lo que entrega {@code JPasswordField.getPassword()}).
 *       Este servicio no las modifica; la ventana que llama debe borrarlas con
 *       {@code Arrays.fill(arreglo, '\0')} después de usarlas.</li>
 *   <li>Solo se guarda el hash BCrypt (costo {@value #COSTO_POR_DEFECTO}). La sal es
 *       aleatoria y va incluida en el propio hash.</li>
 *   <li>BCrypt solo procesa los primeros bytes de la contraseña. Para que dos contraseñas
 *       distintas no terminen generando el mismo hash, se rechazan las que superan
 *       {@value #MAX_BYTES_CONTRASENA} bytes en UTF-8.</li>
 * </ul>
 */
public class ServicioAutenticacion {

    /** Costo (factor de trabajo) de BCrypt: 2^12 iteraciones. */
    public static final int COSTO_POR_DEFECTO = 12;

    /** Límite de BCrypt (versión 2a): 72 bytes incluyendo el terminador nulo. */
    public static final int MAX_BYTES_CONTRASENA = 71;

    /** Mensaje único para cualquier fallo de credenciales: no revela si el usuario existe. */
    public static final String MENSAJE_CREDENCIALES_INVALIDAS = "Usuario o contraseña incorrectos.";

    private final ContextoAplicacion contexto;
    private final int costo;

    /**
     * Hash de una contraseña aleatoria. Si el usuario no existe, se verifica la
     * contraseña contra este hash para que la respuesta tarde lo mismo que con un
     * usuario real. Así no se puede deducir por el tiempo si una cuenta existe.
     */
    private final String hashFicticio;

    public ServicioAutenticacion(ContextoAplicacion contexto) {
        this(contexto, COSTO_POR_DEFECTO);
    }

    /**
     * @param costo factor de trabajo de BCrypt (4 a 31). Las pruebas usan un costo bajo
     *              para ser rápidas; la aplicación usa {@link #COSTO_POR_DEFECTO}.
     */
    public ServicioAutenticacion(ContextoAplicacion contexto, int costo) {
        if (costo < 4 || costo > 31) {
            throw new IllegalArgumentException("El costo de BCrypt debe estar entre 4 y 31.");
        }
        this.contexto = Objects.requireNonNull(contexto, "El contexto no puede ser nulo.");
        this.costo = costo;
        this.hashFicticio = generarHashFicticio(costo);
    }

    // ------------------------------------------------------------------ registro

    /**
     * Registra una cuenta nueva y la guarda en {@code usuarios.json}.
     *
     * <p>Orden de las validaciones: campos obligatorios → usuario sin espacios →
     * contraseñas coincidentes → largo de la contraseña → ID único → usuario único.
     *
     * <p><b>Consistencia:</b> primero se guarda en disco una copia de la lista con el
     * usuario nuevo, y solo si ese guardado funciona se agrega a la lista en memoria.
     * Si falla la escritura, la memoria y el archivo quedan exactamente como estaban.
     *
     * @param id           identificador escrito por el estudiante (se ignoran espacios al inicio y al final)
     * @param nombre       nombre completo (se ignoran espacios al inicio y al final)
     * @param usuario      nombre de usuario (se ignoran espacios al inicio y al final; no puede tener espacios internos)
     * @param contrasena   contraseña en texto plano (no se modifica el arreglo)
     * @param confirmacion confirmación de la contraseña (no se modifica el arreglo)
     */
    public ResultadoAutenticacion registrar(String id, String nombre, String usuario,
                                            char[] contrasena, char[] confirmacion) {
        String idLimpio = limpiar(id);
        String nombreLimpio = limpiar(nombre);
        String usuarioLimpio = limpiar(usuario);

        String faltantes = camposFaltantes(idLimpio, nombreLimpio, usuarioLimpio, contrasena, confirmacion);
        if (!faltantes.isEmpty()) {
            return ResultadoAutenticacion.error(ResultadoAutenticacion.Tipo.CAMPOS_VACIOS,
                    "Todos los campos son obligatorios. Complete: " + faltantes + ".");
        }
        if (!Usuario.esNombreUsuarioValido(usuarioLimpio)) {
            return ResultadoAutenticacion.error(ResultadoAutenticacion.Tipo.USUARIO_INVALIDO,
                    "El nombre de usuario no puede contener espacios.");
        }
        if (!Arrays.equals(contrasena, confirmacion)) {
            return ResultadoAutenticacion.error(ResultadoAutenticacion.Tipo.CONTRASENAS_NO_COINCIDEN,
                    "Las contraseñas no coinciden.");
        }
        if (bytesUtf8(contrasena) > MAX_BYTES_CONTRASENA) {
            return ResultadoAutenticacion.error(ResultadoAutenticacion.Tipo.CONTRASENA_INVALIDA,
                    "La contraseña es demasiado larga (máximo " + MAX_BYTES_CONTRASENA
                            + " caracteres; menos si usa tildes o símbolos especiales).");
        }

        ListaUsuarios usuarios = contexto.getUsuarios();
        if (usuarios.existeId(idLimpio)) {
            return ResultadoAutenticacion.error(ResultadoAutenticacion.Tipo.ID_DUPLICADO,
                    "El ID ya está registrado.");
        }
        if (usuarios.existeUsuario(usuarioLimpio)) {
            return ResultadoAutenticacion.error(ResultadoAutenticacion.Tipo.USUARIO_DUPLICADO,
                    "El nombre de usuario ya está en uso.");
        }

        String hash = BCrypt.withDefaults().hashToString(costo, contrasena);
        Usuario nuevo = new Usuario(idLimpio, nombreLimpio, usuarioLimpio, hash);

        // 1) Guardar en disco una copia que ya incluye al usuario nuevo.
        ListaUsuarios propuesta = usuarios.copia();
        propuesta.agregar(nuevo);
        try {
            contexto.getGestorArchivos().guardarUsuarios(propuesta);
        } catch (PersistenciaException e) {
            return ResultadoAutenticacion.error(ResultadoAutenticacion.Tipo.ERROR_PERSISTENCIA,
                    "No se pudo completar el registro porque los datos no se guardaron. " + e.getMessage());
        }
        // 2) Solo si se guardó, se agrega a la lista en memoria (no puede fallar: ya se validó).
        usuarios.agregar(nuevo);

        return ResultadoAutenticacion.exito("Cuenta creada correctamente. Ya puede iniciar sesión.", nuevo);
    }

    // ------------------------------------------------------------------ inicio de sesión

    /**
     * Valida las credenciales y, si son correctas, establece el usuario activo en {@link Sesion}.
     *
     * <p>Si el usuario no existe o la contraseña es incorrecta, devuelve el mismo
     * resultado ({@link ResultadoAutenticacion.Tipo#CREDENCIALES_INVALIDAS}) con el mismo
     * mensaje. Así no se revela cuál de los dos datos falló.
     *
     * @param usuario    nombre de usuario (sin distinguir mayúsculas; se ignoran espacios al inicio y al final)
     * @param contrasena contraseña en texto plano (no se modifica el arreglo)
     */
    public ResultadoAutenticacion iniciarSesion(String usuario, char[] contrasena) {
        String usuarioLimpio = limpiar(usuario);
        if (usuarioLimpio.isEmpty() || contrasena == null || contrasena.length == 0) {
            return ResultadoAutenticacion.error(ResultadoAutenticacion.Tipo.CAMPOS_VACIOS,
                    "Ingrese su usuario y su contraseña.");
        }

        Usuario encontrado = contexto.getUsuarios().buscarPorUsuario(usuarioLimpio);
        String hashAComparar = encontrado != null ? encontrado.getContrasenaHash() : hashFicticio;
        boolean contrasenaCorrecta = verificar(contrasena, hashAComparar);

        if (encontrado == null || !contrasenaCorrecta) {
            return ResultadoAutenticacion.error(ResultadoAutenticacion.Tipo.CREDENCIALES_INVALIDAS,
                    MENSAJE_CREDENCIALES_INVALIDAS);
        }

        Sesion.iniciar(encontrado);
        return ResultadoAutenticacion.exito("Bienvenido(a), " + encontrado.getNombre() + ".", encontrado);
    }

    // ------------------------------------------------------------------ cierre de sesión

    /**
     * Guarda todos los datos (mediante {@link ContextoAplicacion#guardarTodo()}, que
     * usa {@code GestorArchivos}) y cierra la sesión activa.
     *
     * <p>La sesión se cierra <b>aunque falle el guardado</b>: los datos siguen en memoria y
     * se vuelven a intentar guardar al cerrar la aplicación. En ese caso el resultado es
     * {@link ResultadoAutenticacion.Tipo#ERROR_PERSISTENCIA}, para que la interfaz avise.
     */
    public ResultadoAutenticacion cerrarSesion() {
        Usuario anterior = Sesion.getUsuarioActivo();
        try {
            contexto.guardarTodo();
        } catch (PersistenciaException e) {
            Sesion.cerrar();
            return ResultadoAutenticacion.error(ResultadoAutenticacion.Tipo.ERROR_PERSISTENCIA,
                    "La sesión se cerró, pero no se pudieron guardar los datos. " + e.getMessage());
        }
        Sesion.cerrar();
        return ResultadoAutenticacion.exito("Sesión cerrada correctamente.", anterior);
    }

    // ------------------------------------------------------------------ utilidades

    /** Verifica una contraseña contra un hash BCrypt. Nunca lanza excepciones. */
    private static boolean verificar(char[] contrasena, String hash) {
        if (bytesUtf8(contrasena) > MAX_BYTES_CONTRASENA) {
            return false; // nunca pudo registrarse una contraseña así
        }
        try {
            return BCrypt.verifyer().verify(contrasena, hash).verified;
        } catch (RuntimeException e) {
            return false; // hash dañado o entrada inválida: se trata como credencial incorrecta
        }
    }

    private static String generarHashFicticio(int costo) {
        SecureRandom aleatorio = new SecureRandom();
        char[] aleatoria = new char[24];
        for (int i = 0; i < aleatoria.length; i++) {
            aleatoria[i] = (char) ('a' + aleatorio.nextInt(26));
        }
        String hash = BCrypt.withDefaults().hashToString(costo, aleatoria);
        Arrays.fill(aleatoria, '\0');
        return hash;
    }

    /** Tamaño en bytes UTF-8 de la contraseña, sin crear un {@code String} con ella. */
    private static int bytesUtf8(char[] contrasena) {
        ByteBuffer bytes = StandardCharsets.UTF_8.encode(CharBuffer.wrap(contrasena));
        int tamano = bytes.remaining();
        if (bytes.hasArray()) {
            Arrays.fill(bytes.array(), (byte) 0); // borrar la copia temporal
        }
        return tamano;
    }

    private static String limpiar(String texto) {
        return texto == null ? "" : texto.strip();
    }

    private static boolean contrasenaVacia(char[] contrasena) {
        if (contrasena == null) {
            return true;
        }
        for (char c : contrasena) {
            if (!Character.isWhitespace(c)) {
                return false;
            }
        }
        return true; // vacía o solo espacios
    }

    private static String camposFaltantes(String id, String nombre, String usuario,
                                          char[] contrasena, char[] confirmacion) {
        StringBuilder faltantes = new StringBuilder();
        agregarSi(faltantes, id.isEmpty(), "ID");
        agregarSi(faltantes, nombre.isEmpty(), "Nombre");
        agregarSi(faltantes, usuario.isEmpty(), "Usuario");
        agregarSi(faltantes, contrasenaVacia(contrasena), "Contraseña");
        agregarSi(faltantes, contrasenaVacia(confirmacion), "Confirmar contraseña");
        return faltantes.toString();
    }

    private static void agregarSi(StringBuilder sb, boolean condicion, String campo) {
        if (condicion) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(campo);
        }
    }
}
