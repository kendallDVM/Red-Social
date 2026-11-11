package socialuna.servicios;

import static socialuna.UtilPruebas.hash;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import socialuna.datos.GestorArchivos;
import socialuna.datos.ListaDinamica;
import socialuna.datos.ListaPublicaciones;
import socialuna.datos.ListaUsuarios;
import socialuna.datos.PersistenciaException;
import socialuna.modelo.Publicacion;
import socialuna.modelo.Usuario;

/** Usa una carpeta temporal: nunca toca el data/ real del proyecto. */
class ServicioPublicacionesTest {

    @TempDir
    Path carpetaTemporal;

    private ContextoAplicacion contexto;
    private ServicioPublicaciones servicio;
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
        servicio = new ServicioPublicaciones(contexto, new ServicioAmistades(contexto));
    }

    private static LocalDateTime fecha(int dia, int hora) {
        return LocalDateTime.of(2026, 11, dia, hora, 0, 0);
    }

    // ------------------------------------------------------------------ publicar

    @Test
    void publicarAgregaLaPublicacionConAutorYFechaActual() {
        LocalDateTime antes = LocalDateTime.now().withNano(0);

        ResultadoPublicacion r = servicio.publicar("1", "Hola a todos");

        assertTrue(r.esExitoso());
        Publicacion p = r.getPublicacion();
        assertNotNull(p);
        assertEquals("1", p.getIdAutor());
        assertEquals("Hola a todos", p.getTexto());
        assertFalse(p.getFechaHoraComoFecha().isBefore(antes));
        assertEquals(1, contexto.getPublicaciones().tamano());
    }

    @Test
    void publicarQuitaEspaciosDeLosExtremos() {
        ResultadoPublicacion r = servicio.publicar("1", "   Hola   \n");

        assertEquals("Hola", r.getPublicacion().getTexto());
    }

    @Test
    void publicarAsignaIdsDistintosYSeguidos() {
        String id1 = servicio.publicar("1", "uno").getPublicacion().getId();
        String id2 = servicio.publicar("2", "dos").getPublicacion().getId();

        assertEquals("P1", id1);
        assertEquals("P2", id2);
    }

    @Test
    void publicarGuardaEnPublicacionesJsonAlInstante() throws Exception {
        servicio.publicar("1", "Mensaje guardado ¡con tilde!");

        String json = Files.readString(carpetaTemporal.resolve(GestorArchivos.ARCHIVO_PUBLICACIONES),
                StandardCharsets.UTF_8);
        assertTrue(json.contains("Mensaje guardado ¡con tilde!"));

        // Y un programa recién abierto lo recupera
        ListaPublicaciones recargadas = new ListaPublicaciones();
        new GestorArchivos(carpetaTemporal).cargarPublicaciones(recargadas, contexto.getUsuarios());
        assertEquals(1, recargadas.tamano());
    }

    @Test
    void textoVacioONuloOSoloEspaciosSeRechaza() {
        for (String texto : new String[] {null, "", "   ", "\n\t "}) {
            ResultadoPublicacion r = servicio.publicar("1", texto);

            assertEquals(ResultadoPublicacion.Tipo.TEXTO_VACIO, r.getTipo());
            assertNull(r.getPublicacion());
        }
        assertTrue(contexto.getPublicaciones().estaVacia());
    }

    @Test
    void textoMuyLargoSeRechazaPeroElLimiteExactoSePermite() {
        String exacto = "a".repeat(ServicioPublicaciones.MAX_CARACTERES);
        String excedido = "a".repeat(ServicioPublicaciones.MAX_CARACTERES + 1);

        assertEquals(ResultadoPublicacion.Tipo.TEXTO_MUY_LARGO, servicio.publicar("1", excedido).getTipo());
        assertTrue(servicio.publicar("1", exacto).esExitoso());
        assertEquals(1, contexto.getPublicaciones().tamano());
    }

    @Test
    void sinSesionOUsuarioInexistenteSeRechaza() {
        assertEquals(ResultadoPublicacion.Tipo.SIN_SESION, servicio.publicar(null, "hola").getTipo());
        assertEquals(ResultadoPublicacion.Tipo.SIN_SESION, servicio.publicar(" ", "hola").getTipo());
        assertEquals(ResultadoPublicacion.Tipo.SIN_SESION, servicio.publicar("99", "hola").getTipo());
        assertTrue(contexto.getPublicaciones().estaVacia());
    }

    @Test
    void siNoSePuedeGuardarLaPublicacionNoQuedaEnMemoria() throws Exception {
        // Un publicaciones.json dañado bloquea el guardado (protección de GestorArchivos).
        Files.writeString(carpetaTemporal.resolve(GestorArchivos.ARCHIVO_PUBLICACIONES), "[ roto",
                StandardCharsets.UTF_8);
        assertThrows(PersistenciaException.class,
                () -> contexto.getGestorArchivos().cargarPublicaciones(new ListaPublicaciones(), new ListaUsuarios()));

        ResultadoPublicacion r = servicio.publicar("1", "No se guarda");

        assertEquals(ResultadoPublicacion.Tipo.ERROR_PERSISTENCIA, r.getTipo());
        assertTrue(contexto.getPublicaciones().estaVacia());
    }

    // ------------------------------------------------------------------ muro general

    @Test
    void muroGeneralMuestraPropiasYDeAmigosDeLaMasRecienteALaMasAntigua() {
        contexto.getAmistades().agregarAmistad("1", "2");
        servicio.publicar("1", "ana 1", fecha(1, 9));
        servicio.publicar("2", "luis 1", fecha(2, 9));
        servicio.publicar("1", "ana 2", fecha(3, 9));

        ListaDinamica<Publicacion> muro = servicio.obtenerMuro("1");

        assertEquals(3, muro.tamano());
        assertEquals("ana 2", muro.obtener(0).getTexto());
        assertEquals("luis 1", muro.obtener(1).getTexto());
        assertEquals("ana 1", muro.obtener(2).getTexto());
    }

    @Test
    void visualizacionCruzadaLaAmistadSeVeEnAmbosSentidos() {
        contexto.getAmistades().agregarAmistad("1", "2");
        servicio.publicar("1", "de ana", fecha(1, 9));
        servicio.publicar("2", "de luis", fecha(2, 9));

        // A ve a B y B ve a A
        assertEquals(2, servicio.obtenerMuro("1").tamano());
        assertEquals(2, servicio.obtenerMuro("2").tamano());
        assertEquals("de luis", servicio.obtenerMuro("1").obtener(0).getTexto());
        assertEquals("de luis", servicio.obtenerMuro("2").obtener(0).getTexto());
    }

    @Test
    void unUsuarioSinAmistadNoVePublicacionesAjenas() {
        contexto.getAmistades().agregarAmistad("1", "2");
        servicio.publicar("1", "de ana", fecha(1, 9));
        servicio.publicar("2", "de luis", fecha(2, 9));
        servicio.publicar("3", "de sofia", fecha(3, 9));

        ListaDinamica<Publicacion> muroDeSofia = servicio.obtenerMuro("3");

        assertEquals(1, muroDeSofia.tamano());
        assertEquals("de sofia", muroDeSofia.obtener(0).getTexto());
        // y las de Sofía tampoco aparecen en el muro de Ana
        for (Publicacion p : servicio.obtenerMuro("1")) {
            assertFalse(p.getIdAutor().equals("3"));
        }
    }

    @Test
    void laAmistadNoEsTransitiva() {
        // 1 es amigo de 2 y 2 es amigo de 3, pero 1 y 3 no se conocen
        contexto.getAmistades().agregarAmistad("1", "2");
        contexto.getAmistades().agregarAmistad("2", "3");
        servicio.publicar("3", "de sofia", fecha(1, 9));

        assertTrue(servicio.obtenerMuro("1").estaVacia());
        assertEquals(1, servicio.obtenerMuro("2").tamano());
    }

    @Test
    void alAgregarOEliminarUnAmigoElMuroSeActualizaSinReiniciar() {
        servicio.publicar("2", "de luis", fecha(1, 9));
        assertTrue(servicio.obtenerMuro("1").estaVacia());

        contexto.getAmistades().agregarAmistad("1", "2");
        assertEquals(1, servicio.obtenerMuro("1").tamano());

        contexto.getAmistades().eliminarAmistad("1", "2");
        assertTrue(servicio.obtenerMuro("1").estaVacia());
    }

    @Test
    void muroSinPublicacionesOUsuarioNuloEstaVacio() {
        assertTrue(servicio.obtenerMuro("1").estaVacia());
        assertTrue(servicio.obtenerMuro(null).estaVacia());
        assertTrue(servicio.obtenerMuroPersonal(null).estaVacia());
    }

    // ------------------------------------------------------------------ mi muro

    @Test
    void miMuroSoloMuestraLasPublicacionesPropiasOrdenadas() {
        contexto.getAmistades().agregarAmistad("1", "2");
        servicio.publicar("1", "ana 1", fecha(1, 9));
        servicio.publicar("2", "luis 1", fecha(2, 9));
        servicio.publicar("1", "ana 2", fecha(3, 9));

        ListaDinamica<Publicacion> miMuro = servicio.obtenerMuroPersonal("1");

        assertEquals(2, miMuro.tamano());
        assertEquals("ana 2", miMuro.obtener(0).getTexto());
        assertEquals("ana 1", miMuro.obtener(1).getTexto());
    }

    // ------------------------------------------------------------------ autor

    @Test
    void obtenerAutorUsaLaListaDeUsuarios() {
        Publicacion p = servicio.publicar("2", "hola").getPublicacion();

        assertSame(luis, servicio.obtenerAutor(p));
        assertNull(servicio.obtenerAutor(null));
    }

    @Test
    void obtenerAutorDevuelveNullSiYaNoExiste() {
        Publicacion p = new Publicacion("P9", "77", "huérfana", fecha(1, 9));

        assertNull(servicio.obtenerAutor(p));
    }

    // ------------------------------------------------------------------ varias instancias

    /** Simula una segunda instancia del programa: memoria propia, misma carpeta data/. */
    private ServicioPublicaciones otraInstancia(ContextoAplicacion[] contextoDeSalida) {
        ContextoAplicacion otro = new ContextoAplicacion(new GestorArchivos(carpetaTemporal));
        otro.getUsuarios().agregar(ana);
        otro.getUsuarios().agregar(luis);
        otro.getUsuarios().agregar(sofia);
        contextoDeSalida[0] = otro;
        return new ServicioPublicaciones(otro, new ServicioAmistades(otro));
    }

    @Test
    void publicarIncluyeLoQuePublicoOtraInstanciaYNoRepiteIds() throws Exception {
        ContextoAplicacion[] otro = new ContextoAplicacion[1];
        ServicioPublicaciones servicioB = otraInstancia(otro);

        String idA = servicio.publicar("1", "de la instancia A").getPublicacion().getId();
        // La instancia B no sabe de la publicación de A y publica la suya
        String idB = servicioB.publicar("2", "de la instancia B").getPublicacion().getId();

        assertEquals("P1", idA);
        assertEquals("P2", idB);
        assertEquals(2, otro[0].getPublicaciones().tamano());

        ListaPublicaciones enDisco = new ListaPublicaciones();
        new GestorArchivos(carpetaTemporal).cargarPublicaciones(enDisco, contexto.getUsuarios());
        assertEquals(2, enDisco.tamano()); // ninguna se perdió
    }

    @Test
    void cerrarLaAplicacionNoBorraPublicacionesDeOtraInstancia() throws Exception {
        ContextoAplicacion[] otro = new ContextoAplicacion[1];
        ServicioPublicaciones servicioB = otraInstancia(otro);
        servicio.publicar("1", "de A");
        servicioB.publicar("2", "de B"); // A todavía no la tiene en memoria

        contexto.guardarTodo(); // A cierra con una memoria vieja

        ListaPublicaciones enDisco = new ListaPublicaciones();
        new GestorArchivos(carpetaTemporal).cargarPublicaciones(enDisco, contexto.getUsuarios());
        assertEquals(2, enDisco.tamano());
        assertEquals(2, contexto.getPublicaciones().tamano());
    }

    @Test
    void actualizarDesdeDiscoIncorporaPublicacionesDeOtraInstancia() {
        ContextoAplicacion[] otro = new ContextoAplicacion[1];
        ServicioPublicaciones servicioB = otraInstancia(otro);
        servicioB.publicar("1", "publicada en B");
        assertTrue(contexto.getPublicaciones().estaVacia());

        assertTrue(servicio.actualizarDesdeDisco());

        assertEquals(1, contexto.getPublicaciones().tamano());
        assertEquals("publicada en B", servicio.obtenerMuroPersonal("1").obtener(0).getTexto());
    }

    // ------------------------------------------------------------------ me gusta

    /** Hace amigos a 1 y 2 de forma persistente (en memoria y en amistades.json). */
    private void hacerAmigosAnaYLuis() {
        assertTrue(new ServicioAmistades(contexto).agregarAmigo("1", "2").esExitoso());
    }

    @Test
    void puedeDarMeGustaASuPropiaPublicacion() {
        String id = servicio.publicar("1", "hola").getPublicacion().getId();

        ResultadoPublicacion r = servicio.alternarMeGusta("1", id);

        assertTrue(r.esExitoso());
        assertTrue(r.getPublicacion().leGustaA("1"));
        assertEquals(1, r.getPublicacion().cantidadMeGusta());
    }

    @Test
    void puedeDarMeGustaALaPublicacionDeUnAmigoYSeGuarda() throws Exception {
        hacerAmigosAnaYLuis();
        String id = servicio.publicar("1", "hola").getPublicacion().getId();

        assertTrue(servicio.alternarMeGusta("2", id).esExitoso());

        ListaPublicaciones enDisco = new ListaPublicaciones();
        new GestorArchivos(carpetaTemporal).cargarPublicaciones(enDisco, contexto.getUsuarios());
        assertTrue(enDisco.buscarPorId(id).leGustaA("2"));
    }

    @Test
    void alternarDosVecesQuitaElMeGusta() throws Exception {
        String id = servicio.publicar("1", "hola").getPublicacion().getId();

        ResultadoPublicacion primero = servicio.alternarMeGusta("1", id);
        ResultadoPublicacion segundo = servicio.alternarMeGusta("1", id);

        assertEquals("Te gusta esta publicación.", primero.getMensaje());
        assertEquals("Quitaste tu me gusta.", segundo.getMensaje());
        assertEquals(0, segundo.getPublicacion().cantidadMeGusta());
        ListaPublicaciones enDisco = new ListaPublicaciones();
        new GestorArchivos(carpetaTemporal).cargarPublicaciones(enDisco, contexto.getUsuarios());
        assertEquals(0, enDisco.buscarPorId(id).cantidadMeGusta());
    }

    @Test
    void unUsuarioNoPuedeDarMeGustaDosVeces() {
        String id = servicio.publicar("1", "hola").getPublicacion().getId();
        servicio.alternarMeGusta("1", id);

        servicio.alternarMeGusta("1", id); // lo quita, no suma otro

        assertEquals(0, contexto.getPublicaciones().buscarPorId(id).cantidadMeGusta());
    }

    @Test
    void variosUsuariosSumanSusMeGusta() {
        hacerAmigosAnaYLuis();
        String id = servicio.publicar("1", "hola").getPublicacion().getId();

        servicio.alternarMeGusta("1", id);
        servicio.alternarMeGusta("2", id);

        assertEquals(2, contexto.getPublicaciones().buscarPorId(id).cantidadMeGusta());
    }

    @Test
    void noPuedeDarMeGustaALaPublicacionDeAlguienQueNoEsAmigo() {
        String id = servicio.publicar("1", "hola").getPublicacion().getId();

        ResultadoPublicacion r = servicio.alternarMeGusta("3", id);

        assertEquals(ResultadoPublicacion.Tipo.NO_PERMITIDO, r.getTipo());
        assertEquals(0, contexto.getPublicaciones().buscarPorId(id).cantidadMeGusta());
    }

    @Test
    void meGustaConPublicacionInexistenteOSinSesionSeRechaza() {
        assertEquals(ResultadoPublicacion.Tipo.PUBLICACION_NO_EXISTE,
                servicio.alternarMeGusta("1", "P99").getTipo());
        assertEquals(ResultadoPublicacion.Tipo.PUBLICACION_NO_EXISTE,
                servicio.alternarMeGusta("1", null).getTipo());
        assertEquals(ResultadoPublicacion.Tipo.SIN_SESION, servicio.alternarMeGusta(null, "P1").getTipo());
        assertEquals(ResultadoPublicacion.Tipo.SIN_SESION, servicio.alternarMeGusta("99", "P1").getTipo());
    }

    @Test
    void siNoSePuedeLeerElArchivoElMeGustaNoCambiaNada() throws Exception {
        String id = servicio.publicar("1", "hola").getPublicacion().getId();
        Files.writeString(carpetaTemporal.resolve(GestorArchivos.ARCHIVO_PUBLICACIONES), "[ roto",
                StandardCharsets.UTF_8);

        ResultadoPublicacion r = servicio.alternarMeGusta("1", id);

        assertEquals(ResultadoPublicacion.Tipo.ERROR_PERSISTENCIA, r.getTipo());
        assertEquals(0, contexto.getPublicaciones().buscarPorId(id).cantidadMeGusta());
    }

    @Test
    void losMeGustaDeDosInstanciasSeSumanYUnoQuitadoNoReaparece() {
        hacerAmigosAnaYLuis();
        ContextoAplicacion[] otro = new ContextoAplicacion[1];
        ServicioPublicaciones servicioB = otraInstancia(otro);
        String id = servicio.publicar("1", "hola").getPublicacion().getId();

        servicio.alternarMeGusta("1", id);   // instancia A: a Ana le gusta
        servicioB.alternarMeGusta("2", id);  // instancia B: a Luis le gusta (ve el de Ana)
        assertEquals(2, otro[0].getPublicaciones().buscarPorId(id).cantidadMeGusta());

        servicio.alternarMeGusta("1", id);   // instancia A: Ana lo quita
        servicioB.actualizarDesdeDisco();    // instancia B se actualiza

        Publicacion enB = otro[0].getPublicaciones().buscarPorId(id);
        assertFalse(enB.leGustaA("1")); // no reaparece
        assertTrue(enB.leGustaA("2"));
        assertEquals(1, enB.cantidadMeGusta());
    }

    @Test
    void obtenerNombresMeGustaEnElOrdenEnQueSeDieron() {
        hacerAmigosAnaYLuis();
        Publicacion p = servicio.publicar("1", "hola").getPublicacion();
        servicio.alternarMeGusta("2", p.getId());
        servicio.alternarMeGusta("1", p.getId());

        ListaDinamica<String> nombres = servicio.obtenerNombresMeGusta(p);

        assertEquals(2, nombres.tamano());
        assertEquals("Luis Peña", nombres.obtener(0));
        assertEquals("Ana Mora", nombres.obtener(1));
        assertTrue(servicio.obtenerNombresMeGusta(null).estaVacia());
    }
}
