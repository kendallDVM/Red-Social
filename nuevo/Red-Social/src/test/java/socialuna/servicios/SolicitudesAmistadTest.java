package socialuna.servicios;

import static socialuna.UtilPruebas.COSTO_PRUEBAS;
import static socialuna.UtilPruebas.hash;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import socialuna.datos.GestorArchivos;
import socialuna.datos.ListaDinamica;
import socialuna.modelo.Usuario;
import socialuna.servicios.ResultadoAmistad.Tipo;

/**
 * Flujo de solicitudes de amistad: enviar → pendiente → aceptar / rechazar / cancelar,
 * estados de búsqueda, persistencia y uso de dos instancias del programa sobre la misma
 * carpeta de datos (se simulan con dos {@link ContextoAplicacion} independientes).
 *
 * <p>Usa una carpeta temporal: nunca toca el data/ real del proyecto.
 */
class SolicitudesAmistadTest {

    private static final String ANA = "1";
    private static final String LUIS = "2";
    private static final String SOFIA = "3";

    @TempDir
    Path carpeta;

    private ContextoAplicacion contexto;
    private ServicioAmistades servicio;

    @BeforeEach
    void preparar() throws Exception {
        // Los usuarios se guardan en disco, como en la aplicación real.
        ContextoAplicacion inicial = new ContextoAplicacion(new GestorArchivos(carpeta));
        inicial.getUsuarios().agregar(new Usuario(ANA, "Ana Mora", "ana", hash("a")));
        inicial.getUsuarios().agregar(new Usuario(LUIS, "Luis Peña", "luisp", hash("b")));
        inicial.getUsuarios().agregar(new Usuario(SOFIA, "Sofía Rojas", "sofi", hash("c")));
        inicial.getGestorArchivos().guardarUsuarios(inicial.getUsuarios());

        contexto = abrirInstancia();
        servicio = new ServicioAmistades(contexto);
        Sesion.cerrar();
    }

    @AfterEach
    void limpiar() {
        Sesion.cerrar();
    }

    /** Simula abrir el programa: un contexto nuevo que carga todo desde la carpeta. */
    private ContextoAplicacion abrirInstancia() throws Exception {
        ContextoAplicacion c = new ContextoAplicacion(new GestorArchivos(carpeta));
        c.cargarTodo();
        return c;
    }

    // ------------------------------------------------------------------ enviar

    @Test
    void enviarUnaSolicitudNoCreaLaAmistad() {
        ResultadoAmistad r = servicio.enviarSolicitud(ANA, LUIS);

        assertTrue(r.esExitoso(), r.toString());
        assertFalse(servicio.sonAmigos(ANA, LUIS), "la solicitud no convierte en amigos");
        assertTrue(servicio.obtenerAmigos(ANA).estaVacia());
        assertEquals(EstadoAmistad.SOLICITUD_ENVIADA, servicio.obtenerEstado(ANA, LUIS));
        assertEquals(EstadoAmistad.SOLICITUD_RECIBIDA, servicio.obtenerEstado(LUIS, ANA));
    }

    @Test
    void laSolicitudSeGuardaEnElArchivoEnEseMomento() throws Exception {
        servicio.enviarSolicitud(ANA, LUIS);

        String json = Files.readString(carpeta.resolve(GestorArchivos.ARCHIVO_SOLICITUDES), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"idRemitente\": \"1\""), json);
        assertTrue(json.contains("\"idDestinatario\": \"2\""), json);
        String amistades = Files.readString(carpeta.resolve(GestorArchivos.ARCHIVO_AMISTADES), StandardCharsets.UTF_8);
        assertFalse(amistades.contains("\"2\""), "amistades.json no cambia al enviar");
    }

    @Test
    void noSePuedenEnviarSolicitudesDuplicadas() {
        servicio.enviarSolicitud(ANA, LUIS);

        assertEquals(Tipo.SOLICITUD_YA_ENVIADA, servicio.enviarSolicitud(ANA, LUIS).getTipo());
        assertEquals(1, contexto.getSolicitudes().tamano());
    }

    @Test
    void siElOtroYaEnvioUnaSolicitudNoSeCreaOtraNiSeHacenAmigos() {
        servicio.enviarSolicitud(ANA, LUIS);

        ResultadoAmistad r = servicio.enviarSolicitud(LUIS, ANA);

        assertEquals(Tipo.SOLICITUD_RECIBIDA_PENDIENTE, r.getTipo());
        assertEquals(1, contexto.getSolicitudes().tamano());
        assertTrue(contexto.getSolicitudes().existe(ANA, LUIS));
        assertFalse(servicio.sonAmigos(ANA, LUIS));
    }

    @Test
    void noSePuedeEnviarASiMismoNiAInexistentesNiAAmigos() {
        assertEquals(Tipo.MISMO_USUARIO, servicio.enviarSolicitud(ANA, ANA).getTipo());
        assertEquals(Tipo.USUARIO_NO_EXISTE, servicio.enviarSolicitud(ANA, "999").getTipo());
        assertEquals(Tipo.SIN_SESION, servicio.enviarSolicitud(null, LUIS).getTipo());
        assertEquals(Tipo.SIN_SELECCION, servicio.enviarSolicitud(ANA, null).getTipo());

        servicio.enviarSolicitud(ANA, SOFIA);
        servicio.aceptarSolicitud(SOFIA, ANA);
        assertEquals(Tipo.YA_SON_AMIGOS, servicio.enviarSolicitud(ANA, SOFIA).getTipo());
        assertEquals(Tipo.YA_SON_AMIGOS, servicio.enviarSolicitud(SOFIA, ANA).getTipo());
        assertTrue(contexto.getSolicitudes().estaVacia());
    }

    // ------------------------------------------------------------------ recibir

    @Test
    void elDestinatarioVeQuienLeEnvioLaSolicitud() {
        servicio.enviarSolicitud(ANA, LUIS);
        servicio.enviarSolicitud(SOFIA, LUIS);

        ListaDinamica<Usuario> recibidas = servicio.obtenerSolicitudesRecibidas(LUIS);

        assertEquals(2, recibidas.tamano());
        assertEquals("Ana Mora", recibidas.obtener(0).getNombre());
        assertEquals("Sofía Rojas", recibidas.obtener(1).getNombre());
        assertEquals(LUIS, servicio.obtenerSolicitudesEnviadas(ANA).obtener(0).getId());
        assertTrue(servicio.obtenerSolicitudesRecibidas(ANA).estaVacia());
    }

    // ------------------------------------------------------------------ aceptar

    @Test
    void aceptarCreaLaAmistadEnAmbosSentidosYQuitaLaSolicitud() throws Exception {
        servicio.enviarSolicitud(ANA, LUIS);

        ResultadoAmistad r = servicio.aceptarSolicitud(LUIS, ANA);

        assertTrue(r.esExitoso(), r.toString());
        assertTrue(servicio.sonAmigos(ANA, LUIS));
        assertTrue(servicio.sonAmigos(LUIS, ANA));
        assertEquals(EstadoAmistad.AMIGOS, servicio.obtenerEstado(ANA, LUIS));
        assertEquals(EstadoAmistad.AMIGOS, servicio.obtenerEstado(LUIS, ANA));
        assertTrue(servicio.obtenerSolicitudesRecibidas(LUIS).estaVacia());
        assertTrue(servicio.obtenerSolicitudesEnviadas(ANA).estaVacia());

        ContextoAplicacion reabierto = abrirInstancia();
        assertTrue(reabierto.getAmistades().sonAmigos(ANA, LUIS), "la amistad quedó guardada");
        assertTrue(reabierto.getSolicitudes().estaVacia(), "la solicitud ya no está pendiente en disco");
    }

    @Test
    void elRemitenteNoPuedeAceptarSuPropiaSolicitud() {
        servicio.enviarSolicitud(ANA, LUIS);

        assertEquals(Tipo.SOLICITUD_NO_EXISTE, servicio.aceptarSolicitud(ANA, LUIS).getTipo());
        assertFalse(servicio.sonAmigos(ANA, LUIS));
    }

    @Test
    void aceptarUnaSolicitudInexistenteNoHaceNada() {
        assertEquals(Tipo.SOLICITUD_NO_EXISTE, servicio.aceptarSolicitud(LUIS, ANA).getTipo());
        assertFalse(servicio.sonAmigos(ANA, LUIS));
    }

    // ------------------------------------------------------------------ rechazar y cancelar

    @Test
    void rechazarNoCreaAmistadYQuitaLaSolicitud() throws Exception {
        servicio.enviarSolicitud(ANA, LUIS);

        ResultadoAmistad r = servicio.rechazarSolicitud(LUIS, ANA);

        assertTrue(r.esExitoso(), r.toString());
        assertFalse(servicio.sonAmigos(ANA, LUIS));
        assertTrue(servicio.obtenerSolicitudesRecibidas(LUIS).estaVacia());
        assertTrue(servicio.obtenerAmigos(LUIS).estaVacia(), "Ana no aparece como amiga de Luis");
        assertEquals(EstadoAmistad.SIN_RELACION, servicio.obtenerEstado(ANA, LUIS));
        assertEquals(Tipo.SOLICITUD_NO_EXISTE, servicio.rechazarSolicitud(LUIS, ANA).getTipo());

        ContextoAplicacion reabierto = abrirInstancia();
        assertTrue(reabierto.getSolicitudes().estaVacia());
        assertFalse(reabierto.getAmistades().sonAmigos(ANA, LUIS));
    }

    @Test
    void despuesDeUnRechazoSePuedeVolverAEnviar() {
        servicio.enviarSolicitud(ANA, LUIS);
        servicio.rechazarSolicitud(LUIS, ANA);

        assertTrue(servicio.enviarSolicitud(ANA, LUIS).esExitoso());
        assertEquals(EstadoAmistad.SOLICITUD_ENVIADA, servicio.obtenerEstado(ANA, LUIS));
    }

    @Test
    void elRemitentePuedeCancelarSuSolicitud() {
        servicio.enviarSolicitud(ANA, LUIS);

        assertEquals(Tipo.SOLICITUD_NO_EXISTE, servicio.cancelarSolicitud(LUIS, ANA).getTipo(),
                "el destinatario no cancela: rechaza");
        assertTrue(servicio.cancelarSolicitud(ANA, LUIS).esExitoso());
        assertEquals(EstadoAmistad.SIN_RELACION, servicio.obtenerEstado(LUIS, ANA));
    }

    // ------------------------------------------------------------------ búsqueda con estado

    @Test
    void laBusquedaMuestraUnEstadoParaCadaSituacion() {
        Sesion.cerrar();
        servicio.enviarSolicitud(ANA, LUIS);    // Ana → Luis pendiente
        servicio.enviarSolicitud(SOFIA, ANA);   // Sofía → Ana pendiente

        assertEquals(EstadoAmistad.SOLICITUD_ENVIADA, unico(servicio.buscarConEstado(ANA, "luis")).getEstado());
        assertEquals(EstadoAmistad.SOLICITUD_RECIBIDA, unico(servicio.buscarConEstado(ANA, "sofia")).getEstado());
        assertEquals(EstadoAmistad.PROPIA_CUENTA, unico(servicio.buscarConEstado(ANA, "ana mora")).getEstado());
        assertTrue(servicio.buscarConEstado(ANA, "zzz").estaVacia(), "usuario inexistente: sin resultados");

        servicio.aceptarSolicitud(ANA, SOFIA);
        assertEquals(EstadoAmistad.AMIGOS, unico(servicio.buscarConEstado(ANA, "sofi")).getEstado());

        servicio.cancelarSolicitud(ANA, LUIS);
        assertEquals(EstadoAmistad.SIN_RELACION, unico(servicio.buscarConEstado(ANA, "luis")).getEstado());

        for (EstadoAmistad estado : EstadoAmistad.values()) {
            assertFalse(estado.getDescripcion().isBlank(), "ningún estado queda vacío: " + estado);
        }
    }

    @Test
    void unUsuarioRecienRegistradoApareceSinRelacionYSePuedeAgregar() {
        ServicioAutenticacion autenticacion = new ServicioAutenticacion(contexto, COSTO_PRUEBAS);
        assertTrue(autenticacion.registrar("4", "Marta Solís", "marta", "x".toCharArray(), "x".toCharArray()).esExitoso());

        UsuarioEncontrado encontrado = unico(servicio.buscarConEstado(ANA, "marta"));

        assertEquals("4", encontrado.getUsuario().getId());
        assertEquals(EstadoAmistad.SIN_RELACION, encontrado.getEstado());
        assertTrue(servicio.enviarSolicitud(ANA, "4").esExitoso());
        assertEquals(EstadoAmistad.SOLICITUD_ENVIADA, unico(servicio.buscarConEstado(ANA, "marta")).getEstado());
    }

    // ------------------------------------------------------------------ persistencia

    @Test
    void lasSolicitudesPendientesSobrevivenAReabrirLaAplicacion() throws Exception {
        servicio.enviarSolicitud(ANA, LUIS);
        servicio.enviarSolicitud(SOFIA, LUIS);
        contexto.guardarTodo(); // como al cerrar la aplicación

        ServicioAmistades reabierto = new ServicioAmistades(abrirInstancia());

        assertEquals(2, reabierto.obtenerSolicitudesRecibidas(LUIS).tamano());
        assertEquals(EstadoAmistad.SOLICITUD_RECIBIDA, reabierto.obtenerEstado(LUIS, ANA));
        assertFalse(reabierto.sonAmigos(ANA, LUIS));
    }

    @Test
    void siNoSePuedeGuardarLaSolicitudNoQuedaEnMemoria() throws Exception {
        Files.writeString(carpeta.resolve(GestorArchivos.ARCHIVO_SOLICITUDES), "[ roto", StandardCharsets.UTF_8);

        ResultadoAmistad r = servicio.enviarSolicitud(ANA, LUIS);

        assertEquals(Tipo.ERROR_PERSISTENCIA, r.getTipo());
        assertFalse(contexto.getSolicitudes().existe(ANA, LUIS));
        assertEquals("[ roto", Files.readString(carpeta.resolve(GestorArchivos.ARCHIVO_SOLICITUDES),
                StandardCharsets.UTF_8), "el archivo dañado no se sobrescribe");
    }

    // ------------------------------------------------------------------ dos instancias del programa

    @Test
    void dosInstanciasVenLosCambiosDeLaOtraAlActualizar() throws Exception {
        ServicioAmistades instanciaAna = servicio;
        ContextoAplicacion contextoLuis = abrirInstancia();
        ServicioAmistades instanciaLuis = new ServicioAmistades(contextoLuis);

        // Ana envía desde su instancia
        assertTrue(instanciaAna.enviarSolicitud(ANA, LUIS).esExitoso());

        // Luis la ve al actualizar (pestaña Solicitudes / botón Actualizar)
        assertTrue(instanciaLuis.obtenerSolicitudesRecibidas(LUIS).estaVacia(), "sin actualizar aún no la ve");
        assertTrue(instanciaLuis.actualizarDesdeDisco());
        assertEquals("Ana Mora", instanciaLuis.obtenerSolicitudesRecibidas(LUIS).obtener(0).getNombre());

        // Luis acepta; Ana lo ve al actualizar
        assertTrue(instanciaLuis.aceptarSolicitud(LUIS, ANA).esExitoso());
        instanciaAna.actualizarDesdeDisco();
        assertTrue(instanciaAna.sonAmigos(ANA, LUIS));
        assertEquals(EstadoAmistad.AMIGOS, unico(instanciaAna.buscarConEstado(ANA, "luis")).getEstado());
    }

    @Test
    void cerrarUnaInstanciaConDatosViejosNoBorraLoQueHizoLaOtra() throws Exception {
        ContextoAplicacion contextoLuis = abrirInstancia();
        ServicioAmistades instanciaLuis = new ServicioAmistades(contextoLuis);

        // La instancia de Ana queda abierta sin actualizar mientras Luis y Sofía se hacen amigos
        instanciaLuis.enviarSolicitud(LUIS, SOFIA);
        instanciaLuis.aceptarSolicitud(SOFIA, LUIS);
        instanciaLuis.enviarSolicitud(LUIS, ANA);

        contexto.guardarTodo(); // Ana cierra sesión / cierra la aplicación

        ContextoAplicacion reabierto = abrirInstancia();
        assertTrue(reabierto.getAmistades().sonAmigos(LUIS, SOFIA), "la amistad de la otra instancia se conserva");
        assertTrue(reabierto.getSolicitudes().existe(LUIS, ANA), "la solicitud de la otra instancia se conserva");
    }

    @Test
    void unUsuarioRegistradoEnOtraInstanciaSePuedeBuscarYNoSeBorraAlCerrar() throws Exception {
        ContextoAplicacion otra = abrirInstancia();
        new ServicioAutenticacion(otra, COSTO_PRUEBAS)
                .registrar("4", "Marta Solís", "marta", "x".toCharArray(), "x".toCharArray());

        // La instancia de Ana la encuentra (la búsqueda relee el disco) y le envía solicitud
        UsuarioEncontrado marta = unico(servicio.buscarConEstado(ANA, "marta"));
        assertEquals(EstadoAmistad.SIN_RELACION, marta.getEstado());
        assertTrue(servicio.enviarSolicitud(ANA, "4").esExitoso());

        contexto.guardarTodo(); // Ana cierra: no debe borrar la cuenta de Marta

        ContextoAplicacion reabierto = abrirInstancia();
        assertTrue(reabierto.getUsuarios().existeId("4"));
        assertTrue(reabierto.getSolicitudes().existe(ANA, "4"));
    }

    @Test
    void solicitudesCruzadasDesdeDosInstanciasNoCreanEstadosContradictorios() throws Exception {
        // Ambas instancias abiertas, ninguna ha visto nada de la otra
        ContextoAplicacion contextoLuis = abrirInstancia();
        ServicioAmistades instanciaLuis = new ServicioAmistades(contextoLuis);
        assertEquals(EstadoAmistad.SIN_RELACION, instanciaLuis.obtenerEstado(LUIS, ANA));

        assertTrue(servicio.enviarSolicitud(ANA, LUIS).esExitoso());
        ResultadoAmistad deLuis = instanciaLuis.enviarSolicitud(LUIS, ANA); // pantalla desactualizada

        assertEquals(Tipo.SOLICITUD_RECIBIDA_PENDIENTE, deLuis.getTipo());
        ContextoAplicacion reabierto = abrirInstancia();
        assertEquals(1, reabierto.getSolicitudes().tamano(), "solo una solicitud entre ambos");
        assertTrue(reabierto.getSolicitudes().existe(ANA, LUIS));
        assertFalse(reabierto.getAmistades().sonAmigos(ANA, LUIS));
    }

    @Test
    void cancelarDesdeUnaInstanciaLoQueLaOtraYaAceptoNoDejaContradicciones() throws Exception {
        ContextoAplicacion contextoLuis = abrirInstancia();
        ServicioAmistades instanciaLuis = new ServicioAmistades(contextoLuis);
        servicio.enviarSolicitud(ANA, LUIS);
        instanciaLuis.actualizarDesdeDisco();
        assertTrue(instanciaLuis.aceptarSolicitud(LUIS, ANA).esExitoso());

        ResultadoAmistad cancelacion = servicio.cancelarSolicitud(ANA, LUIS); // Ana no había actualizado

        assertEquals(Tipo.SOLICITUD_NO_EXISTE, cancelacion.getTipo());
        assertTrue(servicio.sonAmigos(ANA, LUIS), "la instancia de Ana ya refleja la amistad");
    }

    // ------------------------------------------------------------------ utilidades

    private static UsuarioEncontrado unico(ListaDinamica<UsuarioEncontrado> resultados) {
        assertEquals(1, resultados.tamano(), "se esperaba un único resultado");
        return resultados.obtener(0);
    }
}
