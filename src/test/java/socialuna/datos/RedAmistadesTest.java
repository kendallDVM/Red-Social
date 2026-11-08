package socialuna.datos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import socialuna.modelo.Amistad;

class RedAmistadesTest {

    private RedAmistades red;

    @BeforeEach
    void preparar() {
        red = new RedAmistades();
    }

    @Test
    void laAmistadEsBidireccional() {
        assertTrue(red.agregarAmistad("1", "2"));

        assertTrue(red.sonAmigos("1", "2"));
        assertTrue(red.sonAmigos("2", "1"));
        assertTrue(red.obtenerAmigos("1").contiene("2"));
        assertTrue(red.obtenerAmigos("2").contiene("1"));
    }

    @Test
    void agregarAmistadRegistraLosVertices() {
        red.agregarAmistad("1", "2");

        assertTrue(red.contieneUsuario("1"));
        assertTrue(red.contieneUsuario("2"));
        assertEquals(2, red.cantidadUsuarios());
    }

    @Test
    void noSeDuplicaLaAmistadEnNingunSentido() {
        assertTrue(red.agregarAmistad("1", "2"));
        assertFalse(red.agregarAmistad("1", "2"));
        assertFalse(red.agregarAmistad("2", "1"));

        assertEquals(1, red.cantidadAmigos("1"));
        assertEquals(1, red.cantidadAmigos("2"));
        assertEquals(1, red.cantidadAmistades());
    }

    @Test
    void noSePuedeSerAmigoDeSiMismo() {
        assertThrows(IllegalArgumentException.class, () -> red.agregarAmistad("1", "1"));
        assertFalse(red.contieneUsuario("1"));
    }

    @Test
    void idsVaciosONulosSeRechazan() {
        assertThrows(IllegalArgumentException.class, () -> red.agregarAmistad(null, "2"));
        assertThrows(IllegalArgumentException.class, () -> red.agregarAmistad("1", " "));
        assertEquals(0, red.cantidadUsuarios());
    }

    @Test
    void sonAmigosConUsuariosDesconocidosDevuelveFalse() {
        red.agregarAmistad("1", "2");

        assertFalse(red.sonAmigos("1", "3"));
        assertFalse(red.sonAmigos("9", "1"));
        assertFalse(red.sonAmigos(null, "1"));
        assertFalse(red.sonAmigos("1", null));
    }

    @Test
    void eliminarAmistadQuitaAmbosSentidos() {
        red.agregarAmistad("1", "2");
        red.agregarAmistad("1", "3");

        assertTrue(red.eliminarAmistad("2", "1"));

        assertFalse(red.sonAmigos("1", "2"));
        assertFalse(red.sonAmigos("2", "1"));
        assertTrue(red.sonAmigos("1", "3"));
        assertEquals(1, red.cantidadAmistades());
        assertFalse(red.eliminarAmistad("1", "2"), "eliminar dos veces no debe fallar");
    }

    @Test
    void obtenerAmigosDevuelveUnaCopia() {
        red.agregarAmistad("1", "2");

        red.obtenerAmigos("1").agregar("99");

        assertEquals(1, red.cantidadAmigos("1"));
        assertFalse(red.sonAmigos("1", "99"));
    }

    @Test
    void obtenerAmigosDeUsuarioDesconocidoEsListaVacia() {
        assertTrue(red.obtenerAmigos("404").estaVacia());
        assertEquals(0, red.cantidadAmigos("404"));
    }

    @Test
    void obtenerAmistadesDevuelveCadaAmistadUnaSolaVez() {
        red.agregarAmistad("1", "2");
        red.agregarAmistad("1", "3");
        red.agregarAmistad("3", "2");

        ListaDinamica<Amistad> amistades = red.obtenerAmistades();

        assertEquals(3, amistades.tamano());
        assertEquals(3, red.cantidadAmistades());
        assertTrue(amistades.contiene(new Amistad("1", "2")));
        assertTrue(amistades.contiene(new Amistad("3", "1")));
        assertTrue(amistades.contiene(new Amistad("2", "3")));
    }

    @Test
    void usuarioSinAmigosNoGeneraAmistades() {
        red.agregarUsuario("1");

        assertTrue(red.obtenerAmistades().estaVacia());
        assertEquals(0, red.cantidadAmistades());
    }

    @Test
    void limpiarVaciaElGrafo() {
        red.agregarAmistad("1", "2");

        red.limpiar();

        assertEquals(0, red.cantidadUsuarios());
        assertFalse(red.sonAmigos("1", "2"));
    }
}
