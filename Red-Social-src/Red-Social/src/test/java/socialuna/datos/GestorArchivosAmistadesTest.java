package socialuna.datos;

import static socialuna.UtilPruebas.hash;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import socialuna.modelo.Usuario;

/**
 * Persistencia de amistades.json. Todas las pruebas usan una carpeta temporal:
 * nunca tocan el data/ real del proyecto.
 */
class GestorArchivosAmistadesTest {

    @TempDir
    Path carpetaTemporal;

    private ListaUsuarios usuarios;

    @BeforeEach
    void preparar() {
        usuarios = new ListaUsuarios();
        usuarios.agregar(new Usuario("1", "Ana", "ana", hash("clave1")));
        usuarios.agregar(new Usuario("2", "Luis", "luis", hash("clave2")));
        usuarios.agregar(new Usuario("3", "Sofía", "sofia", hash("clave3")));
    }

    private void escribir(String json) throws Exception {
        Files.createDirectories(carpetaTemporal);
        Files.writeString(carpetaTemporal.resolve(GestorArchivos.ARCHIVO_AMISTADES), json, StandardCharsets.UTF_8);
    }

    @Test
    void guardarYVolverACargarReconstruyeElGrafo() throws Exception {
        RedAmistades red = new RedAmistades();
        red.agregarAmistad("1", "2");
        red.agregarAmistad("2", "3");
        new GestorArchivos(carpetaTemporal).guardarAmistades(red);

        RedAmistades cargada = new RedAmistades();
        int cantidad = new GestorArchivos(carpetaTemporal).cargarAmistades(cargada, usuarios);

        assertEquals(2, cantidad);
        assertTrue(cargada.sonAmigos("2", "1"), "la amistad debe quedar en ambos sentidos");
        assertTrue(cargada.sonAmigos("3", "2"));
        assertFalse(cargada.sonAmigos("1", "3"));
    }

    @Test
    void cadaAmistadSeGuardaUnaSolaVez() throws Exception {
        RedAmistades red = new RedAmistades();
        red.agregarAmistad("1", "2");
        GestorArchivos gestor = new GestorArchivos(carpetaTemporal);

        gestor.guardarAmistades(red);

        String json = Files.readString(gestor.getRutaAmistades(), StandardCharsets.UTF_8);
        assertEquals(1, json.split("idUsuarioA").length - 1);
    }

    @Test
    void archivoInexistenteSeCreaVacio() throws Exception {
        GestorArchivos gestor = new GestorArchivos(carpetaTemporal);
        RedAmistades red = new RedAmistades();

        assertEquals(0, gestor.cargarAmistades(red, usuarios));

        assertTrue(Files.exists(gestor.getRutaAmistades()));
        assertEquals("[]", Files.readString(gestor.getRutaAmistades()));
    }

    @Test
    void archivoVacioNoFalla() throws Exception {
        escribir("   ");
        RedAmistades red = new RedAmistades();
        red.agregarAmistad("1", "2");

        assertEquals(0, new GestorArchivos(carpetaTemporal).cargarAmistades(red, usuarios));
        assertEquals(0, red.cantidadAmistades());
    }

    @Test
    void seIgnoranIdsQueNoExistenEnUsuarios() throws Exception {
        escribir("""
                [
                  {"idUsuarioA": "1", "idUsuarioB": "2"},
                  {"idUsuarioA": "1", "idUsuarioB": "999"},
                  {"idUsuarioA": "888", "idUsuarioB": "3"}
                ]""");
        RedAmistades red = new RedAmistades();

        assertEquals(1, new GestorArchivos(carpetaTemporal).cargarAmistades(red, usuarios));
        assertTrue(red.sonAmigos("1", "2"));
        assertFalse(red.contieneUsuario("999"));
        assertFalse(red.contieneUsuario("888"));
    }

    @Test
    void losDuplicadosQuedanComoUnaSolaAmistad() throws Exception {
        escribir("""
                [
                  {"idUsuarioA": "1", "idUsuarioB": "2"},
                  {"idUsuarioA": "2", "idUsuarioB": "1"},
                  {"idUsuarioA": "1", "idUsuarioB": "2"}
                ]""");
        RedAmistades red = new RedAmistades();

        assertEquals(1, new GestorArchivos(carpetaTemporal).cargarAmistades(red, usuarios));
        assertEquals(1, red.cantidadAmigos("1"));
    }

    @Test
    void seIgnoranRegistrosIncompletosOConsigoMismo() throws Exception {
        escribir("""
                [
                  {"idUsuarioA": "1"},
                  {"idUsuarioA": "2", "idUsuarioB": "2"},
                  null,
                  {"idUsuarioA": "2", "idUsuarioB": "3"}
                ]""");
        RedAmistades red = new RedAmistades();

        assertEquals(1, new GestorArchivos(carpetaTemporal).cargarAmistades(red, usuarios));
        assertTrue(red.sonAmigos("2", "3"));
    }

    @Test
    void jsonDanadoNoModificaLaRedRespaldaYBloqueaElGuardado() throws Exception {
        escribir("[ {\"idUsuarioA\": \"1\", ");
        GestorArchivos gestor = new GestorArchivos(carpetaTemporal);
        RedAmistades red = new RedAmistades();
        red.agregarAmistad("1", "3");

        assertThrows(PersistenciaException.class, () -> gestor.cargarAmistades(red, usuarios));

        assertTrue(red.sonAmigos("1", "3"), "la red en memoria no debe cambiar");
        assertTrue(gestor.isEscrituraAmistadesBloqueada());
        assertFalse(gestor.isEscrituraUsuariosBloqueada(), "un error de amistades no bloquea usuarios");
        assertThrows(PersistenciaException.class, () -> gestor.guardarAmistades(red));
        try (Stream<Path> archivos = Files.list(carpetaTemporal)) {
            assertTrue(archivos.anyMatch(p -> p.getFileName().toString().startsWith("amistades.json.corrupto-")));
        }
    }

    @Test
    void cargarTodoYGuardarTodoIncluyenLasAmistades() throws Exception {
        GestorArchivos gestor = new GestorArchivos(carpetaTemporal);
        RedAmistades red = new RedAmistades();
        red.agregarAmistad("1", "2");
        gestor.guardarTodo(usuarios, new ListaPublicaciones(), red);

        ListaUsuarios usuariosCargados = new ListaUsuarios();
        RedAmistades redCargada = new RedAmistades();
        new GestorArchivos(carpetaTemporal).cargarTodo(usuariosCargados, new ListaPublicaciones(), redCargada);

        assertEquals(3, usuariosCargados.tamano());
        assertTrue(redCargada.sonAmigos("1", "2"));
    }
}
