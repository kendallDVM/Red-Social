package socialuna.modelo;

import java.util.regex.Pattern;

/**
 * Campos opcionales del perfil de un usuario, en el orden en que se muestran.
 *
 * <p>Cada campo sabe su etiqueta, su sección, su largo máximo, su tipo (texto de una línea,
 * texto largo o enlace) y si es <b>público</b>. En esta primera versión todos los campos son
 * públicos (los ve cualquier usuario si el dueño los completó). Para agregar privacidad por
 * campo en el futuro basta con cambiar {@link #esPublico()} por un valor guardado en cada
 * perfil: el único lugar que filtra lo que ven los demás es
 * {@code ServicioPerfiles.verPerfil(...)}.
 */
public enum CampoPerfil {

    BIOGRAFIA("Biografía", Seccion.SOBRE_MI, Tipo.TEXTO_LARGO, 300),
    INTERESES("Intereses", Seccion.SOBRE_MI, Tipo.TEXTO, 150),
    UBICACION("Ubicación", Seccion.SOBRE_MI, Tipo.TEXTO, 80),
    CARRERA("Carrera", Seccion.ACADEMICO, Tipo.TEXTO, 100),
    SEDE("Universidad o sede", Seccion.ACADEMICO, Tipo.TEXTO, 100),
    INSTAGRAM("Instagram", Seccion.REDES, Tipo.ENLACE, 200),
    FACEBOOK("Facebook", Seccion.REDES, Tipo.ENLACE, 200),
    LINKEDIN("LinkedIn", Seccion.REDES, Tipo.ENLACE, 200),
    OTRA_RED("Otra red o sitio web", Seccion.REDES, Tipo.ENLACE, 200);

    /** Agrupación visual de los campos. */
    public enum Seccion {
        SOBRE_MI("Sobre mí"),
        ACADEMICO("Información académica"),
        REDES("Redes sociales");

        private final String titulo;

        Seccion(String titulo) {
            this.titulo = titulo;
        }

        public String getTitulo() {
            return titulo;
        }
    }

    /** Forma de escribir el valor. */
    public enum Tipo {
        /** Una sola línea de texto. */
        TEXTO,
        /** Texto de varias líneas (biografía). */
        TEXTO_LARGO,
        /** Enlace {@code https://...} o nombre de usuario de la red ({@code @usuario}). */
        ENLACE
    }

    /** {@code https://...} sin espacios, o un nombre de usuario como {@code @ana.mora}. */
    private static final Pattern FORMATO_ENLACE =
            Pattern.compile("^(https?://\\S+|@?[\\p{L}\\p{N}._-]+)$");

    private final String etiqueta;
    private final Seccion seccion;
    private final Tipo tipo;
    private final int largoMaximo;

    CampoPerfil(String etiqueta, Seccion seccion, Tipo tipo, int largoMaximo) {
        this.etiqueta = etiqueta;
        this.seccion = seccion;
        this.tipo = tipo;
        this.largoMaximo = largoMaximo;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public Seccion getSeccion() {
        return seccion;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public int getLargoMaximo() {
        return largoMaximo;
    }

    /** Si los demás usuarios pueden verlo (todos en esta versión; ver la descripción de la clase). */
    public boolean esPublico() {
        return true;
    }

    /**
     * Limpia un valor escrito por el usuario: quita espacios al inicio y al final; en los
     * campos de una línea reemplaza saltos de línea y tabulaciones por espacios; en la
     * biografía unifica los saltos de línea. Un valor vacío se convierte en {@code null}
     * (campo sin completar).
     */
    public String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = tipo == Tipo.TEXTO_LARGO
                ? valor.replace("\r\n", "\n").replace('\r', '\n').replace('\t', ' ')
                : valor.replaceAll("[\\r\\n\\t]+", " ");
        limpio = limpio.strip();
        return limpio.isEmpty() ? null : limpio;
    }

    /**
     * Verifica un valor <b>ya normalizado</b>.
     *
     * @return {@code null} si es válido (un valor {@code null} siempre lo es: el campo es
     *         opcional); si no, el mensaje para el usuario
     */
    public String validar(String valorNormalizado) {
        if (valorNormalizado == null) {
            return null;
        }
        if (valorNormalizado.length() > largoMaximo) {
            return etiqueta + ": máximo " + largoMaximo + " caracteres (tiene " + valorNormalizado.length() + ").";
        }
        if (tipo == Tipo.ENLACE && !FORMATO_ENLACE.matcher(valorNormalizado).matches()) {
            return etiqueta + ": escriba un enlace que empiece con https:// o solo su nombre de usuario"
                    + " (por ejemplo @ana.mora), sin espacios.";
        }
        return null;
    }
}
