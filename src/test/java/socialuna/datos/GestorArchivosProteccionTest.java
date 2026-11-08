package socialuna.datos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static socialuna.UtilPruebas.hash;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import socialuna.modelo.Usuario;

/**
 * Protección de {@code usuarios.json} cuando la carga falla: el archivo dañado se
 * respalda, no se modifica y no se puede sobrescribir hasta una carga válida.
 * Usa una carpeta temporal: nunca toca el data/ real del proyecto.
 */
class GestorArchivosProteccionTest {

    private static final String DANADO = "[ { \"id\": \"1\", \"nombre\": ";

    @TempDir
    Path carpeta;

    private static ListaUsuarios conUnUsuario() {
        ListaUsuarios lista = new ListaUsuarios();
        lista.agregar(new Usuario("9", "Nueva", "nueva", hash("x")));
        return lista;
    }

    private GestorArchivos gestorConArchivoDanado() throws Exception {
        GestorArchivos gestor = new GestorArchivos(carpeta);
        Files.writeString(gestor.getRutaUsuarios(), DANADO);
        assertThrows(PersistenciaException.class, () -> gestor.cargarUsuarios(new ListaUsuarios()));
        return gestor;
    }

    @Test
    void jsonDanadoLanzaExcepcionRespaldaYNoCambiaElOriginal() throws Exception {
        GestorArchivos gestor = gestorConArchivoDanado();

        assertTrue(gestor.isEscrituraUsuariosBloqueada());
        assertEquals(DANADO, Files.readString(gestor.getRutaUsuarios()));
        assertEquals(1, contarRespaldos());
    }

    @Test
    void despuesDeUnaCargaFallidaNoSePuedeSobrescribirElArchivo() throws Exception {
        GestorArchivos gestor = gestorConArchivoDanado();

        PersistenciaException e = assertThrows(PersistenciaException.class,
                () -> gestor.guardarUsuarios(new ListaUsuarios()));             // lista vacía
        assertTrue(e.getMessage().contains("se protegió"));
        assertThrows(PersistenciaException.class, () -> gestor.guardarUsuarios(conUnUsuario()));
        assertThrows(PersistenciaException.class,
                () -> gestor.guardarTodo(conUnUsuario(), new ListaPublicaciones(), new RedAmistades()));

        assertEquals(DANADO, Files.readString(gestor.getRutaUsuarios())); // el original sigue intacto
    }

    @Test
    void cargarTodoConArchivoDanadoTambienBloqueaElGuardado() throws Exception {
        GestorArchivos gestor = new GestorArchivos(carpeta);
        Files.writeString(gestor.getRutaUsuarios(), DANADO);
        ListaUsuarios usuarios = new ListaUsuarios();

        assertThrows(PersistenciaException.class,
                () -> gestor.cargarTodo(usuarios, new ListaPublicaciones(), new RedAmistades()));
        assertThrows(PersistenciaException.class,
                () -> gestor.guardarTodo(usuarios, new ListaPublicaciones(), new RedAmistades()));
        assertEquals(DANADO, Files.readString(gestor.getRutaUsuarios()));
    }

    @Test
    void unaCargaValidaPosteriorPermiteVolverAGuardar() throws Exception {
        GestorArchivos gestor = gestorConArchivoDanado();
        Files.writeString(gestor.getRutaUsuarios(), "[]"); // archivo reparado

        assertEquals(0, gestor.cargarUsuarios(new ListaUsuarios()));
        assertFalse(gestor.isEscrituraUsuariosBloqueada());
        gestor.guardarUsuarios(conUnUsuario());

        ListaUsuarios recargados = new ListaUsuarios();
        assertEquals(1, gestor.cargarUsuarios(recargados));
        assertEquals("nueva", recargados.buscarPorId("9").getUsuario());
    }

    @Test
    void archivoInexistenteSeCreaYNoBloquea() throws Exception {
        GestorArchivos gestor = new GestorArchivos(carpeta.resolve("nueva"));
        assertEquals(0, gestor.cargarUsuarios(new ListaUsuarios()));
        assertEquals("[]", Files.readString(gestor.getRutaUsuarios()));
        assertFalse(gestor.isEscrituraUsuariosBloqueada());
        gestor.guardarUsuarios(conUnUsuario()); // se puede guardar normalmente
    }

    @Test
    void archivoVacioEsListaVaciaYNoBloquea() throws Exception {
        GestorArchivos gestor = new GestorArchivos(carpeta);
        Files.writeString(gestor.getRutaUsuarios(), "");
        ListaUsuarios lista = conUnUsuario();

        assertEquals(0, gestor.cargarUsuarios(lista));
        assertTrue(lista.estaVacia());
        assertFalse(gestor.isEscrituraUsuariosBloqueada());
        gestor.guardarUsuarios(conUnUsuario());
    }

    private long contarRespaldos() throws Exception {
        try (Stream<Path> archivos = Files.list(carpeta)) {
            return archivos.filter(p -> p.getFileName().toString().startsWith("usuarios.json.corrupto-")).count();
        }
    }
}
