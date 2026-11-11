package socialuna.servicios;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import socialuna.datos.GestorArchivos;
import socialuna.datos.ListaUsuarios;
import socialuna.modelo.Usuario;

/**
 * Verifica las cuentas de demostración del archivo real {@code data/usuarios.json}
 * (documentadas en {@code CREDENCIALES_PRUEBA.md}).
 *
 * <p>El archivo real <b>solo se lee</b>: se copia a una carpeta temporal y todas las
 * operaciones se hacen sobre la copia. Una prueba comprueba además que el original no cambió.
 */
class UsuariosPruebaTest {

    private static final Path ARCHIVO_REAL = Paths.get(GestorArchivos.DIRECTORIO_POR_DEFECTO, GestorArchivos.ARCHIVO_USUARIOS);
    private static final String CONTRASENA_DEMO = "SocialUNA2026!";
    private static final String[][] CUENTAS = {
            {"2026001", "Kendall", "kendall"},
            {"2026002", "Greivin", "greivin"},
            {"2026003", "Mariano", "mariano"},
    };

    @TempDir
    Path carpeta;

    private byte[] originalAntes;
    private ContextoAplicacion contexto;

    @BeforeEach
    void copiarArchivoReal() throws Exception {
        originalAntes = Files.readAllBytes(ARCHIVO_REAL);
        Files.copy(ARCHIVO_REAL, carpeta.resolve(GestorArchivos.ARCHIVO_USUARIOS));
        contexto = new ContextoAplicacion(new GestorArchivos(carpeta));
        contexto.cargarTodo();
        Sesion.cerrar();
    }

    @AfterEach
    void verificarQueElOriginalNoCambio() throws Exception {
        Sesion.cerrar();
        assertArrayEquals(originalAntes, Files.readAllBytes(ARCHIVO_REAL), "data/usuarios.json no debe modificarse");
    }

    @Test
    void lasTresCuentasEstanEnElArchivoSinContrasenaEnTextoPlano() throws Exception {
        String json = Files.readString(ARCHIVO_REAL, StandardCharsets.UTF_8);
        for (String[] cuenta : CUENTAS) {
            assertTrue(json.contains("\"usuario\": \"" + cuenta[2] + "\""), cuenta[2]);
            assertTrue(json.contains("\"id\": \"" + cuenta[0] + "\""), cuenta[0]);
        }
        assertFalse(json.contains(CONTRASENA_DEMO), "La contraseña de demostración no debe aparecer en el archivo");
    }

    @Test
    void lasTresCuentasSeCarganEnLaListaYEnElDiccionario() {
        ListaUsuarios usuarios = contexto.getUsuarios();
        for (String[] cuenta : CUENTAS) {
            Usuario porUsuario = usuarios.buscarPorUsuario(cuenta[2]);          // diccionario
            assertNotNull(porUsuario, cuenta[2]);
            assertSame(porUsuario, usuarios.buscarPorId(cuenta[0]));            // lista dinámica
            assertTrue(usuarios.obtenerTodos().contiene(porUsuario));
            assertEquals(cuenta[1], porUsuario.getNombre());
            assertTrue(usuarios.existeUsuario(cuenta[2].toUpperCase()));       // sin distinguir mayúsculas
        }
    }

    @Test
    void lasContrasenasSonHashesBCryptDeCosto12() {
        for (String[] cuenta : CUENTAS) {
            String hash = contexto.getUsuarios().buscarPorUsuario(cuenta[2]).getContrasenaHash();
            assertTrue(Usuario.esHashBCrypt(hash), cuenta[2]);
            assertTrue(hash.startsWith("$2a$12$"), cuenta[2]);
            assertFalse(hash.contains(CONTRASENA_DEMO));
        }
    }

    @Test
    void lasTresCuentasPermitenIniciarSesion() {
        ServicioAutenticacion servicio = new ServicioAutenticacion(contexto);
        for (String[] cuenta : CUENTAS) {
            ResultadoAutenticacion r = servicio.iniciarSesion(cuenta[2], CONTRASENA_DEMO.toCharArray());
            assertTrue(r.esExitoso(), cuenta[2] + ": " + r);
            assertEquals(cuenta[0], Sesion.getUsuarioActivo().getId());
            Sesion.cerrar();
        }
    }

    @Test
    void laContrasenaIncorrectaSeRechaza() {
        ServicioAutenticacion servicio = new ServicioAutenticacion(contexto);
        for (String[] cuenta : CUENTAS) {
            ResultadoAutenticacion r = servicio.iniciarSesion(cuenta[2], "socialuna2026!".toCharArray());
            assertEquals(ResultadoAutenticacion.Tipo.CREDENCIALES_INVALIDAS, r.getTipo(), cuenta[2]);
            assertEquals(ServicioAutenticacion.MENSAJE_CREDENCIALES_INVALIDAS, r.getMensaje());
            assertNull(Sesion.getUsuarioActivo());
        }
    }

    @Test
    void seConservaElUsuarioPreexistenteDavid12() {
        Usuario david = contexto.getUsuarios().buscarPorUsuario("david12");
        assertNotNull(david);
        assertEquals("1234", david.getId());
        assertEquals("david", david.getNombre());
        assertTrue(Usuario.esHashBCrypt(david.getContrasenaHash()));
        assertEquals(4, contexto.getUsuarios().tamano());
    }
}
