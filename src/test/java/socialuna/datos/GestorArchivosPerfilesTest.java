package socialuna.datos;

import static socialuna.UtilPruebas.hash;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import socialuna.modelo.CampoPerfil;
import socialuna.modelo.Perfil;
import socialuna.modelo.Usuario;

/** Persistencia de perfiles.json. Usa una carpeta temporal: nunca toca el data/ real. */
class GestorArchivosPerfilesTest {

    @TempDir
    Path carpeta;

    private GestorArchivos gestor;
    private ListaUsuarios usuarios;

    @BeforeEach
    void preparar() {
        gestor = new GestorArchivos(carpeta);
        usuarios = new ListaUsuarios();
        usuarios.agregar(new Usuario("1", "Ana", "ana", hash("a")));
        usuarios.agregar(new Usuario("2", "Luis", "luis", hash("b")));
    }

    @Test
    void guardarYCargarConservaLosCamposYOmiteLosVacios() throws Exception {
        ListaPerfiles original = new ListaPerfiles();
        Perfil p = new Perfil("1");
        p.establecer(CampoPerfil.BIOGRAFIA, "Hola,\nsoy Ana. ¡Me gusta el café!");
        p.establecer(CampoPerfil.INSTAGRAM, "@ana.mora");
        original.guardar(p);
        gestor.guardarPerfiles(original);

        String json = Files.readString(gestor.getRutaPerfiles(), StandardCharsets.UTF_8);
        assertFalse(json.contains("carrera"), "los campos vacíos no se escriben: " + json);
        assertFalse(json.contains("contrasena"), "el perfil no contiene datos de la cuenta");

        ListaPerfiles cargada = new ListaPerfiles();
        assertEquals(1, new GestorArchivos(carpeta).cargarPerfiles(cargada, usuarios));
        Perfil leido = cargada.buscar("1");
        assertEquals("Hola,\nsoy Ana. ¡Me gusta el café!", leido.obtener(CampoPerfil.BIOGRAFIA));
        assertEquals("@ana.mora", leido.obtener(CampoPerfil.INSTAGRAM));
        assertNull(leido.obtener(CampoPerfil.CARRERA));
    }

    @Test
    void siNoExisteElArchivoSeCreaVacioYLosUsuariosNoTienenPerfil() throws Exception {
        ListaPerfiles destino = new ListaPerfiles();
        assertEquals(0, gestor.cargarPerfiles(destino, usuarios));
        assertEquals("[]", Files.readString(gestor.getRutaPerfiles(), StandardCharsets.UTF_8));
        assertNull(destino.buscar("1"));
    }

    @Test
    void seIgnoranRegistrosInvalidosDeUsuariosInexistentesYRepetidos() throws Exception {
        Files.writeString(gestor.getRutaPerfiles(), "["
                + "{\"idUsuario\":\"1\",\"carrera\":\"Sistemas\"},"
                + "{\"idUsuario\":\"1\",\"carrera\":\"Repetido\"},"             // repetido
                + "{\"idUsuario\":\"999\",\"carrera\":\"Fantasma\"},"           // usuario inexistente
                + "{\"idUsuario\":\"2\",\"instagram\":\"con espacios\"},"       // enlace inválido
                + "{\"carrera\":\"Sin dueño\"},"                                 // sin ID
                + "{\"idUsuario\":\"2\"},"                                       // vacío
                + "{\"idUsuario\":\"2\",\"campoDesconocido\":\"x\",\"sede\":\"Liberia\"},"
                + "null]", StandardCharsets.UTF_8);

        ListaPerfiles destino = new ListaPerfiles();
        assertEquals(2, gestor.cargarPerfiles(destino, usuarios));
        assertEquals("Sistemas", destino.buscar("1").obtener(CampoPerfil.CARRERA));
        assertEquals("Liberia", destino.buscar("2").obtener(CampoPerfil.SEDE),
                "un campo desconocido (versión futura) no invalida el perfil");
    }

    @Test
    void unArchivoDanadoNoModificaLaMemoriaYBloqueaElGuardado() throws Exception {
        Files.writeString(gestor.getRutaPerfiles(), "{ roto", StandardCharsets.UTF_8);
        ListaPerfiles destino = new ListaPerfiles();
        Perfil p = new Perfil("1");
        p.establecer(CampoPerfil.CARRERA, "Sistemas");
        destino.guardar(p);

        assertThrows(PersistenciaException.class, () -> gestor.cargarPerfiles(destino, usuarios));

        assertEquals("Sistemas", destino.buscar("1").obtener(CampoPerfil.CARRERA));
        assertTrue(gestor.isEscrituraPerfilesBloqueada());
        assertThrows(PersistenciaException.class, () -> gestor.guardarPerfiles(destino));
        assertEquals("{ roto", Files.readString(gestor.getRutaPerfiles(), StandardCharsets.UTF_8));
    }
}
