package socialuna.modelo;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Estudiante registrado en Social UNA.
 *
 * <p>Decisiones de diseño:
 * <ul>
 *   <li>{@code id} y {@code usuario} no tienen setter: son las claves con las que
 *       {@code ListaUsuarios} indexa al usuario. Cambiarlas desde fuera dejaría el
 *       índice desincronizado.</li>
 *   <li>El atributo {@code contrasena} conserva el nombre acordado por el equipo (y la
 *       clave del JSON), pero <b>solo guarda el hash BCrypt</b>, nunca el texto plano.
 *       {@link #validar()} rechaza cualquier valor que no tenga formato BCrypt, así que
 *       ni el constructor ni la carga desde JSON aceptan contraseñas en texto plano.</li>
 *   <li>La clase no depende de la librería BCrypt: generar y verificar el hash es
 *       responsabilidad de {@code socialuna.servicios.ServicioAutenticacion}.</li>
 *   <li>{@link #toString()} nunca incluye la contraseña ni su hash.</li>
 * </ul>
 *
 * <p>Nombre de usuario: se guarda tal como lo escribió el estudiante (por ejemplo,
 * {@code Kendall}), pero se compara sin distinguir mayúsculas de minúsculas mediante
 * {@link #normalizarNombreUsuario(String)}.
 */
public class Usuario {

    /**
     * Formato de un hash BCrypt: {@code $2a$}, {@code $2b$} o {@code $2y$}, costo de dos
     * dígitos y 53 caracteres (22 de sal + 31 de hash) en el alfabeto de BCrypt.
     */
    private static final Pattern FORMATO_BCRYPT = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    private String id;
    private String nombre;
    private String usuario;
    private String contrasena; // hash BCrypt

    /** Constructor vacío requerido por Gson para reconstruir objetos desde JSON. */
    private Usuario() {
    }

    /**
     * @param contrasenaHash hash BCrypt de la contraseña (no la contraseña en texto plano)
     * @throws IllegalArgumentException si algún dato no cumple las reglas básicas
     */
    public Usuario(String id, String nombre, String usuario, String contrasenaHash) {
        this.id = id;
        this.nombre = nombre;
        this.usuario = usuario;
        this.contrasena = contrasenaHash;
        validar();
    }

    /**
     * Verifica las reglas básicas del modelo. Se llama desde el constructor y también
     * desde {@code ListaUsuarios} para los objetos que Gson crea sin pasar por el constructor.
     *
     * @throws IllegalArgumentException con un mensaje que describe el problema
     *                                  (el mensaje nunca incluye la contraseña)
     */
    public void validar() {
        if (estaEnBlanco(id)) {
            throw new IllegalArgumentException("El ID es obligatorio.");
        }
        if (estaEnBlanco(nombre)) {
            throw new IllegalArgumentException("El nombre es obligatorio.");
        }
        if (estaEnBlanco(usuario)) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio.");
        }
        if (!esNombreUsuarioValido(usuario)) {
            throw new IllegalArgumentException("El nombre de usuario no puede contener espacios.");
        }
        if (!esHashBCrypt(contrasena)) {
            throw new IllegalArgumentException("La contraseña debe almacenarse como hash BCrypt.");
        }
    }

    /** Regla del equipo: el nombre de usuario no puede estar vacío ni contener espacios. */
    public static boolean esNombreUsuarioValido(String usuario) {
        if (usuario == null || usuario.isEmpty()) {
            return false;
        }
        for (int i = 0; i < usuario.length(); i++) {
            if (Character.isWhitespace(usuario.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Forma canónica de un nombre de usuario para compararlo sin distinguir mayúsculas
     * de minúsculas ({@code "Kendall"} → {@code "kendall"}). Devuelve {@code null} si recibe {@code null}.
     */
    public static String normalizarNombreUsuario(String usuario) {
        return usuario == null ? null : usuario.toLowerCase(Locale.ROOT);
    }

    /** Indica si el texto tiene el formato de un hash BCrypt. */
    public static boolean esHashBCrypt(String valor) {
        return valor != null && FORMATO_BCRYPT.matcher(valor).matches();
    }

    private static boolean estaEnBlanco(String texto) {
        return texto == null || texto.isBlank();
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        if (estaEnBlanco(nombre)) {
            throw new IllegalArgumentException("El nombre es obligatorio.");
        }
        this.nombre = nombre;
    }

    /** Nombre de usuario tal como se registró (conserva mayúsculas y minúsculas). */
    public String getUsuario() {
        return usuario;
    }

    /** Hash BCrypt de la contraseña. Lo usa el servicio de autenticación para verificarla. */
    public String getContrasenaHash() {
        return contrasena;
    }

    /**
     * Reemplaza el hash de la contraseña (por ejemplo, para un futuro cambio de contraseña).
     *
     * @throws IllegalArgumentException si el valor no es un hash BCrypt
     */
    public void setContrasenaHash(String contrasenaHash) {
        if (!esHashBCrypt(contrasenaHash)) {
            throw new IllegalArgumentException("La contraseña debe almacenarse como hash BCrypt.");
        }
        this.contrasena = contrasenaHash;
    }

    /** Dos usuarios son el mismo si tienen el mismo ID. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Usuario)) {
            return false;
        }
        return Objects.equals(id, ((Usuario) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Usuario{id='" + id + "', nombre='" + nombre + "', usuario='" + usuario + "'}";
    }
}
