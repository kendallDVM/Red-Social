package socialuna.datos;

import static socialuna.UtilPruebas.hash;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import socialuna.UtilPruebas;
import socialuna.modelo.Usuario;

/**
 * Todas las pruebas usan una carpeta temporal: nunca tocan el data/ real del proyecto.
 */
class GestorArchivosTest {

    private static final String HASH_A = hash("MiClaveSecreta");
    private static final String HASH_B = hash("OtraClaveSecreta");

    @TempDir
    Path carpetaTemporal;

    private ListaUsuarios usuariosDePrueba() {
        ListaUsuarios lista = new ListaUsuarios();
        lista.agregar(new Usuario("100", "Ana Mora", "ana", hash("clave1")));
        lista.agregar(new Usuario("200", "Luis Solís Peña", "luis", hash("clave2"))); // tildes y ñ
        return lista;
    }

    @Test
    void guardarYVolverACargarRecuperaLosDatos() throws Exception {
        GestorArchivos gestor = new GestorArchivos(carpetaTemporal);
        gestor.guardarUsuarios(usuariosDePrueba());
        assertTrue(Files.exists(gestor.getRutaUsuarios()));

        ListaUsuarios cargados = new ListaUsuarios();
        int cantidad = new GestorArchivos(carpetaTemporal).cargarUsuarios(cargados);

        assertEquals(2, cantidad);
        Usuario luis = cargados.buscarPorUsuario("luis"); // el índice se reconstruyó
        assertNotNull(luis);
        assertEquals("200", luis.getId());
        assertEquals("Luis Solís Peña", luis.getNombre());
        assertTrue(UtilPruebas.verifica("clave2", luis.getContrasenaHash()));
        assertSame(luis, cargados.buscarPorId("200")); // la lista y el índice apuntan al mismo objeto
        assertEquals("ana", cargados.obtenerTodos().obtener(0).getUsuario()); // se conserva el orden
    }

    @Test
    void elJsonGuardadoEsUnArregloLegibleEnUtf8() throws Exception {
        GestorArchivos gestor = new GestorArchivos(carpetaTemporal);
        gestor.guardarUsuarios(usuariosDePrueba());
        String json = Files.readString(gestor.getRutaUsuarios(), StandardCharsets.UTF_8);
        assertTrue(json.trim().startsWith("["));
        assertTrue(json.contains("\"usuario\": \"ana\""));
        assertTrue(json.contains("Solís Peña"));
    }

    @Test
    void creaCarpetaYArchivoSiNoExisten() throws Exception {
        Path carpeta = carpetaTemporal.resolve("no-existe").resolve("data");
        GestorArchivos gestor = new GestorArchivos(carpeta);
        ListaUsuarios lista = new ListaUsuarios();

        assertEquals(0, gestor.cargarUsuarios(lista));
        assertTrue(lista.estaVacia());
        assertTrue(Files.exists(gestor.getRutaUsuarios()));
        assertEquals("[]", Files.readString(gestor.getRutaUsuarios()));
    }

    @Test
    void archivoVacioSeInterpretaComoSinUsuarios() throws Exception {
        GestorArchivos gestor = new GestorArchivos(carpetaTemporal);
        Files.writeString(gestor.getRutaUsuarios(), "   \n");
        ListaUsuarios lista = new ListaUsuarios();
        assertEquals(0, gestor.cargarUsuarios(lista));
        assertTrue(lista.estaVacia());
    }

    @Test
    void jsonInvalidoNoModificaLaListaYCreaRespaldo() throws Exception {
        GestorArchivos gestor = new GestorArchivos(carpetaTemporal);
        String danado = "[ { \"id\": \"1\", \"nombre\": ";
        Files.writeString(gestor.getRutaUsuarios(), danado);

        ListaUsuarios lista = usuariosDePrueba(); // ya tiene datos en memoria
        PersistenciaException e = assertThrows(PersistenciaException.class, () -> gestor.cargarUsuarios(lista));

        assertTrue(e.getMessage().contains("dañado"));
        assertEquals(2, lista.tamano()); // la carga fallida no borró nada
        assertEquals(danado, Files.readString(gestor.getRutaUsuarios())); // original intacto
        assertEquals(1, contarRespaldos());
    }

    @Test
    void registrosDuplicadosEnElArchivoSeRechazan() throws Exception {
        GestorArchivos gestor = new GestorArchivos(carpetaTemporal);
        Files.writeString(gestor.getRutaUsuarios(), "["
                + "{\"id\":\"1\",\"nombre\":\"A\",\"usuario\":\"ana\",\"contrasena\":\"" + HASH_A + "\"},"
                + "{\"id\":\"1\",\"nombre\":\"B\",\"usuario\":\"bea\",\"contrasena\":\"" + HASH_B + "\"}]");

        ListaUsuarios lista = new ListaUsuarios();
        PersistenciaException e = assertThrows(PersistenciaException.class, () -> gestor.cargarUsuarios(lista));
        assertTrue(e.getMessage().contains("registro #2"));
        assertTrue(lista.estaVacia());
    }

    @Test
    void registroConCamposFaltantesSeRechaza() throws Exception {
        GestorArchivos gestor = new GestorArchivos(carpetaTemporal);
        Files.writeString(gestor.getRutaUsuarios(), "[{\"id\":\"1\",\"usuario\":\"ana\"}]");
        assertThrows(PersistenciaException.class, () -> gestor.cargarUsuarios(new ListaUsuarios()));
    }

    @Test
    void losMensajesDeErrorNoIncluyenContrasenas() throws Exception {
        GestorArchivos gestor = new GestorArchivos(carpetaTemporal);
        Files.writeString(gestor.getRutaUsuarios(), "["
                + "{\"id\":\"1\",\"nombre\":\"A\",\"usuario\":\"ana\",\"contrasena\":\"" + HASH_A + "\"},"
                + "{\"id\":\"2\",\"nombre\":\"B\",\"usuario\":\"ANA\",\"contrasena\":\"" + HASH_B + "\"}]");
        PersistenciaException e = assertThrows(PersistenciaException.class,
                () -> gestor.cargarUsuarios(new ListaUsuarios()));
        assertTrue(e.getMessage().contains("registro #2")); // "ana" y "ANA" son la misma cuenta
        assertFalse(e.getMessage().contains(HASH_A));
        assertFalse(e.getMessage().contains(HASH_B));
    }

    @Test
    void contrasenaEnTextoPlanoEnElArchivoSeRechaza() throws Exception {
        GestorArchivos gestor = new GestorArchivos(carpetaTemporal);
        Files.writeString(gestor.getRutaUsuarios(),
                "[{\"id\":\"1\",\"nombre\":\"A\",\"usuario\":\"ana\",\"contrasena\":\"MiClaveSecreta\"}]");
        PersistenciaException e = assertThrows(PersistenciaException.class,
                () -> gestor.cargarUsuarios(new ListaUsuarios()));
        assertTrue(e.getMessage().contains("hash BCrypt"));
        assertFalse(e.getMessage().contains("MiClaveSecreta"));
    }

    @Test
    void guardarSobrescribeSinDejarTemporales() throws Exception {
        GestorArchivos gestor = new GestorArchivos(carpetaTemporal);
        ListaUsuarios lista = usuariosDePrueba();
        gestor.guardarUsuarios(lista);
        lista.agregar(new Usuario("300", "Carla", "carla", hash("c")));
        gestor.guardarUsuarios(lista);

        ListaUsuarios cargados = new ListaUsuarios();
        assertEquals(3, gestor.cargarUsuarios(cargados));
        assertFalse(Files.exists(carpetaTemporal.resolve(GestorArchivos.ARCHIVO_USUARIOS + ".tmp")));
    }

    @Test
    void cargarTodoYGuardarTodoManejanUsuarios() throws Exception {
        GestorArchivos gestor = new GestorArchivos(carpetaTemporal);
        gestor.guardarTodo(usuariosDePrueba(), new ListaPublicaciones(), new RedAmistades());
        ListaUsuarios cargados = new ListaUsuarios();
        gestor.cargarTodo(cargados, new ListaPublicaciones(), new RedAmistades());
        assertEquals(2, cargados.tamano());
    }

    private long contarRespaldos() throws IOException {
        try (Stream<Path> archivos = Files.list(carpetaTemporal)) {
            return archivos.filter(p -> p.getFileName().toString().contains(".corrupto-")).count();
        }
    }
}
