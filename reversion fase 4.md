# Informe de reversión de la Fase 4

## 1. Estado encontrado

- Rama actual: `Kendall` (al día con `origin/Kendall`).
- `git log --oneline` muestra un solo commit: `f5861cc Initial commit`, que solo contiene `README.md`.
- `git status`: todo el código (`src/`, `pom.xml`, `data/`, `lib/`, `.idea/`, `run.bat`, `run.ps1`, `.gitignore`) está **sin seguimiento (untracked)**. Por eso `git diff` no mostraba ningún cambio y Git **no** podía servir para identificar ni revertir la Fase 4.

Para identificar los cambios se usaron dos fuentes que coinciden entre sí:

1. **Registro de la propia sesión de trabajo.** Todos los cambios de la Fase 4 se hicieron en esta misma sesión, y antes de modificar cada archivo existente se había leído su contenido original completo.
2. **Fechas de modificación de los archivos.** Todos los archivos de las Fases 2 y 3 tienen fecha del 4 de noviembre (entre las 23:26 y las 23:44). Los únicos archivos con fecha del 5 de noviembre (después de las 00:30) eran exactamente los de la Fase 4.

Cambios de la Fase 4 encontrados (todos pertenecen al Módulo 2):

| Tipo | Archivo | Contenido |
|---|---|---|
| Creado | `src/main/java/socialuna/servicios/ServicioAmistades.java` | Búsqueda de usuarios, agregar y consultar amigos |
| Creado | `src/main/java/socialuna/servicios/ResultadoAmistad.java` | Resultado de las operaciones de amistad |
| Creado | `src/main/java/socialuna/gui/PanelBuscarUsuarios.java` | Panel de búsqueda |
| Creado | `src/main/java/socialuna/gui/PanelAmigos.java` | Panel de amigos |
| Creado | `src/main/java/socialuna/gui/ModeloTablaUsuarios.java` | Modelo de tabla de los paneles |
| Creado | `src/main/java/socialuna/gui/TablaConMensaje.java` | Tabla con mensaje vacío de los paneles |
| Creado | `src/test/java/socialuna/datos/RedAmistadesTest.java` | Pruebas del Módulo 2 |
| Creado | `src/test/java/socialuna/datos/GestorArchivosAmistadesTest.java` | Pruebas del Módulo 2 |
| Creado | `src/test/java/socialuna/servicios/ServicioAmistadesTest.java` | Pruebas del Módulo 2 |
| Modificado | `src/main/java/socialuna/datos/RedAmistades.java` | Operaciones de aristas, copia, `LinkedHashMap` |
| Modificado | `src/main/java/socialuna/datos/GestorArchivos.java` | `cargarAmistades`/`guardarAmistades`, bloqueo de escritura de amistades, integración en `cargarTodo`/`guardarTodo` |
| Modificado | `src/main/java/socialuna/Main.java` | Opción provisional "Buscar y amigos" en el aviso temporal |
| Modificado | `src/main/java/socialuna/gui/package-info.java` | Comentario de documentación del Módulo 2 |

Archivos del Módulo 1 que **no** fueron modificados en la Fase 4 (fechas del 4 de noviembre, sin cambios): `ServicioAutenticacion.java`, `ResultadoAutenticacion.java`, `VentanaLogin.java`, `VentanaRegistro.java`, `Sesion.java`, `NavegacionPrincipal.java`, `ContextoAplicacion.java`, `CierreAplicacion.java`, `Estilos.java`, `pom.xml` y todas las pruebas anteriores.

El archivo `fase 4 respuesta.md` **no existía**: la Fase 4 se interrumpió antes de escribir ese informe, así que no hubo que eliminarlo.

No se encontró ningún cambio de origen dudoso.

## 2. Archivos eliminados

Se eliminaron uno por uno (sin borrados masivos) los 9 archivos creados durante la Fase 4:

1. `src/main/java/socialuna/servicios/ServicioAmistades.java`
2. `src/main/java/socialuna/servicios/ResultadoAmistad.java`
3. `src/main/java/socialuna/gui/PanelBuscarUsuarios.java`
4. `src/main/java/socialuna/gui/PanelAmigos.java`
5. `src/main/java/socialuna/gui/ModeloTablaUsuarios.java`
6. `src/main/java/socialuna/gui/TablaConMensaje.java`
7. `src/test/java/socialuna/datos/RedAmistadesTest.java`
8. `src/test/java/socialuna/datos/GestorArchivosAmistadesTest.java`
9. `src/test/java/socialuna/servicios/ServicioAmistadesTest.java`

## 3. Archivos restaurados

| Archivo | Cambios revertidos | Cómo se verificó |
|---|---|---|
| `RedAmistades.java` | Se eliminaron `agregarAmistad`, `sonAmigos`, `cantidadAmigos`, `obtenerAmistades`, `cantidadAmistades`, `copia` y `reemplazarCon`. Se volvió a usar `HashMap` y se restauró el comentario original. | Comparación con `diff` contra el contenido original leído antes de la Fase 4: **idéntico**. |
| `GestorArchivos.java` | Se eliminaron `cargarAmistades`, `guardarAmistades`, el indicador `escrituraAmistadesBloqueada`, el método `archivoTieneContenido`, el import de `Amistad` y los cambios en `cargarTodo`, `guardarTodo` y `archivoDanado`. Se restauraron los comentarios `TODO Módulo 2 (Greivin)`. La persistencia de usuarios no se había modificado en su lógica y queda igual que antes. | Comparación con `diff` contra el original: **idéntico**. |
| `Main.java` | Se eliminaron el `ServicioAmistades`, la opción "Buscar y amigos", el método `mostrarModuloSocialTemporal` y sus imports. `destinoTemporal` vuelve a tener sus tres parámetros y sus dos opciones originales ("Cerrar sesión" y "Salir de la aplicación"). | Reversión exacta del parche aplicado (cada bloque debía aparecer exactamente una vez; si no, la reversión se detenía). |
| `gui/package-info.java` | Se restauró la línea `Pendiente, Módulo 2 (Greivin): paneles de búsqueda y amigos.` | Revisión del contenido. |

## 4. Archivos conservados

Se conservaron sin modificaciones todos los elementos de las Fases 2 y 3:

- **Infraestructura:** `pom.xml` (Gson, BCrypt y JUnit), `.gitignore`, `lib/`, `run.bat`, `run.ps1`, `.idea/`, `out/`.
- **Modelos:** `Usuario`, `Publicacion` y `Amistad` (esta última sigue con su contenido original de la Fase 2).
- **Estructuras:** `ListaDinamica`, `ListaUsuarios`, `ListaPublicaciones` y `RedAmistades` (restaurada a su estado original).
- **Persistencia:** `GestorArchivos` (restaurado a su estado original) y `PersistenciaException`.
- **Módulo 1:** `ServicioAutenticacion`, `ResultadoAutenticacion`, `Sesion`, `ContextoAplicacion`, `VentanaLogin`, `VentanaRegistro`, `NavegacionPrincipal`, `CierreAplicacion` y `Estilos`.
- **Pruebas anteriores:** `UtilPruebas`, `GestorArchivosTest`, `ListaDinamicaTest`, `ListaUsuariosTest`, `ModelosTest`, `ServicioAutenticacionTest` y `SesionTest`.
- **Datos:** `data/usuarios.json` no se modificó. Las pruebas y comprobaciones de la Fase 4 usaron carpetas temporales, así que nunca se creó un `data/amistades.json` real.
- **README.md:** no se modificó.

## 5. Verificaciones

Todas las comprobaciones siguientes se ejecutaron realmente:

| Comprobación | Resultado |
|---|---|
| `git status` | Igual que al inicio: solo los mismos 8 elementos sin seguimiento (`.gitignore`, `.idea/`, `data/`, `lib/`, `pom.xml`, `run.bat`, `run.ps1`, `src/`). No hay archivos rastreados modificados. |
| `README.md` | Su contenido en bytes es exactamente `# Red-Social` (12 bytes, sin salto de línea final), igual que al inicio. |
| Compilación (`mvn clean test`, Maven 3.9.16, JDK 27) | **BUILD SUCCESS**. |
| Pruebas | **68 ejecutadas, 68 aprobadas**, 0 fallos y 0 errores. Es la misma cantidad que antes de la Fase 4: GestorArchivosTest 11, ListaDinamicaTest 11, ListaUsuariosTest 13, ModelosTest 8, ServicioAutenticacionTest 21 y SesionTest 4. |
| Clases compiladas | `target/classes` contiene el mismo conjunto de clases de `gui` y `servicios` que la compilación anterior en `out/`, sin ninguna clase del Módulo 2. |
| BCrypt | La dependencia `at.favre.lib:bcrypt` está en `pom.xml` y se usa en `ServicioAutenticacion`. Las pruebas de hash y verificación aprobaron. |
| Autenticación y persistencia de usuarios | Prueba manual con una carpeta temporal: el registro devolvió `EXITO`, el inicio de sesión con `PRUEBA` (en mayúsculas) devolvió `EXITO` y se creó `usuarios.json`. Al cargar ya no se crea `amistades.json`, igual que en la Fase 3. |
| Ventana de login | Se construyó y se capturó una imagen de `VentanaLogin`, que se muestra correctamente con sus campos y los botones "Ingresar" y "Crear cuenta". `VentanaRegistro` compila y no fue modificada, pero no se abrió de forma interactiva. |
| Navegación del Módulo 1 | En `src/` no quedan referencias a `ServicioAmistades`, `PanelBuscarUsuarios`, `PanelAmigos`, `ResultadoAmistad`, `TablaConMensaje` ni `ModeloTablaUsuarios`. `Main` vuelve a usar el aviso temporal original. |
| Archivos originales | Están presentes todos los archivos de las Fases 2 y 3 listados en la sección 4. |

## 6. Cambios pendientes

No quedó ningún cambio de la Fase 4 sin revertir. Observaciones menores:

- **Carpeta `target/`:** la creó Maven al compilar y probar durante la Fase 4 y esta reversión. Es salida de compilación generada, está ignorada en `.gitignore` y ahora contiene solo las clases de la Fase 3. No se eliminó porque no forma parte del código. Puede borrarse sin riesgo si se desea.
- **Repositorio local de Maven (`~/.m2`):** durante las pruebas se descargaron plugins de Maven. Esto no afecta al proyecto.
- **Archivos temporales de comprobación:** los programas de prueba visual y sus datos se crearon fuera del proyecto, en la carpeta temporal de la sesión. No afectan al proyecto.

## 7. Estado final

El proyecto **recuperó el estado de la Fase 3**. Los archivos de código modificados son idénticos a sus versiones originales (`GestorArchivos` y `RedAmistades` se verificaron con `diff`), se eliminaron todos los archivos del Módulo 2, el proyecto compila y aprueba las mismas 68 pruebas de antes, y `README.md` y `data/usuarios.json` no cambiaron. No se hizo ningún commit ni se usaron comandos destructivos de Git.
