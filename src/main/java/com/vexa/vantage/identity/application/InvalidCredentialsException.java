package com.vexa.vantage.identity.application;

/**
 * Se lanza cuando un intento de login falla, ya sea porque el email no
 * corresponde a ningún usuario, la contraseña no coincide, o el usuario
 * está deshabilitado.
 *
 * <p>Deliberadamente no distingue el motivo exacto en el mensaje ni en el
 * tipo: exponer por qué falló un login (email inexistente vs. contraseña
 * incorrecta vs. cuenta deshabilitada) permitiría enumerar usuarios
 * válidos, por lo que las tres causas se tratan de forma indistinguible
 * desde el exterior.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
