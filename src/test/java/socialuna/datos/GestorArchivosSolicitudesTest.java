package socialuna.datos;

import static socialuna.UtilPruebas.hash;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import socialuna.modelo.Usuario;

/** Persistencia de solicitudes.json. Usa una carpeta temporal: nunca toca el data/ real. */
class GestorArchivosSolicitudesTest {

    @TempDir
    Path carpeta;

    private GestorArchivos gestor;
    private ListaUsuarios usuarios;
    private RedAmistades amistades;

    @BeforeEach
    void preparar() {
        gestor = new GestorArchivos(carpeta);
        usuarios = new ListaUsuarios();
        usuarios.agregar(new Usuario("1", "Ana", "ana", hash("a")));
        usuarios.agregar(new Usuario("2", "Luis", "luis", hash("b")));
        usuarios.agregar(new Usuario("3", "Sofía", "sofi", hash("c")));
        amistades = new RedAmistades();
    }

    @Test
    void guardarYCargarConservaLasSolicitudesYSuSentido() throws Exception {
        ListaSolicitudes original = new ListaSolicitudes();
        original.agregar("1", "2");
        original.agregar("3", "1");
        gestor.guardarSolicitudes(original);

        ListaSolicitudes cargada = new ListaSolicitudes();
        int cantidad = new GestorArchivos(carpeta).cargarSolicitudes(cargada, usuarios, amistades);

        assertEquals(2, cantidad);
        assertTrue(cargada.existe("1", "2"));
        assertTrue(cargada.existe("3", "1"));
        assertFalse(cargada.existe("2", "1"));
    }

    @Test
    void siNoExisteElArchivoSeCreaVacio() throws Exception {
        ListaSolicitudes destino = new ListaSolicitudes();

        assertEquals(0, gestor.cargarSolicitudes(destino, usuarios, amistades));
        assertEquals("[]", Files.readString(gestor.getRutaSolicitudes(), StandardCharsets.UTF_8));
    }

    @Test
    void seIgnoranRegistrosInvalidosDuplicadosCruzadosYEntreAmigos() throws Exception {
        amistades.agregarAmistad("2", "3");
        Files.writeString(gestor.getRutaSolicitudes(), "["
                + "{\"idRemitente\":\"1\",\"idDestinatario\":\"2\"},"
                + "{\"idRemitente\":\"1\",\"idDestinatario\":\"2\"},"     // duplicada
                + "{\"idRemitente\":\"2\",\"idDestinatario\":\"1\"},"     // cruzada
                + "{\"idRemitente\":\"1\",\"idDestinatario\":\"1\"},"     // a sí mismo
                + "{\"idRemitente\":\"1\",\"idDestinatario\":\"999\"},"   // usuario inexistente
                + "{\"idRemitente\":\"2\",\"idDestinatario\":\"3\"},"     // ya son amigos
                + "{\"idRemitente\":\"3\"},"                              // incompleta
                + "null]", StandardCharsets.UTF_8);

        ListaSolicitudes destino = new ListaSolicitudes();
        assertEquals(1, gestor.cargarSolicitudes(destino, usuarios, amistades));
        assertTrue(destino.existe("1", "2"));
    }

    @Test
    void unArchivoDanadoNoModificaLaMemoriaYBloqueaElGuardado() throws Exception {
        Files.writeString(gestor.getRutaSolicitudes(), "[ roto", StandardCharsets.UTF_8);
        ListaSolicitudes destino = new ListaSolicitudes();
        destino.agregar("1", "3");

        assertThrows(PersistenciaException.class, () -> gestor.cargarSolicitudes(destino, usuarios, amistades));

        assertTrue(destino.existe("1", "3"), "la memoria no cambia");
        assertTrue(gestor.isEscrituraSolicitudesBloqueada());
        assertThrows(PersistenciaException.class, () -> gestor.guardarSolicitudes(destino));
        assertEquals("[ roto", Files.readString(gestor.getRutaSolicitudes(), StandardCharsets.UTF_8),
                "el archivo original se conserva");
        try (var archivos = Files.list(carpeta)) {
            assertTrue(archivos.anyMatch(p -> p.getFileName().toString().startsWith("solicitudes.json.corrupto-")),
                    "se crea una copia de respaldo");
        }
    }
}
