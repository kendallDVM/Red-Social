# Ejecuta Social UNA usando como directorio de trabajo la raiz del proyecto (la carpeta
# de este script), sin importar desde donde se invoque. Asi siempre se usa el
# data/usuarios.json correcto y no se crean carpetas data/ accidentales.
# Requiere las clases compiladas en target/classes/ (Build en IntelliJ o "mvn compile").
Push-Location -LiteralPath $PSScriptRoot
try {
    java -cp "target/classes;lib/gson-2.15.0.jar;lib/bcrypt-0.10.2.jar;lib/bytes-1.5.0.jar" socialuna.Main
}
finally {
    Pop-Location   # devuelve la consola a la carpeta desde la que se invoco el script
}
