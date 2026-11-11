package socialuna.servicios;

import java.util.Objects;

import socialuna.modelo.Usuario;

/**
 * Un resultado de búsqueda: el usuario encontrado y su relación con el usuario activo.
 * Lo devuelve {@link ServicioAmistades#buscarConEstado(String, String)}.
 */
public final class UsuarioEncontrado {

    private final Usuario usuario;
    private final EstadoAmistad estado;

    public UsuarioEncontrado(Usuario usuario, EstadoAmistad estado) {
        this.usuario = Objects.requireNonNull(usuario, "El usuario no puede ser nulo.");
        this.estado = Objects.requireNonNull(estado, "El estado no puede ser nulo.");
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public EstadoAmistad getEstado() {
        return estado;
    }

    @Override
    public String toString() {
        return "UsuarioEncontrado{" + usuario.getUsuario() + ", " + estado + "}";
    }
}
