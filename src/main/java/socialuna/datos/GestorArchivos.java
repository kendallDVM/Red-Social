package socialuna.datos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.Strictness;

import socialuna.modelo.Amistad;
import socialuna.modelo.Usuario;

/**
 * Punto único de persistencia del proyecto: lee y escribe los archivos JSON de {@code data/}.
 *
 * <p>Reglas de seguridad de los datos:
 * <ul>
 *   <li><b>Carga transaccional:</b> se lee primero en una estructura temporal. La lista
 *       real solo se reemplaza si todo el archivo es válido. Si falla, la lista en
 *       memoria queda como estaba.</li>
 *   <li><b>Archivo inexistente:</b> se crean la carpeta y el archivo con {@code []}.</li>
 *   <li><b>Archivo vacío:</b> se interpreta como "sin datos", sin error.</li>
 *   <li><b>Archivo dañado</b> (JSON inválido o registros inconsistentes): antes de lanzar
 *       el error se hace una copia de respaldo ({@code usuarios.json.corrupto-<fecha>}).
 *       Así siempre queda una copia de los datos originales.</li>
 *   <li><b>Protección tras una carga fallida:</b> si {@link #cargarUsuarios} falla (archivo
 *       dañado o ilegible), este gestor <b>bloquea</b> {@link #guardarUsuarios} para no
 *       reemplazar el archivo original por una lista vacía o incompleta. El bloqueo se
 *       levanta solo cuando una carga posterior termina correctamente.</li>
 *   <li><b>Escritura atómica:</b> se escribe en un {@code .tmp} y luego se reemplaza el
 *       original. Si el programa falla a mitad de la escritura, el archivo anterior queda intacto.</li>
 * </ul>
 *
 * <p>Estado por módulos: usuarios (Módulo 1) y amistades (Módulo 2) ya están
 * implementados. Publicaciones (Módulo 3) tiene definido su archivo y su lugar en
 * {@link #cargarTodo} / {@link #guardarTodo}, pendiente de implementar.
 */
public class GestorArchivos {

    public static final String DIRECTORIO_POR_DEFECTO = "data";
    public static final String ARCHIVO_USUARIOS = "usuarios.json";
    public static final String ARCHIVO_AMISTADES = "amistades.json";
    public static final String ARCHIVO_PUBLICACIONES = "publicaciones.json";

    private static final DateTimeFormatter FORMATO_RESPALDO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Path directorio;
    private final Gson gson;

    /**
     * Si es {@code true}, no se permite sobrescribir usuarios.json: la última carga falló
     * y el archivo original debe conservarse (ver la descripción de la clase).
     */
    private boolean escrituraUsuariosBloqueada;

    /** Igual que {@link #escrituraUsuariosBloqueada}, pero para amistades.json. */
    private boolean escrituraAmistadesBloqueada;

    /** Usa la carpeta {@code data/} relativa al directorio de ejecución (la raíz del proyecto). */
    public GestorArchivos() {
        this(Paths.get(DIRECTORIO_POR_DEFECTO));
    }

    /** Permite indicar otra carpeta (por ejemplo, una temporal en las pruebas). */
    public GestorArchivos(Path directorio) {
        this.directorio = Objects.requireNonNull(directorio, "El directorio no puede ser nulo.");
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .setStrictness(Strictness.STRICT)
                .create();
    }

    public Path getDirectorio() {
        return directorio;
    }

    public Path getRutaUsuarios() {
        return directorio.resolve(ARCHIVO_USUARIOS);
    }

    public Path getRutaAmistades() {
        return directorio.resolve(ARCHIVO_AMISTADES);
    }

    public Path getRutaPublicaciones() {
        return directorio.resolve(ARCHIVO_PUBLICACIONES);
    }

    // ------------------------------------------------------------------ usuarios

    /**
     * Carga {@code usuarios.json} en {@code destino}, reemplazando su contenido.
     * Se reconstruyen la lista dinámica y el índice usuario → Usuario.
     *
     * <p>Si la carga falla, se bloquea {@link #guardarUsuarios} en este gestor hasta que
     * una carga posterior termine correctamente. Un archivo inexistente o vacío
     * <b>no</b> es un fallo.
     *
     * @return cantidad de usuarios cargados
     * @throws PersistenciaException si el archivo no se puede leer o su contenido es
     *                               inválido. En ese caso {@code destino} no se modifica.
     */
    public int cargarUsuarios(ListaUsuarios destino) throws PersistenciaException {
        Objects.requireNonNull(destino, "La lista destino no puede ser nula.");
        try {
            int cantidad = leerUsuariosEn(destino);
            escrituraUsuariosBloqueada = false; // carga válida: se puede volver a guardar
            return cantidad;
        } catch (PersistenciaException e) {
            escrituraUsuariosBloqueada = true;  // conservar el archivo original
            throw e;
        }
    }

    /**
     * Indica si {@link #guardarUsuarios} está bloqueado porque la última carga de
     * usuarios falló.
     */
    public boolean isEscrituraUsuariosBloqueada() {
        return escrituraUsuariosBloqueada;
    }

    /** Lectura transaccional de usuarios.json (ver {@link #cargarUsuarios}). */
    private int leerUsuariosEn(ListaUsuarios destino) throws PersistenciaException {
        Path ruta = getRutaUsuarios();

        crearDirectorioSiNoExiste();
        if (Files.notExists(ruta)) {
            escribirTexto(ruta, "[]");
            destino.limpiar();
            return 0;
        }

        String contenido = leerTexto(ruta);
        if (contenido.isBlank()) {
            destino.limpiar();
            return 0;
        }

        Usuario[] leidos;
        try {
            leidos = gson.fromJson(contenido, Usuario[].class);
        } catch (JsonParseException e) {
            throw archivoDanado(ruta, "el contenido no es un JSON válido de usuarios", e);
        }

        ListaUsuarios temporal = new ListaUsuarios();
        if (leidos != null) {
            for (int i = 0; i < leidos.length; i++) {
                if (leidos[i] == null) {
                    throw archivoDanado(ruta, "el registro #" + (i + 1) + " está vacío", null);
                }
                try {
                    temporal.agregar(leidos[i]);
                } catch (IllegalArgumentException e) {
                    throw archivoDanado(ruta, "el registro #" + (i + 1) + " es inválido (" + e.getMessage() + ")", e);
                }
            }
        }

        destino.reemplazarCon(temporal);
        return destino.tamano();
    }

    /**
     * Guarda todos los usuarios en {@code usuarios.json} (escritura atómica).
     *
     * @throws PersistenciaException si no se puede escribir o la escritura está bloqueada
     */
    public void guardarUsuarios(ListaUsuarios origen) throws PersistenciaException {
        Objects.requireNonNull(origen, "La lista origen no puede ser nula.");
        if (escrituraUsuariosBloqueada) {
            throw new PersistenciaException("No se guardaron los usuarios: " + ARCHIVO_USUARIOS
                    + " no se pudo cargar correctamente y se protegió para no perder sus datos."
                    + " Revise o restaure el archivo y vuelva a abrir la aplicación.");
        }
        ListaDinamica<Usuario> todos = origen.obtenerTodos();
        String json = gson.toJson(todos.aArreglo(new Usuario[0]));
        crearDirectorioSiNoExiste();
        escribirTextoAtomico(getRutaUsuarios(), json);
    }

    // ------------------------------------------------------------------ amistades

    /**
     * Carga {@code amistades.json} en {@code destino} y reconstruye el grafo en memoria.
     * Debe llamarse <b>después</b> de cargar los usuarios, porque los usa para validar.
     *
     * <p>Reglas (además de las generales de la clase):
     * <ul>
     *   <li>Si un ID no existe en {@code usuarios}, esa amistad se ignora sin fallar.</li>
     *   <li>Los registros incompletos (sin IDs o de un usuario consigo mismo) se ignoran.</li>
     *   <li>Los duplicados (A–B repetido o A–B y B–A) quedan como una sola amistad.</li>
     * </ul>
     * Lo ignorado desaparece del archivo la próxima vez que se guarde.
     *
     * @return cantidad de amistades cargadas
     * @throws PersistenciaException si el archivo no se puede leer o no es un JSON válido.
     *                               En ese caso {@code destino} no se modifica y se bloquea
     *                               {@link #guardarAmistades}.
     */
    public int cargarAmistades(RedAmistades destino, ListaUsuarios usuarios) throws PersistenciaException {
        Objects.requireNonNull(destino, "La red destino no puede ser nula.");
        Objects.requireNonNull(usuarios, "La lista de usuarios no puede ser nula.");
        try {
            int cantidad = leerAmistadesEn(destino, usuarios);
            escrituraAmistadesBloqueada = false;
            return cantidad;
        } catch (PersistenciaException e) {
            escrituraAmistadesBloqueada = true;
            throw e;
        }
    }

    /**
     * Indica si {@link #guardarAmistades} está bloqueado porque la última carga de
     * amistades falló.
     */
    public boolean isEscrituraAmistadesBloqueada() {
        return escrituraAmistadesBloqueada;
    }

    /** Lectura transaccional de amistades.json (ver {@link #cargarAmistades}). */
    private int leerAmistadesEn(RedAmistades destino, ListaUsuarios usuarios) throws PersistenciaException {
        Path ruta = getRutaAmistades();

        crearDirectorioSiNoExiste();
        if (Files.notExists(ruta)) {
            escribirTexto(ruta, "[]");
            destino.limpiar();
            return 0;
        }

        String contenido = leerTexto(ruta);
        if (contenido.isBlank()) {
            destino.limpiar();
            return 0;
        }

        Amistad[] leidas;
        try {
            leidas = gson.fromJson(contenido, Amistad[].class);
        } catch (JsonParseException e) {
            throw archivoDanado(ruta, "el contenido no es un JSON válido de amistades", e);
        }

        // Se arma primero en una red temporal; la real solo se toca si todo salió bien.
        RedAmistades temporal = new RedAmistades();
        if (leidas != null) {
            for (Amistad amistad : leidas) {
                if (esAmistadValida(amistad, usuarios)) {
                    temporal.agregarAmistad(amistad.getIdUsuarioA(), amistad.getIdUsuarioB());
                }
            }
        }

        destino.limpiar();
        for (Amistad amistad : temporal.obtenerAmistades()) {
            destino.agregarAmistad(amistad.getIdUsuarioA(), amistad.getIdUsuarioB());
        }
        return destino.cantidadAmistades();
    }

    /** Un registro se acepta si está completo y ambos usuarios existen. */
    private static boolean esAmistadValida(Amistad amistad, ListaUsuarios usuarios) {
        if (amistad == null) {
            return false;
        }
        try {
            amistad.validar(); // IDs presentes y distintos entre sí
        } catch (IllegalArgumentException e) {
            return false;
        }
        return usuarios.existeId(amistad.getIdUsuarioA()) && usuarios.existeId(amistad.getIdUsuarioB());
    }

    /**
     * Guarda todas las amistades en {@code amistades.json} (escritura atómica),
     * cada una una sola vez.
     *
     * @throws PersistenciaException si no se puede escribir o la escritura está bloqueada
     */
    public void guardarAmistades(RedAmistades origen) throws PersistenciaException {
        Objects.requireNonNull(origen, "La red origen no puede ser nula.");
        if (escrituraAmistadesBloqueada) {
            throw new PersistenciaException("No se guardaron las amistades: " + ARCHIVO_AMISTADES
                    + " no se pudo cargar correctamente y se protegió para no perder sus datos."
                    + " Revise o restaure el archivo y vuelva a abrir la aplicación.");
        }
        String json = gson.toJson(origen.obtenerAmistades().aArreglo(new Amistad[0]));
        crearDirectorioSiNoExiste();
        escribirTextoAtomico(getRutaAmistades(), json);
    }

    // ------------------------------------------------------------------ todo el sistema

    /**
     * Carga todos los datos del sistema. Se llama una vez al iniciar la aplicación.
     *
     * <p>Pendiente: Módulo 3 ({@code cargarPublicaciones}).
     */
    public void cargarTodo(ListaUsuarios usuarios, ListaPublicaciones publicaciones, RedAmistades amistades)
            throws PersistenciaException {
        Objects.requireNonNull(publicaciones, "La lista de publicaciones no puede ser nula.");
        Objects.requireNonNull(amistades, "La red de amistades no puede ser nula.");
        cargarUsuarios(usuarios);
        cargarAmistades(amistades, usuarios); // después de usuarios: los necesita para validar
        // TODO Módulo 3 (Mariano): cargarPublicaciones(publicaciones);
    }

    /**
     * Guarda todos los datos del sistema. Se llama al cerrar sesión y al cerrar la aplicación.
     *
     * <p>Pendiente: Módulo 3 ({@code guardarPublicaciones}).
     */
    public void guardarTodo(ListaUsuarios usuarios, ListaPublicaciones publicaciones, RedAmistades amistades)
            throws PersistenciaException {
        Objects.requireNonNull(publicaciones, "La lista de publicaciones no puede ser nula.");
        Objects.requireNonNull(amistades, "La red de amistades no puede ser nula.");
        guardarUsuarios(usuarios);
        guardarAmistades(amistades);
        // TODO Módulo 3 (Mariano): guardarPublicaciones(publicaciones);
    }

    // ------------------------------------------------------------------ utilidades de archivos

    private void crearDirectorioSiNoExiste() throws PersistenciaException {
        try {
            Files.createDirectories(directorio);
        } catch (IOException e) {
            throw new PersistenciaException("No se pudo crear la carpeta de datos '" + directorio + "'.", e);
        }
    }

    private String leerTexto(Path ruta) throws PersistenciaException {
        try {
            String texto = Files.readString(ruta, StandardCharsets.UTF_8);
            // Quitar el BOM que algunos editores de Windows agregan al inicio
            return texto.startsWith("﻿") ? texto.substring(1) : texto;
        } catch (IOException e) {
            throw new PersistenciaException("No se pudo leer el archivo '" + ruta + "'.", e);
        }
    }

    private void escribirTexto(Path ruta, String texto) throws PersistenciaException {
        try {
            Files.writeString(ruta, texto, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PersistenciaException("No se pudo crear el archivo '" + ruta + "'.", e);
        }
    }

    /** Escribe en un archivo temporal y lo mueve sobre el destino. */
    private void escribirTextoAtomico(Path ruta, String texto) throws PersistenciaException {
        Path temporal = ruta.resolveSibling(ruta.getFileName() + ".tmp");
        try {
            Files.writeString(temporal, texto, StandardCharsets.UTF_8);
            try {
                Files.move(temporal, ruta, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal, ruta, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            try {
                Files.deleteIfExists(temporal);
            } catch (IOException ignorada) {
                // El .tmp sobrante no afecta los datos; está ignorado en .gitignore.
            }
            throw new PersistenciaException("No se pudo guardar el archivo '" + ruta + "'.", e);
        }
    }

    /**
     * Respalda un archivo dañado y construye la excepción para informarlo.
     * El bloqueo de escritura lo aplica quien llama ({@link #cargarUsuarios} o
     * {@link #cargarAmistades}) al recibir la excepción, solo para el archivo afectado.
     */
    private PersistenciaException archivoDanado(Path ruta, String motivo, Throwable causa) {
        Path respaldo = ruta.resolveSibling(ruta.getFileName() + ".corrupto-"
                + LocalDateTime.now().format(FORMATO_RESPALDO));
        String mensaje = "El archivo '" + ruta + "' está dañado: " + motivo + ".";
        try {
            Files.copy(ruta, respaldo, StandardCopyOption.REPLACE_EXISTING);
            mensaje += " Se guardó una copia en '" + respaldo.getFileName() + "'.";
        } catch (IOException e) {
            mensaje +=" No se pudo crear una copia de respaldo; se bloqueó la escritura para no perder datos.";
        }
        return new PersistenciaException(mensaje, causa);
    }
}
