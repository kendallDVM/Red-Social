package socialuna.servicios;

import static socialuna.UtilPruebas.COSTO_PRUEBAS;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import socialuna.datos.GestorArchivos;
import socialuna.modelo.CampoPerfil;
import socialuna.modelo.Perfil;
import socialuna.modelo.Usuario;

/**
 * Perfiles: consulta propia y ajena, edición, persistencia, privacidad, compatibilidad con
 * usuarios anteriores y uso con dos instancias. Usa una carpeta temporal.
 */
class ServicioPerfilesTest {

    @TempDir
    Path carpeta;

    private ContextoAplicacion contexto;
    private ServicioPerfiles servicio;
    private String ana;
    private String luis;

    @BeforeEach
    void preparar() throws Exception {
        contexto = abrirInstancia();
        ServicioAutenticacion auth = new ServicioAutenticacion(contexto, COSTO_PRUEBAS);
        auth.registrar("101", "Ana Mora", "ana", "Clave1".toCharArray(), "Clave1".toCharArray());
        auth.registrar("102", "Luis Peña", "luis", "Clave2".toCharArray(), "Clave2".toCharArray());
        ana = "101";
        luis = "102";
        servicio = new ServicioPerfiles(contexto);
        Sesion.cerrar();
    }

    @AfterEach
    void limpiar() {
        Sesion.cerrar();
    }

    private ContextoAplicacion abrirInstancia() throws Exception {
        ContextoAplicacion c = new ContextoAplicacion(new GestorArchivos(carpeta));
        c.cargarTodo();
        return c;
    }

    private Perfil perfilDeAna() {
        Perfil p = servicio.obtenerPerfilEditable(ana);
        p.establecer(CampoPerfil.BIOGRAFIA, "Estudiante de segundo año. Me encantan los grafos.");
        p.establecer(CampoPerfil.CARRERA, "Ingeniería en Sistemas");
        p.establecer(CampoPerfil.SEDE, "Campus Omar Dengo");
        p.establecer(CampoPerfil.INSTAGRAM, "@ana.mora");
        p.establecer(CampoPerfil.LINKEDIN, "https://linkedin.com/in/anamora");
        return p;
    }

    // ------------------------------------------------------------------ perfil propio

    @Test
    void unUsuarioSinInformacionTienePerfilVacioPeroValido() {
        VistaPerfil vista = servicio.verPerfil(ana, ana);

        assertNotNull(vista);
        assertEquals("Ana Mora", vista.getNombre());
        assertEquals("ana", vista.getUsuario());
        assertTrue(vista.esPropio());
        assertEquals("101", vista.getIdCuenta(), "el dueño ve su ID");
        assertFalse(vista.tieneInformacion());
    }

    @Test
    void editarYGuardarElPerfilPropio() {
        ResultadoPerfil r = servicio.guardarPerfil(ana, perfilDeAna());

        assertTrue(r.esExitoso(), r.toString());
        VistaPerfil vista = servicio.verPerfil(ana, ana);
        assertEquals(5, vista.getDatos().tamano());
        assertEquals("Ingeniería en Sistemas", vista.getDatos(CampoPerfil.Seccion.ACADEMICO).obtener(0).getValor());
        assertEquals(2, vista.getDatos(CampoPerfil.Seccion.REDES).tamano());
    }

    @Test
    void borrarUnCampoLoDejaSinCompletar() {
        servicio.guardarPerfil(ana, perfilDeAna());
        Perfil p = servicio.obtenerPerfilEditable(ana);
        p.establecer(CampoPerfil.SEDE, "");
        servicio.guardarPerfil(ana, p);

        assertNull(servicio.obtenerPerfilEditable(ana).obtener(CampoPerfil.SEDE));
        assertEquals("Ingeniería en Sistemas", servicio.obtenerPerfilEditable(ana).obtener(CampoPerfil.CARRERA));
    }

    @Test
    void losCambiosSeConservanAlReabrirLaAplicacion() throws Exception {
        servicio.guardarPerfil(ana, perfilDeAna());
        contexto.guardarTodo(); // como al cerrar la aplicación

        ServicioPerfiles reabierto = new ServicioPerfiles(abrirInstancia());
        Perfil leido = reabierto.obtenerPerfilEditable(ana);
        assertEquals("Estudiante de segundo año. Me encantan los grafos.", leido.obtener(CampoPerfil.BIOGRAFIA));
        assertEquals("@ana.mora", leido.obtener(CampoPerfil.INSTAGRAM));
    }

    @Test
    void unDatoInvalidoNoSeGuardaYSeIndicaElCampo() {
        Perfil p = servicio.obtenerPerfilEditable(ana);
        p.establecer(CampoPerfil.CARRERA, "Sistemas");
        p.establecer(CampoPerfil.FACEBOOK, "mi facebook");

        ResultadoPerfil r = servicio.guardarPerfil(ana, p);

        assertEquals(ResultadoPerfil.Tipo.DATO_INVALIDO, r.getTipo());
        assertEquals(CampoPerfil.FACEBOOK, r.getCampo());
        assertNull(servicio.obtenerPerfilEditable(ana).obtener(CampoPerfil.CARRERA), "no se guardó nada");
    }

    @Test
    void soloSePuedeEditarElPerfilPropio() {
        Perfil deLuis = new Perfil(luis);
        deLuis.establecer(CampoPerfil.BIOGRAFIA, "Texto escrito por otra persona");

        assertEquals(ResultadoPerfil.Tipo.NO_AUTORIZADO, servicio.guardarPerfil(ana, deLuis).getTipo());
        assertEquals(ResultadoPerfil.Tipo.SIN_SESION, servicio.guardarPerfil(null, deLuis).getTipo());
        assertFalse(servicio.verPerfil(ana, luis).tieneInformacion());
    }

    @Test
    void siNoSePuedeGuardarElPerfilNoCambiaNada() throws Exception {
        servicio.guardarPerfil(ana, perfilDeAna());
        Files.writeString(carpeta.resolve(GestorArchivos.ARCHIVO_PERFILES), "[ roto", StandardCharsets.UTF_8);
        Perfil cambio = servicio.obtenerPerfilEditable(ana);
        cambio.establecer(CampoPerfil.CARRERA, "Otra");

        ResultadoPerfil r = servicio.guardarPerfil(ana, cambio);

        assertEquals(ResultadoPerfil.Tipo.ERROR_PERSISTENCIA, r.getTipo());
        assertEquals("[ roto", Files.readString(carpeta.resolve(GestorArchivos.ARCHIVO_PERFILES),
                StandardCharsets.UTF_8), "no se sobrescribe un archivo dañado");
        assertEquals("Ingeniería en Sistemas", contexto.getPerfiles().buscar(ana).obtener(CampoPerfil.CARRERA));
    }

    // ------------------------------------------------------------------ perfil de otra persona

    @Test
    void otroUsuarioVeLosCamposCompletadosPeroNoElIdDeLaCuenta() {
        servicio.guardarPerfil(ana, perfilDeAna());

        VistaPerfil vista = servicio.verPerfil(luis, ana);

        assertFalse(vista.esPropio());
        assertNull(vista.getIdCuenta(), "el ID no se muestra a otros usuarios");
        assertEquals("Ana Mora", vista.getNombre());
        assertEquals(5, vista.getDatos().tamano());
        for (VistaPerfil.Dato d : vista.getDatos()) {
            assertNotNull(d.getValor(), "no hay campos vacíos: " + d.getCampo());
        }
    }

    @Test
    void laVistaPublicaNuncaContieneLaContrasenaNiSuHash() throws Exception {
        servicio.guardarPerfil(ana, perfilDeAna());
        Usuario cuenta = contexto.getUsuarios().buscarPorId(ana);
        String hash = cuenta.getContrasenaHash();

        VistaPerfil vista = servicio.verPerfil(luis, ana);

        // Ningún atributo de la vista (ni de sus datos) contiene el hash, la contraseña o el ID.
        StringBuilder todo = new StringBuilder(vista.toString());
        for (Field f : VistaPerfil.class.getDeclaredFields()) {
            f.setAccessible(true);
            todo.append('|').append(f.get(vista));
        }
        for (VistaPerfil.Dato d : vista.getDatos()) {
            todo.append('|').append(d.getValor());
        }
        assertFalse(todo.toString().contains(hash), "no debe aparecer el hash");
        assertFalse(todo.toString().contains("Clave1"), "no debe aparecer la contraseña");
        assertNull(vista.getIdCuenta(), "no debe aparecer el ID de la cuenta");
        for (Field f : VistaPerfil.class.getDeclaredFields()) {
            assertFalse(f.getType() == Usuario.class || f.getType() == Perfil.class,
                    "la vista no guarda objetos de cuenta completos: " + f.getName());
        }

        String perfilesJson = Files.readString(carpeta.resolve(GestorArchivos.ARCHIVO_PERFILES), StandardCharsets.UTF_8);
        assertFalse(perfilesJson.contains(hash), "perfiles.json no contiene datos de autenticación");
    }

    @Test
    void consultarUnPerfilNoModificaAmistadesNiSolicitudes() throws Exception {
        ServicioAmistades amistades = new ServicioAmistades(contexto);
        amistades.enviarSolicitud(luis, ana);
        Path solicitudes = carpeta.resolve(GestorArchivos.ARCHIVO_SOLICITUDES);
        Path amistadesJson = carpeta.resolve(GestorArchivos.ARCHIVO_AMISTADES);
        byte[] solicitudesAntes = Files.readAllBytes(solicitudes);
        byte[] amistadesAntes = Files.readAllBytes(amistadesJson);

        assertNotNull(servicio.verPerfil(ana, luis)); // con solicitud pendiente, sin amistad
        assertNotNull(servicio.verPerfil(luis, ana));

        assertArrayEquals(solicitudesAntes, Files.readAllBytes(solicitudes));
        assertArrayEquals(amistadesAntes, Files.readAllBytes(amistadesJson));
        assertEquals(EstadoAmistad.SOLICITUD_RECIBIDA, amistades.obtenerEstado(ana, luis));
        assertFalse(amistades.sonAmigos(ana, luis));
    }

    @Test
    void verElPerfilDeUnUsuarioInexistenteDevuelveNull() {
        assertNull(servicio.verPerfil(ana, "999"));
        assertNull(servicio.verPerfil(ana, null));
    }

    // ------------------------------------------------------------------ compatibilidad e instancias

    @Test
    void losUsuariosAnterioresSiguenIniciandoSesionYUsuariosJsonNoCambia() throws Exception {
        Path usuariosJson = carpeta.resolve(GestorArchivos.ARCHIVO_USUARIOS);
        byte[] antes = Files.readAllBytes(usuariosJson);

        servicio.guardarPerfil(ana, perfilDeAna());

        assertArrayEquals(antes, Files.readAllBytes(usuariosJson), "editar el perfil no toca usuarios.json");
        ContextoAplicacion reabierto = abrirInstancia();
        ResultadoAutenticacion login = new ServicioAutenticacion(reabierto, COSTO_PRUEBAS)
                .iniciarSesion("ana", "Clave1".toCharArray());
        assertTrue(login.esExitoso(), login.toString());
        assertTrue(new ServicioAutenticacion(reabierto, COSTO_PRUEBAS)
                .iniciarSesion("luis", "Clave2".toCharArray()).esExitoso(), "usuario sin perfil");
    }

    @Test
    void unPerfilEditadoEnOtraInstanciaSeVeAlConsultarlo() throws Exception {
        ServicioPerfiles instanciaLuis = new ServicioPerfiles(abrirInstancia());

        servicio.guardarPerfil(ana, perfilDeAna());

        VistaPerfil vista = instanciaLuis.verPerfil(luis, ana);
        assertEquals(5, vista.getDatos().tamano(), "la consulta relee perfiles.json");
    }

    @Test
    void cerrarUnaInstanciaConDatosViejosNoBorraLosPerfiles() throws Exception {
        ContextoAplicacion instanciaLuis = abrirInstancia(); // abierta antes de que Ana edite
        servicio.guardarPerfil(ana, perfilDeAna());

        instanciaLuis.guardarTodo(); // Luis cierra sesión

        assertEquals("@ana.mora", abrirInstancia().getPerfiles().buscar(ana).obtener(CampoPerfil.INSTAGRAM));
    }

    @Test
    void dosUsuariosEditandoSusPerfilesEnInstanciasDistintasNoSePisan() throws Exception {
        ServicioPerfiles instanciaLuis = new ServicioPerfiles(abrirInstancia()); // abierta antes
        servicio.guardarPerfil(ana, perfilDeAna());

        Perfil deLuis = instanciaLuis.obtenerPerfilEditable(luis);
        deLuis.establecer(CampoPerfil.CARRERA, "Matemática");
        assertTrue(instanciaLuis.guardarPerfil(luis, deLuis).esExitoso());

        ContextoAplicacion reabierto = abrirInstancia();
        assertEquals("Ingeniería en Sistemas", reabierto.getPerfiles().buscar(ana).obtener(CampoPerfil.CARRERA));
        assertEquals("Matemática", reabierto.getPerfiles().buscar(luis).obtener(CampoPerfil.CARRERA));
    }
}
