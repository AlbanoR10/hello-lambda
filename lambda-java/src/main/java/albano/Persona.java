package albano;

/** Modelo de dominio – se serializa y deserializa como JSON. */
public record Persona(String id,
                      String nombre,
                      String apellido,
                      int edad,
                      String equipoFavorito) {

    public Persona withId(String id) {
        return new Persona(id, nombre, apellido, edad, equipoFavorito);
    }

    public void validar() {
        if (nombre == null || nombre.isBlank())
            throw new IllegalArgumentException("nombre vacío");
        if (apellido == null || apellido.isBlank())
            throw new IllegalArgumentException("apellido vacío");
        if (edad <= 0)
            throw new IllegalArgumentException("edad debe ser > 0");
    }
}
