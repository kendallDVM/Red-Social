package socialuna.servicios;

import static socialuna.UtilPruebas.hash;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import socialuna.modelo.Usuario;

class SesionTest {

    @AfterEach
    void limpiar() {
        Sesion.cerrar(); // la sesión es estática: se limpia para no afectar otras pruebas
    }

    @Test
    void inicialmenteNoHaySesion() {
        Sesion.cerrar();
        assertFalse(Sesion.haySesionActiva());
        assertNull(Sesion.getUsuarioActivo());
    }

    @Test
    void establecerYConsultarUsuarioActivo() {
        Usuario ana = new Usuario("1", "Ana", "ana", hash("x"));
        Sesion.iniciar(ana);
        assertTrue(Sesion.haySesionActiva());
        assertSame(ana, Sesion.getUsuarioActivo());
    }

    @Test
    void cerrarLimpiaLaSesion() {
        Sesion.iniciar(new Usuario("1", "Ana", "ana", hash("x")));
        Sesion.cerrar();
        assertFalse(Sesion.haySesionActiva());
        assertNull(Sesion.getUsuarioActivo());
    }

    @Test
    void noSePuedeIniciarConNulo() {
        assertThrows(IllegalArgumentException.class, () -> Sesion.iniciar(null));
        assertFalse(Sesion.haySesionActiva());
    }
}
