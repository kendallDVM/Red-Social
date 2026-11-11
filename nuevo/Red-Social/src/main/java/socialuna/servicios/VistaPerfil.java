package socialuna.servicios;

import socialuna.datos.ListaDinamica;
import socialuna.modelo.CampoPerfil;

/**
 * Lo que la interfaz puede mostrar de un perfil. La arma {@link ServicioPerfiles#verPerfil}.
 *
 * <p><b>Privacidad:</b> esta clase no guarda el objeto {@code Usuario} ni el {@code Perfil}
 * completos, solo copias de los textos permitidos. Nunca contiene la contraseña ni su hash.
 * El ID de la cuenta solo se incluye cuando quien mira es el propio dueño
 * ({@link #getIdCuenta()} es {@code null} para los demás). Los campos sin completar no
 * aparecen en {@link #getDatos()}.
 */
public final class VistaPerfil {

    /** Un campo completado del perfil, listo para mostrar. */
    public static final class Dato {
        private final CampoPerfil campo;
        private final String valor;

        Dato(CampoPerfil campo, String valor) {
            this.campo = campo;
            this.valor = valor;
        }

        public CampoPerfil getCampo() {
            return campo;
        }

        public String getValor() {
            return valor;
        }
    }

    private final String nombre;
    private final String usuario;
    private final String idCuenta;
    private final boolean propio;
    private final ListaDinamica<Dato> datos;

    VistaPerfil(String nombre, String usuario, String idCuenta, boolean propio, ListaDinamica<Dato> datos) {
        this.nombre = nombre;
        this.usuario = usuario;
        this.idCuenta = idCuenta;
        this.propio = propio;
        this.datos = datos;
    }

    /** Nombre visible de la persona. */
    public String getNombre() {
        return nombre;
    }

    /** Nombre de usuario (sin {@code @}). */
    public String getUsuario() {
        return usuario;
    }

    /** ID de la cuenta; solo para el dueño del perfil ({@code null} si lo mira otra persona). */
    public String getIdCuenta() {
        return idCuenta;
    }

    /** Indica si quien mira es el dueño del perfil. */
    public boolean esPropio() {
        return propio;
    }

    /** Campos completados y visibles para quien mira, en el orden de {@link CampoPerfil}. */
    public ListaDinamica<Dato> getDatos() {
        return datos.copia();
    }

    /** Campos completados de una sección. */
    public ListaDinamica<Dato> getDatos(CampoPerfil.Seccion seccion) {
        ListaDinamica<Dato> resultado = new ListaDinamica<>();
        for (Dato d : datos) {
            if (d.getCampo().getSeccion() == seccion) {
                resultado.agregar(d);
            }
        }
        return resultado;
    }

    /** Indica si la persona completó al menos un campo visible. */
    public boolean tieneInformacion() {
        return !datos.estaVacia();
    }

    @Override
    public String toString() {
        return "VistaPerfil{@" + usuario + ", propio=" + propio + ", datos=" + datos.tamano() + "}";
    }
}
