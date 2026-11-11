package socialuna.modelo;

import static socialuna.UtilPruebas.hash;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class ModelosTest {

    private static final String HASH = hash("secreta123");

    @Test
    void usuarioValidaCamposObligatorios() {
        assertThrows(IllegalArgumentException.class, () -> new Usuario("", "Ana", "ana", HASH));
        assertThrows(IllegalArgumentException.class, () -> new Usuario("1", " ", "ana", HASH));
        assertThrows(IllegalArgumentException.class, () -> new Usuario("1", "Ana", null, HASH));
        assertThrows(IllegalArgumentException.class, () -> new Usuario("1", "Ana", "ana", ""));
        assertThrows(IllegalArgumentException.class, () -> new Usuario("1", "Ana", "ana", null));
    }

    @Test
    void usuarioRechazaContrasenaEnTextoPlano() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new Usuario("1", "Ana", "ana", "secreta123"));
        assertFalse(e.getMessage().contains("secreta123"));
        Usuario u = new Usuario("1", "Ana", "ana", HASH);
        assertThrows(IllegalArgumentException.class, () -> u.setContrasenaHash("otraEnTextoPlano"));
        assertEquals(HASH, u.getContrasenaHash());
    }

    @Test
    void esHashBCryptReconoceElFormato() {
        assertTrue(Usuario.esHashBCrypt(HASH));
        assertFalse(Usuario.esHashBCrypt("secreta123"));
        assertFalse(Usuario.esHashBCrypt(null));
        assertFalse(Usuario.esHashBCrypt(HASH.substring(0, 59)));
    }

    @Test
    void usuarioNoAceptaEspaciosEnElNombreDeUsuario() {
        assertThrows(IllegalArgumentException.class, () -> new Usuario("1", "Ana", "ana mora", HASH));
        assertFalse(Usuario.esNombreUsuarioValido("a b"));
        assertTrue(Usuario.esNombreUsuarioValido("ana_mora"));
    }

    @Test
    void normalizarNombreUsuarioIgnoraMayusculas() {
        assertEquals("kendall", Usuario.normalizarNombreUsuario("Kendall"));
        assertEquals("kendall", Usuario.normalizarNombreUsuario("KENDALL"));
        assertEquals(null, Usuario.normalizarNombreUsuario(null));
    }

    @Test
    void toStringNoMuestraLaContrasenaNiElHash() {
        Usuario u = new Usuario("1", "Ana", "ana", HASH);
        assertFalse(u.toString().contains(HASH));
        assertFalse(u.toString().contains("$2"));
        assertTrue(u.toString().contains("ana"));
    }

    @Test
    void publicacionFormateaYValidaLaFecha() {
        Publicacion p = new Publicacion("p1", "1", "Hola", LocalDateTime.of(2026, 11, 4, 9, 5, 3));
        assertEquals("2026-11-04 09:05:03", p.getFechaHora());
        assertThrows(IllegalArgumentException.class, () -> new Publicacion("p2", "1", "Hola", "04/11/2026"));
        assertThrows(IllegalArgumentException.class, () -> new Publicacion("p3", "1", "  ", "2026-11-04 09:05:03"));
    }

    @Test
    void amistadEsBidireccional() {
        Amistad ab = new Amistad("1", "2");
        Amistad ba = new Amistad("2", "1");
        assertEquals(ab, ba);
        assertEquals(ab.hashCode(), ba.hashCode());
        assertEquals("2", ab.obtenerOtro("1"));
        assertTrue(ab.involucra("2"));
        assertFalse(ab.involucra("3"));
        assertThrows(IllegalArgumentException.class, () -> new Amistad("1", "1"));
    }
}
