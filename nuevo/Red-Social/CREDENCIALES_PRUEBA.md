# Credenciales de prueba — Social UNA

Cuentas de **demostración**, incluidas en `data/usuarios.json` para probar el proyecto.

| ID | Nombre | Usuario | Contraseña de prueba |
|---|---|---|---|
| 2026001 | Kendall | kendall | SocialUNA2026! |
| 2026002 | Greivin | greivin | SocialUNA2026! |
| 2026003 | Mariano | mariano | SocialUNA2026! |

Notas:

- **Son credenciales de demostración.** No las use para cuentas reales.
- El sistema **no guarda las contraseñas en texto plano**. En `data/usuarios.json`, el campo `contrasena` contiene solo un hash **BCrypt** (costo 12) con sal aleatoria. Las contraseñas de esta tabla no aparecen en ningún otro archivo del proyecto.
- Al iniciar sesión, el nombre de usuario **no distingue mayúsculas** (`Kendall` = `kendall`). La contraseña **sí** las distingue.
- Las tres cuentas se crearon con el propio servicio de registro de la aplicación (`ServicioAutenticacion.registrar`), que usa la biblioteca BCrypt del proyecto.