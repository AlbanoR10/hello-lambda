package albano;

/** Modelo de dominio – se serializa y deserializa como JSON. */
public record Persona(String nombre,
                      String apellido,
                      int edad,
                      String equipoFavorito) {

    public void validar() {
        if (nombre == null || nombre.isBlank())
            throw new IllegalArgumentException("nombre vacío");
        if (apellido == null || apellido.isBlank())
            throw new IllegalArgumentException("apellido vacío");
        if (edad <= 0)
            throw new IllegalArgumentException("edad debe ser > 0");
    }
}
