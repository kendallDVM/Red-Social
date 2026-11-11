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

import socialuna.modelo.Publicacion;
import socialuna.modelo.Usuario;

/**
 * Persistencia de publicaciones.json. Todas las pruebas usan una carpeta temporal:
 * nunca tocan el data/ real del proyecto.
 */
class GestorArchivosPublicacionesTest {

    @TempDir
    Path carpetaTemporal;

    private ListaUsuarios usuarios;

    @BeforeEach
    void preparar() {
        usuarios = new ListaUsuarios();
        usuarios.agregar(new Usuario("1", "Ana", "ana", hash("clave1")));
        usuarios.agregar(new Usuario("2", "Luis", "luis", hash("clave2")));
    }

    private Path archivo() {
        return carpetaTemporal.resolve(GestorArchivos.ARCHIVO_PUBLICACIONES);
    }

    private void escribir(String json) throws Exception {
        Files.createDirectories(carpetaTemporal);
        Files.writeString(archivo(), json, StandardCharsets.UTF_8);
    }

    @Test
    void guardarYVolverACargarReconstruyeElHistorial() throws Exception {
        ListaPublicaciones lista = new ListaPublicaciones();
        lista.agregar(new Publicacion("P1", "1", "Primer mensaje", "2026-11-01 10:00:00"));
        lista.agregar(new Publicacion("P2", "2", "Segundo mensaje", "2026-11-02 11:30:00"));
        new GestorArchivos(carpetaTemporal).guardarPublicaciones(lista);

        ListaPublicaciones cargada = new ListaPublicaciones();
        int cantidad = new GestorArchivos(carpetaTemporal).cargarPublicaciones(cargada, usuarios);

        assertEquals(2, cantidad);
        assertEquals("P1", cargada.obtener(0).getId()); // conserva el orden de publicación
        assertEquals("Segundo mensaje", cargada.obtener(1).getTexto());
        assertEquals("2026-11-02 11:30:00", cargada.obtener(1).getFechaHora());
        assertEquals("2", cargada.obtener(1).getIdAutor());
    }

    @Test
    void conservaTildesEnieEmojisComillasYSaltosDeLinea() throws Exception {
        String texto = "¡Hola! Qué día, mañana \"examen\" de Estructuras.\nSegunda línea con ñ, é y ☕ y <b>&</b>.";
        ListaPublicaciones lista = new ListaPublicaciones();
        lista.agregar(new Publicacion("P1", "1", texto, "2026-11-01 10:00:00"));
        new GestorArchivos(carpetaTemporal).guardarPublicaciones(lista);

        // El archivo está en UTF-8 y el texto aparece sin escapes raros
        String crudo = Files.readString(archivo(), StandardCharsets.UTF_8);
        assertTrue(crudo.contains("¡Hola! Qué día"));
        assertTrue(crudo.contains("<b>&</b>"));

        ListaPublicaciones cargada = new ListaPublicaciones();
        new GestorArchivos(carpetaTemporal).cargarPublicaciones(cargada, usuarios);
        assertEquals(texto, cargada.obtener(0).getTexto());
    }

    @Test
    void archivoInexistenteSeCreaVacioYNoEsError() throws Exception {
        ListaPublicaciones cargada = new ListaPublicaciones();
        cargada.agregar(new Publicacion("PX", "1", "Anterior", "2026-11-01 10:00:00"));

        int cantidad = new GestorArchivos(carpetaTemporal).cargarPublicaciones(cargada, usuarios);

        assertEquals(0, cantidad);
        assertTrue(cargada.estaVacia());
        assertTrue(Files.exists(archivo()));
        assertEquals("[]", Files.readString(archivo(), StandardCharsets.UTF_8).strip());
    }

    @Test
    void archivoVacioSeInterpretaComoSinDatos() throws Exception {
        escribir("   \n");

        ListaPublicaciones cargada = new ListaPublicaciones();
        assertEquals(0, new GestorArchivos(carpetaTemporal).cargarPublicaciones(cargada, usuarios));
    }

    @Test
    void ignoraPublicacionesConAutorInexistente() throws Exception {
        escribir("[{\"id\":\"P1\",\"idAutor\":\"1\",\"texto\":\"Válida\",\"fechaHora\":\"2026-11-01 10:00:00\"},"
                + "{\"id\":\"P2\",\"idAutor\":\"99\",\"texto\":\"Sin autor\",\"fechaHora\":\"2026-11-01 11:00:00\"}]");

        ListaPublicaciones cargada = new ListaPublicaciones();
        int cantidad = new GestorArchivos(carpetaTemporal).cargarPublicaciones(cargada, usuarios);

        assertEquals(1, cantidad);
        assertEquals("P1", cargada.obtener(0).getId());
    }

    @Test
    void ignoraRegistrosIncompletosYConFechaInvalida() throws Exception {
        escribir("[null,"
                + "{\"id\":\"P1\",\"idAutor\":\"1\",\"texto\":\"Válida\",\"fechaHora\":\"2026-11-01 10:00:00\"},"
                + "{\"id\":\"P2\",\"idAutor\":\"1\",\"texto\":\"   \",\"fechaHora\":\"2026-11-01 10:00:00\"},"
                + "{\"id\":\"P3\",\"idAutor\":\"1\",\"texto\":\"Fecha mala\",\"fechaHora\":\"ayer\"},"
                + "{\"idAutor\":\"1\",\"texto\":\"Sin id\",\"fechaHora\":\"2026-11-01 10:00:00\"}]");

        ListaPublicaciones cargada = new ListaPublicaciones();
        int cantidad = new GestorArchivos(carpetaTemporal).cargarPublicaciones(cargada, usuarios);

        assertEquals(1, cantidad);
        assertEquals("P1", cargada.obtener(0).getId());
    }

    @Test
    void siUnIdSeRepiteConservaSoloLaPrimera() throws Exception {
        escribir("[{\"id\":\"P1\",\"idAutor\":\"1\",\"texto\":\"Primera\",\"fechaHora\":\"2026-11-01 10:00:00\"},"
                + "{\"id\":\"P1\",\"idAutor\":\"2\",\"texto\":\"Repetida\",\"fechaHora\":\"2026-11-02 10:00:00\"}]");

        ListaPublicaciones cargada = new ListaPublicaciones();
        new GestorArchivos(carpetaTemporal).cargarPublicaciones(cargada, usuarios);

        assertEquals(1, cargada.tamano());
        assertEquals("Primera", cargada.obtener(0).getTexto());
    }

    @Test
    void archivoDanadoLanzaErrorRespaldaYBloqueaElGuardado() throws Exception {
        escribir("[ {roto");
        GestorArchivos gestor = new GestorArchivos(carpetaTemporal);
        ListaPublicaciones destino = new ListaPublicaciones();
        destino.agregar(new Publicacion("P1", "1", "En memoria", "2026-11-01 10:00:00"));

        assertThrows(PersistenciaException.class, () -> gestor.cargarPublicaciones(destino, usuarios));

        assertEquals(1, destino.tamano()); // la lista en memoria no se tocó
        assertTrue(gestor.isEscrituraPublicacionesBloqueada());
        assertThrows(PersistenciaException.class, () -> gestor.guardarPublicaciones(destino));
        assertEquals("[ {roto", Files.readString(archivo(), StandardCharsets.UTF_8)); // original intacto
        try (Stream<Path> archivos = Files.list(carpetaTemporal)) {
            assertTrue(archivos.anyMatch(p -> p.getFileName().toString().startsWith("publicaciones.json.corrupto-")));
        }
    }

    @Test
    void unaCargaCorrectaLevantaElBloqueo() throws Exception {
        escribir("[ {roto");
        GestorArchivos gestor = new GestorArchivos(carpetaTemporal);
        assertThrows(PersistenciaException.class, () -> gestor.cargarPublicaciones(new ListaPublicaciones(), usuarios));
        assertTrue(gestor.isEscrituraPublicacionesBloqueada());

        escribir("[]");
        gestor.cargarPublicaciones(new ListaPublicaciones(), usuarios);

        assertFalse(gestor.isEscrituraPublicacionesBloqueada());
    }

    @Test
    void cargarTodoYGuardarTodoIncluyenLasPublicaciones() throws Exception {
        GestorArchivos gestor = new GestorArchivos(carpetaTemporal);
        ListaPublicaciones publicaciones = new ListaPublicaciones();
        publicaciones.agregar(new Publicacion("P1", "1", "Hola", "2026-11-01 10:00:00"));
        gestor.guardarTodo(usuarios, publicaciones, new RedAmistades());

        ListaUsuarios usuariosCargados = new ListaUsuarios();
        ListaPublicaciones publicacionesCargadas = new ListaPublicaciones();
        new GestorArchivos(carpetaTemporal).cargarTodo(usuariosCargados, publicacionesCargadas, new RedAmistades());

        assertEquals(2, usuariosCargados.tamano());
        assertEquals(1, publicacionesCargadas.tamano());
        assertEquals("Hola", publicacionesCargadas.obtener(0).getTexto());
    }

    @Test
    void noQuedaArchivoTemporalTrasGuardar() throws Exception {
        new GestorArchivos(carpetaTemporal).guardarPublicaciones(new ListaPublicaciones());

        try (Stream<Path> archivos = Files.list(carpetaTemporal)) {
            assertFalse(archivos.anyMatch(p -> p.getFileName().toString().endsWith(".tmp")));
        }
    }

    @Test
    void guardaYRecuperaLosMeGusta() throws Exception {
        ListaPublicaciones lista = new ListaPublicaciones();
        Publicacion p = new Publicacion("P1", "1", "Hola", "2026-11-01 10:00:00");
        p.agregarMeGusta("2");
        p.agregarMeGusta("1");
        lista.agregar(p);
        lista.agregar(new Publicacion("P2", "2", "Sin me gusta", "2026-11-02 10:00:00"));
        new GestorArchivos(carpetaTemporal).guardarPublicaciones(lista);

        ListaPublicaciones cargada = new ListaPublicaciones();
        new GestorArchivos(carpetaTemporal).cargarPublicaciones(cargada, usuarios);

        assertEquals(java.util.List.of("2", "1"), java.util.List.copyOf(cargada.buscarPorId("P1").getMeGusta()));
        assertEquals(0, cargada.buscarPorId("P2").cantidadMeGusta());
        // La publicación sin "me gusta" no trae el campo en el archivo
        String crudo = Files.readString(archivo(), StandardCharsets.UTF_8);
        assertEquals(1, crudo.split("meGusta", -1).length - 1);
    }

    @Test
    void cargaArchivosAnterioresSinElCampoMeGusta() throws Exception {
        escribir("[{\"id\":\"P1\",\"idAutor\":\"1\",\"texto\":\"Antigua\",\"fechaHora\":\"2026-11-01 10:00:00\"}]");

        ListaPublicaciones cargada = new ListaPublicaciones();
        int cantidad = new GestorArchivos(carpetaTemporal).cargarPublicaciones(cargada, usuarios);

        assertEquals(1, cantidad);
        assertEquals(0, cargada.obtener(0).cantidadMeGusta());
    }

    @Test
    void ignoraLosMeGustaDeUsuariosInexistentes() throws Exception {
        escribir("[{\"id\":\"P1\",\"idAutor\":\"1\",\"texto\":\"Hola\",\"fechaHora\":\"2026-11-01 10:00:00\","
                + "\"meGusta\":[\"2\",\"99\"]}]");

        ListaPublicaciones cargada = new ListaPublicaciones();
        new GestorArchivos(carpetaTemporal).cargarPublicaciones(cargada, usuarios);

        assertEquals(java.util.List.of("2"), java.util.List.copyOf(cargada.obtener(0).getMeGusta()));
    }
}
