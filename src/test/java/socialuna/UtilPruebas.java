package socialuna;

import at.favre.lib.crypto.bcrypt.BCrypt;

/** Utilidades compartidas por las pruebas. */
public final class UtilPruebas {

    /** Costo mínimo de BCrypt: hace las pruebas rápidas (la aplicación usa 12). */
    public static final int COSTO_PRUEBAS = 4;

    private UtilPruebas() {
    }

    /** Hash BCrypt de una contraseña de prueba. */
    public static String hash(String contrasena) {
        return BCrypt.withDefaults().hashToString(COSTO_PRUEBAS, contrasena.toCharArray());
    }

    /** Verifica una contraseña contra un hash BCrypt. */
    public static boolean verifica(String contrasena, String hash) {
        return BCrypt.verifyer().verify(contrasena.toCharArray(), hash).verified;
    }
}
