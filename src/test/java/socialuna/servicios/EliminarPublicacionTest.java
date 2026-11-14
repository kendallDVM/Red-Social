package socialuna.servicios;

import static socialuna.UtilPruebas.hash;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import socialuna.datos.GestorArchivos;
import socialuna.datos.ListaPublicaciones;
import socialuna.modelo.Usuario;

/** Eliminar publicaciones. Usa una carpeta temporal: nunca toca el data/ real del proyecto. */
class EliminarPublicacionTest {

    @TempDir
    Path carpetaTemporal;

    private ContextoAplicacion contexto;
    private ServicioPublicaciones servicio;
    private Usuario ana;
    private Usuario luis;

    @BeforeEach
    void preparar() throws Exception {
        contexto = nuevoContexto();
        contexto.getGestorArchivos().guardarUsuarios(contexto.getUsuarios()); // para poder reabrir
        servicio = new ServicioPublicaciones(contexto, new ServicioAmistades(contexto));
        // Ana (1) y Luis (2) son amigos
        assertTrue(new ServicioAmistades(contexto).agregarAmigo("1", "2").esExitoso());
    }

    /** Contexto con los mismos usuarios y la misma carpeta (sirve para simular otra instancia). */
    private ContextoAplicacion nuevoContexto() {
        ContextoAplicacion c = new ContextoAplicacion(new GestorArchivos(carpetaTemporal));
        if (ana == null) {
            ana = new Usuario("1", "Ana Mora", "ana", hash("clave1"));
            luis = new Usuario("2", "Luis Peña", "luisp", hash("clave2"));
        }
        c.getUsuarios().agregar(ana);
        c.getUsuarios().agregar(luis);
        return c;
    }

    private String archivo() throws Exception {
        return Files.readString(carpetaTemporal.resolve(GestorArchivos.ARCHIVO_PUBLICACIONES), StandardCharsets.UTF_8);
    }

    @Test
    void elAutorLaEliminaYDesapareceDeSuMuroYDelDeSusAmigos() {
        String id = servicio.publicar("1", "me equivoqué").getPublicacion().getId();
        assertEquals(1, servicio.obtenerMuro("2").tamano());

        ResultadoPublicacion r = servicio.eliminarPublicacion("1", id);

        assertTrue(r.esExitoso(), r.toString());
        assertTrue(servicio.obtenerMuro("1").estaVacia());
        assertTrue(servicio.obtenerMuroPersonal("1").estaVacia());
        assertTrue(servicio.obtenerMuro("2").estaVacia(), "el amigo tampoco la ve");
    }

    @Test
    void seGuardaAlInstanteYSigueEliminadaAlReabrir() throws Exception {
        String id = servicio.publicar("1", "hola").getPublicacion().getId();

        servicio.eliminarPublicacion("1", id);

        assertTrue(archivo().contains("\"eliminada\": true"));
        ContextoAplicacion reabierto = nuevoContexto();
        reabierto.cargarTodo();
        ServicioPublicaciones servicioReabierto = new ServicioPublicaciones(reabierto, new ServicioAmistades(reabierto));
        assertEquals(1, reabierto.getPublicaciones().tamano(), "se conserva en el archivo, marcada");
        assertTrue(servicioReabierto.obtenerMuro("1").estaVacia());
        assertTrue(servicioReabierto.obtenerMuro("2").estaVacia());
    }

    @Test
    void lasPublicacionesNormalesNoEscribenElCampoEliminada() throws Exception {
        servicio.publicar("1", "hola");

        assertFalse(archivo().contains("eliminada"));
    }

    @Test
    void noSePuedeEliminarUnaPublicacionAjena() {
        String id = servicio.publicar("1", "de Ana").getPublicacion().getId();

        ResultadoPublicacion r = servicio.eliminarPublicacion("2", id);

        assertEquals(ResultadoPublicacion.Tipo.NO_PERMITIDO, r.getTipo());
        assertEquals(1, servicio.obtenerMuro("2").tamano(), "sigue visible");
    }

    @Test
    void eliminarDosVecesOUnaInexistenteAvisa() {
        String id = servicio.publicar("1", "hola").getPublicacion().getId();
        servicio.eliminarPublicacion("1", id);

        assertEquals(ResultadoPublicacion.Tipo.PUBLICACION_NO_EXISTE, servicio.eliminarPublicacion("1", id).getTipo());
        assertEquals(ResultadoPublicacion.Tipo.PUBLICACION_NO_EXISTE, servicio.eliminarPublicacion("1", "P99").getTipo());
    }

    @Test
    void sinSesionNoSeElimina() {
        String id = servicio.publicar("1", "hola").getPublicacion().getId();

        assertEquals(ResultadoPublicacion.Tipo.SIN_SESION, servicio.eliminarPublicacion(null, id).getTipo());
        assertEquals(ResultadoPublicacion.Tipo.SIN_SESION, servicio.eliminarPublicacion("999", id).getTipo());
        assertEquals(1, servicio.obtenerMuro("1").tamano());
    }

    @Test
    void noSePuedeDarMeGustaAUnaPublicacionEliminada() {
        String id = servicio.publicar("1", "hola").getPublicacion().getId();
        servicio.eliminarPublicacion("1", id);

        assertEquals(ResultadoPublicacion.Tipo.PUBLICACION_NO_EXISTE, servicio.alternarMeGusta("2", id).getTipo());
    }

    @Test
    void losIdsNoSeReutilizanDespuesDeEliminar() {
        String id1 = servicio.publicar("1", "uno").getPublicacion().getId();
        servicio.eliminarPublicacion("1", id1);

        String id2 = servicio.publicar("1", "dos").getPublicacion().getId();

        assertEquals("P1", id1);
        assertEquals("P2", id2);
    }

    @Test
    void siNoSePuedenLeerLosDatosNoSeEliminaNada() throws Exception {
        String id = servicio.publicar("1", "hola").getPublicacion().getId();
        Files.writeString(carpetaTemporal.resolve(GestorArchivos.ARCHIVO_PUBLICACIONES), "[ roto", StandardCharsets.UTF_8);

        ResultadoPublicacion r = servicio.eliminarPublicacion("1", id);

        assertEquals(ResultadoPublicacion.Tipo.ERROR_PERSISTENCIA, r.getTipo());
        assertEquals(1, servicio.obtenerMuro("1").tamano(), "sigue visible");
    }

    @Test
    void otraInstanciaAbiertaNoVuelveAAgregarLaPublicacionEliminada() throws Exception {
        // B abre el programa y ya tiene la publicación en memoria
        String id = servicio.publicar("1", "hola").getPublicacion().getId();
        ContextoAplicacion otro = nuevoContexto();
        otro.cargarTodo();
        ServicioPublicaciones servicioB = new ServicioPublicaciones(otro, new ServicioAmistades(otro));
        assertEquals(1, servicioB.obtenerMuro("2").tamano());

        // A la elimina; B publica y luego cierra con su memoria vieja
        servicio.eliminarPublicacion("1", id);
        servicioB.publicar("2", "de B");
        otro.guardarTodo();

        assertTrue(archivo().contains("\"eliminada\": true"), "sigue eliminada en el archivo");
        assertEquals(1, servicioB.obtenerMuro("2").tamano(), "B solo ve su propia publicación");
        ListaPublicaciones enDisco = new ListaPublicaciones();
        new GestorArchivos(carpetaTemporal).cargarPublicaciones(enDisco, contexto.getUsuarios());
        assertTrue(enDisco.buscarPorId(id).isEliminada());
    }
}
