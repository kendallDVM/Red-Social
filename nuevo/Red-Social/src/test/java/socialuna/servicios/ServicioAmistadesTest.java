package socialuna.servicios;

import static socialuna.UtilPruebas.hash;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import socialuna.datos.GestorArchivos;
import socialuna.datos.ListaDinamica;
import socialuna.datos.ListaUsuarios;
import socialuna.datos.PersistenciaException;
import socialuna.datos.RedAmistades;
import socialuna.modelo.Usuario;

/** Usa una carpeta temporal: nunca toca el data/ real del proyecto. */
class ServicioAmistadesTest {

    @TempDir
    Path carpetaTemporal;

    private ContextoAplicacion contexto;
    private ServicioAmistades servicio;
    private Usuario ana;
    private Usuario luis;
    private Usuario sofia;

    @BeforeEach
    void preparar() {
        contexto = new ContextoAplicacion(new GestorArchivos(carpetaTemporal));
        ana = new Usuario("1", "Ana Mora", "ana", hash("clave1"));
        luis = new Usuario("2", "Luis Peña", "luisp", hash("clave2"));
        sofia = new Usuario("3", "Sofía Rojas", "sofi", hash("clave3"));
        contexto.getUsuarios().agregar(ana);
        contexto.getUsuarios().agregar(luis);
        contexto.getUsuarios().agregar(sofia);
        servicio = new ServicioAmistades(contexto);
    }

    // ------------------------------------------------------------------ búsqueda

    @Test
    void buscarEncuentraPorCoincidenciaParcialSinMayusculas() {
        ListaDinamica<Usuario> r = servicio.buscarUsuarios("1", "LUI");

        assertEquals(1, r.tamano());
        assertSame(luis, r.obtener(0));
    }

    @Test
    void buscarPorNombreDeUsuarioYSinTildes() {
        assertSame(sofia, servicio.buscarUsuarios("1", "sofia").obtener(0));
        assertSame(sofia, servicio.buscarUsuarios("1", "sofi").obtener(0));
        assertSame(luis, servicio.buscarUsuarios("1", "pena").obtener(0));
    }

    @Test
    void buscarExcluyeAlUsuarioActivo() {
        ListaDinamica<Usuario> r = servicio.buscarUsuarios("1", "a"); // "Ana", "Luis Peña", "Sofía Rojas"

        assertFalse(r.contiene(ana));
        assertTrue(r.contiene(luis));
        assertTrue(r.contiene(sofia));
    }

    @Test
    void buscarConTextoVacioDevuelveListaVacia() {
        assertTrue(servicio.buscarUsuarios("1", "").estaVacia());
        assertTrue(servicio.buscarUsuarios("1", "   ").estaVacia());
        assertTrue(servicio.buscarUsuarios("1", null).estaVacia());
    }

    @Test
    void buscarSinCoincidenciasDevuelveListaVacia() {
        assertTrue(servicio.buscarUsuarios("1", "zzz").estaVacia());
    }

    // ------------------------------------------------------------------ agregar amigo

    @Test
    void agregarAmigoLoGuardaEnAmbosSentidosYEnElArchivo() throws Exception {
        ResultadoAmistad r = servicio.agregarAmigo("1", "2");

        assertTrue(r.esExitoso(), r.toString());
        assertSame(luis, r.getAmigo());
        assertTrue(servicio.sonAmigos("1", "2"));
        assertTrue(servicio.sonAmigos("2", "1"));

        // Se guardó en ese momento: otro gestor lo lee del archivo.
        RedAmistades leida = new RedAmistades();
        new GestorArchivos(carpetaTemporal).cargarAmistades(leida, contexto.getUsuarios());
        assertTrue(leida.sonAmigos("1", "2"));
    }

    @Test
    void noSePuedeAgregarDosVeces() {
        servicio.agregarAmigo("1", "2");

        assertEquals(ResultadoAmistad.Tipo.YA_SON_AMIGOS, servicio.agregarAmigo("1", "2").getTipo());
        assertEquals(ResultadoAmistad.Tipo.YA_SON_AMIGOS, servicio.agregarAmigo("2", "1").getTipo());
    }

    @Test
    void noSePuedeAgregarASiMismo() {
        assertEquals(ResultadoAmistad.Tipo.MISMO_USUARIO, servicio.agregarAmigo("1", "1").getTipo());
    }

    @Test
    void noSePuedeAgregarAUnUsuarioInexistente() {
        assertEquals(ResultadoAmistad.Tipo.USUARIO_NO_EXISTE, servicio.agregarAmigo("1", "999").getTipo());
        assertFalse(contexto.getAmistades().contieneUsuario("999"));
    }

    @Test
    void sinSeleccionOSinSesionSeAvisa() {
        assertEquals(ResultadoAmistad.Tipo.SIN_SELECCION, servicio.agregarAmigo("1", null).getTipo());
        assertEquals(ResultadoAmistad.Tipo.SIN_SELECCION, servicio.agregarAmigo("1", " ").getTipo());
        assertEquals(ResultadoAmistad.Tipo.SIN_SESION, servicio.agregarAmigo(null, "2").getTipo());
    }

    @Test
    void siNoSePuedeGuardarLaAmistadNoQuedaEnMemoria() throws Exception {
        // Un amistades.json dañado bloquea el guardado (protección de GestorArchivos).
        Files.writeString(carpetaTemporal.resolve(GestorArchivos.ARCHIVO_AMISTADES), "[ roto",
                StandardCharsets.UTF_8);
        assertThrows(PersistenciaException.class,
                () -> contexto.getGestorArchivos().cargarAmistades(new RedAmistades(), new ListaUsuarios()));

        ResultadoAmistad r = servicio.agregarAmigo("1", "2");

        assertEquals(ResultadoAmistad.Tipo.ERROR_PERSISTENCIA, r.getTipo());
        assertFalse(servicio.sonAmigos("1", "2"));
    }

    // ------------------------------------------------------------------ eliminar amigo

    @Test
    void eliminarAmigoQuitaAmbosSentidos() {
        servicio.agregarAmigo("1", "2");

        ResultadoAmistad r = servicio.eliminarAmigo("2", "1");

        assertTrue(r.esExitoso(), r.toString());
        assertFalse(servicio.sonAmigos("1", "2"));
        assertEquals(ResultadoAmistad.Tipo.NO_SON_AMIGOS, servicio.eliminarAmigo("1", "2").getTipo());
    }

    // ------------------------------------------------------------------ consultas

    @Test
    void obtenerAmigosDevuelveObjetosUsuario() {
        servicio.agregarAmigo("1", "2");
        servicio.agregarAmigo("3", "1");

        ListaDinamica<Usuario> amigosDeAna = servicio.obtenerAmigos("1");

        assertEquals(2, amigosDeAna.tamano());
        assertTrue(amigosDeAna.contiene(luis));
        assertTrue(amigosDeAna.contiene(sofia));
        assertSame(ana, servicio.obtenerAmigos("2").obtener(0), "el amigo también la ve");
    }

    @Test
    void obtenerAmigosOmiteIdsQueYaNoExisten() {
        contexto.getAmistades().agregarAmistad("1", "999");

        assertTrue(servicio.obtenerAmigos("1").estaVacia());
    }

    @Test
    void obtenerIdsVisiblesIncluyeAlUsuarioYSusAmigos() {
        servicio.agregarAmigo("1", "2");

        ListaDinamica<String> visiblesAna = servicio.obtenerIdsVisibles("1");
        ListaDinamica<String> visiblesSofia = servicio.obtenerIdsVisibles("3");

        assertEquals(2, visiblesAna.tamano());
        assertTrue(visiblesAna.contiene("1"));
        assertTrue(visiblesAna.contiene("2"));
        assertEquals(1, visiblesSofia.tamano(), "sin amigos solo se ve a sí misma");
        assertTrue(visiblesSofia.contiene("3"));
    }

    @Test
    void laAmistadSobreviveAReabrirLaAplicacion() throws Exception {
        servicio.agregarAmigo("1", "3");
        contexto.getGestorArchivos().guardarUsuarios(contexto.getUsuarios());

        ContextoAplicacion reabierto = new ContextoAplicacion(new GestorArchivos(carpetaTemporal));
        reabierto.cargarTodo();

        ServicioAmistades servicioReabierto = new ServicioAmistades(reabierto);
        assertTrue(servicioReabierto.sonAmigos("3", "1"));
        assertEquals("Ana Mora", servicioReabierto.obtenerAmigos("3").obtener(0).getNombre());
    }
}
