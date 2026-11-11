package socialuna.datos;

import static socialuna.UtilPruebas.hash;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import socialuna.modelo.Usuario;

class ListaUsuariosTest {

    private ListaUsuarios lista;
    private Usuario ana;
    private Usuario luis;

    @BeforeEach
    void preparar() {
        lista = new ListaUsuarios();
        ana = new Usuario("100", "Ana Mora", "ana", hash("clave1"));
        luis = new Usuario("200", "Luis Solís", "luis", hash("clave2"));
        lista.agregar(ana);
        lista.agregar(luis);
    }

    @Test
    void agregarAumentaElTamano() {
        assertEquals(2, lista.tamano());
        assertFalse(lista.estaVacia());
    }

    @Test
    void buscarPorUsuarioUsaElIndice() {
        assertSame(ana, lista.buscarPorUsuario("ana"));
        assertSame(luis, lista.buscarPorUsuario("luis"));
        assertNull(lista.buscarPorUsuario("pedro"));
        assertNull(lista.buscarPorUsuario(null));
    }

    @Test
    void buscarPorId() {
        assertSame(ana, lista.buscarPorId("100"));
        assertSame(luis, lista.buscarPorId("200"));
        assertNull(lista.buscarPorId("999"));
        assertNull(lista.buscarPorId(null));
    }

    @Test
    void existeUsuarioYExisteId() {
        assertTrue(lista.existeUsuario("ana"));
        assertFalse(lista.existeUsuario("pedro"));
        assertTrue(lista.existeId("200"));
        assertFalse(lista.existeId("300"));
    }

    @Test
    void rechazaNombreDeUsuarioDuplicado() {
        Usuario repetido = new Usuario("300", "Otra Ana", "ana", hash("x"));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> lista.agregar(repetido));
        assertTrue(e.getMessage().contains("ya existe"));
        assertEquals(2, lista.tamano()); // no se agregó a la lista
        assertSame(ana, lista.buscarPorUsuario("ana")); // el índice no cambió
    }

    @Test
    void rechazaIdDuplicado() {
        Usuario repetido = new Usuario("100", "Pedro", "pedro", hash("x"));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> lista.agregar(repetido));
        assertTrue(e.getMessage().contains("ID"));
        assertEquals(2, lista.tamano());
        assertFalse(lista.existeUsuario("pedro")); // tampoco quedó en el índice
    }

    @Test
    void buscarPorUsuarioNoDistingueMayusculas() {
        assertSame(ana, lista.buscarPorUsuario("ANA"));
        assertSame(ana, lista.buscarPorUsuario("Ana"));
        assertTrue(lista.existeUsuario("LuIs"));
    }

    @Test
    void rechazaUsuarioDuplicadoConOtrasMayusculas() {
        Usuario repetido = new Usuario("300", "Otra Ana", "ANA", hash("x"));
        assertThrows(IllegalArgumentException.class, () -> lista.agregar(repetido));
        assertEquals(2, lista.tamano());
    }

    @Test
    void conservaElNombreDeUsuarioComoSeEscribio() {
        Usuario kendall = new Usuario("300", "Kendall", "Kendall", hash("x"));
        lista.agregar(kendall);
        assertEquals("Kendall", lista.buscarPorUsuario("kendall").getUsuario());
    }

    @Test
    void copiaEsIndependiente() {
        ListaUsuarios copia = lista.copia();
        copia.agregar(new Usuario("300", "Carla", "carla", hash("c")));
        assertEquals(3, copia.tamano());
        assertEquals(2, lista.tamano());
        assertFalse(lista.existeUsuario("carla"));
        assertSame(ana, copia.buscarPorUsuario("ana")); // comparte los objetos Usuario
    }

    @Test
    void rechazaNulo() {
        assertThrows(IllegalArgumentException.class, () -> lista.agregar(null));
    }

    @Test
    void obtenerTodosDevuelveUnaCopia() {
        ListaDinamica<Usuario> todos = lista.obtenerTodos();
        assertEquals(2, todos.tamano());
        todos.limpiar();
        assertEquals(2, lista.tamano()); // la lista interna no se vio afectada
    }

    @Test
    void limpiarVaciaListaEIndice() {
        lista.limpiar();
        assertTrue(lista.estaVacia());
        assertFalse(lista.existeUsuario("ana"));
        assertNull(lista.buscarPorId("100"));
    }
}
