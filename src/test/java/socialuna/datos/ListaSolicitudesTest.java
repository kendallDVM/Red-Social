package socialuna.datos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import socialuna.modelo.SolicitudAmistad;

class ListaSolicitudesTest {

    @Test
    void agregarYConsultarEsDirigido() {
        ListaSolicitudes lista = new ListaSolicitudes();

        assertTrue(lista.agregar("1", "2"));

        assertTrue(lista.existe("1", "2"));
        assertFalse(lista.existe("2", "1"), "la solicitud tiene sentido");
        assertTrue(lista.existeEntre("2", "1"));
        assertEquals(1, lista.tamano());
    }

    @Test
    void noSeAceptanDuplicadasNiCruzadas() {
        ListaSolicitudes lista = new ListaSolicitudes();
        lista.agregar("1", "2");

        assertFalse(lista.agregar("1", "2"), "duplicada");
        assertFalse(lista.agregar("2", "1"), "cruzada");
        assertEquals(1, lista.tamano());
    }

    @Test
    void noSePuedeEnviarASiMismoNiSinIds() {
        ListaSolicitudes lista = new ListaSolicitudes();

        assertThrows(IllegalArgumentException.class, () -> lista.agregar("1", "1"));
        assertThrows(IllegalArgumentException.class, () -> lista.agregar(null, "1"));
        assertThrows(IllegalArgumentException.class, () -> lista.agregar("1", " "));
        assertTrue(lista.estaVacia());
    }

    @Test
    void eliminarYEliminarEntre() {
        ListaSolicitudes lista = new ListaSolicitudes();
        lista.agregar("1", "2");
        lista.agregar("3", "1");

        assertFalse(lista.eliminar("2", "1"), "sentido contrario: no existe");
        assertTrue(lista.eliminar("1", "2"));
        assertTrue(lista.eliminarEntre("1", "3"));
        assertTrue(lista.estaVacia());
    }

    @Test
    void remitentesYDestinatariosEnOrdenDeEnvio() {
        ListaSolicitudes lista = new ListaSolicitudes();
        lista.agregar("2", "1");
        lista.agregar("1", "4");
        lista.agregar("3", "1");

        ListaDinamica<String> remitentes = lista.obtenerRemitentesPara("1");
        assertEquals(2, remitentes.tamano());
        assertEquals("2", remitentes.obtener(0));
        assertEquals("3", remitentes.obtener(1));

        ListaDinamica<String> destinatarios = lista.obtenerDestinatariosDe("1");
        assertEquals(1, destinatarios.tamano());
        assertEquals("4", destinatarios.obtener(0));
    }

    @Test
    void obtenerTodasDevuelveCopia() {
        ListaSolicitudes lista = new ListaSolicitudes();
        lista.agregar("1", "2");

        lista.obtenerTodas().limpiar();

        assertEquals(1, lista.tamano());
        assertEquals(new SolicitudAmistad("1", "2"), lista.obtenerTodas().obtener(0));
    }
}
