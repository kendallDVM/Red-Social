package socialuna.datos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ConcurrentModificationException;

import org.junit.jupiter.api.Test;

class ListaDinamicaTest {

    @Test
    void listaNuevaEstaVacia() {
        ListaDinamica<String> lista = new ListaDinamica<>();
        assertTrue(lista.estaVacia());
        assertEquals(0, lista.tamano());
        assertEquals(ListaDinamica.CAPACIDAD_INICIAL, lista.capacidad());
    }

    @Test
    void agregarYObtenerConservanElOrden() {
        ListaDinamica<String> lista = new ListaDinamica<>();
        lista.agregar("a");
        lista.agregar("b");
        lista.agregar("c");
        assertEquals(3, lista.tamano());
        assertFalse(lista.estaVacia());
        assertEquals("a", lista.obtener(0));
        assertEquals("b", lista.obtener(1));
        assertEquals("c", lista.obtener(2));
    }

    @Test
    void creceCuandoSeLlenaLaCapacidad() {
        ListaDinamica<Integer> lista = new ListaDinamica<>(2);
        lista.agregar(1);
        lista.agregar(2);
        assertEquals(2, lista.capacidad());
        lista.agregar(3); // fuerza el redimensionamiento
        assertEquals(4, lista.capacidad());
        for (int i = 4; i <= 100; i++) {
            lista.agregar(i);
        }
        assertEquals(100, lista.tamano());
        for (int i = 0; i < 100; i++) {
            assertEquals(i + 1, lista.obtener(i)); // no se perdió ningún elemento al crecer
        }
    }

    @Test
    void eliminarPorIndiceDesplazaLosSiguientes() {
        ListaDinamica<String> lista = new ListaDinamica<>();
        lista.agregar("a");
        lista.agregar("b");
        lista.agregar("c");
        assertEquals("b", lista.eliminar(1));
        assertEquals(2, lista.tamano());
        assertEquals("a", lista.obtener(0));
        assertEquals("c", lista.obtener(1));
    }

    @Test
    void eliminarElementoYBuscar() {
        ListaDinamica<String> lista = new ListaDinamica<>();
        lista.agregar("x");
        lista.agregar("y");
        assertTrue(lista.contiene("y"));
        assertEquals(1, lista.indiceDe("y"));
        assertTrue(lista.eliminarElemento("y"));
        assertFalse(lista.contiene("y"));
        assertFalse(lista.eliminarElemento("noExiste"));
        assertEquals(-1, lista.indiceDe("noExiste"));
    }

    @Test
    void insertarEnMedio() {
        ListaDinamica<String> lista = new ListaDinamica<>();
        lista.agregar("a");
        lista.agregar("c");
        lista.insertar(1, "b");
        assertEquals("[a, b, c]", lista.toString());
    }

    @Test
    void indicesFueraDeRangoLanzanExcepcion() {
        ListaDinamica<String> lista = new ListaDinamica<>();
        assertThrows(IndexOutOfBoundsException.class, () -> lista.obtener(0));
        lista.agregar("a");
        assertThrows(IndexOutOfBoundsException.class, () -> lista.obtener(1));
        assertThrows(IndexOutOfBoundsException.class, () -> lista.obtener(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> lista.eliminar(5));
    }

    @Test
    void recorridoConForEach() {
        ListaDinamica<Integer> lista = new ListaDinamica<>();
        for (int i = 1; i <= 5; i++) {
            lista.agregar(i);
        }
        int suma = 0;
        for (int n : lista) {
            suma += n;
        }
        assertEquals(15, suma);
    }

    @Test
    void modificarDuranteRecorridoSeDetecta() {
        ListaDinamica<Integer> lista = new ListaDinamica<>();
        lista.agregar(1);
        lista.agregar(2);
        assertThrows(ConcurrentModificationException.class, () -> {
            for (Integer n : lista) {
                lista.agregar(n);
            }
        });
    }

    @Test
    void limpiarYCopia() {
        ListaDinamica<String> lista = new ListaDinamica<>();
        lista.agregar("a");
        ListaDinamica<String> copia = lista.copia();
        lista.limpiar();
        assertTrue(lista.estaVacia());
        assertEquals(1, copia.tamano()); // la copia es independiente
    }

    @Test
    void capacidadInvalidaSeRechaza() {
        assertThrows(IllegalArgumentException.class, () -> new ListaDinamica<String>(0));
    }
}
