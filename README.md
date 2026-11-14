# Social UNA

Red social estudiantil hecha en Java (Swing) para el Proyecto Final de Estructuras de Datos, Universidad Nacional de Costa Rica.
Equipo: Kendall, Greivin y Mariano.

## Funcionalidades

| Módulo | Qué incluye |
|---|---|
| **1. Autenticación** (Kendall) | Registro (ID, nombre, usuario y contraseña) y login con las credenciales leídas de los archivos. Las contraseñas se guardan con hash BCrypt. |
| **2. Interacción social** (Greivin) | Buscador de usuarios, solicitudes de amistad (enviar, aceptar, rechazar y cancelar), lista de amigos y eliminar amigos. |
| **3. Publicaciones y muro** (Mariano) | Publicar mensajes (máximo 500 caracteres), **Muro general** (propias y de amigos) y **Mi muro**, de la más reciente a la más antigua. **"Me gusta"** en cada publicación y **eliminar** las publicaciones propias. |
| **Perfiles** (Kendall) | Consulta y edición del perfil propio, y ver el perfil de otros usuarios. |

### "Me gusta"

- Cada publicación muestra un botón con el total. Si te gusta, aparece como «Te gusta».
- Al pasar el mouse sobre el botón se ve a quiénes les gusta.
- Solo se puede dar "me gusta" a las publicaciones propias y a las de los amigos (las del muro general). Volver a pulsar lo quita.
- Se guarda en `data/publicaciones.json` en el momento. Los archivos anteriores, sin ese campo, siguen funcionando.

### Eliminar publicaciones

- Cada publicación propia muestra el botón **Eliminar**, que pide confirmación. Solo el autor puede eliminarla.
- Deja de verse en todos los muros (el propio y el de los amigos) y se guarda en el momento.
- En `data/publicaciones.json` se marca con `"eliminada": true` en vez de borrarse, para que otra instancia abierta del programa se entere y no la vuelva a agregar.

## Estructuras de datos y archivos

Todo se maneja en memoria con estructuras propias y se guarda en `data/` (JSON, sin bases de datos).

| Estructura | Uso | Archivo |
|---|---|---|
| `ListaDinamica` (arreglo que crece al doble) | Base de usuarios, publicaciones y listas de resultados | |
| `ListaUsuarios` | Catálogo de usuarios, con índice por nombre de usuario | `usuarios.json` |
| `RedAmistades` (lista de adyacencia) | Quién es amigo de quién (grafo no dirigido) | `amistades.json` |
| `ListaSolicitudes` | Solicitudes de amistad pendientes | `solicitudes.json` |
| `ListaPublicaciones` | Historial de publicaciones y sus "me gusta" | `publicaciones.json` |
| `ListaPerfiles` | Información opcional de cada usuario | `perfiles.json` |

- Al abrir la aplicación se leen los archivos y se reconstruyen las estructuras. Se guarda en cada cambio y también al cerrar sesión y al cerrar la aplicación.
- La escritura es atómica (archivo temporal y reemplazo). Si un archivo está dañado, se hace una copia de respaldo y se bloquea el guardado para no perder datos.
- Si el programa se abre más de una vez sobre la misma carpeta `data/`, cada instancia relee del disco lo que cambió la otra (amistades, solicitudes, perfiles y publicaciones) antes de modificarlo. Así no se pierden datos.

## Cómo abrirla

Requiere Java 17 o superior. Instrucciones completas en [INSTRUCCIONES.txt](INSTRUCCIONES.txt).

```bash
mvn package        # genera target/SocialUNA.jar
java -jar target/SocialUNA.jar
```

También se puede abrir con `run.bat`. El programa debe ejecutarse junto a la carpeta `data/`.

Usuarios de demostración (contraseña `SocialUNA2026!`): `kendall`, `greivin` y `mariano`. Más detalles en [CREDENCIALES_PRUEBA.md](CREDENCIALES_PRUEBA.md).

## Pruebas

```bash
mvn test
```

Las pruebas usan carpetas temporales y nunca modifican `data/`.

## Estructura del código

```
src/main/java/socialuna/
  modelo/      Usuario, Publicacion, Amistad, SolicitudAmistad, Perfil
  datos/       Listas en memoria y GestorArchivos (lectura y escritura de los JSON)
  servicios/   Reglas de negocio de cada módulo (sin interfaz gráfica)
  gui/         Ventanas y paneles Swing
docs/          Diagrama de clases (UML)
```
