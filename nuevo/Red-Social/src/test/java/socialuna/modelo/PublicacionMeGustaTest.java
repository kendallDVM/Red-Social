package socialuna.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/** "Me gusta" de una publicación: conjunto de IDs sin repetidos que no altera el JSON si está vacío. */
class PublicacionMeGustaTest {

    private Publicacion publicacion;

    @BeforeEach
    void preparar() {
        publicacion = new Publicacion("P1", "1", "Hola", "2026-11-01 10:00:00");
    }

    @Test
    void alInicioANadieLeGusta() {
        assertEquals(0, publicacion.cantidadMeGusta());
        assertFalse(publicacion.leGustaA("2"));
        assertTrue(publicacion.getMeGusta().isEmpty());
    }

    @Test
    void agregarMeGustaCuentaUnaVezPorUsuario() {
        assertTrue(publicacion.agregarMeGusta("2"));
        assertFalse(publicacion.agregarMeGusta("2")); // repetido: no cuenta dos veces
        assertTrue(publicacion.agregarMeGusta("3"));

        assertEquals(2, publicacion.cantidadMeGusta());
        assertTrue(publicacion.leGustaA("2"));
        assertFalse(publicacion.leGustaA("9"));
        assertFalse(publicacion.leGustaA(null));
    }

    @Test
    void quitarMeGusta() {
        publicacion.agregarMeGusta("2");

        assertTrue(publicacion.quitarMeGusta("2"));
        assertFalse(publicacion.quitarMeGusta("2")); // ya no estaba
        assertEquals(0, publicacion.cantidadMeGusta());
    }

    @Test
    void agregarMeGustaSinIdLanzaError() {
        assertThrows(IllegalArgumentException.class, () -> publicacion.agregarMeGusta(null));
        assertThrows(IllegalArgumentException.class, () -> publicacion.agregarMeGusta("  "));
    }

    @Test
    void getMeGustaEsUnaCopiaDeSoloLectura() {
        publicacion.agregarMeGusta("2");

        Set<String> copia = publicacion.getMeGusta();

        assertThrows(UnsupportedOperationException.class, () -> copia.add("3"));
        assertEquals(1, publicacion.cantidadMeGusta());
    }

    @Test
    void conservaElOrdenEnQueDieronMeGusta() {
        publicacion.agregarMeGusta("3");
        publicacion.agregarMeGusta("1");
        publicacion.agregarMeGusta("2");

        assertEquals(List.of("3", "1", "2"), List.copyOf(publicacion.getMeGusta()));
    }

    @Test
    void reemplazarMeGustaIgnoraVaciosYRepetidos() {
        publicacion.agregarMeGusta("9");

        publicacion.reemplazarMeGusta(Arrays.asList("2", null, " ", "2", "3"));

        assertEquals(List.of("2", "3"), List.copyOf(publicacion.getMeGusta()));
    }

    @Test
    void sinMeGustaElJsonNoTraeElCampo() {
        Gson gson = new GsonBuilder().create();

        assertFalse(gson.toJson(publicacion).contains("meGusta"));

        publicacion.agregarMeGusta("2");
        publicacion.quitarMeGusta("2"); // vuelve a quedar sin campo
        assertFalse(gson.toJson(publicacion).contains("meGusta"));
    }

    @Test
    void unJsonAnteriorSinElCampoSeLeeSinMeGusta() {
        Publicacion leida = new Gson().fromJson(
                "{\"id\":\"P1\",\"idAutor\":\"1\",\"texto\":\"Hola\",\"fechaHora\":\"2026-11-01 10:00:00\"}",
                Publicacion.class);

        leida.validar();

        assertEquals(0, leida.cantidadMeGusta());
    }

    @Test
    void validarDescartaIdsVaciosDeUnJsonEditadoAMano() {
        Publicacion leida = new Gson().fromJson(
                "{\"id\":\"P1\",\"idAutor\":\"1\",\"texto\":\"Hola\",\"fechaHora\":\"2026-11-01 10:00:00\","
                        + "\"meGusta\":[\"2\",\"\",null]}", Publicacion.class);

        leida.validar();

        assertEquals(List.of("2"), List.copyOf(leida.getMeGusta()));
    }
}
