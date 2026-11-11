package socialuna.datos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import socialuna.modelo.Publicacion;

/** Consultas del muro sobre la lista de publicaciones (filtro por autor y orden por fecha). */
class ListaPublicacionesTest {

    private ListaPublicaciones lista;

    @BeforeEach
    void preparar() {
        lista = new ListaPublicaciones();
    }

    private static ListaDinamica<String> autores(String... ids) {
        ListaDinamica<String> resultado = new ListaDinamica<>();
        for (String id : ids) {
            resultado.agregar(id);
        }
        return resultado;
    }

    private static Publicacion pub(String id, String autor, String fecha) {
        return new Publicacion(id, autor, "Mensaje " + id, fecha);
    }

    // ------------------------------------------------------------------ generarId

    @Test
    void generarIdEnListaVaciaEmpiezaEnP1() {
        assertEquals("P1", lista.generarId());
    }

    @Test
    void generarIdUsaElMayorNumeroMasUno() {
        lista.agregar(pub("P3", "1", "2026-11-01 10:00:00"));
        lista.agregar(pub("P7", "1", "2026-11-01 11:00:00"));

        assertEquals("P8", lista.generarId());
    }

    @Test
    void generarIdIgnoraIdsConOtroFormatoYNuncaRepite() {
        lista.agregar(pub("abc", "1", "2026-11-01 10:00:00"));
        lista.agregar(pub("P", "1", "2026-11-01 11:00:00"));
        lista.agregar(pub("P2", "1", "2026-11-01 12:00:00"));

        String id = lista.generarId();

        assertEquals("P3", id);
        assertTrue(!lista.existeId(id));
    }

    // ------------------------------------------------------------------ obtenerRecientesDe

    @Test
    void ordenaDeLaMasRecienteALaMasAntigua() {
        lista.agregar(pub("P1", "1", "2026-11-01 10:00:00"));
        lista.agregar(pub("P2", "1", "2026-11-03 08:00:00"));
        lista.agregar(pub("P3", "1", "2026-11-02 23:59:59"));

        ListaDinamica<Publicacion> muro = lista.obtenerRecientesDe(autores("1"));

        assertEquals(3, muro.tamano());
        assertEquals("P2", muro.obtener(0).getId());
        assertEquals("P3", muro.obtener(1).getId());
        assertEquals("P1", muro.obtener(2).getId());
    }

    @Test
    void ordenaPorFechaAunqueSeAgreguenFueraDeOrden() {
        lista.agregar(pub("P1", "1", "2026-12-01 10:00:00"));
        lista.agregar(pub("P2", "2", "2026-01-15 10:00:00"));
        lista.agregar(pub("P3", "1", "2026-06-30 10:00:00"));

        ListaDinamica<Publicacion> muro = lista.obtenerRecientesDe(autores("1", "2"));

        assertEquals("P1", muro.obtener(0).getId());
        assertEquals("P3", muro.obtener(1).getId());
        assertEquals("P2", muro.obtener(2).getId());
    }

    @Test
    void conLaMismaFechaAparecePrimeroLaPublicadaDespues() {
        lista.agregar(pub("P1", "1", "2026-11-01 10:00:00"));
        lista.agregar(pub("P2", "2", "2026-11-01 10:00:00"));
        lista.agregar(pub("P3", "1", "2026-11-01 10:00:00"));

        ListaDinamica<Publicacion> muro = lista.obtenerRecientesDe(autores("1", "2"));

        assertEquals("P3", muro.obtener(0).getId());
        assertEquals("P2", muro.obtener(1).getId());
        assertEquals("P1", muro.obtener(2).getId());
    }

    @Test
    void soloIncluyeLosAutoresIndicados() {
        lista.agregar(pub("P1", "1", "2026-11-01 10:00:00"));
        lista.agregar(pub("P2", "2", "2026-11-02 10:00:00"));
        lista.agregar(pub("P3", "3", "2026-11-03 10:00:00"));

        ListaDinamica<Publicacion> muro = lista.obtenerRecientesDe(autores("1", "3"));

        assertEquals(2, muro.tamano());
        assertEquals("P3", muro.obtener(0).getId());
        assertEquals("P1", muro.obtener(1).getId());
    }

    @Test
    void sinAutoresOListaVaciaDevuelveVacio() {
        lista.agregar(pub("P1", "1", "2026-11-01 10:00:00"));

        assertTrue(lista.obtenerRecientesDe(null).estaVacia());
        assertTrue(lista.obtenerRecientesDe(new ListaDinamica<>()).estaVacia());
        assertTrue(lista.obtenerRecientesDe(autores("9")).estaVacia());
    }

    @Test
    void consultarElMuroNoAlteraElHistorial() {
        lista.agregar(pub("P1", "1", "2026-11-03 10:00:00"));
        lista.agregar(pub("P2", "1", "2026-11-01 10:00:00"));

        lista.obtenerRecientesDe(autores("1"));

        assertEquals("P1", lista.obtener(0).getId()); // sigue en orden de publicación
        assertEquals("P2", lista.obtener(1).getId());
    }

    // ------------------------------------------------------------------ combinarCon

    @Test
    void combinarConAgregaSoloLasQueFaltan() {
        lista.agregar(pub("P1", "1", "2026-11-01 10:00:00"));
        ListaPublicaciones otra = new ListaPublicaciones();
        otra.agregar(pub("P1", "1", "2026-11-01 10:00:00"));
        otra.agregar(pub("P2", "2", "2026-11-02 10:00:00"));

        int agregadas = lista.combinarCon(otra);

        assertEquals(1, agregadas);
        assertEquals(2, lista.tamano());
        assertEquals("P2", lista.obtener(1).getId());
    }

    @Test
    void combinarConNoEliminaNiReemplazaLasExistentes() {
        Publicacion propia = new Publicacion("P1", "1", "Texto propio", "2026-11-01 10:00:00");
        lista.agregar(propia);
        ListaPublicaciones otra = new ListaPublicaciones();
        otra.agregar(new Publicacion("P1", "2", "Texto distinto", "2026-11-05 10:00:00"));

        lista.combinarCon(otra);

        assertEquals(1, lista.tamano());
        assertEquals("Texto propio", lista.buscarPorId("P1").getTexto());
        assertEquals(0, lista.combinarCon(new ListaPublicaciones()));
    }

    @Test
    void combinarConReemplazaLosMeGustaPorLosDelDisco() {
        Publicacion enMemoria = pub("P1", "1", "2026-11-01 10:00:00");
        enMemoria.agregarMeGusta("2");
        enMemoria.agregarMeGusta("3");
        lista.agregar(enMemoria);

        // En el archivo, a "3" ya no le gusta y a "4" sí
        ListaPublicaciones deDisco = new ListaPublicaciones();
        Publicacion actualizada = pub("P1", "1", "2026-11-01 10:00:00");
        actualizada.agregarMeGusta("2");
        actualizada.agregarMeGusta("4");
        deDisco.agregar(actualizada);

        lista.combinarCon(deDisco);

        assertEquals(java.util.List.of("2", "4"), java.util.List.copyOf(lista.buscarPorId("P1").getMeGusta()));
    }

    @Test
    void combinarConQuitaLosMeGustaQueYaNoEstanEnElDisco() {
        Publicacion enMemoria = pub("P1", "1", "2026-11-01 10:00:00");
        enMemoria.agregarMeGusta("2");
        lista.agregar(enMemoria);
        ListaPublicaciones deDisco = new ListaPublicaciones();
        deDisco.agregar(pub("P1", "1", "2026-11-01 10:00:00"));

        lista.combinarCon(deDisco);

        assertEquals(0, lista.buscarPorId("P1").cantidadMeGusta());
    }
}
