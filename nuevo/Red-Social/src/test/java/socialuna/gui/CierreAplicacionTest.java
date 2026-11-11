package socialuna.gui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static socialuna.UtilPruebas.hash;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import socialuna.datos.GestorArchivos;
import socialuna.datos.ListaUsuarios;
import socialuna.modelo.Usuario;
import socialuna.servicios.ContextoAplicacion;
import socialuna.servicios.Sesion;

/**
 * Cierre de la aplicación: guarda los datos y limpia la sesión. Se prueba
 * {@code prepararSalida} (todo excepto {@code System.exit}). No abre ventanas.
 */
class CierreAplicacionTest {

    @TempDir
    Path carpeta;

    @AfterEach
    void limpiar() {
        Sesion.cerrar();
    }

    @Test
    void alSalirSeGuardanLosDatosYSeLimpiaLaSesion() throws Exception {
        ContextoAplicacion contexto = new ContextoAplicacion(new GestorArchivos(carpeta));
        Usuario ana = new Usuario("1", "Ana", "ana", hash("c"));
        contexto.getUsuarios().agregar(ana);
        Sesion.iniciar(ana);

        assertTrue(new CierreAplicacion(contexto).prepararSalida(null));

        assertFalse(Sesion.haySesionActiva());
        ListaUsuarios enDisco = new ListaUsuarios();
        new GestorArchivos(carpeta).cargarUsuarios(enDisco);
        assertNotNull(enDisco.buscarPorUsuario("ana")); // se guardó al cerrar la aplicación
    }
}
