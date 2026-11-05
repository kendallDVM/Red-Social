package socialuna.servicios;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static socialuna.UtilPruebas.COSTO_PRUEBAS;
import static socialuna.UtilPruebas.hash;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import socialuna.UtilPruebas;
import socialuna.datos.GestorArchivos;
import socialuna.datos.ListaUsuarios;
import socialuna.modelo.Usuario;
import socialuna.servicios.ResultadoAutenticacion.Tipo;

/** Todas las pruebas usan una carpeta temporal: nunca tocan el data/ real del proyecto. */
class ServicioAutenticacionTest {

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
    void limpiarSesion() {
        Sesion.cerrar();
    }

    private ResultadoAutenticacion registrar(String id, String nombre, String usuario, String clave, String confirmacion) {
        return servicio.registrar(id, nombre, usuario,
                clave == null ? null : clave.toCharArray(),
                confirmacion == null ? null : confirmacion.toCharArray());
    }

    private void registrarKendall() {
        ResultadoAutenticacion r = registrar("402220333", "Kendall Pérez", "Kendall", "Clave#2026", "Clave#2026");
        assertTrue(r.esExitoso(), r.toString());
    }

    // ================================================================== registro

    @Test
    void registroExitosoAgregaYGuardaConHash() throws Exception {
        ResultadoAutenticacion r = registrar("402220333", "Kendall Pérez", "Kendall", "Clave#2026", "Clave#2026");

        assertTrue(r.esExitoso());
        assertEquals(Tipo.EXITO, r.getTipo());
        assertEquals("Cuenta creada correctamente. Ya puede iniciar sesión.", r.getMensaje());
        Usuario u = r.getUsuario();
        assertNotNull(u);
        assertSame(u, contexto.getUsuarios().buscarPorUsuario("kendall"));
        assertTrue(Usuario.esHashBCrypt(u.getContrasenaHash()));
        assertTrue(UtilPruebas.verifica("Clave#2026", u.getContrasenaHash()));

        String json = Files.readString(carpeta.resolve("usuarios.json"), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"usuario\": \"Kendall\""));
        assertTrue(json.contains("\"contrasena\": \"$2a$"));
        assertFalse(json.contains("Clave#2026"), "la contraseña en texto plano no debe guardarse");
    }

    @Test
    void registroNoIniciaSesion() {
        registrarKendall();
        assertFalse(Sesion.haySesionActiva());
    }

    @Test
    void registroQuitaEspaciosAlInicioYFinal() {
        ResultadoAutenticacion r = registrar("  123  ", "  Ana Mora ", " ana ", "x1", "x1");
        assertTrue(r.esExitoso(), r.toString());
        assertEquals("123", r.getUsuario().getId());
        assertEquals("Ana Mora", r.getUsuario().getNombre());
        assertEquals("ana", r.getUsuario().getUsuario());
    }

    @Test
    void camposVaciosSeRechazanIndicandoCuales() {
        ResultadoAutenticacion r = registrar("", "  ", "ana", "", null);
        assertEquals(Tipo.CAMPOS_VACIOS, r.getTipo());
        assertTrue(r.getMensaje().contains("ID"));
        assertTrue(r.getMensaje().contains("Nombre"));
        assertTrue(r.getMensaje().contains("Contraseña"));
        assertTrue(r.getMensaje().contains("Confirmar contraseña"));
        assertFalse(r.getMensaje().contains("Usuario"));
        assertTrue(contexto.getUsuarios().estaVacia());
        assertFalse(Files.exists(carpeta.resolve("usuarios.json")), "no debe escribir nada si hay errores");
    }

    @Test
    void contrasenaSoloConEspaciosCuentaComoVacia() {
        assertEquals(Tipo.CAMPOS_VACIOS, registrar("1", "Ana", "ana", "   ", "   ").getTipo());
    }

    @Test
    void usuarioConEspaciosSeRechaza() {
        assertEquals(Tipo.USUARIO_INVALIDO, registrar("1", "Ana", "ana mora", "x", "x").getTipo());
    }

    @Test
    void contrasenasDistintasSeRechazan() {
        ResultadoAutenticacion r = registrar("1", "Ana", "ana", "abc123", "abc124");
        assertEquals(Tipo.CONTRASENAS_NO_COINCIDEN, r.getTipo());
        assertEquals("Las contraseñas no coinciden.", r.getMensaje());
        assertTrue(contexto.getUsuarios().estaVacia());
    }

    @Test
    void idDuplicadoSeRechaza() {
        registrarKendall();
        ResultadoAutenticacion r = registrar("402220333", "Otra Persona", "otra", "x", "x");
        assertEquals(Tipo.ID_DUPLICADO, r.getTipo());
        assertEquals(1, contexto.getUsuarios().tamano());
    }

    @Test
    void usuarioDuplicadoSinDistinguirMayusculasSeRechaza() {
        registrarKendall();
        for (String variante : new String[] {"Kendall", "kendall", "KENDALL", "kEnDaLl"}) {
            ResultadoAutenticacion r = registrar("999", "Otra Persona", variante, "x", "x");
            assertEquals(Tipo.USUARIO_DUPLICADO, r.getTipo(), variante);
        }
        assertEquals(1, contexto.getUsuarios().tamano());
    }

    @Test
    void contrasenaDemasiadoLargaSeRechaza() {
        String de71 = "a".repeat(ServicioAutenticacion.MAX_BYTES_CONTRASENA);
        String de72 = de71 + "a";
        String conTildes = "ñ".repeat(36); // 36 caracteres = 72 bytes en UTF-8

        assertTrue(registrar("1", "Ana", "ana", de71, de71).esExitoso());
        assertEquals(Tipo.CONTRASENA_INVALIDA, registrar("2", "Bea", "bea", de72, de72).getTipo());
        assertEquals(Tipo.CONTRASENA_INVALIDA, registrar("3", "Ceci", "ceci", conTildes, conTildes).getTipo());
    }

    @Test
    void elServicioNoModificaLosArreglosDeContrasena() {
        char[] clave = "Clave#2026".toCharArray();
        char[] confirmacion = "Clave#2026".toCharArray();
        servicio.registrar("1", "Ana", "ana", clave, confirmacion);
        assertArrayEquals("Clave#2026".toCharArray(), clave); // la ventana es la que los borra
        assertArrayEquals("Clave#2026".toCharArray(), confirmacion);
    }

    @Test
    void siFallaElGuardadoElRegistroNoSeCompleta() throws Exception {
        Path noEsCarpeta = carpeta.resolve("bloqueado");
        Files.writeString(noEsCarpeta, "un archivo donde debería ir una carpeta");
        ContextoAplicacion contextoRoto = new ContextoAplicacion(new GestorArchivos(noEsCarpeta));
        ServicioAutenticacion servicioRoto = new ServicioAutenticacion(contextoRoto, COSTO_PRUEBAS);

        ResultadoAutenticacion r = servicioRoto.registrar("1", "Ana", "ana", "x".toCharArray(), "x".toCharArray());

        assertFalse(r.esExitoso());
        assertEquals(Tipo.ERROR_PERSISTENCIA, r.getTipo());
        assertTrue(r.getMensaje().startsWith("No se pudo completar el registro"));
        assertNull(r.getUsuario());
        assertTrue(contextoRoto.getUsuarios().estaVacia(), "la memoria no debe quedar con un usuario sin guardar");
    }

    // ================================================================== inicio de sesión

    @Test
    void inicioDeSesionCorrectoEstableceLaSesion() {
        registrarKendall();
        ResultadoAutenticacion r = servicio.iniciarSesion("Kendall", "Clave#2026".toCharArray());
        assertTrue(r.esExitoso());
        assertSame(contexto.getUsuarios().buscarPorUsuario("kendall"), Sesion.getUsuarioActivo());
        assertSame(r.getUsuario(), Sesion.getUsuarioActivo());
    }

    @Test
    void inicioDeSesionNoDistingueMayusculasEnElUsuario() {
        registrarKendall();
        assertTrue(servicio.iniciarSesion("kendall", "Clave#2026".toCharArray()).esExitoso());
        Sesion.cerrar();
        assertTrue(servicio.iniciarSesion("  KENDALL ", "Clave#2026".toCharArray()).esExitoso());
    }

    @Test
    void laContrasenaSiDistingueMayusculas() {
        registrarKendall();
        ResultadoAutenticacion r = servicio.iniciarSesion("kendall", "clave#2026".toCharArray());
        assertEquals(Tipo.CREDENCIALES_INVALIDAS, r.getTipo());
        assertFalse(Sesion.haySesionActiva());
    }

    @Test
    void usuarioInexistenteYContrasenaIncorrectaDanElMismoMensaje() {
        registrarKendall();
        ResultadoAutenticacion malaClave = servicio.iniciarSesion("kendall", "incorrecta".toCharArray());
        ResultadoAutenticacion noExiste = servicio.iniciarSesion("fantasma", "incorrecta".toCharArray());

        assertEquals(Tipo.CREDENCIALES_INVALIDAS, malaClave.getTipo());
        assertEquals(Tipo.CREDENCIALES_INVALIDAS, noExiste.getTipo());
        assertEquals(malaClave.getMensaje(), noExiste.getMensaje());
        assertEquals("Usuario o contraseña incorrectos.", noExiste.getMensaje());
        assertNull(malaClave.getUsuario());
        assertFalse(Sesion.haySesionActiva());
    }

    @Test
    void inicioDeSesionConCamposVacios() {
        assertEquals(Tipo.CAMPOS_VACIOS, servicio.iniciarSesion("", "x".toCharArray()).getTipo());
        assertEquals(Tipo.CAMPOS_VACIOS, servicio.iniciarSesion("ana", new char[0]).getTipo());
        assertEquals(Tipo.CAMPOS_VACIOS, servicio.iniciarSesion(null, null).getTipo());
    }

    @Test
    void contrasenaMuyLargaEnLoginSeRechazaSinErrores() {
        registrarKendall();
        ResultadoAutenticacion r = servicio.iniciarSesion("kendall", "a".repeat(200).toCharArray());
        assertEquals(Tipo.CREDENCIALES_INVALIDAS, r.getTipo());
    }

    @Test
    void usuariosRegistradosSiguenFuncionandoTrasReiniciar() throws Exception {
        registrarKendall();

        // Simula cerrar y volver a abrir la aplicación: contexto nuevo que lee el mismo archivo
        ContextoAplicacion reiniciado = new ContextoAplicacion(new GestorArchivos(carpeta));
        reiniciado.cargarTodo();
        ServicioAutenticacion servicioReiniciado = new ServicioAutenticacion(reiniciado, COSTO_PRUEBAS);

        assertEquals(1, reiniciado.getUsuarios().tamano());
        assertTrue(servicioReiniciado.iniciarSesion("kendall", "Clave#2026".toCharArray()).esExitoso());
        assertEquals("402220333", Sesion.getUsuarioActivo().getId());
    }

    // ================================================================== cierre de sesión

    @Test
    void cerrarSesionGuardaYLimpiaLaSesion() throws Exception {
        registrarKendall();
        servicio.iniciarSesion("kendall", "Clave#2026".toCharArray());
        Files.delete(carpeta.resolve("usuarios.json")); // para comprobar que cerrar sesión vuelve a guardar

        ResultadoAutenticacion r = servicio.cerrarSesion();

        assertTrue(r.esExitoso());
        assertFalse(Sesion.haySesionActiva());
        assertNull(Sesion.getUsuarioActivo());
        assertTrue(Files.exists(carpeta.resolve("usuarios.json")));
        ListaUsuarios recargados = new ListaUsuarios();
        new GestorArchivos(carpeta).cargarUsuarios(recargados);
        assertTrue(recargados.existeUsuario("kendall"));
    }

    @Test
    void cerrarSesionConErrorDeGuardadoAvisaPeroCierraLaSesion() throws Exception {
        Path noEsCarpeta = carpeta.resolve("bloqueado");
        Files.writeString(noEsCarpeta, "x");
        ContextoAplicacion contextoRoto = new ContextoAplicacion(new GestorArchivos(noEsCarpeta));
        contextoRoto.getUsuarios().agregar(new Usuario("1", "Ana", "ana", hash("x")));
        ServicioAutenticacion servicioRoto = new ServicioAutenticacion(contextoRoto, COSTO_PRUEBAS);
        assertTrue(servicioRoto.iniciarSesion("ana", "x".toCharArray()).esExitoso());

        ResultadoAutenticacion r = servicioRoto.cerrarSesion();

        assertEquals(Tipo.ERROR_PERSISTENCIA, r.getTipo());
        assertTrue(r.getMensaje().startsWith("La sesión se cerró, pero no se pudieron guardar los datos."));
        assertFalse(Sesion.haySesionActiva());
        assertEquals(1, contextoRoto.getUsuarios().tamano()); // los datos siguen en memoria
    }
}
