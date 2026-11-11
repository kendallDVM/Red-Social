package socialuna.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import socialuna.datos.ListaPerfiles;

class PerfilTest {

    @Test
    void unPerfilNuevoEstaVacioYRequiereId() {
        Perfil p = new Perfil("1");
        assertTrue(p.estaVacio());
        for (CampoPerfil campo : CampoPerfil.values()) {
            assertNull(p.obtener(campo), campo.name());
        }
        assertThrows(IllegalArgumentException.class, () -> new Perfil(" "));
        assertThrows(IllegalArgumentException.class, () -> new Perfil(null));
    }

    @Test
    void losValoresSeNormalizanYElTextoVacioDejaElCampoSinCompletar() {
        Perfil p = new Perfil("1");
        p.establecer(CampoPerfil.CARRERA, "  Ingeniería en Sistemas  ");
        p.establecer(CampoPerfil.INTERESES, "Ajedrez\nprogramación\tmúsica");
        p.establecer(CampoPerfil.BIOGRAFIA, "Línea 1\r\nLínea 2  ");
        p.establecer(CampoPerfil.SEDE, "   ");

        assertEquals("Ingeniería en Sistemas", p.obtener(CampoPerfil.CARRERA));
        assertEquals("Ajedrez programación música", p.obtener(CampoPerfil.INTERESES), "una sola línea");
        assertEquals("Línea 1\nLínea 2", p.obtener(CampoPerfil.BIOGRAFIA), "la biografía conserva los saltos");
        assertNull(p.obtener(CampoPerfil.SEDE));
        assertFalse(p.estaVacio());
    }

    @Test
    void validarRechazaTextosDemasiadoLargos() {
        Perfil p = new Perfil("1");
        p.establecer(CampoPerfil.BIOGRAFIA, "x".repeat(CampoPerfil.BIOGRAFIA.getLargoMaximo()));
        p.validar(); // justo en el límite: válido

        p.establecer(CampoPerfil.BIOGRAFIA, "x".repeat(CampoPerfil.BIOGRAFIA.getLargoMaximo() + 1));
        assertThrows(IllegalArgumentException.class, p::validar);
    }

    @Test
    void losEnlacesAceptanUrlOUsuarioYRechazanEspacios() {
        assertNull(CampoPerfil.INSTAGRAM.validar("https://instagram.com/ana.mora"));
        assertNull(CampoPerfil.INSTAGRAM.validar("@ana.mora"));
        assertNull(CampoPerfil.LINKEDIN.validar("ana-mora_92"));
        assertNull(CampoPerfil.OTRA_RED.validar("http://ejemplo.com/a?b=c"));
        assertNotNull(CampoPerfil.FACEBOOK.validar("ana mora"));
        assertNotNull(CampoPerfil.FACEBOOK.validar("javascript:alert(1)"));
        assertNotNull(CampoPerfil.FACEBOOK.validar("ftp://servidor"));
    }

    @Test
    void laCopiaEsIndependiente() {
        Perfil p = new Perfil("1");
        p.establecer(CampoPerfil.CARRERA, "Sistemas");
        Perfil copia = p.copia();
        copia.establecer(CampoPerfil.CARRERA, "Biología");
        assertEquals("Sistemas", p.obtener(CampoPerfil.CARRERA));
    }

    @Test
    void listaPerfilesReemplazaYNoGuardaPerfilesVacios() {
        ListaPerfiles lista = new ListaPerfiles();
        Perfil p = new Perfil("1");
        p.establecer(CampoPerfil.CARRERA, "Sistemas");
        lista.guardar(p);
        Perfil otro = new Perfil("1");
        otro.establecer(CampoPerfil.CARRERA, "Química");
        lista.guardar(otro);

        assertEquals(1, lista.tamano());
        assertEquals("Química", lista.buscar("1").obtener(CampoPerfil.CARRERA));

        lista.guardar(new Perfil("1")); // vaciar el perfil elimina el registro
        assertEquals(0, lista.tamano());
        assertNull(lista.buscar("1"));

        lista.guardar(new Perfil("2"));
        assertEquals(0, lista.tamano(), "un perfil vacío no se guarda");
    }
}
