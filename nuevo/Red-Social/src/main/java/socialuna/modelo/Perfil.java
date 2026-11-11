package socialuna.modelo;

/**
 * Información opcional del perfil de un usuario (biografía, carrera, redes sociales...).
 *
 * <p><b>Separada de {@link Usuario} a propósito:</b> {@code Usuario} es el registro de la
 * cuenta (ID, nombre, usuario y hash de la contraseña) y se guarda en {@code usuarios.json};
 * el perfil solo tiene datos que el estudiante decide mostrar y se guarda en
 * {@code perfiles.json}, enlazado por {@link #getIdUsuario()}. Así:
 * <ul>
 *   <li>editar el perfil nunca toca la contraseña, el ID ni el nombre de usuario;</li>
 *   <li>los usuarios registrados antes de existir los perfiles siguen funcionando: si no
 *       tienen registro en {@code perfiles.json}, su perfil simplemente está vacío;</li>
 *   <li>lo que se muestra a otros usuarios se arma sin pasar nunca por el hash.</li>
 * </ul>
 *
 * <p>Todos los campos son opcionales. Un campo sin completar vale {@code null} (Gson no lo
 * escribe en el JSON). Los valores siempre se guardan normalizados mediante
 * {@link #establecer(CampoPerfil, String)}.
 */
public class Perfil {

    private String idUsuario;
    private String biografia;
    private String intereses;
    private String ubicacion;
    private String carrera;
    private String sede;
    private String instagram;
    private String facebook;
    private String linkedin;
    private String otraRed;

    /** Constructor vacío requerido por Gson. */
    private Perfil() {
    }

    /**
     * Crea un perfil vacío para el usuario.
     *
     * @throws IllegalArgumentException si falta el ID del usuario
     */
    public Perfil(String idUsuario) {
        if (idUsuario == null || idUsuario.isBlank()) {
            throw new IllegalArgumentException("El perfil requiere el ID del usuario.");
        }
        this.idUsuario = idUsuario;
    }

    public String getIdUsuario() {
        return idUsuario;
    }

    /** Valor del campo, o {@code null} si no se completó. */
    public String obtener(CampoPerfil campo) {
        switch (campo) {
            case BIOGRAFIA: return biografia;
            case INTERESES: return intereses;
            case UBICACION: return ubicacion;
            case CARRERA: return carrera;
            case SEDE: return sede;
            case INSTAGRAM: return instagram;
            case FACEBOOK: return facebook;
            case LINKEDIN: return linkedin;
            case OTRA_RED: return otraRed;
            default: throw new IllegalArgumentException("Campo desconocido: " + campo);
        }
    }

    /**
     * Guarda el valor <b>normalizado</b> del campo (ver {@link CampoPerfil#normalizar}).
     * Un texto vacío deja el campo sin completar. No valida largo ni formato: para eso
     * está {@link #validar()}, que se llama antes de guardar.
     */
    public void establecer(CampoPerfil campo, String valor) {
        String v = campo.normalizar(valor);
        switch (campo) {
            case BIOGRAFIA: biografia = v; break;
            case INTERESES: intereses = v; break;
            case UBICACION: ubicacion = v; break;
            case CARRERA: carrera = v; break;
            case SEDE: sede = v; break;
            case INSTAGRAM: instagram = v; break;
            case FACEBOOK: facebook = v; break;
            case LINKEDIN: linkedin = v; break;
            case OTRA_RED: otraRed = v; break;
            default: throw new IllegalArgumentException("Campo desconocido: " + campo);
        }
    }

    /**
     * Verifica el ID y todos los campos (también sirve para los objetos que crea Gson).
     *
     * @throws IllegalArgumentException con un mensaje listo para mostrar
     */
    public void validar() {
        if (idUsuario == null || idUsuario.isBlank()) {
            throw new IllegalArgumentException("El perfil requiere el ID del usuario.");
        }
        for (CampoPerfil campo : CampoPerfil.values()) {
            String valor = obtener(campo);
            if (valor != null && !valor.equals(campo.normalizar(valor))) {
                throw new IllegalArgumentException(campo.getEtiqueta() + ": formato inválido.");
            }
            String error = campo.validar(valor);
            if (error != null) {
                throw new IllegalArgumentException(error);
            }
        }
    }

    /** Indica si ningún campo opcional está completado. */
    public boolean estaVacio() {
        for (CampoPerfil campo : CampoPerfil.values()) {
            if (obtener(campo) != null) {
                return false;
            }
        }
        return true;
    }

    /** Copia independiente (modificarla no altera este perfil). */
    public Perfil copia() {
        Perfil copia = new Perfil(idUsuario);
        for (CampoPerfil campo : CampoPerfil.values()) {
            copia.establecer(campo, obtener(campo));
        }
        return copia;
    }

    @Override
    public String toString() {
        return "Perfil{idUsuario='" + idUsuario + "', vacio=" + estaVacio() + "}";
    }
}
