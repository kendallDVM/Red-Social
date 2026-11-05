package socialuna.datos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static socialuna.UtilPruebas.hash;

import org.junit.jupiter.api.Test;

import socialuna.modelo.Usuario;

/** Comprueba que la lista dinámica y el diccionario usuario → Usuario siempre coinciden. */
class ListaUsuariosSincronizacionTest {

    private static final String HASH = hash("c");

    @Test
    void cadaUsuarioDeLaListaEstaEnElDiccionarioYViceversa() {
        ListaUsuarios lista = new ListaUsuarios();
        for (int i = 0; i < 25; i++) { // supera la capacidad inicial de la lista dinámica
            lista.agregar(new Usuario("ID" + i, "Nombre " + i, "Usuario" + i, HASH));
        }

        assertEquals(25, lista.tamano());
        for (Usuario u : lista.obtenerTodos()) {
            assertSame(u, lista.buscarPorUsuario(u.getUsuario().toLowerCase()));
            assertSame(u, lista.buscarPorId(u.getId()));
        }
    }

    @Test
    void unAgregadoRechazadoNoDejaRastroEnNingunaEstructura() {
        ListaUsuarios lista = new ListaUsuarios();
        lista.agregar(new Usuario("1", "Ana", "ana", HASH));

        assertThrows(IllegalArgumentException.class, () -> lista.agregar(new Usuario("1", "Otra", "otra", HASH)));
        assertThrows(IllegalArgumentException.class, () -> lista.agregar(new Usuario("2", "Otra", "ANA", HASH)));

        assertEquals(1, lista.tamano());
        assertFalse(lista.existeUsuario("otra"));   // el ID duplicado no entró al diccionario
        assertNull(lista.buscarPorId("2"));         // el usuario duplicado no entró a la lista
    }

    @Test
    void laCopiaYElReemplazoReconstruyenAmbasEstructuras() {
        ListaUsuarios original = new ListaUsuarios();
        original.agregar(new Usuario("1", "Ana", "Ana", HASH));
        original.agregar(new Usuario("2", "Luis", "luis", HASH));

        ListaUsuarios copia = original.copia();
        assertTrue(copia.existeUsuario("ANA"));
        assertSame(copia.buscarPorUsuario("luis"), copia.buscarPorId("2"));

        original.limpiar();
        assertNull(original.buscarPorUsuario("ana"));
        assertNull(original.buscarPorId("1"));
        assertEquals(2, copia.tamano()); // la copia es independiente
    }
}
