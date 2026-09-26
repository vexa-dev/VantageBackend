package com.vexa.vantage.shared.domain;

/**
 * Se lanza cuando se viola un invariante de dominio por un valor bruto
 * inválido (por ejemplo, un identificador {@code null} o en blanco pasado a
 * un objeto de valor del núcleo compartido).
 *
 * <p>Java puro: extiende {@link IllegalArgumentException} para que el manejo
 * existente de {@code IllegalArgumentException} siga funcionando, a la vez
 * que le da al núcleo compartido un tipo distinto y con nombre semántico que
 * {@code ApiExceptionHandler} puede mapear a HTTP 400.
 */
public class DomainValidationException extends IllegalArgumentException {

    public DomainValidationException(String message) {
        super(message);
    }

    /**
     * Valida que {@code value} no sea {@code null} ni esté en blanco,
     * devolviéndolo sin cambios cuando es válido.
     *
     * @param value     el valor bruto a validar
     * @param fieldName el nombre usado en el mensaje de la excepción (por
     *                  ejemplo, el objeto de valor que se está construyendo)
     * @return {@code value}, cuando es válido
     * @throws DomainValidationException cuando {@code value} es {@code null} o está en blanco
     */
    public static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException(fieldName + " value must not be null or blank");
        }
        return value;
    }
}
