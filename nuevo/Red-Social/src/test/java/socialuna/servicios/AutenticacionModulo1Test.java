package socialuna.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static socialuna.UtilPruebas.COSTO_PRUEBAS;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import socialuna.datos.GestorArchivos;
import socialuna.datos.PersistenciaException;
import socialuna.modelo.Usuario;
import socialuna.servicios.ResultadoAutenticacion.Tipo;

/**
 * Pruebas de las correcciones del Módulo 1: contraseñas en blanco en el login, hash
 * ficticio precalculado, validaciones sin restricciones inventadas, sesión y protección
 * del archivo dañado a nivel de servicio. Usan una carpeta temporal.
 */
class AutenticacionModulo1Test {

    @TempDir
    Path carpeta;

    private ContextoAplicacion contexto;
    private ServicioAutenticacion servicio;

    @BeforeEach
    void preparar() {
        contexto = new ContextoAplicacion(new GestorArchivos(carpeta));
        servicio = new ServicioAutenticacion(contexto, COSTO_PRUEBAS);
        Sesion.cerrar();
    }

    @AfterEach
    void limpiar() {
        Sesion.cerrar();
    }

    private ResultadoAutenticacion registrar(String id, String nombre, String usuario, String clave) {
        return servicio.registrar(id, nombre, usuario, clave.toCharArray(), clave.toCharArray());
    }

    // ------------------------------------------------------------------ contraseñas en blanco en el login

    @Test
    void loginConContrasenaSoloDeEspaciosEsCampoVacio() {
        assertTrue(registrar("1", "Ana", "ana", "Clave1").esExitoso());
        for (String blanca : new String[] {" ", "    ", "\t", " \t\n "}) {
            ResultadoAutenticacion r = servicio.iniciarSesion("ana", blanca.toCharArray());
            assertEquals(Tipo.CAMPOS_VACIOS, r.getTipo(), "contraseña: [" + blanca + "]");
            assertNull(Sesion.getUsuarioActivo());
        }
    }

    @Test
    void loginConContrasenaNulaOVaciaEsCampoVacio() {
        assertEquals(Tipo.CAMPOS_VACIOS, servicio.iniciarSesion("ana", null).getTipo());
        assertEquals(Tipo.CAMPOS_VACIOS, servicio.iniciarSesion("ana", new char[0]).getTipo());
        assertEquals(Tipo.CAMPOS_VACIOS, servicio.iniciarSesion("   ", "Clave1".toCharArray()).getTipo());
        assertNull(Sesion.getUsuarioActivo());
    }

    @Test
    void losEspaciosDeUnaContrasenaValidaNoSeRecortan() {
        assertTrue(registrar("1", "Ana", "ana", "  mi clave  ").esExitoso());

        assertTrue(servicio.iniciarSesion("ana", "  mi clave  ".toCharArray()).esExitoso());
        Sesion.cerrar();
        ResultadoAutenticacion recortada = servicio.iniciarSesion("ana", "mi clave".toCharArray());
        assertEquals(Tipo.CREDENCIALES_INVALIDAS, recortada.getTipo());
        assertNull(Sesion.getUsuarioActivo());
    }

    // ------------------------------------------------------------------ sesión

    @Test
    void laSesionSoloSeEstableceConUnLoginCorrecto() {
        assertTrue(registrar("1", "Ana", "ana", "Clave1").esExitoso());
        assertNull(Sesion.getUsuarioActivo());                                 // antes del login

        assertFalse(servicio.iniciarSesion("ana", "clave1".toCharArray()).esExitoso());
        assertFalse(servicio.iniciarSesion("nadie", "Clave1".toCharArray()).esExitoso());
        assertFalse(Sesion.haySesionActiva());                                // logins fallidos

        ResultadoAutenticacion ok = servicio.iniciarSesion("ANA", "Clave1".toCharArray());
        assertTrue(ok.esExitoso());
        assertSame(contexto.getUsuarios().buscarPorUsuario("ana"), Sesion.getUsuarioActivo());

        assertTrue(servicio.cerrarSesion().esExitoso());
        assertFalse(Sesion.haySesionActiva());                                // después del cierre
    }

    @Test
    void unLoginFallidoNoReemplazaNiCierraUnaSesionYaIniciada() {
        assertTrue(registrar("1", "Ana", "ana", "Clave1").esExitoso());
        assertTrue(servicio.iniciarSesion("ana", "Clave1".toCharArray()).esExitoso());
        Usuario activo = Sesion.getUsuarioActivo();

        servicio.iniciarSesion("ana", "otra".toCharArray());
        assertSame(activo, Sesion.getUsuarioActivo());
    }

    // ------------------------------------------------------------------ hash ficticio precalculado

    @Test
    void elHashFicticioPredeterminadoEsBCryptDeCosto12() {
        String h = ServicioAutenticacion.HASH_FICTICIO_PREDETERMINADO;
        assertTrue(Usuario.esHashBCrypt(h));
        assertTrue(h.startsWith("$2a$" + ServicioAutenticacion.COSTO_POR_DEFECTO + "$"));
    }

    @Test
    void conElCostoPorDefectoUnUsuarioInexistenteRecibeElMensajeGenerico() {
        ServicioAutenticacion porDefecto = new ServicioAutenticacion(contexto);
        ResultadoAutenticacion r = porDefecto.iniciarSesion("no-existe", "cualquiera".toCharArray());
        assertEquals(Tipo.CREDENCIALES_INVALIDAS, r.getTipo());
        assertEquals(ServicioAutenticacion.MENSAJE_CREDENCIALES_INVALIDAS, r.getMensaje());
        assertNull(Sesion.getUsuarioActivo());
    }

    // ------------------------------------------------------------------ validaciones (sin reglas inventadas)

    @Test
    void elIdSeConservaComoTextoConCerosInicialesOLetras() throws Exception {
        assertTrue(registrar("000123", "Ana", "ana", "c").esExitoso());
        assertTrue(registrar("A-77", "Luis", "luis", "c").esExitoso());
        assertEquals(Tipo.ID_DUPLICADO, registrar("000123", "Otro", "otro", "c").getTipo());
        assertTrue(registrar("123", "Bea", "bea", "c").esExitoso()); // "123" ≠ "000123"

        ContextoAplicacion reiniciado = new ContextoAplicacion(new GestorArchivos(carpeta));
        reiniciado.cargarTodo();
        assertEquals("ana", reiniciado.getUsuarios().buscarPorId("000123").getUsuario());
        assertEquals("luis", reiniciado.getUsuarios().buscarPorId("A-77").getUsuario());
    }

    @Test
    void elNombreConservaTildesYEnes() throws Exception {
        assertTrue(registrar("1", "José Ñúñez Güell", "jose", "c").esExitoso());

        ContextoAplicacion reiniciado = new ContextoAplicacion(new GestorArchivos(carpeta));
        reiniciado.cargarTodo();
        assertEquals("José Ñúñez Güell", reiniciado.getUsuarios().buscarPorUsuario("JOSE").getNombre());
    }

    @Test
    void noSeExigeLongitudMinimaNiSimbolosEnLaContrasena() {
        assertTrue(registrar("1", "Ana", "ana", "a").esExitoso());
        assertTrue(servicio.iniciarSesion("ana", "a".toCharArray()).esExitoso());
    }

    // ------------------------------------------------------------------ archivo dañado a nivel de servicio

    @Test
    void conElArchivoDanadoNoSePuedeRegistrarNiSobrescribirAlCerrarSesion() throws Exception {
        Path archivo = contexto.getGestorArchivos().getRutaUsuarios();
        String danado = "{ esto no es una lista";
        Files.writeString(archivo, danado);
        try {
            contexto.cargarTodo();
        } catch (PersistenciaException esperado) {
            // la carga falla, como en Main al iniciar
        }

        ResultadoAutenticacion r = registrar("1", "Ana", "ana", "Clave1");
        assertEquals(Tipo.ERROR_PERSISTENCIA, r.getTipo());
        assertFalse(contexto.getUsuarios().existeUsuario("ana")); // la memoria tampoco cambió

        assertEquals(Tipo.ERROR_PERSISTENCIA, servicio.cerrarSesion().getTipo());
        assertEquals(danado, Files.readString(archivo));          // el archivo original sigue intacto
    }
}
