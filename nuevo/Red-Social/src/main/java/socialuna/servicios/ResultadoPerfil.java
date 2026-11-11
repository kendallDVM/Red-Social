package socialuna.servicios;

import socialuna.modelo.CampoPerfil;

/**
 * Resultado de guardar un perfil. Sigue el mismo estilo que {@link ResultadoAmistad}: la
 * interfaz consulta {@link #esExitoso()} y muestra {@link #getMensaje()}.
 */
public final class ResultadoPerfil {

    public enum Tipo {
        EXITO,
        /** No hay un usuario con sesión activa. */
        SIN_SESION,
        /** Se intentó guardar el perfil de otra persona. */
        NO_AUTORIZADO,
        /** Algún campo no cumple el largo o el formato; ver {@link #getCampo()}. */
        DATO_INVALIDO,
        /** No se pudo leer o guardar perfiles.json; no cambió nada. */
        ERROR_PERSISTENCIA
    }

    private final Tipo tipo;
    private final String mensaje;
    private final CampoPerfil campo;

    private ResultadoPerfil(Tipo tipo, String mensaje, CampoPerfil campo) {
        this.tipo = tipo;
        this.mensaje = mensaje;
        this.campo = campo;
    }

    static ResultadoPerfil exito(String mensaje) {
        return new ResultadoPerfil(Tipo.EXITO, mensaje, null);
    }

    static ResultadoPerfil error(Tipo tipo, String mensaje) {
        return error(tipo, mensaje, null);
    }

    static ResultadoPerfil error(Tipo tipo, String mensaje, CampoPerfil campo) {
        if (tipo == Tipo.EXITO) {
            throw new IllegalArgumentException("Un error no puede tener tipo EXITO.");
        }
        return new ResultadoPerfil(tipo, mensaje, campo);
    }

    public boolean esExitoso() {
        return tipo == Tipo.EXITO;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public String getMensaje() {
        return mensaje;
    }

    /** Campo con el error (solo en {@link Tipo#DATO_INVALIDO}); {@code null} en los demás casos. */
    public CampoPerfil getCampo() {
        return campo;
    }

    @Override
    public String toString() {
        return "ResultadoPerfil{" + tipo + ", '" + mensaje + "'}";
    }
}
